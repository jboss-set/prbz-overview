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

package org.jboss.set.assist;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Commit;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.simplecontainer.SimpleContainer;
import org.jboss.set.aphrodite.spi.NotFoundException;

import javax.naming.NameNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommitHomeService {
    private static final Logger logger = Logger.getLogger(CommitHomeService.class.getCanonicalName());
    private static Aphrodite aphrodite;
    private static Store store;

    private static final String FUTURE = "-future";
    private static final String PROPOSED = "-proposed";
    private static final String EAP7_REPO = "jbossas/jboss-eap7";
    private static final String EAP8_REPO = "jbossas/jboss-eap8";

    private static final long MONTH_MILLI = 2629800000L; // milliseconds in a month

    static {
        try {
            aphrodite = SimpleContainer.instance().lookup(Aphrodite.class.getSimpleName(), Aphrodite.class);
        } catch (NameNotFoundException e) {
            logger.log(Level.SEVERE, "Can not get aphrodite service due to : ", e);
        }
    }

    private static Store store() {
        if (store == null) {
            store = new Store();
        }
        return store;
    }

    private static Set<Commit> getLastCommits(PullRequest pullRequest) {
        if (!(isEAP7PR(pullRequest) || isEAP8PR(pullRequest))) {
            return Collections.emptySet();
        }

        String codebase = pullRequest.getCodebase().getName();
        if (store().getCommits(codebase) == null || store().isStale()) {
            try {
                long since = Instant.now().toEpochMilli() - 5 * MONTH_MILLI; // 5 months in the past
                Set<Commit> commits = new TreeSet<>(new Comparator<Commit>() {

                    @Override
                    public int compare(Commit o1, Commit o2) {
                        if (o1.getSha() == null) {
                            return -1;
                        }
                        if (o2.getSha() == null) {
                            return 1;
                        }
                        if (o1.getSha().equals(o2.getSha())) {
                            return 0;
                        }
                        return o1.getSha().compareTo(o2.getSha());
                    }
                });
                commits.addAll(aphrodite.getCommitsSince(pullRequest.getRepository().getURL(), codebase + FUTURE, since));
                commits.addAll(aphrodite.getCommitsSince(pullRequest.getRepository().getURL(), codebase + PROPOSED, since));
                store().putCommits(codebase, commits);
                store().update();
            } catch (NotFoundException nfe) {
                logger.warning("Could not retrieve commits from " + pullRequest.getRepository().getURL());
            }
        }

        return store().getCommits(codebase);
    }

    private static boolean isEAP7PR(PullRequest pr) {
        return pr.getURL().toString().contains(EAP7_REPO) && pr.getCodebase().getName().startsWith("7.");
    }

    private static boolean isEAP8PR(PullRequest pr) {
        return pr.getURL().toString().contains(EAP8_REPO) && pr.getCodebase().getName().startsWith("8.");
    }


    /**
     * Checks all commits (SHA or message) of the given PR against last commits from a work branch
     * (only considers PRs submitted to 7.<y>.x and 8.<y>.x branch)
     *
     * @param pullRequest
     * @return false if PR is not submitted to 7.x or 8.x or if one or more commits are missing in the work branches, true otherwise
     */
    public static boolean isMergedInWorkBranch(PullRequest pullRequest) {
        if (!(isEAP7PR(pullRequest) || isEAP8PR(pullRequest))) {
            return false;
        }

        Set<Commit> lastCommits = getLastCommits(pullRequest);
        for (Commit prCommit : pullRequest.getCommits()) {
            boolean found = lastCommits.stream()
                    .anyMatch(commit -> prCommit.getSha().equals(commit.getSha()) || pullRequest.getTitle().equals(commit.getMessage()));
            if (!found) {
                return false;
            }
        }

        return true;
    }

    private static class Store {
        private Map<String, Set<Commit>> commitMap = new HashMap<>();
        private LocalTime lastUpdate = LocalTime.now();

        public Set<Commit> getCommits(String branch) {
            return commitMap.get(branch);
        }

        public void putCommits(String branch, Set<Commit> commits) {
            commitMap.put(branch, commits);
        }

        public void update() {
            lastUpdate = isStale() ? LocalTime.now() : lastUpdate;
        }

        public boolean isStale() {
            return Duration.between(lastUpdate, LocalTime.now()).toHours() >= 2;
        }
    }
}
