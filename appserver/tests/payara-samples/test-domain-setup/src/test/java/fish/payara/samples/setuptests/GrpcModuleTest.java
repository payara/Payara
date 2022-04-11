/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.samples.setuptests;

import fish.payara.samples.ServerOperations;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * The gRPC module is not built in to Payara and instead should be compiled & placed into the
 * {payara.home}\glassfish\modules directory by the user, as it frequently clashes with customer apps.
 *
 * This setup test takes the gRPC-Support jar file and moves it into Payara before the restart test so gRPC
 * support can be tested. When a new version of gRPC is released, the `grpcSupportJarName` needs updating and
 * the gRPC jar in `src\main\resources` needs replacing with the latest version.
 *
 * @author James Hillyard
 */

@RunWith(Arquillian.class)
public class GrpcModuleTest {

    private static String grpcSupportJarName = "grpc-support-1.0.0.jar";

    private Path pathToPayaraModulesDir;
    private Path pathToGrpcSupportModule;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "grpc-ejb-setup.war")
                .addPackage(GrpcModuleTest.class.getPackage())
                .addPackage(ServerOperations.class.getPackage())
                .addAsResource(new File("src/main/resources/" + grpcSupportJarName));
    }

    @Test
    public void addGrpcModuleToPayaraTest() throws IOException, URISyntaxException {
        if (ServerOperations.isServer()) {
            pathToGrpcSupportModule = Paths.get(Thread.currentThread().getContextClassLoader().getResource(grpcSupportJarName).toURI());
            pathToPayaraModulesDir = Paths.get(System.getProperty("com.sun.aas.installRoot") + File.separator + "modules" + File.separator + grpcSupportJarName);
            Files.copy(pathToGrpcSupportModule, pathToPayaraModulesDir, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}