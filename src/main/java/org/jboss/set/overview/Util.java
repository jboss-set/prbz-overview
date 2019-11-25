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

import static org.jboss.set.assist.Constants.EAP64ZPAYLOADPATTERN;
import static org.jboss.set.assist.Constants.EAP70ZPAYLOADPATTERN;
import static org.jboss.set.assist.Constants.EAP71ZPAYLOADPATTERN;
import static org.jboss.set.assist.Constants.EAP72ZPAYLOADPATTERN;
import static org.jboss.set.assist.Constants.EAP73ZPAYLOADPATTERN;
import static org.jboss.set.assist.Constants.EAP64ZSTREAM;
import static org.jboss.set.assist.Constants.EAP70ZSTREAM;
import static org.jboss.set.assist.Constants.EAP71ZSTREAM;
import static org.jboss.set.assist.Constants.EAP72ZSTREAM;
import static org.jboss.set.assist.Constants.EAP73ZSTREAM;
import static org.jboss.set.assist.Constants.EAP70ZPAYLOAD_ALIAS_PREFIX;
import static org.jboss.set.assist.Constants.EAP71ZPAYLOAD_ALIAS_PREFIX;
import static org.jboss.set.assist.Constants.EAP72ZPAYLOAD_ALIAS_PREFIX;
import static org.jboss.set.assist.Constants.EAP73ZPAYLOAD_ALIAS_PREFIX;
import static org.jboss.set.assist.Constants.EAP7PAYLOAD_ALIAS_SUFFIX;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueStatus;
import org.jboss.set.aphrodite.domain.Release;
import org.jboss.set.aphrodite.domain.SearchCriteria;
import org.jboss.set.aphrodite.domain.StreamComponent;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.assist.Constants;

/**
 * @author wangc
 *
 */
public class Util {

    private static Logger logger = Logger.getLogger(Util.class.getCanonicalName());
    public static LinkedHashMap<String, Issue> bzPayloadStore = new LinkedHashMap<>();
    public static LinkedHashMap<String, LinkedHashMap<String, Issue>> bzPayloadStoresByStream = new LinkedHashMap<>();
    public static LinkedHashMap<String, List<Issue>> jiraPayloadStore_70Z = new LinkedHashMap<>();
    public static LinkedHashMap<String, List<Issue>> jiraPayloadStore_71Z = new LinkedHashMap<>();
    public static LinkedHashMap<String, List<Issue>> jiraPayloadStore_72Z = new LinkedHashMap<>();
    public static LinkedHashMap<String, List<Issue>> jiraPayloadStore_73Z = new LinkedHashMap<>();
    public static LinkedHashMap<String, LinkedHashMap<String, List<Issue>>> jiraPayloadStoresByStream = new LinkedHashMap<>();

    // some default boundary in first time load.
    // Bugzilla, default range is from eap6411-payload to eap6420-payload, development mode to eap6415-payload
    private static int FIRST_64X_PAYLOAD = 6411;
    private static int DEVMODE_64X_PAYLOAD = 6415;
    private static int LAST_64X_PAYLOAD = 6425;

    // 7.z CP stream jira payload, default range is from 7.1.1 to 7.1.5, development mode to 7.1.2
    private static int FIRST_PAYLOAD = 1;
    private static int DEVMODE_PAYLOAD = 2;
    private static int LAST_PAYLOAD = 5;


    private static final boolean devProfile = System.getProperty("prbz-dev") != null;

    // We are only care about following components defined in jboss-streams
    public static boolean filterComponent(StreamComponent component) {
        String name = component.getName().trim();
        return name.equalsIgnoreCase(Constants.WILDFLY_WILDFLY)
                || name.equalsIgnoreCase(Constants.WILDFLY_WILDFLY_CORE)
                || name.equalsIgnoreCase(Constants.JBOSSAS_WILDFLY_CORE_EAP)
                || name.equalsIgnoreCase(Constants.JBOSSAS_JBOSS_EAP);
    }

