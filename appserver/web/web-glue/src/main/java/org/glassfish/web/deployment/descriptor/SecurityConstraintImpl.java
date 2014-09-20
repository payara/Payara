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

import com.sun.enterprise.deployment.web.AuthorizationConstraint;
import com.sun.enterprise.deployment.web.SecurityConstraint;
import com.sun.enterprise.deployment.web.UserDataConstraint;
import com.sun.enterprise.deployment.web.WebResourceCollection;
import org.glassfish.deployment.common.Descriptor;

import java.util.HashSet;
import java.util.Set;


    /** 
    * Objects exhibiting this interface represent a security constraint on the web application
    * that owns them. 
    * @author Danny Coward
    */

public class SecurityConstraintImpl extends Descriptor implements SecurityConstraint {
    private Set<WebResourceCollection> webResourceCollections;
    private AuthorizationConstraint authorizationConstraint;
    private UserDataConstraint userDataConstraint;
    
    /** Default constructor.*/
    public SecurityConstraintImpl() {
    
    }
    
    /** Copy constructor.*/
    public SecurityConstraintImpl(SecurityConstraintImpl other) {
        if (other.webResourceCollections != null) {
	        this.webResourceCollections = new HashSet<WebResourceCollection>();
            for (WebResourceCollection wrc : other.webResourceCollections) {
                webResourceCollections.add(new WebResourceCollectionImpl((WebResourceCollectionImpl)wrc));
            }
        }
        if (other.authorizationConstraint != null) {
            this.authorizationConstraint = new AuthorizationConstraintImpl((AuthorizationConstraintImpl) other.authorizationConstraint);
        }
        if (other.userDataConstraint != null) {
            this.userDataConstraint = new UserDataConstraintImpl();
            this.userDataConstraint.setTransportGuarantee(other.userDataConstraint.getTransportGuarantee());
        }
    }
    
    
    /** Return all the web resource collection.
    */
    public Set<WebResourceCollection> getWebResourceCollections() {
	if (this.webResourceCollections == null) {
	    this.webResourceCollections = new HashSet<WebResourceCollection>();
	}
	return this.webResourceCollections;
    }
    
    /** Adds a web resource collection to this constraint.*/
    public void addWebResourceCollection(WebResourceCollection webResourceCollection) {
        this.getWebResourceCollections().add(webResourceCollection);
    }

    public void addWebResourceCollection(WebResourceCollectionImpl webResourceCollection) {    
        addWebResourceCollection((WebResourceCollection) webResourceCollection);
    }

    /** Removes the given web resource collection from this constraint.*/
    public void removeWebResourceCollection(WebResourceCollection webResourceCollection) {
        this.getWebResourceCollections().remove(webResourceCollection);
    }
    
	/** The authorization constraint. */
    public AuthorizationConstraint getAuthorizationConstraint() {
	return this.authorizationConstraint;
    }
    
    /** Sets the authorization constraint.*/
    public void setAuthorizationConstraint(AuthorizationConstraint authorizationConstraint) {
	this.authorizationConstraint = authorizationConstraint;
    }
    
    /** Sets the authorization constraint.*/
    public void setAuthorizationConstraint(AuthorizationConstraintImpl authorizationConstraint) {
	setAuthorizationConstraint((AuthorizationConstraint) authorizationConstraint);
    }
    
	/** The user data constraint. */
    public UserDataConstraint getUserDataConstraint() {
	return this.userDataConstraint;
    }
	/** Sets the user data constraint. */
    public void setUserDataConstraint(UserDataConstraint userDataConstraint) {
	this.userDataConstraint = userDataConstraint;
    }
    
    public void setUserDataConstraint(UserDataConstraintImpl userDataConstraint) {
	setUserDataConstraint((UserDataConstraint) userDataConstraint);
    }

    /** Returns a formatted String representing of my state.*/
    public void print(StringBuffer toStringBuffer) {
	toStringBuffer.append("SecurityConstraint: ");
	toStringBuffer.append(" webResourceCollections: ").append(webResourceCollections);
	toStringBuffer.append(" authorizationConstraint ").append(authorizationConstraint);
	toStringBuffer.append(" userDataConstraint ").append(userDataConstraint);
    
    }
    
}
