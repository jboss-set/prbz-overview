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

package org.jboss.set.overview.ejb;

import static org.jboss.set.overview.Util.filterComponent;
import static org.jboss.set.overview.Util.findAllBugzillaPayloads;
import static org.jboss.set.overview.Util.findAllJiraPayloads;

import static org.jboss.set.assist.Constants.EAP64ZSTREAM;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueStatus;
import org.jboss.set.aphrodite.domain.RateLimit;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.aphrodite.domain.StreamComponent;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.assist.AssistantClient;
import org.jboss.set.assist.data.ProcessorData;
import org.jboss.set.assist.processor.PayloadProcessor;
import org.jboss.set.assist.processor.ProcessorException;
import org.jboss.set.assist.processor.PullRequestProcessor;
import org.jboss.set.overview.PrbzStatusSingleton;
import org.jboss.set.overview.Util;

/**
 * @author wangc
 *
 */
@Singleton
@Startup
public class Aider {
    private static Logger logger = Logger.getLogger(Aider.class.getCanonicalName());
    private static final String WILDFLY_STREAM = "wildfly"; // ignored upstream in streams view

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static Aphrodite aphrodite;
    public static List<Stream> allStreams = new ArrayList<>();
    private static Map<String, List<ProcessorData>> pullRequestData = new HashMap<>();
    private static Map<String, List<ProcessorData>> payloadData = new HashMap<>();
    private static Map<String, Map<String, List<ProcessorData>>> payloadsDataByStreams = new HashMap<>();
    private static ServiceLoader<PullRequestProcessor> pullRequestProcessors;
    private static ServiceLoader<PayloadProcessor> payloadProcessors;

    private static final Object pullRequestDataLock = new Object();
    private static final Object payloadDataLock = new Object();

    private static final boolean devProfile = System.getProperty("prbz-dev") != null;

    @Inject
    private PrbzStatusSingleton status;

