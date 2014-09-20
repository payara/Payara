/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.resourcebase.resources;

/**
 * Created with IntelliJ IDEA.
 * User: naman
 * Date: 3/1/13
 * Time: 11:10 AM
 * To change this template use File | Settings | File Templates.
 */
public enum ResourceDeploymentOrder {

    /*
    The number indicates the deployment order for particular resources.

    To add new resource order add constant here and give the deployment order number for that resource.
    Define @ResourceTypeOrder(deploymentOrder=ResourceDeploymentOrder.<your resource>) for your resource. For example
    check JdbcResource class.
     */

    JDBC_RESOURCE(1) , JDBC_POOL(2), CONNECTOR_RESOURCE(3), CONNECTOR_POOL(4), ADMIN_OBJECT_RESOURCE(5),
    DIAGNOSTIC_RESOURCE(6), MAIL_RESOURCE(7), CUSTOM_RESOURCE(8), EXTERNALJNDI_RESOURCE(9),
    RESOURCEADAPTERCONFIG_RESOURCE(10), WORKSECURITYMAP_RESOURCE(11), PERSISTENCE_RESOURCE(12), CONTEXT_SERVICE(13), MANAGED_THREAD_FACTORY(14), MANAGED_EXECUTOR_SERVICE(15), MANAGED_SCHEDULED_EXECUTOR_SERVICE(16);

    private int value;

    private ResourceDeploymentOrder(int value) {
        this.value = value;
    }

    public int getResourceDeploymentOrder() {
        return value;
    }
};
