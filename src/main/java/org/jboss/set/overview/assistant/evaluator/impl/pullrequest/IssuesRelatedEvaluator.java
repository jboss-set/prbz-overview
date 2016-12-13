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

package org.jboss.set.overview.assistant.evaluator.impl.pullrequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.overview.Constants;
import org.jboss.set.overview.assistant.data.IssueData;
import org.jboss.set.overview.assistant.evaluator.PullRequestEvaluator;
import org.jboss.set.overview.assistant.evaluator.PullRequestEvaluatorContext;
import org.jboss.set.overview.assistant.evaluator.Utils;

/**
 * @author egonzalez
 *
 */
public class IssuesRelatedEvaluator implements PullRequestEvaluator {

    @Override
    public String name() {
        return "Issues Related evaluator";
    }

    @Override
    public void eval(PullRequestEvaluatorContext context, Map<String, Object> data) {
        Set<Issue> issues = context.getIssues();
        Map<String, List<String>> issueStream = new HashMap<>();

        for (Issue issue : issues) {
            List<String> streams = Utils.getStreams(issue);
            issueStream.put(issue.getTrackerId().get(), streams);
        }

        Stream currentStream = context.getStream();
        Aphrodite aphrodite = context.getAphrodite();
        Patch patch = context.getPatch();
        List<String> streams = aphrodite.getStreamsBy(patch.getRepository(), patch.getCodebase()).stream().map(e -> e.getName())
                .collect(Collectors.toList());

        if (currentStream.getName().contains("eap-6")) {
            data.put("issuesRelated", issues.stream()
                    .filter(e -> {
                        List<String> intersect = new ArrayList<>(streams);
                        intersect.retainAll(issueStream.get(e.getTrackerId().get()));
                        return !intersect.isEmpty();
                    })
                    .map(e -> new IssueData(e.getTrackerId().get(), issueStream.get(e.getTrackerId().get()), e.getURL(),
                            e.getStatus(), e.getType(), e.getStage().getStateMap().entrySet().stream().collect(
                                    Collectors.toMap(e1 -> String.valueOf(e1.getKey()), e1 -> String.valueOf(e1.getValue()))),e.getStreamStatus()))
                    .collect(Collectors.toList()));

            data.put("issuesOtherStreams", issues.stream()
                    .filter(e -> {
                        List<String> intersect = new ArrayList<>(streams);
                        intersect.retainAll(issueStream.get(e.getTrackerId().get()));
                        return intersect.isEmpty();
                    })
                    .map(e -> new IssueData(e.getTrackerId().get(), issueStream.get(e.getTrackerId().get()), e.getURL(),
                            e.getStatus(), e.getType(), e.getStage().getStateMap().entrySet().stream().collect(
                                    Collectors.toMap(e1 -> String.valueOf(e1.getKey()), e1 -> String.valueOf(e1.getValue()))), e.getStreamStatus()))
                    .collect(Collectors.toList()));
        } else if (currentStream.getName().contains("eap-7")) {
            data.put("issuesRelated", issues.stream()
                    .filter(e -> e.getStreamStatus().keySet().stream().filter(e1 -> checkStream(currentStream, e1)).findAny()
                            .isPresent())
                    .map(e -> new IssueData(e.getTrackerId().get(), issueStream.get(e.getTrackerId().get()), e.getURL(),
                            e.getStatus(), e.getType(),
                            e.getStage().getStateMap().entrySet().stream().collect(
                                    Collectors.toMap(e1 -> String.valueOf(e1.getKey()), e1 -> String.valueOf(e1.getValue()))), e.getStreamStatus()))
                    .collect(Collectors.toList()));

            data.put("issuesOtherStreams", issues.stream()
                    .filter(e -> e.getStreamStatus().keySet().stream().filter(e1 -> !checkStream(currentStream, e1)).findAny()
                            .isPresent())
                    .map(e -> new IssueData(e.getTrackerId().get(), issueStream.get(e.getTrackerId().get()), e.getURL(),
                            e.getStatus(), e.getType(),
                            e.getStage().getStateMap().entrySet().stream().collect(
                                    Collectors.toMap(e1 -> String.valueOf(e1.getKey()), e1 -> String.valueOf(e1.getValue()))), e.getStreamStatus()))
                    .collect(Collectors.toList()));
        } else {
            // wildfly stream
            data.put("issuesRelated", issues.stream()
                    .map(e -> new IssueData(e.getTrackerId().get(), issueStream.get(e.getTrackerId().get()), e.getURL(),
                            e.getStatus(), e.getType(), e.getStage().getStateMap().entrySet().stream().collect(
                                    Collectors.toMap(e1 -> String.valueOf(e1.getKey()), e1 -> String.valueOf(e1.getValue()))), e.getStreamStatus()))
                    .collect(Collectors.toList()));

            data.put("issuesOtherStreams", Collections.emptyList());
        }
    }

    // workaround! it depends on streams.json and target release in jira, can be changed easily before process is steady.
    private boolean checkStream(Stream currentStream, String targetRelease) {
        String currentStreamName = currentStream.getName();
        if (currentStreamName.equals(Constants.EAP70ZSTREAM)) {
            return targetRelease.equals(Constants.EAP7_STREAM_TARGET_RELEASE_70ZGA) || targetRelease.equals(Constants.EAP7_STREAM_TARGET_RELEASE_7BACKLOGGA);
        } else if (currentStreamName.equals(Constants.EAP7Z0STREAM)) {
            return targetRelease.equals(Constants.EAP7_STREAM_TARGET_RELEASE_710GA);
        } else {
            return false;
        }
    }
}