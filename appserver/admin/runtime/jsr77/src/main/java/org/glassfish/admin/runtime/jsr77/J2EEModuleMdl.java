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

package org.glassfish.admin.runtime.jsr77;

import java.util.*;
import javax.management.*;

public abstract class J2EEModuleMdl extends J2EEDeployedObjectMdl {

    HashSet externalVMs = new HashSet();

    public J2EEModuleMdl(String name,boolean state, boolean statistics) {
        // FIXME
        super(name,state,statistics);
    }

    public String [] getjavaVMs() {
        //get the internal VM name
        Set res = new HashSet();
        //get the external VM names
        Iterator vmNames = externalVMs.iterator();
        while(vmNames.hasNext()) {
              String vmName = (String)vmNames.next();
              Set x = findNames("j2eeType=JVM,name=" + vmName);
              if(x.size() > 0) {
        		res.addAll(x);
              } else {
                //no longer an active VM
                externalVMs.remove(vmName);
                vmNames = externalVMs.iterator();
              }
        }
        Iterator it = res.iterator();
        String [] vms = new String [res.size()];
        int i =0;
        while(it.hasNext()) {
            vms[i++] = ((ObjectName)it.next()).toString();
        }
        return vms;
    }
    
    /**
     * The name of the J2EEManagedObject. All managed objects must have a unique name within the context of the management
     * domain. The name must not be null.
     */
    public String getobjectName() {
        Set s = findNames("j2eeType="+getj2eeType()+",name="+this.getModuleName()+",J2EEApplication="+this.getJ2EEApplication()+",J2EEServer="+this.getJ2EEServer());
        Object [] objs = s.toArray();
        if (objs.length > 0) {
        	String name = ((ObjectName)objs[0]).toString();
        	return name;
        } else {
            return null;
        }
    }

    public void addVm(String name) {
        externalVMs.add(name);
    }
    /**
     * Overriding method of ManagedObject 
     */
    public boolean isstateManageable() {
        return false;
    }
    /**
     * Overriding method of ManagedObject 
     */
    public boolean iseventProvider() {
        return false;
    }

    public abstract String getModuleName();

    public abstract String getJ2EEApplication();

    public static boolean isStandAloneModule(String applicationName){
	if ( applicationName == null 
	     || applicationName.equals("") 
	     || applicationName.equals("null") ) {
	    return true;
	}
        return false;
    } 
}
