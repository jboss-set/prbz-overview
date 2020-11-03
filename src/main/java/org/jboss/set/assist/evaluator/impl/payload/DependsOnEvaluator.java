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

package org.jboss.set.assist.evaluator.impl.payload;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.naming.NameNotFoundException;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.config.TrackerType;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueStatus;
import org.jboss.set.aphrodite.domain.IssueType;
import org.jboss.set.aphrodite.domain.Release;
import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.aphrodite.issue.trackers.jira.JiraIssue;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.assist.Constants;
import org.jboss.set.assist.data.payload.DependsOnIssue;
import org.jboss.set.assist.evaluator.PayloadEvaluator;
import org.jboss.set.assist.evaluator.PayloadEvaluatorContext;
import static org.jboss.set.assist.Constants.PATTERN_SUFFIX;

/**
 * @author wangc
 *
 */
public class DependsOnEvaluator implements PayloadEvaluator {

    private static final Logger logger = Logger.getLogger(DependsOnEvaluator.class.getCanonicalName());

    private static final String KEY = "dependsOn";

    @Override
    public String name() {
        return "Depends On Evaluator";
    }

    @Override
    public void eval(PayloadEvaluatorContext context, Map<String, Object> data) {
        Aphrodite aphrodite = context.getAphrodite();
        Issue dependencyIssue = context.getIssue();
        Stream stream = context.getStream();
        java.util.stream.Stream<Issue> upstreamReferences = java.util.stream.Stream.empty();
        try {
            if (dependencyIssue instanceof JiraIssue) {
                upstreamReferences = dependencyIssue.getUpstreamReferences();
            } else {
                // workaround, only JiraIssueHomeImpl implements the upstream reference logic, Bugzilla uses simple getDependsOn() for now.
                Set<Issue> upstreamIssues = new HashSet<>();
                for (URL url : dependencyIssue.getDependsOn()) {
                    try {
                        upstreamIssues.add(aphrodite.getIssue(url));
                    } catch (NotFoundException e) {
                        logger.log(Level.WARNING, "Unable to find issue with url: " + url, e);
                    }
                }
                if (!upstreamIssues.isEmpty())
                    upstreamReferences = upstreamIssues.stream();
            }
        } catch (NameNotFoundException e) {
            logger.log(Level.SEVERE, "Unable to load service due to " + e.getMessage(), e);
        }

        List<DependsOnIssue> dependsOnIssues = new ArrayList<>();

        // TODO complete violations
        if (context.getTrackerType().equals(TrackerType.BUGZILLA)) {
            Issue payloadTracker = context.getPayloadTracker();
            upstreamReferences.forEach(issue -> {
                boolean inPayload = issue.getBlocks().stream().anyMatch(e -> extractId(e).equalsIgnoreCase(payloadTracker.getTrackerId().get())) || checkIsReleased(issue) || checkIssueType(issue) || !matchStream(issue, stream);
                dependsOnIssues.add(new DependsOnIssue(issue.getURL(), issue.getTrackerId().orElse("N/A"),
                        issue.getSummary().orElse(Constants.NOTAPPLICABLE), issue.getStatus(),
                        issue.getRawStatus(), issue.getType(), issue.getRawType(),
                        issue.getStage().getStateMap().entrySet().stream()
                                .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue()))),
                        issue.getPriority().name(),
                        Collections.emptyList(), inPayload, getFixVersions(issue), getPayload(issue),
                        issue.getStreamStatus()));
            });
        } else {
            upstreamReferences.forEach(issue -> {
                String fixVersion = context.getFixVersion();
                boolean inPayload = checkFixVersion(issue, fixVersion) || checkIsReleased(issue) || checkIssueType(issue) || !matchStream(issue, stream);
                dependsOnIssues.add(new DependsOnIssue(issue.getURL(), issue.getTrackerId().orElse("N/A"),
                        issue.getSummary().orElse(Constants.NOTAPPLICABLE), issue.getStatus(),
                        issue.getRawStatus(), issue.getType(), issue.getRawType(),
                        issue.getStage().getStateMap().entrySet().stream()
                                .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue()))),
                        issue.getPriority().name(),
                        Collections.emptyList(), inPayload, getFixVersions(issue), getPayload(issue),
                        issue.getStreamStatus()));
            });
        }
        data.put(KEY, dependsOnIssues);
    }

    // ancient depends on issue does not need to be included in payload.
    private boolean checkIsReleased(Issue issue) {
        IssueStatus issueStatus = issue.getStatus();
        if (issueStatus.equals(IssueStatus.VERIFIED) || issueStatus.equals(IssueStatus.CLOSED)
                || issueStatus.equals(IssueStatus.RELEASE_PENDING))
            return true;
        else
            return false;
    }

    // one-off does not need to be included in payload
    private boolean checkIssueType(Issue issue) {
        return issue.getType().equals(IssueType.ONE_OFF);
    }

    // check whether depend on issue with same stream is missed in payload
    private boolean matchStream(Issue issue, Stream stream) {
        return issue.getStreamStatus().keySet().stream().anyMatch(e -> e.equals(stream.getName()));
    }

    private boolean checkFixVersion(Issue issue, String fixVersion) {
        List<Release> releases = issue.getReleases();
        return releases.stream().anyMatch(e -> {
            if (e.getVersion().isPresent()) {
                return e.getVersion().get().equalsIgnoreCase(fixVersion);
            } else {
                return false;
            }
        });
    }

    private String extractId(URL url) {
        Optional<String> str = Optional.empty();
        try {
            if (url.toString().toLowerCase().contains("bugzilla")) {

                str = Optional.ofNullable(Utils.getParamaterFromUrl(Constants.BZ_ID_PARAM_PATTERN, url));

            } else {
                str = Optional.ofNullable(getIssueKey(url));
            }
        } catch (NotFoundException e) {

        }
        return str.orElse("default");
    }

    private String getIssueKey(URL url) throws NotFoundException {
        String path = url.getPath();
        boolean api = path.contains(Constants.API_ISSUE_PATH);
        boolean browse = path.contains(Constants.BROWSE_ISSUE_PATH);
        if (!(api || browse))
            throw new NotFoundException("The URL path must be of the form '" + Constants.API_ISSUE_PATH + "' OR '" + Constants.BROWSE_ISSUE_PATH + "'");
        return api ? path.substring(Constants.API_ISSUE_PATH.length()) : path.substring(Constants.BROWSE_ISSUE_PATH.length());
    }

    private List<String> getFixVersions(Issue issue){
        return issue.getReleases().stream().filter(e -> e.getVersion().isPresent()).map(e -> e.getVersion().get()).collect(Collectors.toList());
    }

    private String getPayload(Issue issue) {
        List<String> fixVersions = getFixVersions(issue);
        Optional<String> payload = fixVersions.stream()
                .filter(e -> (e.matches(Constants.EAP64ZPAYLOADPATTERN.toString())
                        || e.matches("7\\.[0-9]\\." + PATTERN_SUFFIX)))
                .findFirst();
        return payload.orElse(Constants.NOTAPPLICABLE);
    }
}
