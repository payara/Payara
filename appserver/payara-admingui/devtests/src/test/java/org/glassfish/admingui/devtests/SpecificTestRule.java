/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admingui.devtests;

import org.glassfish.admingui.devtests.util.SeleniumHelper;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Ignore;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 *
 * @author jasonlee
 */
public class SpecificTestRule implements MethodRule {
    protected static boolean debug;
    public SpecificTestRule() {
        debug = Boolean.parseBoolean(SeleniumHelper.getParameter("debug", "false"));
    }

    @Override
    public Statement apply(final Statement statement, final FrameworkMethod frameworkMethod, final Object o) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                boolean runMethod = false;
                final Logger logger = Logger.getLogger(BaseSeleniumTestClass.class.getName());
                String method = System.getProperty("method");
                Set<String> methods = new HashSet<String>();
                if (method != null) {
                    String[] parts = method.split(",");
                    methods.addAll(Arrays.asList(parts));
                }
                Ignore ignore = frameworkMethod.getAnnotation(Ignore.class);

                if (methods.contains(frameworkMethod.getName())) {
                    runMethod = true;
                } else if ((method == null) && (ignore == null)) {
                    runMethod = true;
                }

                if (debug) {
                    logger.log(Level.INFO, "Processing test {0} at {1}",
                            new String[]{
                                frameworkMethod.getName(),
                                (new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss")).format(new Date())
                            });
                }
                if (runMethod) {
                    if (debug) {
                        logger.log(Level.INFO, "\tExecuting.");
                    }
                    try {
                        statement.evaluate();
                    } catch (Throwable t) {
                        SeleniumHelper.captureScreenshot(frameworkMethod.getName());
                        throw t; // rethrow
                        // No explanation as to why this was done, so we'll disable
                        // it and see what happens
                        //statement.evaluate(); // try again. Ugly hack, but if it works...
                    }
                } else {
                    logger.log(Level.INFO, "\tSkipping.");
                }
            }
        };
    }
}
