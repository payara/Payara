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

package org.glassfish.ejb.deployment.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbMessageBeanDescriptor;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.MethodPermission;
import com.sun.enterprise.deployment.ResourceEnvReferenceDescriptor;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.deployment.RoleReference;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WebService;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.types.MessageDestinationReferencer;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.util.EjbBundleVisitor;
import com.sun.enterprise.deployment.util.TracerVisitor;
import org.glassfish.ejb.deployment.descriptor.ContainerTransaction;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.FieldDescriptor;
import org.glassfish.ejb.deployment.descriptor.PersistenceDescriptor;
import org.glassfish.ejb.deployment.descriptor.QueryDescriptor;
import org.glassfish.ejb.deployment.descriptor.RelationshipDescriptor;

public class EjbBundleTracerVisitor extends TracerVisitor implements EjbBundleVisitor {

    @Override
    public void accept(BundleDescriptor descriptor) {
        if (descriptor instanceof EjbBundleDescriptorImpl) {
            EjbBundleDescriptorImpl ejbBundle = (EjbBundleDescriptorImpl) descriptor;
            accept(ejbBundle);

            for (EjbDescriptor anEjb : ejbBundle.getEjbs()) {
                anEjb.visit(getSubDescriptorVisitor(anEjb));
            }
            if (ejbBundle.hasRelationships()) {
                for (RelationshipDescriptor rd : ejbBundle.getRelationships()) {
                    accept(rd);
                }
            }
            for (WebService ws : ejbBundle.getWebServices().getWebServices()) {
                accept(ws);
            }
        }
        super.accept(descriptor);
    }

    @Override
    public void accept(com.sun.enterprise.deployment.EjbBundleDescriptor ebd) {
        logInfo("Ejb Bundle " + ebd.getName());
    }

    protected void accept(EjbDescriptor ejb) {
        logInfo("==================");
        logInfo(ejb.getType() + " Bean " + ejb.getName());
        logInfo("\thomeClassName " + ejb.getHomeClassName());
        logInfo("\tremoteClassName " + ejb.getRemoteClassName());
        logInfo("\tlocalhomeClassName " + ejb.getLocalHomeClassName());
        logInfo("\tlocalClassName " + ejb.getLocalClassName());
        logInfo("\tremoteBusinessIntfs " + ejb.getRemoteBusinessClassNames());
        logInfo("\tlocalBusinessIntfs " + ejb.getLocalBusinessClassNames());

        logInfo("\tjndiName " + ejb.getJndiName());
        logInfo("\tejbClassName " + ejb.getEjbClassName());
        logInfo("\ttransactionType " + ejb.getTransactionType());
        if (ejb.getUsesCallerIdentity() == false) {
            logInfo("\trun-as role " + ejb.getRunAsIdentity());
        } else {
            logInfo("\tuse-caller-identity " + ejb.getUsesCallerIdentity());
        }

        for (EjbReference aRef : ejb.getEjbReferenceDescriptors()) {
            accept(aRef);
        }

        for (Iterator e = ejb.getPermissionedMethodsByPermission().keySet().iterator(); e.hasNext();) {
            MethodPermission mp = (MethodPermission) e.next();
            Set methods = (Set) ejb.getPermissionedMethodsByPermission().get(mp);
            accept(mp, methods);
        }

        if (ejb.getStyledPermissionedMethodsByPermission() != null) {
            for (Iterator e = ejb.getStyledPermissionedMethodsByPermission().keySet().iterator(); e.hasNext();) {
                MethodPermission mp = (MethodPermission) e.next();
                Set methods = (Set) ejb.getStyledPermissionedMethodsByPermission().get(mp);
                accept(mp, methods);
            }
        }

        for (RoleReference roleRef : ejb.getRoleReferences()) {
            accept(roleRef);
        }

        for (Iterator e = ejb.getMethodContainerTransactions().keySet().iterator(); e.hasNext();) {
            MethodDescriptor md = (MethodDescriptor) e.next();
            ContainerTransaction ct = (ContainerTransaction) ejb.getMethodContainerTransactions().get(md);
            accept(md, ct);
        }

        for (EnvironmentProperty envProp : ejb.getEnvironmentProperties()) {
            accept(envProp);
        }

        for (ResourceReferenceDescriptor next : ejb.getResourceReferenceDescriptors()) {
            accept(next);
        }

        for (ResourceEnvReferenceDescriptor next : ejb.getResourceEnvReferenceDescriptors()) {
            accept(next);
        }

        for (MessageDestinationReferencer next : ejb.getMessageDestinationReferenceDescriptors()) {
            accept(next);
        }

        // If this is a message bean, it can be a message destination
        // referencer as well.
        if (ejb.getType().equals(EjbMessageBeanDescriptor.TYPE)) {
            MessageDestinationReferencer msgDestReferencer = (MessageDestinationReferencer) ejb;
            if (msgDestReferencer.getMessageDestinationLinkName() != null) {
                accept(msgDestReferencer);
            }
        }

        for (ServiceReferenceDescriptor sref : ejb.getServiceReferenceDescriptors()) {
            accept(sref);
        }

        if (ejb instanceof EjbCMPEntityDescriptor) {
            EjbCMPEntityDescriptor cmp = (EjbCMPEntityDescriptor) ejb;
            PersistenceDescriptor persistenceDesc = cmp.getPersistenceDescriptor();
            for (Object fd : persistenceDesc.getCMPFields()) {
                accept((FieldDescriptor) fd);
            }
            for (Object o : persistenceDesc.getQueriedMethods()) {
                if (o instanceof MethodDescriptor) {
                    QueryDescriptor qd = persistenceDesc.getQueryFor((MethodDescriptor) o);
                    accept(qd);
                }
            }
        }
    }

    protected void accept(MethodPermission pm, Collection<MethodDescriptor> mds) {
        logInfo("For method permission : " + pm.toString());
        for (MethodDescriptor md : mds) {
            logInfo("\t" + md.prettyPrint());
        }
    }

    protected void accept(RoleReference rr) {
        logInfo("Security Role Reference : " + rr.getName() + " link " + rr.getValue());
    }

    protected void accept(MethodDescriptor md, ContainerTransaction ct) {
        logInfo(ct.getTransactionAttribute()
                + " Container Transaction for method " + md.prettyPrint());
    }

    protected void accept(FieldDescriptor fd) {
        logInfo("CMP Field " + fd);
    }

    protected void accept(QueryDescriptor qd) {
        logInfo(qd.toString());
    }

    protected void accept(RelationshipDescriptor rd) {
        logInfo("============ Relationships ===========");
        logInfo("From EJB " + rd.getSource().getName() + " cmr field : "
                + rd.getSource().getCMRField() + "("
                + rd.getSource().getCMRFieldType() + ")  to EJB "
                + rd.getSink().getName() + " isMany "
                + rd.getSource().getIsMany() + " cascade-delete "
                + rd.getSource().getCascadeDelete());

        logInfo("To  EJB " + rd.getSink().getName() + " isMany "
                + rd.getSink().getIsMany() + " cascade-delete "
                + rd.getSink().getCascadeDelete());

        if (rd.getIsBidirectional()) {
            logInfo("Bidirectional cmr field : " + rd.getSink().getCMRField()
                    + "(" + rd.getSink().getCMRFieldType() + ")");
        }
    }

    private void logInfo(String message) {
        DOLUtils.getDefaultLogger().info(message);
    }

}
