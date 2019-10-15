/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.test.containers.tools.container;

import java.io.File;

/**
 * Configuration for the {@link PayaraServerContainer}
 *
 * @author David Matějček
 */
public class PayaraServerContainerConfiguration extends JavaContainerConfiguration {

    private static final String JACOCO_DOCKER_PAYARA_SERVER_EXEC_FILE = "jacoco-docker-payara-server.exec";


    @Override
    public File getMainApplicationDirectoryInDocker() {
        return new File("/payara5");
    }


    /**
     * @return output file in docker container (path to the
     *         {@value #JACOCO_DOCKER_PAYARA_SERVER_EXEC_FILE})
     */
    @Override
    public File getJaCoCoReportFileInDocker() {
        return new File(getJaCoCoReportDirectoryInDocker(), JACOCO_DOCKER_PAYARA_SERVER_EXEC_FILE);
    }


    /**
     * @return absolute path to the asadmin command
     */
    public File getAsadminFileInDocker() {
        return new File(getMainApplicationDirectoryInDocker(), "/bin/asadmin");
    }


    /**
     * @return absolute path to the passwordfile.txt
     */
    public File getPasswordFileInDocker() {
        return new File("/payara5/glassfish/domains/domain1/config/passwordfile.txt");
    }


    /**
     * @return absolute path to the passwordfile used to change the default empty password
     */
    public File getPasswordFileForChangeInDocker() {
        return new File("/payara5/glassfish/domains/domain1/config/passwordfile-change.txt");
    }
}
