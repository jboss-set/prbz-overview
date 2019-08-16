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

package org.jboss.set.assist.evaluator;

import java.util.Set;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.domain.Stream;

/**
 * @author egonzalez
 *
 */
public class EvaluatorContext {
    private Aphrodite aphrodite;
    private PullRequest pullRequest;
    private Set<Issue> issues;
    private Set<PullRequest> related;
    private Repository repository;

    private Stream stream;

    public EvaluatorContext(Aphrodite aphrodite, Repository repository, PullRequest pullRequest, Set<Issue> issues,
            Set<PullRequest> related, Stream stream) {
        this.aphrodite = aphrodite;
        this.pullRequest = pullRequest;
        this.issues = issues;
        this.related = related;
        this.repository = repository;
        this.stream = stream;
    }

    public Aphrodite getAphrodite() {
        return aphrodite;
    }

    public PullRequest getPullRequest() {
        return pullRequest;
    }

    public String getBranch() {
        return pullRequest.getCodebase().getName();
    }

    public Set<Issue> getIssues() {
        return issues;
    }

    public Set<PullRequest> getRelated() {
        return related;
    }

    public Repository getRepository() {
        return repository;
    }

    public Stream getStream() {
        return stream;
    }

}