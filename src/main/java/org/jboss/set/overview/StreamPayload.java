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

package org.jboss.set.overview;

import static org.jboss.set.assistant.Util.filterByMissedFlags;
import static org.jboss.set.assistant.Util.filterBySelectedStatus;
import static org.jboss.set.assistant.Util.maxSeverity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.jboss.set.assistant.data.ProcessorData;
import org.jboss.set.overview.ejb.Aider;

/**
 * @author wangc
 *
 */
@Path("/streampayload")
public class StreamPayload {

    private static Logger logger = Logger.getLogger(StreamPayload.class.getCanonicalName());
    private List<ProcessorData> payloadData = new ArrayList<>();

    @GET
    public void get(@Context ServletContext context, @Context HttpServletRequest request, @Context HttpServletResponse response)
            throws ServletException, IOException {
        CustomRequest customRequest = new CustomRequest(request);
        CustomResponse customResponse = new CustomResponse(response);

        TreeSet<String> payloadSetByStream = new TreeSet<String>();
        payloadSetByStream.addAll(Aider.getBzPayloadStoresByStream().keySet());
        payloadSetByStream.addAll(Aider.getJiraPayloadStoresByStream().keySet());

        if (payloadSetByStream.isEmpty()) {
            context.getRequestDispatcher("/error-wait.html").forward(customRequest, customResponse);
        } else {
            customRequest.setAttribute("payloadSetByStream", payloadSetByStream);
            context.getRequestDispatcher("/stream_payload_index.jsp").forward(customRequest, customResponse);
        }
    }

    @GET
    @Path("/{streamName}")
    public void getStream(@Context ServletContext context, @Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("streamName") String streamName)
            throws ServletException, IOException {
        CustomRequest customRequest = new CustomRequest(request);
        CustomResponse customResponse = new CustomResponse(response);

        Set<String> payloadSet = new TreeSet<String>();
        if (Aider.getJiraPayloadStoresByStream().containsKey(streamName)) {
            payloadSet = Aider.getJiraPayloadStoresByStream().get(streamName).keySet();
        } else if (Aider.getBzPayloadStoresByStream().containsKey(streamName)) {
            payloadSet = Aider.getBzPayloadStoresByStream().get(streamName).keySet();
        }

        if (payloadSet.isEmpty()) {
            context.getRequestDispatcher("/error-wait.html").forward(customRequest, customResponse);
        } else {
            customRequest.setAttribute("payloadSet", payloadSet);
            customRequest.setAttribute("streamName", streamName);
            context.getRequestDispatcher("/payload_index.jsp").forward(customRequest, customResponse);
        }
    }

    @GET
    @Path("/{streamName}/payload/{payloadName}")
    public void getPayload(@Context UriInfo info, @Context ServletContext context, @Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("streamName") String streamName, @PathParam("payloadName") String payloadName)
            throws ServletException, IOException {
        CustomRequest customRequest = new CustomRequest(request);
        CustomResponse customResponse = new CustomResponse(response);

        Set<String> payloadSet = new TreeSet<>();
        if (payloadName != null && streamName != null) {
            if (Aider.getJiraPayloadStoresByStream().containsKey(streamName)) {
                payloadSet = Aider.getJiraPayloadStoresByStream().get(streamName).keySet();
            } else if (Aider.getBzPayloadStoresByStream().containsKey(streamName)) {
                payloadSet = Aider.getBzPayloadStoresByStream().get(streamName).keySet();
            } else {
                context.getRequestDispatcher("/error-wait.html").forward(customRequest, customResponse);
            }

            if (!payloadSet.isEmpty()) {
                // Put the data list in request and let Freemarker paint it.
                List<String> selectedStatus = info.getQueryParameters().get("selectedStatus"); // from ftl
                List<String> missedFlags = info.getQueryParameters().get("missedFlags"); // from ftl
                payloadData = Aider.getPayloadData(payloadName);
                if (payloadData == null || payloadData.isEmpty()) {
                    context.getRequestDispatcher("/error-wait.html").forward(customRequest, customResponse);
                } else {
                    if (selectedStatus != null && selectedStatus.size() > 0) {
                        payloadData = filterBySelectedStatus(payloadData, selectedStatus);
                    }

                    if (missedFlags != null && missedFlags.size() > 0) {
                        payloadData = filterByMissedFlags(payloadData, missedFlags);
                    }

                    customRequest.setAttribute("rows", payloadData);
                    customRequest.setAttribute("payloadName", payloadName);
                    customRequest.setAttribute("streamName", streamName);
                    customRequest.setAttribute("payloadSize", payloadData.size());
                    customRequest.setAttribute("payloadStatus", maxSeverity(payloadData));
                    customRequest.setAttribute("payloadSet", payloadSet);

                    context.getRequestDispatcher("/payload.ftl").forward(customRequest, customResponse);
                }
            } else {
                context.getRequestDispatcher("/error-wait.html").forward(customRequest, customResponse);
            }
        } else {
            logger.log(Level.WARNING,
                    "streamName " + streamName + " or " + "payloadName " + payloadName + " is not specified in request");
        }
    }
}