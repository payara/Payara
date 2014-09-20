/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.deployment.descriptor;

import com.sun.enterprise.deployment.web.MimeMapping;

import java.io.Serializable;

    /*** I represent a mapping between a mime type and a file extension for specifiying how
    * to handle mime types in a J2EE WAR.
    *@author Danny Coward
    */

public class MimeMappingDescriptor implements MimeMapping, Serializable {
    private String extension;
    private String mimeType;
    
    /** copy constructor */
    public MimeMappingDescriptor(MimeMappingDescriptor other) {
	// super(other);
	extension = other.extension;
	mimeType = other.mimeType;
    }

    /** Construct the mapping for the given extension to the given mime type. */
    public MimeMappingDescriptor(String extension, String mimeType) {
	this.extension = extension;
	this.mimeType = mimeType;
    }
    
    /* Default constructor. */
    public MimeMappingDescriptor() {
    }

    /** Return the filename extension for this mapping. */
    public String getExtension() {
	if (this.extension == null) {
	    this.extension = "";
	}
	return this.extension;
    }
    
    /** Set the filename extension for this mapping. */
    public void setExtension(String extension) {
	this.extension = extension;
    }
    
    /** Get the mime type for this mapping. */
    public String getMimeType() {
	if (this.mimeType == null) {
	    this.mimeType = "";
	}
	return this.mimeType;
    }
    /** Set the mime type for this mapping. */
    public void setMimeType(String mimeType) {
	this.mimeType = mimeType;
    }
    /** My pretty format. */
    public void print(StringBuffer toStringBuffer) {
	toStringBuffer.append("MimeMapping: ").append(this.getExtension()).append("@").append(this.getMimeType());
    }

}
