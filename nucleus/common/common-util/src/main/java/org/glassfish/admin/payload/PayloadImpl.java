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

package org.glassfish.admin.payload;

import java.io.*;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import org.glassfish.api.admin.Payload;

/**
 * Abstract implementation of the Payload API.
 *
 * @author tjquinn
 */
public class PayloadImpl implements Payload {

    public abstract static class Outbound implements Payload.Outbound {
        /**
         * Partial implementation of the Outbound Payload.
         */
        private final ArrayList<Payload.Part> parts = new ArrayList<Payload.Part>();

        private boolean dirty = false;
        
        @Override
        public int size() {
            return getParts().size();
        }

        @Override
        public void addPart(
                final String contentType,
                final String name,
                final Properties props,
                final String content) throws IOException {
            parts.add(Part.newInstance(contentType, name, props, content));
            dirty = true;
        }

        @Override
        public void addPart(
                final String contentType,
                final String name,
                final Properties props,
                final InputStream content) throws IOException {
            parts.add(Part.newInstance(contentType, name, props, content));
            dirty = true;
        }

        @Override
        public void addPart(
                final int index,
                final String contentType,
                final String name,
                final Properties props,
                final InputStream content
                ) throws IOException {
            parts.add(index, Part.newInstance(contentType, name, props, content));
            dirty = true;
        }

        @Override
        public void attachFile(
                final String contentType,
                final URI fileURI,
                final String dataRequestName,
                final File file) throws IOException {
            attachFile(contentType, fileURI, dataRequestName, null /* props */, file, false /* isRecursive */);
            dirty = true;
        }

        @Override
        public void attachFile(
                final String contentType,
                final URI fileURI,
                final String dataRequestName,
                final File file,
                final boolean isRecursive) throws IOException {
            attachFile(contentType, fileURI, dataRequestName, null /* props */, file, isRecursive);
            dirty = true;
        }

        @Override
        public void attachFile(
                final String contentType,
                final URI fileURI,
                final String dataRequestName,
                final Properties props,
                final File file) throws IOException {
            attachFile(contentType, fileURI, dataRequestName, props, file, false /* isRecursive */);
            dirty = true;
        }

        @Override
        public void attachFile(
                final String contentType,
                final URI fileURI,
                final String dataRequestName,
                final Properties props,
                final File file,
                final boolean isRecursive) throws IOException {
            Properties enhancedProps = new Properties();
            if (props != null) {
                enhancedProps.putAll(props);
            }
            enhancedProps.setProperty("data-request-type", "file-xfer");
            enhancedProps.setProperty("data-request-name", dataRequestName);
            enhancedProps.setProperty("data-request-is-recursive", Boolean.toString(isRecursive));
            enhancedProps.setProperty("last-modified", Long.toString(file.lastModified())
                    );

            if (file.isDirectory() && isRecursive) {
                String relativeURIPath = fileURI.getRawPath();
                if (relativeURIPath.endsWith("/")) {
                    relativeURIPath = relativeURIPath.substring(0, relativeURIPath.length() - 1);
                }
                relativeURIPath = relativeURIPath.substring(0, relativeURIPath.lastIndexOf("/") + 1);

                attachFilesRecursively(
                        file.getParentFile().toURI(),
                        URI.create(relativeURIPath),
                        fileURI,
                        dataRequestName,
                        enhancedProps,
                        file);
            } else {
                parts.add(Part.newInstance(
                        contentType,
                        fileURI.getRawPath(),
                        enhancedProps,
                        (file.isDirectory()) ? null : file));
            }
            dirty = true;
        }

