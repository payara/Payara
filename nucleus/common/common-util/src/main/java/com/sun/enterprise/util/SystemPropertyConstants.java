/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.util;

import com.sun.enterprise.util.i18n.StringManager;
import java.io.File;

public class SystemPropertyConstants
{

    /**
     * Field used by Monitoring to encode a forward slash and/or dot
     */
    public static final String SLASH = "___SLASH___";
    public static final String MONDOT = "___MONDOT___";

    /**
     * Field
     */
    public static final String UNIX_ASENV_FILENAME = "asenv.conf";

    /**
     * Field 
     */
    public static final String WINDOWS_ASENV_FILENAME = "asenv.bat";

    /**
     * Field 
     */
    public static final String WEB_SERVICES_LIB_PROPERTY =
        "com.sun.aas.webServicesLib";

    /**
     * Field 
     */
    public static final String PERL_ROOT_PROPERTY = "com.sun.aas.perlRoot";

    /**
     * Field 
     */
    public static final String IMQ_LIB_PROPERTY = "com.sun.aas.imqLib";

    /**
     * Field 
     */
    public static final String IMQ_BIN_PROPERTY = "com.sun.aas.imqBin";

    /**
     * Field 
     */
    public static final String CONFIG_ROOT_PROPERTY = "com.sun.aas.configRoot";

    /**
     * Field 
     */
    public static final String INSTALL_ROOT_PROPERTY =
        "com.sun.aas.installRoot";

    /**
     * Field
     */
    public static final String PRODUCT_ROOT_PROPERTY =
        "com.sun.aas.productRoot";

    /**
     * Field 
     */
    public static final String JAVA_ROOT_PROPERTY = "com.sun.aas.javaRoot";
    public static final String JAVA_ROOT_PROPERTY_ASENV = "com.sun.aas.javaRoot.asenv";

    /**
     * Field 
     */
    public static final String ICU_LIB_PROPERTY = "com.sun.aas.icuLib";

    /**
     * Field 
     */
    public static final String DEFAULT_LOCALE_PROPERTY =
        "com.sun.aas.defaultLocale";

    /**
     * Field 
     */
    public static final String DOMAINS_ROOT_PROPERTY =
        "com.sun.aas.domainsRoot";

    /**
     * Field 
     */
    public static final String INSTANCE_ROOT_PROPERTY =
        "com.sun.aas.instanceRoot";

    /**
     * The certificate nick name specified in the System-Jmx-Conenctor
     * of the DAS with which a Node Agent synchronizes
     */
    public static final String AGENT_CERT_NICKNAME =
        "com.sun.aas.agentCertNickname";
    
    public static final String AGENT_ROOT_PROPERTY =
        "com.sun.aas.agentRoot";
    
    public static final String AGENT_NAME_PROPERTY =
        "com.sun.aas.agentName";

    /**
     * Field 
     */
    public static final String WEBCONSOLE_LIB_PROPERTY =
        "com.sun.aas.webconsoleLib";
    public static final String WEBCONSOLE_APP_PROPERTY =
        "com.sun.aas.webconsoleApp";

    public static final String JATO_ROOT_PROPERTY =
        "com.sun.aas.jatoRoot";

    public static final String ANT_ROOT_PROPERTY = "com.sun.aas.antRoot";

    public static final String ANT_LIB_PROPERTY = "com.sun.aas.antLib";
    
    public static final String JHELP_ROOT_PROPERTY = "com.sun.aas.jhelpRoot";

    /** name of the server instance key */
    public static final String SERVER_NAME = "com.sun.aas.instanceName";

    /** name of the server's cluster */
    public static final String CLUSTER_NAME = "com.sun.aas.clusterName";

    /** name of the HADB location property **/
    public static final String HADB_ROOT_PROPERTY = "com.sun.aas.hadbRoot";

        public static final String NSS_ROOT_PROPERTY = "com.sun.aas.nssRoot";

    public static final String NSS_BIN_PROPERTY = "com.sun.aas.nssBin";        

