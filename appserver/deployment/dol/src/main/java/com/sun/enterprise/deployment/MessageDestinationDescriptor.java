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

package com.sun.enterprise.deployment;

/** 
 * This class represents information about a web service
 * endpoint.
 *
 * @author Kenneth Saks
 */

import com.sun.enterprise.deployment.types.MessageDestinationReferencer;
import org.glassfish.deployment.common.Descriptor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MessageDestinationDescriptor extends Descriptor implements NamedDescriptor{

    private String msgDestName;

    // JNDI name of physical destination to which this logical
    // destination is mapped.
    private String jndiName;
    private String mappedName;
    private String lookupName;

    // Set of MessageDestinationReferencer descriptors pointing to me.
    private Set referencers = new HashSet();

    // bundle in which I am defined
    private BundleDescriptor bundleDescriptor;

    public MessageDestinationDescriptor() {
    }

    public MessageDestinationDescriptor(String name, String description) {
        super("", description);
        msgDestName = name;
    }

    public boolean hasName() {
        return (msgDestName != null);
    }

    public void setName(String name) {
        msgDestName = name;
    }

    public String getName() {
        return msgDestName;
    }

    public void setDisplayName(String displayName) {
        setLocalizedDisplayName(null, displayName);
    }

    public String getDisplayName() {
        return getLocalizedDisplayName(null);
    }

    public Set getAllReferencers() {
        return referencers;
    }

    public void addReferencer(MessageDestinationReferencer referencer) {
        referencers.add(referencer);
    }

    public void removeReferencer(MessageDestinationReferencer referencer) {
        referencers.remove(referencer);
    }

    public BundleDescriptor getBundleDescriptor() {
        return bundleDescriptor;
    }

    public void setBundleDescriptor(BundleDescriptor bundleDesc) {
        if( bundleDesc == null ) {
            for(Iterator iter = referencers.iterator(); iter.hasNext();) {
                MessageDestinationReferencer next =
                    (MessageDestinationReferencer) iter.next();
                next.setMessageDestination(null);
            }
            referencers.clear();
        }
        bundleDescriptor = bundleDesc;
    }

    public String getJndiName() {
        if (jndiName != null  && ! jndiName.equals("")) {
            return jndiName;
        }
        if (mappedName != null && ! mappedName.equals("")) {
            return mappedName;
        }
        return lookupName;
    }

    public void setJndiName(String physicalDestinationName) {
        jndiName = physicalDestinationName;
    }

    public String getMappedName() {
        return mappedName;
    }

    public void setMappedName(String mappedName) {
        this.mappedName = mappedName;
    }

    public void setLookupName(String lName) {
        lookupName = lName;
    }

    public String getLookupName() {
        return (lookupName != null)? lookupName : "";
    }

}
