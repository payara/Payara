/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.admin;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import java.util.logging.Logger;

/**
 * Provides the logging facilities.
 * 
 * @author Shing Wai Chan
 */
public class LogFacade {
    @LogMessagesResourceBundle
    private static final String SHARED_LOGMESSAGE_RESOURCE =
            "org.glassfish.web.admin.monitor.LogMessages";

    @LoggerInfo(subsystem="WEB", description="WEB Admin Logger", publish=true)
    private static final String WEB_ADMIN_LOGGER = "javax.enterprise.web.admin";

    private static final Logger LOGGER =
            Logger.getLogger(WEB_ADMIN_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    private LogFacade() {}

    public static Logger getLogger() {
        return LOGGER;
    }

    private static final String prefix = "AS-WEB-ADMIN-";

    @LogMessageInfo(
            message = "Unable to register StatsProvider {0} with Monitoring Infrastructure. No monitoring data will be collected for {1} and {2}",
            level = "SEVERE",
            cause = "Current server config is null",
            action = "Verify if the server instance is started correctly")
    public static final String UNABLE_TO_REGISTER_STATS_PROVIDERS = prefix + "00001";

    @LogMessageInfo(
            message = "Current server config is null",
            level = "INFO")
    public static final String NULL_CONFIG = prefix + "00002";

    @LogMessageInfo(
            message = "The acceptor threads must be at least 1",
            level = "INFO")
    public static final String ACCEPTOR_THREADS_TOO_LOW = prefix + "00003";

    @LogMessageInfo(
            message = "Listener {0} could not be created, actual reason: {1}",
            level = "INFO")
    public static final String CREATE_HTTP_LISTENER_FAIL = prefix + "00004";

    @LogMessageInfo(
            message = "A default virtual server is required.  Please use --default-virtual-server to specify this value.",
            level = "INFO")
    public static final String CREATE_HTTP_LISTENER_VS_BLANK = prefix + "00005";

    @LogMessageInfo(
            message = "--defaultVS and --default-virtual-server conflict.  Please use only --default-virtual-server to specify this value.",
            level = "INFO")
    public static final String CREATE_HTTP_LISTENER_VS_BOTH_PARAMS = prefix + "00006";

    @LogMessageInfo(
            message = "Attribute value (default-virtual-server = {0}) is not found in list of virtual servers defined in config.",
            level = "INFO")
    public static final String CREATE_HTTP_LISTENER_VS_NOTEXISTS = prefix + "00007";

    @LogMessageInfo(
            message = "Http Listener named {0} already exists.",
            level = "INFO")
    public static final String CREATE_HTTP_LISTENER_DUPLICATE = prefix + "00008";

    @LogMessageInfo(
            message = "Port [{0}] is already taken for address [{1}], please choose another port.",
            level = "INFO")
    public static final String PORT_IN_USE = prefix + "00009";

    @LogMessageInfo(
            message = "Network Listener named {0} already exists.",
            level = "INFO")
    public static final String CREATE_NETWORK_LISTENER_FAIL_DUPLICATE = prefix + "00010";

    @LogMessageInfo(
            message = "Protocol {0} has neither a protocol nor a port-unification configured.",
            level = "INFO")
    public static final String CREATE_NETWORK_LISTENER_FAIL_BAD_PROTOCOL = prefix + "00011";

    @LogMessageInfo(
            message = "{0} create failed:",
            level = "INFO")
    public static final String CREATE_NETWORK_LISTENER_FAIL = prefix + "00012";

    @LogMessageInfo(
            message = "The specified protocol {0} is not yet configured.",
            level = "INFO")
    public static final String CREATE_HTTP_FAIL_PROTOCOL_NOT_FOUND = prefix + "00013";

    @LogMessageInfo(
            message = "Failed to create http-redirect for {0}: {1}.",
            level = "INFO")
    public static final String CREATE_HTTP_REDIRECT_FAIL = prefix + "00014";

    @LogMessageInfo(
            message = "An http element for {0} already exists. Cannot add duplicate http.",
            level = "INFO")
    public static final String CREATE_HTTP_FAIL_DUPLICATE = prefix + "00015";

    @LogMessageInfo(
            message = "An http-redirect element for {0} already exists. Cannot add duplicate http-redirect.",
            level = "INFO")
    public static final String CREATE_HTTP_REDIRECT_FAIL_DUPLICATE = prefix + "00016";

    @LogMessageInfo(
            message = "{0} protocol already exists. Cannot add duplicate protocol.",
            level = "INFO")
    public static final String CREATE_PROTOCOL_FAIL_DUPLICATE = prefix + "00017";

    @LogMessageInfo(
            message = "Failed to create protocol {0}.",
            level = "INFO")
    public static final String CREATE_PROTOCOL_FAIL = prefix + "00018";

    @LogMessageInfo(
            message = "{0} create failed: {1}.",
            level = "INFO")
    public static final String CREATE_PORTUNIF_FAIL = prefix + "00019";

    @LogMessageInfo(
            message = "{0} create failed.  Given class is not a ProtocolFilter: {1}.",
            level = "INFO")
    public static final String CREATE_PORTUNIF_FAIL_NOTFILTER = prefix + "00020";

    @LogMessageInfo(
            message = "{0} create failed.  Given class is not a ProtocolFinder: {1}.",
            level = "INFO")
    public static final String CREATE_PORTUNIF_FAIL_NOTFINDER = prefix + "00021";

    @LogMessageInfo(
            message = "{0} transport already exists. Cannot add duplicate transport.",
            level = "INFO")
    public static final String CREATE_TRANSPORT_FAIL_DUPLICATE = prefix + "00022";

    @LogMessageInfo(
            message = "Failed to create transport {0}.",
            level = "INFO")
    public static final String CREATE_TRANSPORT_FAIL = prefix + "00023";

    @LogMessageInfo(
            message = "Please use only networklisteners.",
            level = "INFO")
    public static final String CREATE_VIRTUAL_SERVER_BOTH_HTTP_NETWORK = prefix + "00024";

    @LogMessageInfo(
            message = "Virtual Server named {0} already exists.",
            level = "INFO")
    public static final String CREATE_VIRTUAL_SERVER_DUPLICATE = prefix + "00025";

    @LogMessageInfo(
            message = "{0} create failed.",
            level = "INFO")
    public static final String CREATE_VIRTUAL_SERVER_FAIL = prefix + "00026";

    @LogMessageInfo(
            message = "Specified http listener, {0}, doesn''t exist.",
            level = "INFO")
    public static final String DELETE_HTTP_LISTENER_NOT_EXISTS = prefix + "00028";

    @LogMessageInfo(
            message = "{0} delete failed.",
            level = "INFO")
    public static final String DELETE_HTTP_LISTENER_FAIL = prefix + "00029";

    @LogMessageInfo(
            message = "{0} Network Listener doesn't exist.",
            level = "INFO")
    public static final String DELETE_NETWORK_LISTENER_NOT_EXISTS = prefix + "00030";

    @LogMessageInfo(
            message = "Deletion of NetworkListener {0} failed.",
            level = "INFO")
    public static final String DELETE_NETWORK_LISTENER_FAIL = prefix + "00031";

    @LogMessageInfo(
            message = "{0} http-redirect doesn't exist.",
            level = "INFO")
    public static final String DELETE_HTTP_NOTEXISTS = prefix + "00032";

    @LogMessageInfo(
            message = "Deletion of http {0} failed.",
            level = "INFO")
    public static final String DELETE_HTTP_FAIL = prefix + "00033";

    @LogMessageInfo(
            message = "Deletion of http-redirect {0} failed.",
            level = "INFO")
    public static final String DELETE_HTTP_REDIRECT_FAIL = prefix + "00034";

    @LogMessageInfo(
            message = "{0} protocol doesn't exist.",
            level = "INFO")
    public static final String DELETE_PROTOCOL_NOT_EXISTS = prefix + "00035";

    @LogMessageInfo(
            message = "{0} protocol is being used in the network listener {1}.",
            level = "INFO")
    public static final String DELETE_PROTOCOL_BEING_USED = prefix + "00036";

    @LogMessageInfo(
            message = "Deletion of Protocol {0} failed.",
            level = "INFO")
    public static final String DELETE_PROTOCOL_FAIL = prefix + "00037";

    @LogMessageInfo(
            message = "{0} delete failed: {1}.",
            level = "INFO")
    public static final String DELETE_FAIL = prefix + "00038";

    @LogMessageInfo(
            message = "No {0} element found with the name {1}.",
            level = "INFO")
    public static final String NOT_FOUND = prefix + "00039";

    @LogMessageInfo(
            message = "{0} transport is being used in the network listener {1}.",
            level = "INFO")
    public static final String DELETE_TRANSPORT_BEINGUSED = prefix + "00040";

    @LogMessageInfo(
            message = "Deletion of Transport {0} failed.",
            level = "INFO")
    public static final String DELETE_TRANSPORT_FAIL = prefix + "00041";

    @LogMessageInfo(
            message = "{0} transport doesn''t exist.",
            level = "INFO")
    public static final String DELETE_TRANSPORT_NOT_EXISTS = prefix + "00042";

    @LogMessageInfo(
            message = "{0} delete failed.",
            level = "INFO")
    public static final String DELETE_VIRTUAL_SERVER_FAIL = prefix + "00043";

    @LogMessageInfo(
            message = "Specified virtual server, {0}, doesn''t exist.",
            level = "INFO")
    public static final String DELETE_VIRTUAL_SERVER_NOT_EXISTS = prefix + "00044";

    @LogMessageInfo(
            message = "Specified virtual server, {0}, can not be deleted because it is referenced from http listener, {1}.",
            level = "INFO")
    public static final String DELETE_VIRTUAL_SERVER_REFERENCED = prefix + "00045";

    @LogMessageInfo(
            message = "Monitoring Registry does not exist. Possible causes are 1) Monitoring is not turned on or at a lower level 2) The corresponding container (web, ejb, etc.) is not loaded yet",
            level = "INFO")
    public static final String MRDR_NULL = prefix + "00046";
}