    public static final String NATIVE_LAUNCHER = "com.sun.aas.nativeLauncher";
    public static final String NATIVE_LAUNCHER_LIB_PREFIX = "com.sun.aas.nativeLauncherLibPrefix";       
    
    public static final String KEYSTORE_PROPERTY = "javax.net.ssl.keyStore";
    public static final String JKS_KEYSTORE = 
        System.getProperty("file.separator") + "config" +
        System.getProperty("file.separator") + "keystore.jks";

    public static final String TRUSTSTORE_PROPERTY = "javax.net.ssl.trustStore";
    public static final String JKS_TRUSTSTORE = 
        System.getProperty("file.separator") + "config" +
        System.getProperty("file.separator") + "cacerts.jks";
   
    public static final String ADMIN_REALM = "admin-realm"; 
    public static final String NSS_DB_PROPERTY = "com.sun.appserv.nss.db";

    public static final String NSS_DB_PASSWORD_PROPERTY = "com.sun.appserv.nss.db.password";

    public static final String CLIENT_TRUSTSTORE_PROPERTY = 
        TRUSTSTORE_PROPERTY;
        //"com.sun.appserv.client.truststore";

    public static final String CLIENT_TRUSTSTORE_PASSWORD_PROPERTY =
        "javax.net.ssl.trustStorePassword";
        //"com.sun.appserv.client.truststore.password";
    
    public static final String PID_FILE = ".__com_sun_appserv_pid";
    public static final String REF_TS_FILE = "admsn";
    
    public static final String KILLSERV_SCRIPT = "killserv";    
    
    public static final String KILL_SERV_UNIX = "killserv";
    public static final String KILL_SERV_WIN = "killserv.bat";
    public static final String KILL_SERV_OS     = 
        OS.isWindows() ? KILL_SERV_WIN : KILL_SERV_UNIX;

    @Deprecated
    public static final String DEFAULT_SERVER_INSTANCE_NAME = "server";

    public static final String DAS_SERVER_NAME = "server";
    public static final String DAS_SERVER_CONFIG = "server-config";

    public static final String JDMK_HOME_PROPERTY="com.sun.aas.jdmkHome";
    
    public static final String DERBY_ROOT_PROPERTY="com.sun.aas.derbyRoot";

    /** Java ES Monitoring Framework install directory */
    public static final String MFWK_HOME_PROPERTY="com.sun.aas.mfwkHome";
 
    /* An implementation note: This variable should be defined at one place.
     * I have chosen this location because most of the other modules depend
     * on appserv-commons for compilation.
     */
    /** name of the domain key */
    public static final String DOMAIN_NAME = "domain.name";
    public static final String HOST_NAME_PROPERTY = "com.sun.aas.hostName";
    public static final String CONFIG_NAME_PROPERTY = "com.sun.aas.configName";
    public static final String DOCROOT_PROPERTY = "docroot";
    public static final String ACCESSLOG_PROPERTY = "accesslog";
    public static final String DEFAULT_SERVER_SOCKET_ADDRESS="0.0.0.0";
    public static final String CLUSTER_AWARE_FEATURE_FACTORY_CLASS 
        = "com.sun.enterprise.ee.server.pluggable.EEPluggableFeatureImpl";
    public static final String DROP_INTERRUPTED_COMMANDS = "org.glassfish.job-manager.drop-interrupted-commands";
    
    /** Name of the default config that determines the configuration for the instances */
    public static final String TEMPLATE_CONFIG_NAME = "default-config";
    public static final String DEFAULT_ADMIN_USER = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "";
    
    private static final StringManager sm = StringManager.getManager(SystemPropertyConstants.class);


    /** A method that returns the passed String as a property that can
     * be replaced at run time.
     * @param name String that represents a property, e.g INSTANCE_ROOT_PROPERTY
     * in this class. The String may not be null.
     * @return a String that represents the replaceable value of passed String. Generally
     * speaking it will be decorated with a pair of braces with $ in the front (e.g. "a" will be returned as "${a}").
     * @throws IllegalArgumentException if the passed String is null
     */
	public static final String getPropertyAsValue(final String name) {
        if (name == null) {
            final String pn = "spc.null_name";
            final String pv = "property";
            throw new IllegalArgumentException(sm.getString(pn, pv));
        }
        final StringBuffer sb   = new StringBuffer();
        sb.append(OPEN).append(name).append(CLOSE);
        return ( sb.toString() );
	}
        
