/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.loadbalancer.admin.cli.reader.api;

import org.glassfish.loadbalancer.config.LbConfig;

/**
 * Reader class to get information about load balancer configuration.
 *
 * @author Satish Viswanatham
 */
public interface LoadbalancerReader extends BaseReader {

    /**
     * Returns properties of the load balancer.
     * For example response-timeout-in-seconds, reload-poll-interval-in-seconds
     * and https-routing etc.
     *
     * @return PropertyReader[]     array of properties
     */
    public PropertyReader[] getProperties() throws LbReaderException;

    /**
     * Returns the cluster info that are load balanced by this LB.
     *
     * @return ClusterReader        array of cluster readers
     */
    public ClusterReader[] getClusters() throws LbReaderException;

    /**
     * Returns the name of the load balancer
     *
     * @return String               name of the LB
     */
    public String getName() throws LbReaderException;

    /**
     * Returns the lbconfig associated with the load balancer
     *
     * @return LbConfig               lbconfig of the LB
     */
    public LbConfig getLbConfig();

    /*** Supported Attribute names for Load balancer **/
    public static final String RESP_TIMEOUT = "response-timeout-in-seconds";
    public static final String RESP_TIMEOUT_VALUE = "60";
    public static final String RELOAD_INTERVAL =
            "reload-poll-interval-in-seconds";
    public static final String RELOAD_INTERVAL_VALUE = "60";
    public static final String HTTPS_ROUTING = "https-routing";
    public static final String HTTPS_ROUTING_VALUE = "false";
    public static final String REQ_MONITOR_DATA = "require-monitor-data";
    public static final String REQ_MONITOR_DATA_VALUE = "false";
    public static final String ROUTE_COOKIE = "route-cookie-enabled";
    public static final String LAST_EXPORTED = "last-exported";
    public static final String ACTIVE_HEALTH_CHECK_VALUE = "false";
    public static final String NUM_HEALTH_CHECK_VALUE = "3";
    public static final String REWRITE_LOCATION_VALUE = "true";
    public static final String ACTIVE_HEALTH_CHECK = "active-healthcheck-enabled";
    public static final String NUM_HEALTH_CHECK = "number-healthcheck-retries";
    public static final String REWRITE_LOCATION = "rewrite-location";
    public static final String REWRITE_COOKIES = "rewrite-cookies";
    public static final String REWRITE_COOKIES_VALUE = "false";
    public static final String PREFERRED_FAILOVER_INSTANCE = "preferred-failover-instance";
    public static final String PREFERRED_FAILOVER_INSTANCE_VALUE = "true";
    
    //server ref attributes default values
    public static final boolean LBENABLED_VALUE = true;
    public static final String DISABLE_TIMEOUT_IN_MINUTES_VALUE = "30";
}
