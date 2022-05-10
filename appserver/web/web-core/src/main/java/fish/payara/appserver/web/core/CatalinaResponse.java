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

package fish.payara.appserver.web.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Request;
import org.apache.coyote.ContinueResponseTiming;
import org.glassfish.grizzly.http.server.Response;

public class CatalinaResponse extends org.apache.catalina.connector.Response {
    private final Response grizzlyResponse;

    CatalinaResponse(Response grizzlyResponse) {
        this.grizzlyResponse = grizzlyResponse;
    }

    @Override
    public org.apache.coyote.Response getCoyoteResponse() {
        return super.getCoyoteResponse();
    }

    @Override
    public void setCoyoteResponse(org.apache.coyote.Response coyoteResponse) {
        super.setCoyoteResponse(coyoteResponse);
    }

    @Override
    public Context getContext() {
        return super.getContext();
    }

    @Override
    public void recycle() {
        super.recycle();
        coyoteResponse.recycle();
    }

    @Override
    public List<Cookie> getCookies() {
        return super.getCookies();
    }

    @Override
    public long getContentWritten() {
        return super.getContentWritten();
    }

    @Override
    public long getBytesWritten(boolean flush) {
        return super.getBytesWritten(flush);
    }

    @Override
    public boolean isAppCommitted() {
        return super.isAppCommitted();
    }

    @Override
    public void setAppCommitted(boolean appCommitted) {
        super.setAppCommitted(appCommitted);
    }

    @Override
    public Request getRequest() {
        return super.getRequest();
    }

    @Override
    public void setRequest(Request request) {
        super.setRequest(request);
    }

    @Override
    public HttpServletResponse getResponse() {
        return super.getResponse();
    }

    @Override
    public void setResponse(HttpServletResponse applicationResponse) {
        super.setResponse(applicationResponse);
    }

    @Override
    public boolean isSuspended() {
        return super.isSuspended();
    }

    @Override
    public void setSuspended(boolean suspended) {
        super.setSuspended(suspended);
    }

    @Override
    public boolean isClosed() {
        return super.isClosed();
    }

    @Override
    public boolean setError() {
        return super.setError();
    }

    @Override
    public boolean isError() {
        return super.isError();
    }

    @Override
    public boolean isErrorReportRequired() {
        return super.isErrorReportRequired();
    }

    @Override
    public boolean setErrorReported() {
        return super.setErrorReported();
    }

    @Override
    public void finishResponse() throws IOException {
        grizzlyResponse.finish();
    }

    @Override
    public int getContentLength() {
        return super.getContentLength();
    }

    @Override
    public void setContentLength(int length) {
        grizzlyResponse.setContentLength(length);
    }

    @Override
    public String getContentType() {
        return super.getContentType();
    }

    @Override
    public void setContentType(String type) {
        grizzlyResponse.setContentType(type);
    }

    @Override
    public PrintWriter getReporter() throws IOException {
        return super.getReporter();
    }

    @Override
    public void flushBuffer() throws IOException {
        grizzlyResponse.getOutputBuffer().flush();
    }

    @Override
    public int getBufferSize() {
        return grizzlyResponse.getBufferSize();
    }

    @Override
    public void setBufferSize(int size) {
        grizzlyResponse.setBufferSize(size);
    }

