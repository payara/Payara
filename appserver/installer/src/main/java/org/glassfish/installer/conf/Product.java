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

/** Holds Product related attributes, currently the installer handles only
 * two products, GlassFish and UpdateTool. There is no need in 3.1 to handle
 * extensive configuration of updatetool, so this class is primarily used only
 * for configuring GlassFish.
 *
 * @author sathyan
 */
public class Product {

    /* Product Name. */
    private String productName;
    /* Installation root directory. */
    private String installLocation;
    /* Path to the main product administration script. For GlassFish it is
     * the reference to asadmin script.
     */
    private String adminScript;
    /* Path to the main product configuration file. For GlassFish it is
     * the reference to asenv file under <installLocation>/config directory.
     */
    private String configFilePath;

    public Product(String productName, String installLocation, String adminScript, String configFilePath) {
        this.productName = productName;
        this.installLocation = installLocation;
        this.adminScript = adminScript;
        this.configFilePath = configFilePath;
    }

    public Product() {
    }

    /* @return String, complete path to the configuration file. */
    public String getConfigFilePath() {
        return configFilePath;
    }

    /* @param configFilePath, configuration file path. */
    public void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    /* @param adminScript, administration script path. */
    public void setAdminScript(String adminScript) {
        this.adminScript = adminScript;
    }

    /* @param installLocation, root directory of the installation. */
    public void setInstallLocation(String installLocation) {
        this.installLocation = installLocation;
    }

    /* @param productName, name of the product. */
    public void setProductName(String productName) {
        this.productName = productName;
    }

    /* @return String, complete path to the product administration script. */
    public String getAdminScript() {
        return adminScript;
    }

    /* @return String, complete path to the installation root directory. */
    public String getInstallLocation() {
        return installLocation;
    }

    /* @return String, name of the product. */
    public String getProductName() {
        return productName;
    }
}
