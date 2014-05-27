/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.fileupload;

import org.apache.catalina.Globals;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

import org.glassfish.grizzly.utils.Charsets;

/**
 * <p>High level API for processing file uploads.</p>
 *
 * <p>This class handles multiple forms/files per single HTML widget, sent
 * using <code>multipart/mixed</code> encoding type, as specified by
 * <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>.
 *
 * Original authors from org.apache.common.fileupload:
 * @author <a href="mailto:Rafal.Krzewski@e-point.pl">Rafal Krzewski</a>
 * @author <a href="mailto:dlr@collab.net">Daniel Rall</a>
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:jmcnally@collab.net">John McNally</a>
 * @author <a href="mailto:martinc@apache.org">Martin Cooper</a>
 * @author Sean C. Sullivan
 *
 * Adopted for glassfish:
 * @author Kin-man Chung
 */
class RequestItemIterator {

    private static class RequestItemImpl implements RequestItem {

        // The file items content type.
        private final String contentType;

        // The file items field name.
        private final String fieldName;

        // The file items file name.
        private final String submittedFileName;

        // Whether the file item is a form field.
        private final boolean formField;

        // The file items input stream.
        private InputStream stream;

        // The headers, if any.
        private PartHeaders headers;

        /**
         * Creates a new instance.
         * @param multipart The multipart instance for accessing global properties
         * @param multiStream The multi part stream to process
         * @param pHeaders The item headers
         * @param pSubmittedFileName The items file name, or null.
         * @param pFieldName The items field name.
         * @param pContentType The items content type, or null.
         * @param pFormField Whether the item is a form field.
         * @param pContentLength The items content length, if known, or -1
         * @throws ServletException Creating the file item failed.
         */
        RequestItemImpl(Multipart multipart, MultipartStream multiStream,
                    PartHeaders pHeaders, String pSubmittedFileName, String pFieldName,
                    String pContentType, boolean pFormField,
                    long pContentLength) throws ServletException {

            headers = pHeaders;
            submittedFileName = pSubmittedFileName;
            fieldName = pFieldName;
            contentType = pContentType;
            formField = pFormField;

            stream = multiStream.newInputStream();
            long fileSizeMax = multipart.getMaxFileSize();
            if (fileSizeMax != -1) {
                if (pContentLength != -1) {
                    if (pContentLength > fileSizeMax) {
                        throw new ServletException(
                                "The field " + fieldName
                                + " exceeds its maximum permitted "
                                + " size of " + fileSizeMax
                                + " characters.");
                    }
                } else {
                    stream = new LimitedInputStream(stream, fileSizeMax) {
                        protected void raiseError(long pSizeMax, long pCount)
                                throws SizeException {
                            throw new SizeException(
                                    "The field " + fieldName
                                    + " exceeds its maximum permitted "
                                    + " size of " + pSizeMax
                                    + " characters.");
                        }
                    };
                }
            }
        }

        /**
         * Returns the items content type, or null.
         * @return Content type, if known, or null.
         */
        public String getContentType() {
            return contentType;
        }

        /**
         * Returns the items field name.
         * @return Field name.
         */
        public String getFieldName() {
            return fieldName;
        }

        /**
         * Returns the items file name.
         * @return File name, if known, or null.
         */
        public String getSubmittedFileName() {
            return submittedFileName;
        }

        /**
         * Returns, whether this is a form field.
         * @return True, if the item is a form field,
         *   otherwise false.
         */
        public boolean isFormField() {
            return formField;
        }

        /**
         * Returns an input stream, which may be used to
         * read the items contents.
         * @return Opened input stream.
         * @throws IOException An I/O error occurred.
         */
        public InputStream openStream() throws IOException {
            return stream;
        }

        /**
         * Closes the file item.
         * @throws IOException An I/O error occurred.
         */
        public void close() throws IOException {
            stream.close();
        }

        /**
         * Returns the file item headers.
         * @return The items header object
         */
        public PartHeaders getHeaders() {
            return headers;
        }

