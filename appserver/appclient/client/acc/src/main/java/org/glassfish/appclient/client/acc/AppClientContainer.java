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

package org.glassfish.appclient.client.acc;

import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.InjectionException;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.security.webservices.ClientPipeCloser;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;

import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import com.sun.logging.LogDomains;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.security.auth.callback.CallbackHandler;
import javax.swing.SwingUtilities;
import javax.transaction.Status;
import javax.transaction.TransactionManager;
import org.apache.naming.resources.DirContextURLStreamHandlerFactory;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.appclient.client.acc.config.AuthRealm;
import org.glassfish.appclient.client.acc.config.ClientCredential;
import org.glassfish.appclient.client.acc.config.MessageSecurityConfig;
import org.glassfish.appclient.client.acc.config.Property;
import org.glassfish.appclient.client.acc.config.Security;
import org.glassfish.appclient.client.acc.config.TargetServer;
import org.glassfish.persistence.jpa.PersistenceUnitLoader;
import com.sun.enterprise.container.common.spi.ManagedBeanManager;


import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.xml.sax.SAXParseException;

/**
 * Embeddable Glassfish app client container (ACC).
 * <p>
 * Allows Java programs to:
 * <ul>
 * <li>create a new builder for an ACC (see {@link #newBuilder} and {@link AppClientContainerBuilder}),
 * <li>optionally modify the configuration by invoking various builder methods,
 * <li>create an embedded instance of the ACC from the builder using {@link AppClientContainerBuilder#newContainer() },
 * <li>startClient the client using {@link #startClient(String[])}, and
 * <li>stop the container using {@link #stop()}.
 * </ul>
 * <p>
 * Each instance of the {@link TargetServer} class passed to the <code>newBuilder</code>
 * method represents one
 * server, conveying its host and port number, which the ACC can use to
 * "bootstrap" into the server-side ORB(s).  The calling
 * program can request to use secured communication to a server by also passing
 * an instance of the {@link Security} configuration class when it creates the <code>TargetServer</code>
 * object.  Note that the caller prepares the <code>TargetServer</code>
 * array completely before passing it to one of the <code>newConfig</code>
 * factory methods.
 * The <code>Builder</code> implementation
 * does not override or augment the list of target servers using
 * system property values, property settings in the container configuration, etc.  If such work
 * is necessary to find additional target servers the calling program should do it
 * and prepare the array of <code>TargetServer</code> objects accordingly.
 * <p>
 * The calling program also passes either a File or URI for the app client
 * archive to be run or a Class object for the main class to be run as an app client.
 * <p>
 * After the calling program has created a new <code>AppClientContainer.Builder</code> instance
 * it can set optional
 * information to control the ACC's behavior, such as
 * <ul>
 * <li>setting the authentication realm
 * <li>setting client credentials
 * (and optionally setting an authentication realm in which the username and password
 * are valid)
 * <li>setting the callback handler class
 * <li>adding one or more {@link MessageSecurityConfig} objects
 * </ul>
 * <p>
 * Once the calling program has used the builder to configure the ACC to its liking it invokes the
 * builder's <code>newContainer()</code> method.
 * The return type is an <code>AppClientContainer</code>, and by the time
 * <code>newContainer</code> returns the <code>AppClientContainer</code>
 * has invoked the app client's main method and that method has returned to the ACC.
 * Any new thread the client creates or any GUI work it triggers on the AWT
 * dispatcher thread continues independently from the thread that called <code>newContainer</code>.
 * <p>
 * If needed, the calling program can invoke the <code>stop</code> method on
 * the <code>AppClientContainer</code> to shut down the ACC-provided services.
 * Invoking <code>stop</code> does not stop any
 * threads the client might have started.  If the calling program needs to
 * control such threads it should do so itself, outside the <code>AppClientContainer</code>
 * API.  If the calling program does not invoke <code>stop</code> the ACC will
 * clean up automatically as the JVM exits.
 * <p>
 * A simple case in which the calling program provides an app client JAR file and
 * a single TargetServer might look like this:
 * <p>
 * <code>
 *
 * import org.glassfish.appclient.client.acc.AppClientContainer;<br>
 * import org.glassfish.appclient.client.acc.config.TargetServer;<br>
 * <br>
 * AppClientContainerBuilder builder = AppClientContainer.newBuilder(<br>
 * &nbsp;&nbsp;    new TargetServer("localhost", 3700));<br>
 * <br>
 * AppClientContainer acc = builder.newContainer(new File("myAC.jar").toURI());<br>
 * <br>
 * </code>(or, alternatively)<code><br>
 * <br>
 * AppClientContainer acc = builder.newContainer(MyClient.class);<br>
 * <br>
 * <br</code>Then, <code><br>
 * <br>
 * acc.startClient(clientArgs);<br>
 * // The newContainer method returns as soon as the client's main method returns,<br>
 * // even if the client has started another thread or is using the AWT event<br>
 * // dispatcher thread
 * <br>
 * // At some later point, the program can synchronize with the app client in<br>
 * // a user-specified way at which point it could invoke<br>
 * <br>
 * acc.stop();<br>
 * <br>
 * </code>
 * <p>
 * Public methods on the Builder interfaces which set configuration information return the
 * Builder object itself.  This allows the calling program to chain together
 * several method invocations, such as
 * <p>
 * <code>
 * AppClientContainerBuilder builder = AppClientContainer.newBuilder(...);<br>
 * builder.clientCredentials(myUser, myPass).logger(myLogger);<br>
 * </code>
 *
 * @author tjquinn
 */
