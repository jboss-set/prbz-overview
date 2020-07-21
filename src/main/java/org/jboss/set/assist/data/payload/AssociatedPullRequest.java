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

package org.jboss.set.assist.data.payload;

import java.net.URL;

/**
 * @author wangc
 *
 */
public class AssociatedPullRequest {

    private String label;
    private URL link;
    private String codebase;
    private String patchState;
    private String commitStatus;
    private boolean noUpstreamRequired;
    private URL upstreamIssueFromPRDesc;
    private URL upstreamPullRequestFromPRDesc;

    public AssociatedPullRequest(String label, URL link, String codebase, String patchState, String commitStatus, boolean noUpstreamRequired, URL upstreamIssueFromPRDesc, URL upstreamPullRequestFromPRDesc) {
        this.label = label;
        this.link = link;
        this.codebase = codebase;
        this.patchState = patchState;
        this.commitStatus = commitStatus;
        this.noUpstreamRequired = noUpstreamRequired;
        this.upstreamIssueFromPRDesc = upstreamIssueFromPRDesc;
        this.upstreamPullRequestFromPRDesc = upstreamPullRequestFromPRDesc;
    }

    public AssociatedPullRequest(String label, URL link, String codebase, String patchState, String commitStatus, boolean noUpstreamRequired) {
        this(label, link, codebase, patchState, commitStatus, noUpstreamRequired, null, null);
    }

    public String getLabel() {
        return label;
    }

    public URL getLink() {
        return link;
    }

    public String getCodebase() {
        return codebase;
    }

    public String getPatchState() {
        return patchState;
    }

    public String getCommitStatus() {
        return commitStatus;
    }

    public boolean isNoUpstreamRequired() {
        return noUpstreamRequired;
    }

    public URL getUpstreamIssueFromPRDesc() {
        return upstreamIssueFromPRDesc;
    }

    public URL getUpstreamPullRequestFromPRDesc() {
        return upstreamPullRequestFromPRDesc;
    }
}