        /**
         * Sets the file item headers.
         * @param pHeaders The items header object
         */
        public void setHeaders(PartHeaders pHeaders) {
            headers = pHeaders;
        }
    }

    private static final String CONTENT_TYPE = "Content-type";
    private static final String CONTENT_LENGTH = "Content-length";
    private static final String CONTENT_DISPOSITION = "Content-disposition";
    private static final String FORM_DATA = "form-data";
    private static final String ATTACHMENT = "attachment";
    private static final String MULTIPART = "multipart/";
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    private static final String MULTIPART_MIXED = "multipart/mixed";

    private static final Charset ISO_8859_1_CHARSET = Charsets.DEFAULT_CHARSET;

    // Multipart instance, for accessing global properties, e.g. maxFileSize
    private Multipart multipart;

    // The multi part stream to process.
    private final MultipartStream multiStream;

    // The notifier, which used for triggering the
    private final MultipartStream.ProgressNotifier notifier;

    // The boundary, which separates the various parts.
    private final byte[] boundary;

    // The item, which we currently process.
    private RequestItem currentItem;

    // The current items field name.
    private String currentFieldName;

    // Whether the current item may still be read.
    private boolean itemValid;
 
    // Whether we have seen the end of the file.
    private boolean eof;

    /**
     * Creates a new instance.
     * @param request The HttpServletRequest.
     * @throws FileUploadException An error occurred while
     *   parsing the request.
     * @throws ServletException An I/O error occurred.
     */
    RequestItemIterator(Multipart multipart, HttpServletRequest request)
                throws IOException, ServletException {

        this.multipart = multipart;
        String contentType = request.getContentType();
        if ((null == contentType)
                || (!contentType.toLowerCase(Locale.ENGLISH).startsWith(MULTIPART))) {
            throw new ServletException(
                    "the request doesn't contain a "
                    + MULTIPART_FORM_DATA
                    + " or "
                    + MULTIPART_MIXED
                    + " stream, content type header is "
                    + contentType);
        }

        InputStream input = request.getInputStream();

        long sizeMax = multipart.getMaxRequestSize();
        if (sizeMax >= 0) {
            int requestSize = request.getContentLength();
            if (requestSize == -1) {
                input = new LimitedInputStream(input, sizeMax) {
                    protected void raiseError(long pSizeMax, long pCount)
                                throws SizeException {
                        throw new SizeException(
                                    "the request was rejected because"
                                    + " its size (" + pCount
                                    + ") exceeds the configured maximum"
                                    + " (" + pSizeMax + ")");
                    }
                };
            } else if (requestSize > sizeMax) {
                throw new ServletException(
                                "the request was rejected because its size ("
                                + requestSize
                                + ") exceeds the configured maximum ("
                                + sizeMax + ")");
            }
        }

        boundary = getBoundary(contentType);
        if (boundary == null) {
            throw new ServletException(
                        "the request was rejected because "
                        + "no multipart boundary was found");
        }

        notifier = new MultipartStream.ProgressNotifier(
                    multipart.getProgressListener(),
                    request.getContentLength());
        try {
            multiStream = new MultipartStream(input, boundary, notifier);
        } catch (IllegalArgumentException iae) {
            throw new ServletException ("The boundary specified in the content-type header is too long", iae);
        }
        multiStream.setHeaderEncoding(request.getCharacterEncoding());

        findNextItem();
    }

