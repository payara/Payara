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

/*
 * BasePolicyWrapper.java
 *
 * @author Harpreet Singh (harpreet.singh@sun.com)
 * @author Ron Monzillo
 * @version
 *
 * Created on May 23, 2002, 1:56 PM
 */

package com.sun.enterprise.security.provider;

//import sun.security.provider.PolicyFile;
import sun.security.util.PropertyExpander;
import java.security.*;
import javax.security.jacc.*;
import javax.management.MBeanPermission;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.logging.*; 
import com.sun.logging.LogDomains;
import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * This class is a wrapper around the default jdk policy file 
 * implementation. BasePolicyWrapper is installed as the JRE policy object
 * It multiples policy decisions to the context specific instance of
 * sun.security.provider.PolicyFile.
 * Although this Policy provider is implemented using another Policy class,
 * this class is not a "delegating Policy provider" as defined by JACC, and
 * as such it SHOULD not be configured using the JACC system property
 * javax.security.jacc.policy.provider.
 * @author Harpreet Singh (harpreet.singh@sun.com)  
 * @author Jean-Francois Arcand
 * @author Ron Monzillo
 *
 */
public class BasePolicyWrapper extends java.security.Policy {

    private static final String FACTORY_NAME = 
	"javax.security.jacc.PolicyConfigurationFactory.provider";
    private static final String myFactoryName = 
            "com.sun.enterprise.security.provider.PolicyConfigurationFactoryImpl";
    // this is the jdk policy file instance
    private java.security.Policy policy = null;
   
    private static Logger logger = Logger.getLogger(LogDomains.SECURITY_LOGGER);
    private static LocalStringManagerImpl localStrings =
    new LocalStringManagerImpl(BasePolicyWrapper.class);

    static String logMsg(Level level,String key,Object[] params,String defMsg){
	String msg = (key == null ? defMsg : localStrings.getLocalString
	    (key,defMsg == null ? key : defMsg, params));
	logger.log(level,msg);
	return msg;
    }

    private static final String REUSE = "java.security.Policy.supportsReuse";
    
    /**
     * Name of the system property to enable detecting and avoiding reentrancy.
     * This property can be set using <jvm-options> in domain.xml.  If not set
     * or set to false, this class will detect or avoid reentrancy in policy
     * evaluation.  Note that if SecurityManager is turned off, this feature is
     * always turned off.
     * Another design approach is to name the property differently
     * and use a list of context ids as its value, so that this feature may be
     * enabled for selected contexts.
     */
    private static final String IGNORE_REENTRANCY_PROP_NAME = 
            "com.sun.enterprise.security.provider.PolicyWrapper.ignoreReentrancy";
    
    /**
     * Flag to indicate if detecting and avoiding reentrancy is enabled.
     * If SecurityManager is turned off, reentrancy is less likely to occur and
     * this feature is always off; else if the system property IGNORE_REENTRANCY_PROP_NAME
     * is not set, or set to false in domain.xml, this feature is on;
     * 
     */
    private static final boolean avoidReentrancy =
            (!Boolean.getBoolean(IGNORE_REENTRANCY_PROP_NAME)) &&
            (System.getSecurityManager() != null);
    
    /**
     * ThreadLocal object to keep track of the reentrancy status of each thread.
     * It contains a byte[] object whose single element is either 0 (initial
     * value or no reentrancy), or 1 (current thread is reentrant). When a
     * thread exists the implies method, byte[0] is alwasy reset to 0.
     */
    private static ThreadLocal reentrancyStatus;

    static {
        if(avoidReentrancy) {
            reentrancyStatus = new ThreadLocal() {
                protected synchronized Object initialValue() {
                    return new byte[]{0};
                }
            };
        }
    }

    
    /** Creates a new instance of BasePolicyWrapper */
    public BasePolicyWrapper() {
        // the jdk policy file implementation
        policy = getNewPolicy();
	refreshTime = 0L;
	// call the following routine to compute the actual refreshTime
	defaultContextChanged();
    }
    
    /** gets the underlying PolicyFile implementation
     * can be overridden by Subclass
     */
    protected java.security.Policy getNewPolicy() {
	return (java.security.Policy) new sun.security.provider.PolicyFile();
    }

