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

package org.glassfish.installer.conf;

/** Holds attributes of a GlassFish Domain.
 *
 * @author sathyan
 */
public class Domain {

    /* Name of the domain. */
    private String domainName;

    /* Domain root, default is <InstallDir>/domains. */
    private String domainRoot;

    /* HTTP Port. */
    private String instancePort;

    /* Administration port. */
    private String adminPort;

    /* --domainproperties of create-domain command where the other ports are passed. */
    private String domainProperties;

    /* Default to true, save login information in filesystem. */
    private boolean saveLogin;

    /* Default to false, Ports are not validated to make sure that they are free.
     * Installer does validate admin and http ports.
     */
    private boolean checkPorts;

    /* Administrator username, default is "admin". */
    private String adminUser;

    /* Password for domain. */
    private char[] adminPassword;

    /* Master password. */
    private char[] masterPassword;

    /* List of port numbers currently defaulted by asadmin command. Ports other
     * than http and admin ports.
     */
    private String otherGlassFishPortBases[][] = {
        {"jms.port", "7676"},
        {"domain.jmxPort", "8686"},
        {"orb.listener.port", "3700"},
        {"http.ssl.port", "8181"},
        {"orb.ssl.port", "3820"},
        {"orb.mutualauth.port", "3920"}
    };

    /* List of port numbers currently defaulted by asadmin command. Ports other
     * than http and admin ports.
     * @return String list of ports to be used in the formation of --domainproperties
     */
    public String[][] getGlassfishPortBases() {
        return otherGlassFishPortBases;
    }

    /* @return String Domain Administrator Port */
    public String getAdminPort() {
        return adminPort;
    }

    /* Assign domain's Admininstration port value. */
    public void setAdminPort(String adminPort) {
        this.adminPort = adminPort;
    }

    /* @return true if ports have to be checked for freeness, false otherwise. */
    public boolean isCheckPorts() {
        return checkPorts;
    }

    /* Assign value for checkport. @param checkPorts boolean true/false. */
    public void setCheckPorts(boolean checkPorts) {
        this.checkPorts = checkPorts;
    }

    /* @return String name of the domain. */
    public String getDomainName() {
        return domainName;
    }

    /* @param domainName, name of the domain. */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    /* @return String domainProperties that contains other ports. */
    public String getDomainProperties() {
        return domainProperties;
    }

    /* @param domainProperties */
    public void setDomainProperties(String domainProperties) {
        this.domainProperties = domainProperties;
    }

    /* @return String domain root directory, usually it is <Installdir>/glassfish/domains. */
    public String getDomainRoot() {
        return domainRoot;
    }

    /* @param domainRoot, domain root directory, usually it is <Installdir>/glassfish/domains. */
    public void setDomainRoot(String domainRoot) {
        this.domainRoot = domainRoot;
    }

    /* @return HTTP Port of the domain. */
    public String getInstancePort() {
        return instancePort;
    }

    /* @param instancePort, set HTTP Port of the domain. */
    public void setInstancePort(String instancePort) {
        this.instancePort = instancePort;
    }

    /* @return char[] Domain Administrator password. */
    public char[] getAdminPassword() {
        return adminPassword;
    }

    /* @param adminPassword, Domain Administrator password. */
    public void setAdminPassword(char[] adminPassword) {
        this.adminPassword = adminPassword;
    }

    /* return char[] master password, currently hard-coded to "changeit". */
    public char[] getMasterPassword() {
        return masterPassword;
    }
    /* @param masterPassword, master password to set. */

    public void setMasterPassword(char[] masterPassword) {
        this.masterPassword = masterPassword;
    }

    /* @return true if the login information has to be saved, default to false. */
    public boolean isSaveLogin() {
        return saveLogin;
    }

    /* @param saveLogin, true/false to save login information to .asadminpass file. */
    public void setSaveLogin(boolean saveLogin) {
        this.saveLogin = saveLogin;
    }

    /* @return String domain administrator username. */
    public String getAdminUser() {
        return adminUser;
    }

    /* @param String adminUser. */
    public void setAdminUser(String adminUser) {
        this.adminUser = adminUser;
    }

    public Domain() {
    }

    public Domain(String domainName, String domainRoot, String instancePort, String adminPort, boolean saveLogin, boolean checkPorts, String adminUser, char[] adminPassword, char[] masterPassword) {
        this.domainName = domainName;
        this.domainRoot = domainRoot;
        this.instancePort = instancePort;
        this.adminPort = adminPort;
        this.saveLogin = saveLogin;
        this.checkPorts = checkPorts;
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
        this.masterPassword = masterPassword;
    }
}
