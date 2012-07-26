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

package com.sun.enterprise.security.ee;

import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

//V3:Commented import com.sun.enterprise.config.serverbeans.ServerBeansFactory;

import com.sun.logging.LogDomains;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.MethodPermission;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.RunAsIdentityDescriptor;
import com.sun.enterprise.deployment.EjbIORConfigurationDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.web.SecurityConstraint;
import com.sun.enterprise.deployment.web.AuthorizationConstraint;
import com.sun.enterprise.deployment.web.LoginConfiguration;
import com.sun.enterprise.deployment.web.UserDataConstraint;
import com.sun.enterprise.deployment.web.SecurityRole;
import com.sun.enterprise.deployment.web.WebResourceCollection;
import com.sun.enterprise.deployment.WebComponentDescriptor;

import org.glassfish.security.common.Role;
import org.glassfish.deployment.common.SecurityRoleMapper;
import javax.servlet.http.HttpServletRequest;

import com.sun.appserv.security.AuditModule;

/**
 * Audit support class.
 *
 * <P>This class provides convenience methods for producing audit output.
 * Audit output is logged using the standard iAS logger SECURITYLOGGER.
 * However, audit output is only produced if auditing is active. Auditing
 * is configured in server.xml in the security-service element.
 *
 * <P>Audit output if logged with Level.WARNING.
 *
 * <P>Some diagnostic methods are also provided for debugging.
 *
 */
public class Audit extends AuditModule
{
    private static final String AUDIT_ON = "auditOn";
    private static boolean auditFlag = false;
    private static Logger logger =
        LogDomains.getLogger(Audit.class, LogDomains.SECURITY_LOGGER);
    /*
    private static String strPrivateAudit = null;
    private static String strDenied = null;
    private static String strOK = null;
    private static String strMethodName = null;
    private static String strSession = null;
    */
    
    /**
     * Check auditing state.
     *
     * @returns True if auditing is active currently.
     *
     */
    public static boolean isActive()
    {
        return auditFlag;
    }

    public void init(Properties props) {
        super.init(props);
        String audit = props.getProperty(AUDIT_ON);
        auditFlag = (audit == null)?false: Boolean.valueOf(audit).booleanValue();
    }
    
    /**
     * Invoked post authentication request for a user in a given realm
     * @param user username for whom the authentication request was made
     * @param realm the realm name under which the user is authenticated.
     * @param success the status of the authentication
     */
    public void authentication(String user, String realm, boolean success) {
        if (auditFlag) {
            StringBuffer sbuf = new StringBuffer("Audit: Authentication for user = (");
            sbuf.append(user); 
            sbuf.append(") under realm = (");
            sbuf.append(realm).append(") returned = ").append(success);
            logger.log(Level.INFO, sbuf.toString());
        }
    }
    
    /**
     * Invoked post web authorization request. 
     * @param user the username for whom the authorization was performed
     * @param req the HttpRequest object for the web request
     * @param type either hasResourcePermission, hasUserDataPermission or 
     * hasRoleRefPermission
     * @param success the status of the web authorization request
     */
    public void webInvocation(String user, HttpServletRequest req,
            String type, boolean success) 
    {
        if (auditFlag){
            StringBuilder sbuf = new StringBuilder("Audit: [Web] Authorization for user = (");
            sbuf.append(user).append(") and permission type = (").append(type).append(") for request ");
            sbuf.append(req.getMethod()).append(" ").append(req.getRequestURI()).append(" returned =").append(success);
            logger.log(Level.INFO, sbuf.toString());
        }
    }
    /**
     * Invoked post ejb authorization request.
     * @param user the username for whom the authorization was performed
     * @param ejb the ejb name for which this authorization was performed
     * @param method the method name for which this authorization was performed
     * @param success the status of the ejb authorization request
     */
    public void ejbInvocation(String user, String ejb, String method, boolean success) {
        if(auditFlag){
            // Modified from StringBuffer to StringBuilder
            StringBuilder sbuf = new StringBuilder("Audit: [EJB] Authorization for user =");
            sbuf.append(user).append(" for ejb = (");
            sbuf.append(ejb).append(") method = (").append(method).append(") returned =").append(success);
            logger.log(Level.INFO, sbuf.toString());
        }
    }

    /**
     * Invoked post ejb authorization request.
     * @param user the username for whom the authorization was performed
     * @param ejb the ejb name for which this authorization was performed
     * @param method the method name for which this authorization was performed
     * @param success the status of the ejb authorization request
     */

