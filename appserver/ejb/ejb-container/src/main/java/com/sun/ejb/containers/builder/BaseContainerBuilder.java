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

package com.sun.ejb.containers.builder;

import java.net.InetAddress;

import java.util.logging.Logger;

import com.sun.ejb.base.container.util.CacheProperties;

import com.sun.ejb.base.sfsb.util.CheckpointPolicyImpl;
import com.sun.ejb.base.sfsb.util.ScrambledKeyGenerator;
import com.sun.ejb.base.sfsb.util.SimpleKeyGenerator;

import com.sun.ejb.containers.BaseContainer;

import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbMessageBeanDescriptor;

import com.sun.enterprise.util.Utility;

import com.sun.logging.LogDomains;
import org.glassfish.api.deployment.DeploymentContext;

/**
 * (Abstract)Base class for all ContainerBuilders
 *
 * @author Mahesh Kannan
 */
public abstract class BaseContainerBuilder {

    protected static final Logger _logger =
        LogDomains.getLogger(BaseContainerBuilder.class, LogDomains.EJB_LOGGER);

    protected EjbDescriptor		    ejbDescriptor;
    protected ClassLoader		    loader;
    protected BaseContainer		    baseContainer;

    //Following two variables are used for constructing SFSBUUIDUtil
    private byte[]			    ipAddress;
    private int				    port;

    private DeploymentContext dynamicDeploymentContext;

    public BaseContainerBuilder() {
    }

    public final void buildContainer(DeploymentContext dynamicDeploymentContext,
                                     EjbDescriptor ejbDescriptor, ClassLoader loader)
	throws Exception
    {
        this.dynamicDeploymentContext = dynamicDeploymentContext;
	this.ejbDescriptor = ejbDescriptor;
	this.loader = loader;

	readDescriptor();

	baseContainer = createContainer(ejbDescriptor, loader); //abstract method
	buildComponents();  //abstract method
    }
    
    public final BaseContainer getContainer() {
	return baseContainer;
    }

    //Currently not called from ContainerFactoryImpl.
    public final void postInitialize(SecurityManager sm) {
	//baseContainer.setSecurityManager(sm);

	boolean hasHome = !(ejbDescriptor instanceof EjbMessageBeanDescriptor);
	if (hasHome) {
	    //Currently BaseContainer.initializeHome is protected.
	    //baseContainer.initializeHome();
	}
    }

    ///////////////// Protected methods /////////////////

    protected abstract BaseContainer createContainer(
	    EjbDescriptor ejbDescriptor, ClassLoader loader)
	throws Exception;

    protected abstract void buildComponents()
	throws Exception;

    protected byte[] getIPAddress() {
	return this.ipAddress;
    }

    protected int getPort() {
	return this.port;
    }

    protected DeploymentContext getDynamicDeploymentContext() {
        return dynamicDeploymentContext;
    }

    ////////////////// Private methods //////////////////

    private void readDescriptor() {
	//FIXME: Read from domain.xml iiop-service ip-addr
	this.ipAddress = new byte[4];
	try {
	    this.ipAddress = InetAddress.getLocalHost().getAddress();
	} catch (Exception ex) {
	    long val = System.identityHashCode(ipAddress)
		+ System.currentTimeMillis();
	    Utility.longToBytes(val, this.ipAddress, 0);
	}

	//FIXME: Read from domain.xml
	this.port = 8080;
    }

}