    /**
     * Called for finding the nex item, if any.
     * @return true, if an next item was found, otherwise false.
     * @throws IOException An I/O error occurred.
     */
    private boolean findNextItem() throws IOException, ServletException {

        if (eof) {
            return false;
        }

        if (currentItem != null) {
            currentItem.close();
            currentItem = null;
        }
        for (;;) {
            if (! multiStream.skipPreamble()) {
                if (currentFieldName == null) {
                    // Outer multipart terminated -> No more data
                    eof = true;
                    return false;
                }
                // Inner multipart terminated -> Return to parsing the outer
                multiStream.setBoundary(boundary);
                currentFieldName = null;
                continue;
            }
            PartHeaders headers = getParsedHeaders(multiStream.readHeaders());
            if (currentFieldName == null) {
                // We're parsing the outer multipart
                String fieldName = getFieldName(headers);
                if (fieldName != null) {
                    String subContentType = headers.getHeader(CONTENT_TYPE);
                    if (subContentType != null
                                &&  subContentType.toLowerCase(Locale.ENGLISH)
                                        .startsWith(MULTIPART_MIXED)) {
                        currentFieldName = fieldName;
                        // Multiple files associated with this field name
                        byte[] subBoundary = getBoundary(subContentType);
                        multiStream.setBoundary(subBoundary);
                        continue;
                    }
                    String fileName = getSubmittedFileName(headers);
                    currentItem = new RequestItemImpl(
                            multipart, multiStream, headers, fileName,
                            fieldName, headers.getHeader(CONTENT_TYPE),
                            fileName == null, getContentLength(headers));
                    notifier.noteItem();
                    itemValid = true;
                    return true;
                }
            } else {
                String fileName = getSubmittedFileName(headers);
                if (fileName != null) {
                    currentItem = new RequestItemImpl(
                            multipart, multiStream, headers, fileName,
                            currentFieldName,
                            headers.getHeader(CONTENT_TYPE),
                            false, getContentLength(headers));
                    notifier.noteItem();
                    itemValid = true;
                    return true;
                }
            }
            multiStream.discardBodyData();
        }
    }

