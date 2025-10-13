/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2021 Payara Foundation and/or affiliates
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
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package org.glassfish.grizzly.config;

import java.io.IOException;
import org.glassfish.grizzly.config.dom.Ssl;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.ProcessingState;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author jonathan coustick
 * @since 5.28.0
 */
public class HSTSFilterTest {
    
    private static final String HSTS_HEADER = "Strict-Transport-Security";
    private static final String EXPECTED_MAX = "max-age=31536000";
    private static final String EXPECTED_ALL = "max-age=31536000; includeSubDomains; preload";
    
    @Test
    public void headerTestJustMax() throws IOException {
        Ssl mockedSsl = Mockito.mock(Ssl.class);
        Mockito.when(mockedSsl.getHstsEnabled()).thenReturn("true");
        Mockito.when(mockedSsl.getHstsPreload()).thenReturn("false");
        Mockito.when(mockedSsl.getHstsSubdomains()).thenReturn("false");
        
        HttpHeader header = processFilter(mockedSsl);
        Assert.assertEquals(EXPECTED_MAX, header.getHeader(HSTS_HEADER));
        
    }
    
    @Test
    public void headerTestAllTrue() throws IOException {
        Ssl mockedSsl = Mockito.mock(Ssl.class);
        Mockito.when(mockedSsl.getHstsEnabled()).thenReturn("true");
        Mockito.when(mockedSsl.getHstsPreload()).thenReturn("true");
        Mockito.when(mockedSsl.getHstsSubdomains()).thenReturn("true");
        
        HttpHeader header = processFilter(mockedSsl);
        Assert.assertEquals(EXPECTED_ALL, header.getHeader(HSTS_HEADER));
        
    }
    
    private HttpHeader processFilter(Ssl config) throws IOException {
        HSTSFilter filter = new HSTSFilter();
        filter.configure(null, null, config);
        
        FilterChainContext context = new FilterChainContext();
        context.setMessage(HttpContent.create(new TestHttpHeader()));
        
        filter.handleRead(context);
        return ((HttpContent) context.getMessage()).getHttpHeader();
        
    }
    
    private class TestHttpHeader extends HttpHeader {

        @Override
        public boolean isRequest() {
            return true;
        }

        @Override
        public ProcessingState getProcessingState() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
    
}
