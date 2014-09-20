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

package com.sun.enterprise.transaction.api;

import java.rmi.RemoteException;
import java.io.Serializable;
import java.util.ArrayList;

public class TransactionAdminBean implements java.io.Serializable {
	private Object m_identifier;
        private String m_id;
	private String m_status;
	private long m_elapsedTime;
        private String m_componentName;
        private ArrayList<String> m_resourceNames;

	public TransactionAdminBean(Object identifier, String id, String status, long elapsedTime,
                                    String componentName, ArrayList<String> resourceNames) {
		m_identifier = identifier;
                m_id=id;
		m_status = status;
		m_elapsedTime = elapsedTime;
                m_componentName = componentName;
                m_resourceNames = resourceNames;
	}

	// getter functions ...

	public Object getIdentifier(){
		return m_identifier;
	}

        public String getId(){
            return m_id;
        }

	public String getStatus(){
		return m_status;
	}

	public long getElapsedTime(){
		return m_elapsedTime;
	}

        public String getComponentName() {
            return m_componentName;
        }

        public ArrayList<String> getResourceNames() {
            return m_resourceNames;
        }

	// setter functions ...

	public void setIdentifier(Object id){
		m_identifier = id;
        }

        public void setId(String id){
            m_id=id;
        }

	public void setStatus(String sts){
		m_status = sts;
	}

	public void setElapsedTime(long time){
		m_elapsedTime = time;
	}

        public void setComponentName(String componentName) {
            m_componentName = componentName;
        }

        public void setResourceNames(ArrayList<String> resourceNames) {
            m_resourceNames = resourceNames;
        }
     
}



