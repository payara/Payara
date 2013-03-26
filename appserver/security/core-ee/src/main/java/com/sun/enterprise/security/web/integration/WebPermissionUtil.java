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

package com.sun.enterprise.security.web.integration;

import java.util.*;
import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;
import javax.security.jacc.WebUserDataPermission;

import java.util.logging.*; 
import com.sun.logging.LogDomains;
import java.security.Permission;
import java.security.Permissions;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.web.*;
import org.glassfish.security.common.Role;

/**
 * This class is used for generating Web permissions based on the 
 * deployment descriptor.
 * @author Harpreet Singh
 * @author Jean-Francois Arcand
 * @author Ron Monzillo
 */
public class WebPermissionUtil {

    static Logger logger = Logger.getLogger(LogDomains.SECURITY_LOGGER);
    
    public WebPermissionUtil() {
    }
    
    /* changed to order default pattern / below extension */
    private static final int PT_DEFAULT      = 0;
    private static final int PT_EXTENSION    = 1;
    private static final int PT_PREFIX	     = 2;
    private static final int PT_EXACT 	     = 3;
      
    static int patternType(Object urlPattern) {
	String pattern = urlPattern.toString();
	if (pattern.startsWith("*.")) return PT_EXTENSION;
	else if (pattern.startsWith("/") && pattern.endsWith("/*")) 
	    return PT_PREFIX;
	else if (pattern.equals("/")) return PT_DEFAULT;
	else return PT_EXACT;
    }

    static boolean implies(String pattern, String path) {

        // Check for exact match
        if (pattern.equals(path))
            return (true);

        // Check for path prefix matching
        if (pattern.startsWith("/") && pattern.endsWith("/*")) {
            pattern = pattern.substring(0, pattern.length() - 2);

	    int length = pattern.length();

            if (length == 0) return (true);  // "/*" is the same as "/"

	    return (path.startsWith(pattern) && 
		    (path.length() == length || 
		     path.substring(length).startsWith("/")));
        }

        // Check for suffix matching
        if (pattern.startsWith("*.")) {
            int slash = path.lastIndexOf('/');
            int period = path.lastIndexOf('.');
            if ((slash >= 0) && (period > slash) &&
                path.endsWith(pattern.substring(1))) {
                return (true);
            }
            return (false);
        }

        // Check for universal mapping
        if (pattern.equals("/"))
            return (true);

        return (false);
    }

