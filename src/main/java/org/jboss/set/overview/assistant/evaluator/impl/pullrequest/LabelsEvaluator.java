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

package org.jboss.set.overview.assistant.evaluator.impl.pullrequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.overview.assistant.data.LabelData;
import org.jboss.set.overview.assistant.evaluator.PullRequestEvaluator;
import org.jboss.set.overview.assistant.evaluator.PullRequestEvaluatorContext;
import org.jboss.set.overview.assistant.evaluator.Utils;

/**
 * @author egonzalez
 *
 */
public class LabelsEvaluator implements PullRequestEvaluator {

    private static Logger logger = Logger.getLogger("org.jboss.pull");

    @Override
    public String name() {
        return "Labels evaluator";
    }

    @Override
    public void eval(PullRequestEvaluatorContext context, Map<String, Object> data) {

        Patch patch = context.getPatch();
        Set<Issue> issues = context.getIssues();

        // if there aren't any bug related then we show a message
        if (issues.isEmpty()) {
            logger.log(Level.WARNING, "No issues found in patch, " + name() + " not applied to " + patch.getURL());
        }

        Map<String, List<LabelData>> labels = new HashMap<>();
        Map<String, Integer> okays = new HashMap<>();
        data.put("labels", labels);
        data.put("status", okays);
        for (Issue issue : issues) {
            List<LabelData> tmp = new ArrayList<>();
            labels.put(issue.getTrackerId().get(), tmp);

            boolean hasAllFlags = true;
            for (Flag flag : Flag.values()) {
                FlagStatus status = issue.getStage().getStatus(flag);
                if (!status.equals(FlagStatus.ACCEPTED)) {
                    hasAllFlags = false;
                    break;
                }
            }
            boolean hasStreams = !Utils.getStreams(issue).isEmpty();
            if (hasStreams) {
                okays.put(issue.getTrackerId().get(), hasAllFlags ? 1 : 3);
            } else {
                okays.put(issue.getTrackerId().get(), 2);
            }

            tmp.add(new LabelData(context.getBranch(), true));
            tmp.add(new LabelData("Has All Acks", hasAllFlags));
            for (Flag flag : Flag.values()) {
                String label = null;
                switch (flag) {
                    case DEV:
                        label = "Needs devel_ack";
                        break;
                    case QE:
                        label = "Needs qa_ack";
                        break;
                    case PM:
                        label = "Needs pm_ack";
                        break;
                }
                FlagStatus status = issue.getStage().getStatus(flag);
                tmp.add(new LabelData(label, !status.equals(FlagStatus.ACCEPTED)));
            }
        }
    }
}