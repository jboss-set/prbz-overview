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

package org.jboss.set.assist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Constants {

    public static final String WILDFLY_WILDFLY = "wildfly-wildfly";
    public static final String WILDFLY_WILDFLY_CORE = "wildfly-wildfly-core";
    public static final String JBOSSAS_WILDFLY_CORE_EAP = "jbossas-wildfly-core-eap";
    public static final String JBOSSAS_JBOSS_EAP7 = "jbossas-jboss-eap7";
    public static final String JBOSSAS_JBOSS_EAP = "jbossas-jboss-eap";

    public static final String GITHUB = "Github";
    public static final String GITLAB = "Gitlab";
    public static final String BUGZILLA = "Bugzilla";
    public static final String JIRA = "Jira";

    public static final String SUCCESS = "Success";
    public static final String FAILURE = "Failure";

    public static final String BUGZILLA_URL_PREFIX = "https://bugzilla.redhat.com/show_bug.cgi?id=";
    public static final String EAP64ZPAYLOAD_ALIAS_PREFIX = "eap";
    public static final String EAP64ZPAYLOAD_ALIAS_SUFFIX = "-payload";

    public static final Pattern RELATED_PR_PATTERN = Pattern
            .compile(".*github\\.com.*?/([a-zA-Z_0-9-]*)/([a-zA-Z_0-9-]*)/pull.?/(\\d+)", Pattern.CASE_INSENSITIVE);

    public static final String PATTERN_SUFFIX = "([0-9]*).GA";

    public static final Pattern BZ_ID_PARAM_PATTERN = Pattern.compile("id=([^&]+)");
    public static final Pattern EAP64ZPAYLOADPATTERN = Pattern.compile("eap([0-9]*)-payload");

    public static final String API_BASE_PATH = "/rest/api/2/";
    public static final String API_ISSUE_PATH = API_BASE_PATH + "issue/";
    public static final String BROWSE_ISSUE_PATH = "/browse/";

    public static final String EAP7_STREAM_TARGET_RELEASE_70ZGA = "7.0.z.GA";
    public static final String EAP7_STREAM_TARGET_RELEASE_71ZGA = "7.1.z.GA";
    public static final String EAP7_STREAM_TARGET_RELEASE_72ZGA = "7.2.z.GA";
    public static final String EAP7_STREAM_TARGET_RELEASE_73ZGA = "7.3.z.GA";
    public static final String EAP7_STREAM_TARGET_RELEASE_7BACKLOGGA = "7.backlog.GA";
//    public static final String EAP7_STREAM_TARGET_RELEASE_710GA = "7.1.0.GA";
//    public static final String EAP7_STREAM_TARGET_RELEASE_720GA = "7.2.0.GA";

    public static final String EAP63ZSTREAM = "jboss-eap-6.3.z";
    public static final String EAP64ZSTREAM = "jboss-eap-6.4.z";
    public static final String EAP70ZSTREAM = "jboss-eap-7.0.z";
    public static final String EAP71ZSTREAM = "jboss-eap-7.1.z";
    public static final String EAP72ZSTREAM = "jboss-eap-7.2.z";
    public static final String EAP73ZSTREAM = "jboss-eap-7.3.z";
    public static final ArrayList<String> EOLSTREAMS = new ArrayList<>(Arrays.asList(EAP63ZSTREAM, EAP70ZSTREAM, EAP71ZSTREAM));
    public static final String WILDFLYSTREAM = "wildfly"; // ignored upstream in streams view
    public static final String EAP_PREFIX = "jboss-eap-";
    public static final String EAP_SUFFIX = ".z";
//    public static final String EAP7Z0STREAM = "jboss-eap-7.z.0";


    public static final String NOTAPPLICABLE = "N/A";

    public static final String DEV_PROFILE = "prbz-dev";
    public static final String RELEASED_DISABLED = "released-disabled";
    public static final String DEV_STREAM = "7.3";
}