    public static HashMap parseConstraints(WebBundleDescriptor wbd)
    {
	
      if (logger.isLoggable(Level.FINE)){
	  logger.entering("WebPermissionUtil", "parseConstraints");
      }

      Set<Role> roleSet = wbd.getRoles();

      HashMap<String, MapValue> qpMap = new HashMap();

      /*
       * bootstrap the map with the default pattern; the default pattern will
       * not be "committed", unless a constraint is defined on "\". This will
       * ensure that a more restrictive constraint can be assigned to it
       */
      qpMap.put("/", new MapValue("/"));

      //Enumerate over security constraints
      Enumeration<SecurityConstraint> esc = wbd.getSecurityConstraints();
      while (esc.hasMoreElements()) {
	  
	  if (logger.isLoggable(Level.FINE)){
	      logger.log(Level.FINE,"JACC: constraint translation: begin parsing security constraint");
	  }

	  SecurityConstraint sc = esc.nextElement();
	  AuthorizationConstraint ac = sc.getAuthorizationConstraint();
	  UserDataConstraint udc = sc.getUserDataConstraint();

	  // Enumerate over collections of URLPatterns within constraint
	  for (WebResourceCollection wrc: sc.getWebResourceCollections()) {

	      if (logger.isLoggable(Level.FINE)){
		  logger.log(Level.FINE,"JACC: constraint translation: begin parsing web resource collection");
	      }

	      // Enumerate over URLPatterns within collection
	      for (String url: wrc.getUrlPatterns()) {
                  if (url != null) {
 		      // FIX TO BE CONFIRMED: encode all colons
 		      url = url.replaceAll(":","%3A");
 		  }

		  if (logger.isLoggable(Level.FINE)){
		      logger.log(Level.FINE,"JACC: constraint translation: process url: "+url);
		  }

		  // determine if pattern is already in map
		  MapValue mValue = qpMap.get(url);

		  // apply new patterns to map
		  if (mValue == null) {
		      mValue = new MapValue(url);

		      //Iterate over patterns in map
		      for(Map.Entry<String, MapValue> qpVal:qpMap.entrySet()) {

			  String otherUrl = qpVal.getKey();

			  int otherUrlType = patternType(otherUrl);
			  switch(patternType(url)) {

			      // if the new url/pattern is a path-prefix 
			      // pattern, it must be qualified by every 
			      // different (from it) path-prefix pattern 
			      // (in the map) that is implied by the new 
			      // pattern, and every exact pattern (in the map)
			      // that is implied by the new url.
			      // Also, the new pattern  must be added as a 
			      // qualifier of the default pattern, and every 
			      // extension pattern (existing in the map), and 
			      // of every different path-prefix pattern that 
			      // implies the new pattern.
			      // Note that we know that the new pattern does
			      // not exist in the map, thus we know that the
			      // new pattern is different from any existing
			      // path prefix pattern.
	     
			      case PT_PREFIX:
				  if ((otherUrlType == PT_PREFIX || 
				      otherUrlType == PT_EXACT) &&
				      implies(url,otherUrl))
				      mValue.addQualifier(otherUrl);
			  
				  else if (otherUrlType == PT_PREFIX &&
					   implies(otherUrl,url))
				      qpVal.getValue().addQualifier(url);
				  
				  else if (otherUrlType == PT_EXTENSION ||
				       otherUrlType == PT_DEFAULT)
				      qpVal.getValue().addQualifier(url);
				  break;

			      // if the new pattern is an extension pattern,
			      // it must be qualified by every path-prefix
			      // pattern (in the map), and every exact
			      // pattern (in the map) that is implied by
			      // the new pattern.
			      // Also, it must be added as a qualifier of
			      // the defualt pattern, if it exists in the
			      // map.
			      case PT_EXTENSION:
				  if (otherUrlType == PT_PREFIX || 
				       (otherUrlType == PT_EXACT &&
					implies(url,otherUrl)))
				      mValue.addQualifier(otherUrl);

				  else if (otherUrlType == PT_DEFAULT)
				      qpVal.getValue().addQualifier(url);
				  break;

			      // if the new pattern is the default pattern
			      // it must be qualified by every other pattern
			      // in the map.
			      case PT_DEFAULT:
				  if (otherUrlType != PT_DEFAULT) 
				      mValue.addQualifier(otherUrl);
				  break;

			      // if the new pattern is an exact pattern, it
			      // is not be qualified, but it must be added as 
			      // as a qualifier to the default pattern, and to
			      // every path-prefix or extension pattern (in 
			      // the map) that implies the new pattern.
			      case PT_EXACT:
				  if ((otherUrlType == PT_PREFIX || 
				       otherUrlType == PT_EXTENSION) &&
				      implies(otherUrl,url))
				      qpVal.getValue().addQualifier(url);
				  else if (otherUrlType == PT_DEFAULT)
				      qpVal.getValue().addQualifier(url);
				  break;
                              default : break;
			  }
		      }

		      // add the new pattern and its pattern spec to the map
		      qpMap.put(url, mValue);

		  }

		  String[] methodNames = wrc.getHttpMethodsAsArray();
		  BitSet methods = MethodValue.methodArrayToSet(methodNames);

		  BitSet omittedMethods = null;

		  if (methods.isEmpty()) {
		      String[] omittedNames = 
			  wrc.getHttpMethodOmissionsAsArray();
		      omittedMethods = 
			  MethodValue.methodArrayToSet(omittedNames);
		  }

		  // set and commit the method outcomes on the pattern
		  // note that an empty omitted method set is used to represent
		  // the set of all http methods

		  mValue.setMethodOutcomes(roleSet,ac,udc,methods,omittedMethods);

		  if (logger.isLoggable(Level.FINE)){
		      logger.log(Level.FINE,"JACC: constraint translation: end processing url: "+url);
		  }
	      }

	      if (logger.isLoggable(Level.FINE)){
		  logger.log(Level.FINE,"JACC: constraint translation: end parsing web resource collection");
	      }
	  }
	  if (logger.isLoggable(Level.FINE)){
	      logger.log(Level.FINE,"JACC: constraint translation: end parsing security constraint");
	  }
      }

      if (logger.isLoggable(Level.FINE)){
	  logger.exiting("WebPermissionUtil","parseConstraints");
      }

        return qpMap;
    }	

    static void handleExcluded(Permissions collection, MapValue m, String name) {
	String actions = null;
	BitSet excludedMethods = m.getExcludedMethods();
	if (m.otherConstraint.isExcluded()) {
	    BitSet methods = m.getMethodSet();
	    methods.andNot(excludedMethods);
	    if (!methods.isEmpty()) {
		actions = "!" + MethodValue.getActions(methods);
	    }
	} else if (!excludedMethods.isEmpty()) { 
	    actions = MethodValue.getActions(excludedMethods);
	} else {
	    return;
	}

	collection.add(new WebResourcePermission(name,actions));
	collection.add(new WebUserDataPermission(name,actions));

	if (logger.isLoggable(Level.FINE)){
	    logger.log(Level.FINE,"JACC: constraint capture: adding excluded methods: "+ actions);
	}
    }

    static Permissions addToRoleMap(HashMap<String, Permissions> map,
				    String roleName, Permission p) {
        Permissions collection = map.get(roleName);
	if (collection == null) {
	    collection = new Permissions();
	    map.put(roleName,collection);
	}
	collection.add(p);
	if (logger.isLoggable(Level.FINE)){
	    logger.log(Level.FINE,"JACC: constraint capture: adding methods to role: "+ roleName+" methods: " + p.getActions());
	}
	return collection;
    }
		
