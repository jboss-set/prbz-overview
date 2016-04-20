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

package org.jboss.set.overview.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.PatchState;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.assistant.data.ProcessorData;
import org.jboss.set.assistant.evaluator.Evaluator;
import org.jboss.set.assistant.evaluator.EvaluatorContext;
import org.jboss.set.assistant.processor.Processor;
import org.jboss.set.assistant.processor.ProcessorException;

public class PullRequestOverviewProcessor implements Processor {

    private static Logger logger = Logger.getLogger(PullRequestOverviewProcessor.class.getCanonicalName());

    private Aphrodite aphrodite;

    private List<Evaluator> evaluators;

    private ExecutorService service;

    @Override
    public void init(Aphrodite aphrodite) throws Exception {
        this.aphrodite = aphrodite;
        this.evaluators = getEvaluators();
        this.service = Executors.newFixedThreadPool(10);
    }

    @Override
    public List<ProcessorData> process(Repository repository) throws ProcessorException {
        logger.info("PullRequestOverviewProcessor process is started...");
        try {
            List<Patch> patches = aphrodite.getPatchesByState(repository, PatchState.OPEN);

            List<Future<ProcessorData>> results = this.service
                    .invokeAll(patches.stream().map(e -> new PatchProcessingTask(repository, e)).collect(Collectors.toList()));

            List<ProcessorData> data = new ArrayList<>();
            for (Future<ProcessorData> result : results) {
                try {
                    data.add(result.get(120, TimeUnit.SECONDS));
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "ouch !" + ex.getCause());
                }
            }

            this.service.shutdown();
            logger.info("PullRequestOverviewProcessor process is finished...");
            return data;

        } catch (NotFoundException | InterruptedException ex) {
            throw new ProcessorException("processor execution failed", ex);
        }
    }

    private class PatchProcessingTask implements Callable<ProcessorData> {
        private Repository repository;
        private Patch patch;

        public PatchProcessingTask(Repository repository, Patch patch) {
            this.repository = repository;
            this.patch = patch;
        }

        @Override
        public ProcessorData call() throws Exception {
            try {
                logger.info("processing " + patch.getURL().toString());
                List<Issue> issues = aphrodite.getIssuesAssociatedWith(patch);

                List<Patch> relatedPatches = aphrodite.findPatchesRelatedTo(patch);
                Map<String, Object> data = new HashMap<>();
                EvaluatorContext context = new EvaluatorContext(aphrodite, repository, patch, issues,
                        relatedPatches);
                for (Evaluator evaluator : evaluators) {
                    logger.fine(
                            "repository " + repository.getURL() + "applying evaluator " + evaluator.name() + " to "
                                    + patch.getId());
                    evaluator.eval(context, data);
                }
                return new ProcessorData(data);
            } catch (Throwable th) {
                logger.log(Level.SEVERE, "failed to read" + patch.getURL(), th);
                throw new Exception(th);
            }
        }
    }

    private List<Evaluator> getEvaluators() {
        ServiceLoader<Evaluator> rules = ServiceLoader.load(Evaluator.class);
        List<Evaluator> evaluators = new ArrayList<Evaluator>();
        for (Evaluator rule : rules) {
            evaluators.add(rule);
        }
        return evaluators;
    }
}
