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

package org.jboss.set.assist.data.payload;

import static org.jboss.set.assist.Util.maxSeverity;

import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.jboss.jbossset.bugclerk.Severity;
import org.jboss.jbossset.bugclerk.Violation;
import org.jboss.set.aphrodite.domain.IssueStatus;
import org.jboss.set.aphrodite.domain.IssueType;

/**
 * @author wangc
 *
 */
public class PayloadIssue {
    private URL link;
    private String label;
    private String summary;
    private IssueStatus status;
    private String rawStatus;
    private IssueType type;
    private String rawType;
    private Map<String, String> flags;
    private String priority;
    private String assignee;
    private boolean allAcks;
    private Collection<Violation> violations;
    private Severity maxSeverity;

    public PayloadIssue(URL link, String label, String summary, IssueStatus status,
                        String rawStatus, IssueType type, String rawType,
                        Map<String, String> flags, String priority, String assignee,
                        boolean allAcks, Collection<Violation> violations) {
        this.link = link;
        this.label = label;
        this.summary = summary;
        this.status = status;
        this.rawStatus = rawStatus;
        this.type = type;
        this.rawType = rawType;
        this.flags = flags;
        this.priority = priority;
        this.assignee = assignee;
        this.allAcks = allAcks;
        this.violations = violations;
        this.maxSeverity = violations.stream().map(violation -> violation.getLevel()).reduce((severity1, severity2) -> maxSeverity(severity1, severity2)).orElse(null);
    }

    public URL getLink() {
        return link;
    }

    public String getLabel() {
        return label;
    }

    public String getSummary() {
        return summary.replace("\"", "&quot;"); // JBEAP-14398 double quotes is in summary
    }

    public IssueStatus getStatus() {
        return status;
    }

    public String getRawStatus() {
        return rawStatus;
    }

    public IssueType getType() {
        return type;
    }

    public String getRawType() {
        return rawType;
    }

    public Map<String, String> getFlags() {
        return flags;
    }

    public String getPriority() {
        return priority;
    }

    public boolean isAllAcks() {
        return allAcks;
    }

    public Collection<Violation> getViolations() {
        return violations;
    }

    public Severity getMaxSeverity() {
        return maxSeverity;
    }

    public String getAssignee() {
        return assignee;
    }
}