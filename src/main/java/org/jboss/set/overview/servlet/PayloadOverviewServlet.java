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

import static org.jboss.set.assistant.Util.maxSeverity;
import static org.jboss.set.assistant.Util.filterBySelectedStatus;
import static org.jboss.set.assistant.Util.filterByMissedFlags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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

import org.jboss.set.assistant.data.ProcessorData;
import org.jboss.set.overview.ejb.Aider;

@WebServlet(name = "PayloadOverviewServlet", loadOnStartup = 1, urlPatterns = { "/payloadview/overview" })
public class PayloadOverviewServlet extends HttpServlet {

    private static final long serialVersionUID = 8833071859201802046L;

    private static Logger logger = Logger.getLogger(PayloadOverviewServlet.class.getCanonicalName());

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private List<ProcessorData> payloadData = new ArrayList<>();

    @EJB
    private Aider aiderService;

    public PayloadOverviewServlet() {
        super();
    }

    @Override
    public void init() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                logger.log(Level.INFO, "payload data initialisation in Servlet init()");
                aiderService.initAllPayloadData();
            }
        });
        executorService.shutdown();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String payloadName = request.getParameter("payloadName");
        String streamName = request.getParameter("streamName");
        Set<String> payloadSet = new TreeSet<>();
        if (payloadName != null && streamName != null) {
            if (Aider.getJiraPayloadStoresByStream().containsKey(streamName)) {
                payloadSet = Aider.getJiraPayloadStoresByStream().get(streamName).keySet();
            } else if (Aider.getBzPayloadStoresByStream().containsKey(streamName)) {
                payloadSet = Aider.getBzPayloadStoresByStream().get(streamName).keySet();
            } else {
                request.getRequestDispatcher("/error-wait.html").forward(request, response);
            }

            if (!payloadSet.isEmpty()) {
                // Put the data list in request and let Freemarker paint it.
                String[] selectedStatus = request.getParameterValues("selectedStatus"); // from ftl
                String[] missedFlags = request.getParameterValues("missedFlags"); // from ftl
                payloadData = Aider.getPayloadData(payloadName);
                if (payloadData == null || payloadData.isEmpty()) {
                    response.addHeader("Refresh", "5");
                    request.getRequestDispatcher("/error-wait.html").forward(request, response);
                } else {
                    if (selectedStatus != null && selectedStatus.length > 0) {
                        payloadData = filterBySelectedStatus(payloadData, Arrays.asList(selectedStatus));
                    }
                    if (missedFlags != null && missedFlags.length > 0) {
                        payloadData = filterByMissedFlags(payloadData, Arrays.asList(missedFlags));
                    }
                    request.setAttribute("rows", payloadData);
                    request.setAttribute("payloadName", payloadName);
                    request.setAttribute("streamName", streamName);
                    request.setAttribute("payloadSize", payloadData.size());
                    request.setAttribute("payloadStatus", maxSeverity(payloadData));
                    request.setAttribute("payloadSet", payloadSet);
                    request.getRequestDispatcher("/payload.ftl").forward(request, response);
                }
            } else {
                request.getRequestDispatcher("/error-wait.html").forward(request, response);
            }
        } else {
            logger.log(Level.WARNING, "streamName " + streamName + " or " + "payloadName " + payloadName + " is not specified in request");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // do nothing
    }
}
