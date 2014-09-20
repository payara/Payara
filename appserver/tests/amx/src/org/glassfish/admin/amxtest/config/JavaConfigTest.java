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

package org.glassfish.admin.amxtest.config;

import com.sun.appserv.management.config.JavaConfig;
import com.sun.appserv.management.util.misc.GSetUtil;
import com.sun.appserv.management.util.misc.StringUtil;
import org.glassfish.admin.amxtest.AMXTestBase;

import java.util.HashSet;
import java.util.Set;


import com.sun.appserv.management.helper.AttributeResolverHelper;

/**
 */
public final class JavaConfigTest
        extends AMXTestBase {
    public JavaConfigTest() {
    }

    public void
    testGetJVMOptions() {
        final JavaConfig jc = getConfigConfig().getJavaConfig();

        final String[] jvmOptions = jc.getJVMOptions();

        if (jvmOptions.length < 2) {
            warning("Fewer than 2 JVM options, is this right: " +
                    StringUtil.toString(jvmOptions));

        }

        /*
        Arrays.sort( jvmOptions );
        trace("length = " + jvmOptions.length);
        for (int ii=0; ii<jvmOptions.length; ii++)
        {
            trace("jvmOptions[" + ii + "] = " + jvmOptions[ii]);
        }
        */
    }

    public void
    testSetJVMOptions() {
        final String newOption1 = "-DJavaConfigTest.OK=true";
        final String newOption2 = "-XJavaConfigTest.OK=true";

        final JavaConfig jc = getConfigConfig().getJavaConfig();

        final Set<String> beforeSet = GSetUtil.newUnmodifiableStringSet(jc.getJVMOptions());

        // add our new options
        final Set<String> requestSet = new HashSet<String>(beforeSet);
        requestSet.add(newOption1);
        requestSet.add(newOption2);
        jc.setJVMOptions(GSetUtil.toStringArray(requestSet));

        Set<String> afterSet = GSetUtil.newUnmodifiableStringSet(jc.getJVMOptions());

        // make sure our new options are present
        assert (afterSet.contains(newOption1));
        assert (afterSet.contains(newOption2));

        // make sure all prior options are still present
        for (final String beforeOption : beforeSet) {
            assert (afterSet.contains(beforeOption));
        }

        // now remove our two options
        requestSet.remove(newOption1);
        requestSet.remove(newOption2);
        jc.setJVMOptions(GSetUtil.toStringArray(requestSet));

        // verify our two options are gone
        afterSet = GSetUtil.newUnmodifiableStringSet(jc.getJVMOptions());
        assert (!afterSet.contains(newOption1));
        assert (!afterSet.contains(newOption2));

        // make sure all prior options are still present
        assert (afterSet.equals(beforeSet));
    }

    public void
    testGetters()
            throws Exception {
        final JavaConfig jc = getConfigConfig().getJavaConfig();

        String s;

        s = jc.getBytecodePreprocessors();
        if (s != null) {
            jc.setBytecodePreprocessors(s);
        }

        s = jc.getClasspathPrefix();
        if (s != null) {
            jc.setClasspathPrefix(s);
        }

        s = jc.getClasspathSuffix();
        if (s != null) {
            jc.setClasspathSuffix(s);
        }

        s = jc.getSystemClasspath();
        if (s != null) {
            jc.setSystemClasspath(s);
        }

        final String debugEnabledStr = jc.getDebugEnabled();
        final boolean debugEnabled = AttributeResolverHelper.resolveBoolean( jc, debugEnabledStr);
        jc.setDebugEnabled( debugEnabledStr );

        s = jc.getDebugOptions();
        if (s != null) {
            jc.setDebugOptions(s);
        }

        final String existingValue = jc.getEnvClasspathIgnored();
        final boolean envClasspathIgnored = AttributeResolverHelper.resolveBoolean( jc, existingValue);
        jc.setEnvClasspathIgnored( existingValue);

        s = jc.getJavaHome();
        if (s != null) {
            jc.setJavaHome(s);
        }

        s = jc.getJavacOptions();
        if (s != null) {
            jc.setJavacOptions(s);
        }

        final String[] options = jc.getJVMOptions();
        if (options != null) {
            jc.setJVMOptions(options);
        }

        s = jc.getNativeLibraryPathPrefix();
        if (s != null) {
            jc.setNativeLibraryPathPrefix(s);
        }

        s = jc.getNativeLibraryPathSuffix();
        if (s != null) {
            jc.setNativeLibraryPathSuffix(s);
        }

        s = jc.getRMICOptions();
        if (s != null) {
            jc.setRMICOptions(s);
        }

        s = jc.getServerClasspath();
        if (s != null) {
            jc.setServerClasspath(s);
        }
    }
}

















