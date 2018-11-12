/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2018] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.admin.launcher;

import com.sun.enterprise.glassfish.bootstrap.MainHelper;

/**
 *
 * @author Andrew Pielage
 */
public class PayaraDefaultJvmOptions {

    public static final String GRIZZLY_DEFAULT_MEMORY_MANAGER_PROPERTY = "org.glassfish.grizzly.DEFAULT_MEMORY_MANAGER";
    public static final String GRIZZLY_DEFAULT_MEMORY_MANAGER_VALUE = "org.glassfish.grizzly.memory.HeapMemoryManager";

    private static String LATEST_NPN_JAR_VERSION = "1.8.1";
    private static String GRIZZLY_NPN_JAR_LOCATION_BASE = "bootclasspath/p:${com.sun.aas.installRoot}/lib/grizzly-npn-bootstrap";
    /**
     * The location of the NPN Jar to load to the boot classpath. If null, none is
     * required.
     */
    public static final String GRIZZLY_NPN_JAR_LOCATION;

    // Configure the Grizzly NPN Jar version, and from that find the location.
    static {
        String npnVersion = null;
        String jarLocation = null;

        try {
            // For Java 8, configure the ALPN jar. Other versions of Java don't require this
            if (MainHelper.getMajorJdkVersion() == 1 && MainHelper.getMinorJdkVersion() == 8) {
                npnVersion = getNpnVersionFromJavaVersion();
                jarLocation = GRIZZLY_NPN_JAR_LOCATION_BASE
                        + ((npnVersion == null) ? "" : "-" + npnVersion)
                        + ".jar";
            }
        } catch (Throwable t) {
            // Any exceptions will also cause the NPN jar to be set to null
            jarLocation = null;
        } finally {
            GRIZZLY_NPN_JAR_LOCATION = jarLocation;
        }
    }

    /**
     * Finds the NPN jar version that corresponds to the current Java version.
     * 
     * @return the correct NPN jar version, or null if none is recognised. If the
     *         Java version is unrecognised, the latest NPN jar version will be
     *         returned.
     */
    private static String getNpnVersionFromJavaVersion() {
        final String javaVersion = System.getProperty("java.version");
        String npnVersion = null;

        // If the Java version is in a recognised format
        if (javaVersion != null && javaVersion.matches("1.8.0_[0-9]{2,3}.*")) {
            // Remove all letters, and parse the version to an integer
            final Integer patchVersion = Integer.parseInt(javaVersion.split("_")[1].replaceAll("[^0-9]", ""));

            if (patchVersion >= 191) {
                npnVersion = LATEST_NPN_JAR_VERSION;
            } else if (patchVersion >= 161) {
                npnVersion = "1.8";
            } else if (patchVersion >= 121) {
                npnVersion = "1.7";
            } else if (patchVersion >= 72) {
                npnVersion = "1.6";
            }
        } else {
            // The java version is unrecognisable, but is Java 8
            // In this case, assume the most recent NPN version
            npnVersion = LATEST_NPN_JAR_VERSION;
        }

        return npnVersion;
    }
}
