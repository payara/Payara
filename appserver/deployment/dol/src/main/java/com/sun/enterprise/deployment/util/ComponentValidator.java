/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

/*
 * ComponentValidator.java
 *
 * Created on August 15, 2002, 5:51 PM
 */

package com.sun.enterprise.deployment.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import javax.security.auth.Subject;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.InjectionCapable;
import com.sun.enterprise.deployment.InjectionTarget;
import com.sun.enterprise.deployment.ManagedBeanDescriptor;
import com.sun.enterprise.deployment.MessageDestinationDescriptor;
import com.sun.enterprise.deployment.MessageDestinationReferenceDescriptor;
import com.sun.enterprise.deployment.ResourceEnvReferenceDescriptor;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.deployment.RunAsIdentityDescriptor;
import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.types.MessageDestinationReferencer;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.deployment.archive.ArchiveType;

import org.glassfish.logging.annotation.LogMessageInfo;

/**
 *
 * @author  dochez
 */
public class ComponentValidator extends DefaultDOLVisitor implements ComponentVisitor {

    public static final Logger deplLogger = com.sun.enterprise.deployment.util.DOLUtils.deplLogger;

    @LogMessageInfo(message = "Could not load {0}", level="FINE")
    private static final String LOAD_ERROR = "AS-DEPLOYMENT-00014";

    private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(ComponentValidator.class);

    protected BundleDescriptor bundleDescriptor = null;

    protected Application application;

    /**
     * @return the Application object if any
     */
    protected Application getApplication() {
        return application;
    }

    /**
     * @return the bundleDescriptor we are visiting
     */
    protected BundleDescriptor getBundleDescriptor() {
        return bundleDescriptor;
    }

    @Override
    public void accept(BundleDescriptor bundleDescriptor) {
        this.bundleDescriptor = bundleDescriptor;

        super.accept(bundleDescriptor);
    }

    /**
     * Visits a message destination referencer for the last J2EE 
     * component visited
     * @param the message destination referencer
     */
    @Override
    protected void accept(MessageDestinationReferencer msgDestReferencer) {

        // if it is linked to a logical destination
        if( msgDestReferencer.isLinkedToMessageDestination() ) {
            return;
        // if it is referred to a physical destination
        } else if (msgDestReferencer.ownedByMessageDestinationRef() && 
            msgDestReferencer.getMessageDestinationRefOwner(
            ).getJndiName() != null) {
            return;
        } else {
            MessageDestinationDescriptor msgDest = 
                msgDestReferencer.resolveLinkName();
            if( msgDest == null ) {
                String linkName = 
                    msgDestReferencer.getMessageDestinationLinkName();
                DOLUtils.getDefaultLogger().log(Level.WARNING, DOLUtils.INVALID_DESC_MAPPING,
                    new Object[] {"message-destination", linkName});
            } else {
                if (msgDestReferencer instanceof MessageDestinationReferenceDescriptor) {
                    ((MessageDestinationReferenceDescriptor)msgDestReferencer).setJndiName(msgDest.getJndiName());
                }
            }                                                 
        }
    }

    /**
     * @param intfName
     * @return default jndi name for a given interface name
     */
    //XXX this is first implementation. It does not handle two ejb with same
    //    interface in different jar
    protected String getDefaultEjbJndiName(String intfName) {
        return intfName;
    }

    private enum EjbIntfType {
        NONE,
        REMOTE_HOME,
        REMOTE_BUSINESS,
        LOCAL_HOME,
        LOCAL_BUSINESS,
        NO_INTF_LOCAL_BUSINESS
    }

    private static class EjbIntfInfo {

        Set<EjbDescriptor> ejbs;

        // Only set when there is one ejb in the set. 
        // Otherwise, value = NONE
        EjbIntfType intfType;
    }


