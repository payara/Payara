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

package org.glassfish.api.admin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Properties;

/**
 * Interface for admin command payloads--data carried
 * in the http request and response which flow between the admin client and the server.
 * This API also allows the requester to ask that the receiver remove a file,
 * presumably one that was transferred via an earlier request payload.
 * <h2>Motivation and Overview</h2>
 * The API is intended to be a simple abstraction of the
 * input and output streams in HTTP requests and responses,
 * inspired by the concepts from the mail API for
 * MIME body parts and multiparts.
 * A payload can contain zero or more parts.
 * If a payload contains only one part then only that part is sent or received
 * in the payload.  If, on the other hand, the payload contains multiple parts
 * then the payload as a whole is a "multipart" payload which in turn contains
 * the individual parts.
 * <h2>Usage</h2>
 * <h3>Outbound</h3>
 * Code (on the client or the server) that needs to place data in the payload or
 * request that earlier-sent data be removed
 * would instantiate an implementation of {@link Payload.Outbound}. (The
 * {@link org.glassfish.admin.payload.PayloadImpl.Outbound} class is such an implementation.)
 * To this Payload.Outbound object the code can add parts using any of the {@link Outbound#addPart}
 * methods or the {@link Outbound#attachFile} or {@link Outbound#requestFileRemoval} convenience methods.
 * After the code has added all
 * the relevant parts to the payload it invokes the {@link Outbound#writeTo} method to write
 * the outbound payload to an OutputStream.  Typically the caller will pass the
 * output stream associated with the request (in the client case) or the response (in the
 * server case).
 * <h3>Inbound</h3>
 * Code that needs to read the payload from a request (as the server does) or
 * from a response (as the client does) will instantiate an implementation of
 * {@link Payload.Inbound}. (The {@link org.glassfish.admin.payload.PayloadImpl.Inbound}
 * is such an implementation.
 * <p>
 * Payload.Inbound exposes the {@link Payload.Inbound#parts()} method
 * which returns an Iterator&lt;Part&gt;.  With each {@link Part} available through the
 * iterator the caller can use {@link Part#getContentType()},
 * {@link Part#getName()}, and {@link Part#getInputStream()}.
 * Note that the caller should close the InputStream returned by getInputStream()
 * once it has read the data from the stream.
 * <h2>Part Properties</h2>
 * Each Part can carry with it Properties.  The conventions as to what property
 * names are used and what they mean can vary from one admin command to the next.
 * For a file transfer these properties should be set:
 * <ul>
 * <li>data-request-type=file-xfer
 * <li>data-request-name=(anything - typically the option name from the command to
 * help identify which part goes with which command argument)
 * <li>file-xfer-root=root path expression in the file system syntax of the system
 * where tranferred files will reside.
 * <li>last-modified=String representation of the long value recording the last-modified time of the original file
 * </ul>
 * As an example as the server processes the "--retrieve xxx" option on the deploy and redeploy commands
 * it will specify file-xfer-root as xxx (in this example) to indicate where the
 * downloaded app client files should be stored on the
 * local system.  Placing such information in the Part's properties helps to
 * decouple the admin client from the specifics of the command which triggered
 * the file transfer.  That is, for example, the admin client does not need to
 * fetch the "--retrieve" option value to know where to store the transferred files
 * because the server placed this information with each part.  The admin client
 * can therefore process this type of admin data request generically, without
 * regard to what command triggered the transfer.
 * <p>
 * The receiver of the file transfer request should return the
 * file-xfer-root value back to the requester without change so the local file path
 * syntax can be used correctly on the system where the file will exist.  
 * <h2>Notes and Restrictions</h2>
 * In some implementations the caller must consume the contents of a
 * Part's input stream before advancing to the next Part.  All callers should
 * follow this practice regardless of the underlying implementation used, in case
 * that implementation were to change in the future.
 *
 * @author tjquinn
 */
public interface Payload {

    public static final String PAYLOAD_HEADER_NAME = "org.glassfish.payload.admin.data";

    /**
     * Public API for outbound Payloads.
     */
    public static interface Outbound {
        
        /** Count of attached parts
         */
        public int size();

