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

package org.jboss.set.overview.assistant.data.payload;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.IssueStatus;
import org.jboss.set.aphrodite.domain.IssueType;

/**
 * @author wangc
 *
 */
public class DependsOnIssue extends PayloadIssue {

    private boolean inPayload;
    private List<String> fixVersions;
    private Map<String, FlagStatus> streamStatus;

    public DependsOnIssue(URL link, String label, IssueStatus status, IssueType type, Map<String, String> flags, boolean inPayload, List<String> fixVersions, Map<String, FlagStatus> streamStatus) {
        super(link, label, status, type, flags);
        this.inPayload = inPayload;
        this.fixVersions = fixVersions;
        this.streamStatus = streamStatus;
    }

    public boolean isInPayload() {
        return inPayload;
    }

    public List<String> getFixVersions() {
        return fixVersions;
    }

    public Map<String, FlagStatus> getStreamStatus() {
        return streamStatus;
    }

}