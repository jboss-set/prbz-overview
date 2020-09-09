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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.set.assist.data.ProcessorData;
import org.jboss.set.assist.data.payload.AssociatedPullRequest;
import org.jboss.set.assist.data.payload.PayloadIssue;

@XmlRootElement(name = "issue")
public class RestIssue {

    private String summary;
    private URL url;
    private String type;
    private String rawType;
    private Map<String, String> acks = new HashMap<>();
    private String status;
    private String rawStatus;
    private String priority;
    private List<AssociatedPullRequest> pullRequests;
    private List<URL> linkedIncorporatesIssues;

    public static RestIssue from(ProcessorData d) {
        PayloadIssue issue = (PayloadIssue) d.getData().get("payloadDependency");
        List<AssociatedPullRequest> associatedPullRequest = new ArrayList<>((List< AssociatedPullRequest >)d.getData().get("associatedPullRequest"));
        associatedPullRequest.addAll((List< AssociatedPullRequest >)d.getData().get("associatedUnrelatedPullRequest"));
        List<URL> incorporatedIssues = (List<URL>) d.getData().get("incorporatedIssues");

        RestIssue restIssue = new RestIssue();
        restIssue.setSummary(issue.getSummary());
        restIssue.setURL(issue.getLink());
        restIssue.setType(issue.getType().get());
        restIssue.setRawType(issue.getRawType());
        restIssue.setAcks(new HashMap<>(issue.getFlags()));
        restIssue.setStatus(issue.getStatus().toString());
        restIssue.setRawStatus(issue.getRawStatus());
        restIssue.setPriority(issue.getPriority());
        restIssue.setIncorporatedIssues(incorporatedIssues);

        if (associatedPullRequest != null) {
            restIssue.setPullRequest(associatedPullRequest);
        }

        return restIssue;
    }

    private void setPullRequest(List<AssociatedPullRequest> associatedPullRequest) {
        this.pullRequests = associatedPullRequest;
    }

    public List<AssociatedPullRequest> getPullRequest() {
        return pullRequests;
    }

    private void setIncorporatedIssues(List<URL> linkedIncorporatesIssues) {
        this.linkedIncorporatesIssues = linkedIncorporatesIssues;
    }

    public List<URL> getLinkedIncorporatesIssues() {
        return linkedIncorporatesIssues;
    }

    private void setPriority(String priority) {
        this.priority = priority;
    }

    public String getPriority() {
        return priority;
    }

    private void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    private void setRawStatus(String rawStatus) {
        this.rawStatus = rawStatus;
    }

    public String getRawStatus() {
        return rawStatus;
    }

    private void setAcks(Map<String, String> acks) {
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

    private void setRawType(String rawType) {
        this.rawType = rawType;
    }

    public String getRawType() {
        return rawType;
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