@Service
@PerLookup
public class AppClientContainer {

    // XXX move this
    /** Prop name for keeping temporary files */
    public static final String APPCLIENT_RETAIN_TEMP_FILES_PROPERTYNAME = "com.sun.aas.jws.retainTempFiles";

    private static final Logger logger = LogDomains.getLogger(AppClientContainer.class,
            LogDomains.ACC_LOGGER);

    private static final Logger _logger = Logger.getLogger(AppClientContainer.class.getName());

    /**
     * Creates a new ACC builder object, preset with the specified
     * target servers.
     *
     * @param targetServers server(s) to contact during ORB bootstrapping
     * @return <code>AppClientContainer.Builder</code> object
     */
    public static AppClientContainer.Builder newBuilder(
            final TargetServer[] targetServers) {
        return new AppClientContainerBuilder(targetServers);
    }

//    /**
//     * Creates a new ACC builder object.
//     * <p>
//     * This variant could be invoked, for example, from the main method of
//     * our main class in the facade JAR file generated during deployment.  If
//     * such a generated JAR is launched directly using a java command (and
//     * not the appclient script) then that class would have no way to find
//     * any configuration information.
//     *
//     * @return <code>AppClientContainer.Builder</code> object
//     */
//    public static AppClientContainer.Builder newBuilder() {
//        return new AppClientContainerBuilder();
//    }

    @Inject
    private AppClientContainerSecurityHelper secHelper;

    @Inject
    private InjectionManager injectionManager;

    @Inject
    private InvocationManager invocationManager;

    @Inject
    private ComponentEnvManager componentEnvManager;

    @Inject
    private ConnectorRuntime connectorRuntime;

    @Inject
    private ServiceLocator habitat;

    private Builder builder;

    private Cleanup cleanup = null;

    private State state = State.INSTANTIATED; // HK2 will create the instance

    private ClientMainClassSetting clientMainClassSetting = null;

    private URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();

    private Collection<EntityManagerFactory> emfs = null;

//    private boolean isJWS = false;

    private Launchable client = null;

    private CallbackHandler callerSuppliedCallbackHandler = null;

    /** returned from binding the app client to naming; used in preparing component invocation */
    private String componentId = null;


    /*
     * ********************* ABOUT INITIALIZATION ********************
     *
     * Note that, internally, the AppClientContainerBuilder's newContainer
     * methods use HK2 to instantiate the AppClientContainer object (so we can
     * inject references to various other services).
     *
     * The newContainer method then invokes one of the ACC's
     * <code>prepare</code> methods to initialize the ACC fully.  All that is
     * left at that point is for the client's main method to be invoked.
     *
     */

