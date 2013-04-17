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
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.jboss.logging.Logger;
import org.jboss.overview.model.OverviewData;

import com.redhat.engineering.bugzilla.Bug;
import com.redhat.engineering.bugzilla.BugzillaClient;
import com.redhat.engineering.bugzilla.BugzillaClientImpl;

public class Helper {

    private static final Logger LOGGER = Logger.getLogger(Helper.class);

    private static String GITHUB_ORGANIZATION;
    private static String GITHUB_EAP_REPO;
    private static String GITHUB_AS_REPO;
    private static String GITHUB_TOKEN;
    private static String GITHUB_PULL_REQUEST_STATE;

    // regular expressions to extract information
    private static final Pattern BUGZILLAIDPATTERN = Pattern.compile("bugzilla\\.redhat\\.com/show_bug\\.cgi\\?id=(\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern UPSTREAMPATTERN = Pattern.compile("github\\.com/jbossas/jboss-as/pull/(\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BUILDOUTCOME = Pattern.compile(
            "Build (\\d+) outcome was (SUCCESS|FAILURE|ABORTED) using a merge of ([a-z0-9]+) on branch (.+):",
            Pattern.CASE_INSENSITIVE);

    public static String EAP_TARGET_VERSION;

    private static final String BUGZILLA_BASE = "https://bugzilla.redhat.com/xmlrpc.cgi";
    private static String BUGZILLA_LOGIN;
    private static String BUGZILLA_PASSWORD;

    private static GitHubClient gitHubClient;
    private static IRepositoryIdProvider repository_eap;
    private static IRepositoryIdProvider repository_as;
    private static PullRequestService pullRequestService;
    private static IssueService issueService;
    private static BugzillaClient bugzillaClient;

    static {
        Properties props;
        try {
            props = Util.loadProperties();

            GITHUB_ORGANIZATION = Util.require(props, "github.organization");
            GITHUB_EAP_REPO = Util.require(props, "github.eap.repo");
            GITHUB_AS_REPO = Util.require(props, "github.as.repo");
            GITHUB_TOKEN = Util.get(props, "github.token");
            GITHUB_PULL_REQUEST_STATE = Util.require(props, "github.pullrequest.state");

            BUGZILLA_LOGIN = Util.require(props, "bugzilla.login");
            BUGZILLA_PASSWORD = Util.require(props, "bugzilla.password");

            EAP_TARGET_VERSION = Util.require(props, "eap.target.version");

            // initialize github client and services
            gitHubClient = new GitHubClient();
            if (GITHUB_TOKEN != null && GITHUB_TOKEN.length() > 0)
                gitHubClient.setOAuth2Token(GITHUB_TOKEN);
            repository_eap = RepositoryId.create(GITHUB_ORGANIZATION, GITHUB_EAP_REPO);
            repository_as = RepositoryId.create(GITHUB_ORGANIZATION, GITHUB_AS_REPO);
            pullRequestService = new PullRequestService(gitHubClient);
            issueService = new IssueService(gitHubClient);

            // initialize bugzilla client
            bugzillaClient = new BugzillaClientImpl(BUGZILLA_BASE, BUGZILLA_LOGIN, BUGZILLA_PASSWORD);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public static List<OverviewData> getOverviewData() throws IOException {
        long startTime = System.currentTimeMillis();

        List<OverviewData> pullRequestsDatas = new ArrayList<OverviewData>();

        List<PullRequest> pullRequests = pullRequestService.getPullRequests(repository_eap, GITHUB_PULL_REQUEST_STATE);

        for (PullRequest pullRequest : pullRequests) {
            PullRequest upStreamPullRequest = null;
            BuildResult buildResult;
            Bug bug = null;

            buildResult = checkBuildResult(pullRequest);

            String body = pullRequest.getBody();
            Integer upStreamId = checkUpStreamPullRequestId(body);
            Integer bugId = checkBugzillaId(body);

            if (upStreamId != null) {
                upStreamPullRequest = pullRequestService.getPullRequest(repository_as, upStreamId);
            }

            if (bugId != null) {
                try {
                    bug = bugzillaClient.getBug(bugId);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
            OverviewData pullRequestData = new OverviewData(pullRequest, buildResult, upStreamPullRequest, bug);
            pullRequestsDatas.add(pullRequestData);
        }
        LOGGER.info("elapsed time of retrieve information : " + (System.currentTimeMillis() - startTime));

        return pullRequestsDatas;
    }

    public static BuildResult checkBuildResult(PullRequest pullRequest) {
        BuildResult buildResult = BuildResult.UNKNOWN;
        List<Comment> comments;
        try {
            comments = issueService.getComments(repository_eap, pullRequest.getNumber());
        } catch (IOException e) {
            LOGGER.error("Error to get comments for pull request : " + pullRequest.getNumber());
            e.printStackTrace(System.err);
            return buildResult;
        }
        if (comments.size() != 0) {
            for (Comment comment : comments) {
                Matcher matcher = BUILDOUTCOME.matcher(comment.getBody());
                while (matcher.find()) {
                    buildResult = BuildResult.valueOf(matcher.group(2));
                }
            }
        }
        return buildResult;
    }

    public static OverviewData getOverviewData(PullRequest pullRequest) {
        PullRequest upStreamPullRequest = null;
        BuildResult buildResult;
        Bug bug = null;

        buildResult = checkBuildResult(pullRequest);

        String body = pullRequest.getBody();
        Integer upStreamId = checkUpStreamPullRequestId(body);
        Integer bugId = checkBugzillaId(body);

        if (bugId != null) {
            try {
                bug = bugzillaClient.getBug(bugId);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }

        if (upStreamId != null) {
            try {
                upStreamPullRequest = pullRequestService.getPullRequest(repository_as, upStreamId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new OverviewData(pullRequest, buildResult, upStreamPullRequest, bug);
    }

    private static Integer checkBugzillaId(String body) {
        Matcher matcher = BUGZILLAIDPATTERN.matcher(body);
        while (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private static Integer checkUpStreamPullRequestId(String body) {
        Matcher matcher = UPSTREAMPATTERN.matcher(body);
        while (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    public static List<PullRequest> getPullRequests() {
        List<PullRequest> pullRequests = null;
        try {
            pullRequests = pullRequestService.getPullRequests(repository_eap, GITHUB_PULL_REQUEST_STATE);
        } catch (IOException e) {
            LOGGER.error("Unable to retrieve pull requests from repository");
            e.printStackTrace();
        }
        return pullRequests;
    }
}