    static void handleRoles(HashMap<String,Permissions> map, MapValue m, 
			    String name) {
	HashMap<String,BitSet> rMap = m.getRoleMap();
	List<String> roleList = null;
	// handle the roles for the omitted methods
	if (!m.otherConstraint.isExcluded() && m.otherConstraint.isAuthConstrained()) {
	    roleList = m.otherConstraint.roleList;
	    for (String roleName : roleList) {
         	BitSet methods = m.getMethodSet();
		//reduce ommissions for explicit methods granted to role  
		BitSet roleMethods = rMap.get(roleName);
		if (roleMethods != null) {
		    methods.andNot(roleMethods);
		}
		String actions = null;
		if (!methods.isEmpty()) {
		    actions = "!" + MethodValue.getActions(methods);
		}
		addToRoleMap(map,roleName,new WebResourcePermission(name,actions)); 
	    }
	}
	//handle explicit methods, skip roles that were handled above 
	BitSet methods = m.getMethodSet();
	if (!methods.isEmpty()) {
	    for (Map.Entry<String,BitSet> rval:rMap.entrySet()) {
		String roleName = rval.getKey();
		if (roleList == null || !roleList.contains(roleName)) {
		    BitSet roleMethods = rval.getValue();
		    if (!roleMethods.isEmpty()) {
			String actions = MethodValue.getActions(roleMethods);
		    	addToRoleMap(map,roleName,new WebResourcePermission(name,actions));
		    }
		}
	    }
	}
    }

    static void handleNoAuth(Permissions collection, MapValue m, 
			     String name) {
	String actions = null;
	BitSet noAuthMethods = m.getNoAuthMethods();
	if (!m.otherConstraint.isAuthConstrained()) {
	    BitSet methods = m.getMethodSet();
	    methods.andNot(noAuthMethods);
	    if (!methods.isEmpty()) {
		actions = "!" + MethodValue.getActions(methods);
	    }
	} else if (!noAuthMethods.isEmpty()) { 
	    actions = MethodValue.getActions(noAuthMethods);
	} else {
	    return;
	}

	collection.add(new WebResourcePermission(name,actions));

	if (logger.isLoggable(Level.FINE)){
	    logger.log(Level.FINE,"JACC: constraint capture: adding unchecked (for authorization) methods: "+ actions);
	}
    }

    static void handleConnections(Permissions collection, MapValue m, 
				  String name) {
	BitSet allConnectMethods = null;
	boolean allConnectAtOther = m.otherConstraint.isConnectAllowed
	    (ConstraintValue.connectTypeNone);

	for (int i=0; i<ConstraintValue.connectKeys.length; i++) {

	    String actions = null;
	    String transport = ConstraintValue.connectKeys[i];

	    BitSet connectMethods = m.getConnectMap(1<<i);
	    if (i == 0) {
		allConnectMethods = connectMethods;
	    } else {
 		/* if connect type protected, remove methods 
 		 * that accept any connect
 		 */
 		connectMethods.andNot(allConnectMethods);
  	    }
            
	    if (m.otherConstraint.isConnectAllowed(1<<i)) {
		if (i != 0 && allConnectAtOther) {
		    /* if all connect allowed at other
  		     */
		    if (connectMethods.isEmpty()) {
			/* skip, if remainder is empty, because methods
			 * that accept any connect were handled at i==0. 
			 */
			continue;
		    }
		    /* construct actions using methods with specific 
		     * connection requirements
		     */
		    actions = MethodValue.getActions(connectMethods) ;
		} else {
		    BitSet methods = m.getMethodSet();
		    methods.andNot(connectMethods);
		    if (!methods.isEmpty()) {
			actions = "!" + MethodValue.getActions(methods);
		    }
		}
	    } else if (!connectMethods.isEmpty()) {
		actions = MethodValue.getActions(connectMethods) ;
	    } else {
		continue;
	    }
	    
	    actions = (actions == null) ? "" : actions;
	    String combinedActions = actions + ":" + transport; 

	    collection.add(new WebUserDataPermission(name,combinedActions));

	    if (logger.isLoggable(Level.FINE)){
		logger.log(Level.FINE,"JACC: constraint capture: adding methods that accept connections with protection: "+ transport +" methods: "+ actions);
	    }
	}
    }

    /**
     * Remove All Policy Statements from Configuration
     * config must be in open state when this method is called
     * @param pc
     * @param wbd
     * @throws javax.security.jacc.PolicyContextException
     */
    public static void removePolicyStatements(PolicyConfiguration pc,
            WebBundleDescriptor wbd)
            throws javax.security.jacc.PolicyContextException {

        pc.removeUncheckedPolicy();
        pc.removeExcludedPolicy();
        // iteration done for old providers
        Set<Role> roleSet = wbd.getRoles();
        for (Role r : roleSet) {
            pc.removeRole(r.getName());
        }
        // 1st call will remove "*" role if present. 2nd will remove all roles (if supported).
 	pc.removeRole("*");
 	pc.removeRole("*");
    }
    
