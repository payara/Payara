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

package org.glassfish.ejb.deployment.node;

import java.util.*;

import com.sun.enterprise.deployment.EjbInterceptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.MethodPermission;
import com.sun.enterprise.deployment.MethodPermissionDescriptor;
import com.sun.enterprise.deployment.node.*;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.TagNames;
import org.glassfish.ejb.deployment.EjbTagNames;
import org.glassfish.ejb.deployment.descriptor.*;
import org.glassfish.ejb.deployment.node.runtime.EjbBundleRuntimeNode;
import org.glassfish.ejb.deployment.node.runtime.GFEjbBundleRuntimeNode;
import org.glassfish.security.common.Role;
import org.jvnet.hk2.annotations.Service;
import org.w3c.dom.Node;

/**
 * This class handles ejb bundle xml files
 *
 * @author  Jerome Dochez
 * @version
 */
@Service
public class EjbBundleNode extends AbstractBundleNode<EjbBundleDescriptorImpl> {

    public final static XMLElement tag = new XMLElement(EjbTagNames.EJB_BUNDLE_TAG);
    public final static String PUBLIC_DTD_ID = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN";
    public final static String PUBLIC_DTD_ID_12 = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";
    
    /** The system ID of an ejb-jar document.*/
    public final static String SYSTEM_ID = "http://java.sun.com/dtd/ejb-jar_2_0.dtd";
    public final static String SYSTEM_ID_12 = "http://java.sun.com/dtd/ejb-jar_1_1.dtd";
    public final static String SCHEMA_ID_21 = "ejb-jar_2_1.xsd";
    public final static String SCHEMA_ID_30 = "ejb-jar_3_0.xsd";
    public final static String SCHEMA_ID_31 = "ejb-jar_3_1.xsd";
    public final static String SCHEMA_ID = "ejb-jar_3_2.xsd";
    public final static String SPEC_VERSION = "3.2";
    private final static List<String> systemIDs = initSystemIDs();

   /**
    * register this node as a root node capable of loading entire DD files
    * 
    * @param publicIDToDTD is a mapping between xml Public-ID to DTD 
    * @return the doctype tag name
    */
   @Override
   public String registerBundle(Map publicIDToDTD) {
        publicIDToDTD.put(PUBLIC_DTD_ID, SYSTEM_ID);
        publicIDToDTD.put(PUBLIC_DTD_ID_12, SYSTEM_ID_12);
        return tag.getQName();
   }

    @Override
    public Map<String,Class> registerRuntimeBundle(final Map<String,String> publicIDToDTD, Map<String, List<Class>> versionUpgrades) {
        final Map<String,Class> result = new HashMap<String,Class>();
        result.put(EjbBundleRuntimeNode.registerBundle(publicIDToDTD), EjbBundleRuntimeNode.class);
        result.put(GFEjbBundleRuntimeNode.registerBundle(publicIDToDTD), GFEjbBundleRuntimeNode.class);
        return result;
    }

    private static List<String> initSystemIDs() {
        ArrayList<String> systemIDs = new ArrayList<String>(3);
        systemIDs.add(SCHEMA_ID);
        systemIDs.add(SCHEMA_ID_31);
        systemIDs.add(SCHEMA_ID_30);
        systemIDs.add(SCHEMA_ID_21);
        return Collections.unmodifiableList(systemIDs);
   }
   
    // Descriptor class we are using   
   private EjbBundleDescriptorImpl descriptor;
      
   public EjbBundleNode() {
       super();
       // register sub XMLNodes
       registerElementHandler(new XMLElement(EjbTagNames.SESSION),
                                                            EjbSessionNode.class);           
       registerElementHandler(new XMLElement(EjbTagNames.ENTITY), 
                                                            EjbEntityNode.class);                   
       registerElementHandler(new XMLElement(EjbTagNames.MESSAGE_DRIVEN), 
                                                            MessageDrivenBeanNode.class);          
       registerElementHandler(new XMLElement(EjbTagNames.METHOD_PERMISSION), 
                                                            MethodPermissionNode.class);                  
       registerElementHandler(new XMLElement(TagNames.ROLE),
                                                            SecurityRoleNode.class, "addRole");       
       registerElementHandler(new XMLElement(EjbTagNames.CONTAINER_TRANSACTION), 
                                                            ContainerTransactionNode.class);       
       registerElementHandler(new XMLElement(EjbTagNames.EXCLUDE_LIST), 
                                                            ExcludeListNode.class);                     
       registerElementHandler(new XMLElement(EjbTagNames.RELATIONSHIPS), 
                                                            RelationshipsNode.class);                     
       registerElementHandler(new XMLElement(TagNames.MESSAGE_DESTINATION),
                                             MessageDestinationNode.class,
                                             "addMessageDestination");
       registerElementHandler(new XMLElement(EjbTagNames.APPLICATION_EXCEPTION),
                                             EjbApplicationExceptionNode.class,
                                             "addApplicationException");
       registerElementHandler(new XMLElement(EjbTagNames.INTERCEPTOR),
                              EjbInterceptorNode.class,
                              "addInterceptor");

       registerElementHandler(new XMLElement(EjbTagNames.INTERCEPTOR_BINDING),
                              InterceptorBindingNode.class,
                              "appendInterceptorBinding");

       SaxParserHandler.registerBundleNode(this, EjbTagNames.EJB_BUNDLE_TAG);
   }

