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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019-2021] [Payara Foundation and/or its affiliates]
package org.glassfish.web.deployment.descriptor;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.web.InitializationParameter;
import com.sun.enterprise.deployment.web.MultipartConfig;
import com.sun.enterprise.deployment.web.SecurityRoleReference;
import com.sun.enterprise.util.LocalStringManagerImpl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static java.util.Collections.enumeration;

/**
 * Common data and behavior of the deployment information about a JSP or JavaServlet in J2EE.
 *
 * @author Jerome Dochez
 */
public class WebComponentDescriptorImpl extends WebComponentDescriptor {

    private static final long serialVersionUID = -426082855000062548L;

    private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(WebComponentDescriptor.class);

    /**
     * Constant for Basic authentication.
     */
    public static final String BASIC_AUTHENTICATION = "basic";

    /**
     * Constant for Form authentication.
     */
    public static final String FORM_AUTHENTICATION = "form";

    /**
     * Constant for Secure authentication.
     */
    public static final String SSL_AUTHENTICATION = "ssl";

    /**
     * Constant for the htpp GET method.
     */
    public static final String GET = "GET";

    /**
     * Constant for the http PUT method.
     */
    public static final String PUT = "PUT";

    /**
     * Constant for the http POST method.
     */
    public static final String POST = "POST";

    /**
     * Constant for the http DELETE method.
     */
    public static final String DELETE = "DELETE";

    private Set<InitializationParameter> initializationParameters;
    private Set<String> urlPatterns;
    private String canonicalName;
    private Integer loadOnStartUp;
    private Set<SecurityRoleReference> securityRoleReferences;

    private RunAsIdentityDescriptor runAs;
    private WebBundleDescriptor webBundleDescriptor;
    private boolean enabled = true;
    private Boolean asyncSupported;
    private MultipartConfig multipartConfig;
    private transient List<Method> httpMethods;

    private boolean conflict;
    private Set<String> conflictedInitParameterNames;

    private String implFile = "";
    private boolean isServlet;


    /**
     * The default constructor.
     */

    public WebComponentDescriptorImpl() {
    }

    /**
     * The copy constructor.
     */

    public WebComponentDescriptorImpl(WebComponentDescriptor other) {
        setCanonicalName(other.getCanonicalName());
        setServlet(other.isServlet());
        setWebComponentImplementation(other.getWebComponentImplementation());
        getInitializationParameterSet().addAll(other.getInitializationParameterSet());
        getUrlPatternsSet().addAll(other.getUrlPatternsSet());
        setLoadOnStartUp(other.getLoadOnStartUp());
        getSecurityRoleReferenceSet().addAll(other.getSecurityRoleReferenceSet());
        setRunAsIdentity(other.getRunAsIdentity());
        setAsyncSupported(other.isAsyncSupported());
        setMultipartConfig(other.getMultipartConfig());
        setWebBundleDescriptor(other.getWebBundleDescriptor());
        conflictedInitParameterNames = other.getConflictedInitParameterNames();
        setConflict(other.isConflict());
    }

    @Override
    public Set<InitializationParameter> getInitializationParameterSet() {
        if (initializationParameters == null) {
            initializationParameters = new OrderedSet<>();
        }

        return initializationParameters;
    }

    /**
     * @return the Set of servlet initialization parameters.
     */
    @Override
    public Enumeration<InitializationParameter> getInitializationParameters() {
        return  enumeration(new ArrayList<>(getInitializationParameterSet()));
    }

    /**
     * @param name
     * @return a matching initialization parameter by its name if there is one.
     */
    @Override
    public InitializationParameter getInitializationParameterByName(String name) {
        for (InitializationParameter next : getInitializationParameterSet()) {
            if (next.getName().equals(name)) {
                return next;
            }
        }
        return null;
    }

    /**
     * Adds a servlet initialization parameter to this component.
     *
     * @param initializationParameter
     */
    @Override
    public void addInitializationParameter(InitializationParameter initializationParameter) {
        getInitializationParameterSet().add(initializationParameter);
    }

