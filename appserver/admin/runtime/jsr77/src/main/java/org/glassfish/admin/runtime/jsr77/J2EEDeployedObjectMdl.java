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

public abstract class J2EEDeployedObjectMdl extends J2EEEventProviderMOMdl {

    public static final int STARTING_STATE = 0;
    public static final int RUNNING_STATE = 1;
    public static final int STOPPING_STATE = 2;
    public static final int STOPPED_STATE = 3;
    public static final int FAILED_STATE = 4;

    private int state = this.RUNNING_STATE;
    private long startTime = System.currentTimeMillis();
    
    private long sequenceNo = 0;
    
    private String [] eventTypes = new String [] {
        "j2ee.state.starting",
        "j2ee.state.running",
        "j2ee.state.stopping",
        "j2ee.state.stopped",
        "j2ee.state.failed"};

    public J2EEDeployedObjectMdl(String name,boolean state, boolean statistics) {
        super(name,state,statistics);
    }

    /**
     * The deploymentDescriptor string must contain the original 
     * XML deployment descriptor that was created for this module 
     * during the deployment process. 
     */ 
    public String getdeploymentDescriptor() {
        // FIXME
        // return module.getDeploymentDescriptor();
        return null;
    }

    /** returns the OBJECT_NAME of the J2EEServer this module is deployed on. */
    public String getserver() {
        String qs = "name=" + getJ2EEServer() + ",j2eeType=J2EEServer";
        Set s = findNames(qs);
        ObjectName[] sa = (ObjectName[]) s.toArray(new ObjectName[s.size()]);
        if (sa.length > 0) {
            return sa[0].toString();
        }
        return "Failed to find the server ObjectName";
    }

    public String[] geteventTypes(){
        return eventTypes;
    }

    public int getstate(){
        return this.state;
    }

    public void setstate(int st){
        this.state = st;
        this.stateChanged(eventTypes[st]);
    }
    
    public long getstartTime(){
        return this.startTime;
    }
    public void start() {

	if ((this.state == this.STARTING_STATE) ||
	    (this.state == this.RUNNING_STATE) ||
	    (this.state == this.STOPPING_STATE)) {
            throw new RuntimeException(
		  new Exception ("cannot start because the current state is " + this.state));
	}

        try{
            this.state = this.STARTING_STATE;
            this.stateChanged("j2ee.state.starting");
            // FIXME
	    // module.start(this);
            this.state = this.RUNNING_STATE;
            this.startTime = System.currentTimeMillis();
            this.stateChanged("j2ee.state.running");
        }catch(Exception ex){
            this.state = this.FAILED_STATE;
            this.stateChanged("j2ee.state.failed");
	    if(ex instanceof RuntimeException)
                throw (RuntimeException)ex;
            throw new RuntimeException(ex);
        }
    }

    public void stop() throws MBeanException {

	if ((this.state == this.STOPPED_STATE) ||
	    (this.state == this.STOPPING_STATE)) {
            throw new RuntimeException(
		new Exception("cannot stop because the current state is " + this.state));
	}

        try{
            this.state = this.STOPPING_STATE;
            this.stateChanged("j2ee.state.stopping");
            // FIXME
	    // module.stop(this);
            this.state = this.STOPPED_STATE;
            this.stateChanged("j2ee.state.stopped");
        }catch(Exception ex){
            this.state = this.FAILED_STATE;
            this.stateChanged("j2ee.state.failed");
	    if(ex instanceof RuntimeException)
                throw (RuntimeException)ex;
            throw new RuntimeException(ex);
        }
    }

    public void startRecursive() throws MBeanException {
        start();
    }

    private void stateChanged(String state){
        ObjectName objectName = null;
        try {
            objectName = new ObjectName(this.getobjectName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if ( objectName != null )
        {
            Notification notification   = null;
            synchronized (this) // thread safety, visibility of 'sequenceNo'
            {
                notification = new Notification( state, objectName, sequenceNo);
                ++sequenceNo;
            }
            this.sendNotification( notification );
        }
    }
    
}
