/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2026] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 */
package org.apache.catalina.connector;

import jakarta.servlet.http.HttpServletResponse;
import java.io.CharConversionException;
import java.nio.charset.StandardCharsets;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.util.RequestURIRef;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoyoteAdapterTest {

    @Mock
    private Connector connector;

    @Mock
    private org.glassfish.grizzly.http.server.Request grizzlyRequest;

    @Mock
    private org.glassfish.grizzly.http.server.Response grizzlyResponse;

    @Mock
    private Request catalinaRequest;

    @Mock
    private Response catalinaResponse;

    private CoyoteAdapter coyoteAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        coyoteAdapter = new CoyoteAdapter(connector);
    }

    @Test
    public void testPostParseRequestWithIllegalHexEscapePercentPercentReturns400() throws Exception {
        RequestURIRef uriRef = new RequestURIRef();
        byte[] rawUri = "/%%3".getBytes(StandardCharsets.US_ASCII);
        uriRef.init(rawUri, 0, rawUri.length);

        HttpRequestPacket requestPacket = mock(HttpRequestPacket.class);
        when(requestPacket.getRequestURIRef()).thenReturn(uriRef);
        when(grizzlyRequest.getRequest()).thenReturn(requestPacket);

        boolean result = coyoteAdapter.postParseRequest(grizzlyRequest, catalinaRequest, grizzlyResponse, catalinaResponse, true);

        assertFalse("Expected postParseRequest to return false for illegal percent escape", result);
        verify(catalinaResponse).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URI");
    }

    @Test
    public void testPostParseRequestWithIncompletePercentEscapeReturns400() throws Exception {
        RequestURIRef uriRef = new RequestURIRef();
        byte[] rawUri = "/%a".getBytes(StandardCharsets.US_ASCII);
        uriRef.init(rawUri, 0, rawUri.length);

        HttpRequestPacket requestPacket = mock(HttpRequestPacket.class);
        when(requestPacket.getRequestURIRef()).thenReturn(uriRef);
        when(grizzlyRequest.getRequest()).thenReturn(requestPacket);

        boolean result = coyoteAdapter.postParseRequest(grizzlyRequest, catalinaRequest, grizzlyResponse, catalinaResponse, true);

        assertFalse("Expected postParseRequest to return false for incomplete percent escape", result);
        verify(catalinaResponse).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URI");
    }

    @Test
    public void testPostParseRequestWithNonHexPercentEscapeReturns400() throws Exception {
        RequestURIRef uriRef = new RequestURIRef();
        byte[] rawUri = "/foo%ZZbar".getBytes(StandardCharsets.US_ASCII);
        uriRef.init(rawUri, 0, rawUri.length);

        HttpRequestPacket requestPacket = mock(HttpRequestPacket.class);
        when(requestPacket.getRequestURIRef()).thenReturn(uriRef);
        when(grizzlyRequest.getRequest()).thenReturn(requestPacket);

        boolean result = coyoteAdapter.postParseRequest(grizzlyRequest, catalinaRequest, grizzlyResponse, catalinaResponse, true);

        assertFalse("Expected postParseRequest to return false for non-hex percent escape", result);
        verify(catalinaResponse).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URI");
    }

    @Test
    public void testPostParseRequestWithCharConversionExceptionReturns400() throws Exception {
        RequestURIRef uriRef = mock(RequestURIRef.class);
        when(uriRef.getDecodedRequestURIBC()).thenThrow(new CharConversionException("Invalid character"));

        HttpRequestPacket requestPacket = mock(HttpRequestPacket.class);
        when(requestPacket.getRequestURIRef()).thenReturn(uriRef);
        when(grizzlyRequest.getRequest()).thenReturn(requestPacket);

        boolean result = coyoteAdapter.postParseRequest(grizzlyRequest, catalinaRequest, grizzlyResponse, catalinaResponse, true);

        assertFalse("Expected postParseRequest to return false on CharConversionException", result);
        verify(catalinaResponse).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URI");
    }
}
