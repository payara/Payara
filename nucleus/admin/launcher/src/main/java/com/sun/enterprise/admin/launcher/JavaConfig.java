/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.launcher;

import com.sun.enterprise.universal.glassfish.GFLauncherUtils;
import static com.sun.enterprise.admin.launcher.GFLauncherConstants.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author bnevins
 */
class JavaConfig {

    JavaConfig(Map<String, String> map) {
        this.map = map;
    }

    Map<String, String> getMap() {
        return map;
    }

    String getJavaHome() {
        return map.get("java-home");
    }

    List<File> getEnvClasspath() {
        if(useEnvClasspath()) {
            String s = System.getenv("CLASSPATH");
            s = stripQuotes(s);
            return GFLauncherUtils.stringToFiles(s);
        }
        else {
            return new ArrayList<File>();
        }
    }

    List<File> getPrefixClasspath() {
        String cp = map.get("classpath-prefix");

        if(GFLauncherUtils.ok(cp)) {
            return GFLauncherUtils.stringToFiles(cp);
        }
        else {
            return new ArrayList<File>();
        }
    }

    String getNativeLibraryPrefix() {
        String s = map.get(NATIVE_LIB_PREFIX);

        if(!GFLauncherUtils.ok(s))
            s = "";

        return s;
    }


    List<File> getSuffixClasspath() {
        String cp = map.get("classpath-suffix");

        if(GFLauncherUtils.ok(cp)) {
            return GFLauncherUtils.stringToFiles(cp);
        }
        else {
            return new ArrayList<File>();
        }
    }

    String getNativeLibrarySuffix() {
        String s = map.get(NATIVE_LIB_SUFFIX);

        if(!GFLauncherUtils.ok(s))
            s = "";

        return s;
    }

    List<File> getSystemClasspath() {
        String cp = map.get("system-classpath");

        if(GFLauncherUtils.ok(cp)) {
            return GFLauncherUtils.stringToFiles(cp);
        }
        else {
            return new ArrayList<File>();
        }
    }

    List<String> getDebugOptions() {
        // we MUST break this up into the total number of -X commands (currently 2),
        // Since our final command line is a List<String>, we can't have 2
        // options in one String -- the JVM will ignore the second option...
        // sample "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9999"
        List<String> empty = Collections.emptyList();
        String s = map.get("debug-options");

        if(!GFLauncherUtils.ok(s)) {
            return empty;
        }
        String[] ss = s.split(" ");

        if(ss.length <= 0) {
            return empty;
        }
        return Arrays.asList(ss);
    }

    boolean isDebugEnabled() {
        return Boolean.parseBoolean(map.get("debug-enabled"));
    }


    private boolean useEnvClasspath() {
        String s = map.get("env-classpath-ignored");

        // the default is true for *ignoring* which means
        // the default is *false* for using (yikes!)
        // If there is no value -- return false
        // else use the opposite of whatever the value is

        if(s == null || s.length() <= 0)
            return false;

        return !Boolean.parseBoolean(s);
    }

    private String stripQuotes(String s) {
        // IT 7500
        // if the CLASSPATH has "C:/foo goo" with actual double-quotes
        // the server will not start.
        // It is not allowed to have a classpath filename that contains quote characters.
        // Here we just mindlessly remove such characters.
        // It looks inefficient but it is incredibly rare for the CLASSPATH to be enabled
        // and for there to be an embedded quote character especially since we give
        // a SEVERE error message everytime.

        if(!hasQuotes(s))
            return s;

        String s2 = stripChar(s, "'");
        s2 = stripChar(s2, "\"");
        GFLauncherLogger.severe(GFLauncherLogger.NO_QUOTES_ALLOWED, s, s2);

        return s2;
    }

    private boolean hasQuotes(String s) {
        if(s == null)
            return false;

        if(s.indexOf('\'') >= 0)
            return true;

        return s.indexOf('"') >= 0;
    }

    private String stripChar(String s, String c) {
        String[] ss = s.split(c);

        StringBuilder sb = new StringBuilder();

        for(String s2 : ss)
            sb.append(s2);

        return sb.toString();
    }

    private Map<String, String> map;
}
/*
 * Sample java-config from a V2 domain.xml
 *  <java-config
        classpath-suffix=""
        debug-enabled="false"
        debug-options="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9009"
        env-classpath-ignored="true"
        java-home="${com.sun.aas.javaRoot}"
        javac-options="-g"
        rmic-options="-iiop -poa -alwaysgenerate -keepgenerated -g"
        system-classpath="">
 * */