    public static void processConstraints(WebBundleDescriptor wbd,
					  PolicyConfiguration pc)
    throws javax.security.jacc.PolicyContextException 
    {
	if (logger.isLoggable(Level.FINE)){
	    logger.entering("WebPermissionUtil", "processConstraints");
	    logger.log(Level.FINE,"JACC: constraint translation: CODEBASE = "+
		       pc.getContextID());
	}

	HashMap qpMap = parseConstraints(wbd);
	HashMap<String,Permissions> roleMap = 
	    new HashMap<String,Permissions>();

	Permissions excluded = new Permissions();
	Permissions unchecked = new Permissions();

	boolean deny = wbd.isDenyUncoveredHttpMethods();
	if (logger.isLoggable(Level.FINE)){
	    logger.log(Level.FINE,"JACC: constraint capture: begin processing qualified url patterns"
	    		+ " - uncovered http methods will be " + (deny ? "denied" : "permitted"));
	}
 
	// for each urlPatternSpec in the map
	Iterator it = qpMap.values().iterator();
	while (it.hasNext()) {
	    MapValue m = (MapValue) it.next();
	    if (!m.irrelevantByQualifier) {

		String name = m.urlPatternSpec.toString();

		if (logger.isLoggable(Level.FINE)){
		    logger.log(Level.FINE,"JACC: constraint capture: urlPattern: "+ name);
		}

		// handle Uncovered Methods
		m.handleUncoveredMethods(deny);

		// handle excluded methods
		handleExcluded(excluded,m,name);

		// handle methods requiring role
		handleRoles(roleMap,m,name);

		// handle methods that are not auth constrained
		handleNoAuth(unchecked,m,name);

		// handle transport constraints 
		handleConnections(unchecked,m,name);
	    }
	}

	if (logger.isLoggable(Level.FINE)){
	    logger.log(Level.FINE,"JACC: constraint capture: end processing qualified url patterns");

	    Enumeration e = excluded.elements();
	    while (e.hasMoreElements()) {
		Permission p = (Permission) e.nextElement();
		String ptype = (p instanceof WebResourcePermission) ? "WRP  " : "WUDP ";
		logger.log(Level.FINE,"JACC: permission(excluded) type: "+ ptype + " name: "+ p.getName() + " actions: "+ p.getActions());
	    }

	    e = unchecked.elements();
	    while (e.hasMoreElements()) {
		Permission p = (Permission) e.nextElement();
		String ptype = (p instanceof WebResourcePermission) ? "WRP  " : "WUDP ";
		logger.log(Level.FINE,"JACC: permission(unchecked) type: "+ ptype + " name: "+ p.getName() + " actions: "+ p.getActions());
	    }
	}
	
	pc.addToExcludedPolicy(excluded);

	pc.addToUncheckedPolicy(unchecked);

	for (Map.Entry<String,Permissions> rVal : roleMap.entrySet()) {
	    String role = rVal.getKey();
	    Permissions pCollection = rVal.getValue();
	    pc.addToRole(role,pCollection);

	    if (logger.isLoggable(Level.FINE)){
		Enumeration e = pCollection.elements();
		while (e.hasMoreElements()) {
		    Permission p = (Permission) e.nextElement();
		    String ptype = (p instanceof WebResourcePermission) ? "WRP  " : "WUDP ";
		    logger.log(Level.FINE,"JACC: permission("+ role + ") type: "+ ptype + " name: "+ p.getName() + " actions: "+ p.getActions());
		}

	    }
	}

	if (logger.isLoggable(Level.FINE)){
	    logger.exiting("WebPermissionUtil", "processConstraints");
	}

    }
      