    @Override
    protected void accept(EjbReference ejbRef) {

        DOLUtils.getDefaultLogger().fine("Visiting Ref" + ejbRef);
    if (ejbRef.getEjbDescriptor()!=null) 
            return;

        // let's try to derive the ejb-ref-type first it is not defined
        if (ejbRef.getType() == null) {
            // if it's EJB30 (no home/local home), it must be session
            if (ejbRef.isEJB30ClientView()) {
                ejbRef.setType("Session");
            } else {
                // if home interface has findByPrimaryKey method, 
                // it's entity, otherwise it's session
                String homeIntf = ejbRef.getEjbHomeInterface();
                BundleDescriptor referringJar = ejbRef.getReferringBundleDescriptor();
                if (referringJar == null) {
                    referringJar = getBundleDescriptor();
                }           
                ClassLoader classLoader = referringJar.getClassLoader();

                Class<?> clazz = null;
                try {
                    clazz = classLoader.loadClass(homeIntf);
                    for (Method method : clazz.getDeclaredMethods()) {
                        if (method.getName().equals("findByPrimaryKey")) {
                            ejbRef.setType("Entity");
                            break;
                        }
                    }
                    if (ejbRef.getType() == null) {
                        ejbRef.setType("Session");
                    }
                } catch(Exception e) {
                  LogRecord lr = new LogRecord(Level.FINE, LOAD_ERROR);
                  Object args[] = { homeIntf };
                  lr.setParameters(args);
                  lr.setThrown(e);
                  deplLogger.log(lr);
                }
            }
        }
  
        //
        // NOTE : In the 3.0 local/remote business view, the local vs.
        // remote designation is not always detectable from the interface 
        // itself.
        //
        // That means 
        // 
        // 1) we need to figure it out during this stage of the processing
        // 2) the EjbReferenceDescriptor.isLocal() operations shouldn't be
        //    be used before the post-application validation stage since its
        //    value would be unreliable.
        // 3) We can't write out the standard deployment descriptors to XML
        //    until the full application has been processed, including this
        //    validation stage.
        //
        // During @EJB processing, setLocal() is set to false if 
        // local vs. remote is ambiguous.  setLocal() is set to true within this 
        // method upon successfuly resolution to a local business interface.
        //

        if (ejbRef.getJndiName()!=null && ejbRef.getJndiName().length()!=0) {


            // ok this is getting a little complicated here
            // the jndi name is not null, if this is a remote ref, proceed with resolution
            // if this is a local ref, proceed with resolution only if ejb-link is null            
            if (!ejbRef.isLocal() || (ejbRef.isLocal() && ejbRef.getLinkName()==null)) {
                DOLUtils.getDefaultLogger().fine("Ref " + ejbRef.getName() + " is bound to Ejb with JNDI Name " + ejbRef.getJndiName());
                if (getEjbDescriptors() != null) {
                    for (Iterator iter = getEjbDescriptors().iterator(); iter.hasNext(); ) {
                        EjbDescriptor ejb = (EjbDescriptor) iter.next();
                        if (ejbRef.getJndiName().equals(ejb.getJndiName())) {
                            ejbRef.setEjbDescriptor(ejb);
                            return;
                        } 
                    }
                }
            }
        }

        // If the reference does not have an ejb-link or jndi-name or lookup string associated
        // with it, attempt to resolve it by checking against all the ejbs
        // within the application.  If no match is found, just fall through
        // and let the existing error-checking logic kick in.
        if (( (ejbRef.getJndiName() == null) || 
              (ejbRef.getJndiName().length() == 0) )
            &&
            ( (ejbRef.getLinkName() == null) ||
              (ejbRef.getLinkName().length() == 0) )
            && !ejbRef.hasLookupName() ) {

            Map<String, EjbIntfInfo> ejbIntfInfoMap = getEjbIntfMap();
            if ( ejbIntfInfoMap.size() > 0 ) {

                String interfaceToMatch = ejbRef.isEJB30ClientView() ?
                    ejbRef.getEjbInterface() : ejbRef.getEjbHomeInterface();
                if ( interfaceToMatch == null ) {
                  String msg = localStrings.getLocalString(
                          "enterprise.deployment.util.no_remoteinterface",
                          "Cannot resolve reference {0} because it does not declare a remote interface or " +
                                  "remote home interface of target bean",
                          new Object[]{ejbRef});
                  throw new IllegalArgumentException(msg);
                }
                EjbIntfInfo intfInfo = ejbIntfInfoMap.get(interfaceToMatch);

                // make sure exactly one match
                if ( intfInfo != null ) {
                    int numMatches = intfInfo.ejbs.size();
                    if( numMatches == 1 ) {
                        EjbDescriptor target = intfInfo.ejbs.iterator().next();

                        BundleDescriptor targetModule = 
                            target.getEjbBundleDescriptor();
                        BundleDescriptor sourceModule =
                            ejbRef.getReferringBundleDescriptor();

                        Application app = targetModule.getApplication();

                        //
                        // It's much cleaner to derive the ejb-link value
                        // and set that instead of the descriptor.  This way,
                        // if there are multiple ejb-jars within the .ear that
                        // each have an ejb with the target bean's ejb-name,
                        // there won't be any ambiguity about which one is
                        // the correct target.  It's not so much a problem
                        // during this phase of the processing, but if the
                        // fully-qualified ejb-link name is required and is not
                        // written out, there could be non-deterministic
                        // behavior when the application is re-loaded.
                        // Let the ejb-link processing logic handle the 
                        // conversion to ejb descriptor.
                        //

                        // If the ejb reference and the target ejb are defined
                        // within the same ejb-jar, the ejb-link will only
                        // be set to ejb-name.  This is done regardless of
                        // whether the ejb-jar is within an .ear or is
                        // stand-alone.  The ejb-link processing
                        // logic will always check the current ejb-jar
                        // first so there won't be any ambiguity.  
                        String ejbLinkName = target.getName();
                        if (!sourceModule.isPackagedAsSingleModule(targetModule)) {
                            String relativeUri = null;
                            if( sourceModule == app ) {
                                // Now that dependencies can be defined within application.xml
                                // it's possible for source module to be the Application object.
                                // In this case, just use the target module uri as the relative
                                // uri.
                                relativeUri = targetModule.getModuleDescriptor().getArchiveUri();
                            } else {
                                // Since there are at least two modules, we
                                // must be within an application.
                                relativeUri = getApplication().getRelativeUri(sourceModule, targetModule);
                            }
                            ejbLinkName = relativeUri + "#" + ejbLinkName;
                        }

                        ejbRef.setLinkName(ejbLinkName);

                    } else {
                        String msg = localStrings.getLocalString(
                                "enterprise.deployment.util.multiple_ejbs_with_interface",
                                "Cannot resolve reference {0} because there are {1} ejbs in the application with interface {2}.",
                                new Object[] {ejbRef, numMatches, interfaceToMatch});
                        throw new IllegalArgumentException(msg);
                    }
                }                          
            } 
        }

        // now all cases fall back here, we need to resolve through the link-name    
        if (ejbRef.getLinkName()==null) {


            // if no link name if present, and this is a local ref, this is an
            // error (unless there is a lookup string) because we must resolve all
            // local refs within the app and we cannot resolve it 
            if (ejbRef.isLocal()) {
                if( ejbRef.hasLookupName() ) {
                    return;
                }
                DOLUtils.getDefaultLogger().severe("Cannot resolve reference " + ejbRef);
                throw new RuntimeException("Cannot resolve reference " + ejbRef);
            } else {
                // this is a remote interface, jndi will eventually contain the referenced 
                // ejb ref, apply default jndi name if there is none
                if (!ejbRef.hasJndiName() && !ejbRef.hasLookupName()) {
                    String jndiName = getDefaultEjbJndiName(
                        ejbRef.isEJB30ClientView() ?
                        ejbRef.getEjbInterface() : ejbRef.getEjbHomeInterface());
                    ejbRef.setJndiName(jndiName);
                    DOLUtils.getDefaultLogger().fine("Applying default to ejb reference: " + ejbRef);
                }

                return;
            }                          
        }        
        
        // Beginning of ejb-link resolution
        
        // save anticipated types for checking if interfaces are compatible
        String homeClassName = ejbRef.getEjbHomeInterface();
        String intfClassName = ejbRef.getEjbInterface();

        // save anticipated type for checking if bean type is compatible
        String type = ejbRef.getType();
        
        EjbDescriptor ejbReferee=null;
            
        String linkName = ejbRef.getLinkName();
        int ind = linkName.lastIndexOf('#');
        if ( ind != -1 ) {
            // link has a relative path from referring EJB JAR,
            // of form "../products/product.jar#ProductEJB"
            String ejbName = linkName.substring(ind+1);
            String jarPath = linkName.substring(0, ind);            
            BundleDescriptor referringJar = ejbRef.getReferringBundleDescriptor();
            if (referringJar==null) {
                ejbRef.setReferringBundleDescriptor(getBundleDescriptor());
                referringJar = getBundleDescriptor();
            }           
            
            if (getApplication()!=null) {
                BundleDescriptor refereeJar = null;
                if( referringJar instanceof Application ) {
                    refereeJar = ((Application)referringJar).getModuleByUri(jarPath);
                } else {
                 refereeJar = 
                    getApplication().getRelativeBundle(referringJar, jarPath);
                }
                if( (refereeJar != null) &&
                    refereeJar instanceof EjbBundleDescriptor ) {
                    // this will throw an exception if ejb is not found
                    ejbReferee = 
                       ((EjbBundleDescriptor)refereeJar).getEjbByName(ejbName);
                }
            }
        }
        else {

            // Handle an unqualified ejb-link, which is just an ejb-name.

            // If we're in an application and currently processing an
            // ejb-reference defined within an ejb-jar, first check
            // the current ejb-jar for an ejb-name match.  From a spec
            // perspective, the deployer can't depend on this behavior,
            // but it's still better to have deterministic results.  In
            // addition, in the case of automatic-linking, the fully-qualified
            // "#" ejb-link syntax is not used when the ejb reference and
            // target ejb are within the same ejb-jar.  Checking the
            // ejb-jar first will ensure the correct linking behavior for
            // that case.
            Application app = getApplication();
            EjbBundleDescriptor ebd = getEjbBundleDescriptor();
            if (app != null && ebd != null && ebd.hasEjbByName(linkName)) {
                ejbReferee = ebd.getEjbByName(linkName);
            } else if (app != null && app.hasEjbByName(linkName)) {
                ejbReferee = app.getEjbByName(ejbRef.getLinkName());
            } else if (getEjbDescriptor()!=null) {
                try {
                    ejbReferee = getEjbDescriptor().getEjbBundleDescriptor().getEjbByName(ejbRef.getLinkName());
                } catch (IllegalArgumentException e) {
                    // this may happen when we have no application and the ejb ref
                    // cannot be resolved to a ejb in the bundle. The ref will
                    // probably be resolved when the application is assembled.
                    DOLUtils.getDefaultLogger().warning("Unresolved <ejb-link>: "+linkName);
                    return;
                }
                    
            }
        }

        if (ejbReferee==null) {
            // we could not resolve through the ejb-link. if this is a local ref, this 
            // is an error, if this is a remote ref, this should be also an error at 
            // runtime but maybe the jndi name will be specified by deployer so 
            // a warning should suffice
            if (ejbRef.isLocal()) {
                DOLUtils.getDefaultLogger().severe("Unresolved <ejb-link>: "+linkName);
                throw new RuntimeException("Error: Unresolved <ejb-link>: "+linkName);
            } else {
                final ArchiveType moduleType = ejbRef.getReferringBundleDescriptor().getModuleType();
                if(moduleType != null && moduleType.equals(DOLUtils.carType())) {
                    // Because no annotation processing is done within ACC runtime, this case typically
                    // arises for remote @EJB annotations, so don't log it as warning.
                    DOLUtils.getDefaultLogger().fine("Unresolved <ejb-link>: "+linkName);
                } else {
                    DOLUtils.getDefaultLogger().warning("Unresolved <ejb-link>: "+linkName);
                }
                return;
            }
        } else {

            if( ejbRef.isEJB30ClientView() ) {

                BundleDescriptor referringBundle = 
                    ejbRef.getReferringBundleDescriptor();

                // If we can verify that the current ejb 3.0 reference is defined
                // in any Application Client module or in a stand-alone web module
                // it must be remote business.
                if( ( (referringBundle == null) && (getEjbBundleDescriptor() == null) )
                    ||
                    ((referringBundle != null) && (referringBundle.getModuleType() == DOLUtils.carType()))
                    ||
                    ( (getApplication() == null) &&
                      (referringBundle.getModuleType() != null && referringBundle.getModuleType().equals(DOLUtils.warType())) ) ) {

                    ejbRef.setLocal(false);

                    // Double-check that target has a remote business interface of this
                    // type.  This will handle the common error case that the target 
                    // EJB has intended to support a remote business interface but
                    // has not used @Remote to specify it, in which case
                    // the interface was assigned the default of local business.

                    if( !ejbReferee.getRemoteBusinessClassNames().contains
                        (intfClassName) ) {
                        String msg = "Target ejb " + ejbReferee.getName() + " for " +
                            " remote ejb 3.0 reference " + ejbRef.getName() + 
                            " does not expose a remote business interface of type " +
                            intfClassName;
                        throw new RuntimeException(msg);
                    }

                } else if(ejbReferee.getLocalBusinessClassNames().contains(intfClassName)) {
                    ejbRef.setLocal(true);
                } else if(ejbReferee.getRemoteBusinessClassNames().contains(intfClassName)) {
                    ejbRef.setLocal(false);
                } else {
                    if (ejbReferee.isLocalBean()) {
                        ejbRef.setLocal(true);
                    } else {
                        String msg = "Warning : Unable to determine local " +
                            " business vs. remote business designation for " +
                            " EJB 3.0 ref " + ejbRef;
                        throw new RuntimeException(msg);
                    }
                }
            }

            ejbRef.setEjbDescriptor(ejbReferee);
        }

        // if we are here, we must have resolved the reference
        if(DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
            if (getEjbDescriptor() != null){
                DOLUtils.getDefaultLogger().fine("Done Visiting " + getEjbDescriptor().getName() + " reference " + ejbRef);
            }
        }

        // check that declared types are compatible with expected values
        // if there is a target ejb descriptor available
            if( ejbRef.isEJB30ClientView() ) {

                Set<String> targetBusinessIntfs = ejbRef.isLocal() ?
                    ejbReferee.getLocalBusinessClassNames() :
                    ejbReferee.getRemoteBusinessClassNames();

                EjbDescriptor ejbDesc = ejbRef.getEjbDescriptor();

                // If it's neither a business interface nor a no-interface view
                if( !targetBusinessIntfs.contains(intfClassName) &&
                    (   ejbDesc.isLocalBean() &&
                        !(intfClassName.equals(ejbReferee.getEjbClassName()))) ) {


                    DOLUtils.getDefaultLogger().log(Level.WARNING,
                           "enterprise.deployment.backend.ejbRefTypeMismatch",
                           new Object[] {ejbRef.getName() , intfClassName, 
                           ejbReferee.getName(), ( ejbRef.isLocal() ?
                           "Local Business" : "Remote Business"),
                                             targetBusinessIntfs.toString()});


                    // We can only figure out what the correct type should be
                    // if there is only 1 target remote/local business intf.
                    if( targetBusinessIntfs.size() == 1 ) {
                        Iterator iter = targetBusinessIntfs.iterator();
                        ejbRef.setEjbInterface((String)iter.next());
                    }
                }

            } else {

                String targetHome = ejbRef.isLocal() ? 
                    ejbReferee.getLocalHomeClassName() : 
                    ejbReferee.getHomeClassName();

                if( !homeClassName.equals(targetHome) ) {

                    DOLUtils.getDefaultLogger().log(Level.WARNING, 
                       "enterprise.deployment.backend.ejbRefTypeMismatch",
                       new Object[] {ejbRef.getName() , homeClassName, 
                       ejbReferee.getName(), ( ejbRef.isLocal() ? 
                       "Local Home" : "Remote Home"), targetHome});

                    if( targetHome != null ) {
                        ejbRef.setEjbHomeInterface(targetHome);
                    }
                }

                String targetComponentIntf = ejbRef.isLocal() ?
                    ejbReferee.getLocalClassName() : 
                    ejbReferee.getRemoteClassName();

                // In some cases for 2.x style @EJBs that point to Entity beans
                // the interface class cannot be derived, so only do the
                // check if the intf is known.
                if( (intfClassName != null) &&
                    !intfClassName.equals(targetComponentIntf) ) {

                    DOLUtils.getDefaultLogger().log(Level.WARNING, 
                       "enterprise.deployment.backend.ejbRefTypeMismatch",
                       new Object[] {ejbRef.getName() , intfClassName, 
                       ejbReferee.getName(), ( ejbRef.isLocal() ? 
                       "Local" : "Remote"), targetComponentIntf});

                    if( targetComponentIntf != null ) {
                        ejbRef.setEjbInterface(targetComponentIntf);
                    }
                }
            }

            // set jndi name in ejb ref 
            ejbRef.setJndiName(ejbReferee.getJndiName());

        if (!type.equals(ejbRef.getType())) {
            // if they don't match 
            // print a warning and reset the type in ejb ref
            DOLUtils.getDefaultLogger().log(Level.WARNING, DOLUtils.INVALID_DESC_MAPPING,
            new Object[] {ejbRef.getName() , type});

            ejbRef.setType(ejbRef.getType());

        }
    }

