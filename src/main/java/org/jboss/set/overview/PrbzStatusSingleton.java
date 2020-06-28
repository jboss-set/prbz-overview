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

import javax.ejb.Singleton;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Singleton
public class PrbzStatusSingleton {
    public static final String STARTING = "Starting";
    public static final String REFRESHING = "Refreshing";
    public static final String COMPLETE = "Complete";
    public static final String SCHEDULED = "SCHEDULED";

    private DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private String refreshStatus = STARTING;
    private String lastRefresh = ZonedDateTime.now().format(formatter);

    public void refreshStarted() {
        if (!this.refreshStatus.equals(STARTING)) {
            this.refreshStatus = REFRESHING;
        }
    }

    public void refreshCompleted() {
        this.refreshStatus = COMPLETE;
        lastRefresh = ZonedDateTime.now().format(formatter);
    }

    public void setRefreshScheduled() {
        this.refreshStatus = SCHEDULED;
    }

    public PrbzStatus currentState() {
        return new PrbzStatus(refreshStatus, lastRefresh);
    }
}
