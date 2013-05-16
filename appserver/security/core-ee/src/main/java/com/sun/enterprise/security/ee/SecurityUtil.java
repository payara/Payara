/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.ee;

import java.security.*;

import javax.security.jacc.*;
//import com.sun.ejb.Invocation; 
import com.sun.enterprise.security.SecurityRoleMapperFactoryGen;
import com.sun.enterprise.security.util.IASSecurityException;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.*;
import java.util.logging.*;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import org.glassfish.deployment.common.SecurityRoleMapperFactory;
import java.util.Collection;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.deployment.versioning.VersioningUtils;
/** 
  * This utility class encloses all the calls to a ejb method
  * in a specified subject
  * @author Harpreet Singh
  * @author Shing Wai Chan
  */
public class SecurityUtil{

    private static final LocalStringManagerImpl localStrings =
	new LocalStringManagerImpl(SecurityUtil.class);
    private static final Logger _logger =
        LogDomains.getLogger(SecurityUtil.class, LogDomains.SECURITY_LOGGER);
    public static final String VENDOR_PRESENT = 
	"com.sun.enterprise.security.provider.jaccvendorpresent";
    private static final boolean vendorPresent = Boolean.getBoolean(VENDOR_PRESENT);
    
     // set in PolicyLoader from domain.xml
    private static final String REPOSITORY_HOME_PROP =
        "com.sun.enterprise.jaccprovider.property.repository";
    //TODO remove use of system property
    // The repository is defined in PolicyFileMgr.
    // It is repeated here since JACC provider is not reference directly.
    public static final String repository = System.getProperty(REPOSITORY_HOME_PROP);

    /** This method is called from the generated code to execute the
     * method.  This is a translation of method.invoke that the
     * generated code needs to do, to invoke a particular ejb
     * method. The method is invoked under a security Subject. This
     * method is called from the generated code.
     * @param Method beanClassMethod, the bean class method to be invoked
     * @param Invocation inv, the current invocation object
     * @param Object o, the object on which this method needs to be invoked,
     * @param Object[] oa, the parameters to the methods,
     * @param Container c, the container from which the appropriate subject is 
     * queried from.
     */
    /* This method is now in EJBSecurityUtil in ejb/ejb-container module of V3
    public static Object runMethod(Method beanClassMethod, Invocation inv, Object o, Object[] oa, Container c)
    throws Throwable {

	    final Method meth = beanClassMethod;
	    final Object obj = o;
	    final Object[] objArr = oa;
	    Object ret;
	    EJBSecurityManager mgr = (EJBSecurityManager) c.getSecurityManager();
 	    if (mgr == null) {
 		throw new SecurityException("SecurityManager not set");
	    }

            // Optimization.  Skip doAsPrivileged call if this is a local
            // invocation and the target ejb uses caller identity or the
	    // System Security Manager is disabled.
            // Still need to execute it within the target bean's policy context.
            // see CR 6331550
            if((inv.isLocal && mgr.getUsesCallerIdentity()) || 
	       System.getSecurityManager() == null) {
                ret = mgr.runMethod(meth, obj, objArr);
            } else {
                try {
                    PrivilegedExceptionAction pea =
                        new PrivilegedExceptionAction(){
                            public java.lang.Object run() throws Exception {
                                return meth.invoke(obj, objArr);
                            }
                        };

                    ret = mgr.doAsPrivileged(pea);
                } catch(PrivilegedActionException pae) {
                    Throwable cause = pae.getCause();
                    if( cause instanceof InvocationTargetException ) {
                        cause = ((InvocationTargetException) cause).getCause();
                    } 
                    throw cause;
                } 
            }
	    return ret;
    } */
    /**
     * This method is similiar to the runMethod, except it keeps the
     * semantics same as the one in reflection. On failure, if the
     * exception is caused due to reflection, it returns the
     * InvocationTargetException.  This method is called from the
     * containers for ejbTimeout, WebService and MDBs.
     * @param Method beanClassMethod, the bean class method to be invoked
     * @param Invocation inv, the current invocation
     * @param Object o, the object on which this method is to be
     * invoked in this case the ejb,
     * @param Object[] oa, the parameters for the method,
     * @param Container c, the container instance, 
     * @param SecurityManager sm, security manager for this container,
     * can be a null value, where in the container will be queried to
     * find its security manager.
     * @return Object, the result of the execution of the method.
     */
    /* This method is now in EJBSecurityUtil in ejb/ejb-container module of V3 
    public static Object invoke(Method beanClassMethod, Invocation inv, Object o, Object[] oa, Container c, 
					       SecurityManager mgr) throws Throwable {
	
	final Method meth = beanClassMethod;
	final Object obj = o;
	final Object[] objArr = oa;
	Object ret = null;
        EJBSecurityManager ejbSecMgr = null;

 	if(mgr == null) {
	    if (c != null) {
		ejbSecMgr = (EJBSecurityManager) c.getSecurityManager();
	    }
	    if (ejbSecMgr == null) {
 		throw new SecurityException("SecurityManager not set");
	    }
	} else {
            ejbSecMgr = (EJBSecurityManager) mgr;
        }

        // Optimization.  Skip doAsPrivileged call if this is a local
        // invocation and the target ejb uses caller identity or the
	// System Security Manager is disabled.
        // Still need to execute it within the target bean's policy context.
        // see CR 6331550
        if((inv.isLocal && ejbSecMgr.getUsesCallerIdentity()) ||
	   System.getSecurityManager() == null) {
            ret = ejbSecMgr.runMethod(meth, obj, objArr);
        } else {

            PrivilegedExceptionAction pea =
                new PrivilegedExceptionAction(){
                    public java.lang.Object run() throws Exception {
                        return meth.invoke(obj, objArr);
                    }
                };
 
            try {
                ret = ejbSecMgr.doAsPrivileged(pea);
            } catch(PrivilegedActionException pae) {
                Throwable cause = pae.getCause();
                throw cause;
            } 
        }
	return ret;
    }*/

