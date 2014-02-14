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

    private SortOrder branchOrder = SortOrder.descending;
    private SortOrder pullRequestOrder = SortOrder.descending;
    private SortOrder buildResultOrder = SortOrder.descending;
    private SortOrder mergeableOrder = SortOrder.descending;
    private SortOrder isReviewedOrder = SortOrder.descending;
    private SortOrder pullStateOrder = SortOrder.descending;

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

    public SortOrder getBuildResultOrder() {
        return buildResultOrder;
    }

    public SortOrder getMergeableOrder() {
        return mergeableOrder;
    }

    public SortOrder getIsReviewedOrder() {
        return isReviewedOrder;
    }

    public SortOrder getPullStateOrder() {
        return pullStateOrder;
    }

    public void sortByBranch() {
        pullRequestOrder = SortOrder.unsorted;
        buildResultOrder = SortOrder.unsorted;
        mergeableOrder = SortOrder.unsorted;
        isReviewedOrder = SortOrder.unsorted;
        pullStateOrder = SortOrder.unsorted;
        LOGGER.debug("sortByBranch..." + branchOrder);
        if (branchOrder.equals(SortOrder.descending)) {
            branchOrder = SortOrder.ascending;
        } else {
            branchOrder = SortOrder.descending;
        }
    }

    public void sortByPullRequest() {
        branchOrder = SortOrder.unsorted;
        buildResultOrder = SortOrder.unsorted;
        mergeableOrder = SortOrder.unsorted;
        isReviewedOrder = SortOrder.unsorted;
        pullStateOrder = SortOrder.unsorted;
        LOGGER.debug("sortByPullRequest..." + pullRequestOrder);
        if (pullRequestOrder.equals(SortOrder.descending)) {
            pullRequestOrder = SortOrder.ascending;
        } else {
            pullRequestOrder = SortOrder.descending;
        }
    }

    public void sortByBuildResult() {
        branchOrder = SortOrder.unsorted;
        pullRequestOrder = SortOrder.unsorted;
        mergeableOrder = SortOrder.unsorted;
        isReviewedOrder = SortOrder.unsorted;
        pullStateOrder = SortOrder.unsorted;
        LOGGER.debug("sortByBuildRequest..." + buildResultOrder);
        if (buildResultOrder.equals(SortOrder.descending)) {
            buildResultOrder = SortOrder.ascending;
        } else {
            buildResultOrder = SortOrder.descending;
        }
    }

    public void sortByMergeable() {
        branchOrder = SortOrder.unsorted;
        pullRequestOrder = SortOrder.unsorted;
        buildResultOrder = SortOrder.unsorted;
        isReviewedOrder = SortOrder.unsorted;
        pullStateOrder = SortOrder.unsorted;
        LOGGER.debug("sortByMergeable..." + mergeableOrder);
        if (mergeableOrder.equals(SortOrder.descending)) {
            mergeableOrder = SortOrder.ascending;
        } else {
            mergeableOrder = SortOrder.descending;
        }
    }

    public void sortByReviewed() {
        branchOrder = SortOrder.unsorted;
        pullRequestOrder = SortOrder.unsorted;
        buildResultOrder = SortOrder.unsorted;
        mergeableOrder = SortOrder.unsorted;
        pullStateOrder = SortOrder.unsorted;
        LOGGER.debug("sortByReviewed..." + isReviewedOrder);
        if (isReviewedOrder.equals(SortOrder.descending)) {
            isReviewedOrder = SortOrder.ascending;
        } else {
            isReviewedOrder = SortOrder.descending;
        }
    }

    public void sortByPullState() {
        branchOrder = SortOrder.unsorted;
        pullRequestOrder = SortOrder.unsorted;
        buildResultOrder = SortOrder.unsorted;
        mergeableOrder = SortOrder.unsorted;
        isReviewedOrder = SortOrder.unsorted;
        LOGGER.debug("sortByPullStateOrder..." + pullStateOrder);
        if (pullStateOrder.equals(SortOrder.descending)) {
            pullStateOrder = SortOrder.ascending;
        } else {
            pullStateOrder = SortOrder.descending;
        }
    }

    public static void push() throws MessageException {
        TopicKey topicKey = new TopicKey("pushAddress");
        TopicsContext topicsContext = TopicsContext.lookup();
        topicsContext.publish(topicKey, "empty message");
    }
}