    protected Collection getEjbDescriptors() {
        if (getApplication() != null) {
            return getApplication().getEjbDescriptors();
        } else if (getEjbBundleDescriptor()!=null) {
            return getEjbBundleDescriptor().getEjbs();
        } else {
            return new HashSet<EjbDescriptor>();
        }
    }

    protected EjbDescriptor getEjbDescriptor() {
        return null;
    }

    protected EjbBundleDescriptor getEjbBundleDescriptor() {
        return null;
    }

    /**
     * Returns a map of interface name -> EjbIntfInfo based on all the ejbs
     * within the application or stand-alone module.  Only RemoteHome, 
     * RemoteBusiness, LocalHome, and LocalBusiness are eligible for map. 
     */
    private Map<String, EjbIntfInfo> getEjbIntfMap() {
        Map<String, EjbIntfInfo> intfInfoMap = new HashMap<String, EjbIntfInfo>();

        for(Iterator iter = getEjbDescriptors().iterator(); iter.hasNext(); ) {
            EjbDescriptor next = (EjbDescriptor) iter.next();
            if( next.isRemoteInterfacesSupported() ) {
                addIntfInfo(intfInfoMap, next.getHomeClassName(), 
                            EjbIntfType.REMOTE_HOME, next);
            }

            if( next.isRemoteBusinessInterfacesSupported() ) {
                for(String nextIntf : next.getRemoteBusinessClassNames()) {
                    addIntfInfo(intfInfoMap, nextIntf, 
                                EjbIntfType.REMOTE_BUSINESS, next);
                }
            }

            if( next.isLocalInterfacesSupported() ) {
                addIntfInfo(intfInfoMap, next.getLocalHomeClassName(), 
                            EjbIntfType.LOCAL_HOME, next);
            }

            if( next.isLocalBusinessInterfacesSupported() ) {
                for(String nextIntf : next.getLocalBusinessClassNames()) {
                    addIntfInfo(intfInfoMap, nextIntf, 
                                EjbIntfType.LOCAL_BUSINESS, next);
                }
            }

            if (next.isLocalBean()) {
                addIntfInfo(intfInfoMap, next.getEjbClassName(),
                                EjbIntfType.NO_INTF_LOCAL_BUSINESS, next);
            }

        }

        return intfInfoMap;
    }