     /** This method obtains the policy configuration object
     *  corresponding to the name, and causes the corresponding policy
     *  statements to be put in service. This method also informs the
     *  policy module to refresh its in service policy contexts.
     *  Note that policy statements have already been
     *  added to the pc, this method works to put them in Service.
     *  @param String name - the module id which serves to identify
     *  the corresponding policy context. The name shall not be null.
     *  If the underlying PolicyModule is the RI PolicyModule, 
     *  A SecurityRoleMapper must have been bound to the policy context
     *  before this method is called or the embedded call to pc.commit will
     *  throw an exception.
     */
    public static void generatePolicyFile(String name) throws IASSecurityException {
	assert name != null;
	if (name == null) {
	    throw new IASSecurityException("Invalid Module Name");
        }
        
	try {

	    boolean inService = 
		PolicyConfigurationFactory.getPolicyConfigurationFactory().
		inService(name);
	    if (!inService) {
		// find the PolicyConfig using remove=false to ensure policy stmts 
		// are retained.

		// Note that it is presumed that the pc exists, and that
		// it is populated with the desired policy statements.
		// If this is not true, the call to commit will not
		// result in the correct policy statements being made
		// available to the policy module.
                PolicyConfigurationFactory pcf = 
                    PolicyConfigurationFactory.getPolicyConfigurationFactory();
                PolicyConfiguration pc =
		    pcf.getPolicyConfiguration(name, false);
		pc.commit();
		if (_logger.isLoggable(Level.FINE)){
		    _logger.fine("JACC: committed policy for context: "+name);
		}
	    }

	    Policy.getPolicy().refresh();
	} catch(java.lang.ClassNotFoundException cnfe){
	    //String msg = localStrings.getLocalString("enterprise.security.securityutil.classnotfound","Could not find PolicyConfigurationFactory class. Check javax.security.jacc.PolicyConfigurationFactory.provider property");
	    throw new IASSecurityException(cnfe);
	} catch(javax.security.jacc.PolicyContextException pce){
	    throw new IASSecurityException(pce);
	}
    }

    /** 
     * Inform the policy module to take the named policy context out of service.
     * The policy context is transitioned to the deleted state. In our provider
     * implementation, the corresponding policy file is deleted, as the presence
     * of a policy file in the repository is how we persistently remember which
     * policy contexts are in service.
     * @param String name - the module id which serves to identify
     * the corresponding policy context. The name shall not be null.
     */
    public static void removePolicy(String name) throws IASSecurityException {
	assert name != null;
	if (name == null) {
	    throw new IASSecurityException("Invalid Module Name");
	}
	try {
	    boolean wasInService = 
		PolicyConfigurationFactory.getPolicyConfigurationFactory().
		inService(name);	    
	    // find the PolicyConfig and delete it.
	    PolicyConfiguration pc = 
		PolicyConfigurationFactory.getPolicyConfigurationFactory().
		getPolicyConfiguration(name, false);
	    pc.delete();
	    // Only do refresh policy if the deleted context was in service
	    if (wasInService) {
		Policy.getPolicy().refresh();
	    }

	} catch(java.lang.ClassNotFoundException cnfe){
	    String msg = localStrings.getLocalString("enterprise.security.securityutil.classnotfound","Could not find PolicyConfigurationFactory class. Check javax.security.jacc.PolicyConfigurationFactory.provider property");
	    throw new IASSecurityException(msg);
	} catch(javax.security.jacc.PolicyContextException pce){
	    throw new IASSecurityException(pce.toString());
	}
    }
    /** This method obtains the policy configuration object
    *  corresponding to the name, and links it, for roleMapping purposes
    *  to another. If the pc is already InService when this method is called,
    *  this method does nothing.
    *  @param String name - the module id which serves to identify
    *  the corresponding policy context. The name shall not be null.
    *  @param String linkName - the module id of the module being linked
    *  to this context. This value may be null, in which case, no link is done,
    *  but the inService state of the named PC is returned.
    *  @param boolean lastInService - the inService state returned by the previous
    *  call to this method. The value of this argument is only significant when linkName
    *  is not null.
    *  @return boolean if linkName is null, returns the inService state of the
    *  PC identified in the name argument. Otherwise returns the value
    *  passed to lastInService.
    */
    public static boolean linkPolicyFile(String name, String linkName, boolean lastInService)
    throws IASSecurityException {
        
        boolean rvalue = lastInService;
        
        assert name != null;
        
        if (name == null) {
            throw new IASSecurityException("Invalid Module Name");
        }
        try {
            PolicyConfigurationFactory pcf = PolicyConfigurationFactory.getPolicyConfigurationFactory();
            boolean inService =  pcf.inService(name);
            
            if (linkName == null) {
                rvalue = inService;
            } else if (inService == lastInService) {
                
                // only do the link if the named PC is not inService.
                if (!inService) {
                    
                    // find the PolicyConfigs using remove=false to ensure policy stmts
                    // are retained.
                    
                    PolicyConfiguration pc = 
                        pcf.getPolicyConfiguration(name, false);                    
                    PolicyConfiguration linkPc =
                        pcf.getPolicyConfiguration(linkName, false);                    
                    pc.linkConfiguration(linkPc);
                }
            } else {
                throw new IASSecurityException("Inconsistent Module State");
            }
            
        } catch(java.lang.ClassNotFoundException cnfe){
            String msg = localStrings.getLocalString("enterprise.security.securityutil.classnotfound","Could not find PolicyConfigurationFactory class. Check javax.security.jacc.PolicyConfigurationFactory.provider property");
            throw new IASSecurityException(msg);
        } catch(javax.security.jacc.PolicyContextException pce){
            throw new IASSecurityException(pce.toString());
        }        
        return rvalue;
    }