    public static void createWebRoleRefPermission(WebBundleDescriptor wbd, 
						  PolicyConfiguration pc)
	throws javax.security.jacc.PolicyContextException 
    {
	if (logger.isLoggable(Level.FINE)){
	    logger.entering("WebPermissionUtil", "createWebRoleRefPermission");
	    logger.log(Level.FINE,"JACC: role-reference translation: Processing WebRoleRefPermission : CODEBASE = "+ pc.getContextID());
	}
	List role = new ArrayList();
	Set roleset = wbd.getRoles();
	Role anyAuthUserRole = new Role("**");
	boolean rolesetContainsAnyAuthUserRole = roleset.contains(anyAuthUserRole);
        Set<WebComponentDescriptor> descs = wbd.getWebComponentDescriptors();
	//V3 Commented for(Enumeration e = wbd.getWebComponentDescriptors(); e.hasMoreElements();){
        for (WebComponentDescriptor comp : descs) {
	    //V3 Commented WebComponentDescriptor comp = (WebComponentDescriptor) e.nextElement();

	    String name = comp.getCanonicalName();
	    Enumeration  esrr = comp.getSecurityRoleReferences();

	    for (; esrr.hasMoreElements();){
		SecurityRoleReference srr = (SecurityRoleReference)esrr.nextElement();
		if(srr != null){
		    String action = srr.getRoleName();
		    WebRoleRefPermission wrrp = new WebRoleRefPermission(name, action);
		    role.add(new Role(action));
		    pc.addToRole(srr.getSecurityRoleLink().getName(),wrrp);
		    if (logger.isLoggable(Level.FINE)){
			logger.log(Level.FINE,"JACC: role-reference translation: RoleRefPermission created with name(servlet-name)  = "+ name  + 
				   " and action(Role-name tag) = " + action + " added to role(role-link tag) = "+ srr.getSecurityRoleLink().getName());
		    }

		}
	    }
	    if (logger.isLoggable(Level.FINE)){
		logger.log(Level.FINE,"JACC: role-reference translation: Going through the list of roles not present in RoleRef elements and creating WebRoleRefPermissions ");
	    }
	    for(Iterator it = roleset.iterator(); it.hasNext();){
		Role r = (Role)it.next();
		if (logger.isLoggable(Level.FINE)){
		    logger.log(Level.FINE,"JACC: role-reference translation: Looking at Role =  "+r.getName());
		}
		if(!role.contains(r)){
		    String action = r.getName();
		    WebRoleRefPermission wrrp = new WebRoleRefPermission(name, action);
		    pc.addToRole(action ,wrrp);
		    if (logger.isLoggable(Level.FINE)){
			logger.log(Level.FINE,"JACC: role-reference translation: RoleRef  = "+ action + 
				   " is added for servlet-resource = " + name);
			logger.log(Level.FINE, "JACC: role-reference translation: Permission added for above role-ref =" 
				   + wrrp.getName() +" "+ wrrp.getActions());
		    }
		}
	    }
        /**
         * JACC MR8 add WebRoleRefPermission for the any authenticated user role '**'
         */
        if ((!role.contains(anyAuthUserRole)) && !rolesetContainsAnyAuthUserRole) {
            addAnyAuthenticatedUserRoleRef(pc, name);
        }
	}
	if (logger.isLoggable(Level.FINE)){
	    logger.exiting("WebPermissionUtil", "createWebRoleRefPermission");
	}
        
        // START S1AS8PE 4966609
        /**
         * For every security role in the web application add a
         * WebRoleRefPermission to the corresponding role. The name of all such
         * permissions shall be the empty string, and the actions of each 
         * permission shall be the corresponding role name. When checking a 
         * WebRoleRefPermission from a JSP not mapped to a servlet, use a 
         * permission with the empty string as its name
         * and with the argument to isUserInRole as its actions
         */
        for(Iterator it = roleset.iterator(); it.hasNext();){
            Role r = (Role)it.next();
            if (logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE,
                    "JACC: role-reference translation: Looking at Role =  "
                        + r.getName());
            }
            String action = r.getName();
            WebRoleRefPermission wrrp = new WebRoleRefPermission("", action);
            pc.addToRole(action ,wrrp);
            if (logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE,
                    "JACC: role-reference translation: RoleRef  = "
                    + action 
                    + " is added for jsp's that can't be mapped to servlets");
                logger.log(Level.FINE, 
                    "JACC: role-reference translation: Permission added for above role-ref =" 
                     + wrrp.getName() +" "+ wrrp.getActions());
            }
        }
        // END S1AS8PE 4966609
        /**
         * JACC MR8 add WebRoleRefPermission for the any authenticated user role '**'
         */
        if (!rolesetContainsAnyAuthUserRole) {
            addAnyAuthenticatedUserRoleRef(pc, "");
        }
    }

    /**
     * JACC MR8 add WebRoleRefPermission for the any authenticated user role '**'
     */
    private static void addAnyAuthenticatedUserRoleRef(PolicyConfiguration pc, String name)
    		throws javax.security.jacc.PolicyContextException {
    	String action = "**";
    	WebRoleRefPermission wrrp = new WebRoleRefPermission(name, action);
    	pc.addToRole(action ,wrrp);
    	if (logger.isLoggable(Level.FINE)){
    		logger.log(Level.FINE, 
    				"JACC: any authenticated user role-reference translation: Permission added for role-ref =" 
    						+ wrrp.getName() +" "+ wrrp.getActions());
    	}
    }
}

class ConstraintValue {

    static String connectKeys[] = 
    { "NONE",
      "INTEGRAL",
      "CONFIDENTIAL"
    };

    static int connectTypeNone = 1;
    static HashMap<String, Integer> connectHash = new HashMap<String, Integer>();
    static 
    {
	for (int i=0; i<connectKeys.length; i++)
	    connectHash.put(connectKeys[i], Integer.valueOf(1<<i));
    };

    boolean excluded;
    boolean ignoreRoleList;
    final List<String> roleList = new ArrayList<String>();
    int connectSet;

    ConstraintValue() {
	excluded = false;
	ignoreRoleList = false;
	//roleList = new ArrayList<String>();
	connectSet = 0;
    }
	
    static boolean bitIsSet(int map , int bit) {
        return (map & bit) == bit ? true : false;
    }

    void setRole(String role) {
	synchronized(roleList) {
	    if (!roleList.contains(role)) {
		roleList.add(role);
	    }
	}
    }   

