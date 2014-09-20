/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.StringUtils;

import static com.sun.enterprise.util.StringUtils.ok;

/**
 *
 * @author bnevins
 */
class JvmOptions {

    JvmOptions(List<String> options) throws GFLauncherException {
        // We get them from domain.xml as a list of Strings
        // -Dx=y   -Dxx  -XXfoo -XXgoo=zzz -client  -server
        // Issue 4434 -- we might get a jvm-option like this:
        // <jvm-options>"-xxxxxx"</jvm-options> notice the literal double-quotes

        for (String s : options) {
            s = StringUtils.removeEnclosingQuotes(s);

            if (s.startsWith("-D")) {
                addSysProp(s);
            }
            else if (s.startsWith("-XX")) {
                addXxProp(s);
            }
            else if (s.startsWith("-X")) {
                addXProp(s);
            }
            else if (s.startsWith("-")) {
                addPlainProp(s);
            }
            else // TODO i18n
            {
                throw new GFLauncherException("UnknownJvmOptionFormat", s);
            }
        }
        filter(); // get rid of forbidden stuff
        setOsgiPort();
    }

    @Override
    public String toString() {
        List<String> ss = toStringArray();
        StringBuilder sb = new StringBuilder();
        for (String s : ss) {
            sb.append(s).append('\n');
        }
        return sb.toString();
    }

    List<String> toStringArray() {
        List<String> ss = new ArrayList<String>();

        Set<String> keys = xxProps.keySet();
        for (String name : keys) {
            String value = xxProps.get(name);
            if (value != null) {
                ss.add("-XX" + name + "=" + value);
            }
            else {
                ss.add("-XX" + name);
            }
        }

        keys = xProps.keySet();
        for (String name : keys) {
            String value = xProps.get(name);
            if (value != null) {
                ss.add("-X" + name + "=" + value);
            }
            else {
                ss.add("-X" + name);
            }
        }
        keys = plainProps.keySet();
        for (String name : keys) {
            String value = plainProps.get(name);
            if (value != null) {
                ss.add("-" + name + "=" + value);
            }
            else {
                ss.add("-" + name);
            }
        }
        keys = sysProps.keySet();
        for (String name : keys) {
            String value = sysProps.get(name);
            if (value != null) {
                ss.add("-D" + name + "=" + value);
            }
            else {
                ss.add("-D" + name);
            }
        }
        return postProcessOrdering(ss);
    }

    Map<String, String> getCombinedMap() {
        // used for resolving tokens
        Map<String, String> all = new HashMap<String, String>(plainProps);
        all.putAll(xProps);
        all.putAll(xxProps);
        all.putAll(sysProps);
        return all;
    }

    int getOsgiPort() {
        return osgiPort;
    }

    private void addPlainProp(String s) {
        s = s.substring(1);
        NameValue nv = new NameValue(s);
        plainProps.put(nv.name, nv.value);
    }

    private void addSysProp(String s) {
        s = s.substring(2);
        NameValue nv = new NameValue(s);
        sysProps.put(nv.name, nv.value);
    }

    private void addXProp(String s) {
        s = s.substring(2);
        NameValue nv = new NameValue(s);
        xProps.put(nv.name, nv.value);
    }

    private void addXxProp(String s) {
        s = s.substring(3);
        NameValue nv = new NameValue(s);
        xxProps.put(nv.name, nv.value);
    }

    @Deprecated
    void addJvmLogging() {
        xxProps.put(":+UnlockDiagnosticVMOptions", null);
        xxProps.put(":+LogVMOutput", null);
        xxProps.put(":LogFile", "${com.sun.aas.instanceRoot}/logs/jvm.log");
    }

    @Deprecated
    void removeJvmLogging() {
        xxProps.remove(":+UnlockDiagnosticVMOptions");
        xxProps.remove(":+LogVMOutput");
        xxProps.remove(":LogFile");
    }


