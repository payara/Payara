/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */

package fish.payara.appserver.web.core.tck;

import java.io.IOException;


import com.sun.ts.tests.servlet.api.jakarta_servlet_http.httpservletrequest40.Client;
import com.sun.ts.tests.servlet.api.jakarta_servlet_http.httpservletrequest40.DispatchServlet;
import com.sun.ts.tests.servlet.api.jakarta_servlet_http.httpservletrequest40.ForwardFilter;
import com.sun.ts.tests.servlet.api.jakarta_servlet_http.httpservletrequest40.ForwardServlet;
import com.sun.ts.tests.servlet.api.jakarta_servlet_http.httpservletrequest40.IncludeServlet;
import com.sun.ts.tests.servlet.api.jakarta_servlet_http.httpservletrequest40.NamedForwardServlet;
import com.sun.ts.tests.servlet.api.jakarta_servlet_http.httpservletrequest40.NamedIncludeServlet;
import com.sun.ts.tests.servlet.api.jakarta_servlet_http.httpservletrequest40.TestServlet;
import com.sun.ts.tests.servlet.api.jakarta_servlet_http.httpservletrequest40.TrailerTestServlet;
import org.apache.catalina.core.StandardContext;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;


public class HttpServletRequest40TCK extends TCKBase {

    @Test
    public void httpServletRequest40Tests() throws IOException {
        context = harness.addContext("servlet_jsh_httpservletrequest40_web", ctx ->
                ctx.addFilter("ForwardFilter", ForwardFilter.class, "/ForwardFilter")
                        .addServlet("TestServlet", TestServlet.class, "/TestServlet", "*.ts")
                        .addServlet("DispatchServlet", DispatchServlet.class, "/DispatchServlet")
                        .addServlet("ForwardServlet", ForwardServlet.class, "/ForwardServlet")
                        .addServlet("defaultServlet", TestServlet.class, "/")
                        .addServlet("IncludeServlet", IncludeServlet.class, "/IncludeServlet")
                        .addServlet("NamedForwardServlet", NamedForwardServlet.class, "/NamedForwardServlet")
                        .addServlet("NamedIncludeServlet", NamedIncludeServlet.class, "/NamedIncludeServlet")
                        .addServlet("TrailerTestServlet", TrailerTestServlet.class, "/TrailerTestServlet"));
        harness.runTck(new Client(), "httpServletMappingDispatchTest");
        // failing test uses async dispatch, we don't have that yet.
    }
}
