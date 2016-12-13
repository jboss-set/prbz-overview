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

package org.jboss.set.overview.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.set.overview.assistant.data.ProcessorData;
import org.jboss.set.overview.ejb.Aider;

@WebServlet(name = "PullRequestOverviewServlet", loadOnStartup = 1, urlPatterns = { "/streamview/pullrequestoverview" })
public class PullRequestOverviewServlet extends HttpServlet {

    public static Logger logger = Logger.getLogger(PullRequestOverviewServlet.class.getCanonicalName());

    private static final long serialVersionUID = -8119634403150269667L;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private List<ProcessorData> pullRequestData = new ArrayList<>();

    @EJB
    private Aider aiderService;

    public PullRequestOverviewServlet() {
        super();
    }

    @Override
    public void init() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                logger.log(Level.INFO, "pull request data initialisation in Servlet init()");
                aiderService.initAllPullRequestData();
            }
        });
        executorService.shutdown();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String streamName = request.getParameter("streamName");
        String componentName = request.getParameter("componentName");

        if (streamName != null && componentName != null) {
            pullRequestData = Aider.getPullRequestData(streamName, componentName);
            if (pullRequestData == null || pullRequestData.isEmpty()) {
                response.addHeader("Refresh", "5");
                request.getRequestDispatcher("/error-wait.html").forward(request, response);
            } else {
                request.setAttribute("rows", pullRequestData);
                request.setAttribute("streamName", streamName);
                request.setAttribute("componentName", componentName);
                request.setAttribute("pullRequestSize", pullRequestData.size());
                request.getRequestDispatcher("/pullrequest.ftl").forward(request, response);
            }
        } else {
            logger.log(Level.WARNING, "streamName " + streamName + " and " + "componentName " + componentName
                    + " must be specified in request parameter");
        }

    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // do nothing
    }
}
