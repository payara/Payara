/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.lbplugin;

/**
 *
 * @author kshitiz
 */
public class Constants {

    public static final String LB = "HTTP_LOAD_BALANCER";
    public static final String SERVICE_TYPE_PROP_NAME = "service-type";
    public static final String LB_TEMPLATE_NAME_PROP_NAME = "lb-template-name";
    public static final String HTTP_PORT_PROP_NAME = "http-port";
    public static final String SSL_ENABLED_PROP_NAME = "ssl-enabled";
    public static final String HTTPS_PORT_PROP_NAME = "https-port";
    public static final String DEFAULT_HTTP_PORT = "50080";
    public static final String DEFAULT_SSL_ENABLED = "false";
    public static final String DEFAULT_HTTPS_PORT = "50443";
    public static final String ServiceTypeLB =
            org.glassfish.virtualization.util.ServiceType.Type.LB.name();
    public static final String HTTP_PROTOCOL = "http";
    public static final String HTTPS_PROTOCOL = "https";
    public static final String IP_ADDRESS_PROP_NAME = "ip-address";

}
