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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Codebase;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.domain.PullRequestState;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.assistant.data.ProcessorData;
import org.jboss.set.assistant.evaluator.Evaluator;
import org.jboss.set.assistant.evaluator.EvaluatorContext;
import org.jboss.set.assistant.processor.ProcessorException;
import org.jboss.set.assistant.processor.PullRequestProcessor;

public class PullRequestOverviewProcessor implements PullRequestProcessor {

    private static Logger logger = Logger.getLogger(PullRequestOverviewProcessor.class.getCanonicalName());

    private Aphrodite aphrodite;

    private List<Evaluator> evaluators;

    private ExecutorService service;

    private ExecutorService singleExecutorService;

    private static final boolean devProfile = System.getProperty("prbz-dev") != null;

    @Override
    public void init(Aphrodite aphrodite) {
        this.aphrodite = aphrodite;
        this.evaluators = getEvaluators();
        this.service = Executors.newFixedThreadPool(10);
        this.singleExecutorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public List<ProcessorData> process(Repository repository, Stream stream) throws ProcessorException {
        logger.info("PullRequestOverviewProcessor process is started for " + repository.getURL());
        try {
            if (singleExecutorService.isShutdown()) {
                singleExecutorService = Executors.newSingleThreadExecutor();
            }

            Future<List<PullRequest>> future = singleExecutorService.submit(new PatchesRetrieveTask(repository, stream));

            List<PullRequest> pullRequests = new ArrayList<>();
            boolean listen = true;
            while (listen) {
                try {
                    pullRequests = future.get(10, TimeUnit.MINUTES);
                } catch (ExecutionException | TimeoutException e) {
                    listen = false;
                    future.cancel(true);
                    singleExecutorService.shutdown();
                    logger.log(Level.SEVERE, "Failed to retrieve patches due to " + e);
                } finally {
                    listen = false;
                }
            }

            List<ProcessorData> data = new ArrayList<>();
            if (!pullRequests.isEmpty()) {
                if (service.isShutdown()) {
                    service = Executors.newFixedThreadPool(10);
                }
                List<Future<ProcessorData>> results = service
                        .invokeAll(
                                pullRequests.stream().map(e -> new PatchProcessingTask(repository, e, stream)).collect(Collectors.toList()), 10, TimeUnit.MINUTES);

                for (Future<ProcessorData> result : results) {
                    try {
                        data.add(result.get());
                    } catch (CancellationException e) {
                        result.cancel(true);
                        logger.log(Level.WARNING, "unfinished task is cancelled due to timeout", e);
                    } catch (ExecutionException ex) {
                        result.cancel(true);
                        logger.log(Level.SEVERE, "ouch !" + ex.getCause());
                    }
                }

                logger.info("PullRequestProcessor process is finished...");
                if (!singleExecutorService.isShutdown())
                    singleExecutorService.shutdown();
                service.shutdown();

                service.shutdown();
            }
            return data;

        } catch (InterruptedException ex) {
            throw new ProcessorException("processor execution failed", ex);
        }
    }

    private class PatchProcessingTask implements Callable<ProcessorData> {
        private Repository repository;
        private PullRequest pullRequest;
        private Stream stream;

        PatchProcessingTask(Repository repository, PullRequest pullRequest, Stream stream) {
            this.repository = repository;
            this.pullRequest = pullRequest;
            this.stream = stream;
        }

        @Override
        public ProcessorData call() throws Exception {
            try {
                logger.info("processing " + pullRequest.getURL().toString());
                Set<Issue> issues = new HashSet<>(aphrodite.getIssuesAssociatedWith(pullRequest));

                Set<PullRequest> relatedRequests = new HashSet<>(aphrodite.findPullRequestsRelatedTo(pullRequest));
                Map<String, Object> data = new HashMap<>();
                EvaluatorContext context = new EvaluatorContext(aphrodite, repository, pullRequest, issues, relatedRequests, stream);
                for (Evaluator evaluator : evaluators) {
                    logger.fine(
                            "repository " + repository.getURL() + "applying evaluator " + evaluator.name() + " to "
                                    + pullRequest.getId());
                    evaluator.eval(context, data);
                }
                return new ProcessorData(data);
            } catch (Throwable th) {
                logger.log(Level.SEVERE, "failed to read " + pullRequest.getURL(), th);
                throw new Exception(th);
            }
        }
    }

    private class PatchesRetrieveTask implements Callable<List<PullRequest>> {

        private Repository repository;
        private Stream stream;

        PatchesRetrieveTask(Repository repository, Stream stream) {
            this.repository = repository;
            this.stream = stream;
        }

        @Override
        public List<PullRequest> call() throws Exception {
            List<PullRequest> pullRequests = new ArrayList<>();
            pullRequests = aphrodite.getPullRequestsByState(repository, PullRequestState.OPEN);
            java.util.stream.Stream<PullRequest> pullRequestStream = pullRequests.stream();
            if (devProfile) {
                pullRequestStream = pullRequestStream.limit(10);
            }
            return pullRequestStream.filter(e -> checkPullRequestBranch(e, stream)).collect(Collectors.toList());
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

    private boolean checkPullRequestBranch(PullRequest pullRequest, Stream stream) {
        Codebase codeBase = pullRequest.getCodebase();
        URL pullRequestURL = pullRequest.getRepository().getURL();
        return stream.getAllComponents().stream().filter(
                e -> (e.getRepositoryURL().toString().equals(pullRequestURL.toString()) && e.getCodebase().equals(codeBase)))
                .findAny().isPresent();
    }
}
