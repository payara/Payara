/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.module.maven.commandsecurityplugin;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.glassfish.module.maven.commandsecurityplugin.CommandAuthorizationInfo.Param;

/**
 * Verifies that all inhabitants in the module that are commands also take care
 * of authorization, issuing warnings or failing the build (configurable) if
 * any do not.
 * <p>
 * The mojo has to analyze not only the inhabitants but potentially also 
 * their ancestor classes.  To improve performance across multiple modules in 
 * the same build the mojo stores information about classes known to be commands and
 * classes known not to be commands in the maven session. 
 * 
 * @author tjquinn
 * 
 */
public class TypeProcessorImpl implements TypeProcessor {
    
    
    private boolean isFailureFatal;
    
    private static final String KNOWN_NONCOMMAND_TYPES_NAME = "org.glassfish.api.admin.knownNonCommandTypes";
    private static final String PROCESSED_MODULES_NAME = "org.glassfish.api.admin.processedModules";
    private static final String CONFIG_BEANS_NAME = "org.glassfish.api.admin.configBeans";
    
    private static final String INHABITANTS_PATH = "META-INF/hk2-locator/default";
    
//    private static final Pattern INHABITANT_DESCR_PATTERN = Pattern.compile("(\\w+)=([^:]+)(?:\\:(.+))*");
    private static final Pattern INHABITANT_IMPL_CLASS_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");
    private static final Pattern INHABITANT_CONTRACTS_PATTERN = Pattern.compile("contract=\\{([^\\}]+)\\}");
    private static final Pattern INHABITANT_NAME_PATTERN = Pattern.compile("name=(.+)");
    private final static Pattern CONFIG_BEAN_METADATA_PREFIX_PATTERN = Pattern.compile(
            "metadata=target=\\{([^\\}]+)\\}(.*)"); // 
    
    // ,<element-name>={class-name} or ,<element-name>={collection\:class-name}
    private final static Pattern CONFIG_BEAN_CHILD_PATTERN = Pattern.compile(
            ",(?:<([^>]+)>=\\{(?:collection\\\\:)?([^,}]+)[},])|(?:\\@[^}]+})|(?:key=[^}]+)|(?:keyed-as=[^}]+)");
    private static final Pattern GENERIC_COMMAND_INFO_PATTERN = Pattern.compile("metadata=MethodListActual=\\{([^\\}]+)\\},MethodName=\\{([^\\}]+)\\},ParentConfigured=\\{([^\\}]+)\\}");
        
    
    
    private static final String ADMIN_COMMAND_NAME = "org.glassfish.api.admin.AdminCommand";
    private static final String CLI_COMMAND_NAME = "com.sun.enterprise.admin.cli.CLICommand";
    
    private static final String LINE_SEP = System.getProperty("line.separator");
    
    private static final String CONFIG_INJECTOR_NAME = "org.jvnet.hk2.config.ConfigInjector";
    
    private static final String GENERIC_CREATE_COMMAND = "org.glassfish.config.support.GenericCreateCommand";
    private static final String GENERIC_DELETE_COMMAND = "org.glassfish.config.support.GenericDeleteCommand";
    private static final String GENERIC_LIST_COMMAND = "org.glassfish.config.support.GenericListCommand";
    private static final Set<String> GENERIC_CRUD_COMMAND_CLASS_NAMES = 
            new HashSet<String>(Arrays.asList(GENERIC_CREATE_COMMAND,
            GENERIC_DELETE_COMMAND,
            GENERIC_LIST_COMMAND));
    
    private static final Map<String,String> genericCommandNameToAction = 
            initCommandNameToActionMap();
    
    private static Map<String,String> initCommandNameToActionMap() {
        final Map<String,String> result = new HashMap<String,String>();
        result.put(GENERIC_CREATE_COMMAND, "create");
        result.put(GENERIC_DELETE_COMMAND, "delete");
        result.put(GENERIC_LIST_COMMAND, "list");
        return result;
    }
    
    private URLClassLoader loader;
    private File buildDir;
    
    private StringBuilder trace = null;
    
