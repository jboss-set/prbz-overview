/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.overview;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.PullRequest;
import org.jboss.logging.Logger;
import org.jboss.overview.model.OverviewData;
import org.jboss.pull.shared.Bug;
import org.jboss.pull.shared.BuildResult;
import org.jboss.pull.shared.PullHelper;

public class Aider {

    private static final Logger LOGGER = Logger.getLogger(Aider.class);

    public static PullHelper helper;

    static {
        try {
            helper = new PullHelper("processor.properties.file", "/home/wangchao/work/jbossas/pull/processor.properties");
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public static List<OverviewData> getOverviewData() {
        long startTime = System.currentTimeMillis();

        List<OverviewData> pullRequestsDatas = new ArrayList<OverviewData>();

        List<PullRequest> pullRequests = new ArrayList<PullRequest>();
        try {
            pullRequests = helper.getPullRequestService().getPullRequests(helper.getRepositoryEAP(), "open");
        } catch (IOException e) {
            LOGGER.error("Can not retrieve pull requests on repository : " + helper.getRepositoryEAP());
            e.printStackTrace(System.err);
        }

        for (PullRequest pullRequest : pullRequests) {
            List<PullRequest> upStreamPullRequests = new ArrayList<PullRequest>();
            BuildResult buildResult;
            List<Bug> bugs = new ArrayList<Bug>();

            buildResult = Aider.helper.checkBuildResult(pullRequest);

            String body = pullRequest.getBody();
            List<Integer> upStreamIds = helper.checkUpStreamPullRequestId(body);

            for (Integer id : upStreamIds) {
                try {
                    upStreamPullRequests.add(helper.getPullRequestService().getPullRequest(helper.getRepositoryAS(), id));
                } catch (IOException e) {
                    LOGGER.error("Can not retrieve upstream pull request number : " + id);
                    e.printStackTrace(System.err);
                }
            }

            bugs = helper.getBug(pullRequest);

            boolean mergeable = helper.isMergeable(pullRequest);

            List<String> overallState = makeOverallState(pullRequest);

            OverviewData pullRequestData = new OverviewData(pullRequest, buildResult, upStreamPullRequests, bugs, overallState, mergeable);
            pullRequestsDatas.add(pullRequestData);
        }
        LOGGER.info("elapsed time of retrieve information : " + (System.currentTimeMillis() - startTime));

        return pullRequestsDatas;
    }

    public static OverviewData getOverviewData(PullRequest pullRequest) {
        List<PullRequest> upStreamPullRequests = new ArrayList<PullRequest>();
        BuildResult buildResult;
        List<Bug> bugs = new ArrayList<Bug>();

        buildResult = Aider.helper.checkBuildResult(pullRequest);

        String body = pullRequest.getBody();

        List<Integer> upStreamIds = helper.checkUpStreamPullRequestId(body);

        for (Integer id : upStreamIds) {
            try {
                upStreamPullRequests.add(helper.getPullRequestService().getPullRequest(helper.getRepositoryAS(), id));
            } catch (IOException e) {
                LOGGER.error("Can not retrieve upstream pull request number : " + id);
                e.printStackTrace(System.err);
            }
        }

        bugs = helper.getBug(pullRequest);

        boolean mergeable = helper.isMergeable(pullRequest);

        List<String> overallState = makeOverallState(pullRequest);

        return new OverviewData(pullRequest, buildResult, upStreamPullRequests, bugs, overallState, mergeable);
    }

    public static List<String> makeOverallState(PullRequest pullRequest) {
        List<String> overallState = new ArrayList<String>();

        // do we have a positive build result of lightning ?
        overallState.add((helper.checkBuildResult(pullRequest).equals(BuildResult.SUCCESS)) ? " + Lightning build result is SUCCESS" : " - Lightning build result is : NOT SUCCESS");

        // do we have a resolved upstream issue ?
        overallState.add(helper.isMergeableByUpstream(pullRequest) ? " + Mergeable by upstream" : " - Not mergeable by upstream");

        // do we have a bugzilla issue ?
        overallState.add(helper.isMergeableByBugzilla(pullRequest, null) ? " + Mergeable by bugzilla" : " - Not mergeable by bugzilla");

        return overallState;
    }
}
