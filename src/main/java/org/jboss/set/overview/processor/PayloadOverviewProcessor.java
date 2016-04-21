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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.config.TrackerType;
import org.jboss.set.aphrodite.domain.Issue;
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

    @Override
    public void init(Aphrodite aphrodite){
        this.aphrodite = aphrodite;
        this.evaluators = getPayloadEvaluators();
        this.service = Executors.newFixedThreadPool(10);
    }

    @Override
    public List<ProcessorData> process(Issue issue) throws ProcessorException {
        logger.info("PayloadProcessor process is started...");

        List<Issue> dependencyIssues = new ArrayList<>();

        TrackerType trackerType = issue.getTrackerType();
        if (trackerType.equals(TrackerType.BUGZILLA)) {
            // Bugzilla payload tracker for EAP 6
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
        } else if (trackerType.equals(TrackerType.JIRA)) {
            // Jira payload tracker for EAP 7
            // dependencyIssues =
        } else {
            throw new IllegalStateException("Tracker Type: " + trackerType + " of " + issue.getURL() + " is not supported");
        }

        try {
            List<Future<ProcessorData>> results = this.service
                    .invokeAll(dependencyIssues.stream().map(e -> new PayloadrocessingTask(e, issue, trackerType)).collect(Collectors.toList()));

            List<ProcessorData> data = new ArrayList<>();
            for (Future<ProcessorData> result : results) {
                try {
                    data.add(result.get(120, TimeUnit.SECONDS));
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "ouch !" + ex.getCause());
                }
            }

            logger.info("PayloadProcessor process is finished...");
            return data;

        } catch (InterruptedException ex) {
            throw new ProcessorException("processor execution failed", ex);
        }
    }

    private class PayloadrocessingTask implements Callable<ProcessorData> {
        private Issue dependencyIssue;
        private Issue payloadTracker;
        private TrackerType trackerType;

        PayloadrocessingTask(Issue dependencyIssue, Issue payloadTracker, TrackerType trackerType) {
            this.dependencyIssue = dependencyIssue;
            this.payloadTracker = payloadTracker;
            this.trackerType = trackerType;
        }

        @Override
        public ProcessorData call() throws Exception {
            try {
                logger.info("processing " + dependencyIssue.getURL());

                Map<String, Object> data = new HashMap<>();
                PayloadEvaluatorContext context = new PayloadEvaluatorContext(aphrodite, dependencyIssue, payloadTracker, trackerType);
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

    private List<PayloadEvaluator> getPayloadEvaluators() {
        ServiceLoader<PayloadEvaluator> rules = ServiceLoader.load(PayloadEvaluator.class);
        List<PayloadEvaluator> evaluators = new ArrayList<PayloadEvaluator>();
        for (PayloadEvaluator rule : rules) {
            evaluators.add(rule);
        }
        return evaluators;
    }
}