    @PostConstruct
    public void init() {
        // Perform action during application's startup
        try {
            aphrodite = AssistantClient.getAphrodite();

            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    logger.log(Level.INFO, "New initialisation in Aider init()");
                    status.refreshStarted();

                    findAllJiraPayloads(Aider.aphrodite, true);
                    findAllBugzillaPayloads(Aider.aphrodite, true);

                    allStreams = aphrodite.getAllStreams().stream().filter(e -> !e.getName().equals(Aider.WILDFLY_STREAM)).collect(Collectors.toList());

                    initProcessors();

                    initAllPayloadData();

                    initAllPullRequestData();

                    status.refreshCompleted();
                }
            });

            executorService.shutdown();

        } catch (AphroditeException e) {
            throw new IllegalStateException("Failed to get aphrodite instance", e);
        }
    }

    public static List<ProcessorData> getPullRequestData(String streamName, String componentName) {
        return pullRequestData.get(streamName + componentName);
    }

    public static List<ProcessorData> getPayloadData(String payloadName) {
        return payloadData.get(payloadName);
    }

    public void initAllPullRequestData() {
        if (true) {
            return;
        }
        logger.info("pull request data initialization is started.");
        try {
            TimeUnit.MINUTES.sleep(2);// wait for streams loading
        } catch (InterruptedException e1) {
            // ignored
        }
        for (Stream s : allStreams) {
            String streamName = s.getName();
            s.getAllComponents().stream().filter(e -> filterComponent(e))
                    .forEach(e -> generatePullRequestData(streamName, e.getName()));
            logger.info("stream " + streamName + " pull request data initialization is finished.");
        }
    }

    public void initAllPayloadData() {
        logger.info("payload data initialization is started.");
        generatePayloadDataForJira(Util.jiraPayloadStoresByStream, true);
        generatePayloadDataForBz(EAP64ZSTREAM, Util.bzPayloadStore, true);
        logger.info("payload data initialization is finished.");
    }

    public void initProcessors() {
        payloadProcessors = ServiceLoader.load(PayloadProcessor.class);
        for (PayloadProcessor processor : payloadProcessors) {
            logger.info("init payload processor: " + processor.getClass().getName());
            processor.init(aphrodite);
        }
        pullRequestProcessors = ServiceLoader.load(PullRequestProcessor.class);
        for (PullRequestProcessor processor : pullRequestProcessors) {
            logger.info("init pull request processor: " + processor.getClass().getName());
            processor.init(aphrodite);
        }
    }

    public static void generatePullRequestData(String streamName, String componentName) {
        List<ProcessorData> dataList = new ArrayList<>();
        try {
            logger.info("stream " + streamName + " component " + componentName + " pull request data genearation is started...");

            Stream stream = aphrodite.getStream(streamName);
            StreamComponent streamComponent = stream.getComponent(componentName);
            URI uri = streamComponent.getRepositoryURL();
            if (uri != null) {
                URL repositoryURL = uri.toURL();
                Repository repository = aphrodite.getRepository(repositoryURL);

                for (PullRequestProcessor processor : pullRequestProcessors) {
                    logger.info("executing processor: " + processor.getClass().getName());
                    dataList.addAll(processor.process(repository, stream));
                }
            }
            logger.info("stream " + streamName + " component " + componentName + " pull request data genearation is finished...");
        } catch (NotFoundException e) {
            logger.log(Level.FINE, e.getMessage(), e);
        } catch (ProcessorException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
        if (!dataList.isEmpty()) {
            synchronized (pullRequestDataLock) {
                pullRequestData.put(streamName + componentName, dataList);
            }
        }
    }

    public static void generatePayloadDataForJira(LinkedHashMap<String, LinkedHashMap<String, List<Issue>>> jiraPayloadStoresByStream, boolean firstInit) {
        jiraPayloadStoresByStream.keySet().forEach(s -> jiraPayloadStoresByStream.get(s).keySet().forEach(payload -> {
            List<ProcessorData> dataList = new ArrayList<>();
            logger.info(payload + " data genearation is started...");
            Optional<Stream> stream = getCurrentStream(s);
            if (stream.isPresent()) {
                for (PayloadProcessor processor : payloadProcessors) {
                    logger.info("executing processor: " + processor.getClass().getName());
                    try {
                        if (firstInit || payloadData.get(payload) == null || !aphrodite.isCPReleased(payload)) {
                            dataList.addAll(processor.process(payload, jiraPayloadStoresByStream.get(s).get(payload), stream.get()));
                        } else {
                            logger.log(Level.INFO, "Released payload " + payload + " is skipped.");
                        }
                    } catch (ProcessorException ex1) {
                        logger.log(Level.WARNING, ex1.getMessage(), ex1);
                    } catch (Exception ex2) {
                        logger.log(Level.WARNING, ex2.getMessage(), ex2);
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                logger.log(Level.WARNING, "Empty stream name for payload " + payload);
            }
            if (!dataList.isEmpty()) {
                synchronized (payloadDataLock) {
                    payloadData.put(payload, dataList);
                }
            }
            logger.info(payload + " data genearation is finished...");
            payloadsDataByStreams.put(s, payloadData);
        }));
    }

    public static void generatedSinglePayloadData(String targetStream, String payload) {
        logger.info("refresh " + payload + " for " + targetStream + " is started ...");
        List<ProcessorData> dataList = new ArrayList<>();
        Optional<Stream> stream = getCurrentStream(targetStream);

        // update payload list
        List<Issue> issues = Util.testJiraPayloadExistence(aphrodite, payload);
        if (!issues.isEmpty()) {
            Util.jiraPayloadStoresByStream.get(targetStream).put(payload, issues);
            logger.log(Level.INFO, "Reload Jira Payload : " + payload);
        }
        LinkedHashMap<String, List<Issue>> payloads = Util.jiraPayloadStoresByStream.get(targetStream);

        // update payload date with processors
        if (stream.isPresent()) {
            for (PayloadProcessor processor : payloadProcessors) {
                logger.info("executing processor: " + processor.getClass().getName());
                try {
                    dataList.addAll(processor.process(payload, payloads.get(payload), stream.get()));
                } catch (ProcessorException ex1) {
                    logger.log(Level.WARNING, ex1.getMessage(), ex1);
                } catch (Exception ex2) {
                    logger.log(Level.WARNING, ex2.getMessage(), ex2);
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            logger.log(Level.WARNING, "Empty stream name for payload " + payload);
        }
        if (!dataList.isEmpty()) {
            synchronized (payloadDataLock) {
                payloadData.put(payload, dataList);
            }
        }
        logger.info("refresh " + payload + " for " + targetStream +  " is finished ...");
        payloadsDataByStreams.put(targetStream, payloadData);
    }

    public void generatePayloadDataForBz(String targetStream, Map<String, Issue> payloads, boolean firstInit) {
        payloads.keySet().forEach(payload -> {
            List<ProcessorData> dataList = new ArrayList<>();
            logger.info(payload + " data genearation is started...");
            Optional<Stream> stream = getCurrentStream(targetStream);
            if (stream.isPresent()) {
                for (PayloadProcessor processor : payloadProcessors) {
                    logger.info("executing processor: " + processor.getClass().getName());
                    try {
                        Issue trackerIssue = payloads.get(payload);
                        if (firstInit || payloadData.get(payload) == null || !(trackerIssue.getStatus().equals(IssueStatus.CLOSED) || trackerIssue.getStatus().equals(IssueStatus.VERIFIED))) {
                            // only add data in first initialization, or later schedule update if tracker is neither closed nor verified.
                            dataList.addAll(processor.process(trackerIssue, stream.get()));
                        } else{
                            logger.log(Level.INFO, "Released payload " + payload + " is skipped.");
                        }
                    } catch (ProcessorException ex1) {
                        logger.log(Level.WARNING, ex1.getMessage(), ex1);
                    } catch (Exception ex2) {
                        logger.log(Level.WARNING, ex2.getMessage(), ex2);
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                logger.log(Level.WARNING, "Empty stream name for payload " + payload);
            }
            if (!dataList.isEmpty()) {
                synchronized (payloadDataLock) {
                    payloadData.put(payload, dataList);
                }
            }
            logger.info(payload + " data genearation is finished...");
        });
    }

    @Asynchronous
    public void scheduleRefresh() {
        updatePayloadData();
    }

    @Asynchronous
    public void scheduleRefresh(String streamName, String payloadName) {
        status.refreshStarted();

        generatedSinglePayloadData(streamName, payloadName);

        status.refreshCompleted();
    }

    @Schedule(hour = "0,2,4,6,8,10,12,14,16,18,20,22")
    public void updatePullRequestData() {
        if (devProfile) return;
        logger.info("schedule pull request data update is started ...");
        // TOOD load new streams, although it's not often.
        for (Stream s : allStreams) {
            String streamName = s.getName();
            s.getAllComponents().stream().filter(e -> filterComponent(e))
                    .forEach(e -> generatePullRequestData(streamName, e.getName()));
            logger.info("stream " + streamName + " scheduled pull request data update is finished.");

        }
    }

    @Schedule(hour = "1,3,5,7,9,11,13,15,17,19,21,23")
    public void updatePayloadData() {
        if (devProfile) {
            status.refreshCompleted();
            return;
        }

        status.refreshStarted();

        logger.info("schedule payload data update is started ...");
        findAllBugzillaPayloads(aphrodite, false);
        findAllJiraPayloads(aphrodite, false);
        generatePayloadDataForJira(Util.jiraPayloadStoresByStream, false);
        Util.bzPayloadStoresByStream.keySet().stream().forEach(e -> generatePayloadDataForBz(e, Util.bzPayloadStoresByStream.get(e), false));
        logger.info("schedule payload data update is finished ...");

        status.refreshCompleted();
    }

    public static Optional<Stream> getCurrentStream(String streamName) {
        return allStreams.stream().filter(e -> e.getName().equalsIgnoreCase(streamName)).findFirst();
    }

    public static List<Stream> getAllStreams() {
        return allStreams;
    }

    public static LinkedHashMap<String, LinkedHashMap<String, Issue>> getBzPayloadStoresByStream() {
        return Util.bzPayloadStoresByStream;
    }

    public static LinkedHashMap<String, LinkedHashMap<String, List<Issue>>> getJiraPayloadStoresByStream() {
        return Util.jiraPayloadStoresByStream;
    }

    public static Map<RepositoryType, RateLimit> getRateLimits() {
        try {
            return aphrodite.getRateLimits();
        } catch (NotFoundException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        return null;
    }
}