    void removeRole(String role) {
	synchronized(roleList) {
	    if (roleList.contains(role)) {
		roleList.remove(role);
	    }
	}
    }   

    void setPredefinedOutcome(boolean outcome) {
	if (!outcome) {
	    excluded = true;
	} else {
	    ignoreRoleList = true;
	}
    }

    void addConnectType(String guarantee) {
	int b = connectTypeNone;
	if (guarantee != null) {
	    Integer bit = connectHash.get(guarantee);
	    if (bit == null) 
		throw new IllegalArgumentException
		    ("constraint translation error-illegal trx guarantee");
	    b = bit.intValue();
	}

	connectSet |= b;
    }

    boolean isExcluded() {
	return excluded;
    }

    /* ignoreRoleList is true if  there was a security-constraint
     * without an auth-constraint; such a constraint combines to
     * allow access without authentication.
     */
    boolean isAuthConstrained() {
	if (excluded) {
	    return true;
	} else if (ignoreRoleList || roleList.isEmpty()) {
	    return false;
	} 
	return true;
    }

    boolean isTransportConstrained() {
	if (excluded || (connectSet != 0 &&
			 !bitIsSet(connectSet,connectTypeNone))) {
	    return true;
	}
	return false;
    }

    boolean isConnectAllowed(int cType) {
	if (!excluded && (connectSet == 0 ||
			  bitIsSet(connectSet,connectTypeNone) ||
			  bitIsSet(connectSet,cType))) {
	    return true;
	}
	return false;
    }

    void setOutcome(Set<Role> roleSet,
		    AuthorizationConstraint ac, UserDataConstraint udc) {
	if (ac == null) {
	    setPredefinedOutcome(true);
	} else {
	    boolean containsAllRoles = false;
	    Enumeration eroles = ac.getSecurityRoles();
	    if (!eroles.hasMoreElements()) {
		setPredefinedOutcome(false);
	    }
	    else while (eroles.hasMoreElements()) {
		SecurityRoleDescriptor srd = 
		    (SecurityRoleDescriptor)eroles.nextElement();
		String roleName = srd.getName();
		if ("*".equals(roleName)) {
			containsAllRoles = true;
		} else {
		    setRole(roleName);
		}
	    }
	    /**
	     * JACC MR8  When role '*' named, do not include any authenticated user
	     * role '**' unless an application defined a role named '**'
	     */
	    if (containsAllRoles) {
		    removeRole("**");
		    Iterator it = roleSet.iterator();
		    while(it.hasNext()) {
			setRole(((Role)it.next()).getName());
		    }
	    }
	}
	addConnectType(udc == null? null :  udc.getTransportGuarantee());

	if (WebPermissionUtil.logger.isLoggable(Level.FINE)){
	    WebPermissionUtil.logger.log
		(Level.FINE,"JACC: setOutcome yields: " + toString());
	}

    }

    void setValue(ConstraintValue constraint) {
	excluded = constraint.excluded;
	ignoreRoleList = constraint.ignoreRoleList;
	roleList.clear();
	Iterator rit = constraint.roleList.iterator();
	while(rit.hasNext()) {
	    String role = (String) rit.next();
	    roleList.add(role);
	}
	connectSet = constraint.connectSet;
    }

    public String toString() {
	StringBuilder roles =new StringBuilder(" roles: ");
	Iterator rit = roleList.iterator();
	while(rit.hasNext()) {
	    roles.append(" ").append((String) rit.next());
	}
	StringBuilder transports = new StringBuilder("transports: ");
	for (int i=0; i<connectKeys.length; i++) {
	    if (isConnectAllowed(1<<i)) {
		transports.append(" ").append(connectKeys[i]);
	    }
	}
	return " ConstraintValue ( " + 
	    " excluded: " + excluded +
	    " ignoreRoleList: " + ignoreRoleList + roles + transports + " ) ";
    }

    /* ignoreRoleList is true if there was a security-constraint
     * without an auth-constraint; such a constraint combines to 
     * allow access without authentication.
     */
    boolean isUncovered() {
        return (!excluded && !ignoreRoleList && roleList.isEmpty() && connectSet == 0);
    }
}

class MethodValue extends ConstraintValue {

    private static final ArrayList<String> methodNames = new ArrayList();

    int index;

    MethodValue (String methodName) {
	index = getMethodIndex(methodName);
    } 

    MethodValue (String methodName, ConstraintValue constraint) {
	index = getMethodIndex(methodName);
	setValue(constraint);
    } 

    static String getMethodName(int index) {
	synchronized(methodNames) {
	    return methodNames.get(index);
	}
    }

    static int getMethodIndex(String name) {
	synchronized(methodNames) {
	    int index = methodNames.indexOf(name);
	    if (index < 0) {
		index = methodNames.size();
		methodNames.add(index,name);
	    }	
	    return index;
	}
    }