    /**
     * Removes the given servlet initialization parameter from this component.
     *
     * @param initializationParameter
     */
    @Override
    public void removeInitializationParameter(InitializationParameter initializationParameter) {
        getInitializationParameterSet().remove(initializationParameter);
    }

    @Override
    public Set<String> getConflictedInitParameterNames() {
        if (conflictedInitParameterNames == null) {
            conflictedInitParameterNames = new HashSet<>();
        }

        return conflictedInitParameterNames;
    }

    /**
     * @return the set of URL pattern aliases for this component.
     */
    @Override
    public Set<String> getUrlPatternsSet() {
        if (urlPatterns == null) {
            urlPatterns = new OrderedSet<String>() {
                /**
                 *
                 */
                private static final long serialVersionUID = -43840281980324986L;

                @Override
                public boolean add(String s) {
                    Map<String, String> up2sname = getUrlPatternToServletNameMap();
                    if (up2sname != null) {
                        String name = getCanonicalName();
                        String oldName = up2sname.put(s, getCanonicalName());
                        if (oldName != null && (!oldName.equals(name))) {
                            throw new IllegalArgumentException(localStrings.getLocalString("web.deployment.exceptionsameurlpattern",
                                    "Servlet [{0}] and Servlet [{1}] have the same url pattern: [{2}]", new Object[] { oldName, name, s }));
                        }
                    }
                    return super.add(s);
                }

                @Override
                public boolean remove(Object o) {
                    boolean result = super.remove(o);
                    if (getWebBundleDescriptor() != null) {
                        getWebBundleDescriptor().resetUrlPatternToServletNameMap();
                    }
                    return result;
                }

                private Map<String, String> getUrlPatternToServletNameMap() {
                    return ((getWebBundleDescriptor() != null) ? getWebBundleDescriptor().getUrlPatternToServletNameMap() : null);
                }
            };
        }
        return urlPatterns;
    }

    /**
     * @return an enumeration of (String) URL pattern aliases for this component.
     */
    @Override
    public Enumeration<String> getUrlPatterns() {
        return enumeration(new ArrayList<>(getUrlPatternsSet()));
    }

    /**
     * Adds an alias to this web component.
     *
     * @param urlPattern
     */
    @Override
    public void addUrlPattern(String urlPattern) {
        getUrlPatternsSet().add(urlPattern);
    }

    /**
     * Removes a URL pattern from this web component.
     *
     * @param urlPattern
     */
    @Override
    public void removeUrlPattern(String urlPattern) {
        getUrlPatternsSet().remove(urlPattern);
    }

    @Override
    public void setWebBundleDescriptor(WebBundleDescriptor webBundleDescriptor) {
        this.webBundleDescriptor = webBundleDescriptor;
    }

    /**
     * @return the web app object to which this object belongs
     */
    @Override
    public WebBundleDescriptor getWebBundleDescriptor() {
        return webBundleDescriptor;
    }

    /**
     * The canonical name for the web component.
     *
     * @return
     */
    @Override
    public String getCanonicalName() {
        if (canonicalName == null) {
            canonicalName = getName();
        }

        return canonicalName;
    }

    /**
     * Sets the canonical name of this web component.
     *
     * @param canonicalName
     */
    @Override
    public void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    /**
     * @return the order on which this component will be loaded by the web server.
     */
    @Override
    public Integer getLoadOnStartUp() {
        return loadOnStartUp;
    }

    /**
     * Sets the order on which this component will be loaded by the web server.
     *
     * @param loadOnStartUp
     */
    @Override
    public void setLoadOnStartUp(Integer loadOnStartUp) {
        this.loadOnStartUp = loadOnStartUp;
    }

    /**
     * Sets the order on which this component will be loaded by the web server.
     *
     * @param loadOnStartUp
     */
    @Override
    public void setLoadOnStartUp(String loadOnStartUp) throws NumberFormatException {
        this.loadOnStartUp = Integer.decode(loadOnStartUp);
    }

