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
// Portions Copyright [2016-2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.util.DOLUtils;
import org.glassfish.config.support.TranslatedConfigView;

/**
 * An object representing a link to another ejb.
 *
 * @author Jerome Dochez
 *
*/

public class EjbReferenceDescriptor extends EnvironmentProperty implements EjbReference, NamedDescriptor {

    static private final int NULL_HASH_CODE = Integer.valueOf(1).hashCode();

    // In case the reference has been resolved, the ejbDescriptor will
    // be the referenced ejb.
    private EjbDescriptor ejbDescriptor;

    // We need the referring bundle for what ?
    private BundleDescriptor referringBundle;

    // bean type and interfaces names
    private String refType=null;
    private String refHomeIntf=null;
    private String refIntf=null;

    // local-ref or remote-ref
    private boolean isLocal=false;

    /**
     * holds the ejb-link value associated to this ejb reference before the
     * ejbs were resolved
     */
    private String ejbLink=null;

    /**
     * Portable lookup name that resolves to this reference's target EJB.
     * NOTE : This value is handled *independently* of the prior V2
     * mappedName/jndi-name logic. It is not eligible as a possible return
     * value from getJndiName() or getValue().  Ejb dependency processing
     * is complicated due to the large number of ways a reference can be
     * resolved, so this is the safest approach to avoiding backward
     * compatibility issues.
     */
    private String lookupName=null;

    /**
     * copy constructor
     *
     * @param other handle to other EjbReferenceDescriptor to clone
     */
    public EjbReferenceDescriptor(EjbReferenceDescriptor other) {
	super(other);
	isLocal = other.isLocal; // boolean
	refType = other.refType; // String
	refHomeIntf = other.refHomeIntf; // String
	refIntf = other.refIntf; // String
	ejbLink = other.ejbLink; // String
    lookupName = other.lookupName;
	referringBundle = other.referringBundle; // copy as-is
	ejbDescriptor = other.ejbDescriptor;
	if (ejbDescriptor != null) {
            ejbDescriptor.addEjbReferencer(this); // ???
	}
    }

    /**
     * Construct an remote ejb reference to the given ejb descriptor
     * with the given name and descriptor of the reference.
     *
     * @param name the ejb-ref name as used in the referencing bean
     * @param description optional description
     * @param ejbDescriptor of the referenced bean
     */
    public EjbReferenceDescriptor(String name, String description, EjbDescriptor ejbDescriptor) {
	super(name, "", description);
	this.setEjbDescriptor(ejbDescriptor);
    }

    /**
     * constructs an local or remote ejb reference to the given ejb descriptor,
     * the description and the name of the reference
     *
     * @param name is the name of the reference
     * @param description is a human readable description of the reference
     * @param ejbDescriptor the referenced EJB
     * @param isLocal true if the reference uses the local interfaces
     */
    public EjbReferenceDescriptor(String name, String description, EjbDescriptor ejbDescriptor, boolean isLocal) {
        super(name, "", description);
        this.isLocal = isLocal;
        this.setEjbDescriptor(ejbDescriptor);
    }

    /**
    * Constructs a reference in the exterrnal state.
    */

    public EjbReferenceDescriptor() {
    }

    /**
     * Set the referring bundle, i.e. the bundle within which this
     * EJB reference is declared.
     */
    @Override
    public void setReferringBundleDescriptor(BundleDescriptor referringBundle)
    {
	this.referringBundle = referringBundle;
    }

    /**
     * Get the referring bundle, i.e. the bundle within which this
     * EJB reference is declared.
     */
    @Override
    public BundleDescriptor getReferringBundleDescriptor()
    {
	return referringBundle;
    }