    /**
     * Invoked during validation of the web service request
     * @param uri The URL representation of the web service endpoint
     * @param endpoint The name of the endpoint representation
     * @param success the status of the web service request validation
     */
    public void webServiceInvocation(String uri, String endpoint, boolean success) {

        if(auditFlag){
            StringBuilder sbuf = new StringBuilder("Audit: [WebService] ");
            sbuf.append("uri: ").append(uri);
            sbuf.append("endpoint: ").append(endpoint);
            sbuf.append(", valid request =").append(success);
            logger.log(Level.INFO, sbuf.toString());
        }
    }


    /**
     * Invoked during validation of the web service request
     * @param endpoint The URL representation of the web service endpoint
     * @param success the status of the web service request validation
     */
    public void ejbAsWebServiceInvocation(String endpoint, boolean success) {

        if(auditFlag){
            StringBuilder sbuf = new StringBuilder("Audit: [EjbAsWebService] ");
            sbuf.append("endpoint : ").append(endpoint).append(", valid request =").append(success);
            logger.log(Level.INFO, sbuf.toString());
        }
    }


    /**
     * Invoked upon completion of the server startup
     */
    public void serverStarted() {
        if(auditFlag){
            logger.log(Level.INFO, "Audit: Application server startup complete");
        }
    }

    /**
     * Invoked upon completion of the server shutdown
     */
    public void serverShutdown() {
        if(auditFlag){
            logger.log(Level.INFO, "Audit: Application server shutdown complete");
        }
    }

