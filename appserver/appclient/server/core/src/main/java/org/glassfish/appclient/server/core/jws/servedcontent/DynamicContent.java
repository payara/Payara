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
 */

package org.glassfish.appclient.server.core.jws.servedcontent;

import java.util.Date;
import java.util.Properties;

/**
 * Prescribes the contract for dynamic content.
 * <p>
 * Each DynamicContent reports its MIME type (for use in setting the MIME type
 * in the HTTP response back to the client).
 * <p>
 * Further, each DynamicContent object must return an "instance" which
 * represents a version of the dynamic content at a given point in time.
 * It is open to the implementer whether the DynamicContent returns a
 * newly-created Instance each time or whether it caches some number of
 * instances as an optimization.
 *
 * @author tjquinn
 */
public interface DynamicContent extends Content {

    /**
     * Retrieves an "instance" of this dynamic content, with placeholders
     * substituted using the provided properties.
     * @param tokenValues maps placeholder tokens to values
     * @return matching Instance; null if no matching instance exists
     */
    public Instance getExistingInstance(Properties tokenValues);

    /**
     * Retrieves an existing "instance" of this dynamic content, with placeholders
     * substituted, creating a new one if none already exists.
     * @param tokenValues maps placeholder tokens to values
     * @return matching or newly-created Instance
     */
    
    public Instance getOrCreateInstance(Properties tokenValues);
    /**
     * Retrieve the MIME type for this dynamic content.
     * @return
     */
    public String getMimeType();

    /**
     * Reports whether this dynamic content represents the main JNLP document
     * for an app client.
     * @return 
     */
    public boolean isMain();
    
    /**
     * Defines the contract for a given version of dynamic content at a single
     * moment in time.
     */
    public interface Instance {

        /**
         * Returns the text of the dynamic content instance.
         * @return
         */
        public String getText();

        /**
         * Returns the timestamp when the instance was created.
         * @return
         */
        public Date getTimestamp();
    }

    /**
     * Convenience implementation of Instance.
     */
    public static class InstanceAdapter implements Instance {
        /** when this instance was created */
        private final Date timestamp;

        /** the content of this instance */
        private final String text;

        /**
         *Creates a new instance of InstanceImpl (!) holding the result of a
         *specific substitution of values for placeholders.
         *@param text the content for this new InstanceImpl
         */
        public InstanceAdapter(final String text) {
            this.text = text;
            timestamp = new Date();
        }

        /**
         *Returns the time stamp associated with this content InstanceImpl.
         *@return the Date representing when this InstanceImpl was created
         */
        public Date getTimestamp() {
            return timestamp;
        }

        /**
         *Returns the content associated with this InstanceImpl.
         *@return the text content stored in the InstanceImpl
         */
        public String getText() {
            return text;
        }
    }
}