    private Map<String,CommandAuthorizationInfo> knownCommandTypes = null;
    private Set<String> knownNonCommandTypes = null;
    private Map<String,CommandAuthorizationInfo> knownCRUDConfigBeanTypes = null;
    
    private Collection<CommandAuthorizationInfo> authInfosThisModule = new ArrayList<CommandAuthorizationInfo>();
    
    private final List<String> offendingClassNames = new ArrayList<String>();
    private List<String> okClassNames = null;
    
    private Set<URL> jarsProcessedForConfigBeans = null;
    
    private Map<String,Inhabitant> configBeans = null;
    
    private final AbstractMojo mojo;
    private final MavenSession session;
    private final MavenProject project;
    
    
    TypeProcessorImpl(final AbstractMojo mojo, final MavenSession session, 
            final MavenProject project) {
        this(mojo, session, project, false);
    }
    
    private TypeProcessorImpl(final AbstractMojo mojo, final MavenSession session, 
            final MavenProject project,
            final boolean isFailureFatal) {
        this.mojo = mojo;
        this.session = session;
        this.project = project;
        this.isFailureFatal = isFailureFatal;
    }
    
    TypeProcessorImpl(final AbstractMojo mojo, final MavenSession session, 
            final MavenProject project,
            final String isFailureFatalSetting) {
        this(mojo, session, project, Boolean.parseBoolean(isFailureFatalSetting));
    }
    
    @Override
    public Map<String,Inhabitant> configBeans() {
        return configBeans;
    }
    
    private Log getLog() {
        return mojo.getLog();
    }
    
    /**
     * Processes a type (class or interface), analyzing its byte code to see if
     * it is a command and, if so, checking for authorization-related annos
     * and interface implementations, finally recording whether it is a known command
     * or a known non-command (to speed up analysis of other types that might
     * refer to this one).
     * 
     * @param internalClassName
     * @return the command authorization info for the class if it is a command, null otherwise
     * @throws MojoExecutionException 
     */
    @Override
    public CommandAuthorizationInfo processType(final String internalClassName) throws MojoFailureException, MojoExecutionException {
        return processType(internalClassName, false);
    }
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.trace = (getLog().isDebugEnabled() ? new StringBuilder() : null);
        this.okClassNames = (getLog().isDebugEnabled() ? new ArrayList<String>() : null);
        
        buildDir = new File(project.getBuild().getOutputDirectory());
        try {
            setUpKnownTypes();
        } catch (Exception ex) {
            throw new MojoExecutionException("Error retrieving information about earlier processing results" + ex.toString());
        }
        
        /*
         * Set up a class loader that knows about this project's dependencies.
         * We don't actually load classes; we use the loader's getResourceAsStream
         * to get a class's byte code for analysis.
         */
        loader = createClassLoader();
        try {
            loadConfigBeans();
        } catch (Exception ex) {
            throw new MojoExecutionException("Error loading config beans" + ex.toString());
        }
        
        final Collection<Inhabitant> inhabitants;
        try {
            inhabitants = findInhabitantsInModule();
        } catch (IOException ex) {
            throw new MojoExecutionException("Error searching inhabitants for commands", ex);
        }
        final Collection<Inhabitant> commandInhabitants = findCommandInhabitants(inhabitants);
        for (Inhabitant i : commandInhabitants) {
            authInfosThisModule.add(processType(i));
        }
        
//        final Collection<Inhabitant> crudCommandInhabitants = findCRUDCommandInhabitants(inhabitants);
//        for (Inhabitant i : crudCommandInhabitants) {
//            final CommandAuthorizationInfo authInfo = processConfigBean(i);
//            if (authInfo != null) {
//                authInfosThisModule.add(authInfo);
//            }
//        }
        
