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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.glassfish.api.admin.Payload;

/**
 * Implementation of Payload based on representing each payload Part as a
 * ZipEntry in a zip stream in the request or response stream.
 * <p>
 * Note that when retrieving Parts from a Payload that is implemented this way the calling
 * program must consume each Part's data - by invoking the Part's getInputStream()
 * method and exhausting the stream - before advancing to the next Part
 * (using the Iterator returned by the parts() method).  This is because all
 * Parts share the same single ZipInputStream from the payload.  To save space
 * and improve performance this implementation does not store each Part's
 * contents internally. That's why the calling program must consume each Part's content
 * before moving on to the next.
 * <p>
 * Each ZipEntry supports "extra" data.  This implementation stores the Part properties
 * in this extra data.  Further, each Part can have a different content-type and
 * the Properties object stored in the extra data also records the content-type
 * for the Part.
 *
 * @author tjquinn
 */
class ZipPayloadImpl extends PayloadImpl {

    /**
     * requests and responses using the zip payload implementation should have
     * the Content-Type set to application/zip.
     */
    private static final String PAYLOAD_IMPL_CONTENT_TYPE =
            "application/zip";

    /**
     * Zip implementation of the Outbound Payload.
     */
    static class Outbound extends PayloadImpl.Outbound {

        private void prepareEntry(final Payload.Part part, final ZipOutputStream zos) throws IOException {
            ZipEntry entry = new ZipEntry(part.getName());
            Extra extra = new Extra(part.getContentType(), part.getProperties());
            entry.setExtra(extra.toBytes());
            zos.putNextEntry(entry);
        }

        @Override
        public void writePartsTo(OutputStream os) throws IOException {
            ZipOutputStream zos = new ZipOutputStream(os);
            for (Payload.Part part : getParts()) {
                prepareEntry(part, zos);
                part.copy(zos);
                zos.closeEntry();
            }
            zos.close();
        }

        private Outbound() {
        }

        public static Outbound newInstance() {
            return new Outbound();
        }

        @Override
        public String getComplexContentType() {
            return PAYLOAD_IMPL_CONTENT_TYPE;
        }
    }

    /**
     * Zip implementation of the Inbound Payload.
     * <p>
     * A single ZipInputStream provides the data for each ZipEntry
     * in the Payload.  So the returned InputStream for each Part is
     * actually a wrapper stream around the single shared ZipInputStream.
     * A given Part's input stream becomes invalid once the caller reads to
     * the end of that ZipEntry OR if the caller moves on to the next Part
     * (using the iterator) before having reached the end of the stream.
     * <p>
     * For this to work, the inbound payload prefetches the next entry from
     * the ZipInputStream and stores it internally.  The implementation of
     * the Iterator uses this prefetched next entry to decide if there is a
     * next result from the iterator, for example.
     * <p>
     * The next entry is prefetched when:
     * <ul>
     * <li>the inbound implementation is first
     * created,
     * <li>the caller invokes the iterator's hasNext() method when
     * the next entry has not already been prefetched (such as if the caller
     * did not read the Part's input stream at all or invoked the iterator's
     * hasNext() method before reading to the end of the part's input stream), and
     * <li>the caller exhausts the input stream from a Part.
     * </ul>
     */
    static class Inbound extends PayloadImpl.Inbound {

        private final ZipInputStream zis;

        private ZipEntry nextEntry = null;
        private boolean isNextEntryPrefetched = false;

        private Inbound(final InputStream is) throws IOException {
            zis = new ZipInputStream(new BufferedInputStream(is));
            prefetchNextEntry();
        }

        private void invalidateCurrentWrapperStream() {
        }
        
        private void prefetchNextEntry() throws IOException {
            invalidateCurrentWrapperStream();
            nextEntry = zis.getNextEntry();
            isNextEntryPrefetched = true;

        }
        private void recordZipEntryEOF() throws IOException {
            invalidateCurrentWrapperStream();
            prefetchNextEntry();
        }

        private void recordZipEntryNonEOF() {
            isNextEntryPrefetched = false;
        }

        /**
         * Wrapper stream around the ZipInputStream.  This stream becomes
         * invalid once the caller reads to the end of this entry's
         * data in the ZipInputStream or once the caller moves on to the
         * next entry using the iterator returned by the payload's parts()
         * method.
         */
        private static class ZipEntryInputStream extends InputStream {

            private final ZipInputStream zis;
            private final Inbound inboundPayload;
            private boolean isValid = true;