    /**
     * Sets the ejb descriptor to which I refer.
     * @param ejbDescriptor the ejb descriptor referenced, null if it is unknown at this time
     */
    @Override
    public void setEjbDescriptor(EjbDescriptor ejbDescriptor) {
	if (this.ejbDescriptor != null) {
            this.ejbDescriptor.removeEjbReferencer(this); // remove previous referencer
	}
        this.ejbDescriptor=ejbDescriptor;
	if (ejbDescriptor!=null) {
            ejbDescriptor.addEjbReferencer(this);
            if (isLocal()) {
                if (!ejbDescriptor.isLocalInterfacesSupported() &&
                    !ejbDescriptor.isLocalBusinessInterfacesSupported() &&
                    !ejbDescriptor.isLocalBean()) {
                     throw new RuntimeException(localStrings.getLocalString(
                     "entreprise.deployment.invalidLocalInterfaceReference",
                     "Trying to set an ejb-local-ref on an EJB while the EJB [{0}] does not define local interfaces", new Object[] {ejbDescriptor.getName()}));
                }
            } else {
                if (!ejbDescriptor.isRemoteInterfacesSupported() &&
                    !ejbDescriptor.isRemoteBusinessInterfacesSupported()) {
                    throw new RuntimeException(localStrings.getLocalString(
                    "entreprise.deployment.invalidRemoteInterfaceReference",
                    "Trying to set an ejb-ref on an EJB, while the EJB [{0}] does not define remote interfaces", new Object[] {ejbDescriptor.getName()}));
                }
            }
	}
    }


    /**
    * Sets the jndi name of the bean type which I am referring.*/

    @Override
    public void setJndiName(String jndiName) {
	this.setValue(jndiName);
    }

    @Override
    public boolean hasJndiName() {
        String name = getJndiName();
        return ( (name != null) && !name.equals("") );
    }

    /** return true if I know the name of the ejb to which I refer.
    */

    public boolean isLinked() {
	return ejbLink!=null;
    }

    /**
     * @return the name of the ejb to which I refer
    */

    @Override
    public String getLinkName() {
	if (ejbDescriptor==null) {
            return ejbLink;
        } else {
            if (ejbLink != null && ejbLink.length()!=0) {
                return ejbLink;
            }
	    return ejbDescriptor.getName();
	}
    }

    /**
     * Sets the name of the ejb to which I refer.
     */
    @Override
    public void setLinkName(String linkName) {
        ejbLink = TranslatedConfigView.expandValue(linkName);
    }
    /**
     * return the jndi name of the bean to which I refer.
     */

    @Override
    public String getJndiName() {
        String jndiName = this.getValue();
        if( isLocal() ) {
            // mapped-name has no meaning for the local ejb view.  ejb-link
            // should be used to resolve any ambiguities about the target
            // local ejb.
            return jndiName;
        } else {
            return (jndiName != null && ! jndiName.equals("")) ?
                jndiName : getMappedName();
        }
    }

    /**
    * Return the jndi name of the bean to which I refer.
    */

    @Override
    public String getValue() {
	if (ejbDescriptor == null) {
            return super.getValue();
	} else {
            if (isLocal()) {
                return super.getValue();
            } else {
                return ejbDescriptor.getJndiName();
            }
        }
    }

    @Override
    public void setLookupName(String l) {
        lookupName = TranslatedConfigView.expandValue(l);
    }

    @Override
    public String getLookupName() {
        return (lookupName != null)? lookupName : "";
    }

    @Override
    public boolean hasLookupName() {
        return (lookupName != null && lookupName.length() > 0);
    }

    /** return the ejb to whoch I refer.
    */

    @Override
    public EjbDescriptor getEjbDescriptor() {
	return ejbDescriptor;
    }

    /**
     * @return true if the EJB reference uses the local interfaces of the EJB
     */
    @Override
    public boolean isLocal() {
        return isLocal;
    }

    /**
     * Set whether this EJB Reference uses local interfaces or remote
     * @param local true if the EJB reference use local interfaces
     */
    @Override
    public void setLocal(boolean local) {
        this.isLocal = local;
    }

    /**
    * Retusn the type of the ejb to whioch I refer.
    */

    @Override
    public String getType() {
        if (ejbDescriptor==null) {
            return refType;
        } else {
            return ejbDescriptor.getType();
        }
    }

    /** Assigns the type of the ejb to whcoih I refer.
    */
    @Override
    public void setType(String type) {
	refType=type;
    }

