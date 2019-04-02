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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jboss.set.aphrodite.domain.RateLimit;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.jboss.set.overview.ejb.Aider;

/**
 * @author wangc
 *
 */
@Path("/status")
public class Status {

    private static final String JIRA_HOST = "https://issues.jboss.org/";
    private static final String BUGZILLA_HOST = "https://bugzilla.redhat.com/";
    private static final String GITHUB_HOST = "https://github.com/";

    private static Map<RepositoryType, RateLimit> rateLimits;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getStatus(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {
        // connection status
        StringBuilder result = new StringBuilder();
        try {
            result.append(checkConnection(response, JIRA_HOST, Constants.JIRA)).append(System.getProperty("line.separator"));
            result.append(checkConnection(response, BUGZILLA_HOST, Constants.BUGZILLA)).append(System.getProperty("line.separator"));
            result.append(checkConnection(response, GITHUB_HOST, Constants.GITHUB)).append(System.getProperty("line.separator"));
        } catch (MalformedURLException e) {
            // ignored
        }

        // check Github Api rate limit
        rateLimits = Aider.getRateLimits();
        if (rateLimits != null) {
            StringBuilder rate = new StringBuilder();
            Set<RepositoryType> keys = rateLimits.keySet();
            for (RepositoryType key : keys) {
                RateLimit rateLimit = rateLimits.get(key);
                rate.append(key.toString() + " RequestLimit : " + rateLimit.limit);
                rate.append(System.getProperty("line.separator"));
                rate.append(key.toString() + " RemainingRequests : " + rateLimit.remaining);
                rate.append(System.getProperty("line.separator"));
            }
            result.append(rate).append(System.getProperty("line.separator"));
        }
        return result.toString();
    }

    private String checkConnection(HttpServletResponse response, String url, String name)
            throws IOException, MalformedURLException, ProtocolException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("HEAD");
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            return name + " connection : " + Constants.FAILURE;
        }
        return name + " connection : " + Constants.SUCCESS;
    }
}
