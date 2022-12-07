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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Request;
import org.apache.coyote.ContinueResponseTiming;
import org.apache.tomcat.util.security.Escape;
import org.glassfish.grizzly.WriteHandler;
import org.glassfish.grizzly.http.io.OutputBuffer;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.http.util.MimeHeaders;

public class CatalinaResponse extends org.apache.catalina.connector.Response {
    private static final Logger LOG = Logger.getLogger(CatalinaResponse.class.getName());

    private Response grizzlyResponse;

    private final AtomicBoolean errorReported = new AtomicBoolean();

    private volatile boolean outputSuspended;

    CatalinaResponse() {
        super();
    }

    @Override
    public org.apache.coyote.Response getCoyoteResponse() {
        // we cannot be strict about not allowing CoyoteResponse, because getting isIoAllowed flag is in main
        // error handling code path
        return super.getCoyoteResponse();
    }

    @Override
    public void setCoyoteResponse(org.apache.coyote.Response coyoteResponse) {
        throw new UnsupportedOperationException("We don't touch Coyote response");
    }

    @Override
    public void recycle() {
        super.recycle();
        coyoteResponse.recycle();
        errorReported.set(false);
    }

    @Override
    public List<Cookie> getCookies() {
        return super.getCookies();
    }

    @Override
    public long getContentWritten() {
        // TODO: need to manually count from buffers/writers.
        return -1;
    }

