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
import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.egit.github.core.PullRequest;
import org.jboss.logging.Logger;
import org.jboss.overview.BuildResult;
import org.jboss.overview.Helper;

import com.redhat.engineering.bugzilla.Bug;
import com.redhat.engineering.bugzilla.Flag;

public class OverviewData implements Serializable {

    private static Logger LOGGER = Logger.getLogger(OverviewData.class);

    private static final long serialVersionUID = -2423691441325004516L;
    private PullRequest pullRequest;
    private PullRequest pullRequestUpStream; // upstream pull request if provided
    private BuildResult buildResult;
    private Bug bug; // bugzilla bug if provided
    private StringBuffer overallState;
    private String mergeable = "Not Mergeable";

    public OverviewData(PullRequest pullRequest) {
        this(pullRequest, null, null, null);
    }

    public OverviewData(PullRequest pullRequest, BuildResult buildResult, PullRequest pullRequestUpStream, Bug bug) {
        this.pullRequest = pullRequest;
        this.buildResult = buildResult;
        this.pullRequestUpStream = pullRequestUpStream;
        this.bug = bug;
    }

    @PostConstruct
    public void postContruct() {
    }

    public PullRequest getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

    public PullRequest getPullRequestUpStream() {
        return pullRequestUpStream;
    }

    public void setPullRequestUpStream(PullRequest pullRequestUpStream) {
        this.pullRequestUpStream = pullRequestUpStream;
    }

    public BuildResult getBuildResult() {
        return buildResult;
    }

    public void setBuildResult(BuildResult buildResult) {
        this.buildResult = buildResult;
    }

    public Bug getBug() {
        return bug;
    }

    public void setBug(Bug bug) {
        this.bug = bug;
    }

    public StringBuffer getOverallState() {

        overallState = new StringBuffer();

        // do we have a positive build result of lightning ?
        overallState.append((buildResult.equals(BuildResult.SUCCESS)) ? " + Lightning build result is : " + buildResult
                + "<br><br>" : " - Lightning build result is : " + buildResult + "<br><br>");
        // do we have a resolved upstream issue ?
        overallState.append((pullRequestUpStream != null) ? " + Upstream pull request is provided : "
                + pullRequestUpStream.getNumber() + "<br><br>" : " - Upstream pull request might needed! <br><br>");
        // does the BZ have qa_ack and pm_ack set to + ?
        if (bug != null) {
            List<Flag> flags = bug.getFlags();
            if (flags.size() > 0) {
                for (Flag flag : flags) {
                    if (flag.getName().equals(Helper.EAP_TARGET_VERSION)) {
                        if (flag.getStatus() == null) {
                            overallState.append(" - " + Helper.EAP_TARGET_VERSION + " is unset <br><br>");
                            continue;
                        }

                        switch (flag.getStatus().charAt(0)) {
                            case '+':
                                overallState.append(" + " + Helper.EAP_TARGET_VERSION + " is + <br><br>");
                                break;
                            case '-':
                                overallState.append(" - " + Helper.EAP_TARGET_VERSION + " is - <br><br>");
                                break;
                            case '?':
                                overallState.append(" - " + Helper.EAP_TARGET_VERSION + " is ? <br><br>");
                                break;
                            default:
                                break;
                        }
                    }

                    if (flag.getName().equals("pm_ack")) {
                        if (flag.getStatus() == null) {
                            overallState.append(" - pm_ack is unset <br><br>");
                            continue;
                        }

                        switch (flag.getStatus().charAt(0)) {
                            case '+':
                                overallState.append(" + pm_ack is + <br><br>");
                                break;
                            case '-':
                                overallState.append(" - pm_ack is - <br><br>");
                                break;
                            case '?':
                                overallState.append(" - pm_ack is ? <br><br>");
                                break;
                            default:
                                break;
                        }
                    }
                    if (flag.getName().equals("qa_ack")) {
                        if (flag.getStatus() == null) {
                            overallState.append(" - qa_ack is unset <br><br>");
                            continue;
                        }
                        switch (flag.getStatus().charAt(0)) {
                            case '+':
                                overallState.append(" + qa_ack is + <br><br>");
                                break;
                            case '-':
                                overallState.append(" - qa_ack is - <br><br>");
                                break;
                            case '?':
                                overallState.append(" - qa_ack is ? <br><br>");
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        } else
            overallState.append(" - Bugzilla issue might needed! <br><br>");
        return overallState;
    }

    public void setOverallState(StringBuffer overallState) {
        this.overallState = overallState;
    }

    /**
     * If lightning build result is SUCCESS, upstream pull request is provided and bugzilla bug is provided For bugziila,
     * eap.target.version, pm_ack and qa_ack flags must be positive Then pull request is mergeable
     *
     * @return
     */
    public String getMergeable() {
        if (buildResult.equals(BuildResult.SUCCESS) && pullRequestUpStream != null && bug != null) {
            List<Flag> flags = bug.getFlags();
            for (Flag flag : flags) {
                if (flag.getName().equals(Helper.EAP_TARGET_VERSION)) {
                    if (flag.getStatus() == null)
                        return mergeable;
                    else if (!flag.getStatus().contains("+"))
                        return mergeable;
                }
                if (flag.getName().equals("pm_ack")) {
                    if (flag.getStatus() == null)
                        return mergeable;
                    else if (!flag.getStatus().contains("+"))
                        return mergeable;
                }
                if (flag.getName().equals("qa_ack")) {
                    if (flag.getStatus() == null)
                        return mergeable;
                    else if (!flag.getStatus().contains("+"))
                        return mergeable;
                }
            }
            mergeable = "Mergeable";
        }
        return mergeable;
    }

    public void setMergeable(String mergeable) {
        this.mergeable = mergeable;
    }
}