    private void addIntfInfo(Map<String, EjbIntfInfo> intfInfoMap,
                             String intf, EjbIntfType intfType,
                             EjbDescriptor ejbDesc) {

        EjbIntfInfo intfInfo = intfInfoMap.get(intf);
        if( intfInfo == null ) {
            EjbIntfInfo newInfo = new EjbIntfInfo();
            newInfo.ejbs = new HashSet<EjbDescriptor>();
            newInfo.ejbs.add(ejbDesc);
            newInfo.intfType = intfType;
            intfInfoMap.put(intf, newInfo);
        } else {
            intfInfo.ejbs.add(ejbDesc);
            // Since there's more than one match, reset intf type.
            intfInfo.intfType = EjbIntfType.NONE;
        }

    }

    /**
     * Visits a service reference for the last J2EE component visited
     * 
     * @param the service reference
     */
    @Override
    protected void accept(ServiceReferenceDescriptor serviceRef) {

        Set portsInfo = serviceRef.getPortsInfo();

        for(Iterator iter = portsInfo.iterator(); iter.hasNext();) {
            ServiceRefPortInfo next = (ServiceRefPortInfo) iter.next();

            if( next.hasPortComponentLinkName() &&
                !next.isLinkedToPortComponent() ) {
                WebServiceEndpoint portComponentLink = next.resolveLinkName();
                if( portComponentLink == null ) {
                    String linkName = next.getPortComponentLinkName(); 
                    DOLUtils.getDefaultLogger().log(Level.WARNING, DOLUtils.INVALID_DESC_MAPPING,
                        new Object[] {"port-component" , linkName});
                }                                                   
            }

        }
    }    