    public void startClient(String[] args) throws Exception, UserError {
        prepare(null);
        launch(args);
    }

    void prepareSecurity(final TargetServer[] targetServers,
            final List<MessageSecurityConfig> msgSecConfigs,
            final Properties containerProperties,
            final ClientCredential clientCredential,
            final CallbackHandler callerSuppliedCallbackHandler,
            final URLClassLoader classLoader,
            final boolean isTextAuth) throws InstantiationException,
                IllegalAccessException, InjectionException, ClassNotFoundException,
                IOException,
                SAXParseException {
        secHelper.init(targetServers, msgSecConfigs, containerProperties, clientCredential,
                callerSuppliedCallbackHandler, classLoader, client.getDescriptor(classLoader),
                isTextAuth);
    }

    void setCallbackHandler(final CallbackHandler callerSuppliedCallbackHandler) {
        this.callerSuppliedCallbackHandler = callerSuppliedCallbackHandler;
    }

    void setBuilder(final Builder builder) {
        this.builder = builder;
    }

    public void prepare(final Instrumentation inst) throws NamingException, 
            IOException, InstantiationException, IllegalAccessException,
            InjectionException, ClassNotFoundException, SAXParseException,
            NoSuchMethodException, UserError {
        completePreparation(inst);
    }

    void setClient(final Launchable client) throws ClassNotFoundException {
        this.client = client;
        clientMainClassSetting = ClientMainClassSetting.set(client.getMainClass());

    }
    
    void processPermissions() throws IOException {
        //need to process the permissions files
        if (classLoader instanceof ACCClassLoader) {
            ((ACCClassLoader)classLoader).processDeclaredPermissions();
        }
    }
    
    protected Class loadClass(final String className) throws ClassNotFoundException {
        return Class.forName(className, true, classLoader);
    }

    protected ClassLoader getClassLoader() {
        return classLoader;
    }
    
    /**
     * Gets the ACC ready so the main class can run.
     * This can be followed, immediately or after some time, by either an
     * invocation of {@link #launch(java.lang.String[])  or
     * by the JVM invoking the client's main method (as would happen during
     * a <code>java -jar theClient.jar</code> launch.
     *
     * @throws java.lang.Exception
     */
    private void completePreparation(final Instrumentation inst) throws 
            NamingException, IOException, InstantiationException,
            IllegalAccessException, InjectionException, ClassNotFoundException,
            SAXParseException, NoSuchMethodException, UserError {
        if (state != State.INSTANTIATED) {
            throw new IllegalStateException();
        }

        /*
         * Attach any names defined in the app client.  Validate the descriptor
         * first, then use it to bind names in the app client.  This order is
         * important - for example, to set up message destination refs correctly.
         */
        client.validateDescriptor();
        final ApplicationClientDescriptor desc = client.getDescriptor(classLoader);
        componentId = componentEnvManager.bindToComponentNamespace(desc);

        /*
         * Arrange for cleanup now instead of during launch() because in some use cases
         * the JVM will invoke the client's main method itself and launch will
         * be skipped.
         */
        cleanup = Cleanup.arrangeForShutdownCleanup(logger, habitat, desc);
        
        /*
         * Allow pre-destroy handling to work on the main class during clean-up.
         */
        cleanup.setInjectionManager(injectionManager, 
                clientMainClassSetting.clientMainClass);

        /*
         * If this app client contains persistence unit refs, then initialize
         * the PU handling.  
         */
        Collection<? extends PersistenceUnitDescriptor> referencedPUs = desc.findReferencedPUs();
        if (referencedPUs != null && ! referencedPUs.isEmpty()) {

            ProviderContainerContractInfoImpl pcci = new ProviderContainerContractInfoImpl(
                    (ACCClassLoader) getClassLoader(), inst, client.getAnchorDir(), connectorRuntime);
            for (PersistenceUnitDescriptor puDesc : referencedPUs) {
                PersistenceUnitLoader pul = new PersistenceUnitLoader(puDesc, pcci);
                desc.addEntityManagerFactory(puDesc.getName(), pul.getEMF());
            }

            cleanup.setEMFs(pcci.emfs());
        }

        cleanup.setConnectorRuntime(connectorRuntime);

        prepareURLStreamHandling();

        //This is required for us to enable interrupt jaxws service
        //creation calls
        System.setProperty("javax.xml.ws.spi.Provider",
                           "com.sun.enterprise.webservice.spi.ProviderImpl");
        //InjectionManager's injectClass will be called from getMainMethod


        // Load any managed beans
        ManagedBeanManager managedBeanManager = habitat.getService(ManagedBeanManager.class);
        managedBeanManager.loadManagedBeans(desc.getApplication());
        cleanup.setManagedBeanManager(managedBeanManager);

        /**
         * We don't really need the main method here but we do need the side-effects.
         */
        getMainMethod();

        state = State.PREPARED;
    }

