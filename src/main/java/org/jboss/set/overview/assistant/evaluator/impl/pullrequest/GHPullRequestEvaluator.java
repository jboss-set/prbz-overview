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

package org.jboss.set.overview.assistant.evaluator.impl.pullrequest;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.CommitStatus;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.overview.Constants;
import org.jboss.set.overview.assistant.data.payload.AssociatedPullRequest;
import org.jboss.set.overview.assistant.evaluator.PullRequestEvaluator;
import org.jboss.set.overview.assistant.evaluator.PullRequestEvaluatorContext;

/**
 * @author egonzalez
 *
 */
public class GHPullRequestEvaluator implements PullRequestEvaluator {

    private static final Logger logger = Logger.getLogger(PullRequestEvaluator.class.getCanonicalName());

    @Override
    public String name() {
        return "Pull Request Evaluator";
    }

    @Override
    public void eval(PullRequestEvaluatorContext context, Map<String, Object> data) {
        Patch patch = context.getPatch();
        boolean isNoUpstreamRequired = false;
        isNoUpstreamRequired = isNoUpstreamRequired(patch);
        Aphrodite aphrodite = context.getAphrodite();
        Optional<CommitStatus> commitStatus = Optional.of(CommitStatus.UNKNOWN);
        try {
            commitStatus = Optional.of(aphrodite.getCommitStatusFromPatch(patch));
        } catch (NotFoundException e) {
            logger.log(Level.FINE, "Unable to find build result for pull request : " + patch.getURL(), e);
        }
        data.put("pullRequest", new AssociatedPullRequest(patch.getId(), patch.getURL(), patch.getCodebase().getName(),
                patch.getState().toString(), commitStatus.orElse(CommitStatus.UNKNOWN).toString(), isNoUpstreamRequired));

    }

    private boolean isNoUpstreamRequired(Patch p) {
        Optional<String> pullRequestBoday = Optional.ofNullable(p.getBody());
        Matcher matcher = Constants.UPSTREAM_NOT_REQUIRED.matcher(pullRequestBoday.orElse("N/A"));
        if (matcher.find())
            return true;
        else
            return false;
    }

}