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

package org.jboss.set.assist.evaluator.impl.pullrequest;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.CommitStatus;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.assist.Constants;
import org.jboss.set.assist.data.payload.AssociatedPullRequest;
import org.jboss.set.assist.evaluator.Evaluator;
import org.jboss.set.assist.evaluator.EvaluatorContext;

/**
 * @author egonzalez
 *
 */
public class PullRequestEvaluator implements Evaluator {

    private static final Logger logger = Logger.getLogger(PullRequestEvaluator.class.getCanonicalName());

    @Override
    public String name() {
        return "Pull Request Evaluator";
    }

    @Override
    public void eval(EvaluatorContext context, Map<String, Object> data) {
        PullRequest pullRequest = context.getPullRequest();
        boolean isNoUpstreamRequired = false;
        isNoUpstreamRequired = isNoUpstreamRequired(pullRequest);
        Aphrodite aphrodite = context.getAphrodite();
        Optional<CommitStatus> commitStatus = Optional.of(CommitStatus.UNKNOWN);
        try {
            commitStatus = Optional.of(aphrodite.getCommitStatusFromPullRequest(pullRequest));
        } catch (NotFoundException e) {
            logger.log(Level.FINE, "Unable to find build result for pull request : " + pullRequest.getURL(), e);
        }
        data.put("pullRequest", new AssociatedPullRequest(pullRequest.getId(), pullRequest.getURL(), pullRequest.getCodebase().getName(),
                pullRequest.getState().toString(), commitStatus.orElse(CommitStatus.UNKNOWN).toString(),
                pullRequest.getMergableState() == null?null:pullRequest.getMergableState().name(), isNoUpstreamRequired));

    }

    private boolean isNoUpstreamRequired(PullRequest pullRequest) {
        Optional<String> pullRequestBoday = Optional.ofNullable(pullRequest.getBody());
        Matcher matcher = Constants.UPSTREAM_NOT_REQUIRED.matcher(pullRequestBoday.orElse("N/A"));
        if (matcher.find())
            return true;
        else
            return false;
    }

}