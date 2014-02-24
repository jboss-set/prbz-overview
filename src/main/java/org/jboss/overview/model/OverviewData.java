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

package org.jboss.overview.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.egit.github.core.PullRequest;
import org.jboss.pull.shared.BuildResult;
import org.jboss.pull.shared.connectors.common.Issue;
import org.jboss.pull.shared.ProcessorPullState;

/**
 * @author wangchao
 */

public class OverviewData implements Serializable {

    private static final long serialVersionUID = -2423691441325004516L;
    private PullRequest pullRequest;
    private List<PullRequest> pullRequestUpStreams; // upstream pull request if provided
    private BuildResult buildResult;
    private List<? extends Issue> issues; // bugzilla or jira bug if provided
    private List<String> overallState;
    private boolean mergeable = false;
    private boolean isReviewed = false;
    private ProcessorPullState state = ProcessorPullState.NEW;

    public OverviewData(PullRequest pullRequest) {
        this(pullRequest, null, null, null, null, false, false, ProcessorPullState.NEW);
    }

    public OverviewData(PullRequest pullRequest, BuildResult buildResult, List<PullRequest> pullRequestUpStreams, List<? extends Issue> issues, List<String> overallState, boolean mergeable, boolean isReviewed, ProcessorPullState state) {
        this.pullRequest = pullRequest;
        this.buildResult = buildResult;
        this.pullRequestUpStreams = pullRequestUpStreams;
        this.issues = issues;
        this.overallState = overallState;
        this.mergeable = mergeable;
        this.isReviewed = isReviewed;
        this.state = state;
    }

    @PostConstruct
    public void postContruct() throws Exception {
    }

    public PullRequest getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

    public List<PullRequest> getPullRequestUpStreams() {
        return pullRequestUpStreams;
    }

    public void setPullRequestUpStreams(ArrayList<PullRequest> pullRequestUpStreams) {
        this.pullRequestUpStreams = pullRequestUpStreams;
    }

    public BuildResult getBuildResult() {
        return buildResult;
    }

    public void setBuildResult(BuildResult buildResult) {
        this.buildResult = buildResult;
    }

    public List<? extends Issue> getIssues() {
        return issues;
    }

    public void setIssues(List<? extends Issue> issues) {
        this.issues = issues;
    }

    public List<String> getOverallState() {
        return overallState;
    }

    public void setOverallState(List<String> overallState) {
        this.overallState = overallState;
    }

    public boolean isMergeable() {
        return mergeable;
    }

    public void setMergeable(boolean mergeable) {
        this.mergeable = mergeable;
    }

    public boolean isReviewed() {
        return isReviewed;
    }

    public ProcessorPullState getState() {
        return state;
    }
}
