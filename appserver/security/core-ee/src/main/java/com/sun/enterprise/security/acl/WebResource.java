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

package com.sun.enterprise.security.acl;
/**
 * @author Harish Prabandham
 */
public class WebResource extends Resource {
    private transient boolean wildcard;
    private transient String path;

    public WebResource(String app, String name, String method) {
        super(app,name,method);
        init(name);
    }

    private void init(String name)                                       
    {                                                                    
    	if (name == null)                                                    
            throw new IllegalArgumentException("name can't be null");
                                                                         
    	if (name.endsWith("/*") || name.equals("*")) {
            wildcard = true;
            if (name.length() == 1) {
                path = "";
            } else {
                path = name.substring(0, name.length()-1);
            }
    	} else {
            path = name;
    	}                                                                    
    }                                                                    

    public boolean equals(Object obj) {
        if(obj == this)
            return true;
        
        if ((obj == null) || (obj.getClass() != getClass()))
            return false;
        
        Resource r = (Resource) obj;
        
        return getApplication().equals(r.getApplication()) &&
            getMethod().equals(r.getMethod()) &&
            getName().equals(r.getName());
    }

    public boolean implies(Resource resource) {
	if(( resource == null) || (resource.getClass() != getClass())) 
		return false;

	WebResource that = (WebResource) resource;

	// Application name is not an issue in implies .....
	if(!getMethod().equals(that.getMethod()))
            return false;
		
	if(this.wildcard) {
            if (that.wildcard)
                // one wildcard can imply another
                return that.path.startsWith(path);
            else
                // make sure ap.path is longer so a/b/* doesn't imply a/b
                return (that.path.length() > this.path.length()) &&
                    that.path.startsWith(this.path);
	} else {
	    if (that.wildcard) {
                // a non-wildcard can't imply a wildcard
                return false;
            }
            else {
                return this.path.equals(that.path);
            }
	}
    }
}
