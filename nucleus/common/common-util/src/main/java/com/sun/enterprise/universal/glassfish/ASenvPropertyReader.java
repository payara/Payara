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

package com.sun.enterprise.universal.glassfish;

import com.sun.enterprise.universal.io.SmartFile;
import static com.sun.enterprise.util.SystemPropertyConstants.*;
import com.sun.enterprise.util.net.NetUtils;

import java.io.*;
import java.util.*;

/**
 * Class ASenvPropertyReader
 * 
 * This class converts the variables stored in asenv.conf (UNIX)
 * or asenv.bat (WINDOWS) into their equivalent system properties.
 * <p>This class <strong>guarantees</strong> that no Exception will get thrown back.
 * You may however, have a bad javaRoot set even though we tried everything to find
 * one
 */
public class ASenvPropertyReader {
    /**
     * Read and process the information in asenv
     * There are no arguments because the installation directory is calculated
     * relative to the jar file you are calling from.
     * Unlike V2 this class will not set any System Properties.  Instead it will
     * give you a Map<String,String> containing the properties.
     * <p>To use the class, create an instance and then call getProps().
     */
    public ASenvPropertyReader() {
        this(GFLauncherUtils.getInstallDir());
    }   
    
    /**
     * Read and process the information in asenv.[bat|conf]
     * This constructor should normally not be called.  It is designed for
     * unit test classes that are not running from an official installation.
     * @param installDir The Glassfish installation directory
     */
    public ASenvPropertyReader(File installDir)
    {
        synchronized (propsMap) {
            try {
                installDir = SmartFile.sanitize(installDir);
                props = propsMap.get(installDir);
                if (props == null) {
                    props = new ASenvMap(installDir);
                    propsMap.put(installDir, props);
                }
            }
            catch(Exception e)
            {
            // ignore -- this is universal utility code there isn't much we can
            // do.
            }
        }
    }

    /**
     * Returns the properties that were processed.  This includes going to a bit of
     * trouble setting up the hostname and java root.
     * @return A Map<String,String> with all the properties
     */
    public Map<String, String> getProps()
    {
        return props;
    }

    /**
     * Returns a string representation of the properties in the Map<String,String>.
     * Format:  name=value\nname2=value2\n etc.
     * @return the string representation.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Set<String> keys = props.keySet();

        for (String key : keys) {
            sb.append(key).append("=").append(props.get(key)).append('\n');
        }
        return sb.toString();
    }


    /*
     * ASenvMap is a "lazy-evaluation" map, i.e., for values that are
     * expensive to calculate, the value is not calculated until it is actually
     * used.
     */
    static class ASenvMap extends HashMap<String, String>
    {
        // If we find a token in a set property, this is set to true.
        boolean foundToken = false;

        ASenvMap(File installDir) {
            put(INSTALL_ROOT_PROPERTY, installDir.getPath());
            File configDir = SmartFile.sanitize(new File(installDir, "config"));
            put(CONFIG_ROOT_PROPERTY, configDir.getPath());
            setProperties(configDir);
            postProcess(configDir);
            // Product root is defined to be the parent of the install root.
            // While tempting to just use installDir.getParent() we go through
            // these gyrations just in case setProperties() changed the value
            // of the INSTALL_ROOT_PROPERTY property.
            File installRoot = new File(super.get(INSTALL_ROOT_PROPERTY));
            put(PRODUCT_ROOT_PROPERTY, installRoot.getParent());
        }

        @Override
        public String get(Object k) {
            String v = super.get(k);
            if (v != null) return v;
            if (k.equals(HOST_NAME_PROPERTY)) {
                v = getHostname();
                put(HOST_NAME_PROPERTY, v);
            }
            else if (k.equals(JAVA_ROOT_PROPERTY)) {
                v = getJavaRoot(super.get(JAVA_ROOT_PROPERTY_ASENV));
                put(JAVA_ROOT_PROPERTY, v);
            }
            return v;
        }

        @Override
        public Set<String> keySet() {
            completeMap();
            return super.keySet();
        }

        @Override
        public Set<Map.Entry<String, String>> entrySet() {
            completeMap();
            return super.entrySet();
        }

        @Override
        public boolean containsKey(Object k) {
            completeMap();
            return super.containsKey((String)k);
        }

        @Override
        public Collection<String> values() {
            completeMap();
            return super.values();
        }

        /*
         * Add the "lazy" items to the map so that the map is complete.
         */
        private void completeMap() {
            get(HOST_NAME_PROPERTY);
            get(JAVA_ROOT_PROPERTY);
        }

        /*
         * 2 things to do
         * 1) change relative paths to absolute
         * 2) change env. variables to either the actual values in the environment
         *  or to another prop in asenv
         */
        private void postProcess(File configDir) {
            if (foundToken) {
                final Map<String, String> env = System.getenv();
                //put env props in first
                Map<String, String> all = new HashMap<String, String>(env);
                // now override with our props
                all.putAll(this);
                TokenResolver tr = new TokenResolver(all);
                tr.resolve(this);
            }

            // props have all tokens replaced now (if they exist)
            // now make the paths absolute.
            // Call super.keySet here so that the lazy values are not added
            // to the map at this point.
            Set<String> keys = super.keySet();

            for (String key : keys) {
                String value = super.get(key);
                if (GFLauncherUtils.isRelativePath(value)) {
                    // we have to handle both of these:
                    // /x/y/../z
                    // ../x/y/../z

                    File f;
                    if (value.startsWith(".")) {
                        f = SmartFile.sanitize(new File(configDir, value));
                    }
                    else {
                        f = SmartFile.sanitize(new File(value));
                    }

                    put(key, f.getPath());
                }
            }
        }

