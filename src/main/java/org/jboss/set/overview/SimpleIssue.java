/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.set.overview;

import javax.xml.bind.annotation.XmlRootElement;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;

@XmlRootElement(name = "issue")
public class SimpleIssue {

    private String summary;
    private URL url;
    private String type;
    private Map<String, String> acks = new HashMap<>();
    private String status;
    private String priority;

    public static SimpleIssue from(Issue issue) {
        SimpleIssue simpleIssue = new SimpleIssue();
        simpleIssue.setSummary(issue.getSummary().get());
        simpleIssue.setURL(issue.getURL());
        simpleIssue.setType(issue.getType().get());
        Map<Flag, FlagStatus> stateMap = issue.getStage().getStateMap();
        Map<String, String> acks = new HashMap<>();
        for (Flag flag : stateMap.keySet()) {
            FlagStatus flagStatus = stateMap.get(flag);
            acks.put(flag.name(), flagStatus.getSymbol());
        }
        simpleIssue.putAcks(acks);
        simpleIssue.putStatus(issue.getStatus().name());
        simpleIssue.putPriority(issue.getPriority().name());

        return simpleIssue;
    }

    private void putPriority(String priority) {
        this.priority = priority;
    }

    public String getPriority() {
        return priority;
    }

    private void putStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    private void putAcks(Map<String, String> acks) {
        this.acks = acks;
    }

    public Map<String, String> getAcks() {
        return acks;
    }

    private void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    private void setURL(URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
