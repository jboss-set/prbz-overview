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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.model.SelectItem;

import org.jboss.logging.Logger;
import org.jboss.overview.model.OverviewData;
import org.richfaces.application.push.MessageException;
import org.richfaces.application.push.TopicKey;
import org.richfaces.application.push.TopicsContext;
import org.richfaces.component.SortOrder;

/**
 * @author wangchao
 */

@ManagedBean
@SessionScoped
public class DataTableScrollerBean implements Serializable {

    private static final long serialVersionUID = 8201807342793317060L;
    private static final Logger LOGGER = Logger.getLogger(DataTableScrollerBean.class);

    private List<OverviewData> dataList = null;

    public String branchFilter;

    private SortOrder branchOrder = SortOrder.ascending;
    private SortOrder pullRequestOrder = SortOrder.ascending;
    private SortOrder stateOrder = SortOrder.ascending;
    private SortOrder buildResultOrder = SortOrder.ascending;
    private SortOrder mergeableOrder = SortOrder.ascending;

    @EJB
    public SingletonAider aider;

    public DataTableScrollerBean() {
    }

    @PostConstruct
    public void postContruct() {
    }

    public List<OverviewData> getDataList() {
        // retrieve a cache
        dataList = new ArrayList<OverviewData>(aider.getCache().values());
        Collections.sort(dataList, new Comparator<OverviewData>() {
            public int compare(OverviewData o1, OverviewData o2) {
                if (o1.getPullRequest().getNumber() > o2.getPullRequest().getNumber())
                    return 1;
                else
                    return -1;
            }
        });
        return dataList;
    }

    public List<SelectItem> getBranchOptions() {
        List<SelectItem> branchOptions = new ArrayList<SelectItem>();
        branchOptions.add(new SelectItem("", "All Branches"));

        for (String branch : aider.getHelper().getEvaluatorFacade().getCoveredBranches()) {
            branchOptions.add(new SelectItem(branch));
        }
        return branchOptions;
    }

    public String getBranchFilter() {
        return branchFilter;
    }

    public void setBranchFilter(String branchFilter) {
        this.branchFilter = branchFilter;
    }

    public SortOrder getBranchOrder() {
        return branchOrder;
    }

    public SortOrder getPullRequestOrder() {
        return pullRequestOrder;
    }

    public SortOrder getStateOrder() {
        return stateOrder;
    }

    public SortOrder getBuildResultOrder() {
        return buildResultOrder;
    }

    public SortOrder getMergeableOrder() {
        return mergeableOrder;
    }

    public void sortByBranch() {
        pullRequestOrder = SortOrder.unsorted;
        stateOrder = SortOrder.unsorted;
        buildResultOrder = SortOrder.unsorted;
        mergeableOrder = SortOrder.unsorted;
        LOGGER.debug("sortByBranch..." + branchOrder);
        if (branchOrder.equals(SortOrder.ascending)) {
            branchOrder = SortOrder.descending;
        } else {
            branchOrder = SortOrder.ascending;
        }
    }

    public void sortByPullRequest() {
        branchOrder = SortOrder.unsorted;
        stateOrder = SortOrder.unsorted;
        buildResultOrder = SortOrder.unsorted;
        mergeableOrder = SortOrder.unsorted;
        LOGGER.debug("sortByPullRequest..." + pullRequestOrder);
        if (pullRequestOrder.equals(SortOrder.ascending)) {
            pullRequestOrder = SortOrder.descending;
        } else {
            pullRequestOrder = SortOrder.ascending;
        }
    }

    public void sortByState() {
        branchOrder = SortOrder.unsorted;
        pullRequestOrder = SortOrder.unsorted;
        buildResultOrder = SortOrder.unsorted;
        mergeableOrder = SortOrder.unsorted;
        LOGGER.debug("sortByState..." + stateOrder);
        if (stateOrder.equals(SortOrder.ascending)) {
            stateOrder = SortOrder.descending;
        } else {
            stateOrder = SortOrder.ascending;
        }
    }

    public void sortByBuildResult() {
        branchOrder = SortOrder.unsorted;
        pullRequestOrder = SortOrder.unsorted;
        stateOrder = SortOrder.unsorted;
        mergeableOrder = SortOrder.unsorted;
        LOGGER.debug("sortByBuildRequest..." + buildResultOrder);
        if (buildResultOrder.equals(SortOrder.ascending)) {
            buildResultOrder = SortOrder.descending;
        } else {
            buildResultOrder = SortOrder.ascending;
        }
    }

    public void sortByMergeable() {
        branchOrder = SortOrder.unsorted;
        pullRequestOrder = SortOrder.unsorted;
        stateOrder = SortOrder.unsorted;
        buildResultOrder = SortOrder.unsorted;
        LOGGER.debug("sortByMergeable..." + mergeableOrder);
        if (mergeableOrder.equals(SortOrder.ascending)) {
            mergeableOrder = SortOrder.descending;
        } else {
            mergeableOrder = SortOrder.ascending;
        }
    }

    public static void push() throws MessageException {
        TopicKey topicKey = new TopicKey("pushAddress");
        TopicsContext topicsContext = TopicsContext.lookup();
        topicsContext.publish(topicKey, "empty message");
    }
}
