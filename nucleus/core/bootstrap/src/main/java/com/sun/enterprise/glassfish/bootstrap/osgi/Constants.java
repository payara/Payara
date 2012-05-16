/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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


package com.sun.enterprise.glassfish.bootstrap.osgi;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class Constants {
    static final String BUNDLEIDS_FILENAME = "glassfish.bundleids";
    static final String PROVISIONING_OPTIONS_FILENAME = "provisioning.properties";
    static final String PROVISIONING_OPTIONS_PREFIX = "glassfish.osgi";
    /**
     * The property name for the auto processor's auto-install property.
     */
    static final String AUTO_INSTALL_PROP = PROVISIONING_OPTIONS_PREFIX + ".auto.install";
    /**
     * The property name for the auto processor's auto-start property.
     */
    static final String AUTO_START_PROP = PROVISIONING_OPTIONS_PREFIX + ".auto.start";
    /**
     * The property name for auto processor's auto-start options property
     * The value of this property is the integer argument to Bundle.start()
     */
    static final String AUTO_START_OPTIONS_PROP = PROVISIONING_OPTIONS_PREFIX + ".auto.start.options";
    /**
     * Prefix for the property name to specify bundle's start level
     */
    static final String AUTO_START_LEVEL_PROP = PROVISIONING_OPTIONS_PREFIX + ".auto.start.level";
    /**
     * The property name for final start level of framework
     */
    static final String FINAL_START_LEVEL_PROP = PROVISIONING_OPTIONS_PREFIX + ".start.level.final";
    /**
     * The property name to configure if bundles should be provisioned on demand.
     */
    static final String ONDEMAND_BUNDLE_PROVISIONING = "glassfish.osgi.ondemand";

    static final String FILE_SCHEME = "file";
}