    @Override
    protected void accept(ResourceReferenceDescriptor resRef) {
        computeRuntimeDefault(resRef);
    }

    @Override
    protected void accept(ResourceEnvReferenceDescriptor resourceEnvRef) {

        if (resourceEnvRef.getJndiName() == null ||
                resourceEnvRef.getJndiName().length() == 0) {
            Map<String, ManagedBeanDescriptor> managedBeanMap = getManagedBeanMap();
            String refType = resourceEnvRef.getRefType();
            if( managedBeanMap.containsKey(refType) ) {
                ManagedBeanDescriptor desc = managedBeanMap.get(refType);

                // In app-client, keep lookup local to JVM so it doesn't need to access
                // server's global JNDI namespace for managed bean.
                String jndiName = ( bundleDescriptor.getModuleType() == DOLUtils.carType() )
                        ?  desc.getAppJndiName() : desc.getGlobalJndiName();

                resourceEnvRef.setJndiName(jndiName);
                resourceEnvRef.setIsManagedBean(true);
                resourceEnvRef.setManagedBeanDescriptor(desc);
            }
        }

        computeRuntimeDefault(resourceEnvRef);
    }

    protected void accept(MessageDestinationReferenceDescriptor msgDestRef) {
        computeRuntimeDefault(msgDestRef);
    }

