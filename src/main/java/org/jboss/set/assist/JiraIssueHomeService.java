/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.set.assist;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.common.Utils;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.PatchState;
import org.jboss.set.aphrodite.domain.PatchType;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.issue.trackers.jira.JiraIssue;
import org.jboss.set.aphrodite.issue.trackers.jira.JiraPatchHomeImpl;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.assist.data.payload.FailedPullRequest;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class JiraIssueHomeService extends JiraPatchHomeImpl {
    private static final Log logger = LogFactory.getLog(JiraIssueHomeService.class);

    @Override
    public java.util.stream.Stream<Patch> findPatchesByIssue(Issue issue) {
        List<URL> urls = ((JiraIssue) issue).getPullRequests();
        return mapURLtoPatchStream(urls);
    }

    private java.util.stream.Stream<Patch> mapURLtoPatchStream(List<URL> urls) {
        List<Patch> list = urls.stream().map(e -> {
            PatchType patchType = getPatchType(e);
            PatchState patchState = getPatchState(e, patchType);
            return new Patch(e, patchType, patchState);
        }).collect(Collectors.toList());
        return list.stream();
    }

    private PatchType getPatchType(URL url) {
        String urlStr = url.toString();
        if (GitUtil.isValidPRUrl(urlStr))
            return PatchType.PULLREQUEST;
        else if (urlStr.contains("/commit/"))
            return PatchType.COMMIT;
        else
            logger.info("patch " + url);
            return PatchType.FILE;
    }

    private PatchState getPatchState(URL url, PatchType patchType) {
        if (patchType.equals(PatchType.PULLREQUEST)) {
            try {
                PullRequest pullRequest = GitUtil.getPullRequest(Aphrodite.instance(), url);
                if (!(pullRequest instanceof FailedPullRequest) && pullRequest.getState() != null) {
                    return PatchState.valueOf(pullRequest.getState().toString());
                }
            } catch (AphroditeException e) {
                Utils.logException(logger, e);
            }
        } else if (patchType.equals(PatchType.COMMIT)) {
            return PatchState.CLOSED;
        }
        return PatchState.UNDEFINED;
    }

}