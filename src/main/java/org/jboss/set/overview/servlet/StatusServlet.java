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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.jboss.set.overview.Constants;
import org.jboss.set.overview.ejb.Aider;

/**
 * @author wangc
 *
 */
@WebServlet(name = "StatusServlet", loadOnStartup = 1, urlPatterns = { "/status" })
public class StatusServlet extends HttpServlet {

    private static final long serialVersionUID = 1700864716729325061L;

    private static final String JIRA_HOST = "https://issues.jboss.org/";
    private static final String BUGZILLA_HOST = "https://bugzilla.redhat.com/";
    private static final String GITHUB_HOST = "https://github.com/";

    private static Map<RepositoryType, Integer> remainingRequests;
    private static Map<RepositoryType, Integer> requestLimit;
    @EJB
    private Aider aiderService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");

        // connection status
        try {
            checkConnection(response, JIRA_HOST, Constants.JIRA);
            checkConnection(response, BUGZILLA_HOST, Constants.BUGZILLA);
            checkConnection(response, GITHUB_HOST, Constants.GITHUB);
        } catch (MalformedURLException e) {
            // ignored
        }

        // check Github Api limitation by querying RequestLimits and RemainingRequests
        requestLimit = aiderService.getRequestLimit();
        remainingRequests = aiderService.getRemainingRequests();

        Set<RepositoryType> keys = requestLimit.keySet();
        for (RepositoryType key : keys) {
            response.getWriter().println(key.toString() + " RequestLimit : " + "<b>" + requestLimit.get(key) + "</b>");
            response.getWriter().println("</br>");
        }

        keys = remainingRequests.keySet();
        for (RepositoryType key : keys) {
            response.getWriter()
                    .println(key.toString() + " RemainingRequests : " + "<b>" + remainingRequests.get(key) + "</b>");
            response.getWriter().println("</br>");
        }
    }

    private void checkConnection(HttpServletResponse response, String url, String name)
            throws IOException, MalformedURLException, ProtocolException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("HEAD");
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            response.getWriter().println(name + " connection : " + "<b>" + Constants.FAILURE + "</b>");
            response.getWriter().println("</br>");
        }
        response.getWriter().println(name + " connection : " + "<b>" + Constants.SUCCESS + "</b>");
        response.getWriter().println("</br>");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        // do nothing
    }

}