   @Override
   public void addDescriptor(Object newDescriptor) {       
       if (newDescriptor instanceof EjbDescriptor) {
           descriptor.addEjb((EjbDescriptor) newDescriptor);
       } else if (newDescriptor instanceof RelationshipDescriptor) {
           descriptor.addRelationship((RelationshipDescriptor) newDescriptor);
       } else if (newDescriptor instanceof MethodPermissionDescriptor) {
           MethodPermissionDescriptor   nd = (MethodPermissionDescriptor) newDescriptor;
           MethodDescriptor[] array = nd.getMethods();
           for (int i=0;i<array.length;i++) {
                EjbDescriptor target  = descriptor.getEjbByName(array[i].getEjbName());           
                MethodPermission[] mps = nd.getMethodPermissions();
                for (int j=0;j<mps.length;j++) {
                    DOLUtils.getDefaultLogger().fine("Adding mp " + mps[j] + " to " + array[i] + " for ejb " + array[i].getEjbName());
                    target.addPermissionedMethod(mps[j], array[i]);
                }
            }
       } else super.addDescriptor(newDescriptor);       
   }

    @Override
    public void setElementValue(XMLElement element, String value) {

        if (TagNames.MODULE_NAME.equals(element.getQName())) {
            EjbBundleDescriptorImpl bundleDesc = getDescriptor();
            // ejb-jar.xml <module-name> only applies if this is an ejb-jar
            if( bundleDesc.getModuleDescriptor().getDescriptor() instanceof EjbBundleDescriptorImpl) {
                bundleDesc.getModuleDescriptor().setModuleName(value);
            }        
        } else {
            super.setElementValue(element, value);
        }
    }

    @Override
    public EjbBundleDescriptorImpl getDescriptor() {
        if (descriptor==null) descriptor = new EjbBundleDescriptorImpl();
        return descriptor;
    }

    @Override
    protected XMLElement getXMLRootTag() {
        return tag;
    }

    @Override
    protected Map getDispatchTable() {
        // no need to be synchronized for now
        Map table = super.getDispatchTable();
        table.put(EjbTagNames.EJB_CLIENT_JAR, "setEjbClientJarUri");
        return table;
    }

    @Override
    public Node writeDescriptor(Node parent, EjbBundleDescriptorImpl ejbDesc) {
        Node jarNode = super.writeDescriptor(parent, ejbDesc);
        Node entrepriseBeansNode = appendChild(jarNode, EjbTagNames.EJBS);
        for (EjbDescriptor ejb : ejbDesc.getEjbs()) {
            if (EjbSessionDescriptor.TYPE.equals(ejb.getType())) {
                EjbSessionNode subNode = new EjbSessionNode();
                subNode.writeDescriptor(entrepriseBeansNode, EjbTagNames.SESSION, (EjbSessionDescriptor) ejb);
            }  else if (EjbEntityDescriptor.TYPE.equals(ejb.getType())) {
                EjbEntityNode subNode = new EjbEntityNode();
                subNode.writeDescriptor(entrepriseBeansNode, EjbTagNames.ENTITY, (EjbEntityDescriptor) ejb);
            } else if (EjbMessageBeanDescriptor.TYPE.equals(ejb.getType())) {
                MessageDrivenBeanNode subNode = new MessageDrivenBeanNode();
                subNode.writeDescriptor(entrepriseBeansNode, EjbTagNames.MESSAGE_DRIVEN, (EjbMessageBeanDescriptor) ejb);
            }  else {
                throw new IllegalStateException("Unknow ejb type " + ejb.getType());
            }
        }

        if( ejbDesc.hasInterceptors() ) {
            Node interceptorsNode = appendChild(jarNode, 
                                                EjbTagNames.INTERCEPTORS);
            EjbInterceptorNode interceptorNode = new EjbInterceptorNode();
            for(EjbInterceptor next : ejbDesc.getInterceptors()) {
                interceptorNode.writeDescriptor( interceptorsNode,
                                                 EjbTagNames.INTERCEPTOR, next);
            }
        }

        // relationships*
        if (ejbDesc.hasRelationships()) {
            (new RelationshipsNode()).writeDescriptor(jarNode, EjbTagNames.RELATIONSHIPS, ejbDesc);
        }
        
        // assembly-descriptor
        writeAssemblyDescriptor(jarNode, ejbDesc);
        
        appendTextChild(jarNode, EjbTagNames.EJB_CLIENT_JAR, ejbDesc.getEjbClientJarUri());        
        return jarNode;
    }