    @Override
    public String getCharacterEncoding() {
        return grizzlyResponse.getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(String charset) {
        grizzlyResponse.setCharacterEncoding(charset);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        // TODO: wrap
        return super.getOutputStream();
    }

    @Override
    public Locale getLocale() {
        return grizzlyResponse.getLocale();
    }

    @Override
    public void setLocale(Locale locale) {
        grizzlyResponse.setLocale(locale);
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(grizzlyResponse.getWriter());
    }

    @Override
    public boolean isCommitted() {
        return grizzlyResponse.isCommitted();
    }

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    public void resetBuffer() {
        grizzlyResponse.resetBuffer();
    }

    @Override
    public void resetBuffer(boolean resetWriterStreamFlags) {
        super.resetBuffer(resetWriterStreamFlags);
    }

    @Override
    public void setContentLengthLong(long length) {
        grizzlyResponse.setContentLengthLong(length);
    }

    @Override
    public String getHeader(String name) {
        return grizzlyResponse.getHeader(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return Arrays.asList(grizzlyResponse.getHeaderNames());
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return Arrays.asList(grizzlyResponse.getHeaderValues(name));
    }

    @Override
    public String getMessage() {
        return grizzlyResponse.getMessage();
    }

    @Override
    public int getStatus() {
        return grizzlyResponse.getStatus();
    }

    @Override
    public void setStatus(int status) {
        grizzlyResponse.setStatus(status);
    }

    @Override
    public void addCookie(Cookie cookie) {
        grizzlyResponse.addCookie(translateCookie(cookie));
    }

    private org.glassfish.grizzly.http.Cookie translateCookie(Cookie cookie) {
        var grizzlyCookie = new org.glassfish.grizzly.http.Cookie(cookie.getName(), cookie.getValue());
        grizzlyCookie.setComment(cookie.getComment());
        grizzlyCookie.setDomain(cookie.getDomain());
        grizzlyCookie.setPath(cookie.getPath());
        grizzlyCookie.setSecure(cookie.getSecure());
        grizzlyCookie.setHttpOnly(cookie.isHttpOnly());
        grizzlyCookie.setMaxAge(cookie.getMaxAge());
        return grizzlyCookie;
    }

    @Override
    public void addSessionCookieInternal(Cookie cookie) {
        super.addSessionCookieInternal(cookie);
    }

    @Override
    public String generateCookieString(Cookie cookie) {
        return super.generateCookieString(cookie);
    }

    @Override
    public void addDateHeader(String name, long value) {
        grizzlyResponse.addDateHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        grizzlyResponse.addHeader(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        grizzlyResponse.addIntHeader(name, value);
    }

    @Override
    public boolean containsHeader(String name) {
        return grizzlyResponse.containsHeader(name);
    }

    @Override
    public Supplier<Map<String, String>> getTrailerFields() {
        return grizzlyResponse.getTrailers();
    }

    @Override
    public void setTrailerFields(Supplier<Map<String, String>> supplier) {
        grizzlyResponse.setTrailers(supplier);
    }

    @Override
    public String encodeRedirectURL(String url) {
        return grizzlyResponse.encodeRedirectURL(url);
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return super.encodeRedirectUrl(url);
    }

    @Override
    public String encodeURL(String url) {
        return grizzlyResponse.encodeURL(url);
    }

    @Override
    public String encodeUrl(String url) {
        return super.encodeUrl(url);
    }

    @Override
    public void sendAcknowledgement(ContinueResponseTiming continueResponseTiming) throws IOException {
        // todo continueResponseTiming
        grizzlyResponse.sendAcknowledgement();
    }

    @Override
    public void sendError(int status) throws IOException {
        grizzlyResponse.sendError(status);
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        grizzlyResponse.sendError(status, message);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        grizzlyResponse.sendRedirect(location);
    }

    @Override
    public void sendRedirect(String location, int status) throws IOException {
        // not supported in grizzly
        super.sendRedirect(location, status);
    }

    @Override
    public void setDateHeader(String name, long value) {
        grizzlyResponse.setDateHeader(name, value);
    }

    @Override
    public void setHeader(String name, String value) {
        grizzlyResponse.setHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        grizzlyResponse.setIntHeader(name, value);
    }

    @Override
    public void setStatus(int status, String message) {
        setStatus(status);
        grizzlyResponse.setDetailMessage(message);
    }

    @Override
    protected boolean isEncodeable(String location) {
        return super.isEncodeable(location);
    }

    @Override
    protected String toAbsolute(String location) {
        return super.toAbsolute(location);
    }

    @Override
    protected String toEncoded(String url, String sessionId) {
        return super.toEncoded(url, sessionId);
    }

}
