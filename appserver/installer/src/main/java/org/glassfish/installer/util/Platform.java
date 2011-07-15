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
package org.glassfish.installer.util;

import java.io.Serializable;

/**
 * The Platform object determines if the platform that the java virtual machine is running on is
 * compatible with the specified platform.  The platform may be specified with a general os type
 * (i.e. Windows or Unix), or by the specific os (i.e. Solaris).
 * @author Sathyan Catari(This class is a copied/customized version of OpenInstaller's Platform utility).
 * @version %Revision%
 */
public class Platform implements Serializable {
    /*
     * OS Types.  NOTE: The String declared on the right of each enum is used when forming
     * filename paths, but otherwise has no bearing on the actual OS-specific value.  It does
     * not (and in some case is not) have to be the same as the enum name.
     */

    public enum OSType {

        UNDEFINED("UNDEFINED"),
        UNIX("Unix"),
        WINDOWS("Windows"),
        BE("BE"),
        MAC("Mac");

        private OSType(final String aName) {
            setValue(aName);
        }

        private void setValue(final String aName) {
            gName = aName;
        }

        public String getValue() {
            return gName;
        }
        private String gName;
    }

    /*
     * OS Names.  NOTE: The String declared on the right of each enum is used when forming
     * filename paths, but otherwise has no bearing on the actual OS-specific value.  It does
     * not (and in some case is not) have to be the same as the enum name.
     */
    public enum OSName {

        UNDEFINED("UNDEFINED"),
        SOLARIS("Solaris"),
        LINUX("Linux"),
        HPUX("HPUX"),
        AIX("AIX"),
        SCO("SCO"),
        WIN95("Windows 95"),
        WIN2003("Windows 2003"),
        WINNT("Windows NT"),
        MACOS("MacOS"),
        MACOSX("MacOSX"),
        BEOS("BE OS"),
        IRIX("Irix"),
        OSF1("OSF1"),
        WINXP("Windows XP"),
        WIN2000("Windows 2000"),
        WIN98("Windows 98");

        private OSName(final String aName) {
            setValue(aName);
        }

        private void setValue(final String aName) {
            gName = aName;
        }

        public String getValue() {
            return gName;
        }
        private String gName;
    }

    public OSType getOSType() {
        /*
         * Is it unix?
         */
        String aOSTypeString = System.getProperty("os.name");
        if (aOSTypeString.equalsIgnoreCase("solaris") || aOSTypeString.equalsIgnoreCase("sunos")
                || aOSTypeString.equalsIgnoreCase("linux") || aOSTypeString.equalsIgnoreCase("hpux")
                || aOSTypeString.equalsIgnoreCase("aix") || aOSTypeString.equalsIgnoreCase("alpha")
                || aOSTypeString.equalsIgnoreCase("irix") || aOSTypeString.equalsIgnoreCase("hpux")
                || aOSTypeString.equalsIgnoreCase("hp-ux") || aOSTypeString.equalsIgnoreCase("sco")
                || aOSTypeString.equalsIgnoreCase("unix") || aOSTypeString.equalsIgnoreCase("osf1")
                || aOSTypeString.equalsIgnoreCase("digital unix") || aOSTypeString.equalsIgnoreCase("dec unix")
                || aOSTypeString.equalsIgnoreCase("dec")) {
            return OSType.UNIX;
        }
        if (aOSTypeString.length() >= "windows".length() && aOSTypeString.substring(0, "windows".length()).equalsIgnoreCase("WINDOWS")) {
            return OSType.WINDOWS;
        }
        if (aOSTypeString.equalsIgnoreCase("beos")
                || aOSTypeString.equalsIgnoreCase("BE")) {
            return OSType.BE;
        }
        if (aOSTypeString.toLowerCase().indexOf("mac") != -1) {
            return OSType.MAC;

        }

        /*
         * Could not determine the os type.
         */
        return OSType.UNDEFINED;
    }

    /**
     * Returns an id for the os name, given the property string for the os name.
     *
     * @param aOsName The name of the os.  This is the property string keyed "os.name".
     *
     * @return The os name.
     */
    public OSName getOSNameForString(final String aOsName) {
        /*
         * Allow substitution of spaces with underscores.
         */
        final String theCookedOsName = aOsName.replace('_', ' ');

        /*
         * Os names.
         */
        if (theCookedOsName.equalsIgnoreCase("sunos")
                || theCookedOsName.equalsIgnoreCase("SOLARIS")) {
            return OSName.SOLARIS;
        }
        if (theCookedOsName.equalsIgnoreCase("LINUX")) {
            return OSName.LINUX;
        }
        if (theCookedOsName.equalsIgnoreCase("hp-ux")
                || theCookedOsName.equalsIgnoreCase("HPUX")) {
            return OSName.HPUX;
        }
        if (theCookedOsName.equalsIgnoreCase("AIX")) {
            return OSName.AIX;
        }
        if (theCookedOsName.equalsIgnoreCase("SCO")) {
            return OSName.SCO;
        }
        if (theCookedOsName.equalsIgnoreCase("windows 95")
                || theCookedOsName.equalsIgnoreCase("WIN95")) {
            return OSName.WIN95;
        }
        if (theCookedOsName.equalsIgnoreCase("windows 98")
                || theCookedOsName.equalsIgnoreCase("WIN98")) {
            return OSName.WIN98;
        }
        if (theCookedOsName.equalsIgnoreCase("windows 2000")
                || theCookedOsName.equalsIgnoreCase("WIN2000")) {
            return OSName.WIN2000;
        }
        if (theCookedOsName.equalsIgnoreCase("windows nt")
                || theCookedOsName.equalsIgnoreCase("WINNT")) {
            return OSName.WINNT;
        }
        if (theCookedOsName.equalsIgnoreCase("windows xp")
                || theCookedOsName.equalsIgnoreCase("WINXP")) {
            return OSName.WINXP;
        }
        if (theCookedOsName.equalsIgnoreCase("windows 2003")
                || theCookedOsName.equalsIgnoreCase("WIN2003")) {
            return OSName.WIN2003;
        }
        if (theCookedOsName.equalsIgnoreCase("MAC OS X") || theCookedOsName.equalsIgnoreCase("MACOSX")) {
            return OSName.MACOSX;
        }
        if (theCookedOsName.equalsIgnoreCase("mac os")
                || theCookedOsName.equalsIgnoreCase("MACOS")) {
            return OSName.MACOS;
        }
        if (theCookedOsName.equalsIgnoreCase("BEOS")) {
            return OSName.BEOS;
        }
        if (theCookedOsName.equalsIgnoreCase("IRIX")) {
            return OSName.IRIX;
        }
        if (theCookedOsName.equalsIgnoreCase("OSF1")) {
            return OSName.OSF1;

            /*
             * Could not determine the os name.
             */
        }
        return OSName.UNDEFINED;
    }
}
