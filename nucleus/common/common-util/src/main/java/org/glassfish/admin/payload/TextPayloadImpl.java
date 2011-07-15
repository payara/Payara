/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.api.admin.*;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Implements the Payload API for a message containing only a single text part.
 * <p>
 * This class is mainly useful so the RemoteCommand logic can treat the return
 * payload from a command the same, regardless of whether it is actually
 * a text-only payload (containing only the command report text itself) or
 * a multi-part payload with different Parts.
 * <p>
 * This class is here primarily to make the plain text in a response look like
 * the more general multi-part responses so the RemoteCommand class is free
 * from dealing with the details of payload formatting - in particular, free
 * from knowing how to tell if the payload contains just the text report or
 * contains other parts as well.
 * <p>
 * Note that if an outbound payload contains only one Part then, currently, the
 * Payload.Outbound.Impl.writeTo method copies the contents of that Part into
 * the request or response stream rather than writing a multi-part payload that
 * contains a single part.  This is for compatibility with existing clients
 * (such as NetBeans) which expect only the text report as the return payload.
 *
 * @author tjquinn
 */
public class TextPayloadImpl {

    /**
     * requests and responses using the text payload implementation should have
     * the Content-Type set to text/*.
     */
    private static final String PAYLOAD_IMPL_CONTENT_TYPE =
            "text/";

    public static class Inbound extends PayloadImpl.Inbound {

        private final InputStream is;
        private final String contentType;

        public static Inbound newInstance(final String messageContentType, final InputStream is) {
            return new Inbound(messageContentType, is);
        }

	/**
	 * Does this Inbound Payload implementation support the given content type?
	 * @return true if the content type is supported
	 */
	public static boolean supportsContentType(String contentType) {
	    return PAYLOAD_IMPL_CONTENT_TYPE.regionMatches(true, 0,
		    contentType, 0, PAYLOAD_IMPL_CONTENT_TYPE.length());
	}

        private Inbound(final String contentType, final InputStream is) {
            this.contentType = contentType;
            this.is = is;
        }

        public Iterator<Payload.Part> parts() {
            return new Iterator<Payload.Part>() {
                private boolean hasReturnedReport = false;

                public boolean hasNext() {
                    return ! hasReturnedReport;
                }

                public Payload.Part next() {
                    hasReturnedReport = true;
                    return new PayloadImpl.Part.Streamed(contentType, "report", null, is);
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

    }
}