    static String getActions (BitSet methodSet) {
	if (methodSet == null || methodSet.isEmpty()) {
	    return null;
	}
	    
	StringBuffer actions = null;

	for (int i=methodSet.nextSetBit(0); i>=0; i=methodSet.nextSetBit(i+1)){
	    if (actions == null) {
		actions = new StringBuffer();
	    } else {
		actions.append(",");
	    }
	    actions.append(getMethodName(i));
	}

	return (actions == null ? null : actions.toString());
    }

    static String[] getMethodArray (BitSet methodSet) {
	if (methodSet == null || methodSet.isEmpty()) {
	    return null;
	}
	    
	int size = 0;

	ArrayList<String> methods = new ArrayList();

	for (int i=methodSet.nextSetBit(0); i>=0; i=methodSet.nextSetBit(i+1)){
	    methods.add(getMethodName(i));
	    size += 1;
	}

	return (String[]) methods.toArray(new String[size]);
    }

    static BitSet methodArrayToSet(String[] methods) {
	BitSet methodSet = new BitSet();

	for (int i=0; methods != null && i<methods.length; i++) {
	    if (methods[i] == null) {
		throw new IllegalArgumentException
		    ("constraint translation error - null method name");
	    }
	    int bit = getMethodIndex(methods[i]);
	    methodSet.set(bit);
	}

	return methodSet;
    }

    public String toString() {
	return "MethodValue( " + getMethodName(index) +
	    super.toString() + " )";
    }
}

class MapValue {

    boolean committed;
 
    int patternType;

    int patternLength;

    boolean irrelevantByQualifier;

    StringBuffer urlPatternSpec;

    final HashMap<String,MethodValue> methodValues =
        new HashMap<String,MethodValue>();

    ConstraintValue otherConstraint; 

    MapValue (String urlPattern) {
	this.committed = false;
	this.patternType = WebPermissionUtil.patternType(urlPattern);
	this.patternLength = urlPattern.length();
	this.irrelevantByQualifier = false;
	this.urlPatternSpec = new StringBuffer(urlPattern);
	otherConstraint = new ConstraintValue();
    } 

    void addQualifier(String urlPattern) {
	if (WebPermissionUtil.implies(urlPattern,
		    this.urlPatternSpec.substring(0,this.patternLength)))
	    this.irrelevantByQualifier = true;
	this.urlPatternSpec.append(":" + urlPattern);
    }

    MethodValue getMethodValue(int methodIndex) {
	String methodName = MethodValue.getMethodName(methodIndex);
	synchronized(methodValues) {
	    MethodValue methodValue = methodValues.get(methodName);
	    if (methodValue == null) {
		methodValue = new MethodValue(methodName,otherConstraint);
		methodValues.put(methodName,methodValue);

		if (WebPermissionUtil.logger.isLoggable(Level.FINE)){
		    WebPermissionUtil.logger.log
			(Level.FINE,"JACC: created MethodValue: " +
			 methodValue); 
		}
	    }
	    return methodValue;
	}
    }

    BitSet getExcludedMethods() {
        BitSet methodSet = new BitSet();

        synchronized (methodValues) {

            Collection<MethodValue> values = methodValues.values();

            for (MethodValue v : values) {
                if (v.isExcluded()) {
                    methodSet.set(v.index);
                }
            }
        }
        return methodSet;
    }

    BitSet getNoAuthMethods() {
	BitSet methodSet = new BitSet();

	synchronized(methodValues) {

	    Collection<MethodValue> values = methodValues.values();
	    for(MethodValue v : values){
		if (!v.isAuthConstrained()) {
		    methodSet.set(v.index);
		}
	    }
	}
	return methodSet;
    }

    BitSet getAuthConstrainedMethods() {
        BitSet methodSet = new BitSet();

        synchronized (methodValues) {

            Collection<MethodValue> values = methodValues.values();

            for (MethodValue v : values) {
                if (v.isAuthConstrained()) {
                    methodSet.set(v.index);
                }
            }
        }
        return methodSet;
    }

    BitSet getTransportConstrainedMethods() {
        BitSet methodSet = new BitSet();

        synchronized (methodValues) {

            Collection<MethodValue> values = methodValues.values();

            for (MethodValue v : values) {
                if (v.isTransportConstrained()) {
                    methodSet.set(v.index);
                }
            }
        }
        return methodSet;
    }

    /**
     * Map of methods allowed per role
     */
    HashMap<String,BitSet> getRoleMap() {
        HashMap<String,BitSet> roleMap = new HashMap<String,BitSet>();

        synchronized (methodValues) {

            Collection<MethodValue> values = methodValues.values();

            for (MethodValue v : values) {
                if (!v.isExcluded() && v.isAuthConstrained()) {
                    for (String role : v.roleList) {
                        BitSet methodSet = roleMap.get(role);

                        if (methodSet == null) {
                            methodSet = new BitSet();
                            roleMap.put(role, methodSet);
                        }

                        methodSet.set(v.index);
                    }
                }
            }
        }

        return roleMap;
    }

