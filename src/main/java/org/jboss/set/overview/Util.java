/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.set.overview;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueStatus;
import org.jboss.set.aphrodite.domain.StreamComponent;
import org.jboss.set.aphrodite.issue.trackers.jira.JiraRelease;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.assist.Constants;
import static org.jboss.set.assist.Constants.DEV_PROFILE;
import static org.jboss.set.assist.Constants.DEV_STREAM;
import static org.jboss.set.assist.Constants.EAP64ZPAYLOADPATTERN;
import static org.jboss.set.assist.Constants.EAP64ZSTREAM;
import static org.jboss.set.assist.Constants.EAP_PREFIX;
import static org.jboss.set.assist.Constants.EAP_SUFFIX;
import static org.jboss.set.assist.Constants.RELEASED_DISABLED;

import javax.naming.NameNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * @author wangc
 *
 */
public class Util {

    private static Logger logger = Logger.getLogger(Util.class.getCanonicalName());
    public static Map<String, List<JiraRelease>> jiraVersions = new TreeMap<>();
    public static LinkedHashMap<String, Issue> bzPayloadStore = new LinkedHashMap<>();
    public static LinkedHashMap<String, LinkedHashMap<String, Issue>> bzPayloadStoresByStream = new LinkedHashMap<>();
    public static LinkedHashMap<String, LinkedHashMap<String, List<Issue>>> jiraPayloadStoresByStream = new LinkedHashMap<>();

    // some default boundary in first time load.
    // Bugzilla, default range is from eap6423-payload to eap6425-payload due to inactive EAP 6.4.x series.
    private static int FIRST_64X_PAYLOAD = 6423;
    private static int DEVMODE_64X_PAYLOAD = 6424;
    private static int LAST_64X_PAYLOAD = 6425;

    // 7.z CP stream jira payload, default range is from 7.1.1 to 7.1.5, development mode to 7.1.2
    private static int DEVMODE_PAYLOAD = 2;
    private static int LAST_PAYLOAD = 5;

    private static final boolean releasedDisabled;
    private static final boolean devProfile;
    static {
        String devProfileValue = getValueFromPropertyAndEnv(DEV_PROFILE);
        devProfile =  devProfileValue != null ? Boolean.valueOf(devProfileValue): false;
        String releasedDisabledValue = getValueFromPropertyAndEnv(RELEASED_DISABLED);
        releasedDisabled = releasedDisabledValue != null ? Boolean.valueOf(releasedDisabledValue): false;
    }

    // We are only care about following components defined in jboss-streams
    public static boolean filterComponent(StreamComponent component) {
        String name = component.getName().trim();
        return name.equalsIgnoreCase(Constants.WILDFLY_WILDFLY)
                || name.equalsIgnoreCase(Constants.WILDFLY_WILDFLY_CORE)
                || name.equalsIgnoreCase(Constants.JBOSSAS_WILDFLY_CORE_EAP)
                || name.equalsIgnoreCase(Constants.JBOSSAS_JBOSS_EAP7)
                || name.equalsIgnoreCase(Constants.JBOSSAS_JBOSS_EAP8)
                || name.equalsIgnoreCase(Constants.JBOSSAS_JBOSS_EAP);
    }

    public static void findAllBugzillaPayloads(Aphrodite aphrodite, boolean first) {
        if (first | bzPayloadStore.size() == 0) {
            int max = LAST_64X_PAYLOAD;
            for (int i = FIRST_64X_PAYLOAD; i <= max; i++) {
                // first time run, search from FIRST_64X_PAYLOAD to LAST_64X_PAYLOAD
                Issue payloadCandidate = testBzPayloadExistence(aphrodite, i);
                if (payloadCandidate != null) {
                    bzPayloadStore.put(Constants.EAP64ZPAYLOAD_ALIAS_PREFIX + i + Constants.EAP64ZPAYLOAD_ALIAS_SUFFIX, payloadCandidate);
                    logger.log(Level.INFO, "Found Bugzilla Payload : " + Constants.EAP64ZPAYLOAD_ALIAS_PREFIX + i);
                }
            }
        } else {
            // schedule update run, search any new one until it hits NotFoundException for nonexistent alias.
            String lastKey = (String) bzPayloadStore.keySet().toArray()[bzPayloadStore.size() - 1];
            Matcher matcher = EAP64ZPAYLOADPATTERN.matcher(lastKey);
            if (matcher.find()) {
                int index = Integer.parseInt(matcher.group(1));
                for (int i = FIRST_64X_PAYLOAD; i <= index; i++) {
                    Issue payloadCandidate = testBzPayloadExistence(aphrodite, i);
                    if (payloadCandidate != null) {
                        if (payloadCandidate.getStatus().equals(IssueStatus.CLOSED) || payloadCandidate.getStatus().equals(IssueStatus.VERIFIED)) {
                            logger.log(Level.INFO, "Skip Bugzilla Payload : " + Constants.EAP64ZPAYLOAD_ALIAS_PREFIX + i);
                        } else {
                            bzPayloadStore.put(Constants.EAP64ZPAYLOAD_ALIAS_PREFIX + i + Constants.EAP64ZPAYLOAD_ALIAS_SUFFIX, payloadCandidate);
                            logger.log(Level.INFO, "Reload Bugzilla Payload : " + Constants.EAP64ZPAYLOAD_ALIAS_PREFIX + i);
                        }
                    }
                }
                // Try to query a new payload on index++
                index++;
                Issue payloadCandidate = testBzPayloadExistence(aphrodite, index);
                while (payloadCandidate != null) {
                    bzPayloadStore.put(Constants.EAP64ZPAYLOAD_ALIAS_PREFIX + index + Constants.EAP64ZPAYLOAD_ALIAS_SUFFIX, payloadCandidate);
                    logger.log(Level.INFO, "Found new Bugzilla Payloads : " + Constants.EAP64ZPAYLOAD_ALIAS_PREFIX + index);
                    index++;
                    payloadCandidate = testBzPayloadExistence(aphrodite, index);
                }
            }
        }
        bzPayloadStoresByStream.put(EAP64ZSTREAM, bzPayloadStore);
        logger.log(Level.INFO, "Found Bugzilla Payloads : " + bzPayloadStore.keySet());
    }

