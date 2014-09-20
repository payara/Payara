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

package org.glassfish.web.ha.session.management;

import java.io.Serializable;

/**
 * AttributeMetadata contains the metadata of an attribute that is part of an
 * Http Session. When a container decides to save a session it passes an
 * instance of CompositeMetaData which contains a collection of
 * AttributeMetadata.
 * 
 * <p>
 * The attribute in question could have been deleted, or modified or could be a
 * new attribute inside the HttpSession. getOperation() tells exactly what
 * operation needs to be performed for this attribute
 * 
 * <p>
 * The attribute state/data itself can be obtained with getState(). Since an
 * attribute is part of a session, the attributes must be deleted when the
 * session is removed. The CompositeMetadata contains the last access time and
 * inactive timeout for the session.
 * 
 * @see CompositeMetadata
 */
public final class SessionAttributeMetadata implements Serializable {

    private String attributeName;

    private Operation opcode;

    private byte[] data;

    /**
     * Operation to be performed on this attribute
     */
    public enum Operation {
        ADD, DELETE, UPDATE
    };

    /**
     * Construct an AtributeMetadata
     * 
     * @param attributeName
     *            the attribute name
     * @param opcode
     *            The operation to be performed on the AttrbuteMetadata
     * @param data
     *            The attribute data
     */
    public SessionAttributeMetadata(String attributeName, Operation opcode, byte[] data) {
        this.attributeName = attributeName;
        this.opcode = opcode;
        this.data = data;
    }

    /**
     * Returns name of the attribute
     * 
     * @return attribute name
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Get the operation to be performed on the attribute.
     * 
     * @return the operation to be performed on this attribute
     */
    public Operation getOperation() {
        return opcode;
    }

    /**
     * Get the attribute data
     * 
     * @return the data
     */
    public byte[] getState() {
        return data;
    }
    
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && obj instanceof SessionAttributeMetadata) {
            SessionAttributeMetadata otherAttributeMetadata = (SessionAttributeMetadata) obj;
            return (getAttributeName() == null)
                ? otherAttributeMetadata.getAttributeName() == null
                : getAttributeName().equals(otherAttributeMetadata.getAttributeName());
        } else {
            return false;
        }
    }
    
    public int hashCode() {
        return 31 + (attributeName == null ? 0 : attributeName.hashCode());
    }
    
}
