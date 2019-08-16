/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.set.assist;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.jbossset.bugclerk.Severity;
import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.assist.data.ProcessorData;
import org.jboss.set.assist.data.payload.PayloadIssue;
import org.jboss.set.assist.evaluator.impl.payload.PayloadIssueEvaluator;

/**
 * @author Jason T. Greene
 */
public class Util {

    private static final Pattern patternStreamFlagBuzilla = Pattern.compile("jboss-eap-[0-9]\\.[0-9]\\.[0-9z]");
    private static final Pattern patternStreamFlagJira = Pattern.compile("[0-9]\\.[a-zA-Z]*(\\.[0-9z])?(\\.[a-zA-Z]*)?");

    private static Logger logger = Logger.getLogger(Util.class.getCanonicalName());

    public static void safeClose(Closeable closeable) {
        if (closeable != null)
            try {
                closeable.close();
            } catch (Throwable e) {
            }
    }

    public static Map<String, String> map(String... args) {
        if (args == null || args.length == 0) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < args.length;) {
            map.put(args[i++], args[i++]);
        }

        return map;
    }

    public static Properties loadProperties(String configurationFileProperty, String configurationFileDefault)
            throws IOException {
        String propsFileUserPath = System.getProperty(configurationFileProperty, configurationFileDefault);
        Properties props = new Properties();
        props.load(new FileReader(new File(propsFileUserPath)));
        return props;
    }

    public static String require(Properties props, String name) {
        String ret = (String) props.get(name);
        if (ret == null)
            throw new RuntimeException(name + " must be specified in processor.properties");

        return ret.trim();
    }

    public static String get(Properties props, String name) {
        return (String) props.get(name);
    }

    public static String get(Properties props, String name, String defaultValue) {
        String value = (String) props.get(name);
        return (value == null) ? defaultValue : value;
    }

    public static String getTime() {
        Date date = new Date();
        return getTime(date);
    }

    public static String getTime(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return dateFormat.format(date);
    }

    public static URI convertURLtoURI(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            logger.log(Level.WARNING, "Error to convert URI from url : " + url, e);
        }
        return null;
    }

    public static Severity maxSeverity(List<ProcessorData> payloadData) {
        List<Severity> maxSeverityList = payloadData.stream()
                .filter(s -> ((PayloadIssue) s.getData().get(PayloadIssueEvaluator.KEY)).getMaxSeverity() != null)
                .map(e -> ((PayloadIssue) e.getData().get(PayloadIssueEvaluator.KEY)).getMaxSeverity())
                .collect(Collectors.toList());
        return maxSeverityList.stream().reduce((severity1, severity2) -> maxSeverity(severity1, severity2)).orElse(null);
    }

    public static Severity maxSeverity(Severity s1, Severity s2) {
        if (s1 == Severity.BLOCKER || s2 == Severity.BLOCKER)
            return Severity.BLOCKER;
        if (s1 == Severity.CRITICAL || s2 == Severity.CRITICAL)
            return Severity.CRITICAL;
        if (s1 == Severity.MAJOR || s2 == Severity.MAJOR)
            return Severity.MAJOR;
        if (s1 == Severity.MINOR || s2 == Severity.MINOR)
            return Severity.MINOR;
        return Severity.TRIVIAL;
    }

    public static Color convertSeverityToColor(Severity s) {
        if (s == Severity.BLOCKER)
            return Color.RED;
        if (s == Severity.CRITICAL)
            return Color.ORANGE;
        if (s == Severity.MAJOR)
            return Color.YELLOW;
        if (s == Severity.MINOR)
            return Color.BLUE;
        if (s == Severity.TRIVIAL)
            return Color.GRAY;
        return null;
    }

    public static List<ProcessorData> filterBySelectedStatus(List<ProcessorData> payloadData, List<String> selectedStatus) {
        return payloadData.stream().filter(e -> {
            Severity s = ((PayloadIssue) e.getData().get(PayloadIssueEvaluator.KEY)).getMaxSeverity();
            if (s == null && selectedStatus.contains(String.valueOf(Color.GREEN)))
                return true;
            else
                return selectedStatus.contains(String.valueOf(convertSeverityToColor(s)));
        }).collect(Collectors.toList());
    }

    public static List<ProcessorData> filterByMissedFlags(List<ProcessorData> payloadData, List<String> missedFlags) {
        return payloadData.stream().filter(e -> {
            Map<String, String> flags = ((PayloadIssue) e.getData().get(PayloadIssueEvaluator.KEY)).getFlags();
            if (flags.isEmpty())
                return true;
            else
                return flags.keySet().stream().anyMatch(k -> (missedFlags.contains(k) && flags.get(k) != String.valueOf(FlagStatus.ACCEPTED)));
        }).collect(Collectors.toList());
    }

    public static List<String> getStreams(Issue issue) {

        EnumSet<FlagStatus> set = EnumSet.of(FlagStatus.ACCEPTED, FlagStatus.SET);
        List<String> streams = new ArrayList<>();
        Map<String, FlagStatus> statuses = issue.getStreamStatus();
        for (Map.Entry<String, FlagStatus> status : statuses.entrySet()) {
            String stream = extract(status.getKey());
            if (stream != null && set.contains(status.getValue())) {
                streams.add(stream);
            }
        }
        return streams;
    }

    public static String extract(String value) {
        Matcher matcherBugzilla = patternStreamFlagBuzilla.matcher(value);
        Matcher matcherJira = patternStreamFlagJira.matcher(value);
        if (matcherBugzilla.find()) {
            String bugzilla = matcherBugzilla.group();
            return bugzilla;
        } else if (matcherJira.find()) {
            return matcherJira.group();
        } else {
            return null;
        }
    }

    /**
     * Check PM, DEV and QE ack for a given issue
     *
     * @param issue to be tested
     * @return true if all 3 ack are positive, otherwise false.
     */
    public static boolean isAllAcks(Issue issue) {
        for (Flag flag : Flag.values()) {
            if (issue.getStage() == null) {
                // no ack is set.
                return false;
            }
            FlagStatus status = issue.getStage().getStatus(flag);
            if (!status.equals(FlagStatus.ACCEPTED)) {
                return false;
            }
        }
        return true;
    }
}
