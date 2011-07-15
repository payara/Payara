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

package com.sun.enterprise.tools.verifier.tests.web;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.TagLibDescriptor;
import com.sun.enterprise.tools.verifier.web.TagDescriptor;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.deployment.WebBundleDescriptor;

/**
 * Tag class implements javax.servlet.jsp.tagext.JspTag for JSP version 2.0,
 * javax.servlet.jsp.tagext.Tag for earlier versions of JSP specification.
 *
 * @author sg133765
 */

public class TagClassExtendsValidInterface extends WebTest implements WebCheck {
    public Result check(WebBundleDescriptor descriptor) {

        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        VerifierTestContext context = getVerifierContext();
        Result result = loadWarFile(descriptor);
        TagLibDescriptor tlds[] = context.getTagLibDescriptors();
        boolean failed=false;
        boolean oneFailed = false;

        if (tlds == null) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                    (getClass().getName() + ".passed",
                            "No tag lib files are specified"));
            return result;

        }
        for(TagLibDescriptor tld : tlds) {
            TagDescriptor[] tagDesc = tld.getTagDescriptors();
            for(TagDescriptor td : tagDesc) {
                String tagclass = td.getTagClass();
                Class c = loadClass(result, tagclass);
                if (c!=null) {
                    if (tld.getSpecVersion().trim().equalsIgnoreCase("2.0")) {
                        failed = !javax.servlet.jsp.tagext.JspTag.class.isAssignableFrom(c);
                    } else {
                        failed = !javax.servlet.jsp.tagext.Tag.class.isAssignableFrom(c);
                    }
                    if(failed) {
                        oneFailed = true;
                        addErrorDetails(result, compName);
                        result.addErrorDetails(smh.getLocalString(getClass().getName() + ".failed",
                                "Error: tag class [ {0} ] in [ {1} ] does not implements valid interface",
                                new Object[] {c.getName(), tld.getUri()}));
                    } else {
                        addGoodDetails(result, compName);
                        result.addGoodDetails(smh.getLocalString
                                (getClass().getName() + ".passed1",
                                        "tag class [ {0} ] in [ {1} ] implements valid interface",
                                        new Object[] {c.getName(), tld.getUri()}));
                    }
                }
            }//for
        }
        if(oneFailed)
            result.setStatus(Result.FAILED);
        else
            result.setStatus(Result.PASSED);

        return result;
    }
}