    @Override
    protected void accept(MessageDestinationDescriptor msgDest) {
        computeRuntimeDefault(msgDest);
    }

    /**
     * visits all entries within the component environment for which
     * isInjectable() == true.
     * @param injectable InjectionCapable environment dependency
     */
    protected void accept(InjectionCapable injectable) {
        acceptWithCL(injectable);
        acceptWithoutCL(injectable);
    }

    // we need to split the accept(InjectionCapable) into two parts:
    // one needs classloader and one doesn't. This is needed because
    // in the standalone war case, the classloader is not created
    // untill the web module is being started.

    protected void acceptWithCL(InjectionCapable injectable) {
        // If parsed from deployment descriptor, we need to determine whether
        // the inject target name refers to an injection field or an
        // injection method for each injection target
        for (InjectionTarget target : injectable.getInjectionTargets()) {
            if( (target.getFieldName() == null) &&
                    (target.getMethodName() == null) ) {

                String injectTargetName = target.getTargetName();
                String targetClassName  = target.getClassName();
                ClassLoader classLoader = getBundleDescriptor().getClassLoader();

                Class targetClazz = null;

                try {

                    targetClazz = classLoader.loadClass(targetClassName);

                } catch(ClassNotFoundException cnfe) {
                    // @@@
                    // Don't treat this as a fatal error for now.  One known issue
                    // is that all .xml, even web.xml, is processed within the
                    // appclient container during startup.  In that case, there
                    // are issues with finding .classes in .wars due to the
                    // structure of the returned client .jar and the way the
                    // classloader is formed.
                    DOLUtils.getDefaultLogger().fine
                            ("Injection class " + targetClassName + " not found for " +
                            injectable);
                    return;
                }

                // Spec requires that we attempt to match on method before field.
                boolean matched = false;

                // The only information we have is method name, so iterate
                // through the methods find a match.  There is no overloading
                // allowed for injection methods, so any match is considered
                // the only possible match.

                String setterMethodName = TypeUtil.
                        propertyNameToSetterMethod(injectTargetName);

                // method can have any access type so use getDeclaredMethods()
                for(Method next : targetClazz.getDeclaredMethods()) {
                    // only when the method name matches and the method
                    // has exactly one parameter, we find a match
                    if( next.getName().equals(setterMethodName) &&
                        next.getParameterTypes().length == 1) {
                        target.setMethodName(next.getName());
                        if( injectable.getInjectResourceType() == null ) {
                            Class[] paramTypes = next.getParameterTypes();
                            if (paramTypes.length == 1) {
                                String resourceType = paramTypes[0].getName();
                                injectable.setInjectResourceType(resourceType);
                            }
                        }
                        matched = true;
                        break;
                    }
                }

               if( !matched ) {

                    // In the case of injection fields, inject target name ==
                    // field name.  Field can have any access type.
                    try {
                        Field f = targetClazz.getDeclaredField(injectTargetName);
                        target.setFieldName(injectTargetName);
                        if( injectable.getInjectResourceType() == null ) {
                            String resourceType = f.getType().getName();
                            injectable.setInjectResourceType(resourceType);
                        }
                        matched = true;
                    } catch(NoSuchFieldException nsfe) {
                        String msg = "No matching injection setter method or " +
                                "injection field found for injection property " +
                                injectTargetName + " on class " + targetClassName +
                                " for component dependency " + injectable;

                        throw new RuntimeException(msg, nsfe);
                    }
                }
            }
        }
    }

