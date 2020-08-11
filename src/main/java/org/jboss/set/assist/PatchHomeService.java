/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

import static org.jboss.set.assist.Util.convertURLtoURI;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.naming.NameNotFoundException;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.config.TrackerType;
import org.jboss.set.aphrodite.domain.Codebase;
import org.jboss.set.aphrodite.domain.CommitStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.PatchState;
import org.jboss.set.aphrodite.domain.PatchType;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.aphrodite.domain.spi.PatchHome;
import org.jboss.set.aphrodite.issue.trackers.jira.JiraIssue;
import org.jboss.set.aphrodite.simplecontainer.SimpleContainer;
import org.jboss.set.aphrodite.spi.NotFoundException;

/**
 * @author wangc
 *
 */
public class PatchHomeService implements PatchHome {
    private static final Logger logger = Logger.getLogger(PatchHomeService.class.getCanonicalName());
    private static Aphrodite aphrodite;

    static {
        try {
            aphrodite = SimpleContainer.instance().lookup(Aphrodite.class.getSimpleName(), Aphrodite.class);
        } catch (NameNotFoundException e1) {
            logger.log(Level.SEVERE, "Can not get aphrodite service due to : ", e1);
        }
    }

    @Override
    public java.util.stream.Stream<Patch> findPatchesByIssue(Issue issue) {
        if (issue.getTrackerType().equals(TrackerType.JIRA)) {
            List<URL> urls = ((JiraIssue) issue).getPullRequests();
            return mapURLtoPatchStream(urls);
        } else {
            List<URL> urls = new ArrayList<>();
            issue.getComments().stream().forEach(e -> extractPullRequests(urls, e.getBody()));
            return mapURLtoPatchStream(urls);
        }
    }

    private java.util.stream.Stream<Patch> mapURLtoPatchStream(List<URL> urls) {
        return urls.stream().map(e -> {
            PatchType patchType = getPatchType(e);
            PatchState patchState = getPatchState(e, patchType);
            return new Patch(e, getPatchType(e), patchState);
        });
    }

    private static PatchType getPatchType(URL url) {
        String urlStr = url.toString();
        if (urlStr.contains("/pull/"))
            return PatchType.PULLREQUEST;
        else if (urlStr.contains("/commit/"))
            return PatchType.COMMIT;
        else
            return PatchType.FILE;
    }

    private static PatchState getPatchState(URL url, PatchType patchType) {
        if (patchType.equals(PatchType.PULLREQUEST)) {
            try {
                PullRequest pullRequest = aphrodite.getPullRequest(url);
                return PatchState.valueOf(pullRequest.getState().toString());
            } catch (NotFoundException e) {
                logger.log(Level.WARNING, "Unable to find pull request with url: " + url, e);
            }
        } else if (patchType.equals(PatchType.COMMIT)) {
            return PatchState.CLOSED;
        }
        return PatchState.UNDEFINED;
    }

    private void extractPullRequests(List<URL> pullRequests, String messageBody) {
        Matcher matcher = Constants.RELATED_PR_PATTERN.matcher(messageBody);
        while (matcher.find()) {
            if (matcher.groupCount() == 3) {
                String urlStr = "https://github.com/" + matcher.group(1) + "/" + matcher.group(2) + "/pull/" + matcher.group(3);
                try {
                    URL url = new URL(urlStr);
                    pullRequests.add(url);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("Invalid URL:" + urlStr, e);
                }
            }
        }
    }

    public static Collection<PullRequest> filterRelatedPatch(Collection<PullRequest> pullRequest, Stream stream) {
        return pullRequest.stream().filter(e -> filterByStream(e, stream)).collect(Collectors.toSet());
    }

    public static boolean filterByStream(PullRequest pullRequest, Stream stream) {
        Codebase codebase = pullRequest.getCodebase();
        Repository repository = pullRequest.getRepository();
        URI uri = convertURLtoURI(repository.getURL());
        if (uri != null) {
            return stream.getAllComponents().stream().anyMatch(e -> e.getCodebase().equals(codebase) && e.getRepositoryURL().equals(uri));

        } else {
            return false;
        }
    }

    public static boolean isNoUpstreamRequired(PullRequest pullRequest) {
        Optional<String> pullRequestBoday = Optional.ofNullable(pullRequest.getBody());
        Matcher matcher = Constants.UPSTREAM_NOT_REQUIRED.matcher(pullRequestBoday.orElse("N/A"));
        if (matcher.find())
            return true;
        else
            return false;
    }

    public static Optional<CommitStatus> retrieveCommitStatus(PullRequest pullRequest) {
        Optional<CommitStatus> commitStatus = Optional.of(CommitStatus.UNKNOWN);
        if (aphrodite != null) {
            try {
                commitStatus = Optional.of(aphrodite.getCommitStatusFromPullRequest(pullRequest));
            } catch (NotFoundException e) {
                logger.log(Level.FINE, "Unable to find build result for pull request : " + pullRequest.getURL(), e);
            }
        }
        return commitStatus;
    }

    public static PatchState getPatchState(URL url) {
        PatchType patchType = getPatchType(url);
        return getPatchState(url, patchType);
    }
}
