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

package org.jboss.set.assist.evaluator.impl.payload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.naming.NameNotFoundException;

import org.jboss.jbossset.bugclerk.Violation;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.issue.trackers.jira.JiraIssue;
import org.jboss.set.aphrodite.simplecontainer.SimpleContainer;
import org.jboss.set.assist.Constants;
import org.jboss.set.assist.Util;
import org.jboss.set.assist.ViolationHome;
import org.jboss.set.assist.data.payload.PayloadIssue;
import org.jboss.set.assist.evaluator.PayloadEvaluator;
import org.jboss.set.assist.evaluator.PayloadEvaluatorContext;

/**
 * @author wangc
 *
 */
public class PayloadIssueEvaluator implements PayloadEvaluator {

    public static final String KEY = "payloadDependency";
    private static final Logger logger =  Logger.getLogger(PayloadIssueEvaluator.class.getCanonicalName());

    @Override
    public String name() {
        return "PayloadIssue Evaluator";
    }

    @Override
    public void eval(PayloadEvaluatorContext context, Map<String, Object> data) {
        Issue dependencyIssue = context.getIssue();
        List<Violation> violations = new ArrayList<>();
        try {
            violations = SimpleContainer.instance().lookup(ViolationHome.class.getSimpleName(), ViolationHome.class).findViolations(dependencyIssue).collect(Collectors.toList());
        } catch (NameNotFoundException e) {
            logger.log(Level.SEVERE, "Can not get ViolationHome service due to ", e);
        }
        data.put(KEY, new PayloadIssue(dependencyIssue.getURL(),
                dependencyIssue.getTrackerId().orElse(Constants.NOTAPPLICABLE),
                dependencyIssue.getSummary().orElse(Constants.NOTAPPLICABLE), dependencyIssue.getStatus(),
                dependencyIssue.getType(),
                dependencyIssue.getStage().getStateMap().entrySet().stream().collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue()))),
                dependencyIssue.getPriority().name(),
                Util.isAllAcks(dependencyIssue),
                violations));

        if (dependencyIssue instanceof JiraIssue) {
            data.put("incorporatedIssues", ((JiraIssue) dependencyIssue).getLinkedIncorporatesIssues());
        }
    }
}