        /**
         * Adds a part of the specified content type, name, and String content
         * to the payload.
         * @param contentType content type of the part
         * @param name name to be assigned to the part
         * @param props Properties to be included with the part
         * @param content String containing the content for the part
         * @throws java.io.IOException
         */
        public void addPart(
                final String contentType,
                final String name,
                final Properties props,
                final String content) throws IOException;

        /**
         * Adds a part of the specified content type, name, and content to
         * the payload.
         * @param contentType content type of the part
         * @param name name to be assigned to the part
         * @param props Properties to be included with the part
         * @param content InputStream furnishing the content for this part
         * @throws java.io.IOException
         */
        public void addPart(
                final String contentType,
                final String name,
                final Properties props,
                final InputStream content) throws IOException;

        /**
         * Adds a part of the specified content type, name, and content at a
         * specified position in the parts of the payload.
         * @param index position (zero-based) where the part should be added
         * @param contentType content type of the part
         * @param name name to be assigned to thepart
         * @param props Properties to be included with the part
         * @param content InputStream furnishing the content for this part
         * @throws java.io.IOException
         */
        public void addPart(
                final int index,
                final String contentType,
                final String name,
                final Properties props,
                final InputStream content
                ) throws IOException;

        /**
         * Adds a part to the payload of the given content type from the
         * specified file.  The name assigned to the new part is the URI
         * for the file relativized according to the specified file URI.  The
         * properties which indicate that this is file transfer data request
         * are set automatically.
         * <p>
         * If the <code>file</code> argument specifies a directory, only the
         * directory - not its contents - are attached to the payload.  To 
         * include the directory and its contents use {@link #attachFile(java.lang.String, java.net.URI, java.lang.String, java.io.File, boolean) }
         * and specify the <code>recursive</code> argument as <code>true</code>.
         * @param contentType content type of the part
         * @param fileURI URI relative to which the part's name should be computed
         * @param dataRequestName name identifying which part of a request this file answers
         * @param file File containing the content for the part
         * @throws java.io.IOException
         */
        public void attachFile(
                final String contentType,
                final URI fileURI,
                final String dataRequestName,
                final File file) throws IOException;

        /**
         * Adds a part to the payload of the given content type from the
         * specified file.  The name assigned to the new part is the URI
         * for the file relativized according to the specified file URI.  The
         * properties which indicate that this is file transfer data request
         * are set automatically.
         * @param contentType content type of the part
         * @param fileURI URI relative to which the part's name should be computed
         * @param dataRequestName name identifying which part of a request this file answers
         * @param file File containing the content for the part
         * @param isRecursive if file is a directory, whether to add its contents as well
         * @throws java.io.IOException
         */
        public void attachFile(
                final String contentType,
                final URI fileURI,
                final String dataRequestName,
                final File file,
                final boolean isRecursive) throws IOException;

        /**
         * Adds a part to the payload of the given content type from the
         * specified file.  The name assigned to the new part is the URI
         * for the file relativized according to the specified file URI.  The
         * properties which indicate that this is a file transfer data request
         * are set automatically and added to the properties passed by the caller.
         * @param contentType content type of the part
         * @param fileURI URI relative to which the part's name should be computed
         * @param dataRequestName name identifying which part of a request this file answers
         * @param props Properties to be included with the part
         * @param file File containing the content for the part
         * @throws java.io.IOException
         */
        public void attachFile(
                final String contentType,
                final URI fileURI,
                final String dataRequestName,
                final Properties props,
                final File file) throws IOException;

        /**
         * Adds a part to the payload of the given content type from the
         * specified file.  The name assigned to the new part is the URI
         * for the file relativized according to the specified file URI.  The
         * properties which indicate that this is a file transfer data request
         * are set automatically and added to the properties passed by the caller.
         * @param contentType content type of the part
         * @param fileURI URI relative to which the part's name should be computed
         * @param dataRequestName name identifying which part of a request this file answers
         * @param props Properties to be included with the part
         * @param file File containing the content for the part
         * @param isRecursive if file is a directory, whether to add its contents as well
         * @throws java.io.IOException
         */
        public void attachFile(
                final String contentType,
                final URI fileURI,
                final String dataRequestName,
                final Properties props,
                final File file,
                final boolean isRecursive) throws IOException;

