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

package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.types.MessageDestinationReference;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;


/**
 * An object representing the use of a message destination in a 
 * J2EE component.
 *
 * @author Kenneth Saks
 *
*/

public class MessageDestinationReferenceDescriptor extends EnvironmentProperty 
    implements MessageDestinationReference {

    static private final int NULL_HASH_CODE = Integer.valueOf(1).hashCode();

    // Usage types
    public static final String CONSUMES = "Consumes";
    public static final String PRODUCES = "Produces";
    public static final String CONSUMES_PRODUCES = "ConsumesProduces";
    
    private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(MessageDestinationReferenceDescriptor.class);
    
    private BundleDescriptor referringBundle;

    private String usage;

    private String destinationType;

    // JNDI name of physical destination
    private String jndiName;

    // Holds information about the destination to which we are linked.
    private MessageDestinationReferencerImpl referencer;
    
    /** 
    * copy constructor
    */
    public MessageDestinationReferenceDescriptor(MessageDestinationReferenceDescriptor other) {
	super(other);
	referringBundle = other.referringBundle; // copy as-is
	usage = other.usage; // immutable String
	destinationType = other.destinationType; // immutable String
        referencer = new MessageDestinationReferencerImpl(other.referencer);
    }

    /** 
     * Construct an message destination reference 
     * with the given name and descriptor of the reference.
     *
     * @param name the message-destination-ref name as used in 
     * the referencing component
     * @param optional description
     */
    public MessageDestinationReferenceDescriptor(String name, String desc) {
	super(name, "", desc);
        referencer = new MessageDestinationReferencerImpl(this);
    }
    
    /** 
    * Constructs a reference in the extrernal state.
    */
    
    public MessageDestinationReferenceDescriptor() {
        referencer = new MessageDestinationReferencerImpl(this);
    }
    
    /**
     * @return the usage type of the message destination reference
     * (Consumes, Produces, ConsumesProduces)
     */
    public String getUsage() {
        return usage;
    }

    /**
     * @param usage the usage type of the message destination reference
     * (Consumes, Produces, ConsumesProduces)
     */
    public void setUsage(String destUsage) {
        usage = destUsage;
    }

    public String getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(String type) {
        destinationType = type;
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

    public String getInjectResourceType() {
        return getDestinationType();
    }

    public void setInjectResourceType(String resourceType) {
        setDestinationType(resourceType);
    }

    /**
     * Set the referring bundle, i.e. the bundle within which this
     * message destination reference is declared. 
     */
    public void setReferringBundleDescriptor(BundleDescriptor referringBundle)
    {
	this.referringBundle = referringBundle;
    }

    /**
     * Get the referring bundle, i.e. the bundle within which this
     * message destination reference is declared.  
     */
    public BundleDescriptor getReferringBundleDescriptor()
    {
	return referringBundle;
    }

    //
    // Implementations of MessageDestinationReferencer methods.
    //

    public boolean isLinkedToMessageDestination() {
        return referencer.isLinkedToMessageDestination();
    }
    
    /** 
     * @return the name of the message destination to which I refer 
     */
    public String getMessageDestinationLinkName() {
        return referencer.getMessageDestinationLinkName();
    }

    /** 
     * Sets the name of the message destination to which I refer.
     */
    public void setMessageDestinationLinkName(String linkName) {
        referencer.setMessageDestinationLinkName(linkName);
    }    

    public MessageDestinationDescriptor setMessageDestinationLinkName
        (String linkName, boolean resolveLink) {
        return referencer.setMessageDestinationLinkName(linkName, resolveLink);
    }

    public MessageDestinationDescriptor resolveLinkName() {
        return referencer.resolveLinkName();
    }
        
    public boolean ownedByMessageDestinationRef() {
        return true;
    }

    /**
     * Get the descriptor for the message destination reference owner.
     */ 
    public MessageDestinationReferenceDescriptor getMessageDestinationRefOwner
        () {
        return this;
    }

    /**
     * True if the owner is a message-driven bean.
     */ 
    public boolean ownedByMessageBean() {
        return false;
    }

    /**
     * Get the descriptor for the message-driven bean owner.
     */ 
    public EjbMessageBeanDescriptor getMessageBeanOwner() {
        return null;
    }

    /** 
     * @return the message destination to which I refer. Can be NULL.
    */
    public MessageDestinationDescriptor getMessageDestination() {
        return referencer.getMessageDestination();
    }  

    /**
     * @param messageDestiation the message destination to which I refer.
     */
    public void setMessageDestination(MessageDestinationDescriptor newMsgDest) {
        referencer.setMessageDestination(newMsgDest);
    }

    /** returns a formatted string representing me.
    */
    
    public void print(StringBuffer toStringBuffer) {
        if (isLinkedToMessageDestination()) {
	    toStringBuffer.append("Resolved Message-Destination-Ref ").append(getName()).append( 
                "points to logical message destination ").append(getMessageDestination().getName());
	} else {
	    toStringBuffer.append("Unresolved Message-Destination-Ref ").append(getName()).append(
                "@").append(getType()).append("@").append(usage);
	}	
    }

    public boolean isConflict(MessageDestinationReferenceDescriptor other) {
        return getName().equals(other.getName()) &&
            (!(
                DOLUtils.equals(getDestinationType(), other.getDestinationType()) &&
                DOLUtils.equals(getUsage(), other.getUsage()) &&
                DOLUtils.equals(getMessageDestinationLinkName(), other.getMessageDestinationLinkName())
                ) ||
            isConflictResourceGroup(other));
    }
    
    /* Equality on name. */
    public boolean equals(Object object) {
	if (object instanceof MessageDestinationReference) {
	    MessageDestinationReference reference = 
                (MessageDestinationReference) object;
	    return reference.getName().equals(this.getName());
	}
	return false;
    }

    public int hashCode() {
        int result = NULL_HASH_CODE;
        String name = getName();
        if (name != null) {
            result += name.hashCode();
        }
        return result;
    }
}
