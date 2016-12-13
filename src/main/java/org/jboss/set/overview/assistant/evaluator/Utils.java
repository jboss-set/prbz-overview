/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.set.overview.assistant.evaluator;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;

/**
 * @author egonzalez
 *
 */
public final class Utils {
    private Utils() {
    }

    private static final Pattern patternStreamFlagBuzilla = Pattern.compile("jboss-eap-[0-9]\\.[0-9]\\.[0-9z]");
    private static final Pattern patternStreamFlagJira = Pattern.compile("[0-9]\\.[a-zA-Z]*(\\.[0-9z])?(\\.[a-zA-Z]*)?");

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
}