    /**
     * create pseudo module context id, and make sure it is unique, by
     * chacking it against the names of all the other modules in the app.
     * @param ejbDesc
     * @return
     */
    private static String createUniquePseudoModuleID(EjbBundleDescriptor ejbDesc) {

        Application app = ejbDesc.getApplication();
        Collection<WebBundleDescriptor> webModules = app.getBundleDescriptors(WebBundleDescriptor.class);
        Collection<EjbBundleDescriptor> ejbModules = app.getBundleDescriptors(EjbBundleDescriptor.class);

        String moduleName = ejbDesc.getUniqueFriendlyId();
        String pseudonym;
        int uniquifier = 0;
        boolean unique;
        do {
            unique = true;
            pseudonym = moduleName +
                    (uniquifier == 0 ? "_internal" : "_internal_" + uniquifier);
            if (webModules != null) {
                for (WebBundleDescriptor w : webModules) {
                    if (pseudonym.equals(w.getUniqueFriendlyId())) {
                        unique = false;
                        break;
                    }
                }
            }
            if (unique && ejbModules != null) {
                for (EjbBundleDescriptor e : ejbModules) {
                   if (pseudonym.equals(e.getUniqueFriendlyId())) {
                        unique = false;
                        break;
                    }
                }
            }
            uniquifier += 1;
            
        } while (!unique);

        return VersioningUtils.getRepositoryName(app.getRegistrationName()) + "/" + pseudonym;
    }

    public static String getContextID(EjbBundleDescriptor ejbBundleDesc) {
        String cid = null;
        if (ejbBundleDesc != null) {
            /* detect special case of EJBs embedded in a war,
             * and make sure psuedo policy context id is unique within app
             */
            Object root = ejbBundleDesc.getModuleDescriptor().getDescriptor();
            if( (root != ejbBundleDesc) && (root instanceof WebBundleDescriptor ) ) {
                cid = createUniquePseudoModuleID(ejbBundleDesc);
            } else {
                cid = VersioningUtils.getRepositoryName(ejbBundleDesc.getApplication().getRegistrationName()) +
                    '/' + ejbBundleDesc.getUniqueFriendlyId();
            }
        }
        return cid;
    }
     public static String getContextID(WebBundleDescriptor wbd) {
        String cid = null;
        if (wbd != null ) {
            //String moduleId = wbd.getUniqueFriendlyId();
            cid = VersioningUtils.getRepositoryName(wbd.getApplication().getRegistrationName()) +
                '/' + wbd.getUniqueFriendlyId();
        }
        return cid;
    }
     
    public static  void removeRoleMapper(DeploymentContext dc) {
        OpsParams params = dc.getCommandParameters(OpsParams.class);
        if (params.origin != OpsParams.Origin.undeploy) {
            return;
        }
        String appName = params.name();
        SecurityRoleMapperFactory factory = getRoleMapperFactory();
        factory.removeRoleMapper(appName);

    }
    
   public static SecurityRoleMapperFactory getRoleMapperFactory() {
       SecurityRoleMapperFactory factory = SecurityRoleMapperFactoryGen.getSecurityRoleMapperFactory();
       if (factory == null) {
           throw new IllegalArgumentException("This application has no role mapper factory defined");
       }
       return factory;
   }
}
