/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Model;
import javax.inject.Inject;

import org.eclipse.egit.github.core.PullRequest;
import org.infinispan.api.BasicCache;
import org.jboss.logging.Logger;
import org.jboss.overview.model.OverviewData;

@Model
public class DataTableScrollerBean implements Serializable {

    private static final long serialVersionUID = 8201807342793317060L;
    public static final String CACHE_NAME = "cache";
    private static final Logger LOGGER = Logger.getLogger(DataTableScrollerBean.class);
    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E");
    private static BasicCache<String, OverviewData> cache;

    @Inject
    private CacheContainerProvider provider;
    private List<OverviewData> dataList = null;
    private String lastUpdateTime = dateFormat.format(new Date());

    public DataTableScrollerBean() {
    }

    @PostConstruct
    public void postContruct() {
        if (cache == null) {
            LOGGER.info("starting populate data...");
            cache = provider.getCacheContainer().getCache(CACHE_NAME);
            List<String> pullRequestNumbers = new ArrayList<String>();

            try {
                List<OverviewData> dataList = Helper.getOverviewData();
                for (OverviewData overviewData : dataList) {
                    String key = String.valueOf(overviewData.getPullRequest().getNumber());
                    cache.putIfAbsent(key, overviewData, -1, TimeUnit.SECONDS);
                    pullRequestNumbers.add(key);
                }
            } catch (Exception e) {
                LOGGER.warn("An exception occured while populating the database!");
            }

            LOGGER.info("Successfully imported data!");
        }
    }

    public List<OverviewData> getDataList() {
        // retrieve a cache
        cache = provider.getCacheContainer().getCache(CACHE_NAME);
        dataList = new ArrayList<OverviewData>(cache.values());
        return dataList;
    }

    public void setDataList(List<OverviewData> dataList) {
        this.dataList = dataList;
    }

    public String getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(String lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    /**
     * Synchronize with latest pull request on github.
     */
    public void refresh() {
        cache = provider.getCacheContainer().getCache(CACHE_NAME);

        Set<String> keys = cache.keySet();

        List<PullRequest> pullRequests = Helper.getPullRequests();

        HashSet<String> ids = new HashSet<String>();
        for (PullRequest pullRequest : pullRequests)
            ids.add(String.valueOf(pullRequest.getNumber()));

        // for all pull requests don't exist anymore, remove from cache.
        for (String key : keys) {
            if (!ids.contains(key)){
                cache.remove(key);
            }
        }

        // for all new pull requests, add into cache.
        for (PullRequest pullRequest : pullRequests) {
            if (!keys.contains(String.valueOf(pullRequest.getNumber()))) {
                OverviewData overviewData = Helper.getOverviewData(pullRequest);
                cache.put(String.valueOf(pullRequest.getNumber()), overviewData);
            }
        }

        //set the last update time
        lastUpdateTime = dateFormat.format(new Date());
    }
}
