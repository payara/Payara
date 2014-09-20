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

package org.glassfish.admin.amxtest;

import java.util.HashMap;
import java.util.Map;

/**
 Property keys used to configure the unit tests.

 @see TestMain
 @see TestRunner */
public final class PropertyKeys {
    private PropertyKeys() {}


    private final static String BASE = "amxtest";
    public static final String DEFAULT_PROPERTIES_FILE = BASE + ".properties";

    public static final String CONNECT_KEY = BASE + ".connect";
    public static final String HOST_KEY = CONNECT_KEY + ".host";
    public static final String PORT_KEY = CONNECT_KEY + ".port";
    public static final String USER_KEY = CONNECT_KEY + ".user";
    public static final String PASSWORD_KEY = CONNECT_KEY + ".password";
    public static final String TRUSTSTORE_KEY = CONNECT_KEY + ".truststore";
    public static final String TRUSTSTORE_PASSWORD_KEY = CONNECT_KEY + ".truststorePassword";
    public static final String USE_TLS_KEY = CONNECT_KEY + ".useTLS";
    public static final String RUN_THREADED_KEY = BASE + ".threaded";
    public static final String VERBOSE_KEY = BASE + ".verbose";
    public static final String ITERATIONS_KEY = BASE + ".iterations";

    /**
     Whether testing is for offline config utilizing
     com.sun.appserv.management.config.OfflineConfigIniter.
     You must also supply a value for the {@link #DOMAIN_XML_KEY}.
     */
    public static final String TEST_OFFLINE_KEY = BASE + ".testOffline";

    /**
     A valid file path for domain.xml.
     */
    public static final String DOMAIN_XML_KEY = TEST_OFFLINE_KEY + ".domainXML";


    /**
     A boolean specifying whether expanded testing is to be used. When specified,
     tests that involve clusters, multiple standalone servers, etc are run
     (if possible).
     */
    public static final String EXPANDED_TESTING_KEY = BASE + ".expandedTesting";

    /**
     Comma-separated list of node-agent names to be used during testing.
     The special name {@link #ALL_NODE_AGENTS} may be used to specify all configured node agents.
     <p/>
     At runtime, the environment contains a Map<String,AppserverConnectionSource> available
     via this key, where the key is the node agent name.
     */
    public static final String NODE_AGENTS_KEY = BASE + ".nodeAgents";

    public static final String ALL_NODE_AGENTS = "ALL";


    /**
     Name of the node agent that the DAS uses.
     */
    public static final String DAS_NODE_AGENT_NAME = BASE + ".dasNodeAgent";

    /**
     Comma-separated list of files.
     */
    public static final String ARCHIVES_TO_DEPLOY_KEY = BASE + ".deploy.files";
    /**
     Delimiter between files contained in the value for {@link #ARCHIVES_TO_DEPLOY_KEY}.
     */
    public static final String ARCHIVES_DELIM = ",";

    /**
     The number of threads to run for DeploymentMgrTest.testDeployHeavilyThreaded()
     */
    public static final String DEPLOY_NUM_THREADS = BASE + ".deploy.numThreads";


    /**
     The number of threads to run for UploadDownloadMgrTest.testHeavilyThreaded()
     */
    public static final String UPLOAD_DOWNLOAD_MGR_TEST_THREADS = BASE + ".UploadDownloadMgrTest.numThreads";
    /**
     The size, in KB, of UploadDownloadMgrTest.testDownloadBigFile()
     */
    public static final String UPLOAD_DOWNLOAD_MGR_TEST_BIG_FILE_KB = BASE + ".UploadDownloadMgrTest.bigFileKB";


    /**
     File consisting of names of tests, one per line
     */
    public static final String TEST_CLASSES_FILE_KEY = BASE + ".testClasses";

    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_PORT = "8686";
    public static final String DEFAULT_USER = "admin";
    public static final String DEFAULT_PASSWORD = "admin123";
    public static final String DEFAULT_TRUSTSTORE = "~/" + BASE + ".truststore";
    public static final String DEFAULT_TRUSTSTORE_PASSWORD = "changeme";
    public static final String DEFAULT_USE_TLS = "true";
    public static final String DEFAULT_RUN_THREADED = "true";
    public static final String DEFAULT_TEST_CLASSES_FILE_KEY = BASE + ".test-classes";
    public static final String DEFAULT_VERBOSE = "false";
    public static final String DEFAULT_ITERATIONS = "2";
    public static final String DEFAULT_CONNECT = "true";
    public static final String DEFAULT_NODE_AGENT_NAMES = ALL_NODE_AGENTS;
    public static final String DEFAULT_EXPANDED_TESTING = "false";
    public static final String DEFAULT_TEST_OFFLINE = "false";

    public static final String DEFAULT_ARCHIVES_TO_DEPLOY = "";
    public static final String DEFAULT_DEPLOY_NUM_THREADS = "10";

    public static final String DEFAULT_UPLOAD_DOWNLOAD_MGR_TEST_THREADS = "10";
    public static final String DEFAULT_UPLOAD_DOWNLOAD_MGR_TEST_BIG_FILE_KB = "1536";


    public static Map<String, String>
    getDefaults() {
        final Map<String, String> props = new HashMap<String, String>();

        props.put(HOST_KEY, DEFAULT_HOST);
        props.put(PORT_KEY, DEFAULT_PORT);
        props.put(USER_KEY, DEFAULT_USER);
        props.put(PASSWORD_KEY, DEFAULT_PASSWORD);
        props.put(TRUSTSTORE_KEY, DEFAULT_TRUSTSTORE);
        props.put(TRUSTSTORE_PASSWORD_KEY, DEFAULT_TRUSTSTORE_PASSWORD);
        props.put(USE_TLS_KEY, DEFAULT_USE_TLS);
        props.put(CONNECT_KEY, DEFAULT_CONNECT);
        props.put(TEST_OFFLINE_KEY, DEFAULT_TEST_OFFLINE);
        props.put(DOMAIN_XML_KEY, "./domain.xml");

        props.put(NODE_AGENTS_KEY, ALL_NODE_AGENTS);
        props.put(EXPANDED_TESTING_KEY, DEFAULT_EXPANDED_TESTING);

        props.put(RUN_THREADED_KEY, DEFAULT_RUN_THREADED);
        props.put(VERBOSE_KEY, DEFAULT_VERBOSE);
        props.put(TEST_CLASSES_FILE_KEY, DEFAULT_TEST_CLASSES_FILE_KEY);
        props.put(ITERATIONS_KEY, DEFAULT_ITERATIONS);

        props.put(ARCHIVES_TO_DEPLOY_KEY, DEFAULT_ARCHIVES_TO_DEPLOY);
        props.put(DEPLOY_NUM_THREADS, DEFAULT_DEPLOY_NUM_THREADS);

        props.put(UPLOAD_DOWNLOAD_MGR_TEST_THREADS, DEFAULT_UPLOAD_DOWNLOAD_MGR_TEST_THREADS);
        props.put(UPLOAD_DOWNLOAD_MGR_TEST_BIG_FILE_KB, DEFAULT_UPLOAD_DOWNLOAD_MGR_TEST_BIG_FILE_KB);
		
		props.put( DEFAULT_PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE );
		return( props );
	}
};

