/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

package fish.payara.docker;

/**
 * Constants related to the management of Docker Instances and Nodes.
 *
 * @author Andrew Pielage
 */
public class DockerConstants {

    public static final String DOCKER_IMAGE_KEY = "Image";
    public static final String DOCKER_NAME_KEY = "name";
    public static final String PAYARA_DAS_HOST = "PAYARA_DAS_HOST";
    public static final String PAYARA_DAS_PORT = "PAYARA_DAS_PORT";
    public static final String PAYARA_NODE_NAME = "PAYARA_NODE_NAME";
    public static final String PAYARA_INSTALL_DIR = "/opt/payara/payara6";
    public static final String DOCKER_HOST_CONFIG_KEY = "HostConfig";
    public static final String DOCKER_MOUNTS_KEY = "Mounts";
    public static final String DOCKER_MOUNTS_TYPE_KEY = "Type";
    public static final String DOCKER_MOUNTS_SOURCE_KEY = "Source";
    public static final String DOCKER_MOUNTS_TARGET_KEY = "Target";
    public static final String DOCKER_MOUNTS_READONLY_KEY = "ReadOnly";
    public static final String DOCKER_NETWORK_MODE_KEY = "NetworkMode";
    public static final String PAYARA_PASSWORD_FILE = "/opt/payara/passwords/passwordfile.txt";
    public static final String PAYARA_INSTANCE_NAME = "PAYARA_INSTANCE_NAME";
    public static final String DOCKER_CONTAINER_ENV = "Env";
    public static final String DEFAULT_IMAGE_NAME = "payara/server-node";
    public static final String DOCKER_FROM_IMAGE_KEY = "fromImage";
    public static final String PAYARA_CONFIG_NAME = "PAYARA_CONFIG_NAME";
}