            private ZipEntryInputStream(final Inbound inboundPayload) {
                this.zis = inboundPayload.zis;
                this.inboundPayload = inboundPayload;
            }

            private void invalidate() {
                isValid = false;
            }

            private void checkValid() {
                if ( ! isValid) {
                    throw new IllegalStateException();
                }
            }

            @Override
            public int read() throws IOException {
                checkValid();
                int result = zis.read();
                if (result == -1) {
                    inboundPayload.recordZipEntryEOF();
                } else {
                    inboundPayload.recordZipEntryNonEOF();
                }
                return result;
            }

            @Override
            public int read(byte[] b) throws IOException {
                checkValid();
                int result = zis.read(b);
                if (result == -1 ) {
                    inboundPayload.recordZipEntryEOF();
                } else {
                    inboundPayload.recordZipEntryNonEOF();
                }
                return result;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                checkValid();
                int result = zis.read(b, off, len);
                if (result == -1) {
                    inboundPayload.recordZipEntryEOF();
                } else {
                    inboundPayload.recordZipEntryNonEOF();
                }
                return result;
            }

            @Override
            public void close() throws IOException {
                invalidate();
            }
        }

        /**
         * Returns a new Zip implementation of the Inbound Payload.
         * @param payloadContentType content type for the payload
         * @param is InputStream from which to read the payload
         * @return Payload.Inbound containing the data from the specified input stream;
         * null if the payloadContentType is not what this zip-based
         * implementation can handle
         * @throws java.io.IOException
         */
        public static Inbound newInstance(final String payloadContentType, final InputStream is) throws IOException {
            return new Inbound(is);
        }

	/**
	 * Does this Inbound Payload implementation support the given content type?
	 * @return true if the content type is supported
	 */
	public static boolean supportsContentType(final String contentType) {
	    return PAYLOAD_IMPL_CONTENT_TYPE.equalsIgnoreCase(contentType);
	}
        
        @Override
        public Iterator<Payload.Part> parts() {
            return new Iterator<Payload.Part>() {

                @Override
                public boolean hasNext() {
                    if ( ! isNextEntryPrefetched) {
                        try {
                            prefetchNextEntry();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    return (nextEntry != null);
                }

                @Override
                public Payload.Part next() {
                    final Extra extra = new Extra(nextEntry.getExtra());
                    final Payload.Part part = new ZipPayloadImpl.Part(
                            nextEntry.getName(), 
                            extra.getContentType(),
                            extra.getProperties(),
                            Inbound.this);
                    isNextEntryPrefetched = false;
                    return part;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    /**
     * Zip-based implementation of the Part interface.
     */
    static class Part extends PayloadImpl.Part {

        private final Inbound inboundPayload;

        private Part(
                final String name,
                final String contentType,
                final Properties props,
                final Inbound inboundPayload) {
            super(contentType, name, props);
            this.inboundPayload = inboundPayload;
        }

        @Override
        public InputStream getInputStream() {
            return new Inbound.ZipEntryInputStream(inboundPayload);
        }
    }

    /**
     * Abstraction of our use of the ZipEntry's "extra" data.
     * <p>
     * We use the ZipEntry extra data to hold Properties file-formatted
     * data holding the data request information plus the content type for
     * the Part.
     * <p>
     * The "normal" properties and the content type are exposed separately to
     * the rest of the ipmlementation but we use a single Properties dump to
     * represent both.  So before exposing the Properties object we remove
     * the content-type entry.
     */
    private static class Extra {
        private static final String CONTENT_TYPE_NAME = "Content-Type";

        private String contentType;
        private Properties props;

        private Extra(final byte[] extra) {
            try {
                props = new Properties();
                ByteArrayInputStream bais = new ByteArrayInputStream(extra);
                props.load(bais);
                contentType = props.getProperty(CONTENT_TYPE_NAME);
                props.remove(CONTENT_TYPE_NAME);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        private Extra(final String contentType, final Properties props) {
            this.contentType = contentType;
            this.props = props;
        }

        private byte[] toBytes() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Properties fullProps = new Properties();
            if (props != null) {
                fullProps.putAll(props);
            }
            fullProps.setProperty(CONTENT_TYPE_NAME, contentType);
            try {
                fullProps.store(baos, null);
                return baos.toByteArray();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        private String getContentType() {
            return contentType;
        }

        private Properties getProperties() {
            return props;
        }
    }

}
