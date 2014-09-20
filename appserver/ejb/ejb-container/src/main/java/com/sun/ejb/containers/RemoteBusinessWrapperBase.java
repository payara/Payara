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

package com.sun.ejb.containers;

import java.rmi.Remote;

import java.io.ObjectInputStream;
import java.io.WriteAbortedException;
import java.io.ObjectStreamException;
import java.io.IOException;

import com.sun.ejb.EJBUtils;

public class RemoteBusinessWrapperBase 
    implements java.io.Serializable {

    // This is the name of the developer-written business interface.
    private String businessInterface_;

    private Remote stub_;
    
    private transient int hashCode_;

    public RemoteBusinessWrapperBase(Remote stub, String busIntf) {
        stub_ = stub;
        businessInterface_ = busIntf;
        this.hashCode_ = busIntf.hashCode();
    }

    public Remote getStub() {
        return stub_;
    }
    
    public int hashCode() {
        return hashCode_;
    }

    public boolean equals(Object obj) {
        
        boolean result = (obj == this); //Most efficient
        if ((result == false) && (obj != null)) { //Do elaborate checks
            if (obj instanceof RemoteBusinessWrapperBase) {
                RemoteBusinessWrapperBase remoteBWB =
                        (RemoteBusinessWrapperBase) obj;
                boolean hasSameBusinessInterface =
                        (remoteBWB.hashCode_ == hashCode_) &&
                        remoteBWB.businessInterface_.equals(businessInterface_);
                if (hasSameBusinessInterface) {
                    org.omg.CORBA.Object other = (org.omg.CORBA.Object) remoteBWB.stub_;
                    org.omg.CORBA.Object me = (org.omg.CORBA.Object) stub_;
                    result = me._is_equivalent(other);
                }
            }
        }
        
        return result;
    }
    
    public String getBusinessInterfaceName() {
        return businessInterface_;
    }

    public Object writeReplace() throws ObjectStreamException {
        return new RemoteBusinessWrapperBase(stub_, businessInterface_); 
    }

    private void writeObject(java.io.ObjectOutputStream oos) 
        throws java.io.IOException 
    {

        oos.writeObject(businessInterface_);
        oos.writeObject(stub_);

    }

    private void readObject(ObjectInputStream ois) 
        throws IOException, ClassNotFoundException {

        try {

            businessInterface_ = (String) ois.readObject();
            hashCode_ = businessInterface_.hashCode();

            EJBUtils.loadGeneratedRemoteBusinessClasses(businessInterface_);

            stub_ = (Remote) ois.readObject();

        } catch(Exception e) {
            IOException ioe = new IOException("RemoteBusinessWrapper.readObj "
                                              + " error");
            ioe.initCause(e);
            throw ioe;
        }
    }
                 
    public Object readResolve() throws ObjectStreamException {

        try {

            return EJBUtils.createRemoteBusinessObject(businessInterface_,
                                                       stub_);
        } catch(Exception e) {
            WriteAbortedException wae = new WriteAbortedException
                ("RemoteBusinessWrapper.readResolve error", e);
            throw wae;
        }

    }
                



}

