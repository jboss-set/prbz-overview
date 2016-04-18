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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
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
    private static Aphrodite aphrodite;
    private static List<ProcessorData> pullRequestData = new ArrayList<>();
    private static List<ProcessorData> payloadData = new ArrayList<>();
    private static final Object pullRequestDataLock = new Object();
    private static final Object payloadDataLock = new Object();

    @PostConstruct
    public void init() {
        try {
            aphrodite = AssistantClient.getAphrodite();
        } catch (AphroditeException e) {
            logger.log(Level.SEVERE, "Failed to get aphrodite instance", e);
        }
    }

    public static List<ProcessorData> getPullRequestData() {
        return pullRequestData;
    }

    public static List<ProcessorData> getPayloadData() {
        return payloadData;
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

    public void generatePayloadData() {
        List<ProcessorData> dataList = new ArrayList<>();
        try {
            logger.info("new payload data values genearation is started...");
            // FIXME hard-coded payload url
            URL payloadURL = new URL("https://bugzilla.redhat.com/show_bug.cgi?id=1324262");
            Issue payloadTracker = aphrodite.getIssue(payloadURL);
            ServiceLoader<PayloadProcessor> processors = ServiceLoader.load(PayloadProcessor.class);
            for (PayloadProcessor processor : processors) {
                logger.info("executing processor: " + processor.getClass().getName());
                processor.init(aphrodite);
                dataList.addAll(processor.process(payloadTracker));
            }
            logger.info("new payload data values genearation is finished...");

        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "payload tracker url is malformed", e);
            e.printStackTrace();
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
                payloadData = dataList;
            }
        }
    }

    // Scheduled task timer to update data values every hour
    @Schedule(hour = "*")
    public void updatePullRequestData() {
        logger.info("schedule pull request data update is started ...");
        generatePullRequestData();
        logger.info("schedule pull request data update is finished ...");
    }

    @Schedule(hour = "*")
    public void updatePayloadData() {
        logger.info("schedule payload data update is started ...");
        generatePayloadData();
        logger.info("schedule payload data update is finished ...");
    }
}