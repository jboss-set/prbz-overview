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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Startup;
import javax.ejb.Stateless;

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
@Stateless
@Startup
public class Aider {
    public static Logger logger = Logger.getLogger(Aider.class.getCanonicalName());
    private static Aphrodite aphrodite;
    private List<ProcessorData> data = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            aphrodite = AssistantClient.getAphrodite();
        } catch (AphroditeException e) {
            logger.log(Level.SEVERE, "Failed to get aphrodite instance", e);
        }
    }

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

    public synchronized void generateData() {
        // FIXME hard-coded stream name
        String streamName = "jboss-eap-6.4.z";
        // use -Daphrodite.config=/path/to/aphrodite.properties.json
        List<ProcessorData> dataList = new ArrayList<>();
        try {
            logger.info("new data values genearation is started...");
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
            logger.info("new data values genearation is finished...");
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        if (!dataList.isEmpty()) {
            data = dataList;
        }
    }

    // Scheduled task timer to update data values every hour
    @Schedule(hour = "*")
    public void doWork() {
        logger.info("schedule update is started ...");
        generateData();
        logger.info("schedule update is finished ...");
    }
}