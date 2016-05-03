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
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
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

import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.config.TrackerType;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Release;
import org.jboss.set.aphrodite.domain.SearchCriteria;
import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.assistant.data.ProcessorData;
import org.jboss.set.assistant.evaluator.PayloadEvaluator;
import org.jboss.set.assistant.evaluator.PayloadEvaluatorContext;
import org.jboss.set.assistant.processor.PayloadProcessor;
import org.jboss.set.assistant.processor.ProcessorException;

/**
 * @author wangc
 *
 */
public class PayloadOverviewProcessor implements PayloadProcessor {

    private static Logger logger = Logger.getLogger(PayloadOverviewProcessor.class.getCanonicalName());

    private Aphrodite aphrodite;

    private List<PayloadEvaluator> evaluators;

    private ExecutorService service;

    private ExecutorService singleExecutorService;

    @Override
    public void init(Aphrodite aphrodite){
        this.aphrodite = aphrodite;
        this.evaluators = getPayloadEvaluators();
        this.service = Executors.newFixedThreadPool(10);
        this.singleExecutorService = Executors.newSingleThreadExecutor();
    }

    @AccessTimeout(value = 15, unit = TimeUnit.MINUTES)
    @Lock(LockType.READ)
    @Override
    public List<ProcessorData> process(Issue issue, Stream stream) throws ProcessorException {
        logger.info("PayloadProcessor process is started for " + issue.getURL());

        try {
            if(singleExecutorService.isShutdown()){
                singleExecutorService = Executors.newSingleThreadExecutor();
            }
            Future<List<Issue>> future = singleExecutorService.submit(new DependecyRetrieveTask(issue));
            List<Issue> dependencyIssues = new ArrayList<>();
            boolean listen = true;
            while (listen) {
                try {
                    dependencyIssues = future.get(10, TimeUnit.MINUTES);
                } catch (ExecutionException | TimeoutException e) {
                    listen = false;
                    future.cancel(true);
                    logger.log(Level.SEVERE, "Failed to retrieve dependency issues due to " + e.getMessage());
                } finally {
                    listen = false;
                }
            }

            List<ProcessorData> data = new ArrayList<>();
            if (!dependencyIssues.isEmpty()) {
                if (service.isShutdown()) {
                    service = Executors.newFixedThreadPool(10);
                }
                List<Future<ProcessorData>> results = service
                        .invokeAll(dependencyIssues.stream()
                                .map(e -> new PayloadrocessingTask(e, issue, TrackerType.BUGZILLA, stream))
                                .collect(Collectors.toList()), 20, TimeUnit.MINUTES);

                for (Future<ProcessorData> result : results) {
                    try {
                        data.add(result.get());
                    } catch (CancellationException e) {
                        logger.log(Level.WARNING, "unfinished task is cancelled due to timeout", e);
                    } catch (ExecutionException ex) {
                        logger.log(Level.SEVERE, "ouch !" + ex.getCause());
                    }
                }

                logger.info("PayloadProcessor process is finished...");
                singleExecutorService.shutdown();
                service.shutdown();
            }
            return data;
        } catch (InterruptedException ex) {
            throw new ProcessorException("processor execution failed", ex);
        }
    }

    @AccessTimeout(value = 15, unit = TimeUnit.MINUTES)
    @Lock(LockType.READ)
    @Override
    public List<ProcessorData> process(String fixVersion, Stream stream) throws ProcessorException {
        logger.info("PayloadProcessor process is started for " + fixVersion);

        SearchCriteria sc = new SearchCriteria.Builder().setRelease(new Release(fixVersion.trim()))
                .setProduct("JBEAP")
                .setMaxResults(100)
                .build();
        try {
            if (singleExecutorService.isShutdown()) {
                singleExecutorService = Executors.newSingleThreadExecutor();
            }
            Future<List<Issue>> future = singleExecutorService.submit(new DependecyRetrieveTask(sc));
            List<Issue> dependencyIssues = new ArrayList<>();
            boolean listen = true;
            while (listen) {
                try {
                    dependencyIssues = future.get(10, TimeUnit.MINUTES);
                } catch (ExecutionException | TimeoutException e) {
                    listen = false;
                    future.cancel(true);
                    logger.log(Level.SEVERE, "Failed to retrieve dependency issues due to " + e.getMessage());
                } finally {
                    listen = false;
                }
            }

            List<ProcessorData> data = new ArrayList<>();
            if (!dependencyIssues.isEmpty()) {
                if (service.isShutdown()) {
                    service = Executors.newFixedThreadPool(10);
                }
                List<Future<ProcessorData>> results = service
                        .invokeAll(dependencyIssues.stream()
                                .map(e -> new PayloadrocessingTask(e, fixVersion, TrackerType.JIRA, stream))
                                .collect(Collectors.toList()), 20, TimeUnit.MINUTES);

                for (Future<ProcessorData> result : results) {
                    try {
                        data.add(result.get());
                    } catch (CancellationException e) {
                        logger.log(Level.WARNING, "unfinished task is cancelled due to timeout", e);
                    } catch (ExecutionException ex) {
                        logger.log(Level.SEVERE, "ouch !" + ex.getCause());
                    }
                }

                logger.info("PayloadProcessor process is finished...");
                service.shutdown();
            }
            return data;
        } catch (InterruptedException ex) {
            throw new ProcessorException("processor execution failed", ex);
        }
    }