    @Override
    public long getBytesWritten(boolean flush) {
        if (flush) {
            try {
                grizzlyResponse.flush();
            } catch (IOException e) {
                // catalina codebase suggests to ignore
            }
        }
        // TODO: need to implement counters
        return -1;
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
    public CatalinaRequest getRequest() {
        return (CatalinaRequest) super.getRequest();
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
        return outputSuspended;
    }

    @Override
    public void setSuspended(boolean suspended) {
        this.outputSuspended = suspended;
    }

    @Override
    public boolean isClosed() {
        return grizzlyResponse.getOutputBuffer().isClosed();
    }

    @Override
    public boolean isErrorReportRequired() {
        return grizzlyResponse.isError() && !errorReported.get();
    }

    @Override
    public boolean setErrorReported() {
        return grizzlyResponse.isError() && errorReported.compareAndSet(false, true);
    }

    @Override
    public void finishResponse() throws IOException {
        grizzlyResponse.finish();
    }

    @Override
    public int getContentLength() {
        return grizzlyResponse.getContentLength();
    }

    @Override
    public void setContentLength(int length) {
        grizzlyResponse.setContentLength(length);
    }

    @Override
    public String getContentType() {
        return grizzlyResponse.getContentType();
    }

    @Override
    public void setContentType(String type) {
        grizzlyResponse.setContentType(type);
    }

    @Override
    public PrintWriter getReporter() throws IOException {
        try {
            grizzlyResponse.resetBuffer(true);
            return getWriter();
        } catch (RuntimeException e) {
            // response has been already written to
            return null;
        }
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
        return new GrizzlyServletOutputStream();
    }

    @Override
    public Locale getLocale() {
        return grizzlyResponse.getLocale();
    }

    @Override
    public void setLocale(Locale locale) {
        // re-telling the super method for grizzly
        if (isCommitted()) {
            return;
        }

        // Ignore any call from an included servlet
        if (included) {
            return;
        }

        grizzlyResponse.setLocale(locale);


        if (grizzlyResponse.getResponse().getCharacterEncoding() != null) {
            return;
        }

        if (locale == null) {
            grizzlyResponse.setCharacterEncoding(null);
        } else {
            // In some error handling scenarios, the context is unknown
            // (e.g. a 404 when a ROOT context is not present)
            Context context = getContext();
            if (context != null) {
                String charset = context.getCharset(locale);
                if (charset != null) {
                    grizzlyResponse.setCharacterEncoding(charset);
                }
            }
        }
    }

    @Override
    public boolean isCommitted() {
        return grizzlyResponse.isCommitted();
    }

    @Override
    public Context getContext() {
        return super.getContext();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        try {
            return new PrintWriter(new GrizzlyServletWriter());
        } catch (UnsupportedCharsetException uce) {
            var ex = new UnsupportedEncodingException(uce.getMessage());
            ex.initCause(uce);
            throw ex;
        }
    }

    @Override
    public void reset() {
        grizzlyResponse.reset();
        grizzlyResponse.setCharacterEncoding(null); //grizzly resets content type in wrong order
    }

    @Override
    public void resetBuffer() {
        grizzlyResponse.resetBuffer();
    }

    @Override
    public void resetBuffer(boolean resetWriterStreamFlags) {
        grizzlyResponse.resetBuffer(resetWriterStreamFlags);
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
        return new ArrayList<>(Arrays.asList(grizzlyResponse.getHeaderNames()));
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return new ArrayList<>(Arrays.asList(grizzlyResponse.getHeaderValues(name)));
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
        // grizzly supports cookies but not exactly in servlet compliant way (doesn't set max-age on version 0 cookies)

        // Ignore any call from an included servlet
        if (included || isCommitted()) {
            return;
        }

        getCookies().add(cookie);

        String header = generateCookieString(cookie);
        //if we reached here, no exception, cookie is valid
        // the header name is Set-Cookie for both "old" and v.1 ( RFC2109 )
        // RFC2965 is not supported by browsers and the Servlet spec
        // asks for 2109.
        // Tomcat supports different encoding for cookies, Grizzly, however, does not.
        addHeader("Set-Cookie", header);
    }

    @Override
    public void addSessionCookieInternal(Cookie cookie) {
        if (isCommitted()) {
            return;
        }

        String name = cookie.getName();
        final String headername = "Set-Cookie";
        final String startsWith = name + "=";
        String header = generateCookieString(cookie);
        boolean set = false;
        MimeHeaders headers = grizzlyResponse.getResponse().getHeaders();
        int n = headers.size();
        for (int i = 0; i < n; i++) {
            if (headers.getName(i).toString().equals(headername)) {
                if (headers.getValue(i).toString().startsWith(startsWith)) {
                    headers.getValue(i).setString(header);
                    set = true;
                }
            }
        }
        if (!set) {
            addHeader(headername, header);
        }
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
    public String encodeURL(String url) {
        return grizzlyResponse.encodeURL(url);
    }


    @Override
    public void sendAcknowledgement(ContinueResponseTiming continueResponseTiming) throws IOException {
        // todo continueResponseTiming
        grizzlyResponse.sendAcknowledgement();
    }

    @Override
    public void sendError(int status) throws IOException {
        super.sendError(status);
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        if (isCommitted()) {
            throw new IllegalStateException
                    (sm.getString("coyoteResponse.sendError.ise"));
        }

        // Ignore any call from an included servlet
        if (included) {
            return;
        }

        setError();

        // this is almost like reset, but headers are retained
        grizzlyResponse.getResponse().getHeaders().removeHeader(Header.TransferEncoding);
        grizzlyResponse.resetBuffer(true);
        grizzlyResponse.setLocale(null);
        grizzlyResponse.setContentLengthLong(-1L);
        grizzlyResponse.getResponse().setChunked(false);
        grizzlyResponse.setContentType((String) null);
        grizzlyResponse.setCharacterEncoding(null);


        String nonNullMsg = message;
        if (nonNullMsg == null) {
            final HttpStatus httpStatus = HttpStatus.getHttpStatus(status);
            if (httpStatus != null && httpStatus.getReasonPhrase() != null) {
                nonNullMsg = httpStatus.getReasonPhrase();
            } else {
                nonNullMsg = "Unknown Error";
            }
        }
        grizzlyResponse.setStatus(status, nonNullMsg);

        setSuspended(true);
    }

    @Override
    public boolean setError() {
        var wasError = isError();
        if (wasError) {
            return false;
        }
        grizzlyResponse.setError();
        return true;
    }

    @Override
    public boolean isError() {
        return grizzlyResponse.isError();
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        super.sendRedirect(location);
    }

    @Override
    public void sendRedirect(String location, int status) throws IOException {
        if (isCommitted()) {
            throw new IllegalStateException(sm.getString("coyoteResponse.sendRedirect.ise"));
        }

        // Ignore any call from an included servlet
        if (included) {
            return;
        }

        // Clear any data content that has been buffered
        resetBuffer(true);

        // Generate a temporary redirect to the specified location
        try {
            String locationUri;
            // Relative redirects require HTTP/1.1
            if (getRequest().supportsRelativeRedirects() &&
                    getContext().getUseRelativeRedirects()) {
                locationUri = location;
            } else {
                locationUri = toAbsolute(location);
            }
            setStatus(status);
            setHeader("Location", locationUri);
            if (getContext().getSendRedirectBody()) {
                PrintWriter writer = getWriter();
                writer.print(sm.getString("coyoteResponse.sendRedirect.note",
                        Escape.htmlElementContent(locationUri)));
                flushBuffer();
            }
        } catch (IllegalArgumentException e) {
            LOG.log(Level.WARNING, sm.getString("response.sendRedirectFail", location), e);
            setStatus(SC_NOT_FOUND);
        }

        // Cause the response to be finished (from the application perspective)
        setSuspended(true);
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

    void setResponses(org.apache.coyote.Response response, Response grizzlyResponse) {
        super.setCoyoteResponse(response);
        this.grizzlyResponse = grizzlyResponse;
    }

    private class GrizzlyServletOutputStream extends ServletOutputStream {
        OutputBuffer buffer = grizzlyResponse.getOutputBuffer();

        OutputStream outputStream = grizzlyResponse.getOutputStream();

        @Override
        public boolean isReady() {
            return buffer.canWrite();
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            buffer.notifyCanWrite(new WriteHandler() {
                @Override
                public void onWritePossible() throws Exception {
                    writeListener.onWritePossible();
                }

                @Override
                public void onError(Throwable t) {
                    writeListener.onError(t);
                }
            });
        }

        @Override
        public void write(int b) throws IOException {
            if (outputSuspended) {
                return;
            }
            outputStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            if (outputSuspended) {
                return;
            }
            outputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (outputSuspended) {
                return;
            }
            outputStream.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            if (outputSuspended) {
                return;
            }
            outputStream.flush();
        }

        @Override
        public void close() throws IOException {
            if (outputSuspended) {
                return;
            }
            outputStream.close();
        }
    }

    private class GrizzlyServletWriter extends Writer {
        Writer delegate = grizzlyResponse.getWriter();

        @Override
        public void write(int c) throws IOException {
            if (outputSuspended) {
                return;
            }
            delegate.write(c);
        }

        @Override
        public void write(char[] cbuf) throws IOException {
            if (outputSuspended) {
                return;
            }
            delegate.write(cbuf);
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            if (outputSuspended) {
                return;
            }
            delegate.write(cbuf, off, len);
        }

        @Override
        public void write(String str) throws IOException {
            if (outputSuspended) {
                return;
            }
            delegate.write(str);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            if (outputSuspended) {
                return;
            }
            delegate.write(str, off, len);
        }

        @Override
        public Writer append(CharSequence csq) throws IOException {
            if (outputSuspended) {
                return this;
            }
            return delegate.append(csq);
        }

        @Override
        public Writer append(CharSequence csq, int start, int end) throws IOException {
            if (outputSuspended) {
                return this;
            }
            return delegate.append(csq, start, end);
        }

        @Override
        public Writer append(char c) throws IOException {
            if (outputSuspended) {
                return this;
            }
            return delegate.append(c);
        }

        @Override
        public void flush() throws IOException {
            if (outputSuspended) {
                return;
            }
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            if (outputSuspended) {
                return;
            }
            delegate.close();
        }
    }
}