    public void launch(String[] args) throws
            NoSuchMethodException,
            ClassNotFoundException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            IOException,
            SAXParseException,
            InjectionException,
            UserError {

        if (state != State.PREPARED) {
            throw new IllegalStateException();
        }
        Method mainMethod = getMainMethod();
        // build args to the main and call it
        Object params [] = new Object [1];
        params[0] = args;

        if (logger.isLoggable(Level.FINE)) {
            dumpLoaderURLs();
        }
        mainMethod.invoke(null, params);
        state = State.STARTED;

        /*
         * We need to clean up when the EDT ends or, if there is no EDT, right
         * away.  In particular, JMS/MQ-related non-daemon threads might still
         * be running due to open queueing connections.
         */
        cleanupWhenSafe();
    }

    private boolean isEDTRunning() {
        Map<Thread,StackTraceElement[]> threads = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Map<Thread,StackTraceElement[]>>() {

            @Override
            public Map<Thread, StackTraceElement[]> run() {
                return Thread.getAllStackTraces();
            }
        });

        logger.fine("Checking for EDT thread...");
        for (Map.Entry<Thread,StackTraceElement[]> entry : threads.entrySet()) {
            logger.log(Level.FINE, "  {0}", entry.getKey().toString());
            StackTraceElement[] frames = entry.getValue();
            if (frames.length > 0) {
                StackTraceElement last = frames[frames.length - 1];
                if (last.getClassName().equals("java.awt.EventDispatchThread") &&
                    last.getMethodName().equals("run")) {
                    logger.log(Level.FINE, "Thread {0} seems to be the EDT", entry.getKey().toString());
                    return true;
                }
            }
            logger.fine("Did not recognize any thread as the EDT");
        }
        return false;
    }

    private void cleanupWhenSafe() {
        if (isEDTRunning()) {
            final AtomicReference<Thread> edt = new AtomicReference<Thread>();
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        edt.set(Thread.currentThread());
                    }
                });
                edt.get().join();
            } catch (Exception e) {

            }
        }
        stop();
    }

    private void dumpLoaderURLs() {
        final String sep = System.getProperty("line.separator");
        final ClassLoader ldr = Thread.currentThread().getContextClassLoader();
        if (ldr instanceof ACCClassLoader) {
            final ACCClassLoader loader = (ACCClassLoader) ldr;
            final URL[] urls = loader.getURLs();

            final StringBuilder sb = new StringBuilder("Class loader URLs:");
            for (URL url : urls) {
                sb.append("  ").append(url.toExternalForm()).append(sep);
            }
            sb.append(sep);
            logger.fine(sb.toString());
        }
    }
    
    private Method getMainMethod() throws NoSuchMethodException,
           ClassNotFoundException, IOException, SAXParseException,
           InjectionException, UserError {
	    // determine the main method using reflection
	    // verify that it is public static void and takes
	    // String[] as the only argument
	    Method result = null;

        result = clientMainClassSetting.getClientMainClass(
                classLoader,
                injectionManager,
                invocationManager,
                componentId,
                this,
                client.getDescriptor(classLoader)).getMethod("main",
                    new Class[] { String[].class } );

	    // check modifiers: public static
	    int modifiers = result.getModifiers ();
	    if (!Modifier.isPublic (modifiers) ||
		!Modifier.isStatic (modifiers))  {
		    final String err = MessageFormat.format(logger.getResourceBundle().
                            getString("appclient.notPublicOrNotStatic"), (Object[]) null);
	    	    throw new NoSuchMethodException(err);
	    }

	    // check return type and exceptions
	    if (!result.getReturnType().equals (Void.TYPE)) {
                final String err = MessageFormat.format(logger.getResourceBundle().
                        getString("appclient.notVoid"), (Object[]) null);
                throw new NoSuchMethodException(err);
	    }
        return result;
    }

    
    /**
     * Stops the app client container.
     * <p>
     * Note that the calling program should not stop the ACC if there might be
     * other threads running, such as the Swing event dispatcher thread.  Stopping
     * the ACC can shut down various services that those continuing threads might
     * try to use.
     * <p>
     * Also note that stopping the ACC will have no effect on any thread that
     * the app client itself might have created.  If the calling program needs
     * to control such threads it and the client code running in the threads
     * should agree on how they will communicate with each other.  The ACC cannot
     * help with this.
     */
    public void stop() {
        /*
         * Because stop can be invoked automatically at the end of launch, allow
         * the developer's driver program to invoke stop again without penalty.
         */
        if (state == State.STOPPED) {
            return;
        }
        if ( state != State.STARTED) {
            throw new IllegalStateException();
        }
        cleanup.start();
        state = State.STOPPED;
    }

    /**
     * Records how the main class has been set - by name or by class - and
     * encapsulates the retrieval of the main class.
     */
    enum ClientMainClassSetting {
        BY_NAME,
        BY_CLASS;

        static String clientMainClassName;
        static volatile Class clientMainClass;
        static boolean isInjected = false;

        static ClientMainClassSetting set(final String name) {
            clientMainClassName = name;
            clientMainClass = null;
            return BY_NAME;
        }

        static ClientMainClassSetting set(final Class cl) {
            clientMainClass = cl;
            clientMainClassName = null;
            return BY_CLASS;
        }

        static Class getClientMainClass(final ClassLoader loader,
                InjectionManager injectionManager,
                InvocationManager invocationManager,
                String componentId,
                AppClientContainer container,
                ApplicationClientDescriptor acDesc) throws ClassNotFoundException,
                    InjectionException, UserError {
            if (clientMainClass == null) {
                if (clientMainClassName == null) {
                    throw new IllegalStateException("neither client main class nor its class name has been set");
                }
                clientMainClass = Class.forName(clientMainClassName, true, loader);
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Loaded client main class {0}", clientMainClassName);
                }
            }
            ComponentInvocation ci = new ComponentInvocation(
                    componentId,
                    ComponentInvocation.ComponentInvocationType.APP_CLIENT_INVOCATION,
                    container,
                    acDesc.getApplication().getAppName(),
                    acDesc.getModuleName());
            
            invocationManager.preInvoke(ci);
            InjectionException injExc = null;
            if ( ! isInjected) {
                int retriesLeft = Integer.getInteger("org.glassfish.appclient.acc.maxLoginRetries", 3);
                while (retriesLeft > 0 && ! isInjected) {
                    injExc = null;
                    try {
                        injectionManager.injectClass(clientMainClass, acDesc);
                        isInjected = true;
                    } catch (InjectionException ie) {
                        Throwable t = ie;
                        boolean isAuthError = false;
                        if (container.secHelper.isLoginCancelled()) {
                            throw new UserError(logger.getResourceBundle().getString("appclient.userCanceledAuth"));
                        }
                        while (t != null && ! isAuthError) {
                            isAuthError = t instanceof org.omg.CORBA.NO_PERMISSION;
                            t = t.getCause();
                        }                        
                        if (isAuthError) {
                            injExc = ie;
                            container.secHelper.clearClientSecurityContext();
                            retriesLeft--;
                        } else {
                            throw ie;
                        }
                    }
                }
                if (injExc != null) {
                    /*
                     * Despite retries, the credentials were not accepted.
                     * Throw a user error which the ACC will display nicely.
                     */
                    if (injExc.getCause() != null &&
                        injExc.getCause() instanceof NamingException) {
                        final NamingException ne = (NamingException) injExc.getCause();
                        final String expl = ne.getExplanation();
                        final String msg = MessageFormat.format(
                                logger.getResourceBundle().getString("appclient.RemoteAuthError"), expl);
                        throw new UserError(msg);
                    }
                }
            }
            return clientMainClass;
        }
    }

    /**
     * Records the current state of the ACC.
     */
    enum State {
        /**
         * HK2 has created the ACC instance
         */
        INSTANTIATED,

        /**
         * ACC is ready for the client to run
         */
        PREPARED,

        /**
         * the ACC has started the client.
         * <p>
         * Note that if the user launches the client JAR directly (using
         * java -jar theClient.jar) the ACC will not be aware of this and
         * so the state remains PREPARED.
         */
        STARTED,

        /**
         * the ACC has stopped in response to a request from the calling
         * program
         */
        STOPPED;
    }

    /**
     * Sets the name of the main class to be executed.
     * <p>
     * Normally the ACC reads the app client JAR's manifest to get the
     * Main-Class attribute.  The calling program can override that value
     * by invoking this method.  The main class name is also useful if
     * the calling program provides an EAR that contains multiple app clients
     * as submodules within it; the ACC needs the calling program to specify
     * which of the possibly several app client modules is the one to execute.
     *
     * @param mainClassName
     * @return
     */
    public void setClientMainClassName(final String clientMainClassName) throws ClassNotFoundException {
        clientMainClassSetting = ClientMainClassSetting.set(clientMainClassName);
    }

    void setClientMainClass(final Class clientMainClass) {
       clientMainClassSetting = ClientMainClassSetting.set(clientMainClass);
    }

    /**
     * Assigns the URL stream handler factory.
     * <p>
     * Needed for web services support.
     */
    private static void prepareURLStreamHandling() {
        // Set the HTTPS URL stream handler.
        java.security.AccessController.doPrivileged(new
                                       java.security.PrivilegedAction() {
                @Override
                public Object run() {
                    URL.setURLStreamHandlerFactory(new
                                       DirContextURLStreamHandlerFactory());
                    return null;
                }
            });
    }

    void setClassLoader(ACCClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Prescribes the exposed behavior of ACC configuration that can be
     * set up further, and can be used to newContainer an ACC.
     */
    public interface Builder {

        public AppClientContainer newContainer(URI archiveURI) throws Exception, UserError;

        public AppClientContainer newContainer(URI archiveURI,
                CallbackHandler callbackHandler,
                String mainClassName,
                String appName) throws Exception, UserError;

        public AppClientContainer newContainer(URI archiveURI,
                CallbackHandler callbackHandler,
                String mainClassName,
                String appName,
                boolean isTextAuth) throws Exception, UserError;



        public AppClientContainer newContainer(Class mainClass) throws Exception, UserError;

        public TargetServer[] getTargetServers();

        /**
         * Adds an optional {@link MessageSecurityConfig} setting.
         *
         * @param msConfig the new MessageSecurityConfig
         * @return the <code>Builder</code> instance
         */
        public Builder addMessageSecurityConfig(final MessageSecurityConfig msConfig);

        public List<MessageSecurityConfig> getMessageSecurityConfig();

       /**
         * Sets the optional authentication realm for the ACC.
         * <p>
         * Each specific realm will determine which properties should be set in the
         * Properties argument.
         *
         * @param className name of the class which implements the realm
         * @return the <code>Builder</code> instance
         */
        public Builder authRealm(final String className);

        public AuthRealm getAuthRealm();

//        /**
//         * Sets the callback handler the ACC will use when authentication is
//         * required.  If the program does not invoke this method the ACC will use
//         * the callback handler specified in the client's deployment descriptor,
//         * if any.  Failing that, the ACC will use its own default callback handler
//         * to prompt for and collect information required during authentication.
//         * <p>
//         * A callback handler class set using this method overrides the
//         * callback handler setting from the client's descriptor, if any, or from
//         * any previous invocations of <code>callbackHandler</code>.
//         *
//         * @param callbackHandlerClassName fully-qualified name of the developer's
//         * callback handler class
//          * @return the <code>Builder</code> instance
//        */
//        public Builder callbackHandler(final Class<? extends CallbackHandler> callbackHandlerClass);
//
//        public Class<? extends CallbackHandler> getCallbackHandler();

        /**
         * Sets the optional client credentials to be used during authentication to the
         * back-end.
         * <p>
         * If the client does not invoke <code>clientCredentials</code> then the
         * ACC will use a {@link CallbackHandler} when it discovers that authentication
         * is required.  See {@link #callerSuppliedCallbackHandler}.
         *
         * @param username username valid in the default realm on the server
         * @param password password valid in the default realm on the server for the username
         * @return the <code>Builder</code> instance
        */
        public Builder clientCredentials(final String user, final char[] password);

        public ClientCredential getClientCredential();

        /**
         * Sets the optional client credentials and server-side realm to be used during
         * authentication to the back-end.
         * <p>
         * If the client does not invoke <code>clientCredentials</code> then the
         * ACC will use a {@link CallbackHandler} when it discovers that authentication
         * is required.  See {@link #callerSuppliedCallbackHandler}.
         *
         * @param username username valid in the specified realm on the server
         * @param password password valid in the specified realm on the server for the username
         * @param realmName name of the realm on the server within which the credentials are valid
         * @return the <code>Builder</code> instance
         */
        public Builder clientCredentials(final String user, final char[] password, final String realm);

        /**
         * Sets the container-level Properties.
         *
         * @param containerProperties
         * @return
         */
        public Builder containerProperties(final Properties containerProperties);

        /**
         * Sets the container-level properties.
         * <p>
         * Typically used when setting the properties from the parsed XML config
         * file.
         *
         * @param containerProperties Property objects to use in setting the properties
         * @return
         */
        public Builder containerProperties(final List<Property> containerProperties);

        /**
         * Returns the container-level Properties.
         * @return container-level properties
         */
        public Properties getContainerProperties();

        /**
         * Sets the logger which the ACC should use as it runs.
         *
         * @param logger
         * @return
         */
        public Builder logger(final Logger logger);

        public Logger getLogger();

        /**
         * Sets whether the ACC should send the password to the server during
         * authentication.
         *
         * @param sendPassword
         * @return
         */
        public Builder sendPassword(final boolean sendPassword);

        public boolean getSendPassword();

    }


    /**
     * Encapsulates all clean-up activity.
     * <p>
     * The calling program can invoke clean-up by invoking the <code>stop</code>
     * method or by letting the JVM exit, in which case clean-up will occur as
     * part of VM shutdown.
     */
    private static class Cleanup implements Runnable {
        private AppClientInfo appClientInfo = null;
        private boolean cleanedUp = false;
        private InjectionManager injectionMgr = null;
        private ApplicationClientDescriptor appClient = null;
        private Class cls = null;
        private final Logger logger;
        private Thread cleanupThread = null;
        private Collection<EntityManagerFactory> emfs = null;
        private final ServiceLocator habitat;
        private ConnectorRuntime connectorRuntime;
        private ManagedBeanManager managedBeanMgr;

        static Cleanup arrangeForShutdownCleanup(final Logger logger,
                final ServiceLocator habitat, final ApplicationClientDescriptor appDesc) {
            final Cleanup cu = new Cleanup(logger, habitat, appDesc);
            cu.enable();
            return cu;
        }

        private Cleanup(final Logger logger, final ServiceLocator habitat, final ApplicationClientDescriptor appDesc) {
            this.logger = logger;
            this.habitat = habitat;
            this.appClient = appDesc;
        }

        void setAppClientInfo(AppClientInfo info) {
            appClientInfo = info;
        }

        void setInjectionManager(InjectionManager injMgr, Class cls) {
            injectionMgr = injMgr;
            this.cls = cls;
        }

        void setManagedBeanManager(ManagedBeanManager mgr) {
            managedBeanMgr = mgr;
        }

        void setEMFs(Collection<EntityManagerFactory> emfs) {
            this.emfs = emfs;
        }

        void setConnectorRuntime(ConnectorRuntime connectorRuntime) {
            this.connectorRuntime = connectorRuntime;
        }

        void enable() {
            Runtime.getRuntime().addShutdownHook(cleanupThread = new Thread(this, "Cleanup"));
        }

        void disable() {
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction() {

                    @Override
                    public Object run() {
                        Runtime.getRuntime().removeShutdownHook(cleanupThread);
                        return null;
                    }
                }
                    );
        }

        /**
         * Requests cleanup without relying on the VM's shutdown handling.
         */
        void start() {
            disable();
            run();
        }

        /**
         * Performs clean-up of the ACC.
         * <p>
         * This method should be invoked directly only by the VM's shutdown
         * handling (or by the CleanUp newContainer method).  To trigger clean-up
         * without relying on the VM's shutdown handling invoke Cleanup.newContainer()
         * not Cleanup.run().
         */
        @Override
        public void run() {
            logger.fine("Clean-up starting");
            _logger.fine("Clean-up starting");
            /*
             * Do not invoke disable from here.  The run method might execute
             * while the VM shutdown is in progress, and attempting to remove
             * the shutdown hook at that time would trigger an exception.
             */
            cleanUp();
            logger.fine("Clean-up complete");
            _logger.fine("Clean-up complete");
        }

        void cleanUp() {
            if( !cleanedUp ) {

                // Do managed bean cleanup early since it can result in
                // application code (@PreDestroy) invocations
                cleanupManagedBeans();
                cleanupEMFs();
                cleanupInfo();
                cleanupInjection();
                cleanupServiceReferences();
                cleanupTransactions();
                cleanupConnectorRuntime();

                cleanedUp = true;
            } // End if -- cleanup required
        }
        
        private void cleanupEMFs() {
            try {
                if (emfs != null) {
                    for (EntityManagerFactory emf : emfs) {
                        emf.close();
                    }
                    emfs.clear();
                    emfs = null;
                }
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "cleanupEMFs", t);
            }
        }

        private void cleanupInfo() {
            try {
                if ( appClientInfo != null ) {
                    appClientInfo.close();
                }
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "cleanupInfo", t);
            }
        }

        private void cleanupInjection() {
            try {
                if ( injectionMgr != null) {
                    // inject the pre-destroy methods before shutting down
                    injectionMgr.invokeClassPreDestroy(cls, appClient);
                    injectionMgr = null;
                }
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "cleanupInjection", t);
            }

        }

        private void cleanupManagedBeans() {
            try {
                if ( managedBeanMgr != null) {
                    managedBeanMgr.unloadManagedBeans(appClient.getApplication());
                }
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "cleanupManagedBeans", t);
            }

        }

        private void cleanupServiceReferences() {
            try {
                if(appClient != null && appClient.getServiceReferenceDescriptors() != null) {
                    // Cleanup client pipe line, if there were service references
                    for (Object desc: appClient.getServiceReferenceDescriptors()) {
                         ClientPipeCloser.getInstance()
                            .cleanupClientPipe((ServiceReferenceDescriptor)desc);
                    }
                }
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "cleanupServiceReferences", t);
            }
        }

        private void cleanupTransactions() {
            try {
                ServiceHandle<TransactionManager> inhabitant =
                        habitat.getServiceHandle(TransactionManager.class);
                if (inhabitant != null && inhabitant.isActive()) {
                    TransactionManager txmgr = inhabitant.getService();
                    if (txmgr.getStatus() == Status.STATUS_ACTIVE 
                            || txmgr.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                        txmgr.rollback();
                    }
                }
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "cleanupTransactions", t);
            }

        }

        private void cleanupConnectorRuntime() {
            try {
                if (connectorRuntime != null) {
                    connectorRuntime.cleanUpResourcesAndShutdownAllActiveRAs();
                    connectorRuntime = null;
                }
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "cleanupConnectorRuntime", t);
            }
        }
    }
}
