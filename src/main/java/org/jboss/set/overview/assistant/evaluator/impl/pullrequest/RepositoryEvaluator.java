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

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.domain.StreamComponent;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.overview.assistant.data.LinkData;
import org.jboss.set.overview.assistant.evaluator.PullRequestEvaluator;
import org.jboss.set.overview.assistant.evaluator.PullRequestEvaluatorContext;

/**
 * @author egonzalez
 *
 */
public class RepositoryEvaluator implements PullRequestEvaluator {

    private static Logger logger = Logger.getLogger(RepositoryEvaluator.class.getCanonicalName());

    @Override
    public String name() {
        return "Repository evaluator";
    }

    @Override
    public void eval(PullRequestEvaluatorContext context, Map<String, Object> data) {
        Repository repository = context.getRepository();
        Aphrodite aphrodite = context.getAphrodite();
        Patch patch = context.getPatch();
        StreamComponent streamComponent;
        Optional<String> componentName = Optional.of("N/A");
        try {
            streamComponent = aphrodite.getComponentBy(patch.getRepository(), patch.getCodebase());
            componentName = Optional.of(streamComponent.getName());
        } catch (NotFoundException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        data.put("repository", new LinkData(componentName.get(), repository.getURL()));
    }
}