    /**
     * Evaluates the global policy and returns a
     * PermissionCollection object specifying the set of
     * permissions allowed for code from the specified
     * code source.
     *
     * @param codesource the CodeSource associated with the caller.
     * This encapsulates the original location of the code (where the code
     * came from) and the public key(s) of its signer.
     *
     * @return the set of permissions allowed for code from <i>codesource</i>
     * according to the policy.The returned set of permissions must be
     * a new mutable instance and it must support heterogeneous
     * Permission types.
     *
     */
    public PermissionCollection getPermissions(CodeSource codesource) {
	String contextId = PolicyContext.getContextID();
	PolicyConfigurationImpl pci = getPolicyConfigForContext(contextId);
	Policy appPolicy = getPolicy(pci);
	PermissionCollection perms = appPolicy.getPermissions(codesource);
	if (perms != null) {
	    perms = removeExcludedPermissions(pci,perms);
	}
	if (logger.isLoggable(Level.FINEST)){
	    logger.finest("JACC Policy Provider: PolicyWrapper.getPermissions(cs), context ("+contextId+") codesource ("+codesource+") permissions: "+perms);
	}
        return perms;
    }

    /**
     * Evaluates the global policy and returns a
     * PermissionCollection object specifying the set of
     * permissions allowed given the characteristics of the
     * protection domain.
     *
     * @param domain the ProtectionDomain associated with the caller.
     *
     * @return the set of permissions allowed for the <i>domain</i>
     * according to the policy.The returned set of permissions must be
     * a new mutable instance and it must support heterogeneous
     * Permission types.
     *
     * @see java.security.ProtectionDomain
     * @see java.security.SecureClassLoader
     * @since 1.4
     */
    public PermissionCollection getPermissions(ProtectionDomain domain) {
	String contextId = PolicyContext.getContextID();
	PolicyConfigurationImpl pci = getPolicyConfigForContext(contextId);
	Policy appPolicy = getPolicy(pci);
	PermissionCollection perms = appPolicy.getPermissions(domain);
	if (perms != null) {
	    perms = removeExcludedPermissions(pci,perms);
	}
	if (logger.isLoggable(Level.FINEST)){
	    logger.finest("JACC Policy Provider: PolicyWrapper.getPermissions(d), context ("+contextId+") permissions: "+perms);
	}
        return perms;
    }
    
    /**
     * Evaluates the global policy for the permissions granted to
     * the ProtectionDomain and tests whether the permission is
     * granted.
     *
     * @param domain the ProtectionDomain to test
     * @param permission the Permission object to be tested for implication.
     *
     * @return true if "permission" is a proper subset of a permission
     * granted to this ProtectionDomain.
     *
     * @see java.security.ProtectionDomain
     * @since 1.4
     */
    public boolean implies(ProtectionDomain domain, Permission permission) {
        if(avoidReentrancy) {
            byte[] alreadyCalled = (byte[]) reentrancyStatus.get();
            if(alreadyCalled[0] == 1) {
                return true;
            } else {
                alreadyCalled[0] = 1;
                try {
                    return doImplies(domain, permission);
                } finally {
                    alreadyCalled[0] = 0;
                }
            }
        } else {
            return doImplies(domain, permission);
        }
    }
    
    
    /**
     * Refreshes/reloads the policy configuration. The behavior of this method
     * depends on the implementation. For example, calling <code>refresh</code>
     * on a file-based policy will cause the file to be re-read.
     *
     */
    public void refresh() {
	if (logger.isLoggable(Level.FINE)){
	    logger.fine("JACC Policy Provider: Refreshing Policy files!");
	}

	// always refreshes default policy context, but refresh 
	// of application context depends on PolicyConfigurationImpl
	// this could result in an inconsistency since default context is
	// included in application contexts.
        policy.refresh();

	// try to determine if default policy context has changed.
	// if so, force refresh of application contexts.
	// if the following code is not robust enough to detect
	// changes to the policy files read by the default context,
	// then you can configure the provider to force on every refresh
	// (see FORCE_APP_REFRESH_PROP_NAME).

	boolean force = defaultContextChanged();
        PolicyConfigurationImpl pciArray[] = null;
        PolicyConfigurationFactoryImpl pcf = getPolicyFactory();
        if (pcf != null) {
            pciArray = pcf.getPolicyConfigurationImpls();
        }
        if (pciArray != null) {

            for (PolicyConfigurationImpl pci : pciArray) {

                if (pci != null) {
                    // false means don't force refresh if no update since
                    // last refresh.
                    pci.refresh(force);
                }
            }
        }
        try {
            if (PolicyContext.getHandlerKeys().contains(REUSE)) {
                PolicyContext.getContext(REUSE);
            }
        } catch (PolicyContextException pe) {
            throw new IllegalStateException(pe.toString());
        }
        
    }