    @Override
    public String getDocType() {
        return null;
    }

    @Override
    public String getSystemID() {
        return SCHEMA_ID;
    }

    @Override
    public List<String> getSystemIDs() {
        return systemIDs;
    }

    /**
     * write assembly-descriptor related xml information to the DOM tree
     */
    private void writeAssemblyDescriptor(Node parentNode, EjbBundleDescriptorImpl bundleDescriptor) {
       Node assemblyNode = parentNode.getOwnerDocument().createElement(EjbTagNames.ASSEMBLY_DESCRIPTOR);
       
       // security-role*
       SecurityRoleNode roleNode = new SecurityRoleNode();
       for (Iterator e = bundleDescriptor.getRoles().iterator();e.hasNext();) {
           roleNode.writeDescriptor(assemblyNode, TagNames.ROLE, (Role) e.next());
       }
       
       // method-permission*       
       Map excludedMethodsByEjb = new HashMap();
       MethodPermissionNode mpNode = new MethodPermissionNode();       
       for (EjbDescriptor ejbDesc : bundleDescriptor.getEjbs()) {
           if (ejbDesc instanceof EjbMessageBeanDescriptor)                
               continue;
           Vector excludedMethods = new Vector();
           addMethodPermissions(ejbDesc, ejbDesc.getPermissionedMethodsByPermission(), excludedMethods,  mpNode, assemblyNode);
           addMethodPermissions(ejbDesc, ejbDesc.getStyledPermissionedMethodsByPermission(), excludedMethods, mpNode, assemblyNode);
           if (excludedMethods.size()>0) {
               excludedMethodsByEjb.put(ejbDesc, excludedMethods);
           }
       }

       // container-transaction*
       ContainerTransactionNode ctNode = new ContainerTransactionNode();
       for (EjbDescriptor ejbDesc : bundleDescriptor.getEjbs()) {
           ctNode.writeDescriptor(assemblyNode, EjbTagNames.CONTAINER_TRANSACTION, ejbDesc);
       }

       // interceptor-binding*
       InterceptorBindingNode ibNode = new InterceptorBindingNode();
        for(EjbDescriptor ejbDesc : bundleDescriptor.getEjbs()) {
            if (!ejbDesc.getInterceptorClasses().isEmpty()) {
                ibNode.writeBindings(assemblyNode, ejbDesc);
            }
        }

       // message-destination*
       writeMessageDestinations
           (assemblyNode, bundleDescriptor.getMessageDestinations().iterator());
                                
       // exclude-list*              
       if (excludedMethodsByEjb.size()>0) {
           Node excludeListNode = this.appendChild(assemblyNode, EjbTagNames.EXCLUDE_LIST);
           for (Object o : excludedMethodsByEjb.entrySet()) {
               Map.Entry entry = (Map.Entry) o;
               EjbDescriptor ejbDesc = (EjbDescriptor) entry.getKey();
               Vector excludedMethods = (Vector) entry.getValue();
               
               MethodPermissionDescriptor mpd = new MethodPermissionDescriptor();
               mpd.addMethodPermission(MethodPermission.getExcludedMethodPermission());
               mpd.addMethods(excludedMethods);
               mpNode.writeDescriptorInNode(excludeListNode, mpd, ejbDesc);
           }
       }
       
       for(EjbApplicationExceptionInfo next : 
               bundleDescriptor.getApplicationExceptions().values()) {

           EjbApplicationExceptionNode node = new EjbApplicationExceptionNode();
           
           node.writeDescriptor(assemblyNode, EjbTagNames.APPLICATION_EXCEPTION,
                                next);

       }

       if (assemblyNode.hasChildNodes()) {
           parentNode.appendChild(assemblyNode);
       }
    }
    
    private void addMethodPermissions(
            EjbDescriptor ejb, 
            Map mpToMethods,  
            Vector  excludedMethods,
            MethodPermissionNode mpNode, 
            Node assemblyNode) {
                
        if (mpToMethods==null || mpToMethods.size()==0) {
            return;
        }
        
        for (Object o : mpToMethods.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            MethodPermission mp = (MethodPermission) entry.getKey();
            if (mp.isExcluded()) {
                // we need to be sure the method descriptors knows who owns them
                Set methods = (Set) entry.getValue();
                excludedMethods.addAll(methods);
            } else {
                MethodPermissionDescriptor mpd = new MethodPermissionDescriptor();
                mpd.addMethodPermission(mp);
                mpd.addMethods((Set) mpToMethods.get(mp));
                mpNode.writeDescriptor(assemblyNode, EjbTagNames.METHOD_PERMISSION, mpd, ejb);
            }
        }
    }

    @Override
    public String getSpecVersion() {
        return SPEC_VERSION;
    }
    
}
