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

import static org.jboss.set.assistant.Constants.EAP70ZSTREAM;
import static org.jboss.set.assistant.Constants.EAP64ZSTREAM;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Startup;
import javax.ejb.Stateless;

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
import org.jboss.set.assistant.AssistantClient;
import org.jboss.set.assistant.data.ProcessorData;
import org.jboss.set.assistant.processor.PayloadProcessor;
import org.jboss.set.assistant.processor.ProcessorException;
import org.jboss.set.assistant.processor.PullRequestProcessor;
import org.jboss.set.overview.Util;

/**
 * @author wangc
 *
 */
@Stateless
@Startup
public class Aider {
    public static Logger logger = Logger.getLogger(Aider.class.getCanonicalName());
    public static final String PAYLOAD_PROPERTIES = "payload.properties";
    public static final String PAYLOAD_LIST = "payloadlist";
    public static final String WILDFLY_STREAM = "wildfly"; // ignored upstream in streams view

    private static Aphrodite aphrodite;
    private static List<Stream> allStreams = new ArrayList<>();
    private static Map<String, List<ProcessorData>> pullRequestData = new HashMap<>();
    private static Map<String, List<ProcessorData>> payloadData = new HashMap<>();
    private static ServiceLoader<PullRequestProcessor> pullRequestProcessors;
    private static ServiceLoader<PayloadProcessor> payloadProcessors;

    private static final Object pullRequestDataLock = new Object();
    private static final Object payloadDataLock = new Object();

    private static final boolean devProfile = System.getProperty("prbz-dev") != null;

    @PostConstruct
    public void init() {
        try {
            aphrodite = AssistantClient.getAphrodite();

            findAllJiraPayloads(aphrodite, true);
            findAllBugzillaPayloads(aphrodite, true);

            allStreams = aphrodite.getAllStreams().stream().filter(e -> !e.getName().equals(WILDFLY_STREAM)).collect(Collectors.toList());

            initProcessors();
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
        generatePayloadDataForJira(Util.jiraPayloadStore, true);
        generatePayloadDataForBz(Util.bzPayloadStore, true);
        logger.info("payload data initialization is finished.");
    }

    private void initProcessors() {
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

    public void generatePullRequestData(String streamName, String componentName) {
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

    public void generatePayloadDataForJira(Map<String, List<Issue>> payloads, boolean firstInit) {
        payloads.keySet().forEach(payload -> {
            List<ProcessorData> dataList = new ArrayList<>();
            logger.info(payload + " data genearation is started...");
            Optional<Stream> stream = getCurrentStream(EAP70ZSTREAM);
            if (stream.isPresent()) {
                for (PayloadProcessor processor : payloadProcessors) {
                    logger.info("executing processor: " + processor.getClass().getName());
                    try {
                        if (firstInit || !aphrodite.isCPReleased(payload)) {
                            dataList.addAll(processor.process(payload, payloads.get(payload), stream.get()));
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
        });
    }

    public void generatePayloadDataForBz(Map<String, Issue> payloads, boolean firstInit) {
        payloads.keySet().forEach(payload -> {
            List<ProcessorData> dataList = new ArrayList<>();
            logger.info(payload + " data genearation is started...");
            Optional<Stream> stream = getCurrentStream(EAP64ZSTREAM);
            if (stream.isPresent()) {
                for (PayloadProcessor processor : payloadProcessors) {
                    logger.info("executing processor: " + processor.getClass().getName());
                    try {
                        Issue trackerIssue = payloads.get(payload);
                        if (firstInit || !(trackerIssue.getStatus().equals(IssueStatus.CLOSED) || trackerIssue.getStatus().equals(IssueStatus.VERIFIED))) {
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

    @Schedule(minute = "*/30", hour = "*")
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

    @Schedule(minute = "*/30", hour = "*")
    public void updatePayloadData() {
        if (devProfile) return;
        logger.info("schedule payload data update is started ...");
        findAllBugzillaPayloads(aphrodite, false);
        findAllJiraPayloads(aphrodite, false);
        generatePayloadDataForJira(Util.jiraPayloadStore, false);
        generatePayloadDataForBz(Util.bzPayloadStore, false);
        logger.info("schedule payload data update is finished ...");

    }

    public Optional<Stream> getCurrentStream(String streamName) {
        return allStreams.stream().filter(e -> e.getName().equalsIgnoreCase(streamName)).findFirst();
    }

    public static List<Stream> getAllStreams() {
        return allStreams;
    }

    public static LinkedHashMap<String, Issue> getBzPayloadStore() {
        return Util.bzPayloadStore;
    }

    public static LinkedHashMap<String, List<Issue>> getJiraPayloadStore() {
        return Util.jiraPayloadStore;
    }

    public Map<RepositoryType, RateLimit> getRateLimits() {
        try {
            return aphrodite.getRateLimits();
        } catch (NotFoundException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        return null;
    }
}