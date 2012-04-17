/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.deployment.types.*;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.DescriptorVisitor;

import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author  dochez
 * @version 
 */
public class TracerVisitor extends DefaultDOLVisitor implements ApplicationVisitor, EjbBundleVisitor {

    /** Creates new TracerVisitor */
    public TracerVisitor() {
    }
    
    public void accept (BundleDescriptor descriptor) {
        if (descriptor instanceof Application) {
            Application application = (Application)descriptor;
            accept(application);

            for (BundleDescriptor ebd : application.getBundleDescriptorsOfType(org.glassfish.deployment.common.DeploymentUtils.ejbType())) {
                ebd.visit(getSubDescriptorVisitor(ebd));
            }

            for (BundleDescriptor wbd : application.getBundleDescriptorsOfType(org.glassfish.deployment.common.DeploymentUtils.warType())) {
                // This might be null in the case of an appclient
                // processing a client stubs .jar whose original .ear contained
                // a .war.  This will be fixed correctly in the deployment
                // stage but until then adding a non-null check will prevent
                // the validation step from bombing.
                if (wbd != null) {
                    wbd.visit(getSubDescriptorVisitor(wbd));
                }
            }

            for (BundleDescriptor cd :  application.getBundleDescriptorsOfType(org.glassfish.deployment.common.DeploymentUtils.rarType())) {
                cd.visit(getSubDescriptorVisitor(cd));
            }

            for (BundleDescriptor acd : application.getBundleDescriptorsOfType(org.glassfish.deployment.common.DeploymentUtils.carType())) {
               acd.visit(getSubDescriptorVisitor(acd));
            }
            super.accept(descriptor);
        } else if (descriptor instanceof EjbBundleDescriptor) {
            EjbBundleDescriptor ejbBundle = (EjbBundleDescriptor)descriptor;
            accept(ejbBundle);

            for (EjbDescriptor anEjb : ejbBundle.getEjbs()) {
                anEjb.visit(getSubDescriptorVisitor(anEjb));
            }
            if (ejbBundle.hasRelationships()) {
                for (Iterator itr = ejbBundle.getRelationships().iterator();itr.hasNext();) {
                    RelationshipDescriptor rd = (RelationshipDescriptor) itr.next();
                    accept(rd);
                }
            }
            for (WebService aWebService : ejbBundle.getWebServices().getWebServices()) {
                accept(aWebService);
            }

            super.accept(descriptor);
        } else {
            super.accept(descriptor);
        }
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
    protected void accept(EjbDescriptor ejb) {
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

        for (Iterator itr = ejb.getEjbReferenceDescriptors().iterator(); itr.hasNext();) {
            EjbReference aRef = (EjbReference) itr.next();
            accept(aRef);
        }

        for (Iterator e = ejb.getPermissionedMethodsByPermission().keySet().iterator(); e.hasNext();) {
            MethodPermission nextPermission = (MethodPermission) e.next();
            Set methods = (Set) ejb.getPermissionedMethodsByPermission().get(nextPermission);
            accept(nextPermission, methods.iterator());
        }

        if (ejb.getStyledPermissionedMethodsByPermission() != null) {
            for (Iterator e = ejb.getStyledPermissionedMethodsByPermission().keySet().iterator(); e.hasNext();) {
                MethodPermission nextPermission = (MethodPermission) e.next();
                Set methods = (Set) ejb.getStyledPermissionedMethodsByPermission().get(nextPermission);
                accept(nextPermission, methods.iterator());
            }
        }

        for (Iterator e = ejb.getRoleReferences().iterator(); e.hasNext();) {
            RoleReference roleRef = (RoleReference) e.next();
            accept(roleRef);
        }

        for (Iterator e = ejb.getMethodContainerTransactions().keySet().iterator(); e.hasNext();) {
            MethodDescriptor md = (MethodDescriptor) e.next();
            ContainerTransaction ct = (ContainerTransaction) ejb.getMethodContainerTransactions().get(md);
            accept(md, ct);
        }

        for (Iterator e = ejb.getEnvironmentProperties().iterator(); e.hasNext();) {
            EnvironmentProperty envProp = (EnvironmentProperty) e.next();
            accept(envProp);
        }

        for (Iterator it = ejb.getResourceReferenceDescriptors().iterator();
             it.hasNext();) {
            ResourceReferenceDescriptor next =
                    (ResourceReferenceDescriptor) it.next();
            accept(next);
        }

        for (Iterator it = ejb.getResourceEnvReferenceDescriptors().iterator(); it.hasNext();) {
            ResourceEnvReferenceDescriptor next =
                    (ResourceEnvReferenceDescriptor) it.next();
            accept(next);
        }

        for (Iterator it = ejb.getMessageDestinationReferenceDescriptors().iterator(); it.hasNext();) {
            MessageDestinationReferencer next =
                    (MessageDestinationReferencer) it.next();
            accept(next);
        }

        // If this is a message bean, it can be a message destination
        // referencer as well.
        if (ejb.getType().equals(EjbMessageBeanDescriptor.TYPE)) {
            MessageDestinationReferencer msgDestReferencer =
                    (MessageDestinationReferencer) ejb;
            if (msgDestReferencer.getMessageDestinationLinkName() != null) {
                accept(msgDestReferencer);
            }
        }

        Set serviceRefs = ejb.getServiceReferenceDescriptors();
        for (Iterator itr = serviceRefs.iterator(); itr.hasNext();) {
            accept((ServiceReferenceDescriptor) itr.next());
        }

        if (ejb instanceof EjbCMPEntityDescriptor) {
            EjbCMPEntityDescriptor cmp = (EjbCMPEntityDescriptor)ejb;
            PersistenceDescriptor persistenceDesc = cmp.getPersistenceDescriptor();
            for (Iterator e=persistenceDesc.getCMPFields().iterator();e.hasNext();) {
                FieldDescriptor fd = (FieldDescriptor) e.next();
                accept(fd);
            }
            for (Iterator e=persistenceDesc.getQueriedMethods().iterator();e.hasNext();) {
                Object method = e.next();
                if (method instanceof MethodDescriptor) {
                    QueryDescriptor qd = persistenceDesc.getQueryFor((MethodDescriptor) method);
                    accept((MethodDescriptor) method, qd);
                }
            }
        }
    }

    /**
     * visits an ejb reference  for the last J2EE component visited
     * @param the ejb reference
     */
    protected void accept(EjbReference ejbRef) {
        DOLUtils.getDefaultLogger().info(ejbRef.toString());
    }
    
    protected void accept(MessageDestinationReferencer referencer) {
        DOLUtils.getDefaultLogger().info
            (referencer.getMessageDestinationLinkName());
    }
    
    protected void accept(WebService webService) {
        DOLUtils.getDefaultLogger().info(webService.getName());
    }

    protected void accept(ServiceReferenceDescriptor serviceRef) {
        DOLUtils.getDefaultLogger().info(serviceRef.getName());
    }

    /**
     * visits a method permission and permitted methods  for the last J2EE component visited
     * @param ejb descriptor the role is referenced from
     * @param method permission 
     * @param the methods associated with the above permission
     */
    protected void accept(MethodPermission pm, Iterator methods) {
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
    protected void accept(RoleReference roleRef) {
        DOLUtils.getDefaultLogger().info("Security Role Reference : " 
                                + roleRef.getName() + " link " + roleRef.getValue());
    }
    /**
     * visists a method transaction  for the last J2EE component visited
     * @param ejb descritptor this method applies to
     * @param method descriptor the method
     * @param container transaction
     */
    protected void accept(MethodDescriptor method, ContainerTransaction ct) {
            
        DOLUtils.getDefaultLogger().info( ct.getTransactionAttribute() 
                                + " Container Transaction for method "
                                + method.prettyPrint() );
            
    }
    
    /**
     * visists an environment property  for the last J2EE component visited
     * @paren the environment property
     */
    protected void accept(EnvironmentProperty envEntry) {
        DOLUtils.getDefaultLogger().info( envEntry.toString());    
    }

    /**
     * visits a CMP field definition (for CMP entity beans)
     * @param field descriptor for the CMP field
     */
    protected void accept(FieldDescriptor fd) {
        DOLUtils.getDefaultLogger().info("CMP Field "  +fd);
    }
    
    /**
     * visits a query method
     * @param method descriptor for the method
     * @param query descriptor
     */
    protected void accept(MethodDescriptor method, QueryDescriptor qd) {
        DOLUtils.getDefaultLogger().info(qd.toString());
    }

    /**
     * visits an ejb relationship descriptor
     * @param the relationship descriptor
     */
    protected void accept(RelationshipDescriptor descriptor) {
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

    /**
     * get the visitor for its sub descriptor
     * @param sub descriptor to return visitor for
     */
    public DescriptorVisitor getSubDescriptorVisitor(Descriptor subDescriptor) {
        if (subDescriptor instanceof BundleDescriptor) {
            DescriptorVisitor tracerVisitor = ((BundleDescriptor)subDescriptor).getTracerVisitor();
            if (tracerVisitor == null) {
                return this;
            }
            return tracerVisitor;
        }
        return super.getSubDescriptorVisitor(subDescriptor);
    }
}