    private static Issue testBzPayloadExistence(Aphrodite aphrodite, int i) {
        try {
            URL url = new URL(Constants.BUGZILLA_URL_PREFIX + Constants.EAP64ZPAYLOAD_ALIAS_PREFIX + i + Constants.EAP64ZPAYLOAD_ALIAS_SUFFIX);
            Issue payloadCandidate = aphrodite.getIssue(url);
            return payloadCandidate;
        } catch (MalformedURLException e) {
            // OK, ignored with null as return value.
            return null;
        } catch (NotFoundException e) {
            // OK, ignored with null as return value. Nonexistent alias, just don't add to return result.
            return null;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error to find Bugzilla for payload " + i + " due to " + e.getMessage(), e);
            return null;
        }
    }

    public static void findAllJiraVersions() {
        try {
            jiraVersions = JiraRelease.findAll().stream()
                    .filter(jiraRelease -> !devProfile || jiraRelease.getVersion().getName().contains(DEV_STREAM))
                    .filter(jiraRelease -> !(releasedDisabled && jiraRelease.getVersion().isReleased()))
                    .filter(jiraRelease -> !jiraRelease.getVersion().getName().endsWith(".0.GA"))
                    .sorted(Comparator.comparing((JiraRelease jr) -> jr.getVersion().getName()).reversed())
                    .collect(Collectors.groupingBy(jr -> jr.getVersion().getName().substring(0,3)));
        } catch (NameNotFoundException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    public static void findAllJiraPayloads() {
        for (String jp : jiraVersions.keySet()) {
            findJiraPayloads(jp);
        }
    }

    private static void findJiraPayloads(String shortStreamName) {
        boolean first = false;
        String eapStream = EAP_PREFIX + shortStreamName + EAP_SUFFIX;
        LinkedHashMap<String, List<Issue>> payloadStore = jiraPayloadStoresByStream.get(eapStream);
        if (payloadStore == null) {
            first = true;
            payloadStore = new LinkedHashMap<>();
        }
        try {
            // load 'max' payloads the first time, next time load the rest
            int i = 0;
            int max = devProfile ? DEVMODE_PAYLOAD : LAST_PAYLOAD;

            for(JiraRelease release : jiraVersions.get(shortStreamName)) {
                String version = release.getVersion().getName();
                if (payloadStore.containsKey(version) && release.getVersion().isReleased()) {
                    logger.log(Level.INFO, "Skipping update for: " + version);
                    continue;
                }
                List<Issue> issues = new ArrayList<>(release.getIssues());
                payloadStore.put(version, issues);
                logger.log(Level.INFO, "Found Jira Payload : " + version + " with " + issues.size() + " issues.");
                if (first && ++i >= max) break;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, " Error to find jira payload ", e);
        } finally {
            jiraPayloadStoresByStream.put(eapStream, payloadStore);
            logger.log(Level.INFO, "Found Jira Payloads : " + payloadStore.keySet() + " for " + eapStream);
        }
    }

    @Deprecated
    public static List<Issue> testJiraPayloadExistence(Aphrodite aphrodite, String fixVersion) {
        return getJiraPayload(fixVersion);
    }

    public static List<Issue> getJiraPayload(String fixVersion) {
        List<Issue> result = new ArrayList<>();
        String shortStreamName = fixVersion.substring(0,3);
        for (JiraRelease release : jiraVersions.get(shortStreamName)) {
            if (release.getVersion().getName().equals(fixVersion)) {
                result.addAll(release.getIssues());
                break;
            }
        }
        return result;
    }

    public static List<String> getNewIssuesInPayload(String fixVersion, String since) {
        try {
            String shortStreamName = fixVersion.substring(0,3);
            for (JiraRelease release : jiraVersions.get(shortStreamName)) {
                if (release.getVersion().getName().equals(fixVersion)) {
                    LocalDate sinceDate = LocalDate.parse(since);
                    return release.getNewIssues(sinceDate, null).stream()
                            .map(issue -> issue.getURL().toString()).collect(Collectors.toList());
                }
            }
        } catch (NameNotFoundException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }

        return Collections.EMPTY_LIST;
    }

    public static String getValueFromPropertyAndEnv(String key) {
        // check system properties first, if null check environment variables as well.
        String value = System.getProperty(key);
        if (value == null) {
            return System.getenv(key);
        }
        return value;
    }
}