        @Override
        public void requestFileReplacement(
                final String contentType, 
                final URI fileURI, 
                final String dataRequestName, 
                final Properties props, 
                final File file,
                final boolean isRecursive) throws IOException {
            final Properties enhancedProps = new Properties();
            if (props != null) {
                enhancedProps.putAll(props);
            }
            enhancedProps.setProperty("data-request-type", "file-replace");
            enhancedProps.setProperty("data-request-name", dataRequestName);
            enhancedProps.setProperty("data-request-is-recursive", Boolean.toString(isRecursive));
	    enhancedProps.setProperty("last-modified", Long.toString(file.lastModified()));

            /*
             * Add a dummy part for the replacement of the directory - if
             * the part refers to a directory.
             */
            if (file.isDirectory()) {
                parts.add(Part.newInstance(
                        "application/octet-stream", /* not much effect */
                        fileURI.getRawPath(),
                        enhancedProps,
                        (String) null));
                /*
                 * If this is also a recursive replacement, add file-xfer
                 * Parts for the files in the directory.
                 */
                if (isRecursive) {
                    enhancedProps.setProperty("data-request-type", "file-xfer");
                    attachContainedFilesRecursively(
                            file.toURI(),
                            fileURI,
                            dataRequestName,
                            enhancedProps,
                            file);
                }
            } else {
                /*
                 * This is a non-directory file. Add a simple replacement
                 * request part for the single file.
                 */
                parts.add(Part.newInstance(
                            contentType, 
                            fileURI.getRawPath(),
                            enhancedProps,
                            file));
            }
            dirty = true;
        }

        private void attachContainedFilesRecursively(
                final URI actualBaseDirAbsURI,
                final URI targetBaseDirRelURI,
                final String dataRequestName,
                final Properties enhancedProps,
                final File dirFile) throws FileNotFoundException, IOException {

            for (File f : dirFile.listFiles()) {
                if (f.isDirectory()) {
                    enhancedProps.setProperty("last-modified", Long.toString(f.lastModified()));
                    attachFilesRecursively(
                            actualBaseDirAbsURI,
                            targetBaseDirRelURI,
                            targetBaseDirRelURI.resolve(actualBaseDirAbsURI.relativize(f.toURI())),
                            "",
                            enhancedProps,
                            f);
                } else {
                    String contentType = URLConnection.guessContentTypeFromName(f.getName());
                    if (contentType == null) {
                        final InputStream is = new BufferedInputStream(new FileInputStream(f));
                        try {
                            contentType = URLConnection.guessContentTypeFromStream(is);
                            if (contentType == null) {
                                contentType = "application/octet-stream";
                            }
                        } finally {
                            if (is != null) {
                                is.close();
                            }
                        }
                    }
                    enhancedProps.setProperty("last-modified", Long.toString(f.lastModified()));
                    final URI fileURI = targetBaseDirRelURI.resolve(actualBaseDirAbsURI.relativize(f.toURI()));
                    parts.add(Part.newInstance(
                            contentType,
                            fileURI.getRawPath(),
                            enhancedProps,
                            f));
                }
            }
        }

        private void attachFilesRecursively(
                final URI actualBaseDirAbsURI,
                final URI targetBaseDirRelURI,
                final URI dirFileURI,
                final String dataRequestName,
                final Properties enhancedProps,
                final File dirFile) throws FileNotFoundException, IOException {
            final String dirFileURIPath = dirFileURI.getRawPath();
            parts.add(Part.newInstance(
                "application/octet-stream", /* for the directory itself */
                dirFileURIPath + (dirFileURIPath.endsWith("/") ? "" : "/"),
                enhancedProps,
                (InputStream) null));
            /*
             * The enhanced properties contains a setting for the data-request-name
             * which will be used to inject as a paramter if the receiver is
             * a command.  We don't want lower-level directories to appear to be
             * the injectable value when in fact the higher-level directory is
             * the correct value.
             */
            enhancedProps.remove("data-request-name");
            attachContainedFilesRecursively(
                    actualBaseDirAbsURI,
                    targetBaseDirRelURI,
                    dataRequestName,
                    enhancedProps,
                    dirFile);

        }

        @Override
        public void requestFileRemoval(
                final URI fileURI,
                final String dataRequestName,
                final Properties props) throws IOException {
            requestFileRemoval(fileURI, dataRequestName, props, false /* isRecursive */);
            dirty = true;
        }

