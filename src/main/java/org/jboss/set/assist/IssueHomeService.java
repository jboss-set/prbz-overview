/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.naming.NameNotFoundException;

import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.spi.IssueHome;
import org.jboss.set.aphrodite.simplecontainer.SimpleContainer;
import org.jboss.set.aphrodite.spi.NotFoundException;

/**
 * @author wangc
 *
 */
public class IssueHomeService implements IssueHome {
    private static final Logger logger = Logger.getLogger(IssueHomeService.class.getCanonicalName());
    private static Aphrodite aphrodite;

    static {
        try {
            aphrodite = SimpleContainer.instance().lookup(Aphrodite.class.getSimpleName(), Aphrodite.class);
        } catch (NameNotFoundException e) {
            logger.log(Level.SEVERE, "Can not get aphrodite service due to : ", e);
        }
    }

    public Stream<Issue> findUpstreamReferences(Issue issue) {
        Set<Issue> upstreamIssues = new HashSet<>();
        if (aphrodite != null) {
            for (URL url : issue.getDependsOn()) {
                try {
                    upstreamIssues.add(aphrodite.getIssue(url));
                } catch (NotFoundException e) {
                    logger.log(Level.WARNING, "Unable to find issue with url: " + url, e);
                }
            }
        }
        return upstreamIssues.stream();
    }
}
