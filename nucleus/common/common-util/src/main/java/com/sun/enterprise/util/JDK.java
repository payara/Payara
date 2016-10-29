/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2016 Oracle and/or its affiliates. All rights reserved.
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

/**
 * A simple class that fills a hole in the JDK.  It parses out the version numbers
 *  of the JDK we are running.
 * Example:<p>
 * 1.6.0_u14 == major = 1 minor = 6, subminor = 0, update = 14
 *
 * @author bnevins
 */
public final class JDK {

    private JDK(String string) {
        String[] split = string.split("[\\._\\-]+");

        if (split.length > 0) {
            major = Integer.parseInt(split[0]);
        }
        if (split.length > 1) {
            minor = Integer.parseInt(split[1]);
        }
        if (split.length > 2) {
            subminor = Integer.parseInt(split[2]);
        }
        if (split.length > 3) {
            update = Integer.parseInt(split[3]);
        }
    }


    public static JDK getVersion(String string) {
        if (string.matches("([0-9]+[\\._\\-]+)*[0-9]+")) {
            return new JDK(string);
        } else {
            return null;
        }
    }
    /**
     * See if the current JDK is legal for running GlassFish
     * @return true if the JDK is >= 1.6.0
     */
    public static boolean ok() {
        return major == 1 && minor >= 6;
    }

    public static int getMajor() {
        return major;
    }
    public static int getMinor() {
        return minor;
    }

    public static int getSubMinor() {
        return subminor;
    }

    public static int getUpdate() {
        return update;
    }

    public boolean newerThan(JDK version) {
        if (major > version.getMajor()) {
            return true;
        } else if (major == version.getMajor()) {
            if (minor > version.getMinor()) {
                return true;
            } else if (minor == version.getMinor()) {
                if (subminor > version.getSubMinor()) {
                    return true;
                } else if (subminor == version.getSubMinor()) {
                    if (update > version.getUpdate()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean newerOrEquals(JDK version) {
        return newerThan(version) || equals(version);
    }

    public boolean olderThan(JDK version) {
        return !newerOrEquals(version);
    }

    public boolean olderOrEquals(JDK version) {
        return !newerThan(version);
    }

    /**
     * No instances are allowed so it is pointless to override toString
     * @return Parsed version numbers
     */
    public static String toStringStatic() {
        return "major: " + JDK.getMajor() +
        "\nminor: " + JDK.getMinor() +
        "\nsubminor: " + JDK.getSubMinor() +
        "\nupdate: " + JDK.getUpdate() +
        "\nOK ==>" + JDK.ok();
    }

    static {
        initialize();
    }

    // DO NOT initialize these variables.  You'll be sorry if you do!
    private static int major;
    private static int minor;
    private static int subminor;
    private static int update;

    // silently fall back to ridiculous defaults if something is crazily wrong...
    private static void initialize() {
        major = 1;
        minor = subminor = update = 0;
        try {
            String jv = System.getProperty("java.version");

            if(!StringUtils.ok(jv))
                return; // not likely!!

            String[] ss = jv.split("\\.");

            if(ss.length < 3 || !ss[0].equals("1"))
                return;

            major = Integer.parseInt(ss[0]);
            minor = Integer.parseInt(ss[1]);
            ss = ss[2].split("_");

            if(ss.length < 1)
                return;

            subminor = Integer.parseInt(ss[0]);

            if(ss.length > 1)
                update = Integer.parseInt(ss[1]);
        }
        catch(Exception e) {
            // ignore -- use defaults
        }
    }
}