        @Override
        public void requestFileRemoval(
                final URI fileURI,
                final String dataRequestName,
                final Properties props,
                final boolean isRecursive) throws IOException {
            final Properties enhancedProps = new Properties();
            if (props != null) {
                enhancedProps.putAll(props);
            }
            enhancedProps.setProperty("data-request-type", "file-remove");
            enhancedProps.setProperty("data-request-name", dataRequestName);
            enhancedProps.setProperty("data-request-is-recursive", Boolean.toString(isRecursive));
            parts.add(Part.newInstance(
                    "application/octet-stream", /* not much effect */
                    fileURI.getRawPath(),
                    enhancedProps,
                    (String) null));
            dirty = true;
        }


        @Override
        public String getHeaderName() {
            return Payload.PAYLOAD_HEADER_NAME;
        }

        @Override
        public String getContentType() {
            return (isComplex()) ? getComplexContentType() : getSinglePartContentType();
        }

        public ArrayList<Payload.Part> getParts() {
            return parts;
        }

        /**
         * Writes the Parts in this Outbound Payload to the specified output
         * stream; concrete implementations will implement this abstract method.
         * @param os the OutputStream to which the Parts should be written
         * @throws java.io.IOException
         */
        protected abstract void writePartsTo(final OutputStream os) throws IOException;

        /**
         * Writes the Payload to the specified output stream.
         *
         * @param os the OutputStream to which the Payload should be written
         * @throws java.io.IOException
         */
        @Override
        public void writeTo(final OutputStream os) throws IOException {
            if (isComplex()) {
                writePartsTo(os);
            } else {
                parts.get(0).copy(os);
            }
        }

        /**
         * Returns the Content-Type which reflects that multiple Parts will be
         * in the Payload.
         * <p>
         * This content type might vary among different implementations of
         * Payload.
         *
         * @return the content type for complex payloads
         */
        public abstract String getComplexContentType();

        private boolean isComplex(final String partType) {
            return (parts.size() > 1) ||
                   ( ! partType.startsWith("text"));
        }

        private boolean isComplex() {
            return isComplex(parts.get(0).getContentType());
        }

        String getSinglePartContentType() {
            /*
             * If the one part is text/? then return it as the single-part
             * content type.  Otherwise the more complicated part is stored
             * in an implementation-dependent way so we need to return
             */
            String partType = parts.get(0).getContentType();
            if (isComplex(partType)) {
                return getComplexContentType();
            } else {
                return partType;
            }
        }

        public static Outbound newInstance() {
            return ZipPayloadImpl.Outbound.newInstance();
        }
        
        @Override
        public Iterator<Payload.Part> parts() {
            ArrayList<Payload.Part> prts = getParts();
            if (prts == null) {
                return Collections.<Payload.Part>emptyList().iterator();
            } else {
                return prts.iterator();
            }
        }

        @Override
        public boolean isDirty() {
            return dirty;
        }

        @Override
        public void resetDirty() {
            dirty = false;
        }
    }

    /**
     * Partial implementation of the Inbound interface.
     */
    public static abstract class Inbound implements Payload.Inbound {

        /**
         * Creates a new Inbound Payload of the given content type, read from
         * the specified InputStream.  The payloadContentType should be the
         * content-type from the inbound http request or response.
         * @param payloadContentType content-type from the inbound http request or response
         * @param is the InputStream from which the Payload should be read
         * @return the prepared Payload
         * @throws java.io.IOException
         */
        public static Inbound newInstance(final String payloadContentType, final InputStream is) throws IOException {
            if (payloadContentType == null) {
                return EMPTY_PAYLOAD;
            }
            if (TextPayloadImpl.Inbound.supportsContentType(payloadContentType)) {
                return TextPayloadImpl.Inbound.newInstance(payloadContentType, is);
	    } else if (ZipPayloadImpl.Inbound.supportsContentType(payloadContentType)) {
                return ZipPayloadImpl.Inbound.newInstance(payloadContentType, is);
            } else {
		return null;
            }
        }

        @Override
        public String getHeaderName() {
            return Payload.PAYLOAD_HEADER_NAME;
        }

        /**
         * An empty inbound payload.
         */
        private static final Inbound EMPTY_PAYLOAD = new Inbound() {

            @Override
            public Iterator<Payload.Part> parts() {
                return Collections.<Payload.Part>emptyList().iterator();
            }

        };
    }

