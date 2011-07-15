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

package com.sun.enterprise.admin.servermgmt;

import java.util.Map;
import java.util.Properties;

/**
 */
public interface InstancesManager
{    
    /**
     * Creates a server instance.
     * @throws InstanceException  This exception is thrown if 
     *  - the instance already exists.
     *  - an invalid or insufficient config. is supplied.
     *  - an exception occurred during instance creation.
     */
    public void createInstance() 
        throws InstanceException;

    /**
     * Deletes an instance identified by the given name.
     * (Should we stop the instance before deleting the instance?)
     * @throws InstanceException  This exception is thrown if 
     * - the instance doesnot exist.
     * - an exception occurred while deleting the instance.
     */
    public void deleteInstance() 
        throws InstanceException;

    /**
     * Starts the instance.
     * @param startParams 
     * @throws InstanceException
     */
    public Process startInstance() 
        throws InstanceException;

    /**
     * Starts the instance.
     * @param interativeOptions which may be used for security, these paramters
     * are passed in on the standard input stream of the executing process
     * @throws InstanceException
     */
    public Process startInstance(String[] interativeOptions) 
        throws InstanceException;

    /**
     * Starts the instance.
     * @param interativeOptions which may be used for security, these paramters
     *        are passed in on the standard input stream of the executing process
     * @param commandLineArgs is additional commandline arguments that are to be appended
     *        to the processes commandline when it starts
     * @throws InstanceException
     */
    public Process startInstance(String[] interativeOptions, String[] commandLineArgs) 
        throws InstanceException;
  
    /**
     * Starts the instance.
     * @param interativeOptions which may be used for security, these paramters
     *        are passed in on the standard input stream of the executing process
     * @param commandLineArgs is additional commandline arguments that are to be appended
     *        to the processes commandline when it starts
     * @param envProps properties to be added to System
     * @throws InstanceException
     */
    public Process startInstance(String[] interativeOptions, String[] commandLineArgs, Properties envProps) 
        throws InstanceException;
    
    /**
     * Stops the instance.
     * @throws InstanceException  
     */    
    public void stopInstance() 
        throws InstanceException;

    /**
     * Lists all the instances.
     */
    public String[] listInstances() 
        throws InstanceException;

    /**
     * Returns status of an instance.
     */
    public int getInstanceStatus()
        throws InstanceException;

    /**
     * @return true if the instance requires a restart for some config changes
     * to take effect, false otherwise.
     */
    boolean isRestartNeeded() throws InstanceException;

    public String getNativeName();
    
    /**
     * Trys to stop the instance with the specified timeout.
     * Returns true if success; false if failure  
     * @throws InstanceException  
     */    
    public boolean stopInstanceWithinTime(int timeout) 
        throws InstanceException;
    
    public void killRelatedProcesses() throws InstanceException;        
}