        if (trace != null) {
            getLog().debug(trace.toString());
        }
        
    }

    private void loadConfigBeans() throws MalformedURLException, IOException {
        for (URL url : loader.getURLs()) {
            if ( ! jarsProcessedForConfigBeans.contains(url)) {
                getLog().debug("Starting to load configBeans from " + url.toExternalForm());
                loadConfigBeansFromJar(url);
                jarsProcessedForConfigBeans.add(url);
            }
        }
    }
    
    private void loadConfigBeansFromJar(final URL url) throws MalformedURLException, IOException {
        final URL inhabitantsURL;
        if (url.getPath().endsWith(".jar")) {
            inhabitantsURL = new URL("jar:file:" + url.getPath() + "!/" + INHABITANTS_PATH);
        } else {
            inhabitantsURL = new URL(url, INHABITANTS_PATH);
        }
        final InputStream is;
        try {
            is = new BufferedInputStream(inhabitantsURL.openStream());
        } catch (FileNotFoundException ex) {
            // This must means that the JAR does not contain an inhabitants file.  Continue.
            return;
        }
            
        /*
         * As a side effect, findInhabitantsInModule adds config beans in the
         * specified input to configBeans.
         */
        try {
            findInhabitantsInModule(new InputStreamReader(is));
        } finally {
            is.close();
        }
    }
    
    @Override
    public Collection<CommandAuthorizationInfo> authInfosThisModule() {
        return authInfosThisModule;
    }
    
    @Override
    public List<String> okClassNames() {
        return okClassNames;
    }

    @Override
    public List<String> offendingClassNames() {
        return offendingClassNames;
    }

    @Override
    public boolean isFailureFatal() {
        return isFailureFatal;
    }

    @Override
    public StringBuilder trace() {
        return trace;
    }
    
    
    private CommandAuthorizationInfo processType(final Inhabitant i) throws MojoFailureException, MojoExecutionException {
        /*
         * If this inhabitant is generated as a CRUD command then we do not
         * need to analyze the byte code - what we need to know is already
         * present in the inhabitant data.
         */
        if ( ! GENERIC_CRUD_COMMAND_CLASS_NAMES.contains(i.className)) {
            return processType(i.className, true);
        }
        final CommandAuthorizationInfo info = new CommandAuthorizationInfo();
        final Param primary = new Param("name", "");
        primary.addValue("primary", Boolean.TRUE);
        
        info.addParam(primary);
        info.setName(i.serviceName);
        info.setClassName(i.className);
        info.setLocal(false);
        info.setGeneric(i.methodListActual, i.methodName, i.fullPath(), i.action);
        return info;
    }

    private CommandAuthorizationInfo processType(final String internalClassName, final boolean isInhabitant) throws MojoExecutionException, MojoFailureException {
        /*
         * If we have already processed this type, use the earlier result if it
         * is a command and if it is not a command, return null immediately.
         */
        if (knownCommandTypes.containsKey(internalClassName)) {
            getLog().debug("Recognized previously-IDd class as command: " + internalClassName);
            return knownCommandTypes.get(internalClassName);
        }
        if (knownNonCommandTypes.contains(internalClassName)) {
            getLog().debug("Recognized previously-IDd class as non-command: " + internalClassName);
            return null;
        }
        
        /*
         * Find the byte code for this class so we can analyze it.
         */
        final String resourcePath = internalClassName.replace('.','/') + ".class";
        final InputStream is = loader.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new MojoFailureException("Cannot locate byte code for inhabitant class " + resourcePath);
        }
        try {
            final TypeAnalyzer typeAnalyzer = new TypeAnalyzer(is, knownCommandTypes, this);
            typeAnalyzer.setTrace(trace);
            typeAnalyzer.run();
            if (trace != null) {
                getLog().debug(trace.toString());
                trace = new StringBuilder();
            }
            final CommandAuthorizationInfo authInfo = typeAnalyzer.commandAuthInfo();
            if (authInfo != null) {
                if (trace != null) {
                    trace.append(LINE_SEP).append("Adding ").append(internalClassName).append(" to knownCommandTypes");
                }
                knownCommandTypes.put(internalClassName, authInfo);
            } else {
                if (trace != null) {
                    trace.append(LINE_SEP).append("Adding ").append(internalClassName).append(" to knownNonCommandTypes");
                }
                knownNonCommandTypes.add(internalClassName);
            }
            /*
                * Log our decision about whether this class is OK only if this
                * is an inhabitant.  (This method might have been invoked to analyze a
                * superclass, and if the superclass was not listed as an 
                * inhabitant we don't need to report whether it has authorization
                * info or not).
                */
            if (isInhabitant) {
                if ((authInfo == null) || ! authInfo.isOKDeep()) {
                    offendingClassNames.add(internalClassName);
                } else {
                    if (okClassNames != null) {
                        okClassNames.add(internalClassName);
                    }
                }
            }
            return typeAnalyzer.commandAuthInfo();
        } catch (Exception ex) {
            throw new MojoExecutionException("Error analyzing " + internalClassName, ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    getLog().warn("Error closing input stream for " + internalClassName + "; " + ex.getLocalizedMessage());
                }
            }
        }
        
    }
    
