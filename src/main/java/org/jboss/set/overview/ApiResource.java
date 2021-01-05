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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.set.aphrodite.domain.VersionUpgrade;
import org.jboss.set.assist.data.ProcessorData;
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
    public Collection<RestIssue> generatePayload(@PathParam("streamName") String streamName, @PathParam("payloadName") String payloadName) {
        List<ProcessorData> payloadData = Aider.getPayloadData(payloadName);

        if (payloadData == null) {
            return Collections.EMPTY_LIST;
        }

        return payloadData.parallelStream()
                .map(d-> RestIssue.from(d))
                .collect(Collectors.toList());
    }

    @POST
    @Path("/refresh")
    @Produces("application/json")
    public PrbzStatus scheduleUpdate() {
        if (status.currentState().getRefreshState().equals(PrbzStatusSingleton.COMPLETE) || status.currentState().getRefreshState().equals(PrbzStatusSingleton.SCHEDULED)) {
            status.setRefreshScheduled();
            aider.scheduleRefresh();
        }

        return status.currentState();
    }

    @POST
    @Path("/refresh/{streamName}/{payloadName}")
    @Produces("application/json")
    public PrbzStatus scheduleSingleUpdate(@PathParam("streamName") String streamName, @PathParam("payloadName") String payloadName) {
        if (status.currentState().getRefreshState().equals(PrbzStatusSingleton.COMPLETE) || status.currentState().getRefreshState().equals(PrbzStatusSingleton.SCHEDULED)) {
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

    @GET
    @Path("/payloads")
    @Produces("application/json")
    public Map<String, Set<String>> availablePayloads() {
        Map<String, Set<String>> res = new HashMap<>();
        for (String stream : Util.jiraPayloadStoresByStream.keySet()) {
            Set<String> payloads = Util.jiraPayloadStoresByStream.get(stream).keySet();
            res.put(stream, payloads);
        }

        return res;
    }

    @GET
    @Path("/upgrades/{comp}/{tag1}/{tag2}")
    @Produces("application/json")
    public List<VersionUpgrade> getComponentUpgrades(@PathParam("comp") String comp, @PathParam("tag1") String tag1, @PathParam("tag2") String tag2) {
        return aider.getComponentUpgrades(comp, tag1, tag2);
    }

    @GET
    @Path("/tags/{comp}")
    @Produces("application/json")
    public List<String> getTags(@PathParam("comp") String comp) {
        return aider.getTagsAndBranches(comp);
    }
}