    BitSet getConnectMap(int cType) {
        BitSet methodSet = new BitSet();

        synchronized (methodValues) {

            Collection<MethodValue> values = methodValues.values();
            for (MethodValue v : values) {
                /*
                 * NOTE WELL: prior version of this method
                 * could not be called during constraint parsing
                 * because it finalized the connectSet when its
                 * value was 0 (indicating any connection, until
                 * some specific bit is set)
                 *
                if (v.connectSet == 0) {
                v.connectSet = MethodValue.connectTypeNone;
                }

                 */

                if (v.isConnectAllowed(cType)) {
                    methodSet.set(v.index);
                }
            }
        }

        return methodSet;
    }

    BitSet getMethodSet() {
        BitSet methodSet = new BitSet();

        synchronized (methodValues) {

            Collection<MethodValue> values = methodValues.values();
            for (MethodValue v : values) {
                methodSet.set(v.index);
            }
        }

        return methodSet;
    }

    void setMethodOutcomes(Set<Role> roleSet,
			   AuthorizationConstraint ac, UserDataConstraint udc, 
			   BitSet methods,BitSet omittedMethods) {

	committed = true;

	if (omittedMethods != null) {
	    
	    // get the ommitted methodSet
	    BitSet methodsInMap = getMethodSet();

	    BitSet saved = (BitSet) omittedMethods.clone();

	    // determine methods being newly omitted
	    omittedMethods.andNot(methodsInMap);

	    // create values for newly omitted, init from otherConstraint
	    for (int i = omittedMethods.nextSetBit(0); i >= 0; 
		 i = omittedMethods.nextSetBit(i+1)) {
		getMethodValue(i);
	    }
	    
	    //combine this constraint into constraint on all other methods
	    otherConstraint.setOutcome(roleSet,ac,udc);

	    methodsInMap.andNot(saved);

	    // recursive call to combine constraint into prior omitted methods 
	    setMethodOutcomes(roleSet,ac,udc,methodsInMap,null);
	    
	} else {

	    for (int i = methods.nextSetBit(0); i >= 0; 
		 i = methods.nextSetBit(i+1)){
		// create values (and init from otherConstraint) if not in map
		// then combine with this constraint.
		getMethodValue(i).setOutcome(roleSet,ac,udc);
	    }
	}
    }

    void handleUncoveredMethods(boolean deny) {
    	/*
    	 * bypass any uncommitted patterns (e.g. the default pattern) which were
    	 * entered in the map, but that were not named in a security constraint
    	 */
    	if (!committed) {
    		return;
    	}

    	boolean otherIsUncovered = false;
    	synchronized (methodValues) {
    		BitSet uncoveredMethodSet = new BitSet();
    		// for all the methods in the mapValue
    		for (MethodValue v : methodValues.values()) {
    			// if the method is uncovered add its id to the uncovered set
    			if (v.isUncovered()) {
    				if (deny) {
    					v.setPredefinedOutcome(false);
    				}
    				uncoveredMethodSet.set(v.index);
    			}
    		}
    		// if the constraint on all other methods is uncovered
    		if (otherConstraint.isUncovered()) {
    			/*
    			 * this is the case where the problem is most severe, since
    			 * a non-enumerble set of http methods has been left uncovered.
    			 * the set of method  will be logged and denied.
    			 */
    			otherIsUncovered = true;
    			if (deny) {
    				otherConstraint.setPredefinedOutcome(false);
    			}
    			/*
    			 * ensure that the methods that are reported as uncovered
    			 * includes any enumerated methods that were found to be uncovered.
    			 */
    			BitSet otherMethodSet = getMethodSet();
    			if (!uncoveredMethodSet.isEmpty()) {
    				/*
    				 * uncoveredMethodSet contains methods that otherConstraint
    				 * pertains to, so remove them from otherMethodSet which 
    				 * is the set to which the otherConstraint does not apply
    				 */
    				otherMethodSet.andNot(uncoveredMethodSet);
    			}
    			/*
    			 * when otherIsUncovered, uncoveredMethodSet contains methods to
    			 * which otherConstraint does NOT apply
    			 */
    			uncoveredMethodSet = otherMethodSet;
    		}
    		if (otherIsUncovered || !uncoveredMethodSet.isEmpty()) {
    			String uncoveredMethods = MethodValue.getActions(uncoveredMethodSet);
    			Object[] args = new Object[] {urlPatternSpec, uncoveredMethods};
    			if (deny) {
        			if (otherIsUncovered) {
        				WebPermissionUtil.logger.log(Level.INFO,
        						"JACC: For the URL pattern {0}, all but the following methods have been excluded: {1}", args);
        			}
        			else {
        				WebPermissionUtil.logger.log(Level.INFO,
        						"JACC: For the URL pattern {0}, the following methods have been excluded: {1}", args);
        			}
    			}
    			else {
        			if (otherIsUncovered) {
        				WebPermissionUtil.logger.log(Level.WARNING,
        						"JACC: For the URL pattern {0}, all but the following methods were uncovered: {1}", args);
        			}
        			else {
        				WebPermissionUtil.logger.log(Level.WARNING,
        						"JACC: For the URL pattern {0}, the following methods were uncovered: {1}", args);
        			}
    			}
    		}
    	}
    }
}