    @Override
    public Set<SecurityRoleReference> getSecurityRoleReferenceSet() {
        if (securityRoleReferences == null) {
            securityRoleReferences = new OrderedSet<>();
        }

        return securityRoleReferences;
    }

    /**
     * @return the Set of security role references that I have.
     */
    @Override
    public Enumeration<SecurityRoleReference> getSecurityRoleReferences() {
        return enumeration(new ArrayList<>(getSecurityRoleReferenceSet()));
    }

    /**
     * @param roleReferenceName
     * @return a matching role reference by name or null if there is none matching.
     */
    @Override
    public SecurityRoleReference getSecurityRoleReferenceByName(String roleReferenceName) {
        for (SecurityRoleReference scurityRoleReference : getSecurityRoleReferenceSet()) {
            if (scurityRoleReference.getRoleName().equals(roleReferenceName)) {
                return scurityRoleReference;
            }
        }

        return null;
    }

    /**
     * Adds a security role reference to this web component.
     *
     * @param securityRoleReference
     */
    @Override
    public void addSecurityRoleReference(SecurityRoleReference securityRoleReference) {
        getSecurityRoleReferenceSet().add(securityRoleReference);
    }

    /**
     * Removes the given security role reference from this web component.
     *
     * @param securityRoleReference
     */
    @Override
    public void removeSecurityRoleReference(SecurityRoleReference securityRoleReference) {
        getSecurityRoleReferenceSet().remove(securityRoleReference);
    }

    /**
     * Sets the run-as of the referee EJB.
     *
     * @param runAs the value of run-as
     */
    @Override
    public void setRunAsIdentity(RunAsIdentityDescriptor runAs) {
        if (this.runAs == null) {
            this.runAs = runAs;
        }
    }

    /**
     * Gets the run-as of the referee EJB.
     *
     * @return the value of run-as.
     */
    @Override
    public RunAsIdentityDescriptor getRunAsIdentity() {
        return runAs;
    }

    @Override
    public boolean getUsesCallerIdentity() {
        return (runAs == null);
    }

    @Override
    public void setUsesCallerIdentity(boolean isCallerID) {
        if (isCallerID) {
            runAs = null;
        } else {
            runAs = new RunAsIdentityDescriptor("");
        }
    }

    @Override
    public MultipartConfig getMultipartConfig() {
        return multipartConfig;
    }

    @Override
    public void setMultipartConfig(MultipartConfig multipartConfig) {
        this.multipartConfig = multipartConfig;
    }

    /**
     * DeploymentDescriptorNode.addNodeDescriptor(node) need this.
     *
     * @param multipartConfigDesc
     */
    public void setMultipartConfig(MultipartConfigDescriptor multipartConfigDesc) {
        this.multipartConfig = multipartConfigDesc;
    }

    @Override
    public Application getApplication() {
        if (getWebBundleDescriptor() != null) {
            return getWebBundleDescriptor().getApplication();
        }

        return null;
    }

    /**
     * sets the implementation file for this web component, the implementation file is either a servlet class name of a jsp
     * file name.
     *
     * @param implFile the servlet class name or the jsp file
     */
    @Override
    public void setWebComponentImplementation(String implFile) {
        if (!isServlet && !implFile.startsWith("/")) {
            implFile = "/" + implFile;
        }

        this.implFile = implFile;
    }

    @Override
    public String getWebComponentImplementation() {
        return implFile;
    }

    @Override
    public boolean isServlet() {
        return isServlet;
    }

