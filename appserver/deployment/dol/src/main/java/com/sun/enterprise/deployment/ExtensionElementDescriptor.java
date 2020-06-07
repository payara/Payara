/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package com.sun.enterprise.deployment;

import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.DynamicAttributesDescriptor;

import java.util.*;

/**
 * This class contains the deployment extensions element for a particular
 * xml node. It can contains sub elements (other ExtensionElementDescriptor
 * instances) or final leafs like attribute or string elements.
 *
 * @author Jerome Dochez
 */
public class ExtensionElementDescriptor extends Descriptor implements Observer {

    private List elementNames;
    private Map elementValues;
    private DynamicAttributesDescriptor attributes;

    /**
     * @return the value holder for all sub elements of
     * this deployment extension element
     */
    public Iterator getElementNames() {
        if (elementNames!=null) {
            return elementNames.iterator();
        }
        return null;
    }

    public void addElement(String elementName, Object value) {
        if (elementNames==null) {
            elementNames = new LinkedList();
            elementValues = new HashMap();
        }
        elementNames.add(elementName);
        elementValues.put(elementName, value);
    }

    public Object getElement(String elementName) {
        if (elementValues!=null) {
            return elementValues.get(elementName);
        }
        return null;
    }

    /**
     * @return a value holder for all attributes of
     * this deployment extension elements
     */
    public DynamicAttributesDescriptor getAttributes() {
        if (attributes==null) {
            attributes = new DynamicAttributesDescriptor();
            attributes.addObserver(this);
        }
        return attributes;
    }

    /**
     * @return true if the deployment extension contains attributes
     */
    public boolean hasAttributes() {
        return attributes!=null;
    }

    /**
     * notification of changed from our attributes/elements
     * storage
     */
    public void update(Observable o, Object arg) {
        setChanged();
        notifyObservers();
    }

    /**
     * @return a meaningful string describing myself
     */
    public void print(StringBuilder toStringBuilder) {
        toStringBuilder.append("ExtensionElementDescriptor");
        toStringBuilder.append("\n");
        super.print(toStringBuilder);
        for (Iterator itr = getElementNames();itr.hasNext();) {
            toStringBuilder.append("\n  Element=").append(getElement((String) itr.next()));
        }
        if (hasAttributes()) {
            toStringBuilder.append("\n  Attributes = ").append(getAttributes());
        }
    }
}
