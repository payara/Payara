/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.glassfish.bootstrap;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public final class Constants {
    public final static String PLATFORM_PROPERTY_KEY = "GlassFish_Platform";
    // bundle containing module startup
    public final static String GF_KERNEL = "org.glassfish.core.kernel";
    public static final String ARGS_PROP = "com.sun.enterprise.glassfish.bootstrap.args";
    public final static String DEFAULT_DOMAINS_DIR_PROPNAME = "AS_DEF_DOMAINS_PATH";
    public static final String ORIGINAL_CP     = "-startup-classpath";
    public static final String ORIGINAL_CN     = "-startup-classname";
    public static final String ORIGINAL_ARGS   = "-startup-args";
    public static final String ARG_SEP         = ",,,";

    public final static String INSTANCE_ROOT_PROP_NAME = "com.sun.aas.instanceRoot";
    public static final String INSTALL_ROOT_PROP_NAME = "com.sun.aas.installRoot";
    public static final String INSTALL_ROOT_URI_PROP_NAME = "com.sun.aas.installRootURI";
    public static final String INSTANCE_ROOT_URI_PROP_NAME = "com.sun.aas.instanceRootURI";
    public static final String HK2_CACHE_DIR = "com.sun.enterprise.hk2.cacheDir";
    public static final String INHABITANTS_CACHE = "inhabitants";
    public static final String BUILDER_NAME_PROPERTY = "GlassFish.BUILDER_NAME";
    public static final String NO_FORCED_SHUTDOWN = "--noforcedshutdown";

    private Constants(){}


    // Supported platform we know about, not limited to.
    public enum Platform {Felix, Knopflerfish, Equinox, Static}
}