    private class PayloadrocessingTask implements Callable<ProcessorData> {
        private Issue dependencyIssue;
        private Issue payloadTracker;
        private String fixVersion;
        private TrackerType trackerType;
        private Stream stream;

        PayloadrocessingTask(Issue dependencyIssue, Issue payloadTracker, TrackerType trackerType, Stream stream) {
            this.dependencyIssue = dependencyIssue;
            this.payloadTracker = payloadTracker;
            this.fixVersion = null;
            this.trackerType = trackerType;
            this.stream = stream;
        }

        PayloadrocessingTask(Issue dependencyIssue, String fixVersion, TrackerType trackerType, Stream stream) {
            this.dependencyIssue = dependencyIssue;
            this.payloadTracker = null;
            this.fixVersion = fixVersion;
            this.trackerType = trackerType;
            this.stream = stream;
        }

        @Override
        public ProcessorData call() throws Exception {
            try {
                logger.info("processing " + dependencyIssue.getURL());
                Map<String, Object> data = new HashMap<>();
                PayloadEvaluatorContext context;
                if (trackerType.equals(TrackerType.BUGZILLA)) {
                    context = new PayloadEvaluatorContext(aphrodite, dependencyIssue, payloadTracker, trackerType, stream);
                } else {
                    context = new PayloadEvaluatorContext(aphrodite, dependencyIssue, fixVersion, trackerType, stream);
                }
                for (PayloadEvaluator evaluator : evaluators) {
                    logger.fine("issue " + dependencyIssue.getURL() + " is applying evaluator " + evaluator.name());
                    evaluator.eval(context, data);
                }
                return new ProcessorData(data);
            } catch (Throwable th) {
                logger.log(Level.SEVERE, "failed to read" + dependencyIssue.getURL(), th);
                throw new Exception(th);
            }
        }
    }

    private class DependecyRetrieveTask implements Callable<List<Issue>> {

        private Issue issue;
        private SearchCriteria sc;

        DependecyRetrieveTask(Issue issue) {
            this.issue = issue;
            this.sc = null;
        }

        DependecyRetrieveTask(SearchCriteria sc) {
            this.issue = null;
            this.sc = sc;
        }

        @Override
        public List<Issue> call() throws Exception {
            List<Issue> dependencyIssues = new ArrayList<>();
            // Bugzilla payload tracker for EAP 6
            if (issue != null) {
                List<URL> dependencyURLs = issue.getDependsOn();
                for (URL url : dependencyURLs) {
                    Issue i;
                    try {
                        i = aphrodite.getIssue(url);
                        dependencyIssues.add(i);
                    } catch (NotFoundException e) {
                        logger.log(Level.WARNING, "failed to find depends on issue from " + url, e);
                    }
                }
            } else {
                dependencyIssues = aphrodite.searchIssues(sc);
            }
            return dependencyIssues;
        }
    }

    private List<PayloadEvaluator> getPayloadEvaluators() {
        ServiceLoader<PayloadEvaluator> rules = ServiceLoader.load(PayloadEvaluator.class);
        List<PayloadEvaluator> evaluators = new ArrayList<PayloadEvaluator>();
        for (PayloadEvaluator rule : rules) {
            evaluators.add(rule);
        }
        return evaluators;
    }
}
