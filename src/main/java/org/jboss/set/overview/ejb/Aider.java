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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.aphrodite.domain.StreamComponent;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.assistant.AssistantClient;
import org.jboss.set.assistant.data.ProcessorData;
import org.jboss.set.assistant.processor.Processor;

/**
 * @author wangc
 *
 */
@Singleton
@Startup
public class Aider {
    public static Logger logger = Logger.getLogger(Aider.class.getCanonicalName());
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final long DELAY = 10; // 10 minutes delay before task is to be executed.
    private final long PERIOD = 60; // 60 minutes between successive task executions.
    private Aphrodite aphrodite;

    @PostConstruct
    public void init() {
        System.out.println("First time data initialisation in @PostConstruct");
        try {
            aphrodite = AssistantClient.getAphrodite();
        } catch (AphroditeException e) {
            logger.log(Level.SEVERE, "Failed to get aphrodite instance", e);
        }
        executorService.execute(new Runnable() {
            public void run() {
                generateData();
            }
        });

        // Scheduled task timer to update data values
        scheduler.scheduleAtFixedRate(new TaskThread(), DELAY, PERIOD, TimeUnit.MINUTES);
    }

    List<ProcessorData> data;

    public List<ProcessorData> getData() {
        return data;
    }

    @PreDestroy
    public void destroy() {
        try {
            aphrodite.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to close aphrodite instance.", e);
        }
    }

    private synchronized void generateData() {
        logger.info("new data values genearation is started...");
        // FIXME hard-coded stream name
        String streamName = "jboss-eap-6.4.z";
        // use -Daphrodite.config=/path/to/aphrodite.properties.json
        try {
            Stream stream;
            List<ProcessorData> dataList = new ArrayList<>();
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
            if (!dataList.isEmpty()) {
                this.data = dataList;
            }
            logger.info("new data values genearation is finished...");
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    class TaskThread implements Runnable {
        @Override
        public void run() {
            logger.info("schedule update is started ...");
            generateData();
            logger.info("schedule update is finished ...");
        }
    }
}