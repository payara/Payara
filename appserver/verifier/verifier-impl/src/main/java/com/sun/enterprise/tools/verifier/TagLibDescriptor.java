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

package com.sun.enterprise.tools.verifier;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NodeList;
import com.sun.enterprise.tools.verifier.web.TagDescriptor;
import com.sun.enterprise.tools.verifier.web.FunctionDescriptor;

/**
 * class which defines methods required for implementing tests based
 * out of jsp tag library files.
 *
 * @author Sudipto Ghosh
 */
public class TagLibDescriptor {
    public static final String TAG = "tag"; // NOI18N
    public static final String LISTENER_CLASS = "listener-class"; // NOI18N
    public static final String FUNCTION = "function"; // NOI18N

    private Document doc = null;
    private String version = null;
    private String uri = null;

    public TagLibDescriptor(Document doc, String version, String uri) {
        this.doc = doc;
        this.version = version;
        this.uri = uri;
    }
    /**
     * @return spec version of tld file
     */
    public String getSpecVersion() {
        return this.version;
    }

    /**
     * @return location of the tld file
     */
    public String getUri() {
        return this.uri;
    }

    public String getPublicID() {
        DocumentType docType = doc.getDoctype();
        return ((docType == null) ? null : docType.getPublicId());
    }

    /**
     * @return system-id of the tld file.
     */
    public String getSystemID() {
        DocumentType docType = doc.getDoctype();
        return ((docType == null) ? null : docType.getSystemId());
    }

    public String[] getListenerClasses(){
        NodeList nl = doc.getElementsByTagName(LISTENER_CLASS);
        String[] classes = null;
        if (nl != null) {
            int size = nl.getLength();
            classes = new String[size];
            for (int i = 0; i < size; i++) {
                classes[i] = nl.item(i).getFirstChild().getNodeValue();
            }
        }
        return classes;
    }

    /**
     * for each tag in the tag lib descriptor create a TagDescriptor and return
     * the array of TagDescriptors present in the tag lib.
     * @return
     */
    public TagDescriptor[] getTagDescriptors() {
        NodeList nl = doc.getElementsByTagName(TAG);
        TagDescriptor[] tagdescriptor = null;
        if (nl != null) {
            int size = nl.getLength();
            tagdescriptor = new TagDescriptor[size];
            for (int i = 0; i < size; i++) {
                tagdescriptor[i] = new TagDescriptor(nl.item(i));
            }
        }
        return tagdescriptor;
    }

    /**
     * for each functions in tag lib descriptor creates a function descritor and
     * return the array of FunctionDescriptors 
     * @return array of function descriptor.
     */
    public FunctionDescriptor[] getFunctionDescriptors() {
        NodeList nl = doc.getElementsByTagName(FUNCTION);
        List<FunctionDescriptor> list = new ArrayList<FunctionDescriptor>();
        if (nl != null) {
            int size = nl.getLength();
            for (int i = 0; i < size; i++) {
                list.add(new FunctionDescriptor(nl.item(i)));
            }
        }
        return list.toArray(new FunctionDescriptor[0]);
    }
}
