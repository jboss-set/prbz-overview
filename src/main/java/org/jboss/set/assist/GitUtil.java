/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2020, Red Hat, Inc., and individual contributors
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

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.PullRequest;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.jboss.set.assist.data.payload.FailedPullRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitUtil {

    private static Logger logger = Logger.getLogger(GitUtil.class.getCanonicalName());

    private static String[] prIds = {"/pull/", "/merge_requests/"};
    private static List<Pattern> patterns = new ArrayList<>();

    static {
        for (String id : prIds) {
            patterns.add(Pattern.compile("(.*" + id + "\\d+)/.*"));
        }
    }

    public static boolean isValidPRUrl(String urlString) {
        for (String id : prIds) {
            if (urlString.contains(id)) return true;
        }
        return false;
    }

    public static PullRequest getPullRequest(Aphrodite aphrodite, URL url) {
        String urlString = url.toString();

        if (!isValidPRUrl(urlString)) {
            logger.warning("Invalid pull request url: " + url);
            return new FailedPullRequest(url, "Invalid URL");
        }

        // sometimes a URL contains more than necessary, e.g. '/pull/123/files'
        try {
            for (Pattern pat : patterns) {
                Matcher m = pat.matcher(urlString);
                if (m.find()) {
                    String newUrl = m.group(1);
                    url = new URL(newUrl);
                    break;
                }
            }
        } catch (MalformedURLException mue) {
        }

        try {
            return aphrodite.getPullRequest(url);
        } catch (NotFoundException e) {
            return new FailedPullRequest(url, e.getMessage());
        }
    }
}
