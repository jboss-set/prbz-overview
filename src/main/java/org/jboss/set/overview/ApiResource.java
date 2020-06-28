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

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.overview.ejb.Aider;

@Path("/api")
public class ApiResource {

    @Inject
    private Aider aider;
    @Inject
    private PrbzStatusSingleton status;

    @GET
    @Path("/payload/{streamName}/{payloadName}")
    @Produces("application/json")
    public Collection<SimpleIssue> generatePayload(@PathParam("streamName") String streamName, @PathParam("payloadName") String payloadName) {

        List<Issue> issues = Util.jiraPayloadStoresByStream.get(streamName).get(payloadName);

        return issues.parallelStream().map(SimpleIssue::from).collect(Collectors.toList());
    }

    @POST
    @Path("/refresh")
    @Produces("application/json")
    public PrbzStatus scheduleUpdate() {
        if (status.currentState().getRefreshState().equals(PrbzStatusSingleton.COMPLETE)) {
            status.setRefreshScheduled();
            aider.scheduleRefresh();
        }

        return status.currentState();
    }

    @POST
    @Path("/refresh/{streamName}/{payloadName}")
    @Produces("application/json")
    public PrbzStatus scheduleSingleUpdate(@PathParam("streamName") String streamName, @PathParam("payloadName") String payloadName) {
        if (status.currentState().getRefreshState().equals(PrbzStatusSingleton.COMPLETE)) {
            status.setRefreshScheduled();
            aider.scheduleRefresh(streamName, payloadName);
        }

        return status.currentState();
    }

    @GET
    @Path("/status")
    @Produces("application/json")
    public PrbzStatus freshnessStatus() {
        return status.currentState();
    }
}