    private List<String> postProcessOrdering(List<String> unsorted) {
        /*
         * (1) JVM has one known order dependency. If these 3 are here, then
         * unlock MUST appear first in the list -XX:+UnlockDiagnosticVMOptions
         * -XX:+LogVMOutput -XX:LogFile=D:/as/domains/domain1/logs/jvm.log
         *
         * June 2012 http://java.net/jira/browse/GLASSFISH-18777 JFR needs
         * UnlockCommercialFeatures -- it is also order-dependent. New algorithm
         * -- put -XX:+Unlock* first
         *
         * (2) TODO Get the name of the instance early. We no longer send in the
         * instanceRoot as an arg so -- ????
         */

        // go through the list hunting for the magic string.  If such a string is
        // found then move it to the top.  In June 2012 I changed this to a less
        // efficient but much more robust and simple algorithm...

        List<String> sorted = new ArrayList<String>(unsorted.size());

        for (String s : unsorted)
            if (hasMagic(s))
                sorted.add(s);

        for (String s : unsorted)
            if (!hasMagic(s))
                sorted.add(s);

        return sorted;
    }

    private boolean hasMagic(String s) {
        final String magic = "-XX:+Unlock";
        return s != null && s.startsWith(magic);
    }

    /**
     * Filters out unwanted properties and filters in interested properties that
     * may need to be present by default in certain environments (OS, vm.vendor)
     *
     * bnevins September 2009 There may be System Properties from V2 that cause
     * havoc. E.g. the MBean Server sys prop from V2 will be removed by upgrade
     * code in the server but the server will blow up before it starts with a
     * CNFE! We need to remove it carefully. I.e. the user may want to set up
     * their own MBean Server Factory so we just check to see if the value is
     * identical to the V2 class...
     *
     */
    private void filter() {
        // there is only one forbidden sys prop now so no need yet for fancy
        // data structures to contain the one key/value

        // I have seen these 2 values:
        // com.sun.enterprise.admin.server.core.jmx.AppServerMBeanServerBuilder
        // com.sun.enterprise.ee.admin.AppServerMBeanServerBuilder

        final String key = "javax.management.builder.initial";
        final String forbiddenStart = "com.sun.enterprise";
        final String forbiddenEnd = "AppServerMBeanServerBuilder";

        String val = sysProps.get(key);

        if (val != null && val.startsWith(forbiddenStart) && val.endsWith(forbiddenEnd))
            sysProps.remove(key);

        if (OS.isDarwin() && System.getProperty("java.vm.vendor").equals("Apple Inc.")) {
            // on Mac OS, unless the property is specified in the domain.xml, we add
            // the -d32 flag to start the JVM in 32 bits mode
            Pattern pattern = Pattern.compile("d\\d+");
            boolean settingPresent = false;
            for (String propName : plainProps.keySet()) {
                Matcher m = pattern.matcher(propName);
                if (m.matches()) {
                    settingPresent = true;
                }
            }
            if (!settingPresent) {
                addPlainProp("-d32");
            }
        }
    }

    private void setOsgiPort() {
        String s = sysProps.get("osgi.shell.telnet.port");

        // not configured
        if (!ok(s))
            return;

        try {
            osgiPort = Integer.parseInt(s);
        }
        catch (Exception e) {
            // already handled -- it is already set to -1
        }
    }
    Map<String, String> sysProps = new HashMap<String, String>();
    Map<String, String> xxProps = new HashMap<String, String>();
    Map<String, String> xProps = new HashMap<String, String>();
    Map<String, String> plainProps = new HashMap<String, String>();
    int osgiPort = -1;

    private static class NameValue {

        NameValue(String s) {
            int index = s.indexOf("=");

            if (index < 0) {
                name = s;
            }
            else {
                name = s.substring(0, index);
                if (index + 1 < s.length()) {
                    value = s.substring(index + 1);
                }
            }
        }
        private String name;
        private String value;
    }
}
/**
 * Reference Section <jvm-options>-XX:+UnlockDiagnosticVMOptions</jvm-options>
 * <jvm-options>-XX:+LogVMOutput</jvm-options>
 * <jvm-options>-XX:LogFile=${com.sun.aas.instanceRoot}/logs/jvm.log</jvm-options>
 */
