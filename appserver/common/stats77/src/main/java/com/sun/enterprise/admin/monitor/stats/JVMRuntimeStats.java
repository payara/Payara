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

package com.sun.enterprise.admin.monitor.stats;
import org.glassfish.j2ee.statistics.Stats;
import org.glassfish.j2ee.statistics.CountStatistic;
import com.sun.enterprise.admin.monitor.stats.StringStatistic;

/**
 * A Stats interface to expose information about the JVM Runtime
 * @since 8.1
 */

public interface JVMRuntimeStats extends Stats {
    
    /**
     * Returns the name representing the running JVM
     * @return StringStatistic  the name of the running JVM
     */
    public StringStatistic getName();
    
    
    /**
     * Returns the JVM implementation name
     * @return StringStatistic  JVM implementation name
     */
    public StringStatistic getVmName();
    
    /**
     * Returns the JVM implementation vendor
     * @return StringStatistic  JVM implementation vendor
     */
    public StringStatistic getVmVendor();
    
    /**
     * Returns the JVM implementation version
     * @return StringStatistic JVM implementation version
     */
    public StringStatistic getVmVersion();
    
    /**
     * Returns the JVM specification name
     * @return StringStatistic  JVM specification name
     */
    public StringStatistic getSpecName();
    
    /**
     * Returns the JVM specification vendor
     * @return StringStatistic  JVM specification vendor
     */
    public StringStatistic getSpecVendor();
    
    /**
     * Returns the JVM specification version
     * @return StringStatistic  JVM specification version
     */
    public StringStatistic getSpecVersion();
    
    /**
     * Returns the management spec version implemented by the 
     * JVM
     * @return  StringStatistic Management specification version
     */
    public StringStatistic getManagementSpecVersion();
    
    /**
     * Returns the classpath that is used by the system class loader
     * to search for class files
     * @return StringStatistic  Java class path
     */
    public StringStatistic getClassPath();
    
    /**
     * returns the Java library path
     * @return StringStatistic  Java library path
     */
    public StringStatistic getLibraryPath();
    
    /**
     * Returns the classpath that is used by the bootstrap class loader
     * to search for class files
     * @return StringStatistic  the boot classpath
     */
    public StringStatistic getBootClasspath();
    
    /**
     * Returns the input arguments passed to the JVM. Does not include
     * the arguments to the main method
     * @return StringStatistic  arguments to the JVM
     */
    public StringStatistic getInputArguments();
    
    /**
     * Returns the uptime of the JVM in milliseconds
     * @return CountStatistic   Uptime in milliseconds
     */
    public CountStatistic getUptime();
    
    
}
