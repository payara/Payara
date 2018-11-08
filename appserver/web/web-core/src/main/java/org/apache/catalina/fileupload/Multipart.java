/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

/**
 * This class is the base for implementing servlet 3.0 file upload
 *
 * @author Kin-man Chung
 */

package org.apache.catalina.fileupload;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.catalina.connector.Request;

public class Multipart {

    private final String location;
    private final long maxFileSize;
    private final long maxRequestSize;
    private final int fileSizeThreshold;
    private File repository;
    private ProgressListener listener;

    private final Request request;
    private ArrayList<Part> parts;
    private List<Part> unmodifiableParts;

    public Multipart(Request request,
                String location, long maxFileSize, long maxRequestSize,
                int fileSizeThreshold) {
        this.request = request;
        this.location = location;
        this.maxFileSize = maxFileSize;
        this.maxRequestSize = maxRequestSize;
        this.fileSizeThreshold = fileSizeThreshold;
        repository = (File) request.getServletContext().getAttribute(
            ServletContext.TEMPDIR);
        if (location != null && location.length() != 0) {
            File tempFile= new File(location);
            if (tempFile.isAbsolute()) {
                repository = tempFile;
            } else {
                repository = new File(repository, location);
            }
        }
    }

    public void init() {
        try {
            initParts();
        } catch (Exception ex) {
            throw new RuntimeException("Error in multipart initialization", ex);
        }
    }

    public String getLocation() {
        return location;
    }

    public int getFileSizeThreshold() {
        return fileSizeThreshold;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public long getMaxRequestSize() {
        return maxRequestSize;
    }

    public File getRepository() {
        return repository;
    }

    private boolean isMultipart() {

        if (!request.getMethod().toLowerCase(Locale.ENGLISH).equals("post")) {
            return false;
        }
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }
        if (contentType.toLowerCase(Locale.ENGLISH).startsWith("multipart/form-data")) {
            return true;
        }
        return false;
    }

    private void initParts() throws IOException, ServletException {
        if (parts != null) {
            return;
        }
        parts = new ArrayList<>();
        try {
            RequestItemIterator iter = new RequestItemIterator(this, request);
            while (iter.hasNext()) {
                RequestItem requestItem = iter.next();
                PartItem partItem = new PartItem(this,
                                         requestItem.getHeaders(),
                                         requestItem.getFieldName(),
                                         requestItem.getContentType(),
                                         requestItem.isFormField(),
                                         requestItem.getSubmittedFileName(),
                                         request.getCharacterEncoding());
                try (InputStream itemInputStream = requestItem.openStream()) {
                    Streams.copy(itemInputStream, partItem.getOutputStream(), true);
                    String fileName = partItem.getSubmittedFileName();
                    if (fileName == null || fileName.length() == 0) {
                        // Add part name and value as a parameter
                        request.addParameter(partItem.getName(),
                                             new String[] {partItem.getString()});
                    }
                    parts.add(partItem);
                }
            }
        } catch (SizeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public synchronized Collection<Part> getParts()
            throws IOException, ServletException {
        if (! isMultipart()) {
            throw new ServletException("The request content-type is not a multipart/form-data");
        }

        initParts();

        if (null == unmodifiableParts) {
            unmodifiableParts = Collections.unmodifiableList(parts);
        }

        return unmodifiableParts;
    }

    public Part getPart(String name) throws IOException, ServletException {

        if (! isMultipart()) {
            throw new ServletException("The request content-type is not a multipart/form-data");
        }

        initParts();
        for (Part part: parts) {
            String fieldName = part.getName();
            if (name.equals(fieldName)) {
                return part;
            }
        }
        return null;
    }

    /**
     * Returns the progress listener.
     * @return The progress listener, if any, or null.
     */
    public ProgressListener getProgressListener() {
        return listener;
    }

    /**
     * Sets the progress listener.
     * @param pListener The progress listener, if any. Defaults to null.
     */
    public void setProgressListener(ProgressListener pListener) {
        listener = pListener;
    }

}

