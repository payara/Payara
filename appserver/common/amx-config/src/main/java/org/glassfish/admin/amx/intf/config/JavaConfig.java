/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.intf.config;

import org.glassfish.admin.amx.base.Singleton;

import java.util.List;

@Deprecated
public interface JavaConfig
        extends Singleton, AnonymousElementList, ConfigElement, PropertiesAccess {


    public String getBytecodePreprocessors();

    public void setBytecodePreprocessors(String param1);

    public String getClasspathPrefix();

    public void setClasspathPrefix(String param1);

    public String getClasspathSuffix();

    public void setClasspathSuffix(String param1);

    public String getDebugEnabled();

    public void setDebugEnabled(String param1);

    public String getDebugOptions();

    public void setDebugOptions(String param1);

    public String getEnvClasspathIgnored();

    public void setEnvClasspathIgnored(String param1);

    public String getJavaHome();

    public void setJavaHome(String param1);

    public String getJavacOptions();

    public void setJavacOptions(String param1);

    public String getSystemClasspath();

    public void setSystemClasspath(String param1);

    public String[] getJvmOptions();

    /**
     * Add jvm options.
     * <p/>
     * If a JVM option contains a space or tab, you must enclose
     * it in quotes eg </code>"C:Program Files\dir"</code>
     */
    public void setJvmOptions(String[] value);

    public String getNativeLibraryPathPrefix();

    public void setNativeLibraryPathPrefix(String param1);

    public String getNativeLibraryPathSuffix();

    public void setNativeLibraryPathSuffix(String param1);

    public String getRmicOptions();

    public void setRmicOptions(String param1);

    public String getServerClasspath();

    public void setServerClasspath(String param1);

    public Profiler getProfiler();

    public void setProfiler(Profiler param1);

    public List getJavacOptionsAsList();

}