    public static void findAllBugzillaPayloads(Aphrodite aphrodite, boolean first) {
        if (first | bzPayloadStore.size() == 0) {
            int max = devProfile ? DEVMODE_64X_PAYLOAD : LAST_64X_PAYLOAD;
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

    public static void findAllJiraPayloads(Aphrodite aphrodite, boolean first) {
        findJiraPayloads(aphrodite, first, EAP70ZSTREAM, jiraPayloadStore_70Z, EAP70ZPAYLOAD_ALIAS_PREFIX, EAP70ZPAYLOADPATTERN);
        findJiraPayloads(aphrodite, first, EAP71ZSTREAM, jiraPayloadStore_71Z, EAP71ZPAYLOAD_ALIAS_PREFIX, EAP71ZPAYLOADPATTERN);
        findJiraPayloads(aphrodite, first, EAP72ZSTREAM, jiraPayloadStore_72Z, EAP72ZPAYLOAD_ALIAS_PREFIX, EAP72ZPAYLOADPATTERN);
        findJiraPayloads(aphrodite, first, EAP73ZSTREAM, jiraPayloadStore_73Z, EAP73ZPAYLOAD_ALIAS_PREFIX, EAP73ZPAYLOADPATTERN);
    }

    private static void findJiraPayloads(Aphrodite aphrodite, boolean first, String eapStream, LinkedHashMap<String, List<Issue>> jiraPayloadStore, String payloadPrefix, Pattern payloadPattern) {
        try {
            if (first | jiraPayloadStore.size() == 0) {
                int max = devProfile ? DEVMODE_PAYLOAD : LAST_PAYLOAD;
                for (int i = FIRST_PAYLOAD; i <= max; i++) {
                    // search from firstPayload to lastPayload, add to list if result is not empty.
                    String fixVersion = payloadPrefix + i + EAP7PAYLOAD_ALIAS_SUFFIX;
                    List<Issue> issues = testJiraPayloadExistence(aphrodite, fixVersion);
                    if (!issues.isEmpty()) {
                        jiraPayloadStore.put(fixVersion, issues);
                        logger.log(Level.INFO, "Found Jira Payload : " + fixVersion);

                    }
                }
            } else {
                String lastKey = (String) jiraPayloadStore.keySet().toArray()[jiraPayloadStore.size() - 1];
                Matcher matcher = payloadPattern.matcher(lastKey);
                if (matcher.find()) {
                    int index = Integer.parseInt(matcher.group(1));
                    for (int i = FIRST_PAYLOAD; i <= index; i++) {
                        // update from 7.0.1.GA to index, add to list if result is not empty.
                        String fixVersion = payloadPrefix + i + EAP7PAYLOAD_ALIAS_SUFFIX;
                        if (aphrodite.isCPReleased(fixVersion)) {
                            logger.log(Level.INFO, "Skip released Jira Payload : " + fixVersion);
                        } else {
                            List<Issue> issues = testJiraPayloadExistence(aphrodite, fixVersion);
                            if (!issues.isEmpty()) {
                                jiraPayloadStore.put(fixVersion, issues);
                                logger.log(Level.INFO, "Reload Jira Payload : " + fixVersion);
                            }
                        }
                    }

                    index++;
                    // Try to query a new payload on index++
                    String fixVersion = payloadPrefix + index + EAP7PAYLOAD_ALIAS_SUFFIX;
                    List<Issue> issues = testJiraPayloadExistence(aphrodite, fixVersion);
                    if (!issues.isEmpty()) {
                        jiraPayloadStore.put(fixVersion, issues);
                        logger.log(Level.INFO, "Found new Jira Payloads : " + fixVersion);
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, " Error to find jira payload ", e);
        } finally {
            jiraPayloadStoresByStream.put(eapStream, jiraPayloadStore);
            logger.log(Level.INFO, "Found Jira Payloads : " + jiraPayloadStore.keySet());
        }
    }

    private static List<Issue> testJiraPayloadExistence(Aphrodite aphrodite, String fixVersion) {
        int maxResults = devProfile ? 5 : 10;
        // A big maxResults value can cause SocketTimeoutException in JiraIssueTracker.paginateResults method
        SearchCriteria sc = new SearchCriteria.Builder()
                .setRelease(new Release(fixVersion.trim()))
                .setProduct("JBEAP")
                .setMaxResults(maxResults)
                .build();
        return aphrodite.searchIssues(sc);
    }
}
