/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2021-2022] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package org.apache.catalina.core;

import junit.framework.TestCase;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.web.valve.GlassFishValve;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StandardContextValveTest extends TestCase {

    @Mock
    private HttpRequest httpRequest;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private HttpServletResponse httpServletResponse;

    @InjectMocks
    private StandardContextValve standardContextValve = new StandardContextValve();

    @Test
    public void preventAccessToRestrictedDirectoryWithEmptyContextRootTest() throws IOException, ServletException {
        DataChunk dataChunkURL1 = DataChunk.newInstance();
        dataChunkURL1.setString("WEB-INF/web.xml");
        DataChunk dataChunkURL2 = DataChunk.newInstance();
        dataChunkURL2.setString("META-INF/MANIFEST.MF");

        when(httpRequest.getCheckRestrictedResources()).thenReturn(true);
        when(httpRequest.getRequestPathMB()).thenReturn(dataChunkURL1).thenReturn(dataChunkURL2);
        when(httpResponse.getResponse()).thenReturn(httpServletResponse);

        int pipelineResult = standardContextValve.invoke(httpRequest, httpResponse);

        verifyThatResourceIsNotFound(pipelineResult, 1, httpRequest, httpResponse, httpServletResponse);

        pipelineResult = standardContextValve.invoke(httpRequest, httpResponse);

        verifyThatResourceIsNotFound(pipelineResult, 2, httpRequest, httpResponse, httpServletResponse);
    }

    @Test
    public void normalizeURLTest() {
        String path1 = "/app/../some/../something/../my.jsp";
        String path2 = "/app/./some/./something/./my.jsp";
        String path3 = "./my.jsp";
        String path4 = "../app/WEB-INF/web.xml";
        String path5 = "../app/META-INF/MANIFEST.MF";

        String result = standardContextValve.normalize(path1);

        assertEquals("/my.jsp", result);

        result = standardContextValve.normalize(path2);

        assertEquals("/app/some/something/my.jsp", result);

        result = standardContextValve.normalize(path3);

        assertEquals("/my.jsp", result);

        result = standardContextValve.normalize(path4);

        assertEquals("/WEB-INF", result);

        result = standardContextValve.normalize(path5);

        assertEquals("/META-INF", result);
    }

    protected void verifyThatResourceIsNotFound(int pipelineResult, int times, HttpRequest httpRequest, HttpResponse httpResponse,
                                 HttpServletResponse httpServletResponse) throws IOException {
        assertEquals(GlassFishValve.END_PIPELINE, pipelineResult);
        verify(httpRequest, times(times)).getCheckRestrictedResources();
        verify(httpRequest, times(times)).getRequestPathMB();
        verify(httpResponse, times(times)).getResponse();
        verify(httpServletResponse, times(times)).sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}