        /**
         * Adds a part to the payload that represents a request to remove the
         * specified file, presumably previously transferred in a payload
         * during an earlier request.
         * @param fileURI relative URI of the file for deletion
         * @param dataRequestName name identifying which part of a request triggered the file removal
         * @param props Properties to be included with the part
         * @throws IOException
         */
        public void requestFileRemoval(
                final URI fileURI,
                final String dataRequestName,
                final Properties props) throws IOException;

        /**
         * Adds a part to the payload that represents a request to remove the
         * specified file, presumably previously transferred in a payload
         * during an earlier request.
         * @param fileURI relative URI of the file for deletion
         * @param dataRequestName name identifying which part of a request triggered the file removal
         * @param props Properties to be included with the part
         * @param isRecursive if fileURI is a directory, whether to remove its contents as well
         * @throws IOException
         */
        public void requestFileRemoval(
                final URI fileURI,
                final String dataRequestName,
                final Properties props,
                final boolean isRecursive) throws IOException;

        /**
         * Adds a part to the payload to request that the specified file be
         * replaced.  
         * <p>
         * If the fileURI translates to a non-directory file on the receiving
         * system then calling this method will replace the file's contents
         * on the target with the contents of the <code>file</code> argument.
         * <p>
         * If the fileURI is for a directory, then if isRecursive is also 
         * specified the payload will contain one Part to replace the 
         * directory (which will have the result of removing the directory
         * and its contents and then recreating the directory) plus a Part for
         * each file, including subdirectories, below the directory.  The
         * intent is to replace the entire directory with new contents.  
         * @param fileURI
         * @param dataRequestName
         * @param props
         * @param isRecursive
         * @throws IOException
         */
        public void requestFileReplacement(
                final String contentType,
                final URI fileURI,
                final String dataRequestName,
                final Properties props,
                final File file,
                final boolean isRecursive) throws IOException;

        /**
         * Writes the parts already added to the payload to the specified
         * OutputStream.
         * @param os OutputStream to receive the formatted payload
         * @throws java.io.IOException
         */
        public void writeTo(final OutputStream os) throws IOException;

        /**
         * Returns the content type of the payload, determined by whether there
         * are multiple parts and, if not, if the content type of the single
         * part is of type "text."
         * @return the content type of the pauload
         */
        public String getContentType();

        /**
         * Returns the name of the header that should be set in the outgoing and
         * incoming http request or response.
         * @return the header name
         */
        public String getHeaderName();
        
        /**
         * Returns the parts from the outbound payload.
         * @return Iterator over the outbound Parts
         */
        public Iterator<Part> parts();

        /**
         * Resets Payload dirty flag, indicating whether Payload was modified.
         */
        public void resetDirty();

        /**
         * Indicates whether Payload was modified since dirty flag was reset.
         * @return <code>true</code> if Payload was modified.
         */
        public boolean isDirty();

    }

    /**
     * Public API for inbound payloads.
     */
    public static interface Inbound {

        /**
         * Returns the parts from the inbound payload.
         * @return Iterator over the inbound Parts
         */
        public Iterator<Part> parts();

        /**
         * Returns the name of the header that should be set in the outgoing and
         * incoming http request or response.
         * @return the header name
         */
        public String getHeaderName();


    }

    /**
     * Public API for the payload Part.
     */
    public static interface Part {

        /**
         * Returns the name assigned to the part when it was created.
         * @return name
         */
        public String getName();

        /**
         * Returns the content type of the part.
         * @return content type
         */
        public String getContentType();

        /**
         * Returns the Properties associated with the Part.
         * @return Properties for the Part
         */
        public Properties getProperties();

        /**
         * Returns an InputStream suitable for reading the content of the Part.
         * @return
         */
        public InputStream getInputStream();

        /**
         * Copies the contents of the Part to the specified OutputStream.
         * @param os target OutputStream to receive the content of the Part
         * @throws java.io.IOException
         */
        public void copy(final OutputStream os) throws IOException;

        /**
         * Indicates if the Part represents a recursive action or not.
         * @return
         */
        public boolean isRecursive();
        
        /** Extractor of content can note where the content was extracted. 
         * It can help next user of the same Part to read content.
         * @param extractedFile 
         */
        public void setExtracted(File extractedFile);
        
        /** File where content was extracted from the payload.
         */
        public File getExtracted();
    }
}