    /**
     * Partial implementation of Part.
     */
    public static abstract class Part implements Payload.Part {
        private String name;
        private String contentType;
        private Properties props;
        private boolean isRecursive;
        private File extractedFile;

        /**
         * Creates a new Part implementation.
         * @param contentType content type of the Part
         * @param name name for the Part
         * @param props Properties associated with the Part
         */
        Part(final String contentType, final String name, final Properties props) {
            this.contentType = contentType;
            this.name = name;
            /*
             * Copy the caller-supplied properties in case the caller
             * adjusts the properties later.
             */
            this.props = new Properties();
            if (props != null) {
                this.props.putAll(props);
            }
            isRecursive = Boolean.valueOf(this.props.getProperty("data-request-is-recursive"));
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public Properties getProperties() {
            return props;
        }

        @Override
        public boolean isRecursive() {
            return isRecursive;
        }
        
        /** Some use cases need reentrantable implementation of this stream 
         * implementation. Information about extraction can be used for it.
         */
        @Override
        public void setExtracted(File extractedFile) {
            if (extractedFile != null && extractedFile.exists() && extractedFile.isFile()) {
                this.extractedFile = extractedFile;
            }
        }

        @Override
        public File getExtracted() {
            return this.extractedFile;
        }
        
        protected InputStream getExtractedInputStream() {
            File file = getExtracted();
            if (file != null) {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException ex) {
                    return null;
                }
            } else {
                return null;
            }
        }

        /**
         * Creates a new Part from an InputStream.
         * @param contentType content type for the Part
         * @param name name of the Part
         * @param props Properties to be associated with the Part
         * @param is InputStream to be used to populate the Part's data
         * @return the new Part
         */
        public static Part newInstance(
                final String contentType,
                final String name,
                final Properties props,
                final InputStream is) {
            return new Streamed(contentType, name, props, is);
        }

        /**
         * Creates a new Part from a String.
         * @param contentType content type for the Part
         * @param name name of the Part
         * @param props Properties to be associated with the Part
         * @param content String containing the content for the Part
         * @return
         */
        public static Part newInstance(
                final String contentType,
                final String name,
                final Properties props,
                final String content) {
            return new Buffered(contentType, name, props, content);
        }

        /**
         * Creates a new Part from a File.
         * @param contentType content type for the Part
         * @param name name of the Part
         * @param props Properties to be associated with the Part
         * @param file File containing the content for the Part
         * @return
         */
        public static Part newInstance(
                final String contentType,
                final String name,
                final Properties props,
                final File file) throws FileNotFoundException {
            return new Filed(contentType, name, props, file);
        }

