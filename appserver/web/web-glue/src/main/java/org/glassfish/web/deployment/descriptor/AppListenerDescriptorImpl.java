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

import com.sun.enterprise.deployment.web.AppListenerDescriptor;
import org.glassfish.deployment.common.Descriptor;

/** 
 * Objects exhibiting this interface represent an event listener descriptor.
 * This represents the <listener-class> XML element defined in the
 * Servlet 2.3 spec.
 * @author Vivek Nagar
 */

public class AppListenerDescriptorImpl extends Descriptor
        implements AppListenerDescriptor
{
    private String listenerClass;
    private String displayName;
    
    /** The default constructor.
    */
    public AppListenerDescriptorImpl() {
    }
    
    /**
     * Create an instance of the descriptor with the specified listener class.
     * @param the listener class name.
     */
    public AppListenerDescriptorImpl(String clz) {
	this.listenerClass = clz;
    }
    
    /** 
     * Return the listener class. 
     * @return the listener class name or empty string if none.
     */
    public String getListener() {
	if (this.listenerClass == null) {
	    this.listenerClass = "";
	}
	return this.listenerClass;
    }
    
    /** 
     * Sets the listener class.
     * @param the listener class name.
     */
    public void setListener(String clz) {
	this.listenerClass = clz;
    }

    /** set display name */
    public void setDisplayName(String name) {
	this.displayName = (name != null)? name : "";
    }

    /** get display name */
    public String getDisplayName() {
	String n = this.displayName;
	if ((n == null) || n.equals("")) {
	    n = this.getName();
	}
	return n;
    }

    /** 
     * Test for equals 
     */
    public boolean equals(Object obj) {
	return (obj instanceof AppListenerDescriptorImpl)?
	    this.getListener().equals(
		((AppListenerDescriptorImpl)obj).getListener()) :
	    super.equals(obj);
    }

    public int hashCode() {
        int result = 17;
        result = 37*result + getListener().hashCode();
        return result;
    }


    /** 
     * A formatted version of the state as a String. 
     */
    public void print(StringBuffer toStringBuffer) {
        toStringBuffer.append("Listener Class ").append(this.getListener());
    }
}