    /**
     * Initialize auditing.  This reads the server.xml configuration to
     * determine whether audit is turned on or off.
     *
     */
    /*
    public static void init()
    {
        try {
            ConfigContext configContext =
                ApplicationServer.getServerContext().getConfigContext();
            assert(configContext != null);

            Server configBean =
                ServerBeansFactory.getServerBean(configContext);
            assert(configBean != null);

            SecurityService securityBean =
                ServerBeansFactory.getSecurityServiceBean(configContext);
            assert(securityBean != null);

            auditFlag = securityBean.isAuditEnabled();
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "audit.badinit", e);
        }

        if (auditFlag) {
            logger.info("audit.enabled");
        }

                                // load i18n message bits for audit entries
        ResourceBundle resBundle = logger.getResourceBundle();
        strPrivateAudit = resBundle.getString("audit.string_private_audit");
        strDenied = " " + resBundle.getString("audit.denied");
        strOK = " " + resBundle.getString("audit.ok");
        strMethodName = " " + resBundle.getString("audit.methodname");
        strSession = " " + resBundle.getString("audit.session");
    }

*/
    /**
     * Log an EJB method invocation.
     *
     * @param user Effective user for the invocation.
     * @param ejb EJB name.
     * @param method Method name.
     * @param success True if the invocation was allowed, false if denied.
     *
     */
/*
    public static void ejbMethodInvocation(SecurityContext secCtx,
                                           EJBLocalRemoteObject ejbObj,
                                           Method method,
                                           boolean success)
    {
        if (!logger.isLoggable(Level.INFO)) {
            return;
        }
        
        String user = "(null)";
        if (secCtx != null) {
            Principal p = secCtx.getCallerPrincipal();
            if (p!=null) {
                user = p.getName();
            }
        }

        String ejb = "(N/A)";
        if (ejbObj != null) {
            ejb = ejbObj.toString();
        }

        String meth = "(N/A)";
        if (method != null) {
            meth = method.toString();
        }
        
        StringBuffer sb = new StringBuffer();
        sb.append(strPrivateAudit); // "Audit: principal="
        
        if(user != null) {
            sb.append(user);
        } else {
            sb.append("(null)");
        }

        sb.append(" ejb=");
        sb.append(ejb);
        sb.append(strMethodName); // " method="
        sb.append(method);
        if (success) {
            sb.append(strOK);   // " OK"
        } else {
            sb.append(strDenied); // " DENIED"
        }

        logger.info(sb.toString());
    }

*/
    /**
     * Log a servlet invocation.
     *
     * @param req The HttpRequest.
     * @param success True if the invocation was allowed, false if denied.
     *
     */
  /*
    public static void webInvocation(HttpRequest req, boolean success)
    {
        /// DO NOTHING FOR NOW.
        //if (!logger.isLoggable(Level.INFO) || !auditFlag) {
        //    return;
        //}
                
        //if (req == null) {
        //    logger.fine("Audit: No HttpRequest available.");
        //    return;
        //}
        
        //if (!(req instanceof HttpRequestBase)) {
        //    logger.fine("Audit internal error, class: " + req.getClass());
        //    return;
        //}

        //HttpRequestBase reqs = (HttpRequestBase)req;
        
        //StringBuffer sb = new StringBuffer();
        //sb.append(strPrivateAudit); // "Audit: principal="
        
        //String user = reqs.getRemoteUser();
        //if (user != null) {
        //    sb.append(user);
        //} else {
        //    sb.append("(null)");
        //}

        //sb.append(" ");
        //sb.append(reqs.getMethod());
        //sb.append(" ");
        //sb.append(reqs.getRequestURI());
        //sb.append(strSession); //  " session="
        //sb.append(reqs.getRequestedSessionId());
        //if (success) {
        //    sb.append(strOK);   //  " OK"
        //} else {
        //    sb.append(strDenied); // " DENIED"
        //}

        //logger.info(sb.toString());
    }

    */
    /**
     * Diagnostic method. Read roles and ACLs from the given Application
     * and dump a somewhat organized summary of what has been set.
     * This can be used to diagnose deployment or runtime deployment errors
     * as well as to help in configuring application descriptors.
     *
     * <P>Implementation is not particularly efficient but this is only
     * called for debugging purposes at startup. All errors are ignored.
     *
     * @param app Application object to analyze.
     *
     */
    public static void showACL(Application app)
    {
        if (!isActive() || !logger.isLoggable(Level.FINEST)) {
            return;
        }

        try {
            dumpDiagnostics(app);

        } catch (Throwable e) {
            logger.fine("Error while showing ACL diagnostics: " +
                        e.toString());
        }
    }

    
    /**
     * Do the work for showACL().
     *
     */
    private static void dumpDiagnostics(Application app)
    {
        logger.finest("====[ Role and ACL Summary ]==========");
        if (!app.isVirtual()) {
            logger.finest("Summary for application: "+
                          app.getRegistrationName());
        } else {
            logger.finest("Standalone module.");
        }
        logger.finest("EJB components: "+
                           getEjbComponentCount(app));
        logger.finest("Web components: " +
                           getWebComponentCount(app));

        Iterator i;
        StringBuffer sb;
        
        // show all roles with associated group & user mappings
        Set allRoles = app.getRoles();
        if (allRoles == null) {
            logger.finest("- No roles present.");
            return;
        }
        SecurityRoleMapper rmap = app.getRoleMapper();
        if (rmap == null) {
            logger.finest("- No role mappings present.");
            return;
        }
        
        i = allRoles.iterator();
        logger.finest("--[ Configured roles and mappings ]--");
        HashMap allRoleMap = new HashMap();
        
        while (i.hasNext()) {
            Role r = (Role)i.next();
            logger.finest(" [" + r.getName() + "]");
            allRoleMap.put(r.getName(), new HashSet());
            
            sb = new StringBuffer();
            sb.append("  is mapped to groups: ");
            Enumeration grps = rmap.getGroupsAssignedTo(r);
            while (grps.hasMoreElements()) {
                sb.append(grps.nextElement());
                sb.append(" ");
            }
            logger.finest(sb.toString());

            sb = new StringBuffer();
            sb.append("  is mapped to principals: ");
            Enumeration users = rmap.getUsersAssignedTo(r);
            while (users.hasMoreElements()) {
                sb.append(users.nextElement());
                sb.append(" ");
            }
            logger.finest(sb.toString());
        }

        // Process all EJB modules

        Set ejbDescriptorSet = app.getBundleDescriptors(EjbBundleDescriptor.class) ;

        i = ejbDescriptorSet.iterator();
        while (i.hasNext()) {

            EjbBundleDescriptor bundle = (EjbBundleDescriptor)i.next();

            logger.finest("--[ EJB module: " + bundle.getName() + " ]--");
            Set ejbs = bundle.getEjbs();
            Iterator it = ejbs.iterator();
            while (it.hasNext()) {

                EjbDescriptor ejb = (EjbDescriptor)it.next();
                logger.finest("EJB: "+ejb.getEjbClassName());

                // check and show run-as if present
                if (!ejb.getUsesCallerIdentity()) {
                     RunAsIdentityDescriptor runas = ejb.getRunAsIdentity();
                     if (runas == null) {
                         logger.finest(" (ejb does not use caller "+
                                            "identity)");
                     } else {
                         String role = runas.getRoleName();
                         String user = runas.getPrincipal();
                         logger.finest(" Will run-as: Role: " + role +
                                            "  Principal: " + user);
                         if (role==null || "".equals(role) ||
                             user==null || "".equals(user)) {
                                 if(logger.isLoggable(Level.FINEST)){
                                    logger.finest("*** Configuration error!");
                                 }
                         }
                     }
                }

                // iterate through available methods
                logger.finest(" Method to Role restriction list:");
                Set methods = ejb.getMethodDescriptors();
                Iterator si = methods.iterator();
                
                while (si.hasNext()) {
                    
                    MethodDescriptor md = (MethodDescriptor)si.next();
                    logger.finest("   "+md.getFormattedString());

                    Set perms = ejb.getMethodPermissionsFor(md);
                    StringBuffer rbuf = new StringBuffer();
                    rbuf.append("     can only be invoked by: ");
                    Iterator sip = perms.iterator();
                    boolean unchecked=false,excluded=false,roleBased=false;
                    
                    while (sip.hasNext()) {
                        MethodPermission p = (MethodPermission)sip.next();
                        if (p.isExcluded()) {
                            excluded=true;
                            logger.finest("     excluded - can not "+
                                               "be invoked");
                        } else if (p.isUnchecked()) {
                            unchecked=true;
                            logger.finest("     unchecked - can be "+
                                               "invoked by all");
                        } else if (p.isRoleBased()) {
                            roleBased = true;
                            Role r = p.getRole();
                            rbuf.append(r.getName());
                            rbuf.append(" ");
                                // add to role's accessible list
                            HashSet ram = (HashSet)allRoleMap.get(r.getName());
                            ram.add(bundle.getName() + ":" +
                                    ejb.getEjbClassName() + "." +
                                    md.getFormattedString());
                        }
                    }

                    if (roleBased) {
                        logger.finest(rbuf.toString());
                        if (excluded || unchecked) {
                            logger.finest("*** Configuration error!");
                        }
                    } else if (unchecked) {
                        if (excluded) {
                            logger.finest("*** Configuration error!");
                        }
                        Set rks = allRoleMap.keySet();
                        Iterator rksi = rks.iterator();
                        while (rksi.hasNext()) {
                            HashSet ram = (HashSet)allRoleMap.get(rksi.next());
                            ram.add(bundle.getName() + ":" +
                                    ejb.getEjbClassName() + "." +
                                    md.getFormattedString());
                        }
                    } else if (!excluded) {
                        logger.finest("*** Configuration error!");
                    }
                }

                // IOR config for this ejb
                logger.finest(" IOR configuration:");
                Set iors = ejb.getIORConfigurationDescriptors();
                if (iors != null) {
                    Iterator iorsi = iors.iterator();
                    while (iorsi.hasNext()) {
                        EjbIORConfigurationDescriptor ior =
                            (EjbIORConfigurationDescriptor)iorsi.next();
                        StringBuffer iorsb = new StringBuffer();
                        iorsb.append("realm=");
                        iorsb.append(ior.getRealmName());
                        iorsb.append(", integrity=");
                        iorsb.append(ior.getIntegrity());
                        iorsb.append(", trust-in-target=");
                        iorsb.append(ior.getEstablishTrustInTarget());
                        iorsb.append(", trust-in-client=");
                        iorsb.append(ior.getEstablishTrustInClient());
                        iorsb.append(", propagation=");
                        iorsb.append(ior.getCallerPropagation());
                        iorsb.append(", auth-method=");
                        iorsb.append(ior.getAuthenticationMethod());
                        logger.finest(iorsb.toString());
                    }
                }
            }
        }

        // show role->accessible methods list
        logger.finest("--[ EJB methods accessible by role ]--");

        Set rks = allRoleMap.keySet();
        Iterator rksi = rks.iterator();
        while (rksi.hasNext()) {
            String roleName = (String)rksi.next();
            logger.finest(" [" + roleName + "]");
            HashSet ram = (HashSet)allRoleMap.get(roleName);
            Iterator rami = ram.iterator();
            while (rami.hasNext()) {
                String meth = (String)rami.next();
                logger.finest("   "+meth);
            }
        }

        

        // Process all Web modules

        Set webDescriptorSet = app.getBundleDescriptors(WebBundleDescriptor.class) ;

        i = webDescriptorSet.iterator();
        while (i.hasNext()) {
            WebBundleDescriptor wbd = (WebBundleDescriptor)i.next();
            logger.finest("--[ Web module: " + wbd.getContextRoot() + " ]--");

            // login config
            LoginConfiguration lconf = wbd.getLoginConfiguration();
            if (lconf != null) {
                logger.finest("  Login config: realm="+
                              lconf.getRealmName() + ", method="+
                              lconf.getAuthenticationMethod() + ", form="+
                              lconf.getFormLoginPage() + ", error="+
                              lconf.getFormErrorPage());
            }

            // get WebComponentDescriptorsSet()  info
            logger.finest("  Contains components:");
            Set webComps = wbd.getWebComponentDescriptors();
            Iterator webCompsIt = webComps.iterator();
            while (webCompsIt.hasNext()) {
                WebComponentDescriptor wcd =
                    (WebComponentDescriptor)webCompsIt.next();
                StringBuffer name = new StringBuffer();
                name.append("   - "+wcd.getCanonicalName());
                name.append(" [ ");
                Enumeration urlPs = wcd.getUrlPatterns();
                while (urlPs.hasMoreElements()) {
                    name.append(urlPs.nextElement().toString());
                    name.append(" ");
                }
                name.append("]");
                logger.finest(name.toString());
                
                RunAsIdentityDescriptor runas =
                    (RunAsIdentityDescriptor)wcd.getRunAsIdentity();
                if (runas!=null) {
                    String role = runas.getRoleName();
                    String user = runas.getPrincipal();
                    logger.finest("      Will run-as: Role: " + role +
                                  "  Principal: " + user);
                    if (role==null || "".equals(role) ||
                        user==null || "".equals(user)) {
                        logger.finest("*** Configuration error!");
                    }
                }
                
            }
            
            // security constraints
            logger.finest("  Security constraints:");
            Enumeration scEnum = wbd.getSecurityConstraints();
            while (scEnum.hasMoreElements()) {

                SecurityConstraint sc =
                    (SecurityConstraint)scEnum.nextElement();

                for (WebResourceCollection wrc: sc.getWebResourceCollections()) {
                    // show list of methods for this collection
                    StringBuffer sbm = new StringBuffer();
                    for (String httpMethod: wrc.getHttpMethods()) {
                        sbm.append(httpMethod);
                        sbm.append(" ");
                    }
                    logger.finest("     Using method: "+sbm.toString());

                    // and then list of url patterns
                    for (String urlPattern: wrc.getUrlPatterns()) {
                        logger.finest("       "+ urlPattern);
                    }
                } // end res.collection iterator

                // show roles which apply to above set of collections
                AuthorizationConstraint authCons =
                        sc.getAuthorizationConstraint();
                Enumeration rolesEnum = authCons.getSecurityRoles();
                StringBuffer rsb = new StringBuffer();
                rsb.append("     Accessible by roles: ");
                while (rolesEnum.hasMoreElements()) {
                    SecurityRole sr = (SecurityRole)rolesEnum.nextElement();
                    rsb.append(sr.getName());
                    rsb.append(" ");
                }
                logger.finest(rsb.toString());

                // show transport guarantee
                UserDataConstraint udc =sc.getUserDataConstraint();
                if (udc != null) {
                    logger.finest("     Transport guarantee: "+
                                  udc.getTransportGuarantee());
                }
                
            } // end sec.constraint
            
        } // end webDescriptorSet.iterator
        

        logger.finest("======================================");
    }
    
    /**
     * The number of Web Components in this application.
     * Current implementation only return the number of servlets
     * inside the application, and not the JSPs since we cannot
     * get that information from deployment descriptors.
     *
     * @return the number of Web Components
     */
    private static int getWebComponentCount(Application app) {
        int count = 0;
        for (WebBundleDescriptor wbd : app.getBundleDescriptors(WebBundleDescriptor.class)) {
            count = count + wbd.getWebComponentDescriptors().size();
        }
        return count;
    }

    /**
     * The number of EJB JARs in this application.
     *
     * @return the number of EJB JARS
     */
    private static int getEjbComponentCount(Application app) {
        int count = 0;
        for (EjbBundleDescriptor ejbd : app.getBundleDescriptors(EjbBundleDescriptor.class)) {
            count = count + ejbd.getEjbs().size();
        }
        return count;
    }
}