    private long getContentLength(PartHeaders pHeaders) {
        try {
            return Long.parseLong(pHeaders.getHeader(CONTENT_LENGTH));
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Retrieves the file name from the <code>Content-disposition</code>
     * header.
     *
     * @param headers The HTTP headers object.
     *
     * @return The file name for the current <code>encapsulation</code>.
     */
    protected String getSubmittedFileName(PartHeaders headers) {
        return getSubmittedFileName(headers.getHeader(CONTENT_DISPOSITION));
    }

    /**
     * Returns the given content-disposition headers file name.
     * @param pContentDisposition The content-disposition headers value.
     * @return The file name
     */
    private String getSubmittedFileName(String pContentDisposition) {
        String fileName = null;
        if (pContentDisposition != null) {
            String cdl = pContentDisposition.toLowerCase(Locale.ENGLISH);
            if (cdl.startsWith(FORM_DATA) || cdl.startsWith(ATTACHMENT)) {
                ParameterParser parser = new ParameterParser();
                parser.setLowerCaseNames(true);
                // Parameter parser can handle null input
                Map<String, String> params =
                        parser.parse(pContentDisposition, ';');
                if (params.containsKey("filename")) {
                    fileName = params.get("filename");
                    if (fileName != null) {
                        fileName = fileName.trim();
                    } else {
                        // Even if there is no value, the parameter is present,
                        // so we return an empty file name rather than no file
                        // name.
                        fileName = "";
                    }
                }
            }
        }
        return fileName;
    }


    /**
     * Retrieves the field name from the <code>Content-disposition</code>
     * header.
     *
     * @param headers A <code>Map</code> containing the HTTP request headers.
     *
     * @return The field name for the current <code>encapsulation</code>.
     */
    protected String getFieldName(PartHeaders headers) {
        return getFieldName(headers.getHeader(CONTENT_DISPOSITION));
    }

    /**
     * Returns the field name, which is given by the content-disposition
     * header.
     * @param pContentDisposition The content-dispositions header value.
     * @return The field jake
     */
    private String getFieldName(String pContentDisposition) {
        String fieldName = null;
        if (pContentDisposition != null
                && pContentDisposition.toLowerCase(Locale.ENGLISH).startsWith(FORM_DATA)) {
            ParameterParser parser = new ParameterParser();
            parser.setLowerCaseNames(true);
            // Parameter parser can handle null input
            Map<String, String> params = parser.parse(pContentDisposition, ';');
            fieldName = params.get("name");
            if (fieldName != null) {
                fieldName = fieldName.trim();
            }
        }
        return fieldName;
    }

    /**
     * <p> Parses the <code>header-part</code> and returns as key/value
     * pairs.
     *
     * <p> If there are multiple headers of the same names, the name
     * will map to a comma-separated list containing the values.
     *
     * @param headerPart The <code>header-part</code> of the current
     *                   <code>encapsulation</code>.
     *
     * @return A PartHeaders which hass a <code>Map</code> containing
     *     the parsed HTTP request headers.
     */
    protected PartHeaders getParsedHeaders(String headerPart) {

        final int len = headerPart.length();
        PartHeaders headers = new PartHeaders();
        int start = 0;
        for (;;) {
            int end = parseEndOfLine(headerPart, start);
            if (start == end) {
                break;
            }
            StringBuilder header =
                new StringBuilder(headerPart.substring(start, end));
            start = end + 2;
            while (start < len) {
                int nonWs = start;
                while (nonWs < len) {
                    char c = headerPart.charAt(nonWs);
                    if (c != ' '  &&  c != '\t') {
                        break;
                    }
                    ++nonWs;
                }
                if (nonWs == start) {
                    break;
                }
                // Continuation line found
                end = parseEndOfLine(headerPart, nonWs);
                header.append(" ").append(headerPart.substring(nonWs, end));
                start = end + 2;
            }
            final int colonOffset = header.indexOf(":");
            if (colonOffset == -1) {
                // This header line is malformed, skip it.
                continue;
            }
            String headerName = header.substring(0, colonOffset).trim();
            String headerValue =
                header.substring(header.indexOf(":") + 1).trim();
            headers.addHeader(headerName, headerValue);
        }
        return headers;
    }

    /**
     * Skips bytes until the end of the current line.
     * @param headerPart The headers, which are being parsed.
     * @param end Index of the last byte, which has yet been
     *   processed.
     * @return Index of the \r\n sequence, which indicates
     *   end of line.
     */
    private int parseEndOfLine(String headerPart, int end) {
        int index = end;
        for (;;) {
            int offset = headerPart.indexOf('\r', index);
            if (offset == -1  ||  offset + 1 >= headerPart.length()) {
                throw new IllegalStateException(
                    "Expected headers to be terminated by an empty line.");
            }
            if (headerPart.charAt(offset + 1) == '\n') {
                return offset;
            }
            index = offset + 1;
        }
    }

    /**
     * Returns, whether another instance of {@link FileItemStream}
     * is available.
     * @throws FileUploadException Parsing or processing the
     *   file item failed.
     * @throws IOException Reading the file item failed.
     * @return true, if one or more additional file items
     *   are available, otherwise false.
     */
    public boolean hasNext() throws ServletException, IOException {
        if (eof) {
            return false;
        }
        if (itemValid) {
            return true;
        }
        return findNextItem();
    }

    /**
     * Returns the next available {@link FileItemStream}.
     * @throws ServletException Parsing or processing the
     *   file item failed.
     * @throws IOException Reading the file item failed.
     * @return RequestItem instance
     */
    public RequestItem next() throws ServletException, IOException {

        itemValid = false;
        return currentItem;
    }


    /**
     * Retrieves the boundary from the <code>Content-type</code> header.
     *
     * @param contentType The value of the content type header from which to
     *                    extract the boundary value.
     *
     * @return The boundary, as a byte array.
     */
    protected byte[] getBoundary(String contentType) {
        ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        // Parameter parser can handle null input
        Map<String, String> params =
            parser.parse(contentType, new char[] {';', ','});
        String boundaryStr = params.get("boundary");

        if (boundaryStr == null) {
            return null;
        }
        byte[] boundary;
        try {
            boundary = boundaryStr.getBytes(ISO_8859_1_CHARSET);
        } catch (Exception e) {
            boundary = boundaryStr.getBytes(Charset.defaultCharset());
        }
        return boundary;
    }

}
