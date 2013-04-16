/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.overview;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.faces.application.Application;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.infinispan.api.BasicCache;
import org.jboss.overview.model.OverviewData;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

/**
 * Populates a cache with initial data. We need to obtain BeanManager from JNDI and create an instance of CacheContainerProvider
 * manually since injection into Listeners is not supported by CDI specification.
 * 
 * @author Martin Gencur
 * 
 */
public class PopulateCache implements SystemEventListener {

    private Logger log = Logger.getLogger(this.getClass().getName());

    private CacheContainerProvider provider;

    @Override
    public void processEvent(SystemEvent event) throws AbortProcessingException {
        provider = getContextualInstance(getBeanManager(), CacheContainerProvider.class);
        startup();
    }

    public void startup() {
        log.info("starting populate data...");
        BasicCache<String, OverviewData> cache = provider.getCacheContainer().getCache(DataTableScrollerBean.CACHE_NAME);
        List<String> pullRequestNumbers = new ArrayList<String>();

        // cache.put(DataTableScrollerBean.PULL_REQUEST_NUMBERS_KEY, pullRequestNumbers);

        try {
            List<OverviewData> dataList = Helper.getOverviewData();
            for (OverviewData overviewData : dataList) {
                String key = String.valueOf(overviewData.getPullRequest().getNumber());
                cache.putIfAbsent(key, overviewData, -1, TimeUnit.SECONDS);
                pullRequestNumbers.add(key);
            }
        } catch (Exception e) {
            log.warning("An exception occured while populating the database!");
        }

        log.info("Successfully imported data!");
    }

    private BeanManager getBeanManager() {
        // BeanManager is only available when CDI is available. This is achieved by the presence of beans.xml file
        InitialContext context;
        Object result;
        try {
            context = new InitialContext();
            result = context.lookup("java:comp/env/BeanManager"); // lookup in Tomcat
        } catch (NamingException e) {
            try {
                context = new InitialContext();
                result = context.lookup("java:comp/BeanManager"); // lookup in JBossAS
            } catch (NamingException ex) {
                throw new RuntimeException("BeanManager could not be found in JNDI", e);
            }
        }
        return (BeanManager) result;
    }

    @SuppressWarnings("unchecked")
    public <T> T getContextualInstance(final BeanManager manager, final Class<T> type) {
        T result = null;
        Bean<T> bean = (Bean<T>) manager.resolve(manager.getBeans(type));
        if (bean != null) {
            CreationalContext<T> context = manager.createCreationalContext(bean);
            if (context != null) {
                result = (T) manager.getReference(bean, type, context);
            }
        }
        return result;
    }

    @Override
    public boolean isListenerForSource(Object source) {
        return source instanceof Application;
    }
}