    protected void acceptWithoutCL(InjectionCapable injectable) {
    }

    /**
     * Set a default RunAs principal to given RunAsIdentityDescriptor
     * if necessary.
     * @param runAs
     * @param application
     * @exception RuntimeException
     */
    protected void computeRunAsPrincipalDefault(RunAsIdentityDescriptor runAs,
            Application application) {

        // for backward compatibile
        if (runAs != null &&
                (runAs.getRoleName() == null ||
                    runAs.getRoleName().length() == 0)) {
            DOLUtils.getDefaultLogger().log(Level.WARNING,
            "enterprise.deployment.backend.emptyRoleName");
            return;
        }

        if (runAs != null &&
                (runAs.getPrincipal() == null ||
                    runAs.getPrincipal().length() == 0) &&
                application != null && application.getRoleMapper() != null) {

            String principalName = null;
            String roleName = runAs.getRoleName();

            final Subject fs = (Subject)application.getRoleMapper().getRoleToSubjectMapping().get(roleName);
            if (fs != null) {
                principalName = (String)AccessController.doPrivileged(new PrivilegedAction() {
                    @Override
                    public Object run() {
                        Set<Principal> pset = fs.getPrincipals();
                        Principal prin = null;
                        if (pset.size() > 0) {
                            prin = (Principal)pset.iterator().next();
                            DOLUtils.getDefaultLogger().log(Level.WARNING,
                            "enterprise.deployment.backend.computeRunAsPrincipal",
                            new Object[] { prin.getName() });
                        }
                        return (prin != null) ? prin.getName() : null;
                    }
                });
            }

            if (principalName == null || principalName.length() == 0) {
                throw new RuntimeException("The RunAs role " + "\"" + roleName + "\"" +
                    " is not mapped to a principal.");
            }
            runAs.setPrincipal(principalName);
        }
    }