    @Override
    public String getInjectResourceType() {
        return isEJB30ClientView() ?
            getEjbInterface() : getEjbHomeInterface();
    }

    @Override
    public void setInjectResourceType(String resourceType) {
        if (isEJB30ClientView()) {
            setEjbInterface(resourceType);
        } else {
            setEjbHomeInterface(resourceType);
        }
    }

    /**
      * Gets the home classname of the referee EJB.
      */
    public String getHomeClassName() {
        return refHomeIntf;
    }

    /**
     * Sets the home classname of the bean to whcioh I refer.
     */

    public void setHomeClassName(String homeClassName) {
        refHomeIntf = homeClassName;
    }

    /**
     * @return the bean instance interface classname of the referee EJB.
     */
    public String getBeanClassName() {
        return refIntf;
    }

    /** Sets the bean instance business interface classname of the bean to which I refer.
     * this interface is the local object or the remote interfaces depending if the
     * reference is local or not.
    */
    public void setBeanClassName(String remoteClassName) {
        refIntf = remoteClassName;
    }

    /**
     * Gets the home classname of the referee EJB.
     * @return the class name of the EJB home.
     */
    @Override
    public String getEjbHomeInterface() {
        return getHomeClassName();
    }

    /**
     * Sets the local or remote home classname of the referee EJB.
     * @param homeClassName the class name of the EJB home.
     */
    @Override
    public void setEjbHomeInterface(String homeClassName) {
        setHomeClassName(homeClassName);
    }

    /**
     * Gets the local or remote interface classname of the referee EJB.
     * @return the classname of the EJB remote object.
     */
    @Override
    public String getEjbInterface() {
        return getBeanClassName();
    }
    /**
     * Sets the local or remote bean interface classname of the referee EJB.
     * @param remoteClassName the classname of the EJB remote object.
     */
    @Override
    public void setEjbInterface(String remoteClassName) {
        setBeanClassName(remoteClassName);
    }

    /**
     * @return true if the EJB reference is a 30 client view
     */
    @Override
    public boolean isEJB30ClientView() {
        return (getHomeClassName() == null);
    }

    /** returns a formatted string representing me.
    */

    @Override
    public void print(StringBuilder toStringBuilder) {
        String localVsRemote = isLocal() ? "Local" : "Remote";
        toStringBuilder.append(localVsRemote + " ejb-ref ");
        toStringBuilder.append("name="+getName());

        if( isEJB30ClientView() ) {
            toStringBuilder.append("," + localVsRemote + " 3.x interface ="+getEjbInterface());
        } else {
            toStringBuilder.append("," + localVsRemote + " 2.x home ="+getEjbHomeInterface());
            toStringBuilder.append("," + localVsRemote + " 2.x component interface="+getEjbInterface());
        }

        if( ejbDescriptor != null ) {
            toStringBuilder.append(" resolved to intra-app EJB "+ejbDescriptor.getName() +
            " in module " + ejbDescriptor.getEjbBundleDescriptor().getModuleName());
        }

        toStringBuilder.append(",ejb-link="+getLinkName());
        toStringBuilder.append(",lookup="+getLookupName());
        toStringBuilder.append(",mappedName="+getMappedName());
        toStringBuilder.append(",jndi-name=" + getValue());

        toStringBuilder.append(",refType="+getType());
    }

    /* Equality on name. */
    @Override
    public boolean equals(Object object) {
	if (object instanceof EjbReference) {
	    EjbReference ejbReference = (EjbReference) object;
	    return ejbReference.getName().equals(this.getName());
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

    public boolean isConflict(EjbReferenceDescriptor other) {
        return (getName().equals(other.getName())) &&
            (!(
                DOLUtils.equals(getType(), other.getType()) &&
                DOLUtils.equals(getEjbHomeInterface(), other.getEjbHomeInterface()) &&
                DOLUtils.equals(getEjbInterface(), other.getEjbInterface()) &&
                DOLUtils.equals(getLinkName(), other.getLinkName())
                ) ||
            isConflictResourceGroup(other));
    }
}