    @Override
    public void setServlet(boolean isServlet) {
        this.isServlet = isServlet;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void setAsyncSupported(Boolean asyncSupported) {
        this.asyncSupported = asyncSupported;
    }

    @Override
    public Boolean isAsyncSupported() {
        return asyncSupported;
    }

    @Override
    public void setConflict(boolean conflict) {
        this.conflict = conflict;
    }

    @Override
    public boolean isConflict() {
        return conflict;
    }

    /**
     * This method return an array of user defined http doDelete, doGet, doHead, doOptions, doPost, doPut, doTrace methods.
     * It is used for processing web security annotations.
     *
     * @return an array of methods.
     */
    @Override
    public Method[] getUserDefinedHttpMethods() {
        if (httpMethods == null) {
            httpMethods = new ArrayList<Method>();

            if (isServlet) {
                List<String> searchingMethods = new ArrayList<String>();
                String[] httpMString = new String[] { "doDelete", "doGet", "doHead", "doOptions", "doPost", "doPut", "doTrace" };
                for (String s : httpMString) {
                    searchingMethods.add(s);
                }

                try {
                    Class<?> implClass = Class.forName(implFile, true, Thread.currentThread().getContextClassLoader());
                    Class<?> clazz = implClass;
                    String packageName = null;
                    Package clazzPackage = implClass.getPackage();
                    if (clazzPackage != null) {
                        packageName = clazzPackage.getName();
                    }

                    // the processing is stopped at jakarta.servlet package as
                    // a) there is no user defined class there
                    // b) there is no security annotations used there
                    while (clazz != null && (!clazz.getName().startsWith("jakarta.servlet.")) && searchingMethods.size() > 0) {
                        Package p = clazz.getPackage();
                        Method[] methods = clazz.getDeclaredMethods();
                        for (Method m : methods) {
                            String methodName = m.getName();
                            if (searchingMethods.contains(methodName)) {
                                Class<?> returnType = m.getReturnType();
                                Class<?>[] parameterTypes = m.getParameterTypes();
                                int modifiers = m.getModifiers();
                                boolean isSamePackage = (p == null && clazzPackage == null)
                                        || (p != null && clazzPackage != null && packageName.equals(p.getName()));
                                boolean valid = (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)
                                        || ((!Modifier.isPrivate(modifiers)) && isSamePackage));
                                valid = valid && (void.class.equals(returnType)) && (parameterTypes.length == 2)
                                        && (parameterTypes[0].equals(HttpServletRequest.class) && parameterTypes[1].equals(HttpServletResponse.class));
                                if (valid) {
                                    httpMethods.add(m);
                                    searchingMethods.remove(methodName);
                                }

                                if (searchingMethods.size() == 0) {
                                    break;
                                }
                            }
                        }
                        clazz = clazz.getSuperclass();
                    }
                } catch (Throwable t) {
                    throw new IllegalStateException(t);
                }
            }
        }

        return httpMethods.toArray(new Method[httpMethods.size()]);
    }

    /*
     * -----------
     */

