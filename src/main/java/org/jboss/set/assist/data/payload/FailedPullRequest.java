/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

import org.jboss.set.aphrodite.domain.Codebase;
import org.jboss.set.aphrodite.domain.MergeableState;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.PullRequestState;

import java.net.URL;
import java.util.Collections;
import java.util.List;

public class FailedPullRequest extends PullRequest {

    public FailedPullRequest(final URL url, final String reason) {
        super(url.toString().substring(url.toString().lastIndexOf("/") + 1), url, null, new Codebase(""), PullRequestState.UNDEFINED, reason, "",
                false, false, MergeableState.UNKNOWN, null, Collections.EMPTY_LIST, null);
    }

    @Override
    public URL findUpstreamPullRequestURL() {
        return null;
    }

    @Override
    public URL findUpstreamIssueURL() {
        return null;
    }

    @Override
    public URL findIssueURL() {
        return null;
    }

    @Override
    public List<URL> findRelatedIssuesURL() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<URL> findDependencyPullRequestsURL() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean hasUpgrade() {
        return false;
    }
}