//    public CommandAuthorizationInfo processConfigBean(final Inhabitant configBean) throws MojoFailureException, MojoExecutionException {
//        /*
//         * Find the byte code for this class so we can analyze it.
//         */
//        final String internalClassName = configBean.configBeanClassName;
//        final String resourcePath = internalClassName.replace('.','/') + ".class";
//        final InputStream is = loader.getResourceAsStream(resourcePath);
//        if (is == null) {
//            throw new MojoFailureException("Cannot locate byte code for inhabitant class " + resourcePath);
//        }
//        try {
//            
//        } catch (Exception ex) {
//            throw new MojoExecutionException("Error analyzing " + internalClassName, ex);
//        } finally {
//            if (is != null) {
//                try {
//                    is.close();
//                } catch (IOException ex) {
//                    getLog().warn("Error closing input stream for " + internalClassName + "; " + ex.getLocalizedMessage());
//                }
//            }
//        }
//    }
    
    private void setUpKnownTypes() throws InstantiationException, IllegalAccessException {
        knownCommandTypes = getOrCreate(KNOWN_AUTH_TYPES_NAME, knownCommandTypes);
        knownNonCommandTypes = getOrCreate(KNOWN_NONCOMMAND_TYPES_NAME, knownNonCommandTypes);
        knownCRUDConfigBeanTypes = getOrCreate(KNOWN_CRUD_CONFIG_BEAN_TYPES_NAME, knownCRUDConfigBeanTypes);
        jarsProcessedForConfigBeans = getOrCreate(PROCESSED_MODULES_NAME, jarsProcessedForConfigBeans);
        configBeans = getOrCreate(CONFIG_BEANS_NAME, configBeans);
    }
    
    private <T,U> Map<T,U> getOrCreate(final String propertyName, final Map<T,U> m) {
        Map<T,U> collection = (Map<T,U>) getSessionProperties().get(propertyName);
        if (collection == null) {
            collection = new HashMap<T,U>();
            getSessionProperties().put(propertyName, collection);
        }
        return collection;
    }
    
    private <T> Set<T> getOrCreate(final String propertyName, final Set<T> s) {
        Set<T> collection = (Set<T>) getSessionProperties().get(propertyName);
        if (collection == null) {
            collection = new HashSet<T>();
            getSessionProperties().put(propertyName, collection);
        }
        return collection;
    }
    
    private Collection<Inhabitant> findCommandInhabitants(final Collection<Inhabitant> inhabitants) {
        final List<Inhabitant> result = new ArrayList<Inhabitant>();
        for (Inhabitant inh : inhabitants) {
            if (inh.contracts.contains(ADMIN_COMMAND_NAME) ||
                inh.contracts.contains(CLI_COMMAND_NAME)) {
                if (trace != null) {
                    trace.append(LINE_SEP).append(" Inhabitant ").append(inh.className).append(" seems to be a command");
                }
                result.add(inh);
            }
        }
        return result;
    }
    