    /**
     * A formatted string representing my state.
     *
     * @param toStringBuilder
     */
    @Override
    public void print(StringBuilder toStringBuilder) {
        super.print(toStringBuilder);
        toStringBuilder.append("WebComponentDescriptor\n");
        toStringBuilder.append("\n initializationParameters ").append(initializationParameters);
        toStringBuilder.append("\n urlPatterns ").append(urlPatterns);
        toStringBuilder.append("\n canonicalName ").append(canonicalName);
        toStringBuilder.append("\n loadOnStartUp ").append(loadOnStartUp);
        toStringBuilder.append("\n enabled ").append(enabled);
        toStringBuilder.append("\n asyncSupported ").append(asyncSupported);
        toStringBuilder.append("\n securityRoleReferences ").append(securityRoleReferences);
        toStringBuilder.append("\n multipartConfig ").append(multipartConfig);
        if (isServlet()) {
            toStringBuilder.append("\n servlet className ").append(getWebComponentImplementation());
        } else {
            toStringBuilder.append("\n jspFileName ").append(getWebComponentImplementation());
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof WebComponentDescriptor && this.getCanonicalName().equals(((WebComponentDescriptor) other).getCanonicalName())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + getCanonicalName().hashCode();
        return result;
    }

    @Override
    public void add(WebComponentDescriptor other) {
        add(other, true, false);
    }

    /**
     * this method will combine the information from this "other" // WebComponentDescriptor with current
     * WebComponentDescriptor // // when there are conflicts between the contents of the two, // the value from current
     * WebComponentDescriptor will override // the value in "other" // // Note: in the Set API, we only add value when such
     * value // is not existed in the Set already // // If combineUrlPatterns is false, then the first one take priority, //
     * otherwise take the second one. // // If combineConflict is true, it will combine the init parameter // conflict
     * information in #getConflictedInitParameterSet. // // And the conflict boolean will not be set.
     *
     * @param other
     * @param combineUrlPatterns
     * @param combineConflict
     */
    @Override
    public void add(WebComponentDescriptor other, boolean combineUrlPatterns, boolean combineConflict) {
        // do not do anything if the canonical name of the two web
        // components are different
        if (!getCanonicalName().equals(other.getCanonicalName())) {
            return;
        }
        // do not do anything if the type of the two web
        // components are different
        if ((isServlet() && !other.isServlet()) || (!isServlet() && other.isServlet())) {
            return;
        }

        // for simple String types, we can rely on Set API
        if (combineUrlPatterns || getUrlPatternsSet().size() == 0) {
            getUrlPatternsSet().addAll(other.getUrlPatternsSet());
        }

        // for complex types, only added it if the complex type with same
        // name is not in the set yet
        if (conflictedInitParameterNames == null) {
            conflictedInitParameterNames = other.getConflictedInitParameterNames();
        } else {
            conflictedInitParameterNames.addAll(other.getConflictedInitParameterNames());
        }
        if (!combineConflict) {
            for (InitializationParameter initParam : getInitializationParameterSet()) {
                conflictedInitParameterNames.remove(initParam.getName());
            }
        }

        for (Iterator<InitializationParameter> initParamIter = other.getInitializationParameterSet().iterator(); initParamIter.hasNext();) {
            InitializationParameter initParam = initParamIter.next();
            InitializationParameter origInitParam = getInitializationParameterByName(initParam.getName());
            if (origInitParam == null) {
                getInitializationParameterSet().add(initParam);
            } else if (combineConflict && !origInitParam.getValue().equals(initParam.getValue())) {
                getConflictedInitParameterNames().add(initParam.getName());
            }
        }
        for (SecurityRoleReference secRoleRef : other.getSecurityRoleReferenceSet()) {
            if (getSecurityRoleReferenceByName(secRoleRef.getRoleName()) == null) {
                getSecurityRoleReferenceSet().add(secRoleRef);
            }
        }

        // only set these values if they are not set in the current
        // web component already

        if (getLoadOnStartUp() == null) {
            setLoadOnStartUp(other.getLoadOnStartUp());
        }
        if (isAsyncSupported() == null) {
            setAsyncSupported(other.isAsyncSupported());
        }
        if (getRunAsIdentity() == null) {
            setRunAsIdentity(other.getRunAsIdentity());
        }
        if (getMultipartConfig() == null) {
            setMultipartConfig(other.getMultipartConfig());
        }
        if (getWebComponentImplementation() == null) {
            setWebComponentImplementation(other.getWebComponentImplementation());
        }
    }

    @Override
    public boolean isConflict(WebComponentDescriptor other, boolean allowNullImplNameOverride) {
        if (conflict || other.isConflict()) {
            return true;
        }

        if (!getCanonicalName().equals(other.getCanonicalName())) {
            return false;
        }

        String otherImplFile = other.getWebComponentImplementation();
        boolean matchImplName = (allowNullImplNameOverride) ?
        // note that "" and null are regarded as the same here
                (implFile == null || implFile.length() == 0 || otherImplFile == null || otherImplFile.length() == 0 || implFile.equals(otherImplFile))
                : (((implFile == null || implFile.length() == 0) && (otherImplFile == null || otherImplFile.length() == 0))
                        || (implFile != null && implFile.equals(otherImplFile)));

        boolean otherAsyncSupported = (other.isAsyncSupported() != null) ? other.isAsyncSupported() : false;
        boolean thisAsyncSupported = (asyncSupported != null) ? asyncSupported : false;
        boolean matchAsyncSupported = (thisAsyncSupported == otherAsyncSupported);

        return !(matchImplName && matchAsyncSupported);
    }
}
