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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Startup;
import javax.ejb.Stateless;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.aphrodite.domain.StreamComponent;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.assistant.AssistantClient;
import org.jboss.set.assistant.Util;
import org.jboss.set.assistant.data.ProcessorData;
import org.jboss.set.assistant.processor.PayloadProcessor;
import org.jboss.set.assistant.processor.Processor;
import org.jboss.set.assistant.processor.ProcessorException;

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

    private static Aphrodite aphrodite;
    private static List<Stream> streams;
    private static Map<String, String> payloadMap = new HashMap<>();
    private static List<ProcessorData> pullRequestData = new ArrayList<>();
    private static Map<String, List<ProcessorData>> payloadData = new HashMap<>();
    private static ServiceLoader<PayloadProcessor> payloadProcessors;

    private static final Object pullRequestDataLock = new Object();
    private static final Object payloadDataLock = new Object();

    @PostConstruct
    public void init() {
        try {
            aphrodite = AssistantClient.getAphrodite();
            payloadMap = loadPayloadMap();
            streams = aphrodite.getAllStreams();
            initProcessors();
        } catch (AphroditeException e) {
            throw new IllegalStateException("Failed to get aphrodite instance", e);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Failed to find payload.properties File", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load payload.properties File", e);
        }
    }

    public static List<ProcessorData> getPullRequestData() {
        return pullRequestData;
    }

    public static List<ProcessorData> getPayloadData(String payloadName) {
        return payloadData.get(payloadName);
    }

    @PreDestroy
    public void destroy() {
        try {
            aphrodite.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to close aphrodite instance.", e);
        }
    }

    public void generatePullRequestData() {
        // FIXME hard-coded stream name
        String streamName = "jboss-eap-6.4.z";
        // use -Daphrodite.config=/path/to/aphrodite.properties.json
        List<ProcessorData> dataList = new ArrayList<>();
        try {
            logger.info("new pull request data values genearation is started...");
            Stream stream;
            stream = aphrodite.getStream(streamName);
            StreamComponent streamComponent = stream.getComponent("Application Server");
            Repository repository = streamComponent.getRepository();

            logger.info("found component for : " + streamComponent.getName());
            ServiceLoader<Processor> processors = ServiceLoader.load(Processor.class);

            for (Processor processor : processors) {
                logger.info("executing processor: " + processor.getClass().getName());
                processor.init(aphrodite);
                dataList.addAll(processor.process(repository));
            }
            logger.info("new pull request data values genearation is finished...");
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
                pullRequestData = dataList;
            }
        }
    }

    public void initAllPayloadData() {
        logger.info("payload data initialization is started.");
        payloadMap.keySet().stream().forEach(e -> generatePayloadData(e));
        logger.info("payload data initialization is finished.");
    }

    private void initProcessors() {
        payloadProcessors = ServiceLoader.load(PayloadProcessor.class);
        for (PayloadProcessor processor : payloadProcessors) {
            logger.info("init processor: " + processor.getClass().getName());
            processor.init(aphrodite);
        }
    }

    public void generatePayloadData(String payloadName) {
        List<ProcessorData> dataList = new ArrayList<>();
        try {
            boolean eap6 = payloadName.toLowerCase().contains("eap6");
            logger.info(payloadName + " data genearation is started...");
            String payloadInfo = payloadMap.get(payloadName);
            String[] payloadArray = payloadInfo.split(",");
            Stream stream = getCurrentStream(payloadArray[1].trim());
            if (payloadArray.length != 2)
                throw new IllegalArgumentException("payload metadata defined in payload.properties is incorrect.");
            if (eap6) {
                // EAP 6 based on Bugzilla tracker bug
                URL payloadURL = new URL(payloadArray[0]);
                Issue payloadTracker = aphrodite.getIssue(payloadURL);
                for (PayloadProcessor processor : payloadProcessors) {
                    logger.info("executing processor: " + processor.getClass().getName());
                    dataList.addAll(processor.process(payloadTracker, stream));
                }
            } else {
                // EAP 7 based on Jira fix version e.g. 7.0.1.GA
                for (PayloadProcessor processor : payloadProcessors) {
                    logger.info("executing processor: " + processor.getClass().getName());
                    String fixVersion = payloadArray[0];
                    dataList.addAll(processor.process(fixVersion, stream));
                }
            }
            logger.info(payloadName + " data genearation is finished...");
        } catch (NotFoundException e) {
            logger.log(Level.FINE, e.getMessage(), e);
        } catch (ProcessorException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        if (!dataList.isEmpty()) {
            synchronized (payloadDataLock) {
                payloadData.put(payloadName, dataList);
            }
        }
    }

    // Scheduled task timer to update data values every hour
    // @Schedule(hour = "*")
    // public void updatePullRequestData() {
    // logger.info("schedule pull request data update is started ...");
    // generatePullRequestData();
    // logger.info("schedule pull request data update is finished ...");
    // }

    @Schedule(hour = "*")
    public void updatePayloadData() {
        logger.info("schedule payload data update is started ...");
        payloadMap.keySet().stream().forEach(e -> generatePayloadData(e));
        logger.info("schedule payload data update is finished ...");
    }

    // load payload list from payload.properties file
    public Map<String, String> loadPayloadMap() throws FileNotFoundException, IOException {
        String payloadProperiesFilePath = System.getProperty(PAYLOAD_PROPERTIES);
        if (payloadProperiesFilePath == null)
            throw new IllegalArgumentException("Unable to find payload properties file path with property name : " + PAYLOAD_PROPERTIES);
        Properties props = new Properties();
        File file = new File(payloadProperiesFilePath);
        try (FileReader reader = new FileReader(file)) {
            props.load(reader);
        }
        String payloads = Util.require(props, PAYLOAD_LIST);
        StringTokenizer tokenizer = new StringTokenizer(payloads, ",");
        Map<String, String> payloadMap = new HashMap<>();
        while (tokenizer.hasMoreElements()) {
            String payloadName = (String) tokenizer.nextElement();
            String payloadInfo = Util.require(props, payloadName);
            payloadMap.put(payloadName, payloadInfo);
        }
        return payloadMap;
    }

    private Stream getCurrentStream(String streamName) {
        return streams.stream().filter(e -> e.getName().equalsIgnoreCase(streamName)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(streamName + " is not defined in streamConfigs json file"));
    }

    public static Map<String, String> getPayloadMap() {
        return payloadMap;
    }
}