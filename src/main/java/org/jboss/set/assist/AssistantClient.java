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

package org.jboss.set.assist;

import java.util.logging.Level;
import java.util.logging.Logger;


import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.spi.IssueHome;
import org.jboss.set.aphrodite.domain.spi.PatchHome;
import org.jboss.set.aphrodite.domain.spi.PullRequestHome;
import org.jboss.set.aphrodite.issue.trackers.jira.JiraIssueHomeImpl;
import org.jboss.set.aphrodite.repository.services.github.GithubPullRequestHomeService;
import org.jboss.set.aphrodite.simplecontainer.SimpleContainer;
import org.jboss.set.aphrodite.spi.AphroditeException;

/**
 * @author wangc
 *
 */
public class AssistantClient {

    private static Logger logger = Logger.getLogger(AssistantClient.class.getCanonicalName());

    private static final SimpleContainer simpleContainer = (SimpleContainer) SimpleContainer.instance();

    private static Aphrodite aphrodite;

    private AssistantClient() {
        logger.info("starting AssistantClient.");
    }

    static {
        try {
            simpleContainer.register(Aphrodite.class.getSimpleName(), getAphrodite());
        } catch (AphroditeException e) {
            logger.log(Level.SEVERE, "Can not get aphrodite due to : ", e);
        }
        PatchHomeService patchHomeService = new PatchHomeService();
        JiraIssueHomeImpl  issueHomeService = new JiraIssueHomeImpl();
        ViolationHomeService violationHomeService = new ViolationHomeService();
        GithubPullRequestHomeService GithubPullRequestHomeService = new GithubPullRequestHomeService(aphrodite);
        simpleContainer.register(PatchHome.class.getSimpleName(), patchHomeService);
        simpleContainer.register(IssueHome.class.getSimpleName(), issueHomeService);
        simpleContainer.register(ViolationHome.class.getSimpleName(), violationHomeService);
        simpleContainer.register(PullRequestHome.class.getSimpleName(), GithubPullRequestHomeService);
    }

    public static synchronized Aphrodite getAphrodite() throws AphroditeException {
        if (aphrodite == null) {
            aphrodite = Aphrodite.instance();
        }
        return aphrodite;
    }

}