        private void setProperties(File configDir) {
            //Read in asenv.conf/bat and set system properties accordingly
            File asenv = new File(configDir,
                    GFLauncherUtils.isWindows() ?
                        WINDOWS_ASENV_FILENAME : UNIX_ASENV_FILENAME);

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(asenv));
                String line;
                while ((line = reader.readLine()) != null) {
                    setProperty(line);
                }
            }
            catch (Exception ex) {
            // Nothing to do
            }
            finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                }
                catch (Exception ex) {
                }
            }
        }

        /**
         * Method setProperty
         * Parses a single line of asenv.conf or asenv.bat and attempt to
         * set the corresponding property. Note that if the system
         * property is already set (e.g. via -D on the command line), then
         * we will not clobber its existing value.
         *
         * @param line
         *
         */
        private void setProperty(String line) {
            int pos = line.indexOf("=");

            if (pos > 0) {
                String lhs = (line.substring(0, pos)).trim();
                String rhs = (line.substring(pos + 1)).trim();

                if (GFLauncherUtils.isWindows()) {    //trim off the "set "
                    lhs = (lhs.substring(3)).trim();
                }
                else {      // take the quotes out
                    pos = rhs.indexOf("\"");
                    if (pos != -1) {
                        rhs = (rhs.substring(pos + 1)).trim();
                        pos = rhs.indexOf("\"");
                        if (pos != -1) {
                            rhs = (rhs.substring(0, pos)).trim();
                        }
                    }
                }

                String systemPropertyName = envToPropMap.get(lhs);

                if (systemPropertyName != null) {
                    if (TokenResolver.hasToken(rhs)) foundToken = true;
                    put(systemPropertyName, rhs);
                }
            }
        }

        static private String getHostname() {
            String hostname = "localhost";
            try {
                // canonical name checks to make sure host is proper
                hostname = NetUtils.getCanonicalHostName();
            }
            catch (Exception ex) {
            // ignore, go with "localhost"
            }
            return hostname;
        }

        /*
         * Get a value for the Java installation directory.  The value that is
         * passed in should be the value from the ASenv config file. If it is valid
         * it is returned.  Otherwise, this method checks the following:
         * - JAVA_HOME environment variable
         * - java.home system property
         */
        static private String getJavaRoot(String fileValue) {
            // make sure we have a folder with java in it!
            // note that we are not in a position to set it from domain.xml yet

            // first choice -- whatever is in asenv
            String javaRootName = fileValue;

            if (isValidJavaRoot(javaRootName))
                return javaRootName; // we are already done!

            // try JAVA_HOME
            javaRootName = System.getenv("JAVA_HOME");

            if (isValidJavaRoot(javaRootName))
            {
                javaRootName = SmartFile.sanitize(new File(javaRootName)).getPath();
                return javaRootName;
            }
            // try java.home with ../
            // usually java.home is pointing at jre and ".." goes to the jdk
            javaRootName = System.getProperty("java.home") + "/..";

            if (isValidJavaRoot(javaRootName))
            {
                javaRootName = SmartFile.sanitize(new File(javaRootName)).getPath();
                return javaRootName;
            }

            // try java.home as-is
            javaRootName = System.getProperty("java.home");

            if (isValidJavaRoot(javaRootName))
            {
                javaRootName = SmartFile.sanitize(new File(javaRootName)).getPath();
                return javaRootName;
            }
            // TODO - should this be an Exception?  A log message?
            return null;
        }

        static private boolean isValidJavaRoot(String javaRootName) {
            if (!GFLauncherUtils.ok(javaRootName))
                return false;

            // look for ${javaRootName}/bin/java[.exe]
            File f = new File(javaRootName);

            if (GFLauncherUtils.isWindows())
                f = new File(f, "bin/java.exe");
            else
                f = new File(f, "bin/java");

            return f.exists();
        }

    }

    static private Map<String, String> envToPropMap = new HashMap<String, String>();
    {
        envToPropMap.put("AS_DERBY_INSTALL", DERBY_ROOT_PROPERTY);
        envToPropMap.put("AS_IMQ_LIB", IMQ_LIB_PROPERTY);
        envToPropMap.put("AS_IMQ_BIN", IMQ_BIN_PROPERTY);
        envToPropMap.put("AS_CONFIG", CONFIG_ROOT_PROPERTY);
        envToPropMap.put("AS_INSTALL", INSTALL_ROOT_PROPERTY);
        envToPropMap.put("AS_JAVA", JAVA_ROOT_PROPERTY_ASENV);
        envToPropMap.put("AS_DEF_DOMAINS_PATH", DOMAINS_ROOT_PROPERTY);
        envToPropMap.put("AS_DEF_NODES_PATH", AGENT_ROOT_PROPERTY);
    }

    /*
     * Typically, only one asenv file will be read, even though there may be many
     * ASenvPropertyReader objects.  So for each unique File, only one ASenvMap
     * is created, and all ASenvPropertyReader objects that reference the file
     * will share the same map. The key to the propsMap is the install dir that
     * is passed to the constructor.
     */
    static private final HashMap<File, ASenvMap> propsMap = new HashMap<File, ASenvMap>();
    private ASenvMap props;
}
