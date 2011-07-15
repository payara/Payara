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

package com.sun.enterprise.tools.verifier.util;

import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Class LogDomains
 */
public class LogDomains {

    
    private static final String DOMAIN_ROOT = "javax.enterprise.";

    public static final String AVK_VERIFIER_LOGGER =
        DOMAIN_ROOT + "system.tools.avk.tools.verifier";

    public static final String AVK_APPVERIFICATION_LOGGER =
        DOMAIN_ROOT + "system.tools.avk.appverification";

    public static final String AVK_APPVERIFICATION_TOOLS_LOGGER =
        DOMAIN_ROOT + "system.tools.avk.appverification.tools";

    public static final String AVK_APPVERIFICATION_XML_LOGGER =
        DOMAIN_ROOT + "system.tools.avk.appverification.xml";

    // RESOURCE_BUNDLES the name of the logging resource bundles.

    private static final String PACKAGE_ROOT = "com.sun.enterprise.";

    private static final String AVK_VERIFIER_BUNDLE =
        PACKAGE_ROOT + "tools.verifier.LocalStrings";

    // Note that these 3 bundles are packaged only in javke.jar and
    // they are not present in appserv-rt.jar
    private static final String AVK_APPVERIFICATION_BUNDLE =
        PACKAGE_ROOT + "appverification.LocalStrings";

    private static final String AVK_APPVERIFICATION_TOOLS_BUNDLE =
        PACKAGE_ROOT + "appverification.tools.LocalStrings";

    private static final String AVK_APPVERIFICATION_XML_BUNDLE =
        PACKAGE_ROOT + "appverification.xml.LocalStrings";

    // static field
    private static Hashtable<String, Logger> loggers = null;

    // static initializer
    static {
      loggers = new Hashtable<String, Logger>();
      loggers.put(AVK_VERIFIER_LOGGER,
                  Logger.getLogger(AVK_VERIFIER_LOGGER,
                                   AVK_VERIFIER_BUNDLE));
      // When run in instrumentation mode, with javke.jar in classpath
      // the calls below will succeed
      try {
      loggers.put(AVK_APPVERIFICATION_LOGGER,
                  Logger.getLogger(AVK_APPVERIFICATION_LOGGER,
                                   AVK_APPVERIFICATION_BUNDLE));
      loggers.put(AVK_APPVERIFICATION_TOOLS_LOGGER,
                  Logger.getLogger(AVK_APPVERIFICATION_TOOLS_LOGGER,
                                   AVK_APPVERIFICATION_TOOLS_BUNDLE));
      loggers.put(AVK_APPVERIFICATION_XML_LOGGER,
                  Logger.getLogger(AVK_APPVERIFICATION_XML_LOGGER,
                                   AVK_APPVERIFICATION_XML_BUNDLE));
      }catch(Exception e) {
         // during normal appserver-run, these 3 initializations will fail
      }
    }

    private LogDomains() {} // prevent instance creation

    public static Logger getLogger(String name) {
        return loggers.get(name);
    }

    public static Logger getDefaultLogger() {
        return loggers.get(AVK_VERIFIER_LOGGER);
    }
}