    private  PolicyConfigurationImpl getPolicyConfigForContext(String contextId) {
	PolicyConfigurationImpl pci = null;
        PolicyConfigurationFactoryImpl pcf = getPolicyFactory();
	if (contextId != null && pcf != null) {
	    pci = pcf.getPolicyConfigurationImpl(contextId);
	}
	return pci;
    }

    private Policy getPolicy(PolicyConfigurationImpl pci) {
	Policy result = null;
	if (pci == null) {
	    result = policy;
	} else {
	    result = pci.getPolicy();
	    if (result == null) {
		// the pc is not in service so use the default context
		result = policy;
	    }
	}
	return result;
    }

    private static Permissions getExcludedPolicy(PolicyConfigurationImpl pci) {
	Permissions result = null;
	if (pci != null) {
	    result = pci.getExcludedPolicy();
	}
	return result;
    }

    // should find a way to do this that preserves the argument PermissionCollection
    // safe for now, becauuse on EJBMethodPermission, WebResourcePermission, and
    // WebUserDatePermissions are excluded, and none of these classes implement a
    // custom collection.
    private static PermissionCollection removeExcludedPermissions
	(PolicyConfigurationImpl pci, PermissionCollection perms) {
	PermissionCollection result = perms;
	boolean noneRemoved = true;
	Permissions excluded = getExcludedPolicy(pci);
	if (excluded != null &&  excluded.elements().hasMoreElements()) {
	    result = null;
	    Enumeration e = perms.elements(); 
	    while (e.hasMoreElements()) {
		Permission granted = (Permission) e.nextElement();
		if (!grantedIsExcluded(granted,excluded)) {
		    if (result == null) result = new Permissions();
		    result.add(granted);
		} else {
		    noneRemoved = false;
		}
	    }
	    if (noneRemoved) {
		result = perms;
	    }
	}
	return result;
    }

    private static boolean grantedIsExcluded(Permission granted, Permissions excluded) {
	boolean isExcluded = false;
	if (excluded != null) {
	    if (!excluded.implies(granted)) {
		Enumeration e = excluded.elements();
		while (!isExcluded && e.hasMoreElements()) {
		    Permission excludedPerm = (Permission) e.nextElement();
		    if (granted.implies(excludedPerm))  {
			isExcluded = true;
		    }
		}
	    } else {
		isExcluded = true;
	    }
	}
	if (logger.isLoggable(Level.FINEST)){
	    if (isExcluded) {
		logger.finest("JACC Policy Provider: permission is excluded: "+granted);
	    }
	}
	return isExcluded;
    }
    
