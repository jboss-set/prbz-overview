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

import static org.jboss.set.assistant.Constants.EAP64XPAYLOADPATTERN;
import static org.jboss.set.assistant.Constants.EAP70XPAYLOADPATTERN;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Release;
import org.jboss.set.aphrodite.domain.SearchCriteria;
import org.jboss.set.aphrodite.domain.StreamComponent;
import org.jboss.set.aphrodite.spi.NotFoundException;

/**
 * @author wangc
 *
 */
public class Util {

    public static Logger logger = Logger.getLogger(Util.class.getCanonicalName());
    public static LinkedHashMap<String, Issue> bzPayloadStore = new LinkedHashMap<>();
    public static LinkedHashMap<String, List<Issue>> jiraPayloadStore = new LinkedHashMap<>();

    public static boolean filterComponent(StreamComponent component) {
        String name = component.getName().trim();
        return name.equalsIgnoreCase(Constants.WILDFLY_WILDFLY)
                || name.equalsIgnoreCase(Constants.WILDFLY_WILDFLY_CORE)
                || name.equalsIgnoreCase(Constants.JBOSSAS_JBOSS_EAP7)
                || name.equalsIgnoreCase(Constants.JBOSSAS_WILDFLY_CORE_EAP)
                || name.equalsIgnoreCase(Constants.JBOSSAS_JBOSS_EAP);
    }

    public static void findAllBugzillaPayloads(Aphrodite aphrodite, boolean first) {
        if (first) {
            for (int i = 6411; i < 6420; i++) {
                // first time run, search from eap6411-payload to eap6420-payload
                Issue payloadCandidate = testBzPayloadExistence(aphrodite, i);
                if (payloadCandidate != null) {
                    bzPayloadStore.put(Constants.EAP64XPAYLOAD_ALIAS_PREFIX + i + Constants.EAP64XPAYLOAD_ALIAS_SUFFIX, payloadCandidate);
                    logger.log(Level.INFO, "Found Bugzilla Payload : " + Constants.EAP64XPAYLOAD_ALIAS_PREFIX + i);
                }
            }
        } else {
            // schedule update run, search any new one until it hits NotFoundException for nonexistent alias.
            String lastKey = (String) bzPayloadStore.keySet().toArray()[bzPayloadStore.size() -1];
            Matcher matcher = EAP64XPAYLOADPATTERN.matcher(lastKey);
            if (matcher.find()) {
                int index = Integer.parseInt(matcher.group(1));
                for (int i = 6411; i <= index; i++) {
                    Issue payloadCandidate = testBzPayloadExistence(aphrodite, i);
                    if (payloadCandidate != null) {
                        bzPayloadStore.put(Constants.EAP64XPAYLOAD_ALIAS_PREFIX + i + Constants.EAP64XPAYLOAD_ALIAS_SUFFIX, payloadCandidate);
                        logger.log(Level.INFO, "Reload Bugzilla Payload : " + Constants.EAP64XPAYLOAD_ALIAS_PREFIX + i);
                    }
                }
                index++;
                Issue payloadCandidate = testBzPayloadExistence(aphrodite, index);
                while (payloadCandidate != null) {
                    bzPayloadStore.put(Constants.EAP64XPAYLOAD_ALIAS_PREFIX + index + Constants.EAP64XPAYLOAD_ALIAS_SUFFIX, payloadCandidate);
                    logger.log(Level.INFO, "Found new Bugzilla Payloads : " + Constants.EAP64XPAYLOAD_ALIAS_PREFIX + index);
                    index++;
                    payloadCandidate = testBzPayloadExistence(aphrodite, index);
                }
            }
        }
        logger.log(Level.INFO, "Found Bugzilla Payloads : " + bzPayloadStore.keySet());
    }

    private static Issue testBzPayloadExistence(Aphrodite aphrodite, int i) {
        try {
            URL url = new URL(Constants.BUGZILLA_URL_PREFIX + Constants.EAP64XPAYLOAD_ALIAS_PREFIX + i + Constants.EAP64XPAYLOAD_ALIAS_SUFFIX);
            Issue payloadCandidate = aphrodite.getIssue(url);
            return payloadCandidate;
        } catch (MalformedURLException e) {
            // OK, ignored with null as return value.
            return null;
        } catch (NotFoundException e) {
            // OK, ignored with null as return value. Nonexistent alias, just don't add to return result.
            return null;
        }
    }

    public static void findAllJiraPayloads(Aphrodite aphrodite, boolean first) {
        if (first) {
            for (int i = 1; i < 10; i++) {
                // search from 7.0.1.GA to 7.0.9.GA, add to list if result is not empty.
                String fixVersion = Constants.EAP70XPAYLOAD_ALIAS_PREFIX + i + Constants.EAP70XPAYLOAD_ALIAS_SUFFIX;
                List<Issue> issues = testJiraPayloadExistence(aphrodite, fixVersion);
                if (!issues.isEmpty()){
                    jiraPayloadStore.put(fixVersion, issues);
                    logger.log(Level.INFO, "Found Jira Payload : " + fixVersion);
                }
            }
        } else {
            String lastKey = (String) jiraPayloadStore.keySet().toArray()[jiraPayloadStore.size() - 1];
            Matcher matcher = EAP70XPAYLOADPATTERN.matcher(lastKey);
            if (matcher.find()) {
                int index = Integer.parseInt(matcher.group(1));

                for (int i = 1; i <= index; i++) {
                    // update from 7.0.1.GA to index, add to list if result is not empty.
                    String fixVersion = Constants.EAP70XPAYLOAD_ALIAS_PREFIX + i + Constants.EAP70XPAYLOAD_ALIAS_SUFFIX;
                    List<Issue> issues = testJiraPayloadExistence(aphrodite, fixVersion);
                    if (!issues.isEmpty()) {
                        jiraPayloadStore.put(fixVersion, issues);
                        logger.log(Level.INFO, "Reload Jira Payload : " + fixVersion);
                    }
                }

                index++;
                String fixVersion = Constants.EAP70XPAYLOAD_ALIAS_PREFIX + index + Constants.EAP70XPAYLOAD_ALIAS_SUFFIX;
                List<Issue> issues = testJiraPayloadExistence(aphrodite, fixVersion);
                if (!issues.isEmpty()) {
                    jiraPayloadStore.put(fixVersion, issues);
                    logger.log(Level.INFO, "Found new Jira Payloads : " + fixVersion);
                }
            }
        }
        logger.log(Level.INFO, "Found Jira Payloads : " + jiraPayloadStore.keySet());
    }

    private static List<Issue> testJiraPayloadExistence(Aphrodite aphrodite, String fixVersion) {
        SearchCriteria sc = new SearchCriteria.Builder()
                .setRelease(new Release(fixVersion.trim()))
                .setProduct("JBEAP")
                .setMaxResults(200)
                .build();
        return aphrodite.searchIssues(sc);
    }
}