        public static final String OPEN       = "${";
        public static final String CLOSE      = "}";
        
        /** Returns the string removing the "system-property syntax" from it.
         * If the given string is not in "system-property syntax" the same string is returned.
         * The "system-propery syntax" is "${...}"
         * The given String may not be null.
         * The returned String may be an empty String, if it is of the form "${}" (rarely so).
         */
        public static final String unSystemProperty(final String sp) {
            if (sp == null) 
                throw new IllegalArgumentException ("null_arg");
            String ret = sp;
            if (isSystemPropertySyntax(sp)) {
                ret = sp.substring(2, sp.length() - 1);
            }
            return ( ret );
        }
        
        public static final boolean isSystemPropertySyntax(final String s) {
            if ( s == null)
                throw new IllegalArgumentException ("null_arg");
            boolean sp = false;
            if (s.startsWith(OPEN) && s.endsWith(CLOSE))
                sp = true;
            return ( sp );
        }
    
    /** Returns the default value (as would appear in the domain.xml on installation)
     * of docroot of a virtual server, as a String. Never returns a null.
     * Returned String contains no backslashes.
     * Note that it is <b> not <b> the absolute value of the path on a file system.
     */
    public static final String getDocRootDefaultValue() {
        final StringBuffer sb = new StringBuffer(getPropertyAsValue(INSTANCE_ROOT_PROPERTY));
        return ( sb.append("/docroot").toString() );
    }
    
    /** Returns the default value (as would appear in the domain.xml on installation)
     * of file where the acess log of a virtual server is stored, as a String. Never returns a null.
     * Returned String contains no backslashes.
     * Note that it is <b> not <b> the absolute value of the path on a file system.
     */
    public static final String getAccessLogDefaultValue() {
        final StringBuffer sb = new StringBuffer(getPropertyAsValue(INSTANCE_ROOT_PROPERTY));
        return ( sb.append("/logs/access").toString() );
    }
    
    /** Returns the system specific file.separator delimited path to the asadmin script. Any changes to file layout should

     * be reflected here. The path will contain '/' as the separator character, regardless of operating
     * platform. Never returns a null. Assumes the the property "INSTALL_ROOT_PROPERTY" is set in the VM
     * before calling this. As of now (September 2005) all the server instances and asadmin VM itself has
     * this property set. The method does not guarantee that the script exists on the given system. It should
     * only be used when caller wants to know the location of the script. Caller should make sure it exists.
     * @return String representing the Path to asadmin script. Might return a string beginning with "null", if
     * the INSTALL_ROOT_PROPERTY is not defined
     */
    public static final String getAsAdminScriptLocation() {
        return getAdminScriptLocation(System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY));
    }

    public static final String getAsAdminScriptLocation(String installRoot) {
        return getAdminScriptLocation(installRoot);
    }

    public static final String getAdminScriptLocation(String installRoot) {
        final StringBuilder sb = new StringBuilder();
        final String ext = OS.isWindows() ? OS.WINDOWS_BATCH_FILE_EXTENSION : "";
        final String ASADMIN = "nadmin";
        final String suffix = new StringBuilder("lib").append(System.getProperty("file.separator")).append(ASADMIN).append(ext).toString();
        sb.append(installRoot);
        final String fs = System.getProperty("file.separator");
        if (!sb.toString().endsWith(fs))
            sb.append(fs);
        sb.append(suffix);

        return ( sb.toString() );
    }

    /** Returns the component identifier associated with the INSTALL_ROOT.
     *  For example if INSTALL_ROOT is /home/glassfish4/glassfish the
     *  component name will "glassfish".
     * @return String representing the component identifier.
     */
    public static final String getComponentName() {
        final File installRootFile = new File(System.getProperty(
            SystemPropertyConstants.INSTALL_ROOT_PROPERTY));
        return installRootFile.getName();
    }
}
