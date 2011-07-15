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

package com.sun.enterprise.tools.verifier.tests.web;

import com.sun.enterprise.tools.verifier.tests.web.WebTest;
import java.util.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;
import org.glassfish.deployment.common.Descriptor;

/** 
 * url-pattern element must not contain New Line (NL) or Carriage Return (CR)
 * In the schema j2ee_1_4.xsd it states that
       The url-patternType contains the url pattern of the mapping.
        It must follow the rules specified in Section 11.2 of the
        Servlet API Specification. This pattern is assumed to be in
        URL-decoded form and must not contain CR(#xD) or LF(#xA).
        If it contains those characters, the container must inform
        the developer with a descriptive error message.
 */
//This test won't be exercised now as DOL already has this test (see bug#4903530)
//But we should have it so that if any day DOL removes that test, we would catch it.
public class URLPatternContainsCRLF extends URLPattern { 

    protected void checkUrlPatternAndSetResult(String urlPattern, Descriptor descriptor, Result result, ComponentNameConstructor compName){
        if (urlPattern == null) return; //some other test takes care of this.
        // In Ascii table, Line Feed (LF) decimal value is 10 and Carriage Return (CR) decimal value is 13
        final int LF = 10, CR = 13;
        if (urlPattern.indexOf(CR)!=-1 || urlPattern.indexOf(LF)!=-1) { 
            oneFailed=true;
            result.failed(smh.getLocalString
                                   ("tests.componentNameConstructor",
                                    "For [ {0} ]",
                                    new Object[] {compName.toString()}));
            result.addErrorDetails (smh.getLocalString
                                         (getClass().getName() + ".failed",
                                          "url-pattern [ {0} ] within [ {1} ] contains a carriage return or line feed char",
                                          new Object[] {urlPattern, descriptor.getName()}));
        } else {
            result.passed(smh.getLocalString
                                  ("tests.componentNameConstructor",
                                   "For [ {0} ]",
                                   new Object[] {compName.toString()}));
            result.addGoodDetails (smh.getLocalString
                                    (getClass().getName() + ".passed",
                                     "url-pattern [ {0} ] within [ {1} ] does not contain carriage return or line feed char",
                                     new Object[] {urlPattern, descriptor.getName()}));
        }
    }
}
