/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.server;

import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.universal.glassfish.ASenvPropertyReader;
import com.sun.enterprise.util.SystemPropertyConstants;

import java.io.*;
import java.util.*;

import org.glassfish.api.admin.RuntimeType;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.api.admin.ServerEnvironment;

import javax.inject.Inject;

/**
 * Defines various global configuration for the running GlassFish instance.
 *
 * <p>
 * This primarily replaces all the system variables in V2.
 *
 * @author Jerome Dochez
 * @author Byron Nevins
 */
@Service
public class ServerEnvironmentImpl implements ServerEnvironment, PostConstruct {
    @Inject
    StartupContext startupContext;

    /** folder where all generated code like compiled jsps, stubs is stored */
    public static final String kGeneratedDirName = "generated";
    public static final String kRepositoryDirName = "applications";
    public static final String kAppAltDDDirName = "altdd";
    public static final String kEJBStubDirName = "ejb";
    public static final String kGeneratedXMLDirName = "xml";
    public static final String kPolicyFileDirName = "policy";

    public static final String kConfigXMLFileName = "domain.xml";
    public static final String kConfigXMLFileNameBackup = "domain.xml.bak";
    public static final String kLoggingPropertiesFileName = "logging.properties";
    public static final String kDefaultLoggingPropertiesFileName = "default-logging.properties";
    /** folder where the configuration of this instance is stored */
    public static final String kConfigDirName = "config";
    /** init file name */
    public static final String kInitFileName = "init.conf";

    public static final String DEFAULT_ADMIN_CONSOLE_CONTEXT_ROOT = "/admin";
    public static final String DEFAULT_ADMIN_CONSOLE_APP_NAME     = "__admingui"; //same as folder

    private /*almost final*/ File root;
    private /*almost final*/ boolean verbose;
    private /*almost final*/ boolean debug;
    private ASenvPropertyReader asenv;
    private /*almost final*/ String domainName;
    private /*almost final*/ String instanceName;
    private RuntimeType serverType = RuntimeType.DAS; //set to DAS to avoid null

    private final static String INSTANCE_ROOT_PROP_NAME = "com.sun.aas.instanceRoot";
    private static final String INSTALL_ROOT_PROP_NAME = "com.sun.aas.installRoot";

    /**
     * Compute all the values per default.
     */
    public ServerEnvironmentImpl() {
    }

    public ServerEnvironmentImpl(File root) {
        // the getParentFile() that we do later fails to work correctly if
        // root is for example "new File(".")
        this.root = root.getAbsoluteFile();
        asenv = new ASenvPropertyReader();
    }

    /**
     * This is where the real initialization happens.
     */
    @Override
    public void postConstruct() {

        // todo : dochez : this will need to be reworked...
        String installRoot = startupContext.getArguments().getProperty(INSTALL_ROOT_PROP_NAME);
        if (installRoot == null) {
            // During unit testing, we find an empty StartupContext.
            // Let's first see if the installRoot system property is set
            // in the client VM, if not
            // To be consistent with earlier code (i.e., code that relied on StartupContext.getRootDirectory()),
            // I am setting user.dir as installRoot.
            if (System.getProperty(INSTALL_ROOT_PROP_NAME) != null) {
                installRoot = System.getProperty(INSTALL_ROOT_PROP_NAME);
            } else {
                installRoot = System.getProperty("user.dir");
            }
        }
        asenv = new ASenvPropertyReader(new File(installRoot));

        // default
        if(this.root==null) {
            String envVar = System.getProperty(INSTANCE_ROOT_PROP_NAME);
            if (envVar!=null) {
                root = new File(envVar);
            } else {
                String instanceRoot = startupContext.getArguments().getProperty(INSTANCE_ROOT_PROP_NAME);
                if (instanceRoot == null) {
                    // In client container, instanceRoot is not set. It is a different question altogether as to why
                    // an object called ServerEnvironmentImpl is at all active in client runtime. To be consistent
                    // with earlier code, we use installRoot as instanceRoot.
                    instanceRoot = installRoot;
                }
                root = new File(instanceRoot);
            }
        }

        /*
         * bnevins 12/12/11
         * The following chunk of code sets things like hostname to be a file under instance root
         * I.e. it's crazy.  It's 1 hour until SCF so I'll just fix the current problem which is a NPE
         * if the value is null.
         * At any rate the weird values that get set into System Properties get un-done at
         * the line of code in bootstrap (see end of this comment).  It's easy to trace just step out of this method
         * in a debugger
         * createGlassFish(gfKernel, habitat, gfProps.getProperties())
         */
        asenv.getProps().put(SystemPropertyConstants.INSTANCE_ROOT_PROPERTY, root.getAbsolutePath());
        for (Map.Entry<String, String> entry : asenv.getProps().entrySet()) {

            if(entry.getValue() == null) // don't NPE File ctor
                continue;

            File location = new File(entry.getValue());
            if (!location.isAbsolute()) {
                location = new File(asenv.getProps().get(SystemPropertyConstants.INSTANCE_ROOT_PROPERTY), entry.getValue());
            }
            System.setProperty(entry.getKey(), location.getAbsolutePath());
        }

        Properties args = startupContext.getArguments();

        verbose = Boolean.parseBoolean(args.getProperty("-verbose"));
        debug = Boolean.parseBoolean(args.getProperty("-debug"));

        // ugly code because domainName & instanceName are final...
        String s = args.getProperty("-domainname");

        if (!ok(s)) {
            s = root.getName();
        }
        domainName = s;

        s = args.getProperty("-instancename");

        if (!ok(s)) {
            instanceName = "server";
        }
        else {
            instanceName = s;
        }
        // bnevins IT 10209
        asenv.getProps().put(SystemPropertyConstants.SERVER_NAME, instanceName);
        System.setProperty(SystemPropertyConstants.SERVER_NAME, instanceName);

        // bnevins Apr 2010 adding clustering support...
        String typeString = args.getProperty("-type");
        serverType = RuntimeType.getDefault();

        try {
            if(typeString != null)
                serverType = RuntimeType.valueOf(typeString);
        }
        catch(Exception e) {
            // already handled above...
        }
    }

