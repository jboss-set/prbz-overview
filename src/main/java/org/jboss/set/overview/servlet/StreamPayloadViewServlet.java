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
import java.util.TreeSet;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.set.overview.ejb.Aider;

/**
 * @author wangc
 *
 */
@WebServlet(name = "StreamPayloadViewServlet", loadOnStartup = 1, urlPatterns = { "/streampayloadview" })
public class StreamPayloadViewServlet extends HttpServlet {

    private static final long serialVersionUID = 6161071130951666640L;

    @EJB
    private Aider aiderService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        TreeSet<String> payloadSetByStream = new TreeSet<String>();
        payloadSetByStream.addAll(Aider.getBzPayloadStoresByStream().keySet());
        payloadSetByStream.addAll(Aider.getJiraPayloadStoresByStream().keySet());
        if (payloadSetByStream.isEmpty()) {
            response.addHeader("Refresh", "5");
            request.getRequestDispatcher("/error-wait.html").forward(request, response);
        } else {
            request.setAttribute("payloadSetByStream", payloadSetByStream);
            request.getRequestDispatcher("/payload_stream_index.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        // do nothing
    }
}