//    private Collection<Inhabitant> findCRUDCommandInhabitants(final Collection<Inhabitant> inhabitants) {
//        final List<Inhabitant> result = new ArrayList<Inhabitant>();
//        for (Inhabitant inh : inhabitants) {
//            if (inh.contracts.contains(CONFIG_INJECTOR_NAME)) {
//                if (trace != null) {
//                    trace.append(LINE_SEP).append(" Inhabitant ").append(inh.metadataTarget).append(" seems to be a ConfigBean");
//                }
//                result.add(inh);
//            }
//        }
//        return result;
//    }
    
    private Properties getSessionProperties() {
        return session.getUserProperties();
    }
    
    
    private List<Inhabitant> findInhabitantsInModule() throws IOException {
        final File inhabFile = new File(buildDir, INHABITANTS_PATH);
        return findInhabitantsInModule(inhabFile);
    }
    
    private List<Inhabitant> findInhabitantsInModule(final File inhabFile) throws FileNotFoundException, IOException {
        if ( ! inhabFile.canRead()) {
            getLog().debug("Cannot read " + inhabFile.getAbsolutePath());
            return Collections.EMPTY_LIST;
        }
        
        return findInhabitantsInModule(new InputStreamReader(new BufferedInputStream(new FileInputStream(inhabFile))));
    }
    
    private List<Inhabitant> findInhabitantsInModule(final Reader r) throws IOException {
        
        /*
         * Inhabitants files look like this:
         * 
         * [implementation-class-name]
         * ...
         * contract={contract-1-name,constract-2-name,...}
         * ...
         * (blank line)
         * [next-implementation-class-name]
         * ...
         */
        
        final List<Inhabitant> result = new ArrayList<Inhabitant>();
        
        final LineNumberReader rdr = new LineNumberReader(r);

        String line;

        String implClassName;
//        String methodListActual = null;
//        String methodName = null;
//        String parentConfigured = null;
        
//        List<String> contracts = new ArrayList<String>();
//        String serviceName = null;
        
        /*
         * This Inhabitant accumulates information from several successive
         * lines in the hk2 inhabitants file.  Once we detect the end of this
         * inhabitants info - either a blank line or the end of the file -
         * we process this inhabitant.
         */
        Inhabitant inhabitant = null;
        
        try {
            while ((line = rdr.readLine()) != null) {
                final int commentSlot = line.indexOf('#');
                if (commentSlot != -1) {
                    line = line.substring(0, commentSlot);
                }
                line = line.trim();
                if (line.isEmpty()) {
                    inhabitant = null;
                    continue;
                }
                
                final Matcher implClassNameMatcher = INHABITANT_IMPL_CLASS_PATTERN.matcher(line);
                if (implClassNameMatcher.matches()) {
                    implClassName = implClassNameMatcher.group(1);
                    getLog().debug("Detected start of inhabitant: " + implClassName);
//                    inhabitant = configBeans.get(implClassName);
//                    if (inhabitant == null) {
//                        inhabitant = new Inhabitant(implClassName);
//                    }
                    /*
                     * Add the inhabitant to the result as soon as we start
                     * processing it.
                     */
                    inhabitant = new Inhabitant(implClassName);
                    result.add(inhabitant);
                } else {
                    final Matcher contractsMatcher = INHABITANT_CONTRACTS_PATTERN.matcher(line);
                    if (contractsMatcher.matches()) {
                        inhabitant.contracts.addAll(Arrays.asList(contractsMatcher.group(1).split(",")));
                    } else {
                        final Matcher nameMatcher = INHABITANT_NAME_PATTERN.matcher(line);
                        if (nameMatcher.matches()) {
                            inhabitant.serviceName = nameMatcher.group(1);
                        } else {
                            if (GENERIC_CRUD_COMMAND_CLASS_NAMES.contains(inhabitant.className)) {
                                final Matcher genericInfoMatcher = GENERIC_COMMAND_INFO_PATTERN.matcher(line);
                                if (genericInfoMatcher.matches()) {
                                    inhabitant.methodListActual = genericInfoMatcher.group(1);
                                    inhabitant.methodName = genericInfoMatcher.group(2);
                                    inhabitant.parentConfigured = genericInfoMatcher.group(3);
                                    getLog().debug("Recognized generic command " + inhabitant.serviceName);
                                    inhabitant.action = genericCommandNameToAction.get(inhabitant.className);
                                    Inhabitant configBeanParent = configBeans.get(inhabitant.parentConfigured);
                                    if (configBeanParent == null) {
                                        configBeanParent = new Inhabitant(inhabitant.parentConfigured);
                                        configBeans.put(configBeanParent.className, configBeanParent);
                                        getLog().debug("Created parent bean " + configBeanParent.className + " for target bean " + inhabitant.methodListActual);
                                    } else {
                                        getLog().debug("Found parent bean " + configBeanParent.className + " for target bean " + inhabitant.methodListActual);
                                    }
                                    
                                    Inhabitant configBean = configBeans.get(inhabitant.methodListActual);
                                    if (configBean == null) {
                                        configBean = new Inhabitant(inhabitant.methodListActual);
                                        configBeans.put(configBean.className, configBean);
                                        getLog().debug("Created new config bean for " + configBean.className);
                                    } else {
                                        getLog().debug("Found existing config bean for " + configBean.className);
                                    }
                                    configBean.parent = configBeanParent;
                                    inhabitant.configBeanForCommand = configBean;
                                }
                            }
                        }
                    }
                }
                
                /*
                 * If the previously-found contracts include ConfigInjector then
                 * this is a config bean.  Try matching the config bean metadata.
                 */
                final Matcher configBeanPrefixMatcher = CONFIG_BEAN_METADATA_PREFIX_PATTERN.matcher(line);
                if (configBeanPrefixMatcher.matches()) {
                    final String configBeanClassName = configBeanPrefixMatcher.group(1);
                    getLog().debug("Recognized " + configBeanClassName + " as a config bean");
                    Inhabitant configBean = configBeans.get(configBeanClassName);
                    if (configBean == null) {
                        configBean = new Inhabitant(configBeanClassName);
                        configBeans.put(configBeanClassName, configBean);
                    }
                    
                    /*
                     * If the prefix has a group 2 then that is the part we
                     * need to parse for children.
                     */
                    final String restOfLine = configBeanPrefixMatcher.group(2);
                    if ( restOfLine != null && 
                            restOfLine.length() > 0) {
                        final Matcher configBeanChildMatcher = 
                                CONFIG_BEAN_CHILD_PATTERN.matcher(restOfLine);
                        while (configBeanChildMatcher.find()) {
                            /*
                             * Make sure we have group 1.  Otherwise we matched
                             * one of the variants that we are not interested in
                             * (such as an attribute declaration).
                             */
                            if (configBeanChildMatcher.groupCount() > 0 && configBeanChildMatcher.group(1) != null) {
                                final String childClassName = configBeanChildMatcher.group(2);
                                final String subpathInParent = configBeanChildMatcher.group(1);
                                getLog().debug("Identified " + childClassName + " as child " + subpathInParent + " of " + configBean.className);
                                Inhabitant childInh = configBeans.get(childClassName);
                                if (childInh == null) {
                                    childInh = new Inhabitant(childClassName);
                                    configBeans.put(childClassName, childInh);
                                    getLog().debug("Added child inhabitant to configBeans");
                                } else {
                                    getLog().debug("Found child as previously-defined config bean");
                                }
                                getLog().debug("Assigning " + configBean.className + " as parent of " + childInh.className);
                                childInh.parent = configBean;

                                if (configBean.children == null) {
                                    configBean.children = new HashMap<String,Child>();
                                }
                                Child child = configBean.children.get(childClassName);
                                if (child == null) {
                                    child = new Child(subpathInParent, childInh);
                                    configBean.children.put(childClassName, child);
                                    getLog().debug("Adding config bean " + childClassName + " as child " + subpathInParent + " to config bean " + configBean.className);
                                }
                            }
                        }
                    }
                }
            }
//                /*
//                 * See if this named inhabitant already exists among the config
//                 * beans we know about.
//                 */
//                Inhabitant configBean = configBeans.get(inhabitant.className);
//                if (configBean == null) {
//                    configBean = new Inhabitant(implClassName, contracts, serviceName,
//                        methodListActual, methodName, parentConfigured);
//                    result.add(configBean);
//                } else {
//                    configBean.set(contracts, serviceName, methodListActual, methodName, parentConfigured);
//                    Inhabitant parent = configBeans.get(parentConfigured);
//                    if (parent == null) {
//                        parent = new Inhabitant(parentConfigured);
//                        configBeans.put(parentConfigured, parent);
//                    }
//                    configBean.parent = parent;
//                }
        } finally {
            rdr.close();
        }
        return result;
    }
    
