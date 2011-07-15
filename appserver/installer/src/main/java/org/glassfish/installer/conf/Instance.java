/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.installer.conf;

/** GlassFish Instance, no differentiation is done between a clustered
 * and a standalone instance now. In case of a standalone instance the
 * clustername will be set to null.
 *
 * @author sathyan
 */
public class Instance {

    /* Name of the instance. */
    protected String instanceName;

    /* Name of the Host where a Domain Admin. Server is pre-installed.*/
    protected String serverHostName;

    /* Port number in the Host where a Domain Admin. Server is pre-installed,
     * that can be used for connection.
     */
    protected String serverAdminPort;

    /* Cluster name, null in case of standalone instance. */
    protected String clusterName;

    /* @return String, name of the cluster. */
    public String getClusterName() {
        return clusterName;
    }

    /* @param clusterName, name of the cluster to create/manipulate. */
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    /* @return String, name of the instance. */
    public String getInstanceName() {
        return instanceName;
    }

    /* param instanceName, instance name. */
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /* @return String, Admin port on DAS Host. */
    public String getServerAdminPort() {
        return serverAdminPort;
    }

    /* @param serverAdminPort, Admin port on DAS Host to connect. */
    public void setServerAdminPort(String serverAdminPort) {
        this.serverAdminPort = serverAdminPort;
    }

    /* @return String, name of the DAS Host. */
    public String getServerHostName() {
        return serverHostName;
    }

    /* @param serverHostName, name of the host where DAS is pre-installed. */
    public void setServerHostName(String serverHostName) {
        this.serverHostName = serverHostName;
    }
}