    /**
     * Get a map of bean class to managed bean descriptor for the managed beans
     * defined within the current module.
     */
    private Map<String, ManagedBeanDescriptor> getManagedBeanMap() {
        BundleDescriptor thisBundle = getBundleDescriptor();

        Set<ManagedBeanDescriptor> managedBeans = new HashSet<ManagedBeanDescriptor>();

        // Make sure we're dealing with the top-level bundle descriptor when looking
        // for managed beans
        if( thisBundle != null ) {
            Object desc = thisBundle.getModuleDescriptor().getDescriptor();
            if( desc instanceof BundleDescriptor ) {
                managedBeans = ((BundleDescriptor)desc).getManagedBeans();
            }
        }

        Map<String, ManagedBeanDescriptor> managedBeanMap = new HashMap<String, ManagedBeanDescriptor>();

        for(ManagedBeanDescriptor managedBean : managedBeans ) {

            String beanClassName = managedBean.getBeanClassName();
            managedBeanMap.put(beanClassName, managedBean);
        }

        return managedBeanMap;

    }

    /**
     * Set runtime default value for ResourceReferenceDescriptor.
     */
    private void computeRuntimeDefault(ResourceReferenceDescriptor resRef) {
        if (resRef.getType() != null && resRef.getType().equals("org.omg.CORBA.ORB")) {
            resRef.setJndiName("java:comp/ORB");
        }

        else if (resRef.getJndiName() == null ||
                resRef.getJndiName().length() == 0) {
            if (resRef.getType() != null) {
                if (resRef.getType().equals("javax.sql.DataSource"))
                    resRef.setLookupName("java:comp/DefaultDataSource");
                else if (resRef.getType().equals("javax.jms.ConnectionFactory"))
                    resRef.setLookupName("java:comp/DefaultJMSConnectionFactory");
                else
                    resRef.setJndiName(getDefaultResourceJndiName(resRef.getName()));
            } else {
                resRef.setJndiName(getDefaultResourceJndiName(resRef.getName()));
            }
        }
    }

    /**
     * Set runtime default value for ResourceEnvReferenceDescriptor.
     */
    private void computeRuntimeDefault(ResourceEnvReferenceDescriptor resourceEnvRef) {
        if (resourceEnvRef.getRefType() != null && resourceEnvRef.getRefType().equals(
            "javax.transaction.UserTransaction")) {
            resourceEnvRef.setJndiName("java:comp/UserTransaction");
        }

        else if (resourceEnvRef.getRefType() != null && resourceEnvRef.getRefType().equals("javax.transaction.TransactionSynchronizationRegistry")) {
            resourceEnvRef.setJndiName(
                "java:comp/TransactionSynchronizationRegistry");
        }

        else if (resourceEnvRef.getJndiName() == null ||
                resourceEnvRef.getJndiName().length() == 0) {
            if (resourceEnvRef.getRefType() != null) {
                if (resourceEnvRef.getRefType().equals("javax.enterprise.concurrent.ManagedExecutorService")) {
                    resourceEnvRef.setLookupName("java:comp/DefaultManagedExecutorService");
                } else if (resourceEnvRef.getRefType().equals("javax.enterprise.concurrent.ManagedScheduledExecutorService")) {
                    resourceEnvRef.setLookupName("java:comp/DefaultManagedScheduledExecutorService");
                } else if (resourceEnvRef.getRefType().equals("javax.enterprise.concurrent.ManagedThreadFactory")) {
                    resourceEnvRef.setLookupName("java:comp/DefaultManagedThreadFactory");
                } else if (resourceEnvRef.getRefType().equals("javax.enterprise.concurrent.ContextService")) {
                    resourceEnvRef.setLookupName("java:comp/DefaultContextService");
                } else {
                    resourceEnvRef.setJndiName(getDefaultResourceJndiName(resourceEnvRef.getName()));
                }
            }  else {
                resourceEnvRef.setJndiName(getDefaultResourceJndiName(resourceEnvRef.getName()));
            }
        }
    }

    /**
     * Set runtime default value for MessageDestinationReferenceDescriptor.
     */
    private void computeRuntimeDefault(MessageDestinationReferenceDescriptor msgDestRef) {
        if (msgDestRef.getJndiName() == null ||
                msgDestRef.getJndiName().length() == 0) {
            msgDestRef.setJndiName(getDefaultResourceJndiName(msgDestRef.getName()));
        }
    }

    /**
     * Set runtime default value for MessageDestinationDescriptor.
     */
    private void computeRuntimeDefault(MessageDestinationDescriptor msgDest) {
        if (msgDest.getJndiName() == null ||
                msgDest.getJndiName().length() == 0) {
            msgDest.setJndiName(getDefaultResourceJndiName(msgDest.getName()));
        }
    }

    /**
     * @param resName
     * @return default jndi name for a given interface resource name
     */
    private String getDefaultResourceJndiName(String resName) {
        return resName;
    }

}
