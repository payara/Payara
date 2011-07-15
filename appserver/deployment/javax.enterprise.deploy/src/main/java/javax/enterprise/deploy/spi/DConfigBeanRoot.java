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

package javax.enterprise.deploy.spi;

import javax.enterprise.deploy.model.DDBeanRoot;

/**
 * A Java EE component module consists of one or more deployment 
 * descriptor files and zero or more non-deployment descriptor 
 * XML instance documents.  A module must contain a component-specific 
 * deployment descriptor file (see the component specification for 
 * details). It may contain one or more secondary deployment descriptor 
 * files that define extra functionality on the component and zero or more 
 * non-deployment descriptor XML instance documents (see the Web Services 
 * specification).
 *
 * <p>
 * The DConfigBeanRoot object is a deployment configuration bean 
 * (DConfigBean) that is associated with the root of the component's 
 * deployment descriptor. It must be created by calling the 
 * DeploymentConfiguration.getDConfigBean(DDBeanRoot) method, where 
 * DDBeanRoot represents the component's deployment descriptor.
 *
 * <p>
 * A DConfigBean object is associated with a deployment descriptor 
 * that extends a component's functionality.  It must be created by 
 * calling the DConfigBeanRoot.getDConfigBean(DDBeanRoot) method. This 
 * DConfigBean object is a child of the compontent's DConfigBeanRoot 
 * object.  The DDBeanRoot argument represents the secondary deployment 
 * descriptor.  Deployment  descriptor files  such as webservice.xml and 
 * webserviceclient.xml are examples of secondary deployment descriptor 
 * files.
 *
 * <p>
 * The server plugin must request a DDBeanRoot object for any non-deployment 
 * descriptor XML instance document data it requires.  The plugin must 
 * call method DeployableObject.getDDBeanRoot(String) where String is the 
 * full path name from the root of the module to the file to be represented. 
 * A WSDL file is an example of a non-deployment descriptor XML instance 
 * document.
 *
 * @author  gfink
 */
public interface DConfigBeanRoot extends DConfigBean {

    /**
     * Return a DConfigBean for a deployment descriptor that is not 
     * the module's primary deployment descriptor.   Web services 
     * provides a deployment descriptor in addition to the module's 
     * primary deployment descriptor.  Only the DDBeanRoot for this 
     * catagory of secondary deployment descriptors are to be passed 
     * as arguments through this method.  
     *
     * Web service has two deployment descriptor files, one that defines 
     * the web service and one that defines a client of a web service.
     * See the Web Service specificiation for the details.

     *
     * @param ddBeanRoot represents the root element of a deployment 
     *    descriptor file. 
     *
     * @return a DConfigBean to be used for processing this deployment 
     *   descriptor data. Null may be returned if no DConfigBean is 
     *   required for this deployment descriptor. 
     */ 
     public DConfigBean getDConfigBean(DDBeanRoot ddBeanRoot); 
}

