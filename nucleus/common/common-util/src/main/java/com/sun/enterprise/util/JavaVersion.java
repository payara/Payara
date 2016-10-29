/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class has been copied from open installer
 * @author yamini
 */
public class JavaVersion {

    public static JavaVersion getVersion(String string) {
        // convert 1.7.0-b10 to 1.7.0.0.10
        if (string.matches(
                "[0-9]+\\.[0-9]+\\.[0-9]+-b[0-9]+")) {
          string = string.replace("-b", ".0.");
        }

        // convert 1.7.0_01-b10 to 1.7.0_01.10
        if (string.matches(
                "[0-9]+\\.[0-9]+\\.[0-9]+_[0-9]+-b[0-9]+")) {
          string = string.replace("-b", ".");
        }

        if (string.matches(
                "[0-9]+\\.[0-9]+\\.[0-9]+_[0-9]+-ea")) {
          string = string.replace("-ea", ".");
        }
        
        // and create the version
        final Matcher matcher = Pattern.
                compile("[0-9]+\\.[0-9]+[0-9_\\.\\-]+").
                matcher(string);

        if (matcher.find()) {
            return new JavaVersion(matcher.group());
        } else {
            return null;
        }
    }
    private long major;
    private long minor;
    private long micro;
    private long update;
    private long build;

    public JavaVersion(String string) {
        String[] split = string.split("[\\._\\-]+");

        if (split.length > 0) {
            major = Long.parseLong(split[0]);
        }
        if (split.length > 1) {
            minor = Long.parseLong(split[1]);
        }
        if (split.length > 2) {
            micro = Long.parseLong(split[2]);
        }
        if (split.length > 3) {
            update = Long.parseLong(split[3]);
        }
        if (split.length > 4) {
            build = Long.parseLong(split[4]);
        }
    }

    public boolean newerThan(JavaVersion version) {
        if (getMajor() > version.getMajor())
            return true;
        if (getMajor() < version.getMajor())
            return false;
        
        // majors are equal, so compare minors
        if (getMinor() > version.getMinor())
            return true;
        if (getMinor() < version.getMinor())
            return false;
        
        // minors are equal, so compare micros
        if (getMicro() > version.getMicro())
            return true;
        if (getMicro() < version.getMicro())
            return false;
        
        // micros are equal, so compare updates
        if (getUpdate() > version.getUpdate())
            return true;
        if (getUpdate() < version.getUpdate())
            return false;
        
        // updates are equal, so compare builds
        if (getBuild() > version.getBuild())
            return true;
        if (getBuild() < version.getBuild())
            return true;
        
        return false;
    }

    public boolean newerOrEquals(JavaVersion version) {
        return newerThan(version) || equals(version);
    }

    public boolean olderThan(JavaVersion version) {
        return !newerOrEquals(version);
    }

    public boolean olderOrEquals(JavaVersion version) {
        return !newerThan(version);
    }   

    public long getMajor() {
        return major;
    }

    public long getMinor() {
        return minor;
    }

    public long getMicro() {
        return micro;
    }

    public long getUpdate() {
        return update;
    }

    public long getBuild() {
        return build;
    }

    public String toMinor() {
        return "" + major + "." + minor;
    }

    public String toJdkStyle() {
        return "" + major
                + "." + minor
                + "." + micro
                + (update != 0 ? "_" + (update < 10 ? "0" + update : update) : "");
    }
}
