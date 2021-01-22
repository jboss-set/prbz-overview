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

import static org.jboss.set.overview.Util.filterComponent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import org.jboss.set.aphrodite.domain.Stream;
import org.jboss.set.assist.data.ProcessorData;
import org.jboss.set.overview.ejb.Aider;

/**
 * @author wangc
 *
 */
@Path("/streampullrequest")
public class StreamPullRequest {

    private static Logger logger = Logger.getLogger(StreamPullRequest.class.getCanonicalName());

    private List<ProcessorData> pullRequestData = new ArrayList<>();

    private static final String LIST_TITLE = "Stream Repositories List";

    @GET
    public void get(@Context ServletContext context, @Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException, ServletException {
        CustomRequest customRequest = new CustomRequest(request);
        CustomResponse customResponse = new CustomResponse(response);

        List<Stream> streams = Aider.getSubStreams();
        if (streams == null) {
            context.getRequestDispatcher("/error-wait.html").forward(customRequest, customResponse);
        } else {
            Map<String, List<String>> streamMap =getStreamMap();
            customRequest.setAttribute("title", LIST_TITLE);
            customRequest.setAttribute("first", "streampullrequest");
            customRequest.setAttribute("second", "component");
            customRequest.setAttribute("streamMap", streamMap);
            context.getRequestDispatcher("/stream_index.jsp").forward(customRequest, customResponse);
        }
    }

    @GET
    @Path("/{streamName}/component/{componentName}")
    public void getPullRequest(@Context ServletContext context, @Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("streamName") String streamName, @PathParam("componentName") String componentName)
            throws ServletException, IOException {
        CustomRequest customRequest = new CustomRequest(request);
        CustomResponse customResponse = new CustomResponse(response);

        if (streamName != null && componentName != null) {
            pullRequestData = Aider.getPullRequestData(streamName, componentName);
            if (pullRequestData == null || pullRequestData.isEmpty()) {
                context.getRequestDispatcher("/error-wait.html").forward(customRequest, customResponse);
            } else {
                Map<String, List<String>> streamMap = getStreamMap();
                customRequest.setAttribute("rows", pullRequestData);
                customRequest.setAttribute("streamName", streamName);
                customRequest.setAttribute("componentName", componentName);
                customRequest.setAttribute("pullRequestSize", pullRequestData.size());
                customRequest.setAttribute("streamMap", streamMap);
                context.getRequestDispatcher("/pullrequest.ftl").forward(customRequest, customResponse);
            }
        } else {
            logger.log(Level.WARNING, "streamName " + streamName + " and " + "componentName " + componentName
                    + " must be specified in request parameter");
        }

    }

    private Map<String, List<String>> getStreamMap () {
        return new TreeMap<>(
                Aider.getSubStreams().stream().collect(
                        Collectors.toMap(e -> e.getName(),
                                e -> e.getAllComponents().stream()
                                        .filter(f -> filterComponent(f)).map(g -> g.getName())
                                        .collect(Collectors.toList()))
                ));
    }
}