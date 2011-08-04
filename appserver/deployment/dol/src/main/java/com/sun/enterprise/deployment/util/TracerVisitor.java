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

package com.sun.enterprise.deployment.util;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.types.MessageDestinationReferencer;
import org.glassfish.deployment.common.Descriptor;

import java.util.Iterator;

/**
 *
 * @author  dochez
 * @version 
 */
public class TracerVisitor extends DefaultDOLVisitor {

    /** Creates new TracerVisitor */
    public TracerVisitor() {
    }
    
    /**
     * visit an application object
     * @param the application descriptor
     */
    public void accept(Application application) {
	DOLUtils.getDefaultLogger().info("Application");
	DOLUtils.getDefaultLogger().info("name " + application.getName());
	DOLUtils.getDefaultLogger().info("smallIcon " + application.getSmallIconUri());
    }
    
    /**
     * visits an ejb bundle descriptor
     * @param an ejb bundle descriptor
     */
    public void accept(EjbBundleDescriptor bundleDescriptor) {
        DOLUtils.getDefaultLogger().info("Ejb Bundle " + bundleDescriptor.getName());
    }
    
    /**
     * visits an ejb descriptor
     * @param ejb descriptor
     */
    public void accept(EjbDescriptor ejb) {
        DOLUtils.getDefaultLogger().info("==================");                
        DOLUtils.getDefaultLogger().info(ejb.getType() + " Bean " + ejb.getName());        
	DOLUtils.getDefaultLogger().info("\thomeClassName " + ejb.getHomeClassName());
	DOLUtils.getDefaultLogger().info("\tremoteClassName " + ejb.getRemoteClassName());
	DOLUtils.getDefaultLogger().info("\tlocalhomeClassName " +ejb.getLocalHomeClassName());
	DOLUtils.getDefaultLogger().info("\tlocalClassName " + ejb.getLocalClassName());
	DOLUtils.getDefaultLogger().info("\tremoteBusinessIntfs " + ejb.getRemoteBusinessClassNames());
	DOLUtils.getDefaultLogger().info("\tlocalBusinessIntfs " + ejb.getLocalBusinessClassNames());

	DOLUtils.getDefaultLogger().info("\tjndiName " + ejb.getJndiName());        
	DOLUtils.getDefaultLogger().info("\tejbClassName " + ejb.getEjbClassName());
	DOLUtils.getDefaultLogger().info("\ttransactionType " + ejb.getTransactionType());
        if (ejb.getUsesCallerIdentity() == false) {
            DOLUtils.getDefaultLogger().info("\trun-as role " + ejb.getRunAsIdentity());             
        } else {
            DOLUtils.getDefaultLogger().info("\tuse-caller-identity " + ejb.getUsesCallerIdentity()); 
        }
    }

    /**
     * visits an ejb reference  for the last J2EE component visited
     * @param the ejb reference
     */
    public void accept(EjbReference ejbRef) {
        DOLUtils.getDefaultLogger().info(ejbRef.toString());
    }
    
    public void accept(MessageDestinationReferencer referencer) {
        DOLUtils.getDefaultLogger().info
            (referencer.getMessageDestinationLinkName());
    }
    
    public void accept(WebService webService) {
        DOLUtils.getDefaultLogger().info(webService.getName());
    }

    public void accept(ServiceReferenceDescriptor serviceRef) {
        DOLUtils.getDefaultLogger().info(serviceRef.getName());
    }

    /**
     * visits a method permission and permitted methods  for the last J2EE component visited
     * @param ejb descriptor the role is referenced from
     * @param method permission 
     * @param the methods associated with the above permission
     */
    public void accept(MethodPermission pm, Iterator methods) {
        DOLUtils.getDefaultLogger().info("For method permission : " + pm.toString());
        while (methods.hasNext()) {
            DOLUtils.getDefaultLogger().info("\t"  + ((MethodDescriptor) methods.next()).prettyPrint());
        }
    }
    
    /**
     * visits a role reference  for the last J2EE component visited
     * @param ejb descriptor the role is referenced from*
     * @param role reference
     */
    public void accept(RoleReference roleRef) {
        DOLUtils.getDefaultLogger().info("Security Role Reference : " 
                                + roleRef.getName() + " link " + roleRef.getValue());
    }
    /**
     * visists a method transaction  for the last J2EE component visited
     * @param ejb descritptor this method applies to
     * @param method descriptor the method
     * @param container transaction
     */
    public void accept(MethodDescriptor method, ContainerTransaction ct) {
            
        DOLUtils.getDefaultLogger().info( ct.getTransactionAttribute() 
                                + " Container Transaction for method "
                                + method.prettyPrint() );
            
    }
    
    /**
     * visists an environment property  for the last J2EE component visited
     * @paren the environment property
     */
    public void accept(EnvironmentProperty envEntry) {
        DOLUtils.getDefaultLogger().info( envEntry.toString());    
    }

    /**
     * visits a CMP field definition (for CMP entity beans)
     * @param field descriptor for the CMP field
     */
    public void accept(FieldDescriptor fd) {
        DOLUtils.getDefaultLogger().info("CMP Field "  +fd);
    }
    
    /**
     * visits a query method
     * @param method descriptor for the method
     * @param query descriptor
     */
    public void accept(MethodDescriptor method, QueryDescriptor qd) {
        DOLUtils.getDefaultLogger().info(qd.toString());
    }

    /**
     * visits an ejb relationship descriptor
     * @param the relationship descriptor
     */
    public void accept(RelationshipDescriptor descriptor) {
        DOLUtils.getDefaultLogger().info("============ Relationships ===========");
        DOLUtils.getDefaultLogger().info("From EJB " + descriptor.getSource().getName()  
                                                                    + " cmr field : " + descriptor.getSource().getCMRField() 
                                                                    + "(" + descriptor.getSource().getCMRFieldType() + ")  to EJB " + descriptor.getSink().getName() 
                                                                    + " isMany " + descriptor.getSource().getIsMany() 
                                                                    + " cascade-delete " + descriptor.getSource().getCascadeDelete());                                                                    
        
       DOLUtils.getDefaultLogger().info("To  EJB " + descriptor.getSink().getName()
                                                                    + " isMany " + descriptor.getSink().getIsMany() 
                                                                    + " cascade-delete " + descriptor.getSink().getCascadeDelete());        

        if (descriptor.getIsBidirectional()) {        
            DOLUtils.getDefaultLogger().info( "Bidirectional cmr field : " + descriptor.getSink().getCMRField()
                                                                    + "(" + descriptor.getSink().getCMRFieldType() + ")");
        }
    }    
    
    /**
     * visits a J2EE descriptor
     * @param the descriptor
     */
    public void accept(Descriptor descriptor) {
        DOLUtils.getDefaultLogger().info(descriptor.toString());
    } 
}
