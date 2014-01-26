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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.eclipse.egit.github.core.PullRequest;
import org.infinispan.api.BasicCache;
import org.jboss.logging.Logger;
import org.jboss.overview.model.OverviewData;
import org.jboss.pull.shared.Bug;
import org.jboss.pull.shared.BuildResult;
import org.jboss.pull.shared.PullHelper;
import org.richfaces.application.push.MessageException;

/**
 * @author wangchao
 */

@Startup
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class SingletonAider {

    private final Logger LOGGER = Logger.getLogger(SingletonAider.class);
    private final String PULL_REQUEST_STATE = "open";
    private static final String CACHE_NAME = "cache";
    private PullHelper helper;
    private final long DELAY = 10; // 10 minutes delay before task is to be executed.
    private final long PERIOD = 60; // 60 minutes between successive task executions.

    @Inject
    private CacheContainerProvider provider;
    private BasicCache<Integer, OverviewData> cache;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    public SingletonAider() {
    }

    @PostConstruct
    public void postConstruct() {
        // retrieve properties file defined in web.xml
        try {
            helper = new PullHelper("properties.file.path", "./processor.properties");
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
        // create cache
        cache = provider.getCacheContainer().getCache(CACHE_NAME);

        // another thread initialize cache
        executorService.execute(new Runnable() {
            public void run() {
                initCache();
            }
        });

        // Scheduled task timer to update cache values
        scheduler.scheduleAtFixedRate(new TaskThread(), DELAY, PERIOD, TimeUnit.MINUTES);
    }

    @Lock(LockType.WRITE)
    public void initCache() {
        List<PullRequest> pullRequests = new ArrayList<PullRequest>();
        try {
            pullRequests = helper.getPullRequestService().getPullRequests(helper.getRepository(), PULL_REQUEST_STATE);
        } catch (IOException e) {
            LOGGER.error("Can not retrieve pull requests on repository : " + helper.getRepository());
            e.printStackTrace(System.err);
        }

        for (PullRequest pullRequest : pullRequests) {
            List<PullRequest> upStreamPullRequests = new ArrayList<PullRequest>();
            BuildResult buildResult;
            List<Bug> bugs = new ArrayList<Bug>();

            buildResult = helper.checkBuildResult(pullRequest);

            String body = pullRequest.getBody();
            List<Integer> upStreamIds = helper.checkUpStreamPullRequestId(body);

            for (Integer id : upStreamIds) {
                try {
                    upStreamPullRequests.add(helper.getPullRequestService().getPullRequest(helper.getRepositoryUpstream(), id));
                } catch (IOException e) {
                    LOGGER.error("Can not retrieve upstream pull request number : " + id);
                    e.printStackTrace(System.err);
                }
            }

            bugs = helper.getBug(pullRequest);

            boolean mergeable = helper.isMergeable(pullRequest);

            List<String> overallState = makeOverallState(pullRequest);

            OverviewData pullRequestData = new OverviewData(pullRequest, buildResult, upStreamPullRequests, bugs, overallState,
                    mergeable);
            cache.put(pullRequest.getNumber(), pullRequestData, -1, TimeUnit.SECONDS);

            try {
                DataTableScrollerBean.push();
            } catch (MessageException e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public OverviewData getOverviewData(PullRequest pullRequest) {
        List<PullRequest> upStreamPullRequests = new ArrayList<PullRequest>();
        BuildResult buildResult;
        List<Bug> bugs = new ArrayList<Bug>();

        buildResult = helper.checkBuildResult(pullRequest);

        String body = pullRequest.getBody();

        List<Integer> upStreamIds = helper.checkUpStreamPullRequestId(body);

        for (Integer id : upStreamIds) {
            try {
                upStreamPullRequests.add(helper.getPullRequestService().getPullRequest(helper.getRepositoryUpstream(), id));
            } catch (IOException e) {
                LOGGER.error("Can not retrieve upstream pull request number : " + id);
                e.printStackTrace(System.err);
            }
        }

        bugs = helper.getBug(pullRequest);

        boolean mergeable = helper.isMergeable(pullRequest);

        List<String> overallState = makeOverallState(pullRequest);

        return new OverviewData(pullRequest, buildResult, upStreamPullRequests, bugs, overallState, mergeable);
    }

    public List<String> makeOverallState(PullRequest pullRequest) {
        List<String> overallState = new ArrayList<String>();

        // do we have a positive build result of lightning ?
        // comment this out since we have some intermittent failures
        // thus we can get a positive overall state even have build failure
        // BuildResult buildResult = helper.checkBuildResult(pullRequest);
        // overallState.add(buildResult.equals(BuildResult.SUCCESS) ? " + Lightning build result is " + buildResult :
        // " - Lightning build result is : " + buildResult);

        // do we have a resolved upstream issue ?
        overallState.add(helper.isMergeableByUpstream(pullRequest) ? " + Mergeable by upstream" : " - Not mergeable by upstream");

        // do we have a bugzilla issue ?
        overallState.add(helper.isMergeableByBugzilla(pullRequest, null) ? " + Mergeable by bugzilla" : " - Not mergeable by bugzilla");

        return overallState;
    }

    @Lock(LockType.WRITE)
    public void updateCache() {
        Set<Integer> keys = cache.keySet();

        List<PullRequest> pullRequests = new ArrayList<PullRequest>();
        try {
            pullRequests = helper.getPullRequestService().getPullRequests(helper.getRepository(), "open");
        } catch (IOException e) {
            LOGGER.info("Error to get pull requests on repository : " + helper.getRepository());
            e.printStackTrace(System.err);
        }

        Map<Integer, PullRequest> pullRequestsMap = new HashMap<Integer, PullRequest>();

        for (PullRequest pullRequest : pullRequests) {
            pullRequestsMap.put(pullRequest.getNumber(), pullRequest);
        }

        Set<Integer> ids = pullRequestsMap.keySet();

        // for all closed pull requests, remove from cache.
        for (Integer key : keys) {
            if (!ids.contains(key)) {
                cache.remove(key);
                try {
                    DataTableScrollerBean.push();
                } catch (MessageException e) {
                    e.printStackTrace(System.err);
                }
            }
        }

        // for all old pull request, update information
        keys = cache.keySet();
        for (Integer key : keys) {
            cache.replace(key, cache.get(key), getOverviewData(pullRequestsMap.get(key)));
            try {
                DataTableScrollerBean.push();
            } catch (MessageException e) {
                e.printStackTrace(System.err);
            }
        }

        // for all new pull requests, add into cache.
        for (Integer id : ids) {
            if (!keys.contains(id)) {
                OverviewData overviewData = getOverviewData(pullRequestsMap.get(id));
                cache.put(id, overviewData);
                try {
                    DataTableScrollerBean.push();
                } catch (MessageException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    public PullHelper getHelper() {
        return helper;
    }

    @Lock(LockType.READ)
    @AccessTimeout(value = 4, unit = TimeUnit.SECONDS)
    public BasicCache<Integer, OverviewData> getCache() {
        return cache;
    }

    class TaskThread implements Runnable {
        @Override
        public void run() {
            updateCache();
        }
    }
}