        @Override
        public void copy(final OutputStream os) throws IOException {
            int bytesRead;
            byte [] buffer = new byte[1024];
            final InputStream is = getInputStream();
            /*
             * Directory entries can have null input streams.
             */
            if (is != null) {
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }
        
        /**
         * Implements Part using a stream.
         */
        static class Streamed extends PayloadImpl.Part {
            private final InputStream is;

            /**
             * Creates a new stream-baesd Part.
             * @param contentType content type for the Part
             * @param name name of the Part
             * @param props Properties to be associated with the Part
             * @param is InputStream containing the data for the Part
             */
            Streamed(
                    final String contentType,
                    final String name,
                    final Properties props,
                    final InputStream is) {
                super(contentType, name, props);
                this.is = is;
            }

            @Override
            public InputStream getInputStream() {
                InputStream extrIS = getExtractedInputStream();
                if (extrIS == null) {
                    return is;
                } else {
                    return extrIS;
                }
            }
            
        }

        /**
         * Implements Part using an internal buffer.
         */
        static class Buffered extends PayloadImpl.Part {
            private final String content;
            private InputStream is = null;

            /**
             * Creates a new buffer-based Part.
             * @param contentType content type for the Part
             * @param name name of the Part
             * @param props Properties to be associated with the Part
             * @param content String containing the data to be placed in the Part
             */
            Buffered(
                    final String contentType,
                    final String name,
                    final Properties props,
                    final String content) {
                super(contentType, name, props);
                this.content = content;

            }

            @Override
            public InputStream getInputStream() {
                if (is == null) {
                    /*
                     * Some parts might not have content.
                     */
                    final byte[] data = (content != null) ?
                        content.getBytes() : new byte[0];
                    is = new ByteArrayInputStream(data);
                }
                return is;
            }
        }

        /**
         * Implements Part using a File.
         * <p>
         * Note that directories can be added as Parts to the payload, but
         * a null file is passed in that case.  For those, return a dummy
         * stream with no content in response to getInputStream.
         * <p>
         * Further, getInputStream returns a self-closing input stream.  Calling
         * code which passes a File to attachFile or addPart will not have access
         * to the input stream we open here, so we need to close it ourselves.
         * We do that automatically when we detect the end-of-stream while
         * preserving the external behavior of the stream.
         */
        static class Filed extends PayloadImpl.Part {
            private final File file;

            Filed(final String contentType,
                    final String name,
                    final Properties props,
                    final File file) throws FileNotFoundException {
                super(contentType, name, props);
                this.file = file;
                validateFile(file);
            }

            @Override
            public InputStream getInputStream() {
                try {
                    return (file != null
                            ? new SelfClosingInputStream(
                                new BufferedInputStream(
                                    new FileInputStream(file)))
                            : dummyStream());
                } catch (FileNotFoundException ex) {
                    /*
                     * Silently return null; validateFile has already logged a message
                     * when the original caller tried to add this file to
                     * the payload.
                     */
                    return null;
                }
            }

            private void validateFile(final File f) throws FileNotFoundException {
                if ( f != null && ! f.canRead()) {
                    /*
                     * Throw an exception so, for example, an asadmin user will
                     * become aware of the problem.
                     */
                    throw new FileNotFoundException(f.getAbsolutePath());
                }
            }

            private InputStream dummyStream() {
                return new ByteArrayInputStream(new byte[0]);
            }

            /**
             * An InputStream that automatically closes itself when the
             * wrapped stream reached end-of-stream.  The close() method
             * is still supported but, if the wrapped stream has already been closed,
             * acts as a no-op.
             * <p>
             * The read and close method implementations are interesting.  The
             * other methods simply delegate to the wrapped stream.
             */
            private static class SelfClosingInputStream extends InputStream {

                private final InputStream wrappedStream;
                private boolean isWrappedStreamClosed = false;
                private boolean isExternallyClosed = false;
                
                private SelfClosingInputStream(final InputStream wrappedStream) {
                    this.wrappedStream = wrappedStream;
                }
                
                @Override
                public int read() throws IOException {
                    if (isExternallyClosed) {
                        /*
                         * The API does not permit reading after the stream
                         * has been closed.  Mimic that behavior.
                         */
                        throw new IOException();
                    }
                    if (isWrappedStreamClosed) {
                        /*
                         * We have closed the wrapped stream, because it
                         * returned a -1 from read, but the stream has not
                         * been externally closed yet.  So continue returning
                         * the end-of-stream indiciator.
                         */
                        return -1;
                    }

                    final int result = wrappedStream.read();
                    if (result == -1) {
                        /*
                         * We've exhausted the wrapped stream, so close it
                         * internally.
                         */
                        closeInternally();
                    }
                    return result;
                }

                @Override
                public long skip(long n) throws IOException {
                    return wrappedStream.skip(n);
                }


                @Override
                public int available() throws IOException {
                    return wrappedStream.available();
                }

                @Override
                public synchronized void mark(int readlimit) {
                    wrappedStream.mark(readlimit);
                }

                @Override
                public boolean markSupported() {
                    return wrappedStream.markSupported();
                }

                @Override
                public synchronized void reset() throws IOException {
                    wrappedStream.reset();
                }

                @Override
                public void close() throws IOException {
                    closeInternally();
                    isExternallyClosed = true;
                }

                private void closeInternally() throws IOException {
                    wrappedStream.close();
                    isWrappedStreamClosed = true;
                }
            }
        }
    }

}
