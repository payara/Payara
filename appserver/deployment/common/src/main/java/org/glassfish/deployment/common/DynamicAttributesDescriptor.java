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

package org.glassfish.deployment.common;

import java.io.Serializable;
import java.util.*;

/**
 * This class is a value holder for dynamic attributes. Dynamic attributes 
 * can be added, queried and removed from this value holder. Attributes are 
 * identified by a string key.
 *
 * @author Jerome Dochez
 */
public class DynamicAttributesDescriptor extends Observable implements Serializable {
    
    private Map dynamicAttributes;
    
    /**
    * Direct acess to the dynamic attributes repository
    * @return the Map of dynamic attributes
    */

    public Map getExtraAttributes() {
	if (dynamicAttributes == null) {
	    dynamicAttributes = new Hashtable();
	}
	return dynamicAttributes;
    }
    
    /**
     * Add a new dynamic attribte
     * @param name the attribute name
     * @param value the attribute value
     */
    public void addExtraAttribute(String name, Object value) {
        if (value==null) {
            return;
        }
	if (dynamicAttributes == null) {
	    dynamicAttributes = new Hashtable();
	}
        dynamicAttributes.put(name, value);
        changed();
    }
    
    /**
     * Obtain a dynamic attribute from the repository
     * @param name the attribute name
     * @return the attribute value of null of non existent
     */
    public Object getExtraAttribute(String name) {
        if (dynamicAttributes == null) {
            return null;
        }
        return dynamicAttributes.get(name);
    }
    
    /**
     * Removes a dynamic attribute from the repository
     * @param name the attribute name
     */
    public void removeExtraAttribute(String name) {
        if (dynamicAttributes == null) {
            return;
        } 
        dynamicAttributes.remove(name);
        changed();
    }
    
    /**
     * @return a meaningfull string about ourself
     * This method is invoked by toString() method.  This method can be overwritten by any descriptor which inherits this class
     * to self describe. This pattern is adopted to imporve the performance of S1AS 8.0 which avoids creation of several String objects
     * in toString() method. 
     * When toString() method is called on a Descriptor object, the toString method of this class is called.  
     * The toString method of this class invokes print(StringBuffer) method.  If the Descriptor object overrides print method, its method
     * will be invoked. 
     * For better performance, care should be taken to use print method on all descriptors instead of printing object itself (which calls to toString).
     * For example 
     * Iterator itr = getDeploymentExtensions();
            if (itr!=null && itr.hasNext()) {
               do {
                   sb.append("\n Deployment Extension : ").append(itr.next());
               } while (itr.hasNext());
           }


            should probably read as below.

           Iterator itr = getDeploymentExtensions();
           if (itr!=null && itr.hasNext()) {
               do {
                   sb.append("\n Deployment Extension : ");
            ((Descriptor) itr.next()).print(sb);
               } while (itr.hasNext());
           } 
     */ 
    public void print(StringBuffer toStringBuffer) {
        if (dynamicAttributes==null) {
            toStringBuffer.append("<== No attribute ==>");
        }  else {
           toStringBuffer.append("==>Dynamic Attribute");
           Set keys = dynamicAttributes.keySet();
           for (Iterator itr = keys.iterator();itr.hasNext();) {
               String keyName = (String) itr.next();
               Object o = getExtraAttribute(keyName);
	       if (o instanceof Object[]) {
		   Object[] objects = (Object[]) o;
		   for (int i=0;i<objects.length;i++) {
		       toStringBuffer.append("\n Indexed prop name ").append(keyName).append("[").append(i).append("] = ");
                       if(objects[i] instanceof DynamicAttributesDescriptor)
                            ((DynamicAttributesDescriptor)objects[i]).print(toStringBuffer);
                       else
                            toStringBuffer.append(objects[i]);
		   }
	       } else {
		   toStringBuffer.append("\n  Property name = ").append(keyName).append(" value = ");
                   if(o instanceof DynamicAttributesDescriptor)
                            ((DynamicAttributesDescriptor)o).print(toStringBuffer);
                       else
                            toStringBuffer.append(o);
               }
           }
	   toStringBuffer.append("\n<==End");
           return ;
        }
    }
    /**
     * @return a meaningfull string about ourself
     * No Descriptor class which inherits this class should override this method.  Rather print() method which is defined in this class
     * should be overridden to describe itself. Refer to the comments on print() method for more details.
     * This method is optimized for persformance reasons.
     */ 
    public String toString() {
        StringBuffer toStringBuf = new StringBuffer();
        this.print(toStringBuf);
        return toStringBuf.toString();
    }
    
    /**
     * notify our observers we have changed
     */
    private void changed() {
        setChanged();
        notifyObservers();
    }
}