    // XXX - many of these methods should be on ServerEnvironment

    public String getDomainName() {
        return domainName;
    }

    @Override
    public File getInstanceRoot() {
        return root;
    }

    /**
     * @return the instance root
     * @deprecated  As of GlassFish 3.1 replaced with {@link #getInstanceRoot() }
     */
    @Deprecated
    @Override
    public File getDomainRoot() {
        return getInstanceRoot();
    }


    @Override
    public StartupContext getStartupContext() {
        return startupContext;
    }

    /**
     * Gets the directory to store configuration.
     * Normally {@code ROOT/config}
     */
    @Override
    public File getConfigDirPath() {
        return new File(root,kConfigDirName);
    }

    /**
     * Gets the directory to store deployed applications
     * Normally {@code ROOT/applications}
     */
    @Override
    public File getApplicationRepositoryPath() {
        return new File(root,kRepositoryDirName);
    }

    /**
     * Gets the directory to store generated stuff.
     * Normally {@code ROOT/generated}
     */
    @Override
    public File getApplicationStubPath() {
        return new File(root,kGeneratedDirName);
    }

    /**
     * Gets the <tt>init.conf</tt> file.
     */
    public File getInitFilePath() {
        return new File(getConfigDirPath(),kInitFileName);
    }

    /**
     * Gets the directory for hosting user-provided jar files.
     * Normally {@code ROOT/lib}
     */
    @Override
    public File getLibPath() {
        return new File(root,"lib");

    }

    @Override
    public File getApplicationEJBStubPath() {
        return new File(getApplicationStubPath(), kEJBStubDirName);
    }

    @Override
    public File getApplicationGeneratedXMLPath() {
        return new File(getApplicationStubPath(),kGeneratedXMLDirName);
    }

    /**
     * Returns the path for compiled JSP Pages from an application
     * that is deployed on this instance. By default all such compiled JSPs
     * should lie in the same folder.
     */
    @Override
    public File getApplicationCompileJspPath() {
        return new File(getApplicationStubPath(),kCompileJspDirName);
    }

    /**
     * Returns the path for policy files for applications
     * deployed on this instance.
     */
    @Override
    public File getApplicationPolicyFilePath() {
        return new File(getApplicationStubPath(),kPolicyFileDirName);
    }

    /**
     * Gets the directory to store external alternate deployment descriptors
     * Normally {@code ROOT/generated/altdd}
     */
    public File getApplicationAltDDPath() {
        return new File(getApplicationStubPath(), kAppAltDDDirName);
    }

    /*
     * XXX - no one is using these methods, so I'm commenting them out
     * for now.  When they're needed, they should probably be added to
     * the ServerEnvironment Interface.
     *
    public String getJavaWebStartPath() {
        return null;
    }

    public String getApplicationBackupRepositoryPath() {
        return null;
    }

    public String getInstanceClassPath() {
        return null;
    }
     */

    /**
     * Return the value of one property.
     * Example <br>
     * String pr = getProp(SystemPropertyConstants.PRODUCT_ROOT_PROPERTY);
     * <br>
     * @param key the name of the property
     * @return the value of the property
     */
    public final String getProp(String key) {
        return getProps().get(key);
    }

    public Map<String, String> getProps() {
        return Collections.unmodifiableMap(asenv.getProps());
    }

    /** Returns the folder where the admin console application's folder (in the
     *  name of admin console application) should be found. Thus by default,
     *  it should be: [install-dir]/lib/install/applications. No attempt is made
     *  to check if this location is readable or writable.
     *  @return java.io.File representing parent folder for admin console application
     *   Never returns a null
     */
    public File getDefaultAdminConsoleFolderOnDisk() {
        File install = new File(asenv.getProps().get(SystemPropertyConstants.INSTALL_ROOT_PROPERTY));
        File agp = new File(new File(new File(install, "lib"), "install"), "applications");
        return (agp);
    }

    @Override
    public File getMasterPasswordFile() {
        return new File (getInstanceRoot(), "master-password");
    }

    @Override
    public File getJKS() {
        return new File (getConfigDirPath(), "keystore.jks");
    }

    @Override
    public File getTrustStore() {
        return new File(getConfigDirPath(), "cacerts.jks");
    }

    private Status status=Status.starting;
    @Override
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isEmbedded() {
        return serverType == RuntimeType.EMBEDDED;
    }

    @Override
    public RuntimeType getRuntimeType() {
        return serverType;
    }
 /**
     * Every server has a name that can be found in the server element in domain.xml
     * @return the name of this server i.e. "my" name
     */
    @Override
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * Am I a running GlassFish server instance?
     * @return true if we are an instance
     */
    @Override
    public boolean isInstance() {
        return serverType == RuntimeType.INSTANCE;
    }

    /**
     * Am I a running GlassFish DAS server?
     * @return true if we are a DAS
     */
    @Override
    public boolean isDas() {
        return serverType == RuntimeType.DAS;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isVerbose() {
        return verbose;
    }

    private boolean ok(String s) {
        return s != null && s.length() > 0;
    }
}
