/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2017 Oracle and/or its affiliates. All rights reserved.
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
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright 2018-2022 Payara Foundation and/or its affiliates

package com.sun.enterprise.util;

import java.util.Optional;

/**
 * A simple class that fills a hole in the JDK.  It parses out the version numbers
 *  of the JDK we are running.
 * Example:<p>
 * 1.6.0_u14 == major = 1 minor = 6, subminor = 0, update = 14
 *
 * @author bnevins
 */
public final class JDK {

    /**
     * See if the current JDK is an LTS version
     * @return true if JDK is an LTS version (8, 11, 17, 21, 25, 29...)
     */
    public static boolean isRunningLTSJDK() {
        int major = getMajor();
        //Checks for LTS JDK versions following the 2 year LTS cadence after and including JDK 17
        //JDK 1-8 always have a major version of 1.
        return (major - 17) % 4 == 0 && major >= 17 || major == 11;
    }

    /**
     * See if the current JDK is legal for running GlassFish
     * @return true if the JDK is >= 11
     */
    public static boolean ok() {
        return major >= 11;
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

    public static String getVendor() {
        return vendor;
    }

    public static String getVm() {
        return vm;
    }

    public static class Version {
        private final int major;
        private final Optional<Integer> minor;
        private final Optional<Integer> subminor;
        private final Optional<Integer> update;

        private Version(String version) {
            // split java version into it's constituent parts, i.e.
            // 1.2.3.4 -> [ 1, 2, 3, 4]
            // 1.2.3u4 -> [ 1, 2, 3, 4]
            // 1.2.3_4 -> [ 1, 2, 3, 4]

            if (version.contains("-")) {
                String[] versionSplit = version.split("-");
                version = versionSplit.length > 1 ? versionSplit[1] : "";
            }
            String[] split = version.split("[\\._u\\-]+");

            major = split.length > 0 ? Integer.parseInt(split[0]) : 0;
            minor = split.length > 1 ? Optional.of(Integer.parseInt(split[1])) : Optional.empty();
            subminor = split.length > 2 ? Optional.of(Integer.parseInt(split[2])) : Optional.empty();
            update = split.length > 3 ? Optional.of(Integer.parseInt(split[3])) : Optional.empty();
        }

        private Version() {
            major = JDK.major;
            minor = Optional.of(JDK.minor);
            subminor = Optional.of(JDK.subminor);
            update = Optional.of(JDK.update);
        }

        public boolean newerThan(Version version) {
            if (major > version.major) {
                return true;
            } else if (major == version.major) {
                if (greaterThan(minor, version.minor)) {
                    return true;
                } else if (equals(minor, version.minor)) {
                    if (greaterThan(subminor, version.subminor)) {
                        return true;
                    } else if (equals(subminor, version.subminor)) {
                        if (greaterThan(update, version.update)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        public boolean olderThan(Version version) {
            if (major < version.major) {
                return true;
            } else if (major == version.major) {
                if (lessThan(minor, version.minor)) {
                    return true;
                } else if (equals(minor, version.minor)) {
                    if (lessThan(subminor, version.subminor)) {
                        return true;
                    } else if (equals(subminor, version.subminor)) {
                        if (lessThan(update, version.update)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        private static boolean greaterThan(Optional<Integer> leftHandSide, Optional<Integer> rightHandSide) {
            return leftHandSide.orElse(0) > rightHandSide.orElse(0);
        }

        private static boolean lessThan(Optional<Integer> leftHandSide, Optional<Integer> rightHandSide) {
            return leftHandSide.orElse(0) < rightHandSide.orElse(0);
        }

        /**
         * if either left-hand-side or right-hand-side is empty, it is equals
         *
         * @param leftHandSide
         * @param rightHandSide
         * @return true if equals, otherwise false
         */
        private static boolean equals(Optional<Integer> leftHandSide, Optional<Integer> rightHandSide) {
            if(!leftHandSide.isPresent() || !rightHandSide.isPresent()) {
                return true;
            }
            return leftHandSide.orElse(0).equals(rightHandSide.orElse(0));
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 61 * hash + this.major;
            hash = 61 * hash + this.minor.orElse(0);
            hash = 61 * hash + this.subminor.orElse(0);
            hash = 61 * hash + this.update.orElse(0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Version other = (Version) obj;
            if (this.major != other.major) {
                return false;
            }
            if (!equals(this.minor, other.minor)) {
                return false;
            }
            if (!equals(this.subminor, other.subminor)) {
                return false;
            }
            if (!equals(this.update, other.update)) {
                return false;
            }
            return true;
        }

        public boolean newerOrEquals(Version version) {
            return newerThan(version) || equals(version);
        }

        public boolean olderOrEquals(Version version) {
            return olderThan(version) || equals(version);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(10);
            sb.append(major);
            if (minor.isPresent()) {
                sb.append('.').append(minor.get());
            }
            if (subminor.isPresent()) {
                sb.append('.').append(subminor.get());
            }
            if (update.isPresent()) {
                sb.append('.').append(update.get());
            }
            return sb.toString();
        }
    }

    public static Version getVersion(String string) {
        if (string != null && string.matches("([0-9]+[\\._u\\-]+)*[0-9]+")) {
            // make sure the string is a valid JDK version, i.e.
            // 1.8.0_162 or something that is returned by "java -version"
            return new Version(string);
        } else {
            return null;
        }
    }

    public static Version getVersion() {
        return new Version();
    }

    public static boolean isCorrectJDK(Optional<Version> minVersion, Optional<Version> maxVersion) {
        return isCorrectJDK(Optional.of(JDK_VERSION), minVersion, maxVersion);
    }

    public static boolean isCorrectJDK(Optional<Version> reference, Optional<Version> minVersion, Optional<Version> maxVersion) {
        return isCorrectJDK(reference, Optional.empty(), minVersion, maxVersion);
    }

    /**
     * Check if the reference version falls between the minVersion and maxVersion.
     *
     * @param reference The version to compare; falls back to the current JDK version if empty.
     * @param vendorOrVM The inclusive JDK vendor or VM name.
     * @param minVersion The inclusive minimum version.
     * @param maxVersion The inclusive maximum version.
     * @return true if within the version range, false otherwise
     */
    public static boolean isCorrectJDK(Optional<Version> reference, Optional<String> vendorOrVM, Optional<Version> minVersion, Optional<Version> maxVersion) {
        Version version = reference.orElse(JDK_VERSION);
        boolean correctJDK = true;

        if (vendorOrVM.isPresent()) {
            correctJDK = JDK.vendor.contains(vendorOrVM.get()) || JDK.vm.contains(vendorOrVM.get());
        }

        if (correctJDK && minVersion.isPresent()) {
            correctJDK = version.newerOrEquals(minVersion.get());
        }

        if (correctJDK && maxVersion.isPresent()) {
            correctJDK = version.olderOrEquals(maxVersion.get());
        }

        return correctJDK;
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
    private static String vendor;
    private static String vm;
    private static Version JDK_VERSION;

    // silently fall back to ridiculous defaults if something is crazily wrong...
    private static void initialize() {
        major = 1;
        minor = subminor = update = 0;
        try {
            String javaVersion = System.getProperty("java.version");
            vendor = System.getProperty("java.vendor");
            vm = System.getProperty("java.vm.name");

            /*In JEP 223 java.specification.version will be a single number versioning , not a dotted versioning . So if we get a single
            integer as versioning we know that the JDK is post JEP 223
            For JDK 8:
                java.specification.version  1.8
                java.version    1.8.0_122
             For JDK 9:
                java.specification.version 9
                java.version 9.1.2
            */
            String javaSpecificationVersion = System.getProperty("java.specification.version");
            String[] jsvSplit = javaSpecificationVersion.split("\\.");
            if (jsvSplit.length == 1) {
                //This is handle Early Access build .Example 9-ea
                String[] jvSplit = javaVersion.split("-");
                String jvReal = jvSplit[0];
                String[] split = jvReal.split("[\\.]+");

                if (split.length > 0) {
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
            } else {
                if (!StringUtils.ok(javaVersion))
                    return; // not likely!!

                String[] ss = javaVersion.split("\\.");

                if (ss.length < 3 || !ss[0].equals("1"))
                    return;

                major = Integer.parseInt(ss[0]);
                minor = Integer.parseInt(ss[1]);
                ss = ss[2].split("_");

                if (ss.length < 1)
                    return;

                subminor = Integer.parseInt(ss[0]);

                if (ss.length > 1)
                    update = Integer.parseInt(ss[1]);
            }
        }
        catch(Exception e) {
            // ignore -- use defaults
        }

        JDK_VERSION = new Version();
    }
}
