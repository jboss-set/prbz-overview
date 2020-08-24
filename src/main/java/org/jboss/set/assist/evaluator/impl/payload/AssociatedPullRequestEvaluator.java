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

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.CommitStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.PatchType;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.assist.PatchHomeService;
import org.jboss.set.assist.data.payload.AssociatedPullRequest;
import org.jboss.set.assist.evaluator.PayloadEvaluator;
import org.jboss.set.assist.evaluator.PayloadEvaluatorContext;

import javax.naming.NameNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author wangc
 *
 */
public class AssociatedPullRequestEvaluator implements PayloadEvaluator {
    private static final Logger logger = Logger.getLogger(AssociatedPullRequestEvaluator.class.getCanonicalName());
    private static final String KEY = "associatedPullRequest";
    private static final String KEY_UNRELATED = "associatedUnrelatedPullRequest";

    @Override
    public String name() {
        return "Associate PullRequest Evaluator";
    }

    @Override
    public void eval(PayloadEvaluatorContext context, Map<String, Object> data) {
        Issue dependencyIssue = context.getIssue();

        java.util.stream.Stream<Patch> allPatches = java.util.stream.Stream.empty();
        Collection <PullRequest> allPullRequests = Collections.emptyList();
        try {
            allPatches = dependencyIssue.getPatches();
        } catch (NameNotFoundException e) {
            logger.log(Level.SEVERE, "Can not get patch service due to : ", e);
        }

        Aphrodite aphrodite = context.getAphrodite();

        allPullRequests = allPatches.filter(e -> e.getPatchType().equals(PatchType.PULLREQUEST)).map(e -> {
            URL url = e.getUrl();
            try {
                return aphrodite.getPullRequest(url);
            } catch (NotFoundException ex) {
                logger.log(Level.WARNING, "Can not get pull request from url : " + url + " due to : " + ex);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        Stream stream = context.getStream();

        Collection<PullRequest> relatedPullRequests = PatchHomeService.filterRelatedPatch(allPullRequests, stream);

        allPullRequests.removeAll(relatedPullRequests);
        Collection<PullRequest> unrelatedPullRequests = allPullRequests;

        List<AssociatedPullRequest> relatedDataList = new ArrayList<>();
        List<AssociatedPullRequest> unrelatedDataList = new ArrayList<>();

        for (PullRequest pullRequest : relatedPullRequests) {
            CommitStatus commitStatus = PatchHomeService.retrieveCommitStatus(pullRequest);
            URL upstreamIssueFromPRDesc = null;
            URL upstreamPRFromPRDesc = null;
            String upstreamPatchState = null;
            try {
                upstreamIssueFromPRDesc = pullRequest.findUpstreamIssueURL();
            } catch (MalformedURLException e) {
                logger.log(Level.WARNING, "Can not form upstream issue url due to : " + e.getMessage());
            }
            try {
                upstreamPRFromPRDesc = pullRequest.findUpstreamPullRequestURL();
                if (upstreamPRFromPRDesc != null) {
                    upstreamPatchState = PatchHomeService.getPatchState(upstreamPRFromPRDesc).name();
                }
            } catch (MalformedURLException e) {
                logger.log(Level.WARNING, "Can not form upstream pull reuqest url due to : " + e.getMessage());
            }
            relatedDataList.add(new AssociatedPullRequest(pullRequest.getId(), pullRequest.getURL(),
                    pullRequest.getCodebase().getName(), pullRequest.getState().toString(),
                    commitStatus.toString(),
                    pullRequest.getMergableState() == null?null:pullRequest.getMergableState().name(),
                    !pullRequest.isUpstreamRequired(), upstreamIssueFromPRDesc, upstreamPRFromPRDesc, upstreamPatchState));
        }
        data.put(KEY, relatedDataList);

        for (PullRequest pullRequest : unrelatedPullRequests) {
            CommitStatus commitStatus = PatchHomeService.retrieveCommitStatus(pullRequest);
            unrelatedDataList.add(new AssociatedPullRequest(pullRequest.getId(), pullRequest.getURL(),
                    pullRequest.getCodebase().getName(), pullRequest.getState().toString(),
                    commitStatus.toString(),
                    pullRequest.getMergableState() == null ? null : pullRequest.getMergableState().name(),
                    !pullRequest.isUpstreamRequired()));
        }
        data.put(KEY_UNRELATED, unrelatedDataList);
    }
}