    private boolean doImplies(ProtectionDomain domain, Permission permission) {
	String contextId = PolicyContext.getContextID();
	PolicyConfigurationImpl pci = getPolicyConfigForContext(contextId);
	Policy appPolicy = getPolicy(pci);
        
	boolean result = appPolicy.implies(domain,permission);
        
        //log failures but skip failures that occurred prior to redirecting to 
        //login pages, and javax.management.MBeanPermission
	if (!result) {
            if(!(permission instanceof WebResourcePermission) &&
                !(permission instanceof MBeanPermission) &&
                !(permission instanceof WebRoleRefPermission) &&
                !(permission instanceof EJBRoleRefPermission)) {
                
                final String contextId2 = contextId;
                final Permission permission2 = permission;
                final ProtectionDomain domain2 = domain;
                if (logger.isLoggable(Level.FINE)) {
                    Exception ex = new Exception();
                    ex.fillInStackTrace();
                    logger.log(Level.FINE, "JACC Policy Provider, failed Permission Check at :", ex);
                }
                AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        logger.info("JACC Policy Provider: Failed Permission Check, context(" + contextId2 + ")- permission(" + permission2 + ")");
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("Domain that failed(" + domain2 + ")");
                        }
                        return null;
                    }
                });
            }
        } else {
	    Permissions excluded = getExcludedPolicy(pci);
	    if (excluded != null) {
		result = !grantedIsExcluded(permission,excluded);
	    }
	}

	// at FINEST: log only denies
	if (!result && logger.isLoggable(Level.FINEST)){
	    logger.finest("JACC Policy Provider: PolicyWrapper.implies, context ("+
			contextId+")- result was("+result+") permission ("
			+permission+")");
	}
       
        return result;
    }

    /**
     * This method repeats the policy file loading algorithm of
     * sun.security.provider.Policyfile to determine if the refresh
     * resulted in a change to the loaded policy.
     *
     * Note:
     * For backward compatibility with JAAS 1.0 it loads
     * both java.auth.policy and java.policy. However it 
     * is recommended that java.auth.policy be not used
     * and the java.policy contain all grant entries including
     * that contain principal-based entries.
     *
     *
     * <p> This object stores the policy for entire Java runtime,
     * and is the amalgamation of multiple static policy
     * configurations that resides in files.
     * The algorithm for locating the policy file(s) and reading their
     * information into this <code>Policy</code> object is:
     *
     * <ol>
     * <li>
     *   Loop through the <code>java.security.Security</code> properties,
     *   <i>policy.url.1</i>, <i>policy.url.2</i>, ...,
     *   <i>policy.url.X</i>" and 
     *   <i>auth.policy.url.1</i>, <i>auth.policy.url.2</i>, ...,
     *   <i>auth.policy.url.X</i>".  These properties are set
     *   in the Java security properties file, which is located in the file named
     *   &lt;JAVA_HOME&gt;/lib/security/java.security, where &lt;JAVA_HOME&gt;
     *   refers to the directory where the JDK was installed.
     *   Each property value specifies a <code>URL</code> pointing to a
     *   policy file to be loaded.  Read in and load each policy.
     *   
     *   <i>auth.policy.url</i> is supported only for backward compatibility.
     *   
     * <li>
     *   The <code>java.lang.System</code> property <i>java.security.policy</i>
     *   may also be set to a <code>URL</code> pointing to another policy file
     *   (which is the case when a user uses the -D switch at runtime).
     *   If this property is defined, and its use is allowed by the
     *   security property file (the Security property,
     *   <i>policy.allowSystemProperty</i> is set to <i>true</i>),
     *   also load that policy.
     *
     * <li>
     *   The <code>java.lang.System</code> property
     *   <i>java.security.auth.policy</i> may also be set to a 
     *   <code>URL</code> pointing to another policy file
     *   (which is the case when a user uses the -D switch at runtime).
     *   If this property is defined, and its use is allowed by the
     *   security property file (the Security property,
     *   <i>policy.allowSystemProperty</i> is set to <i>true</i>),
     *   also load that policy.
     * 
     *   <i>java.security.auth.policy</i> is supported only for backward
     *   compatibility.
     *
     *   If the  <i>java.security.policy</i> or
     *   <i>java.security.auth.policy</i> property is defined using
     *   "==" (rather than "="), then ignore all other specified
     *   policies and only load this policy.
     * </ol>
     */
    private static final String POLICY = "java.security.policy";
    private static final String POLICY_URL = "policy.url.";
    private static final String AUTH_POLICY = "java.security.auth.policy";
    private static final String AUTH_POLICY_URL = "auth.policy.url.";

    /**
     * Name of the system property that effects whether or not application
     * policy objects are forced to refresh whenever the default context
     * policy object is refreshed. Normally app policy objects only refresh
     * when their app sepcifc policy files have changes. Since app policy 
     * objects alos include the rules of the default context; so they should
     * be refreshed whenever the default context files are changed, but the 
     * algorithm by which a policy module finds its policy files is complex;
     * and dependent on configuration; so this force switch is provided to
     * ensure refresh of the app contexts (when the performace cost of doing 
     * so is acceptable). When this switch is not set, it may be necessary
     * to restart the appserver to force changes in the various policy files to
     * be in effect for specific applications.
     */
    private static final String FORCE_APP_REFRESH_PROP_NAME = 
        "com.sun.enterprise.security.provider.PolicyWrapper.force_app_refresh";

    /**
     * Flag to indicate if application specific policy objects are forced
     * to refresh (independent of whether or not their app specific policy 
     * files have changed).
     */
    private static final boolean forceAppRefresh =
            Boolean.getBoolean(FORCE_APP_REFRESH_PROP_NAME);
    
    private long refreshTime;

    synchronized boolean defaultContextChanged() {

	if (forceAppRefresh) {
	    return true;
	}

	long newTime = getTimeStamp(POLICY,POLICY_URL);
	newTime += getTimeStamp(AUTH_POLICY,AUTH_POLICY_URL);
	boolean rvalue = refreshTime != newTime;
	refreshTime = newTime;
	return rvalue;
    }

    private static long 
    getTimeStamp(final String propname, final String urlname) {
	Long l = (Long) AccessController.doPrivileged(new PrivilegedAction() {
	    public Long run() {
		long sum = 0L;
		boolean allowSystemProperties = "true".equalsIgnoreCase
		    (Security.getProperty("policy.allowSystemProperty"));
		if (allowSystemProperties) {
		    String extra_policy = System.getProperty(propname);
		    if (extra_policy != null) {
			boolean overrideAll = false;
			if (extra_policy.startsWith("=")) {
			    overrideAll = true;
			    extra_policy = extra_policy.substring(1);
			}
			try {
			    String path =PropertyExpander.expand(extra_policy);
			    File policyFile = new File(path);
			    boolean found = policyFile.exists();
			    if (!found) {
				URL policy_url = new URL(path);
				if ("file".equals(policy_url.getProtocol())) {
				    path = policy_url.getFile().
					replace('/', File.separatorChar);
				    path = sun.net.www.ParseUtil.decode(path);
				    policyFile = new File(path);
				    found = policyFile.exists();
				}
			    }
			    if (found) {
				sum += policyFile.lastModified();
				if (logger.isLoggable(Level.FINE)) {
				    logMsg(Level.FINE,"pc.file_refreshed",
					 new Object[]{path},null);
				}			
			    } else {
				if (logger.isLoggable(Level.FINE)) {
				    logMsg(Level.FINE,"pc.file_not_refreshed",
					   new Object[]{path},null);
				}
			    }
			} catch (Exception e) {
			    // ignore. 
			}
			if (overrideAll) {
			    return Long.valueOf(sum);
			}
		    }
		}
		int n = 1;
		String policy_uri;
		while ((policy_uri = Security.getProperty(urlname+n)) != null){
		    try {
			URL policy_url = null;
			String expanded_uri = PropertyExpander.expand
			    (policy_uri).replace(File.separatorChar, '/');
			if (policy_uri.startsWith("file:${java.home}/") ||
			    policy_uri.startsWith("file:${user.home}/")) {
			    // this special case accommodates
			    // the situation java.home/user.home
			    // expand to a single slash, resulting in
			    // a file://foo URI
			    policy_url = new File
				(expanded_uri.substring(5)).toURI().toURL();
			} else {
			    policy_url = new URI(expanded_uri).toURL();
			}
			if ("file".equals(policy_url.getProtocol())) {
			    String path = policy_url.getFile().
				replace('/', File.separatorChar);
			    path = sun.net.www.ParseUtil.decode(path);
			    File policyFile = new File(path);
			    if (policyFile.exists()) {
				sum += policyFile.lastModified();
				if (logger.isLoggable(Level.FINE)) {
				    logMsg(Level.FINE,"pc.file_refreshed",
					   new Object[]{path},null);
				}			
			    } else {
				if (logger.isLoggable(Level.FINE)) {
				    logMsg(Level.FINE,"pc.file_not_refreshed",
					   new Object[]{path},null);
				}
			    }
			} else {
			    if (logger.isLoggable(Level.FINE)) {
				logMsg(Level.FINE,"pc.file_not_refreshed",
				       new Object[]{policy_url},null);
			    }
			}
		    } catch (Exception e) {
			// ignore that policy
		    }
		    n++;
		}
		return Long.valueOf(sum);
	    }
	});
	return l.longValue();
    }
    
    // obtains PolicyConfigurationFactory 
    private PolicyConfigurationFactoryImpl getPolicyFactory() {
       return PolicyConfigurationFactoryImpl.getInstance();
    }
}