//    private List<Inhabitant> findInhabitantsInModule() throws IOException {
//        /*
//         * class=com.sun.enterprise.admin.cli.LoginCommand,index=com.sun.enterprise.admin.cli.CLICommand:login
//         */
//        final File inhabFile = new File(buildDir, INHABITANTS_PATH);
//        if ( ! inhabFile.canRead()) {
//            return Collections.EMPTY_LIST;
//        }
//        
//        final List<Inhabitant> result = new ArrayList<Inhabitant>();
//        
//        final LineNumberReader rdr = new LineNumberReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(inhabFile))));
//
//        String line;
//
//        try {
//            while ((line = rdr.readLine()) != null) {
//                final int commentSlot = line.indexOf('#');
//                if (commentSlot != -1) {
//                    line = line.substring(0, commentSlot);
//                }
//                line.trim();
//                if (line.isEmpty()) {
//                    continue;
//                }
//                String className = null;
//                final List<HK2_Index> indexes = new ArrayList<HK2_Index>();
//
//                final String[] segments = line.split(",");
//                for (String segment : segments) {
//                    final Matcher m = INHABITANT_DESCR_PATTERN.matcher(segment);
//                    if (m.matches()) {
//                        if (m.group(1).equals("class")) {
//                            className = m.group(2);
//                        } else if (m.group(1).equals("index")) {
//                            indexes.add(new HK2_Index(m.group(2), m.groupCount() < 3 ? null : m.group(3)));
//                        } else {
//                            getLog().debug("Weird: scanning line '" + line + "' groups are " + m.group(1) + ", " + m.group(2) + (m.groupCount() < 3 ? "" : ", " + m.group(3)));
//                        }
//                    }
//                }
//                result.add(new Inhabitant(className, indexes));
//                if (trace != null) {
//                    trace.append(LINE_SEP).append("Adding inhabitant ").append(className);
//                }
//            }
//        } finally {
//            rdr.close();
//        }
//        return result;
//    }
    
    private URLClassLoader createClassLoader() throws MojoExecutionException {
        final List<String> compileClasspathElements;
        try {
            compileClasspathElements = project.getRuntimeClasspathElements();
            final URL[] urls = new URL[compileClasspathElements.size()];
            int urlSlot = 0;

            for (String cpElement : compileClasspathElements) {
                getLog().debug(" Processing class path element " + cpElement);
                urls[urlSlot++] = new File(cpElement).toURI().toURL();
            }
            
            return new URLClassLoader(urls);
            
        } catch (DependencyResolutionRequiredException ex) {
            throw new MojoExecutionException("Error fetching compile-time classpath", ex);
        } catch (MalformedURLException ex) {
            throw new MojoExecutionException("Error processing class path URL segment", ex);
        }
    }
    
    public static class Inhabitant {
        
        private enum GenericCommand {
            CREATE(GENERIC_CREATE_COMMAND, "create"),
            DELETE(GENERIC_DELETE_COMMAND, "delete"),
            LIST(GENERIC_LIST_COMMAND, "read"),
            UNKNOWN("","????");

            private final String commandType;
            private final String action;
            GenericCommand(final String commandType, final String action) {
                this.commandType = commandType;
                this.action = action;
            }

            String action() {
                return action;
            }

            static GenericCommand match(final String commandType) {
                for (GenericCommand gc : GenericCommand.values()) {
                    if (gc.commandType.equals(commandType)) {
                        return gc;
                    }
                }
                return UNKNOWN;
            }

        }
        
        private List<String> contracts = new ArrayList<String>();
//        private final Map<String,String> indexes = new HashMap<String,String>();
        
        private boolean isFilledIn = false;
        private String className;
        private String serviceName;
        private String methodListActual;
        private String methodName;
        private String parentConfigured;
        private Inhabitant parent = null;
        private String nameInParent = null;
        private String action;
        private Inhabitant configBeanForCommand = null;
        
        private Map<String,Child> children = null;
        
        private Inhabitant() {}
        
        private Inhabitant(final String className) {
            this.className = className;
        }
        
        private Inhabitant(final String className, final List<String> contracts, 
                final String serviceName,
                final String methodListActual,
                final String methodName,
                final String parentConfigured) {
            this.className = className;
            this.contracts.addAll(contracts);
            this.serviceName = serviceName;
            this.methodListActual = methodListActual;
            this.methodName = methodName;
            this.parentConfigured = parentConfigured;
            this.action = GenericCommand.match(className).action;
            this.isFilledIn = true;
        }
        
        void setParent(final Inhabitant p) {
            parent = p;
        }
        
        void set(final List<String> contracts, final String serviceName,
                final String methodListActual,
                final String methodName,
                final String parentConfigured) {
            this.contracts = contracts;
            this.serviceName = serviceName;
            this.methodListActual = methodListActual;
            this.methodName = methodName;
            this.parentConfigured = parentConfigured;
            isFilledIn = true;
        }
        
        boolean isFilledIn() {
            return isFilledIn;
        }
        
        Inhabitant parent() {
            return parent;
        }
        
        String nameInParent() {
            return nameInParent;
        }
        
        String fullPath() {
            final StringBuilder path = new StringBuilder();
            for (Inhabitant i = (configBeanForCommand != null ? configBeanForCommand : this); i != null; i = i.parent) {
                if (path.length() > 0) {
                    path.insert(0, '/');
                }
                final Inhabitant p = i.parent;
                if (p != null) {
                    Child childForThisInh = null;
                    if (p.children != null && ((childForThisInh = p.children.get(i.className)) != null)) {
                        path.insert(0, childForThisInh.subpathInParent);
                    } else {
                        path.insert(0, Util.convertName(Util.lastPart(i.className)));
                    }
                } else {
                    path.insert(0, Util.convertName(Util.lastPart(i.className)));
                }
                
            }
            return path.toString();
        }
        
//        private static String chooseAction(final String className) {
//            if (className.contains("Create")) {
//                return "create";
//            } else if (className.contains("Delete")) {
//                return "delete";
//            } else if (className.contains("List")) {
//                return "list";
//            }
//            return "????";
//        }
    }
    
    static class Child {
        private String subpathInParent;
        private Inhabitant child;
        
        Child(final String subpathInParent, final Inhabitant childInh) {
            this.subpathInParent = subpathInParent;
            child = childInh;
        }
    }
    
//    static class ConfigBeanInhabitant  {
//        
//        
//        private final String beanName;
//        private String metadataTarget = null;
//        
//        private ConfigBeanInhabitant(final String beanName, final List<String> contracts, final String name) {
//            this.beanName = beanName;
//        }
//        private static String findTarget(final String metadata) {
//            if (metadata == null) {
//                return null;
//            }
//            String result = null;
//            final Matcher m = CONFIG_BEAN_NAME_PATTERN.matcher(metadata);
//            if (m.matches()) {
//                result = m.group(1);
//            }
//            return result;
//        }
////        private Inhabitant(final String className, final List<HK2_Index> indexes) {
////            this.className = className;
////            for (HK2_Index i : indexes) {
////                this.indexes.put(i.indexName, i.serviceName);
////            }
////        }
//    }

//    private static  class HK2_Index {
//        private String indexName;
//        private String serviceName;
//        
//        private HK2_Index(final String indexName, final String serviceName) {
//            this.indexName = indexName;
//            this.serviceName = serviceName;
//        }
//    }
    
    
    
}
