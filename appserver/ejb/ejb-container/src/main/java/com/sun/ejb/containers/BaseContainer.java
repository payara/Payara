/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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
// Portions Copyright [2016-2025] [Payara Foundation and/or its affiliates]

package com.sun.ejb.containers;

import com.sun.ejb.ComponentContext;
import com.sun.ejb.Container;
import com.sun.ejb.EJBUtils;
import com.sun.ejb.EjbInvocation;
import com.sun.ejb.EjbInvocationFactory;
import com.sun.ejb.InvocationInfo;
import com.sun.ejb.MethodLockInfo;
import com.sun.ejb.codegen.EjbOptionalIntfGenerator;
import com.sun.ejb.containers.interceptors.InterceptorManager;
import com.sun.ejb.containers.interceptors.SystemInterceptorProxy;
import com.sun.ejb.containers.util.MethodMap;
import com.sun.ejb.monitoring.probes.EjbCacheProbeProvider;
import com.sun.ejb.monitoring.probes.EjbMonitoringProbeProvider;
import com.sun.ejb.monitoring.probes.EjbTimedObjectProbeProvider;
import com.sun.ejb.monitoring.stats.EjbCacheStatsProvider;
import com.sun.ejb.monitoring.stats.EjbMonitoringStatsProvider;
import com.sun.ejb.monitoring.stats.EjbMonitoringUtils;
import com.sun.ejb.monitoring.stats.EjbPoolStatsProvider;
import com.sun.ejb.monitoring.stats.EjbThreadPoolExecutorStatsProvider;
import com.sun.ejb.monitoring.stats.EjbTimedObjectStatsProvider;
import com.sun.ejb.portable.EJBMetaDataImpl;
import com.sun.ejb.spi.container.OptionalLocalInterfaceProvider;
import com.sun.enterprise.admin.monitor.callflow.CallFlowInfo;
import com.sun.enterprise.admin.monitor.callflow.ComponentType;
import com.sun.enterprise.container.common.spi.JCDIService;
import com.sun.enterprise.container.common.spi.JavaEEContainer;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.IndirectlySerializable;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.InterceptorDescriptor;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.WebServicesDescriptor;
import com.sun.enterprise.deployment.util.TypeUtil;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import com.sun.enterprise.loader.ASURLClassLoader;
import com.sun.enterprise.security.SecurityManager;
import com.sun.enterprise.transaction.api.JavaEETransaction;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.Utility;
import fish.payara.cluster.DistributedLockType;
import fish.payara.notification.requesttracing.RequestTraceSpanLog;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.opentracing.OpenTracingService;
import io.opentracing.Span;
import io.opentracing.Tracer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.AccessLocalException;
import jakarta.ejb.CreateException;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.EJBContext;
import jakarta.ejb.EJBException;
import jakarta.ejb.EJBHome;
import jakarta.ejb.EJBLocalHome;
import jakarta.ejb.EJBLocalObject;
import jakarta.ejb.EJBMetaData;
import jakarta.ejb.EJBObject;
import jakarta.ejb.EJBTransactionRequiredException;
import jakarta.ejb.EJBTransactionRolledbackException;
import jakarta.ejb.FinderException;
import jakarta.ejb.LockType;
import jakarta.ejb.NoSuchEJBException;
import jakarta.ejb.NoSuchObjectLocalException;
import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.RemoveException;
import jakarta.ejb.TransactionRequiredLocalException;
import jakarta.ejb.TransactionRolledbackLocalException;
import jakarta.enterprise.inject.Vetoed;
import jakarta.interceptor.AroundConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.UserTransaction;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.rmi.AccessException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.ejb.LogFacade;
import org.glassfish.ejb.api.EjbEndpointFacade;
import org.glassfish.ejb.deployment.descriptor.EjbApplicationExceptionInfo;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbInitInfo;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.ejb.deployment.descriptor.ScheduledTimerDescriptor;
import org.glassfish.ejb.spi.EjbContainerInterceptor;
import org.glassfish.ejb.spi.WSEjbEndpointRegistry;
import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;
import org.glassfish.enterprise.iiop.api.ProtocolManager;
import org.glassfish.enterprise.iiop.api.RemoteReferenceFactory;
import org.glassfish.enterprise.iiop.spi.EjbContainerFacade;
import org.glassfish.flashlight.provider.ProbeProviderFactory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.logging.annotation.LogMessageInfo;

import static com.sun.enterprise.deployment.MethodDescriptor.EJB_WEB_SERVICE;
import static java.util.logging.Level.FINE;
import static java.util.stream.Collectors.toList;

/**
 * This class implements part of the com.sun.ejb.Container interface.
 * It implements the container's side of the EJB-to-Container
 * contract defined by the EJB 2.0 spec.
 * It contains code shared by SessionBeans, EntityBeans and MessageDrivenBeans.
 * Its subclasses provide the remaining implementation of the
 * container functionality.
 *
 */
public abstract class BaseContainer implements Container, EjbContainerFacade, JavaEEContainer {

    public enum ContainerType {
        STATELESS, STATEFUL, SINGLETON, MESSAGE_DRIVEN, ENTITY, READ_ONLY
    }

    protected static final Logger _logger = LogFacade.getLogger();

    @LogMessageInfo(
            message = "The feature {0} requires Full Java EE Profile to be supported",
            level = "WARNING")
    private static final String WARN_FEATURE_REQUIRES_FULL_PROFILE = "AS-EJB-00053";

    @LogMessageInfo(
            message = "Portable JNDI names for EJB {0}: {1}",
            level = "INFO")
    private static final String PORTABLE_JNDI_NAMES = "AS-EJB-00054";

    @LogMessageInfo(
            message = "Glassfish-specific (Non-portable) JNDI names for EJB {0}: {1}",
            level = "INFO")
    private static final String GLASSFISH_SPECIFIC_JNDI_NAMES = "AS-EJB-00055";

    @LogMessageInfo(
            message = "A system exception occurred during an invocation on EJB {0}, method: {1}",
            level = "WARNING")
    private static final String SYSTEM_EXCEPTION = "AS-EJB-00056";

    @LogMessageInfo(
            message = "Error while creating enterprise bean context for {0} during jacc callback",
            level = "WARNING")
    private static final String CONTEXT_FAILURE_JACC = "AS-EJB-00057";

    @LogMessageInfo(
            message = "Attempt to override reserved ejb interface method [{0}] in [{1}]. Override will be ignored.",
            level = "WARNING")
    private static final String ILLEGAL_EJB_INTERFACE_OVERRIDE = "AS-EJB-00058";

    @LogMessageInfo(
            message = "Bean class for ejb [{0}] does not define a method corresponding to [{1}] interface method [{2}]",
            level = "WARNING")
    private static final String BEAN_CLASS_METHOD_NOT_FOUND = "AS-EJB-00059";

    @LogMessageInfo(
            message = "keepstate is true and will not create new auto timers during deployment.",
            level = "INFO")
    private static final String KEEPSTATE_IS_TRUE = "AS-EJB-00060";

    @LogMessageInfo(
            message = "Failed to initialize the interceptor",
            level = "SEVERE",
            cause = "Error during initializing the interceptor",
            action = "Try to restart the server")
    private static final String FAILED_TO_INITIALIZE_INTERCEPTOR = "AS-EJB-00061";

    @LogMessageInfo(
            message = "[**BaseContainer**] Could not create MonitorRegistryMediator. [{0}]",
            level = "SEVERE",
            cause = "Fail to create MonitorRegistryMediator",
            action = "Check the exception stack")
    private static final String COULD_NOT_CREATE_MONITORREGISTRYMEDIATOR = "AS-EJB-00062";

    @LogMessageInfo(
            message = "Internal Error",
            level = "WARNING",
            cause = "Error during invoke the ejb application",
            action = "Trying to invoke the ejb application"
    )
    private static final String INTERNAL_ERROR = "AS-EJB-00052";

    protected static final Class[] NO_PARAMS = new Class[]{};

    protected Object[] logParams = null;

    protected ContainerType containerType;

    private final RequestTracingService requestTracingService;
    private final OpenTracingService openTracingService;

    // constants for EJB(Local)Home/EJB(Local)Object methods,
    // used in authorizeRemoteMethod and authorizeLocalMethod
    private static final int EJB_INTF_METHODS_LENGTH = 16;
    static final int EJBHome_remove_Handle = 0;
    static final int EJBHome_remove_Pkey = 1;
    static final int EJBHome_getEJBMetaData = 2;
    static final int EJBHome_getHomeHandle = 3;
    static final int EJBLocalHome_remove_Pkey = 4;
    static final int EJBObject_getEJBHome = 5;
    protected static final int EJBObject_getPrimaryKey = 6; //TODO - move related to entity-container
    static final int EJBObject_remove = 7;
    static final int EJBObject_getHandle = 8;
    static final int EJBObject_isIdentical = 9;
    static final int EJBLocalObject_getEJBLocalHome = 10;
    protected static final int EJBLocalObject_getPrimaryKey = 11; //TODO - move related to entity-container
    static final int EJBLocalObject_remove = 12;
    static final int EJBLocalObject_isIdentical = 13;
    static final int EJBHome_create = 14;
    static final int EJBLocalHome_create = 15;

    // true if home method, false if component intf method.
    // Used for setting info on invocation object during authorization.
    private static final boolean[] EJB_INTF_METHODS_INFO =
            {true, true, true, true, true,
                    false, false, false, false, false,
                    false, false, false, false,
                    true, true};

    private static final byte HOME_KEY = (byte) 0xff;
    private static final byte[] homeInstanceKey = {HOME_KEY};

    protected static final String SINGLETON_BEAN_POOL_PROP = "singleton-bean-pool";

    protected final ClassLoader loader;
    protected Class<?> ejbClass;
    protected Class<?> sfsbSerializedClass;
    protected Method ejbPassivateMethod;
    protected Method ejbActivateMethod;
    protected Method ejbRemoveMethod;
    private Method ejbTimeoutMethod;

    protected Class<?> webServiceEndpointIntf;

    // true if exposed as a web service endpoint.
    protected boolean isWebServiceEndpoint;

    private boolean isTimedObject_;
    private boolean hasPersistenceTimer;
    private boolean hasNonPersistenceTimer;

    /*****************************************
     *    Data members for Local views       *
     *****************************************/

    // True if bean has a LocalHome/Local view
    // OR a Local business view OR both.
    protected boolean isLocal;

    // True if bean exposes a local home view
    protected boolean hasLocalHomeView;

    // True if bean exposes a local business view
    protected boolean hasLocalBusinessView;

    protected boolean hasOptionalLocalBusinessView;

    protected Class ejbGeneratedOptionalLocalBusinessIntfClass;
    //
    // Data members for LocalHome/Local view
    //

    // LocalHome interface written by developer
    protected Class localHomeIntf = null;

    // Local interface written by developer
    private Class localIntf = null;

    // Client reference to ejb local home
    protected EJBLocalHome ejbLocalHome;

    // Implementation of ejb local home.  May or may not be the same
    // object as ejbLocalHome, for example in the case of dynamic proxies.
    protected EJBLocalHomeImpl ejbLocalHomeImpl;
    
    //
    // Data members for 3.x Local business view
    //

    // Internal interface describing operation used to create an
    // instance of a local business object. (GenericEJBLocalHome)
    protected Class localBusinessHomeIntf = null;
    protected Class ejbOptionalLocalBusinessHomeIntf = null;

    // Local business interface written by developer
    protected Set<Class> localBusinessIntfs = new HashSet();

    // Client reference to internal local business home interface.
    // This is only seen by internal ejb code that instantiates local
    // business objects during lookups.
    protected GenericEJBLocalHome ejbLocalBusinessHome;

    protected GenericEJBLocalHome ejbOptionalLocalBusinessHome;

    // Implementation of internal local business home interface.
    protected EJBLocalHomeImpl ejbLocalBusinessHomeImpl;

    // Implementation of internal local business home interface.
    protected EJBLocalHomeImpl ejbOptionalLocalBusinessHomeImpl;

    private Collection<EjbContainerInterceptor> interceptors = null;

    /*****************************************
     *     Data members for Remote views     *
     *****************************************/

    // True if bean has a RemoteHome/Remote view
    // OR a Remote business view OR both.
    protected boolean isRemote = false;

    // True if bean exposes a RemoteHome view
    protected boolean hasRemoteHomeView = false;

    // True if bean exposes a Remote Business view.
    protected boolean hasRemoteBusinessView = false;

    //
    // Data members for RemoteHome/Remote view
    //

    // Home interface written by developer.
    protected Class homeIntf = null;

    // Remote interface written by developer.
    protected Class remoteIntf = null;

    // Container implementation of EJB Home. May or may not be the same
    // object as ejbHome, for example in the case of dynamic proxies.
    protected EJBHomeImpl ejbHomeImpl;

    // EJB Home reference used by ORB Tie within server to deliver
    // invocation.
    protected EJBHome ejbHome;

    // Client reference to EJB Home.
    protected EJBHome ejbHomeStub;

    // Remote interface proxy class
    private Class ejbObjectProxyClass;

    // RemoteReference Factory for RemoteHome view
    protected RemoteReferenceFactory remoteHomeRefFactory = null;

    //
    // Data members for 3.x Remote business view
    //

    // Internal interface describing operation used to create an
    // instance of a remote business object.
    protected Class remoteBusinessHomeIntf;

    // Container implementation of internal EJB Business Home. May or may
    // not be same object as ejbRemoteBusinessHome, for example in the
    // case of dynamic proxies.
    protected EJBHomeImpl ejbRemoteBusinessHomeImpl;

    // EJB Remote Business Home reference used by ORB Tie within server
    // to deliver invocation.
    protected EJBHome ejbRemoteBusinessHome;

    // Client reference to internal Remote EJB Business Home.  This is
    // only seen by internal EJB code that instantiates remote business
    // objects during lookups.
    protected EJBHome ejbRemoteBusinessHomeStub;


    // Holds information such as remote reference factory that are associated
    // with a particular remote business interface
    protected final Map<String, RemoteBusinessIntfInfo> remoteBusinessIntfInfo = new HashMap<>();

    //
    // END -- Data members for Remote views
    //

    protected EJBMetaData metadata = null;

    protected final SecurityManager securityManager;

    protected boolean isSession;
    protected boolean isStatelessSession;
    protected boolean isStatefulSession;
    protected boolean isMessageDriven;
    protected boolean isSingleton;

    protected EjbDescriptor ejbDescriptor;
    protected String componentId; // unique id for java:comp namespace lookup

    protected final Map<Method, InvocationInfo> invocationInfoMap = new HashMap<>();

    protected final Map<TimerPrimaryKey, Method> scheduleIds = new HashMap<>();

    private final Map<Method, List<ScheduledTimerDescriptor>> schedules = new HashMap<>();

    // Need a separate map for web service methods since it's possible for
    // an EJB Remote interface to be a subtype of the Service Endpoint
    // Interface.  In that case, it's ambiguous to do a lookup based only
    // on a java.lang.reflect.Method
    protected final Map<Method, InvocationInfo> webServiceInvocationInfoMap = new HashMap<>();

    // optimized method map for proxies to resolve invocation info
    private MethodMap proxyInvocationInfoMap;

    protected Method[] ejbIntfMethods;
    protected InvocationInfo[] ejbIntfMethodInfo;

    protected Properties envProps;
    protected boolean isBeanManagedTran = false;


    protected boolean debugMonitorFlag = false;

    private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(BaseContainer.class);

    private final ThreadLocal threadLocalContext = new ThreadLocal();

    protected static final int CONTAINER_INITIALIZING = -1;
    protected static final int CONTAINER_STARTED = 0;
    protected static final int CONTAINER_STOPPED = 1;
    protected static final int CONTAINER_UNDEPLOYED = 3;
    protected static final int CONTAINER_ON_HOLD = 4;

    protected volatile int containerState = CONTAINER_INITIALIZING;

    protected EjbMonitoringStatsProvider ejbProbeListener;
    protected EjbMonitoringProbeProvider ejbProbeNotifier;
    protected EjbTimedObjectStatsProvider timerProbeListener;
    protected EjbTimedObjectProbeProvider timerProbeNotifier;
    private boolean timersStarted = false;
    protected EjbPoolStatsProvider poolProbeListener;
    protected EjbCacheProbeProvider cacheProbeNotifier;
    protected EjbCacheStatsProvider cacheProbeListener;
    protected EjbThreadPoolExecutorStatsProvider executorProbeListener;

    protected ContainerInfo containerInfo;

    private final String _debugDescription;

    //protected Agent callFlowAgent;

    protected CallFlowInfo callFlowInfo;

    protected InterceptorManager interceptorManager;

    private final Set<Object> pendingInterceptors = new HashSet<>();

    // the order must be the same as CallbackType and getPre30LifecycleMethodNames
    private static final Class[] lifecycleCallbackAnnotationClasses = {
            AroundConstruct.class,
            PostConstruct.class, PreDestroy.class,
            PrePassivate.class, PostActivate.class
    };

    private final Set<Class> monitoredGeneratedClasses = new HashSet<>();

    protected InvocationManager invocationManager;

    protected InjectionManager injectionManager;

    protected GlassfishNamingManager namingManager;

    protected JavaEETransactionManager transactionManager;

    private EjbInvocationFactory invFactory;

    private ProtocolManager protocolMgr;

    protected EjbContainerUtil ejbContainerUtilImpl = EjbContainerUtilImpl.getInstance();

    protected EjbOptionalIntfGenerator optIntfClassLoader;

    private final Set<String> publishedPortableGlobalJndiNames = new HashSet<>();

    private final Set<String> publishedNonPortableGlobalJndiNames = new HashSet<>();

    private final Set<String> publishedInternalGlobalJndiNames = new HashSet<>();


    private final Map<String, JndiInfo> jndiInfoMap = new HashMap<>();

    private String optIntfClassName;

    // Used to track whether we've done the base container cleanup (JNDI entries, etc.)
    // Only.  Not applicable to concrete containers.
    private boolean baseContainerCleanupDone = false;

    // True if there is at least one asynchronous method exposed from the bean.
    private boolean hasAsynchronousInvocations = false;

    // Information about a web service ejb endpoint.  Used as a conduit
    // between webservice runtime and ejb container.  Contains a Remote
    // servant used by jaxrpc to call web service business method.
    private WebServiceEndpoint webServiceEndpoint;

    //The Webservices Ejb Endpoint Registry contract
    // used to register and unregister ejb webservices endpoints
    private WSEjbEndpointRegistry wsejbEndpointRegistry;

    protected EJBContainerStateManager containerStateManager;
    protected EJBContainerTransactionManager containerTransactionManager;

    protected JCDIService jcdiService;

    private Class[] ejbLocalBusinessProxyInterfaces;

    private Class[] ejbOptionalLocalBusinessHomeProxyInterfaces;

    /**
     * This constructor is called from ContainerFactoryImpl when an
     * EJB Jar is deployed.
     */
    protected BaseContainer(final ContainerType type, final EjbDescriptor ejbDesc, final ClassLoader loader,
                            final SecurityManager sm) throws Exception {
        this.containerType = type;
        this.securityManager = sm;
        this.requestTracingService = Globals.getDefaultHabitat().getService(RequestTracingService.class);
        this.openTracingService = Globals.getDefaultHabitat().getService(OpenTracingService.class);

        try {
            this.loader = loader;
            this.ejbDescriptor = ejbDesc;
            //this.callFlowAgent = ejbContainerUtilImpl.getCallFlowAgent();

            logParams = new Object[1];
            logParams[0] = ejbDesc.getName();
            invocationManager = ejbContainerUtilImpl.getInvocationManager();
            injectionManager = ejbContainerUtilImpl.getInjectionManager();
            namingManager = ejbContainerUtilImpl.getGlassfishNamingManager();
            transactionManager = ejbContainerUtilImpl.getTransactionManager();

            // URLClassLoader now uses DirWatcher because negative lookups are expensive (on Windows).
            // However, DirWatcher, since it relies on OS events, may not actually be up to date
            // and may not know that classes already exist (race condition)
            // here we tell URLClassLoader that the class must exist in this loader, and
            // ignore DirWatcher for this loader. Internally, this is stored in a boolean variable
            // and the Auto-Closeable will clear out that value
            try (AutoCloseable closeable = ASURLClassLoader.mustLoadFrom(loader)) {
                // get Class objects for creating new EJBs
                ejbClass = loader.loadClass(ejbDescriptor.getEjbImplClassName());
            }

            containerStateManager = new EJBContainerStateManager(this);
            containerTransactionManager = new EJBContainerTransactionManager(this, ejbDesc);

            isBeanManagedTran = ejbDescriptor.getTransactionType().equals("Bean");

            if (ejbDescriptor instanceof EjbSessionDescriptor) {
                isSession = true;
                EjbSessionDescriptor sd = (EjbSessionDescriptor) ejbDescriptor;

                if (!sd.isSessionTypeSet()) {
                    throw new RuntimeException(localStrings.getLocalString(
                            "ejb.session_type_not_set",
                            "Invalid ejb Descriptor. Session type not set for {0}: {1}",
                            sd.getName(), sd));
                }

                if (sd.isSingleton()) {
                    isSingleton = true;
                } else {
                    isStatelessSession = sd.isStateless();
                    isStatefulSession = !isStatelessSession;

                    if (isStatefulSession) {

                        /**
                         * If bean class isn't explicitly marked Serializable, generate
                         * a subclass that is.   We do this with a generator that uses
                         * ASM directly instead of the CORBA codegen library since none
                         * of the corba .jars are part of the Web Profile.
                         */
                        if (!Serializable.class.isAssignableFrom(ejbClass)) {

                            sfsbSerializedClass = EJBUtils.loadGeneratedSerializableClass(loader, ejbClass);
                        }

                    }
                }

                hasAsynchronousInvocations = sd.hasAsynchronousMethods();
            }

            if (ejbDescriptor.isRemoteInterfacesSupported() ||
                    ejbDescriptor.isRemoteBusinessInterfacesSupported()) {

                assertFullProfile("exposes a Remote client view");

                initializeProtocolManager();

            }

            if (ejbDescriptor.isRemoteInterfacesSupported()) {

                isRemote = true;
                hasRemoteHomeView = true;

                String homeClassName = ejbDescriptor.getHomeClassName();

                homeIntf = loader.loadClass(homeClassName);
                remoteIntf = loader.loadClass(ejbDescriptor.getRemoteClassName());

                String id = Long.toString(ejbDescriptor.getUniqueId()) + "_RHome";

                remoteHomeRefFactory = getProtocolManager().getRemoteReferenceFactory(this, true, id);

            }

            if (ejbDescriptor.isRemoteBusinessInterfacesSupported()) {
                isRemote = true;
                hasRemoteBusinessView = true;

                remoteBusinessHomeIntf = EJBUtils.loadGeneratedGenericEJBHomeClass(loader, ejbClass);

                for (String next : ejbDescriptor.getRemoteBusinessClassNames()) {
                    Class<?> genRemoteIntf = EJBUtils.loadGeneratedRemoteBusinessClasses(loader, next);

                    RemoteBusinessIntfInfo info = new RemoteBusinessIntfInfo();
                    info.generatedRemoteIntf = genRemoteIntf;
                    info.remoteBusinessIntf = loader.loadClass(next);

                    // One remote reference factory for each remote
                    // business interface.  Id must be unique across
                    // all ejb containers.
                    String id = Long.toString(ejbDescriptor.getUniqueId()) + "_RBusiness" + "_" + genRemoteIntf.getName();

                    info.referenceFactory = getProtocolManager().
                            getRemoteReferenceFactory(this, false, id);

                    remoteBusinessIntfInfo.put(genRemoteIntf.getName(), info);

                    addToGeneratedMonitoredMethodInfo(genRemoteIntf);
                }

            }

            if (ejbDescriptor.isLocalInterfacesSupported()) {
                // initialize class objects for LocalHome/LocalIntf etc.
                isLocal = true;
                hasLocalHomeView = true;

                String localHomeClassName = ejbDescriptor.getLocalHomeClassName();

                localHomeIntf = loader.loadClass(localHomeClassName);
                localIntf = loader.loadClass(ejbDescriptor.getLocalClassName());
            }

            if (ejbDescriptor.isLocalBusinessInterfacesSupported()) {
                isLocal = true;
                hasLocalBusinessView = true;

                localBusinessHomeIntf = GenericEJBLocalHome.class;

                for (String next : ejbDescriptor.getLocalBusinessClassNames()) {
                    Class clz = loader.loadClass(next);
                    localBusinessIntfs.add(clz);
                    addToGeneratedMonitoredMethodInfo(clz);
                }
            }

            if (ejbDescriptor.isLocalBean()) {
                isLocal = true;
                hasOptionalLocalBusinessView = true;

                ejbOptionalLocalBusinessHomeIntf = GenericEJBLocalHome.class;
                Class clz = loader.loadClass(ejbDescriptor.getEjbClassName());
                addToGeneratedMonitoredMethodInfo(clz);

                this.optIntfClassName = EJBUtils.getGeneratedOptionalInterfaceName(ejbClass.getName());
                optIntfClassLoader = new EjbOptionalIntfGenerator(loader);
                optIntfClassLoader.generateOptionalLocalInterface(ejbClass, optIntfClassName);
                ejbGeneratedOptionalLocalBusinessIntfClass = optIntfClassLoader.loadClass(optIntfClassName);
            }

            if (isStatelessSession || isSingleton) {
                EjbBundleDescriptorImpl bundle = ejbDescriptor.getEjbBundleDescriptor();
                WebServicesDescriptor webServices = bundle.getWebServices();
                Collection endpoints = webServices.getEndpointsImplementedBy(ejbDescriptor);
                // JSR 109 doesn't require support for a single ejb
                // implementing multiple port ex.
                if (endpoints.size() == 1) {

                    assertFullProfile("is a Web Service Endpoint");

                    webServiceEndpointIntf = loader.loadClass
                            (ejbDescriptor.getWebServiceEndpointInterfaceName());
                    isWebServiceEndpoint = true;
                }
            }


            try {
                // get Method objects for ejbPassivate/Activate/ejbRemove
                ejbPassivateMethod = ejbClass.getMethod("ejbPassivate", NO_PARAMS);
                ejbActivateMethod = ejbClass.getMethod("ejbActivate", NO_PARAMS);
                ejbRemoveMethod = ejbClass.getMethod("ejbRemove", NO_PARAMS);
            } catch (NoSuchMethodException nsme) {
                // ignore.  Will happen for EJB 3.0 session beans
            }

            hasPersistenceTimer = false;
            hasNonPersistenceTimer = false;
            if (ejbDescriptor.isTimedObject()) {

                warnIfNotFullProfile("use of persistent EJB Timer Service");

                MethodDescriptor ejbTimeoutMethodDesc =
                        ejbDescriptor.getEjbTimeoutMethod();
                // Can be a @Timeout or @Schedule or TimedObject
                if (ejbTimeoutMethodDesc != null) {
                    Method method = ejbTimeoutMethodDesc.getMethod(ejbDescriptor);
                    // timers defined in runtime
                    hasPersistenceTimer = true;
                    hasNonPersistenceTimer = true;
                    processEjbTimeoutMethod(method);

                    ejbTimeoutMethod = method;
                }


                for (ScheduledTimerDescriptor schd : ejbDescriptor.getScheduledTimerDescriptors()) {
                    Method method = schd.getTimeoutMethod().getMethod(ejbDescriptor);
                    if (method == null) {
                        // This should've been caught in EjbBundleValidator
                        throw new EJBException(localStrings.getLocalString(
                                "ejb.no_timeout_method",
                                "Class {0} does not define timeout method {1}",
                                ejbClass.getName(), schd.getTimeoutMethod().getFormattedString()));
                    }

                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "... processing {0}", method);
                    }
                    if (schd.getPersistent()) {
                        hasPersistenceTimer = true;
                    } else {
                        hasNonPersistenceTimer = true;
                    }
                    processEjbTimeoutMethod(method);

                    List<ScheduledTimerDescriptor> list = schedules.get(method);
                    if (list == null) {
                        list = new ArrayList<>();
                        schedules.put(method, list);
                    }
                    list.add(schd);
                }

            }

            preInitialize(ejbDesc, loader);

            initializeEjbInterfaceMethods();

            if (needSystemInterceptorProxy()) {
                addSystemInterceptorProxy();
            }

            // NOTE : InterceptorManager initialization delayed until transition to START state.

            addLocalRemoteInvocationInfo();
            addWSOrTimedObjectInvocationInfo();

            initializeInvocationInfo();

            setupEnvironment();

            ServiceLocator services = ejbContainerUtilImpl.getServices();

            jcdiService = services.getService(JCDIService.class);

            initEjbInterceptors();
        } catch (Exception ex) {
            _logger.log(Level.FINE, "Exception creating BaseContainer : [{0}]", logParams);
            _logger.log(Level.FINE, "", ex);
            throw ex;
        }

        _debugDescription = "ejbName: " + ejbDescriptor.getName()
                + "; containerId: " + ejbDescriptor.getUniqueId();
        _logger.log(Level.FINE, "Instantiated container for: "
                + _debugDescription);
    }

    @Override
    public void initialize() {
        if (isTimedObject_) {
            if (!isStatefulSession) {
                // EJBTimerService should be accessed only if needed
                // not to cause it to be loaded if it's not used.
                if (hasPersistenceTimer) {
                    EJBTimerService timerService = EJBTimerService.getEJBTimerService(null, true, true);
                    if (timerService != null) {
                        timerService.timedObjectCount();
                        timersStarted = true;
                    }
                }
                if (hasNonPersistenceTimer) {
                    EJBTimerService timerService = EJBTimerService.getEJBTimerService(null, true, false);
                    if (timerService != null) {
                        timerService.timedObjectCount();
                        timersStarted = true;
                    }
                }
            } else {
                isTimedObject_ = false;
                throw new EJBException(localStrings.getLocalString(
                        "ejb.stateful_cannot_be_timed_object",
                        "EJB {0} is invalid. Stateful session ejbs cannot be Timed Objects",
                        ejbDescriptor.getName()));
            }
        }
    }

    protected ProtocolManager getProtocolManager() {
        return protocolMgr;
    }

    public ContainerType getContainerType() {
        return containerType;
    }


    protected void doEJBHomeRemove(Object pk, Method m, boolean isLocal)
            throws RemoteException, RemoveException {
        throw new UnsupportedOperationException(localStrings.getLocalString(
                "ejb.ejbhome_remove_on_nonentity",
                "EJBHome.remove() called on non entity container"));
    }

    private void addToGeneratedMonitoredMethodInfo(Class generatedClass) {
        monitoredGeneratedClasses.add(generatedClass);
    }

    protected void initializeProtocolManager() {

        try {

            GlassFishORBHelper orbHelper = ejbContainerUtilImpl.getORBHelper();
            protocolMgr = orbHelper.getProtocolManager();

        } catch (Throwable t) {
            throw new RuntimeException("IIOP Protocol Manager initialization failed.  " +
                    "Possible cause is that ORB is not available in this " +
                    ((ejbContainerUtilImpl.isEmbeddedServer()) ?
                            "embedded container, or server instance is running and required ports are in use" :
                            "container")
                    , t);
        }

    }

    protected void preInitialize(EjbDescriptor ejbDesc, ClassLoader loader) {
        //Overridden in sub classes
    }

    public void checkUserTransactionLookup(ComponentInvocation inv)
            throws javax.naming.NameNotFoundException {
        if (!this.isBeanManagedTran) {
            throw new javax.naming.NameNotFoundException(
                    localStrings.getLocalString("ejb.ut_lookup_not_allowed",
                            "Lookup of java:comp/UserTransaction not allowed for Container managed Transaction beans"));
        }
    }

    protected final void createCallFlowAgent(ComponentType compType) {

        this.callFlowInfo = new CallFlowInfoImpl(
                this, ejbDescriptor, compType);
    }

    @Override
    public String toString() {
        return _debugDescription;
    }

    @Override
    public final void setStartedState() {

        if (containerState == CONTAINER_STARTED) {
            return;
        }

        // NOTE : we used to initialize interceptor manager in the ctor but we need to delay
        // the initialization to account for the possiblity of a 299-enabled app.  In
        // that case, the 299-defined ejb interceptors are not added until the
        // deployment load() phase.   That's ok, as long as everything is initialized
        // before any bean instances are created or any ejb invocations take place.
        // Therefore, moving the initialization to the point that we transition into the
        // ejb container START state.

        try {
            initializeInterceptorManager();

            for (InvocationInfo info : invocationInfoMap.values()) {
                setInterceptorChain(info);
            }
            for (InvocationInfo info : this.webServiceInvocationInfoMap.values()) {
                setInterceptorChain(info);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        containerState = CONTAINER_STARTED;
    }

    private void setInterceptorChain(InvocationInfo info) {
        if (info.aroundMethod != null) {
            if (info.isEjbTimeout) {
                MethodDescriptor md = new MethodDescriptor(info.aroundMethod, MethodDescriptor.TIMER_METHOD);
                info.interceptorChain =
                        interceptorManager.getAroundTimeoutChain(md, info.aroundMethod);
            } else {
                MethodDescriptor md = new MethodDescriptor(info.aroundMethod, MethodDescriptor.EJB_BEAN);
                info.interceptorChain =
                        interceptorManager.getAroundInvokeChain(md, info.aroundMethod);
            }
        }
    }

    @Override
    public final void setStoppedState() {
        containerState = CONTAINER_STOPPED;
    }

    public final boolean isStopped() {
        return containerState == CONTAINER_STOPPED;
    }

    @Override
    public final void setUndeployedState() {
        containerState = CONTAINER_UNDEPLOYED;
    }

    public final boolean isUndeployed() {
        return (containerState == CONTAINER_UNDEPLOYED);
    }

    @Override
    public final boolean isTimedObject() {
        return isTimedObject_;
    }

    @Override
    public final boolean isLocalObject() {
        return isLocal;
    }

    @Override
    public final boolean isRemoteObject() {
        return isRemote;
    }

    @Override
    public final ClassLoader getContainerClassLoader() {
        return loader;
    }

    @Override
    public final ClassLoader getClassLoader() {
        return loader;
    }

    @Override
    public final String getUseThreadPoolId() {
        return ejbDescriptor.getIASEjbExtraDescriptors().getUseThreadPoolId();
    }

    @Override
    public final boolean getPassByReference() {
        return ejbDescriptor.getIASEjbExtraDescriptors().getPassByReference();
    }

    protected final long getContainerId() {
        return ejbDescriptor.getUniqueId();
    }

    public final long getApplicationId() {
        return ejbDescriptor.getApplication().getUniqueId();
    }

    @Override
    public final EjbDescriptor getEjbDescriptor() {
        return ejbDescriptor;
    }

    /**
     * Method defined on JavaEEContainer
     */
    @Override
    public final Descriptor getDescriptor() {
        return getEjbDescriptor();
    }

    @Override
    public final EJBMetaData getEJBMetaData() {
        return metadata;
    }

    public final UserTransaction getUserTransaction() {
        return containerTransactionManager.getUserTransaction();
    }

    public boolean isHAEnabled() {
        return false;
    }

    /**
     * EJB spec makes a distinction between access to the UserTransaction
     * object itself and access to its methods.  getUserTransaction covers
     * the first check and this method covers the second.  It is called
     * by the UserTransaction implementation to verify access.
     */
    @Override
    public boolean userTransactionMethodsAllowed(ComponentInvocation inv) {
        // Overridden by containers that allowed BMT;
        return false;
    }

    public final EJBHome getEJBHomeStub() {
        return ejbHomeStub;
    }

    @Override
    public final EJBHome getEJBHome() {
        return ejbHome;
    }

    /**
     * Return an object that implements ejb's local home interface.
     * If dynamic proxies are being used, this is the proxy itself,
     * it can't be directly cast to an EJBLocalHomeImpl.
     */
    public final EJBLocalHome getEJBLocalHome() {
        return ejbLocalHome;
    }

    /**
     * Return an object that implements ejb's local business home interface.
     */
    public final GenericEJBLocalHome getEJBLocalBusinessHome(String clientViewClassName) {

        return isLocalBeanClass(clientViewClassName)
                ? ejbOptionalLocalBusinessHome
                : ejbLocalBusinessHome;
    }

    boolean isLocalBeanClass(String className) {

        return hasOptionalLocalBusinessView &&
                (className.equals(ejbClass.getName()) ||
                        className.equals(ejbGeneratedOptionalLocalBusinessIntfClass.getName()));
    }

    public final Class getEJBClass() {
        return ejbClass;
    }

    @Override
    public final SecurityManager getSecurityManager() {
        return securityManager;
    }

    final Properties getEnvironmentProperties() {
        return envProps;
    }

    /**
     * Create an EJBObject reference from the instanceKey
     * Called from EJBObjectOutputStream.SerializableRemoteRef
     * during deserialization of a remote-ref
     *
     * @param instanceKey                 instanceKey of the ejbobject
     * @param generatedRemoteBusinessIntf non-null, this is a remote business view and the param
     *                                    is the name of the generated remote business interface.
     *                                    Otherwise, this is for the RemoteHome view
     */
    public java.rmi.Remote createRemoteReferenceWithId
    (byte[] instanceKey, String generatedRemoteBusinessIntf) {

        final Thread currentThread = Thread.currentThread();
        final ClassLoader previousClassLoader =
                currentThread.getContextClassLoader();
        final ClassLoader myClassLoader = loader;
        try {
            currentThread.setContextClassLoader(myClassLoader);
            java.rmi.Remote remoteRef;
            if (generatedRemoteBusinessIntf == null) {
                remoteRef = remoteHomeRefFactory.createRemoteReference
                        (instanceKey);
            } else {
                RemoteReferenceFactory remoteBusinessRefFactory =
                        remoteBusinessIntfInfo.get(generatedRemoteBusinessIntf).
                                referenceFactory;

                remoteRef = remoteBusinessRefFactory.createRemoteReference
                        (instanceKey);
            }
            return remoteRef;
        } finally {
            currentThread.setContextClassLoader(previousClassLoader);
        }
    }


    private void assertFullProfile(String description) {
        if (ejbContainerUtilImpl.isEJBLite()) {
            throw new RuntimeException(localStrings.getLocalString(
                    "ejb.assert_full_profile",
                    "Invalid application.  EJB {0} {1}. This feature is not part of the EJB 3.1 Lite API",
                    ejbDescriptor.getName(), description));
        }
    }

    private void warnIfNotFullProfile(String description) {
        if (ejbContainerUtilImpl.isEJBLite()) {
            _logger.log(Level.WARNING, WARN_FEATURE_REQUIRES_FULL_PROFILE, description);
        }
    }


    /**
     * Called from the ContainerFactory during initialization.
     */
    protected void initializeHome()
            throws Exception {

        if (isWebServiceEndpoint) {

            EjbBundleDescriptorImpl bundle =
                    ejbDescriptor.getEjbBundleDescriptor();
            WebServicesDescriptor webServices = bundle.getWebServices();
            Collection myEndpoints =
                    webServices.getEndpointsImplementedBy(ejbDescriptor);


            // An ejb can only be exposed through 1 web service endpoint
            Iterator iter = myEndpoints.iterator();
            webServiceEndpoint =
                    (com.sun.enterprise.deployment.WebServiceEndpoint) iter.next();

            Class serviceEndpointIntfClass = loader.loadClass(webServiceEndpoint.getServiceEndpointInterface());

            if (serviceEndpointIntfClass == null || !serviceEndpointIntfClass.isInterface()) {
                serviceEndpointIntfClass = EJBUtils.generateSEI(loader, ejbClass);
                if (serviceEndpointIntfClass == null) {
                    throw new RuntimeException(localStrings.getLocalString(
                            "ejb.error_generating_sei",
                            "Error in generating service endpoint interface class for EJB class {0}", this.ejbClass));
                }
            }


            WebServiceInvocationHandler invocationHandler = new WebServiceInvocationHandler(ejbClass,
                    webServiceEndpoint, serviceEndpointIntfClass, ejbContainerUtilImpl, webServiceInvocationInfoMap);

            invocationHandler.setContainer(this);
            Object servant = Proxy.newProxyInstance(loader, new Class[]{serviceEndpointIntfClass}, invocationHandler);

            // starting in 2.0, there is no more generated Ties
            final Class<?> tieClass = getTieClass();

            // Create a facade for container services to be used by web services runtime.
            EjbEndpointFacade endpointFacade = new EjbEndpointFacadeImpl(this, ejbContainerUtilImpl);

            wsejbEndpointRegistry = Globals.getDefaultBaseServiceLocator().getService(WSEjbEndpointRegistry.class);
            if (wsejbEndpointRegistry == null) {
                throw new DeploymentException(localStrings.getLocalString("ejb.no_webservices_module",
                        "EJB-based Webservice endpoint is detected"
                                + " but there is no webservices module installed to handle it"));
            }
            wsejbEndpointRegistry.registerEndpoint(webServiceEndpoint, endpointFacade, servant, tieClass);
        }

        Map<String, Object> intfsForPortableJndi = new HashMap<>();

        // Root of portable global JNDI name for this bean
        String javaGlobalName = getJavaGlobalJndiNamePrefix();

        if (isRemote) {
            boolean disableNonPortableJndiName = false;
            Boolean disableInDD = ejbDescriptor.getEjbBundleDescriptor().getDisableNonportableJndiNames();
            if (disableInDD != null) {  // explicitly set in glassfish-ejb-jar.xml
                disableNonPortableJndiName = disableInDD;
            } else {
                String disableInServer = ejbContainerUtilImpl.getEjbContainer()
                        .getPropertyValue(RuntimeTagNames.DISABLE_NONPORTABLE_JNDI_NAMES);
                disableNonPortableJndiName = Boolean.parseBoolean(disableInServer);
            }

            String glassfishSpecificJndiName = null;
            if (!disableNonPortableJndiName) {
                // This is either the default glassfish-specific (non-portable)
                // global JNDI name or the one specified via mappedName(), sun-ejb-jar.xml,
                // etc.
                glassfishSpecificJndiName = ejbDescriptor.getJndiName();

                // If the explicitly specified name is the same as the portable name,
                // don't register any of the glassfish-specific names to prevent
                // clashes.
                if ((glassfishSpecificJndiName != null)
                        && (glassfishSpecificJndiName.equals("")
                        || glassfishSpecificJndiName.equals(javaGlobalName))) {
                    glassfishSpecificJndiName = null;
                }
            }

            if (hasRemoteHomeView) {
                this.ejbHomeImpl = instantiateEJBHomeImpl();
                this.ejbHome = ejbHomeImpl.getEJBHome();
                
                //
                // Make sure all Home/Remote interfaces conform to RMI-IIOP
                // rules.  Checking for conformance here keeps the exposed
                // deployment/startup error behavior consistent since when
                // rmic is used during codegen it makes equivalent checks and
                // treats any validation problems as fatal errors.
                //
                // These same checks will be made when setTarget is called
                // in POARemoteReferenceFactory.preinvoke, but that happens
                // only when the actual invocation is made, so it's better to
                // know at container initialization time if there is a problem.
                //

                getProtocolManager().validateTargetObjectInterfaces(this.ejbHome);

                // Unlike the Home, each of the concrete containers are
                // responsible for creating the EJBObjects, so just create
                // a dummy EJBObjectImpl for validation purposes.
                EJBObjectImpl dummyEJBObjectImpl = instantiateEJBObjectImpl();
                EJBObject dummyEJBObject = (EJBObject) dummyEJBObjectImpl.getEJBObject();
                getProtocolManager().validateTargetObjectInterfaces(dummyEJBObject);

                // Remotereference factory needs instances of
                // Home and Remote to get repository Ids since it doesn't have
                // stubs and ties.  This must be done before any Home or Remote
                // references are created.
                remoteHomeRefFactory.setRepositoryIds(homeIntf, remoteIntf);

                // get a remote ref for the EJBHome
                ejbHomeStub = (EJBHome) remoteHomeRefFactory.createHomeReference(homeInstanceKey);

                // Add 2.x Home for later portable JNDI name processing.
                intfsForPortableJndi.put(ejbDescriptor.getHomeClassName(), ejbHomeStub);

                // If there's a glassfish-specific JNDI name, any 2.x Home object is always
                // regsitered under that name.  This preserves backward compatibility since
                // this was the original use of the jndi name.
                if (glassfishSpecificJndiName != null) {

                    JndiInfo jndiInfo = JndiInfo.newNonPortableRemote(glassfishSpecificJndiName, ejbHomeStub);
                    jndiInfoMap.put(jndiInfo.name, jndiInfo);
                }

            }

            if (hasRemoteBusinessView) {
                this.ejbRemoteBusinessHomeImpl = instantiateEJBRemoteBusinessHomeImpl();
                this.ejbRemoteBusinessHome = ejbRemoteBusinessHomeImpl.getEJBHome();

                // RMI-IIOP validation
                getProtocolManager().validateTargetObjectInterfaces(this.ejbRemoteBusinessHome);
                for (RemoteBusinessIntfInfo next : remoteBusinessIntfInfo.values()) {
                    // Remotereference factory needs instances of
                    // Home and Remote to get repository Ids since it
                    // doesn't have stubs and ties.  This must be done before
                    // any Home or Remote references are created.
                    next.referenceFactory.setRepositoryIds
                            (remoteBusinessHomeIntf, next.generatedRemoteIntf);

                    // Create home stub from the remote reference factory
                    // associated with one of the remote business interfaces.
                    // It doesn't matter which remote reference factory is
                    // selected, so just do it the first time through the loop.
                    if (ejbRemoteBusinessHomeStub == null) {
                        ejbRemoteBusinessHomeStub = (EJBHome) next.referenceFactory
                                .createHomeReference(homeInstanceKey);
                    }

                }

                EJBObjectImpl dummyEJBObjectImpl = instantiateRemoteBusinessObjectImpl();


                // Internal jndi name under which remote business home is registered for
                // glassfish-specific remote business JNDI names
                String remoteBusinessHomeJndiName = null;

                if (glassfishSpecificJndiName != null) {

                    remoteBusinessHomeJndiName =
                            EJBUtils.getRemote30HomeJndiName(glassfishSpecificJndiName);
                }

                // Convenience location for common case of 3.0 session bean with only
                // 1 remote business interface and no adapted remote home.  Allows a
                // stand-alone client to access 3.0 business interface by using simple
                // jndi name.  Each remote business interface is also always available
                // at <jndi-name>#<business_interface_name>.  This is needed for the
                // case where the bean has an adapted remote home and/or multiple business
                // interfaces.
                String simpleRemoteBusinessJndiName = null;

                if (glassfishSpecificJndiName != null && !hasRemoteHomeView && remoteBusinessIntfInfo.size() == 1) {
                    simpleRemoteBusinessJndiName = glassfishSpecificJndiName;
                }

                // We need a separate name for the internal generated home object to
                // support the portable global JNDI names for business interfaces.
                // There won't necessarily be a glassfish-specific name specified so
                // it's cleaner to just always use a separate ones.
                String internalHomeJndiNameForPortableRemoteNames = EJBUtils.getRemote30HomeJndiName(javaGlobalName);

                for (RemoteBusinessIntfInfo next : remoteBusinessIntfInfo.values()) {

                    java.rmi.Remote dummyEJBObject = dummyEJBObjectImpl
                            .getEJBObject(next.generatedRemoteIntf.getName());

                    getProtocolManager().validateTargetObjectInterfaces(dummyEJBObject);

                    if (glassfishSpecificJndiName != null) {

                        next.jndiName = EJBUtils.getRemoteEjbJndiName
                                (true, next.remoteBusinessIntf.getName(), glassfishSpecificJndiName);

                        Reference remoteBusRef = new Reference(next.remoteBusinessIntf.getName(),
                                new StringRefAddr("url", remoteBusinessHomeJndiName),
                                "com.sun.ejb.containers.RemoteBusinessObjectFactory", null);


                        // Glassfish-specific JNDI name for fully-qualified 3.0 Remote business interface.
                        JndiInfo jndiInfo = JndiInfo.newNonPortableRemote(next.jndiName, remoteBusRef);
                        jndiInfoMap.put(jndiInfo.name, jndiInfo);
                    }

                    if (simpleRemoteBusinessJndiName != null) {

                        Reference remoteBusRef = new Reference
                                (next.remoteBusinessIntf.getName(),
                                        new StringRefAddr("url", remoteBusinessHomeJndiName),
                                        "com.sun.ejb.containers.RemoteBusinessObjectFactory", null);

                        // Glassfish-specific JNDI name for simple 3.0 Remote business interface lookup.
                        // Applicable when the bean exposes only a single Remote 3.x client view.
                        JndiInfo jndiInfo = JndiInfo.newNonPortableRemote
                                (simpleRemoteBusinessJndiName, remoteBusRef);
                        jndiInfoMap.put(jndiInfo.name, jndiInfo);
                    }

                    Reference remoteBusRef = new Reference(next.remoteBusinessIntf.getName(),
                            new StringRefAddr("url", internalHomeJndiNameForPortableRemoteNames),
                            "com.sun.ejb.containers.RemoteBusinessObjectFactory", null);

                    // Always register portable JNDI name for each remote business view
                    intfsForPortableJndi.put(next.remoteBusinessIntf.getName(), remoteBusRef);

                }


                if (remoteBusinessHomeJndiName != null) {
                    // Glassfish-specific JNDI name for internal generated
                    // home object used by container
                    JndiInfo jndiInfo = JndiInfo.newNonPortableRemote
                            (remoteBusinessHomeJndiName, ejbRemoteBusinessHomeStub);
                    jndiInfo.setInternal(true);
                    jndiInfoMap.put(jndiInfo.name, jndiInfo);
                }

                // Always registeer internal name for home in support of portable global
                // remote business JNDI names.
                JndiInfo jndiInfo = JndiInfo.newPortableRemote
                        (internalHomeJndiNameForPortableRemoteNames, ejbRemoteBusinessHomeStub);
                jndiInfo.setInternal(true);
                jndiInfoMap.put(jndiInfo.name, jndiInfo);

                // If there isn't any jndi name from the descriptor, set one so the
                // lookup logic that depends on ejbDescriptor.getJndiName() will work.
                if (glassfishSpecificJndiName == null) {
                    ejbDescriptor.setJndiName(javaGlobalName);
                }
            }
        }

        if (isLocal) {

            if (hasLocalHomeView) {
                this.ejbLocalHomeImpl = instantiateEJBLocalHomeImpl();
                this.ejbLocalHome = ejbLocalHomeImpl.getEJBLocalHome();
                // Portable JNDI name for EJB 2.x LocalHome.  We don't provide a
                // glassfish-specific way of accessing Local EJBs.

                JavaGlobalJndiNamingObjectProxy namingProxy = //
                        new JavaGlobalJndiNamingObjectProxy(this, localHomeIntf.getName());

                intfsForPortableJndi.put(localHomeIntf.getName(), namingProxy);
            }

            if (hasLocalBusinessView) {
                ejbLocalBusinessHomeImpl = instantiateEJBLocalBusinessHomeImpl();
                ejbLocalBusinessHome = (GenericEJBLocalHome) ejbLocalBusinessHomeImpl.getEJBLocalHome();

                ejbLocalBusinessProxyInterfaces = new Class[localBusinessIntfs.size() + 1];
                ejbLocalBusinessProxyInterfaces[0] = IndirectlySerializable.class;
                int index = 1;
                for (Class<?> next : localBusinessIntfs) {
                    ejbLocalBusinessProxyInterfaces[index] = next;
                    index++;
                }
                
                for (Class<?> next : localBusinessIntfs) {
                    // Portable JNDI name for EJB 3.x Local business interface.
                    // We don't provide a glassfish-specific way of accessing Local EJBs.
                    JavaGlobalJndiNamingObjectProxy namingProxy =
                            new JavaGlobalJndiNamingObjectProxy(this, next.getName());
                    intfsForPortableJndi.put(next.getName(), namingProxy);
                }

            }

            if (hasOptionalLocalBusinessView) {
                EJBLocalHomeImpl obj = instantiateEJBOptionalLocalBusinessHomeImpl();
                ejbOptionalLocalBusinessHomeImpl = obj;
                ejbOptionalLocalBusinessHome = (GenericEJBLocalHome) ejbOptionalLocalBusinessHomeImpl.getEJBLocalHome();

                ejbOptionalLocalBusinessHomeProxyInterfaces  = new Class[2];
                ejbOptionalLocalBusinessHomeProxyInterfaces[0] = IndirectlySerializable.class;
                String optionalIntfName = EJBUtils.getGeneratedOptionalInterfaceName(ejbClass.getName());
                ejbGeneratedOptionalLocalBusinessIntfClass = optIntfClassLoader.loadClass(optionalIntfName);
                ejbOptionalLocalBusinessHomeProxyInterfaces[1] = ejbGeneratedOptionalLocalBusinessIntfClass;

                // Portable JNDI name for no-interface view.
                // We don't provide a glassfish-specific way of accessing the
                // no-interface view of a session bean.
                JavaGlobalJndiNamingObjectProxy namingProxy = //
                        new JavaGlobalJndiNamingObjectProxy(this, ejbClass.getName());

                intfsForPortableJndi.put(ejbClass.getName(), namingProxy);
            }
        }

        for (Map.Entry<String, Object> entry : intfsForPortableJndi.entrySet()) {
            String intf = entry.getKey();

            String fullyQualifiedJavaGlobalName = javaGlobalName + "!" + intf;
            Object namingProxy = entry.getValue();
            boolean local = (namingProxy instanceof JavaGlobalJndiNamingObjectProxy);

            if (intfsForPortableJndi.size() == 1) {
                JndiInfo jndiInfo = local ?
                        JndiInfo.newPortableLocal(javaGlobalName, namingProxy) :
                        JndiInfo.newPortableRemote(javaGlobalName, namingProxy);
                jndiInfoMap.put(jndiInfo.name, jndiInfo);
            }

            JndiInfo jndiInfo = local ?
                    JndiInfo.newPortableLocal(fullyQualifiedJavaGlobalName, namingProxy) :
                    JndiInfo.newPortableRemote(fullyQualifiedJavaGlobalName, namingProxy);
            jndiInfoMap.put(jndiInfo.name, jndiInfo);

        }

        for (Map.Entry<String, JndiInfo> entry : jndiInfoMap.entrySet()) {
            JndiInfo jndiInfo = entry.getValue();
            try {
                jndiInfo.publish(this.namingManager);
                if (jndiInfo.internal) {
                    publishedInternalGlobalJndiNames.add(jndiInfo.name);
                } else {
                    if (jndiInfo.portable) {
                        publishedPortableGlobalJndiNames.add(jndiInfo.name);
                    } else {
                        publishedNonPortableGlobalJndiNames.add(jndiInfo.name);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(localStrings.getLocalString(
                        "ejb.error_binding_jndi_name",
                        "Error while binding JNDI name {0} for EJB {1}",
                        jndiInfo.name, this.ejbDescriptor.getName()), e);
            }
        }

        if (!publishedPortableGlobalJndiNames.isEmpty()) {
            _logger.log(Level.INFO, PORTABLE_JNDI_NAMES,
                    new Object[]{this.ejbDescriptor.getName(), publishedPortableGlobalJndiNames});
        }

        if (!publishedNonPortableGlobalJndiNames.isEmpty()) {
            _logger.log(Level.INFO, GLASSFISH_SPECIFIC_JNDI_NAMES,
                    new Object[]{this.ejbDescriptor.getName(), publishedNonPortableGlobalJndiNames});
        }

        if (!publishedInternalGlobalJndiNames.isEmpty()) {
            _logger.log(Level.FINE, "Internal container JNDI names for EJB {0}: {1}",
                    new Object[]{this.ejbDescriptor.getName(), publishedInternalGlobalJndiNames});
        }

        // set EJBMetaData
        setEJBMetaData();
    }

    // default impl
    protected void setEJBMetaData() throws Exception {
        metadata = new EJBMetaDataImpl(ejbHomeStub, homeIntf, remoteIntf, isSession, isStatelessSession);
    }

    protected String getJavaGlobalJndiNamePrefix() {

        String appName = null;

        Application app = ejbDescriptor.getApplication();
        if (!app.isVirtual()) {
            appName = ejbDescriptor.getApplication().getAppName();
        }

        EjbBundleDescriptorImpl ejbBundle = ejbDescriptor.getEjbBundleDescriptor();
        String modName = ejbBundle.getModuleDescriptor().getModuleName();

        String ejbName = ejbDescriptor.getName();

        StringBuilder javaGlobalPrefix = new StringBuilder("java:global/");

        if (appName != null) {
            javaGlobalPrefix.append(appName);
            javaGlobalPrefix.append("/");
        }

        javaGlobalPrefix.append(modName);
        javaGlobalPrefix.append("/");
        javaGlobalPrefix.append(ejbName);


        return javaGlobalPrefix.toString();
    }

    // This method is used to create the ejb after the around_construct interceptor chain has completed.
    public void createEjbInstanceForInterceptors(Object[] params, EJBContextImpl context) throws Exception {
        Object instance;
        if (isJCDIEnabled()) {
            // ejb creation for cdi is handled in JCDIServiceImpl not here.
            instance = context.getJCDIInjectionContext().createEjbAfterAroundConstruct();
        } else {
            // this is only for non-cdi case.
            instance = _constructEJBInstance();
        }
        context.setEJB(instance);
    }

    protected void createEmptyContextAndInterceptors(EJBContextImpl context) throws Exception {
        JCDIService.JCDIInjectionContext<?> cdiContext = null;
        if (isJCDIEnabled()) {
            // In cdi we need this for the interceptors to store dependent jcdi contexts.  We can't assign the
            // other info as the ejb has not been created yet.
            cdiContext = jcdiService.createEmptyJCDIInjectionContext();
            context.setJCDIInjectionContext(cdiContext);
        }

        // Interceptors must be created before the ejb so they're available for around construct.
        createEjbInterceptors(context, cdiContext);
    }

    protected EJBContextImpl createEjbInstanceAndContext() throws Exception {
        JCDIService.JCDIInjectionContext<?> jcdiCtx = null;
        Object instance = null;

        EJBContextImpl ctx = _constructEJBContextImpl(null);
        EjbInvocation ejbInv = null;
        boolean success = false;
        try {
            ejbInv = createEjbInvocation(null, ctx);
            invocationManager.preInvoke(ejbInv);

            createEmptyContextAndInterceptors(ctx);

            if (isJCDIEnabled()) {
                ctx.setJCDIInjectionContext(_createJCDIInjectionContext(ctx, null, jcdiCtx));
                jcdiCtx = ctx.getJCDIInjectionContext();
                if (jcdiCtx != null) {
                    instance = jcdiCtx.getInstance();
                }
            } else {
                injectEjbInstance(ctx);
                intercept(CallbackType.AROUND_CONSTRUCT, ctx);
                instance = ctx.getEJB();
            }
            success = true;

        } catch (Throwable th) {
            try {
                if (jcdiCtx != null) {
                    // protecte against memory leak
                    jcdiCtx.cleanup(true);
                }
            } catch (Throwable ignore) {
            }
            throw new InvocationTargetException(th);
        } finally {
            try {
                if (ejbInv != null) {
                    // Complete the dummy invocation
                    invocationManager.postInvoke(ejbInv);
                }
            } catch (Throwable t) {
                if (success) {
                    throw new InvocationTargetException(t);
                } else {
                    _logger.log(Level.WARNING, "", t);
                }
            }
        }

        ctx.setEJB(instance);

        return ctx;
    }

    protected boolean isJCDIEnabled() {
        return jcdiService != null && jcdiService.isJCDIEnabled(ejbDescriptor.getEjbBundleDescriptor()) && (this.ejbClass.getAnnotation(Vetoed.class) == null);
    }

    protected JCDIService.JCDIInjectionContext<?> _createJCDIInjectionContext(EJBContextImpl ctx, Object instance) {
        return _createJCDIInjectionContext(ctx, instance, null);
    }

    protected JCDIService.JCDIInjectionContext<?> _createJCDIInjectionContext(EJBContextImpl ejbContext, Object instance, JCDIService.JCDIInjectionContext<?> cdiInjectionContext) {
        JCDIService.JCDIInjectionContext<?> context = jcdiService.createJCDIInjectionContext(ejbDescriptor,
                instance, buildJCDIInjectionEjbInfo(ejbContext, cdiInjectionContext));
        if (context == null) {
            jcdiService = null;
        }
        return context;
    }

    private HashMap<Class<?>, Object> buildJCDIInjectionEjbInfo(EJBContextImpl ejbContext, JCDIService.JCDIInjectionContext<?> cdiInjectionContext) {
        HashMap<Class<?>, Object> ejbInfo = new HashMap<>();
        ejbInfo.put(BaseContainer.class, this);
        ejbInfo.put(EJBContextImpl.class, ejbContext);
        ejbInfo.put(JCDIService.JCDIInjectionContext.class, cdiInjectionContext == null ? jcdiService.createEmptyJCDIInjectionContext() : cdiInjectionContext);
        return ejbInfo;
    }

    /**
     * Must be overridden for any container that supports injection
     *
     * @param instance
     * @return {@link EJBContextImpl}
     */
    protected EJBContextImpl _constructEJBContextImpl(Object instance) {
        throw new IllegalStateException("Not implemented by this container: " + getClass());
    }

    protected Object _constructEJBInstance() throws Exception {
        return ejbClass.newInstance();
    }

    private void createEjbInterceptors(EJBContextImpl context,
                                       JCDIService.JCDIInjectionContext<?> ejbInterceptorsJCDIInjectionContext) throws Exception {
        Object[] interceptorInstances;
        //sanitizing the null reference of interceptorManager
        if (interceptorManager == null) {
            _logger.severe("The reference for interceptorManager is not available, this is an un-sync state of the container");
            return;
        }

        if (isJCDIEnabled()) {
            Class[] interceptorClasses = interceptorManager.getInterceptorClasses();
            interceptorInstances = new Object[interceptorClasses.length];
            for (int i = 0; i < interceptorClasses.length; i++) {
                // 299 impl will instantiate and inject the instance, but PostConstruct
                // is still our responsibility
                interceptorInstances[i] = jcdiService.createInterceptorInstance(interceptorClasses[i], ejbDescriptor,
                        ejbInterceptorsJCDIInjectionContext,
                        context.getContainer().getEjbDescriptor().getInterceptorClasses());
            }
            interceptorManager.initializeInterceptorInstances(interceptorInstances);
        } else {
            interceptorInstances = interceptorManager.createInterceptorInstances();
            for (Object interceptorInstance : interceptorInstances) {
                injectionManager.injectInstance(interceptorInstance, ejbDescriptor, false);
            }
        }

        context.setInterceptorInstances(interceptorInstances);
    }


    protected void injectEjbInstance(EJBContextImpl context) throws Exception {
        if (isJCDIEnabled()) {
            jcdiService.injectEJBInstance(context.getJCDIInjectionContext());
        } else {
            if (context.getEJB() != null) {
                injectionManager.injectInstance(context.getEJB(), ejbDescriptor, false);
            }
        }
    }

    protected void cleanupInstance(EJBContextImpl context) {

        JCDIService.JCDIInjectionContext<?> jcdiCtx = context.getJCDIInjectionContext();
        if (jcdiCtx != null) {
            jcdiCtx.cleanup(false);
        }

    }

    /**
     * Return the EJBObject/EJBHome Proxy for the given ejbId and instanceKey.
     * Called from the ProtocolManager when a remote invocation arrives.
     *
     * @throws NoSuchObjectLocalException if the target object does not exist
     */
    @Override
    public java.rmi.Remote getTargetObject(byte[] instanceKey,
                                           String generatedRemoteBusinessIntf) {

        externalPreInvoke();
        boolean remoteHomeView = (generatedRemoteBusinessIntf == null);
        if (instanceKey.length == 1 && instanceKey[0] == HOME_KEY) {
            return remoteHomeView ?
                    ejbHomeImpl.getEJBHome() :
                    ejbRemoteBusinessHomeImpl.getEJBHome();
        } else {

            java.rmi.Remote targetObject = null;
            EJBObjectImpl ejbObjectImpl = null;


            if (remoteHomeView) {
                ejbObjectImpl = getEJBObjectImpl(instanceKey);
                // In rare cases for sfsbs and entity beans, this can be null.
                if (ejbObjectImpl != null) {
                    targetObject = ejbObjectImpl.getEJBObject();
                }
            } else {
                ejbObjectImpl = getEJBRemoteBusinessObjectImpl(instanceKey);
                // In rare cases for sfsbs and entity beans, this can be null.
                if (ejbObjectImpl != null) {
                    targetObject = ejbObjectImpl.
                            getEJBObject(generatedRemoteBusinessIntf);
                }
            }

            return targetObject;
        }
    }

    /**
     * Release the EJBObject/EJBHome object.
     * Called from the ProtocolManager after a remote invocation completes.
     */
    @Override
    public void releaseTargetObject(java.rmi.Remote remoteObj) {
        externalPostInvoke();
    }

    @Override
    public void externalPreInvoke() {
        BeanContext beanContext = new BeanContext();
        final Thread currentThread = Thread.currentThread();
        beanContext.previousClassLoader = currentThread.getContextClassLoader();
        if (getClassLoader().equals(beanContext.previousClassLoader) == false) {
            currentThread.setContextClassLoader(getClassLoader());
            beanContext.classLoaderSwitched = true;
        }

        ArrayDeque beanContextStack =
                (ArrayDeque) threadLocalContext.get();

        if (beanContextStack == null) {
            beanContextStack = new ArrayDeque();
            threadLocalContext.set(beanContextStack);
        }
        beanContextStack.push(beanContext);
    }

    @Override
    public void externalPostInvoke() {
        try {
            ArrayDeque beanContextStack =
                    (ArrayDeque) threadLocalContext.get();

            final BeanContext bc = (BeanContext) beanContextStack.pop();
            if (bc.classLoaderSwitched == true) {
                Thread.currentThread().setContextClassLoader(bc.previousClassLoader);
            }
        } catch (Exception ex) {
            _logger.log(Level.FINE, "externalPostInvoke ex", ex);
        }
    }

    private boolean doPreInvokeAuthorization(EjbInvocation inv) {

        // preInvocation authorization does not apply if this is a timer callback
        // OR if it's a remove operation initiated via the 299 SPI
        boolean skipPreInvokeAuth = inv.isTimerCallback ||
                (inv.isLocal &&
                        inv.method.equals(ejbIntfMethods[EJBLocalObject_remove]) &&
                        !((EJBLocalObjectImpl) inv.ejbObject).isLocalHomeView());

        return !skipPreInvokeAuth;
    }


    /**
     * Called from EJBObject/EJBHome before invoking on EJB.
     * Set the EJB instance in the EjbInvocation.
     * <p>
     * It must be ensured that the following general pattern
     * is followed by various parts of the EJBContainer code:
     * <p>
     * try {
     * container.preInvoke(inv);
     * returnValue = container.intercept(inv);
     * } catch (Exception1 e1) {
     * ...
     * } catch (Exception2 e2) {
     * ...
     * } finally {
     * container.postInvoke();
     * }
     *
     */
    @Override
    public void preInvoke(EjbInvocation inv) {

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Entering BaseContainer::preInvoke : " + inv);
        }

        try {
            if (containerState != CONTAINER_STARTED) {
                throw new EJBException(localStrings.getLocalString(
                        "ejb.container_not_started",
                        "Attempt to invoke when container is in {0}",
                        containerStateToString(containerState)));
            }

            if (inv.method == null) {
                throw new EJBException(localStrings.getLocalString(
                        "ejb.null_invocation_method",
                        "Attempt to invoke container with null invocation method"));
            }

            if (inv.invocationInfo == null) {
                inv.invocationInfo = getInvocationInfo(inv);
                if (inv.invocationInfo == null) {
                    throw new EJBException(localStrings.getLocalString(
                            "ejb.null_invocation_info",
                            "EjbInvocation Info lookup failed for method {0}", inv.method));
                }
            }

            inv.transactionAttribute = inv.invocationInfo.txAttr;
            inv.setContainer(this);

            if (inv.mustInvokeAsynchronously()) {
                return;
            }

            if (doPreInvokeAuthorization(inv)) {
                if (!authorize(inv)) {
                    throw new AccessLocalException(localStrings.getLocalString(
                            "ejb.client_not_authorized",
                            "Client not authorized for this invocation"));
                }
            }

            // Cache value of txManager.getStatus() in invocation to avoid
            // multiple thread-local accesses of that value during pre-invoke
            // stage.
            inv.setPreInvokeTxStatus(transactionManager.getStatus());

            ComponentContext ctx = getContext(inv);
            inv.context = ctx;

            inv.instance = inv.ejb = ctx.getEJB();
            InvocationInfo info = inv.invocationInfo;

            inv.useFastPath = (info.isTxRequiredLocalCMPField) && (inv.foundInTxCache);
            //    _logger.log(Level.INFO, "Use fastPath() ==> " + info.method);

            if (!inv.useFastPath) {
                // Sets thread-specific state for Transaction, Naming, Security,
                // etc
                invocationManager.preInvoke(inv);

                // Do Tx machinery
                preInvokeTx(inv);

                // null out invocation preInovkeTxStatus since the cache value
                // is obsolete
                inv.setPreInvokeTxStatus(null);

                enlistExtendedEntityManagers(ctx);
            }

        } catch (Exception ex) {
            _logger.log(Level.FINE, "Exception while running pre-invoke : ejbName = [{0}]", logParams);
            _logger.log(Level.FINE, "", ex);

            EJBException ejbEx;
            if (ex instanceof EJBException) {
                ejbEx = (EJBException) ex;
            } else {
                ejbEx = new EJBException(ex);
            }

            throw new PreInvokeException(ejbEx);
        }
    }

    public boolean intercept(CallbackType eventType, EJBContextImpl ctx)
            throws Throwable {
        //sanitizing the null reference of interceptorManager
        if (interceptorManager == null) {
            _logger.severe("The reference for interceptorManager is not available, this is an un-sync state of the container");
            return false;
        }
        return interceptorManager.intercept(eventType, ctx);
    }

    protected void enlistExtendedEntityManagers(ComponentContext ctx) {
        // Do nothing in general case
    }

    protected void delistExtendedEntityManagers(ComponentContext ctx) {
        // Do nothing in general case
    }

    /**
     * Containers that allow extended EntityManager will override this method.
     */
    @Override
    public EntityManager lookupExtendedEntityManager(EntityManagerFactory emf) {
        throw new IllegalStateException(localStrings.getLocalString(
                "ejb.extended_persistence_context_not_supported",
                "EntityManager with PersistenceContextType.EXTENDED is not supported for this bean type"));
    }

    @Override
    public void webServicePostInvoke(EjbInvocation inv) {
        // postInvokeTx is handled by WebServiceInvocationHandler.
        // Invoke postInvoke with instructions to skip tx processing portion.
        postInvoke(inv, false);
    }

    /**
     * Called from EJBObject/EJBHome after invoking on bean.
     */
    @Override
    public void postInvoke(EjbInvocation inv) {
        postInvoke(inv, true);
    }

    protected void postInvoke(EjbInvocation inv, boolean doTxProcessing) {
        if (containerState != CONTAINER_STARTED) {
            throw new EJBException(localStrings.getLocalString(
                    "ejb.container_not_started",
                    "Attempt to invoke when container is in {0}",
                    containerStateToString(containerState)));
        }

        inv.setDoTxProcessingInPostInvoke(doTxProcessing);
        if (inv.mustInvokeAsynchronously()) {
            EjbAsyncInvocationManager asyncManager =
                    ((EjbContainerUtilImpl) ejbContainerUtilImpl).getEjbAsyncInvocationManager();
            asyncManager.submit(inv);
            return;
        }

        if (inv.ejb != null) {
            // counterpart of invocationManager.preInvoke
            if (!inv.useFastPath) {
                invocationManager.postInvoke(inv);
                delistExtendedEntityManagers(inv.context);
            } else {
                doTxProcessing = doTxProcessing && (inv.exception != null);
            }

            try {
                if (doTxProcessing) {
                    postInvokeTx(inv);
                }
            } catch (Exception ex) {
                _logger.log(Level.FINE, "Exception occurred in postInvokeTx  : [{0}]", ex);
                if (ex instanceof EJBException) {
                    inv.exception = ex;
                } else {
                    inv.exception = new EJBException(ex);
                }
            }
            releaseContext(inv);
        }

        if (inv.exception != null) {

            // Unwrap the PreInvokeException if necessary
            if (inv.exception instanceof PreInvokeException) {
                inv.exception = inv.exception.getCause();
            }

            // Log system exceptions by default and application exceptions only
            // when log level is FINE or higher.

            if (isSystemUncheckedException(inv.exception)) {
                _logger.log(Level.WARNING, SYSTEM_EXCEPTION, new Object[]{ejbDescriptor.getName(), inv.beanMethod});
                _logger.log(Level.WARNING, "", inv.exception);
            } else {
                _logger.log(Level.FINE,
                        "An application exception occurred during an invocation on EJB {0}, method: {1}",
                        new Object[]{ejbDescriptor.getName(), inv.beanMethod});
                _logger.log(Level.FINE, "", inv.exception);
            }

            if (inv.isRemote) {
                if (protocolMgr != null) {
                    // For remote business case, exception mapping is performed
                    // in client wrapper.
                    // TODO need extra logic to handle implementation-specific ejb exceptions
                    // (ParallelAccessEXCeption etc. that used to be handled by iiop glue code
                    inv.exception = mapRemoteException(inv);
                }

                // The most useful portion of the system exception is logged
                // above.  Only log mapped form when log level is FINE or
                // higher.
                _logger.log(Level.FINE, "", inv.exception);

            } else {
                if (inv.isBusinessInterface) {
                    inv.exception = mapLocal3xException(inv.exception);
                }
            }

        }
        /*TODO
        if ( AppVerification.doInstrument()) {
            // need to pass the method, exception info,
            // and EJB descriptor to get app info
            AppVerification.getInstrumentLogger().doInstrumentForEjb(
            ejbDescriptor, inv.method, inv.exception);

        }
        */

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Leaving BaseContainer::postInvoke : " + inv);
        }
    }


    /**
     * Check if caller is authorized to invoke the method.
     * Only called for EJBLocalObject and EJBLocalHome methods,
     * from EJBLocalHome|ObjectImpl classes.
     *
     * @param method an integer identifying the method to be checked,
     *               must be one of the EJBLocal{Home|Object}_* constants.
     */
    protected void authorizeLocalMethod(int method) {

        EjbInvocation inv = invFactory.create();
        inv.isLocal = true;
        inv.isHome = EJB_INTF_METHODS_INFO[method];
        inv.method = ejbIntfMethods[method];
        inv.invocationInfo = ejbIntfMethodInfo[method];

        if (!authorize(inv)) {
            throw new AccessLocalException(localStrings.getLocalString( //
                    "ejb.client_not_authorized", //
                    "Client not authorized for this invocation"));
        }
    }

    /**
     * Check if caller is authorized to invoke the method.
     * Only called for EJBObject and EJBHome methods,
     * from EJBHome|ObjectImpl classes.
     *
     * @param method an integer identifying the method to be checked,
     *               must be one of the EJB{Home|Object}_* constants.
     */
    protected void authorizeRemoteMethod(int method) throws RemoteException {
        EjbInvocation inv = invFactory.create();
        inv.isLocal = false;
        inv.isHome = EJB_INTF_METHODS_INFO[method];
        inv.method = ejbIntfMethods[method];
        inv.invocationInfo = ejbIntfMethodInfo[method];

        if (!authorize(inv)) {
            final Throwable t = mapRemoteException(inv);
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof RemoteException) {
                throw (RemoteException) t;
            } else {
                throw new AccessException(localStrings.getLocalString( //
                        "ejb.client_not_authorized", //
                        "Client not authorized for this invocation"));
            }
        }
    }

    /**
     * Call back from the timer migration process to add
     * automatic timers to the map of scheduleIds
     */
    void addSchedule(TimerPrimaryKey timerId, EJBTimerSchedule ts) {
        for (Map.Entry<Method, List<ScheduledTimerDescriptor>> entry : schedules.entrySet()) {
            Method m = entry.getKey();
            if (m.getName().equals(ts.getTimerMethodName()) &&
                    m.getParameterTypes().length == ts.getMethodParamCount()) {
                scheduleIds.put(timerId, m);
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Adding schedule: " +
                            ts.getScheduleAsString() + " FOR method: " + m);
                }
            }
        }
    }

    /**
     * Check timeout method and set it accessible
     */
    private void processEjbTimeoutMethod(Method method) throws Exception {
        Class[] params = method.getParameterTypes();
        if ((params.length == 0 ||
                (params.length == 1 && params[0] == jakarta.ejb.Timer.class)) &&
                (method.getReturnType() == Void.TYPE)) {

            isTimedObject_ = true;

            final Method ejbTimeoutAccessible = method;
            // Since timeout method can have any kind of access
            // setAccessible to true.
            if (!ejbTimeoutAccessible.isAccessible()) {
                ejbTimeoutAccessible.setAccessible(true);
            }
        } else {
            throw new EJBException(localStrings.getLocalString(
                    "ejb.invalid_timeout_method",
                    "Invalid @Timeout or @Schedule signature for: {0} @Timeout or @Schedule method must return void and be a no-arg method or take a single jakarta.ejb.Timer param",
                    method));
        }
    }

    private Class<?> getTieClass() throws ClassNotFoundException {
        if (webServiceEndpoint.getTieClassName() == null) {
            return null;
        }
        return loader.loadClass(webServiceEndpoint.getTieClassName());
    }

    /**
     * Encapsulate logic used to map invocation method to invocation info.
     * At present, we have two different maps, one for webservice invocation
     * info and one for everything else.  That might change in the future.
     */
    private InvocationInfo getInvocationInfo(EjbInvocation inv) {
        return inv.isWebService ? webServiceInvocationInfoMap.get(inv.method) : invocationInfoMap.get(inv.method);
    }

    private Throwable mapRemoteException(EjbInvocation inv) {
        final Throwable originalException = inv.exception;
        final boolean remoteClient = inv.isClientInterfaceAssignableToOneOf(java.rmi.Remote.class);
        final Throwable mappedException = mapRemoteException( //
                inv.exception, inv.invocationInfo.isAsynchronous(), inv.isBusinessInterface, remoteClient);
        _logger.log(Level.FINE, () -> "Mapped original remote exception " + originalException //
                + " to exception " + mappedException + " for " + inv);
        return mappedException;
    }


    private Throwable mapRemoteException(final Throwable remoteException, final boolean async,
                                         final boolean businessIface, boolean remoteClient) {
        final Throwable originalException = remoteException;
        // If it's an asnyc invocation and we're mapping an exception it
        // means this is the thread of execution.  The exception won't directly
        // flow over the wire as a remote exception from the orb's perspective.
        // If it's asychronous we know it's a remote business interface, not the
        // 2.x client view.
        if (async) {
            if (remoteClient) {
                final Throwable protocolException = protocolMgr.mapException(originalException);
                if (protocolException != originalException) {
                    return protocolException;
                }
                return wrapEjbExceptionToRemote(originalException, protocolException);
            }
            return mapLocal3xException(originalException);
        }
        // Synchronous invocation.  First let the protocol manager perform its mapping.
        final Throwable protocolException = protocolMgr.mapException(originalException);
        if (protocolException != originalException) {
            return protocolException;
        }
        if (businessIface) {
            // Wrap it up in a special exception so the
            // client can unwrap it and ensure that the client receives EJBException.
            return wrapEjbExceptionTInternal(originalException, protocolException);
        }
        return wrapEjbExceptionToRemote(originalException, protocolException);
    }

    private Throwable wrapEjbExceptionToRemote(final Throwable originalException, final Throwable protocolException) {
        if (originalException instanceof EJBException) {
            return new RemoteException(originalException.getMessage(), originalException);
        }
        return protocolException;
    }


    private Throwable wrapEjbExceptionTInternal(final Throwable originalException, final Throwable protocolException) {
        if (originalException instanceof EJBException) {
            return new InternalEJBContainerException(originalException.getMessage(), originalException);
        }
        return protocolException;
    }

    private Throwable mapLocal3xException(final Throwable t) {
        if (t instanceof TransactionRolledbackLocalException) {
            return new EJBTransactionRolledbackException(t.getMessage(), (Exception) t);
        }
        if (t instanceof TransactionRequiredLocalException) {
            final EJBTransactionRequiredException exception = new EJBTransactionRequiredException(t.getMessage());
            exception.initCause(t);
            return exception;
        }
        if (t instanceof NoSuchObjectLocalException) {
            return new NoSuchEJBException(t.getMessage(), (Exception) t);
        }
        if (t instanceof AccessLocalException) {
            final EJBAccessException exception = new EJBAccessException(t.getMessage());
            exception.initCause(t);
            return exception;
        }
        return t;
    }

    /**
     * Common code to handle EJB security manager authorization call.
     */
    @Override
    public boolean authorize(EjbInvocation inv) {

        // There are a few paths (e.g. authorizeLocalMethod,
        // authorizeRemoteMethod, Ejb endpoint pre-handler )
        // for which invocationInfo is not set.  We get better
        // performance with the security manager on subsequent
        // invocations of the same method if invocationInfo is
        // set on the invocation.  However, the authorization
        // does not depend on it being set.  So, try to set
        // invocationInfo but in this case don't treat it as
        // an error if it's not available.
        if (inv.invocationInfo == null) {
            inv.invocationInfo = getInvocationInfo(inv);
        }

        // Internal methods for 3.0 bean creation so there won't
        // be corresponding permissions in the security policy file.
        if ((inv.method.getDeclaringClass() == localBusinessHomeIntf)
                ||
                (inv.method.getDeclaringClass() == remoteBusinessHomeIntf)) {
            return true;
        }

        boolean authorized = securityManager.authorize(inv);
        if (!authorized) {
            if (inv.context != null) {
                // This means that an enterprise bean context was created
                // during the authorization call because of a callback from
                // a JACC enterprise bean handler. Since the invocation will
                // not proceed due to the authorization failure, we need
                // to release the enterprise bean context.
                releaseContext(inv);
            }
        }

        return authorized;
    }

    /**
     * Create an array of all methods in the standard EJB interfaces:
     * jakarta.ejb.EJB(Local){Home|Object} .
     */
    private void initializeEjbInterfaceMethods() throws Exception {
        ejbIntfMethods = new Method[EJB_INTF_METHODS_LENGTH];

        if (isRemote) {
            ejbIntfMethods[EJBHome_remove_Handle] = EJBHome.class.getMethod("remove", jakarta.ejb.Handle.class);
            ejbIntfMethods[EJBHome_remove_Pkey] = EJBHome.class.getMethod("remove", java.lang.Object.class);
            ejbIntfMethods[EJBHome_getEJBMetaData] = EJBHome.class.getMethod("getEJBMetaData", NO_PARAMS);
            ejbIntfMethods[EJBHome_getHomeHandle] = EJBHome.class.getMethod("getHomeHandle", NO_PARAMS);

            ejbIntfMethods[EJBObject_getEJBHome] = EJBObject.class.getMethod("getEJBHome", NO_PARAMS);
            ejbIntfMethods[EJBObject_getPrimaryKey] = EJBObject.class.getMethod("getPrimaryKey", NO_PARAMS);
            ejbIntfMethods[EJBObject_remove] = EJBObject.class.getMethod("remove", NO_PARAMS);
            ejbIntfMethods[EJBObject_getHandle] = EJBObject.class.getMethod("getHandle", NO_PARAMS);
            ejbIntfMethods[EJBObject_isIdentical] = EJBObject.class.getMethod("isIdentical", jakarta.ejb.EJBObject.class);

            if (isStatelessSession) {
                if (hasRemoteHomeView) {
                    ejbIntfMethods[EJBHome_create] = homeIntf.getMethod("create", NO_PARAMS);
                }
            }
        }

        if (isLocal) {
            ejbIntfMethods[EJBLocalHome_remove_Pkey] = EJBLocalHome.class.getMethod("remove", java.lang.Object.class);

            ejbIntfMethods[EJBLocalObject_getEJBLocalHome] = //
                    EJBLocalObject.class.getMethod("getEJBLocalHome", NO_PARAMS);
            ejbIntfMethods[EJBLocalObject_getPrimaryKey] = EJBLocalObject.class.getMethod("getPrimaryKey", NO_PARAMS);
            ejbIntfMethods[EJBLocalObject_remove] = EJBLocalObject.class.getMethod("remove", NO_PARAMS);
            ejbIntfMethods[EJBLocalObject_isIdentical] = //
                    EJBLocalObject.class.getMethod("isIdentical", jakarta.ejb.EJBLocalObject.class);

            if (isStatelessSession) {
                if (hasLocalHomeView) {
                    Method m = localHomeIntf.getMethod("create", NO_PARAMS);
                    ejbIntfMethods[EJBLocalHome_create] = m;
                }
            }
        }

    }

    protected void cancelTimers(Object key) {
        if (isTimedObject()) {
            // EJBTimerService should be accessed only if needed
            // not to cause it to be loaded if it's not used.
            if (hasPersistenceTimer) {
                EJBTimerService timerService = EJBTimerService.getPersistentTimerService();
                if (timerService != null) {
                    timerService.cancelTimersByKey(getContainerId(), key);
                }
            }
            if (hasNonPersistenceTimer) {
                EJBTimerService timerService = EJBTimerService.getNonPersistentTimerService();
                if (timerService != null) {
                    timerService.cancelTimersByKey(getContainerId(), key);
                }
            }
        }
    }

    private void stopTimers() {
        if (isTimedObject() && timersStarted) {
            if (hasPersistenceTimer) {
                EJBTimerService ejbTimerService = EJBTimerService.getPersistentTimerService();
                if (ejbTimerService != null) {
                    ejbTimerService.stopTimers(getContainerId());
                }
            }
            if (hasNonPersistenceTimer) {
                EJBTimerService ejbTimerService = EJBTimerService.getNonPersistentTimerService();
                if (ejbTimerService != null) {
                    ejbTimerService.stopTimers(getContainerId());
                }
            }
        }
    }

    protected boolean isEjbTimeoutMethod(Method m) {
        return schedules.containsKey(m) || m.equals(ejbTimeoutMethod);
    }

    // internal API, implemented in subclasses
    protected abstract EJBObjectImpl createEJBObjectImpl()
            throws CreateException, RemoteException;


    // Only applies to concrete session containers
    EJBObjectImpl createRemoteBusinessObjectImpl() throws CreateException, RemoteException {
        throw new EJBException("Internal ERROR: BaseContainer.createRemoteBusinessObject called");
    }


    // internal API, implemented in subclasses
    protected EJBLocalObjectImpl createEJBLocalObjectImpl() throws CreateException {
        throw new EJBException("Internal ERROR: BaseContainer.createEJBLocalObject called");
    }

    // Only implemented in Stateless , Stateful, and Singleton session containers
    EJBLocalObjectImpl createEJBLocalBusinessObjectImpl(boolean localBeanView) throws CreateException {
        throw new EJBException("Internal ERROR: BaseContainer.createEJBLocalBusinessObject called");
    }


    EJBLocalObjectImpl createEJBLocalBusinessObjectImpl(String clientIntf) throws CreateException {
        boolean useLocalBeanView = isLocalBeanClass(clientIntf);
        return createEJBLocalBusinessObjectImpl(useLocalBeanView);
    }

    /**
     * Called when a remote invocation arrives for an EJB.
     * Implemented in subclasses.
     */
    protected abstract EJBObjectImpl getEJBObjectImpl(byte[] streamKey);

    EJBObjectImpl getEJBRemoteBusinessObjectImpl(byte[] streamKey) {
        throw new EJBException(localStrings.getLocalString(
                "ejb.basecontainer_internal_error",
                "Internal ERROR: BaseContainer.{0} called",
                "getRemoteBusinessObjectImpl"));
    }

    protected EJBLocalObjectImpl getEJBLocalObjectImpl(Object key) {
        throw new EJBException(localStrings.getLocalString(
                "ejb.basecontainer_internal_error",
                "Internal ERROR: BaseContainer.{0} called",
                "getEJBLocalObjectImpl"));
    }

    EJBLocalObjectImpl getEJBLocalBusinessObjectImpl(Object key) {
        throw new EJBException(localStrings.getLocalString(
                "ejb.basecontainer_internal_error",
                "Internal ERROR: BaseContainer.{0} called",
                "getEJBLocalBusinessObjectImpl"));
    }

    EJBLocalObjectImpl getOptionalEJBLocalBusinessObjectImpl(Object key) {
        throw new EJBException(localStrings.getLocalString(
                "ejb.basecontainer_internal_error",
                "Internal ERROR: BaseContainer.{0} called",
                "getOptionalEJBLocalBusinessObjectImpl"));
    }

    /**
     * Check if the given EJBObject/LocalObject has been removed.
     *
     * @throws NoSuchObjectLocalException if the object has been removed.
     */
    protected void checkExists(EJBLocalRemoteObject ejbObj) {
        throw new EJBException(localStrings.getLocalString(
                "ejb.basecontainer_internal_error",
                "Internal ERROR: BaseContainer.{0} called",
                ("checkExists for bean " + ejbDescriptor.getName())));
    }

    protected final ComponentContext getContext(EjbInvocation inv)
            throws EJBException {

        return (inv.context == null) ? _getContext(inv) : inv.context;

    }

    protected final Object getInvocationKey(EjbInvocation inv) {
        return (inv.ejbObject == null) ? null : inv.ejbObject.getKey();
    }

    // internal API, implemented in subclasses
    protected abstract ComponentContext _getContext(EjbInvocation inv)
            throws EJBException;

    // internal API, implemented in subclasses
    protected abstract void releaseContext(EjbInvocation inv)
            throws EJBException;

    protected abstract boolean passivateEJB(ComponentContext context);

    // internal API, implemented in subclasses
    protected abstract void forceDestroyBean(EJBContextImpl sc)
            throws EJBException;

    protected abstract void removeBean(EJBLocalRemoteObject ejbo, Method removeMethod,
                                       boolean local)
            throws RemoveException, EJBException, RemoteException;

    // default implementation
    protected void authorizeLocalGetPrimaryKey(EJBLocalRemoteObject ejbObj) throws EJBException {
        throw new EJBException(localStrings.getLocalString(
                "containers.invalid_operation",
                "Invalid operation for Session EJBs."));
    }

    // default implementation
    protected void authorizeRemoteGetPrimaryKey(EJBLocalRemoteObject ejbObj) throws RemoteException {
        throw new RemoteException(localStrings.getLocalString(
                "containers.invalid_operation",
                "Invalid operation for Session EJBs."));
    }

    // default implementation
    protected Object invokeFindByPrimaryKey(Method method,
                                            EjbInvocation inv, Object[] args) throws Throwable {
        assertSupportedOption("invokeFindByPrimaryKey");
        return null;
    }

    // default implementation
    @Override
    public void removeBeanUnchecked(Object pkey) {
        assertSupportedOption("removeBeanUnchecked");
    }

    // default implementation
    @Override
    public void removeBeanUnchecked(EJBLocalObject bean) {
        assertSupportedOption("removeBeanUnchecked");
    }

    @Override
    public void preSelect() {
        assertSupportedOption("preSelect");
    }

    // default implementation
    @Override
    public EJBLocalObject getEJBLocalObjectForPrimaryKey(Object pkey, EJBContext ctx) {
        assertSupportedOption("getEJBLocalObjectForPrimaryKey(pkey, ctx)");
        return null;
    }

    // default implementation
    @Override
    public EJBLocalObject getEJBLocalObjectForPrimaryKey(Object pkey) {
        assertSupportedOption("getEJBLocalObjectForPrimaryKey");
        return null;
    }

    // default implementation
    @Override
    public EJBObject getEJBObjectForPrimaryKey(Object pkey) {
        assertSupportedOption("getEJBObjectForPrimaryKey");
        return null;
    }

    private void assertSupportedOption(String name) {
        throw new EJBException(localStrings.getLocalString(
                "ejb.entity_container_only", "{0} only works for EntityContainer", name));
    }

    // internal API, implemented in subclasses
    protected boolean isIdentical(EJBObjectImpl ejbo, EJBObject other)
            throws RemoteException {
        throw new EJBException(localStrings.getLocalString(
                "ejb.basecontainer_internal_error",
                "Internal ERROR: BaseContainer.{0} called",
                "isIdentical"));
    }

    /**
     * Called-back from security implementation through EjbInvocation
     * when a jacc policy provider wants an enterprise bean instance.
     */
    @Override
    public Object getJaccEjb(EjbInvocation inv) {
        Object bean = null;

        // Access to an enterprise bean instance is undefined for
        // anything but business method invocations through
        // Remote , Local, and ServiceEndpoint interfaces.
        if (((inv.invocationInfo != null) &&
                inv.invocationInfo.isBusinessMethod)
                ||
                inv.isWebService) {

            // In the typical case the context will not have been
            // set when the policy provider invokes this callback.
            // There are some cases where it is ok for it to have been
            // set, e.g. if the policy provider invokes the callback
            // twice within the same authorization decision.
            if (inv.context == null) {

                try {
                    inv.context = getContext(inv);
                    bean = inv.context.getEJB();
                    // NOTE : inv.ejb is not set here.  Post-invoke logic for
                    // BaseContainer and webservices uses the fact that
                    // inv.ejb is non-null as an indication that that
                    // BaseContainer.preInvoke() proceeded past a certain
                    // point, which affects which cleanup needs to be
                    // performed.  It would be better to have explicit
                    // state in the invocation that says which cleanup
                    // steps are necessary(e.g. for invocationMgr.postInvoke
                    // , postInvokeTx, etc) but I'm keeping the logic the
                    // same for now.   BaseContainer.authorize() will
                    // explicitly handle the case where a context was
                    // created as a result of this call and the
                    // authorization failed, which means the context needs
                    // be released.

                } catch (EJBException e) {
                    _logger.log(Level.WARNING, CONTEXT_FAILURE_JACC, logParams[0]);
                    _logger.log(Level.WARNING, "", e);
                }

            } else {
                bean = inv.context.getEJB();
            }
        }

        return bean;
    }

    @Override
    public void assertValidLocalObject(Object o) throws EJBException {
        boolean valid = false;
        String errorMsg = "";

        if ((o != null) && (o instanceof EJBLocalObject)) {
            // Given object is always the client view EJBLocalObject.
            // Use utility method to translate it to EJBLocalObjectImpl
            // so we handle both the generated and proxy case.
            EJBLocalObjectImpl ejbLocalObjImpl =
                    EJBLocalObjectImpl.toEJBLocalObjectImpl((EJBLocalObject) o);
            BaseContainer otherContainer =
                    (BaseContainer) ejbLocalObjImpl.getContainer();
            if (otherContainer.getContainerId() == getContainerId()) {
                valid = true;
            } else {
                errorMsg = "Local objects of ejb-name " + otherContainer.ejbDescriptor.getName() +
                        " and ejb-name " + ejbDescriptor.getName() +
                        " are from different containers";

            }
        } else {
            errorMsg = (o != null) ?
                    "Parameter instance of class '" + o.getClass().getName() +
                            "' is not a valid local interface instance for bean " +
                            ejbDescriptor.getName()
                    :
                    "A null parameter is not a valid local interface of bean " + ejbDescriptor.getName();
        }

        if (!valid) {
            throw new EJBException(errorMsg);
        }

    }

    /**
     * Asserts validity of RemoteHome objects.  This was defined for the
     * J2EE 1.4 implementation and is exposed through Container SPI.
     */
    @Override
    public void assertValidRemoteObject(Object o) throws EJBException {
        boolean valid = false;
        String errorMsg = "";
        Exception causeException = null;

        if ((o != null) && (o instanceof EJBObject)) {
            String className = o.getClass().getName();

            // Given object must be an instance of the remote stub class for
            // this ejb.
            if (hasRemoteHomeView) {
                try {
                    valid = remoteHomeRefFactory.hasSameContainerID((org.omg.CORBA.Object) o);
                } catch (Exception ex) {
                    causeException = ex;
                    errorMsg = "Parameter instance of class '" + className
                            + "' is not a valid remote interface instance for bean " + ejbDescriptor.getName();
                }
            } else {
                errorMsg = "Parameter instance of class '" + className
                        + "' is not a valid remote interface instance for bean " + ejbDescriptor.getName();

            }
        } else {
            errorMsg = (o == null)
                    ? "A null parameter is not a valid remote interface of bean " + ejbDescriptor.getName()
                    : "Parameter instance of class '" + o.getClass().getName()
                    + "' is not a valid remote interface instance for bean " + ejbDescriptor.getName();
        }

        if (!valid) {
            if (causeException == null) {
                throw new EJBException(errorMsg);
            } else {
                throw new EJBException(errorMsg, causeException);
            }
        }
    }

    protected final int getTxAttr(Method method, String methodIntf) throws EJBException {
        InvocationInfo invInfo = methodIntf.equals(EJB_WEB_SERVICE) //
                ? webServiceInvocationInfoMap.get(method)
                : invocationInfoMap.get(method);
        if (invInfo == null) {
            throw new EJBException("Transaction Attribute not found for method " + method);
        }
        return invInfo.txAttr;
    }

    // Get the transaction attribute for a method.
    // Note: this method object is of the remote/EJBHome interface
    // class, not the EJB class. (except for MDB's message listener
    // callback method or TimedObject ejbTimeout method)
    protected final int getTxAttr(EjbInvocation inv) throws EJBException {
        if (inv.transactionAttribute != TX_NOT_INITIALIZED) {
            return inv.transactionAttribute;
        }

        inv.transactionAttribute = getTxAttr(inv.method, inv.getMethodInterface());
        return inv.transactionAttribute;
    }


    // Check if a method is a business method.
    // Note: this method object is of the EJB's remote/home/local interfaces,
    // not the EJB class.
    final boolean isBusinessMethod(Method method) {
        Class methodClass = method.getDeclaringClass();

        // All methods on the Home/LocalHome & super-interfaces
        // are not business methods.
        // All methods on jakarta.ejb.EJBObject and EJBLocalObject
        // (e.g. remove) are not business methods.
        // All remaining methods are business methods

        if (isRemote) {
            if ((hasRemoteHomeView && (methodClass == homeIntf || methodClass.isAssignableFrom(homeIntf)))
                    || (hasRemoteBusinessView
                    && (methodClass == remoteBusinessHomeIntf || methodClass.isAssignableFrom(remoteBusinessHomeIntf)))
                    || methodClass == EJBObject.class) {
                return false;
            }
        }
        if (isLocal) {
            if ((hasLocalHomeView && (methodClass == localHomeIntf || methodClass.isAssignableFrom(localHomeIntf)))
                    || (hasLocalBusinessView
                    && (methodClass == localBusinessHomeIntf || methodClass.isAssignableFrom(localBusinessHomeIntf)))
                    || methodClass == EJBLocalObject.class) {
                return false;
            }
        }
        // NOTE : Web Service client view contains ONLY
        // business methods

        return true;
    }

    // Check if a method is a create / finder / home method.
    // Note: this method object is of the EJB's remote/home/local interfaces,
    // not the EJB class.
    protected boolean isCreateHomeFinder(Method method) {
        Class methodClass = method.getDeclaringClass();

        if (hasRemoteHomeView
                && methodClass.isAssignableFrom(homeIntf)
                && (methodClass != EJBHome.class)) {
            return true;
        }

        if (hasRemoteBusinessView
                && methodClass.isAssignableFrom(remoteBusinessHomeIntf)
                && (methodClass != EJBHome.class)) {
            return true;
        }

        if (hasLocalHomeView
                && methodClass.isAssignableFrom(localHomeIntf)
                && (methodClass != EJBLocalHome.class)) {
            return true;
        }

        if (hasLocalBusinessView
                && methodClass.isAssignableFrom(localBusinessHomeIntf)
                && (methodClass != EJBLocalHome.class)) {
            return true;
        }


        return false;
    }

    protected InvocationInfo addInvocationInfo(Method method, String methodIntf,
                                               Class originalIntf)
            throws EJBException {

        return addInvocationInfo(method, methodIntf, originalIntf, false, false);
    }

    private InvocationInfo addInvocationInfo(Method method, String methodIntf,
                                             Class originalIntf, boolean isEjbTimeout)
            throws EJBException {

        return addInvocationInfo(method, methodIntf, originalIntf, isEjbTimeout, false);
    }


    private InvocationInfo addInvocationInfo(Method method, String methodIntf, Class originalIntf, boolean isEjbTimeout,
                                             boolean optionalLocalBusView) throws EJBException {
        MethodDescriptor md = new MethodDescriptor(method, methodIntf);
        boolean flushEnabled = findFlushEnabledAttr(md);
        int txAttr = containerTransactionManager.findTxAttr(md);
        InvocationInfo info = createInvocationInfo(method, txAttr, flushEnabled, methodIntf, originalIntf);
        boolean isHomeIntf = methodIntf.equals(MethodDescriptor.EJB_HOME)
                || methodIntf.equals(MethodDescriptor.EJB_LOCALHOME);

        if (!isHomeIntf) {
            Method beanMethod = null;
            if (!isEjbTimeout) {
                try {
                    beanMethod = getEJBClass().getMethod(method.getName(), method.getParameterTypes());
                } catch (NoSuchMethodException nsmEx) {
                    //TODO
                }
            } else {
                // For a timeout it is the method
                beanMethod = method;
            }

            if (beanMethod != null) {
                // Can't set AroundInvoke/AroundTimeout chains here, but set up some
                // state on info object so it can be done right after InterceptorManager
                // is initialized.
                info.aroundMethod = beanMethod;
                info.isEjbTimeout = isEjbTimeout;
            }


            // Asynchronous method initialization
            if (isEligibleForAsync(originalIntf, methodIntf)) {

                Method targetMethod = optionalLocalBusView ? beanMethod : method;

                boolean isAsync = ((EjbSessionDescriptor) ejbDescriptor).
                        isAsynchronousMethod(targetMethod);

                if (isAsync) {

                    // Check return type
                    if (optionalLocalBusView && beanMethod != null) {

                        boolean beanMethodReturnTypeVoid = beanMethod.getReturnType().equals(Void.TYPE);
                        boolean beanMethodReturnTypeFuture = beanMethod.getReturnType().equals(Future.class);

                        if (!beanMethodReturnTypeVoid && !beanMethodReturnTypeFuture) {
                            throw new RuntimeException("Invalid no-interface view asynchronous method '"
                                    + beanMethod + "' for bean " + ejbDescriptor.getName() +
                                    ". Async method exposed through no-interface view must " +
                                    " have return type void or java.lang.concurrent.Future<V>");
                        }

                    } else {

                        // Use actual interface method instead of method from generated interface
                        Method intfMethod = null;
                        try {
                            intfMethod = originalIntf.getMethod(method.getName(), method.getParameterTypes());
                        } catch (NoSuchMethodException nsmEx) {
                            throw new RuntimeException("No matching async intf method for method '" + beanMethod
                                    + "' on bean " + ejbDescriptor.getName());
                        }

                        if (beanMethod == null) {

                            throw new RuntimeException("No matching bean class method for async method '" + intfMethod
                                    + "' on bean " + ejbDescriptor.getName());
                        }

                        boolean beanMethodReturnTypeVoid = beanMethod.getReturnType().equals(Void.TYPE);
                        boolean beanMethodReturnTypeFuture = beanMethod.getReturnType().equals(Future.class);

                        boolean intfMethodReturnTypeVoid = intfMethod.getReturnType().equals(Void.TYPE);
                        boolean intfMethodReturnTypeFuture = intfMethod.getReturnType().equals(Future.class);

                        boolean bothVoid = intfMethodReturnTypeVoid && beanMethodReturnTypeVoid;
                        boolean bothFuture = intfMethodReturnTypeFuture && beanMethodReturnTypeFuture;

                        if (!bothVoid && !bothFuture) {
                            throw new RuntimeException("Invalid asynchronous bean class / interface " //
                                    + "method signatures for bean " + ejbDescriptor.getName() //
                                    + ". beanMethod = '" + beanMethod + "' , interface method = '" + intfMethod + "'");
                        }
                    }

                    info.setIsAsynchronous(true);

                }
            }
        }

        if (methodIntf.equals(MethodDescriptor.EJB_WEB_SERVICE)) {
            webServiceInvocationInfoMap.put(method, info);
        } else {
            invocationInfoMap.put(method, info);
        }

        return info;
    }

    private boolean isEligibleForAsync(Class originalIntf, String methodIntf) {
        if (methodIntf.equals(MethodDescriptor.EJB_LOCAL) || methodIntf.equals(MethodDescriptor.EJB_REMOTE)) {
            // ejb1/2
            return !EJBObject.class.isAssignableFrom(originalIntf) //
                    && !EJBLocalObject.class.isAssignableFrom(originalIntf);
        }
        return false;
    }

    /**
     * Create invocation info for one method.
     *
     * @param originalIntf Leaf interface for the given view.  Not set for
     *                     methodIntf == bean.
     */
    private final InvocationInfo createInvocationInfo(Method method, int txAttr, boolean flushEnabled,
                                                      String methodIntf, Class originalIntf) throws EJBException {

        InvocationInfo invInfo = new InvocationInfo(method);
        invInfo.str_method_sig = EjbMonitoringUtils.stringify(method);

        invInfo.ejbName = ejbDescriptor.getName();
        invInfo.txAttr = txAttr;
        invInfo.methodIntf = methodIntf;

        invInfo.isBusinessMethod = isBusinessMethod(method);
        invInfo.isCreateHomeFinder = isCreateHomeFinder(method);

        invInfo.startsWithCreate = method.getName().startsWith("create");
        invInfo.startsWithFind = method.getName().startsWith("find");
        invInfo.startsWithRemove = method.getName().startsWith("remove");
        invInfo.startsWithFindByPrimaryKey =
                method.getName().startsWith("findByPrimaryKey");
        invInfo.flushEnabled = flushEnabled;

        if (methodIntf.equals(MethodDescriptor.EJB_LOCALHOME)) {
            if (method.getDeclaringClass() != EJBLocalHome.class) {
                setHomeTargetMethodInfo(invInfo, true);
            }
        } else if (methodIntf.equals(MethodDescriptor.EJB_HOME)) {
            if (method.getDeclaringClass() != EJBHome.class) {
                setHomeTargetMethodInfo(invInfo, false);
            }
        } else if (methodIntf.equals(MethodDescriptor.EJB_LOCAL)) {
            if (method.getDeclaringClass() != EJBLocalObject.class) {
                setEJBObjectTargetMethodInfo(invInfo, true, originalIntf);
            }
        } else if (methodIntf.equals(MethodDescriptor.EJB_REMOTE)) {
            if (method.getDeclaringClass() != EJBObject.class) {
                setEJBObjectTargetMethodInfo(invInfo, false, originalIntf);
            }
        }

        setConcurrencyInvInfo(method, methodIntf, invInfo);

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, invInfo.toString());
        }

        adjustInvocationInfo(invInfo, method, txAttr, flushEnabled, methodIntf, originalIntf);

        return invInfo;
    }

    // default impl
    protected void adjustInvocationInfo(InvocationInfo invInfo, Method method, int txAttr,
                                        boolean flushEnabled,
                                        String methodIntf,
                                        Class originalIntf)
            throws EJBException {
        // Nothing todo
    }

    private void setConcurrencyInvInfo(Method invInfoMethod, String methodIntf,
                                       InvocationInfo invInfo) {

        MethodLockInfo lockInfo = null;

        // Set READ/WRITE lock info.  Only applies to singleton beans.
        if (isSingleton) {
            EjbSessionDescriptor singletonDesc = (EjbSessionDescriptor) ejbDescriptor;
            List<MethodDescriptor> readLockMethods = singletonDesc.getReadLockMethods();
            List<MethodDescriptor> writeLockMethods = singletonDesc.getWriteLockMethods();

            DistributedLockType distLockType = singletonDesc.isClustered() ?
                    singletonDesc.getClusteredLockType() : DistributedLockType.LOCK_NONE;

            for (MethodDescriptor readLockMethodDesc : readLockMethods) {
                Method readLockMethod = readLockMethodDesc.getMethod(singletonDesc);
                if (implMethodMatchesInvInfoMethod(invInfoMethod, methodIntf, readLockMethod)) {

                    lockInfo = new MethodLockInfo();
                    switch (distLockType) {
                        case INHERIT: {
                            _logger.log(Level.WARNING, "Distributed Read Lock for Method {0} Upgraded to Read/Write", readLockMethod.getName());
                            lockInfo.setLockType(LockType.WRITE, true);
                            break;
                        }
                        case LOCK_NONE: {
                            lockInfo.setLockType(LockType.READ, false);
                        }
                    }
                    break;
                }
            }

            if (lockInfo == null) {
                for (MethodDescriptor writeLockMethodDesc : writeLockMethods) {
                    Method writeLockMethod = writeLockMethodDesc.getMethod(singletonDesc);
                    if (implMethodMatchesInvInfoMethod(invInfoMethod, methodIntf, writeLockMethod)) {

                        lockInfo = new MethodLockInfo();
                        lockInfo.setLockType(LockType.WRITE, distLockType != DistributedLockType.LOCK_NONE);
                        break;
                    }
                }
            }
        }

        // Set AccessTimeout info
        if (isSingleton || isStatefulSession) {

            EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor) ejbDescriptor;
            List<EjbSessionDescriptor.AccessTimeoutHolder> accessTimeoutInfo =
                    sessionDesc.getAccessTimeoutInfo();


            for (EjbSessionDescriptor.AccessTimeoutHolder accessTimeoutHolder : accessTimeoutInfo) {
                MethodDescriptor accessTimeoutMethodDesc = accessTimeoutHolder.method;
                Method accessTimeoutMethod = accessTimeoutMethodDesc.getMethod(sessionDesc);
                if (implMethodMatchesInvInfoMethod(invInfoMethod, methodIntf, accessTimeoutMethod)) {

                    if (lockInfo == null) {
                        lockInfo = new MethodLockInfo();
                    }

                    lockInfo.setTimeout(accessTimeoutHolder.value, accessTimeoutHolder.unit);

                    break;
                }
            }
        }

        if (lockInfo != null) {
            invInfo.methodLockInfo = lockInfo;
        }

    }

    private boolean implMethodMatchesInvInfoMethod(Method invInfoMethod, String methodIntf, Method implMethod) {

        boolean match = false;

        if (methodIntf.equals(MethodDescriptor.EJB_BEAN)) {
            // Declaring class must match in addition to signature
            match = (implMethod.getDeclaringClass().equals(invInfoMethod.getDeclaringClass())
                    && TypeUtil.sameMethodSignature(implMethod, invInfoMethod));

        } else {
            match = Modifier.isPublic(implMethod.getModifiers()) //
                    && Modifier.isPublic(invInfoMethod.getModifiers()) //
                    && TypeUtil.sameMethodSignature(implMethod, invInfoMethod);
        }

        return match;
    }

    protected InvocationInfo postProcessInvocationInfo(InvocationInfo invInfo) {
        return invInfo;
    }

    // default impl
    protected void adjustHomeTargetMethodInfo(InvocationInfo invInfo, String methodName, Class[] paramTypes)
            throws NoSuchMethodException {
        // Nothing todo
    }


    private void setHomeTargetMethodInfo(InvocationInfo invInfo, boolean isLocal) throws EJBException {

        Class homeIntfClazz = isLocal ? jakarta.ejb.EJBLocalHome.class : jakarta.ejb.EJBHome.class;

        Class methodClass = invInfo.method.getDeclaringClass();
        Class[] paramTypes = invInfo.method.getParameterTypes();
        String methodName = invInfo.method.getName();

        try {
            Method m = homeIntfClazz.getMethod(methodName, paramTypes);
            // Attempt to override Home/LocalHome method.  Print warning
            // but don't treat it as a fatal error. At runtime,
            // the EJBHome/EJBLocalHome method will be called.
            String[] params = {m.toString(), invInfo.method.toString()};
            _logger.log(Level.WARNING, ILLEGAL_EJB_INTERFACE_OVERRIDE, params);
            invInfo.ejbIntfOverride = true;
            return;
        } catch (NoSuchMethodException nsme) {
        }

        try {
            if (invInfo.startsWithCreate) {
                String extraCreateChars = methodName.substring("create".length());
                invInfo.targetMethod1 = ejbClass.getMethod("ejbCreate" + extraCreateChars, paramTypes);
                adjustHomeTargetMethodInfo(invInfo, methodName, paramTypes);

            } else if (invInfo.startsWithFind) {
                String extraFinderChars = methodName.substring("find".length());
                invInfo.targetMethod1 = ejbClass.getMethod("ejbFind" + extraFinderChars, paramTypes);
            } else {
                // HOME method
                String upperCasedName = methodName.substring(0, 1).toUpperCase(Locale.US) + methodName.substring(1);
                invInfo.targetMethod1 = ejbClass.getMethod("ejbHome" + upperCasedName, paramTypes);
            }
        } catch (NoSuchMethodException nsme) {

            if (methodClass == localBusinessHomeIntf || methodClass == remoteBusinessHomeIntf
                    || methodClass == ejbOptionalLocalBusinessHomeIntf || methodClass == GenericEJBHome.class) {
                // Not an error.  This is the case where the EJB 3.0
                // client view is being used and there is no corresponding
                // create/init method.
            } else if (isStatelessSession || isSingleton) {
                // Ignore.  Not an error.
                // EJB 3.0 Stateless session ejbCreate/PostConstruct
                // is decoupled from RemoteHome/LocalHome create().
            } else {

                Method initMethod = null;
                if (isSession) {
                    EjbSessionDescriptor sessionDesc =
                            (EjbSessionDescriptor) ejbDescriptor;

                    for (EjbInitInfo next : sessionDesc.getInitMethods()) {
                        MethodDescriptor beanMethod = next.getBeanMethod();
                        Method m = beanMethod.getMethod(sessionDesc);
                        if (next.getCreateMethod().getName().equals(methodName)
                                &&
                                TypeUtil.sameParamTypes(m, invInfo.method)) {
                            initMethod = m;
                            break;
                        }
                    }
                }

                if (initMethod != null) {
                    invInfo.targetMethod1 = initMethod;
                } else {
                    Object[] params = {logParams[0],
                            (isLocal ? "LocalHome" : "Home"),
                            invInfo.method.toString()};
                    _logger.log(Level.WARNING, BEAN_CLASS_METHOD_NOT_FOUND, params);
                    // Treat this as a warning instead of a fatal error.
                    // That matches the behavior of the generated code.
                    // Mark the target methods as null.  If this method is
                    // invoked at runtime it will be result in an exception
                    // from the invocation handlers.
                    invInfo.targetMethod1 = null;
                    invInfo.targetMethod2 = null;
                }
            }
        }
    }

    private void setEJBObjectTargetMethodInfo(InvocationInfo invInfo, boolean isLocal, Class originalIntf)
            throws EJBException {

        Class ejbIntfClazz = isLocal ? jakarta.ejb.EJBLocalObject.class : jakarta.ejb.EJBObject.class;

        Class[] paramTypes = invInfo.method.getParameterTypes();
        String methodName = invInfo.method.getName();

        // Check for 2.x Remote/Local bean attempts to override
        // EJBObject/EJBLocalObject operations.
        if (ejbIntfClazz.isAssignableFrom(originalIntf)) {
            try {
                Method m = ejbIntfClazz.getMethod(methodName, paramTypes);
                // Attempt to override EJBObject/EJBLocalObject method.  Print
                // warning but don't treat it as a fatal error. At runtime, the
                // EJBObject/EJBLocalObject method will be called.
                String[] params = {m.toString(), invInfo.method.toString()};
                _logger.log(Level.WARNING, ILLEGAL_EJB_INTERFACE_OVERRIDE, params);
                invInfo.ejbIntfOverride = true;
                return;
            } catch (NoSuchMethodException nsme) {
            }
        }

        try {
            invInfo.targetMethod1 = ejbClass.getMethod(methodName, paramTypes);

            if (isSession && isStatefulSession) {
                MethodDescriptor methodDesc = new MethodDescriptor(invInfo.targetMethod1, MethodDescriptor.EJB_BEAN);

                // Assign removal info to inv info.  If this method is not
                // an @Remove method, result will be null.
                invInfo.removalInfo = ((EjbSessionDescriptor) ejbDescriptor).
                        getRemovalInfo(methodDesc);
            }

        } catch (NoSuchMethodException nsme) {
            Object[] params = {logParams[0] + ":" + nsme.toString(),
                    (isLocal ? "Local" : "Remote"),
                    invInfo.method.toString()};
            _logger.log(Level.WARNING, BEAN_CLASS_METHOD_NOT_FOUND, params);
            // Treat this as a warning instead of a fatal error.
            // That matches the behavior of the generated code.
            // Mark the target methods as null.  If this method is
            // invoked at runtime it will be result in an exception from
            // the invocation handlers.
            invInfo.targetMethod1 = null;
        }
    }

    //Overridden in StatefulContainerOnly
    protected String[] getPre30LifecycleMethodNames() {
        // null to match AroundConstruct
        return new String[]{
                null, "ejbCreate", "ejbRemove", "ejbPassivate", "ejbActivate"};
    }

    private synchronized void initializeInterceptorManager() throws Exception {
        this.interceptorManager = new InterceptorManager( //
                _logger, this, lifecycleCallbackAnnotationClasses, getPre30LifecycleMethodNames());
        if (!pendingInterceptors.isEmpty()) {
            pendingInterceptors.forEach(this::registerSystemInterceptor);
            pendingInterceptors.clear();
        }
    }

    void registerSystemInterceptor(Object interceptor) {
        if (needSystemInterceptorProxy()) {
            synchronized (this) {
                if (interceptorManager == null) {
                    pendingInterceptors.add(interceptor);
                } else {
                    interceptorManager.registerRuntimeInterceptor(interceptor);
                }
            }
        }
    }

    private boolean needSystemInterceptorProxy() {

        // TODO only really needed if JAX-RS needs to dynamically register an
        // interceptor during web application init.  Can optimize this out
        // by checking for the existence of any JAX-RS resources in module.
        // Only applies to stateless and singleton session beans.
        return isSession && !isStatefulSession;

    }

    private void addSystemInterceptorProxy() {
        InterceptorDescriptor interceptorDesc = SystemInterceptorProxy.createInterceptorDesc();
        ejbDescriptor.addFrameworkInterceptor(interceptorDesc);
    }


    protected void addLocalRemoteInvocationInfo() throws Exception {
        if (isRemote) {
            if (hasRemoteHomeView) {
                // Process Remote intf
                Method[] methods = remoteIntf.getMethods();
                for (Method method : methods) {
                    addInvocationInfo(method, MethodDescriptor.EJB_REMOTE, remoteIntf);
                }

                // Process EJBHome intf
                methods = homeIntf.getMethods();
                for (Method method : methods) {
                    addInvocationInfo(method, MethodDescriptor.EJB_HOME, homeIntf);
                }
            }

            if (hasRemoteBusinessView) {
                for (RemoteBusinessIntfInfo next : remoteBusinessIntfInfo.values()) {
                    // Get methods from generated remote intf but pass
                    // actual business interface as original interface.
                    Method[] methods = next.generatedRemoteIntf.getMethods();
                    for (Method method : methods) {
                        addInvocationInfo(method, MethodDescriptor.EJB_REMOTE, next.remoteBusinessIntf);
                    }
                }

                // Process internal EJB RemoteBusinessHome intf
                Method[] methods = remoteBusinessHomeIntf.getMethods();
                for (Method method : methods) {
                    addInvocationInfo(method, MethodDescriptor.EJB_HOME, remoteBusinessHomeIntf);
                }
            }
        }

        if (isLocal) {
            if (hasLocalHomeView) {
                // Process Local interface
                Method[] methods = localIntf.getMethods();
                for (Method method : methods) {
                    InvocationInfo info = addInvocationInfo(method, MethodDescriptor.EJB_LOCAL, localIntf);
                    postProcessInvocationInfo(info);
                }

                // Process LocalHome interface
                methods = localHomeIntf.getMethods();
                for (Method method : methods) {
                    addInvocationInfo(method, MethodDescriptor.EJB_LOCALHOME, localHomeIntf);
                }
            }

            if (hasLocalBusinessView) {
                // Process Local Business interfaces
                for (Class localBusinessIntf : localBusinessIntfs) {
                    Method[] methods = localBusinessIntf.getMethods();
                    for (Method method : methods) {
                        addInvocationInfo(method, MethodDescriptor.EJB_LOCAL, localBusinessIntf);
                    }
                }

                // Process (internal) Local Business Home interface
                Method[] methods = localBusinessHomeIntf.getMethods();
                for (Method method : methods) {
                    addInvocationInfo(method, MethodDescriptor.EJB_LOCALHOME, localBusinessHomeIntf);
                }
            }

            if (hasOptionalLocalBusinessView) {
                // Process generated Optional Local Business interface
                String optClassName = EJBUtils.getGeneratedOptionalInterfaceName(ejbClass.getName());
                ejbGeneratedOptionalLocalBusinessIntfClass = optIntfClassLoader.loadClass(optClassName);
                Method[] methods = ejbGeneratedOptionalLocalBusinessIntfClass.getMethods();
                for (Method method : methods) {
                    addInvocationInfo( //
                            method, MethodDescriptor.EJB_LOCAL, ejbGeneratedOptionalLocalBusinessIntfClass, false, true);
                }

                // Process generated Optional Local Business interface
                Method[] optHomeMethods = ejbOptionalLocalBusinessHomeIntf.getMethods();
                for (Method method : optHomeMethods) {
                    addInvocationInfo(method, MethodDescriptor.EJB_LOCALHOME, ejbOptionalLocalBusinessHomeIntf);
                }
            }

            if (!hasLocalHomeView) {
                // Add dummy local business interface remove method so that internal
                // container remove operations will work. (needed for internal 299 contract)
                addInvocationInfo( //
                        this.ejbIntfMethods[EJBLocalObject_remove], MethodDescriptor.EJB_LOCAL,
                        jakarta.ejb.EJBLocalObject.class);
            }
        }
    }

    private void addWSOrTimedObjectInvocationInfo() throws Exception {

        if (isWebServiceEndpoint) {
            // Process Service Endpoint interface
            Method[] methods = webServiceEndpointIntf.getMethods();
            for (Method method : methods) {
                addInvocationInfo(method, MethodDescriptor.EJB_WEB_SERVICE,
                        webServiceEndpointIntf);
            }
        }

        if (isTimedObject()) {
            if (ejbTimeoutMethod != null) {
                processTxAttrForScheduledTimeoutMethod(ejbTimeoutMethod);
            }

            for (Map.Entry<Method, List<ScheduledTimerDescriptor>> entry : schedules.entrySet()) {
                processTxAttrForScheduledTimeoutMethod(entry.getKey());
            }
        }
    }

    private void initializeInvocationInfo() throws Exception {
        // Create a map implementation that is optimized
        // for method lookups.  This is especially important for local
        // invocations through dynamic proxies, where the overhead of the
        // the (method -> invocationInfo) lookup has been measured to be
        // 6X greater than the overhead of the reflective call itself.
        proxyInvocationInfoMap = new MethodMap(invocationInfoMap);


        // Store InvocationInfo by standard ejb interface method type
        // to avoid an invocation info map lookup during authorizeLocalMethod
        // and authorizeRemoteMethod.
        ejbIntfMethodInfo = new InvocationInfo[EJB_INTF_METHODS_LENGTH];
        for (int i = 0; i < ejbIntfMethods.length; i++) {
            Method m = ejbIntfMethods[i];
            ejbIntfMethodInfo[i] = invocationInfoMap.get(m);
        }
    }

    /**
     * Validate transaction attribute value. Allow subclasses to add their own validation.
     */
    protected void validateTxAttr(MethodDescriptor md, int txAttr) throws EJBException {
    }

    /**
     * Verify transaction attribute on the timeout or schedule method and process
     * this method if it's correct.
     */
    private void processTxAttrForScheduledTimeoutMethod(Method m) {
        int txAttr = containerTransactionManager.findTxAttr(new MethodDescriptor(m, MethodDescriptor.TIMER_METHOD));
        if (isBeanManagedTran || txAttr == TX_REQUIRED || txAttr == TX_REQUIRES_NEW || txAttr == TX_NOT_SUPPORTED) {
            addInvocationInfo(m, MethodDescriptor.TIMER_METHOD, null, true);
        } else {
            throw new EJBException("Timeout method " + m + "must have TX attribute of "
                    + "TX_REQUIRES_NEW or TX_REQUIRED or TX_NOT_SUPPORTED for ejb " + ejbDescriptor.getName());
        }
    }

    // Check if the user has enabled flush at end of method flag
    // This is only used during container initialization and set into
    // the invocation info object. This method is over-riden in the
    // EntityContainer.
    protected boolean findFlushEnabledAttr(MethodDescriptor md) {

        //Get the flushMethodDescriptor and then find if flush has been
        //enabled for this method
        boolean flushEnabled =
                ejbDescriptor.getIASEjbExtraDescriptors().isFlushEnabledFor(md);

        return flushEnabled;
    }

    // default impl
    protected void addProxyInterfacesSetClass(Set proxyInterfacesSet, boolean local) {
        // no-op
    }

    // default impl
    protected EJBHomeInvocationHandler getEJBHomeInvocationHandler(Class homeIntfClass) throws Exception {
        return new EJBHomeInvocationHandler(ejbDescriptor, homeIntfClass);
    }

    private EJBHomeImpl instantiateEJBHomeImpl() throws Exception {
        EJBHomeInvocationHandler handler = getEJBHomeInvocationHandler(homeIntf);
        handler.setMethodMap(proxyInvocationInfoMap);

        EJBHomeImpl homeImpl = handler;

        // Maintain insertion order
        Set proxyInterfacesSet = new LinkedHashSet();

        addProxyInterfacesSetClass(proxyInterfacesSet, false);

        proxyInterfacesSet.add(homeIntf);

        Class[] proxyInterfaces = (Class[])
                proxyInterfacesSet.toArray(new Class[proxyInterfacesSet.size()]);

        try {
            EJBHome ejbHomeProxy = (EJBHome) Proxy.newProxyInstance(loader, proxyInterfaces, handler);
            handler.setProxy(ejbHomeProxy);
        } catch (ClassCastException e) {
            String msg = localStrings.getLocalString("ejb.basecontainer_invalid_home_interface",
                    "Home interface [{0}] is invalid since it does not extend jakarta.ejb.EJBHome.", homeIntf);
            throw new IllegalArgumentException(msg, e);
        }

        homeImpl.setContainer(this);
        return homeImpl;
    }

    private EJBHomeImpl instantiateEJBRemoteBusinessHomeImpl() throws Exception {
        EJBHomeInvocationHandler handler = getEJBHomeInvocationHandler(remoteBusinessHomeIntf);
        handler.setMethodMap(proxyInvocationInfoMap);

        EJBHomeImpl remoteBusinessHomeImpl = handler;

        EJBHome ejbRemoteBusinessHomeProxy = (EJBHome) Proxy
                .newProxyInstance(loader, new Class[]{remoteBusinessHomeIntf}, handler);

        handler.setProxy(ejbRemoteBusinessHomeProxy);

        remoteBusinessHomeImpl.setContainer(this);
        return remoteBusinessHomeImpl;
    }

    protected EjbInvocation createEjbInvocation() {
        return invFactory.create();
    }

    protected EjbInvocation createEjbInvocation(Object ejb, ComponentContext context) {
        return invFactory.create(ejb, context);
    }

    // default impl
    protected EJBLocalHomeInvocationHandler getEJBLocalHomeInvocationHandler(Class homeIntfClass) throws Exception {
        return new EJBLocalHomeInvocationHandler(ejbDescriptor, homeIntfClass);
    }

    private EJBLocalHomeImpl instantiateEJBLocalHomeImpl()
            throws Exception {

        // LocalHome impl
        EJBLocalHomeInvocationHandler invHandler = getEJBLocalHomeInvocationHandler(localHomeIntf);
        invHandler.setMethodMap(proxyInvocationInfoMap);

        EJBLocalHomeImpl homeImpl = invHandler;

        // Maintain insertion order
        Set proxyInterfacesSet = new LinkedHashSet();

        proxyInterfacesSet.add(IndirectlySerializable.class);
        addProxyInterfacesSetClass(proxyInterfacesSet, true);
        proxyInterfacesSet.add(localHomeIntf);

        Class[] proxyInterfaces = (Class[])
                proxyInterfacesSet.toArray(new Class[proxyInterfacesSet.size()]);

        // Client's EJBLocalHome object
        try {
            EJBLocalHome proxy = (EJBLocalHome) Proxy.newProxyInstance(loader, proxyInterfaces, invHandler);
            invHandler.setProxy(proxy);
        } catch (ClassCastException e) {
            String msg = localStrings.getLocalString("ejb.basecontainer_invalid_local_home_interface",
                    "Local home interface [{0}] is invalid since it does not extend jakarta.ejb.EJBLocalHome.", localHomeIntf);
            throw new IllegalArgumentException(msg, e);
        }

        homeImpl.setContainer(this);
        return homeImpl;
    }

    private EJBLocalHomeImpl instantiateEJBLocalBusinessHomeImpl()
            throws Exception {

        EJBLocalHomeInvocationHandler invHandler = getEJBLocalHomeInvocationHandler(localBusinessHomeIntf);
        invHandler.setMethodMap(proxyInvocationInfoMap);

        EJBLocalHomeImpl homeImpl = invHandler;

        EJBLocalHome proxy = (EJBLocalHome) Proxy.newProxyInstance
                (loader, new Class[]{IndirectlySerializable.class,
                        localBusinessHomeIntf}, invHandler);

        invHandler.setProxy(proxy);

        homeImpl.setContainer(this);

        return homeImpl;
    }


    private EJBLocalHomeImpl instantiateEJBOptionalLocalBusinessHomeImpl()
            throws Exception {

        EJBLocalHomeInvocationHandler invHandler = getEJBLocalHomeInvocationHandler(localBusinessHomeIntf);
        invHandler.setMethodMap(proxyInvocationInfoMap);

        EJBLocalHomeImpl homeImpl = invHandler;

        EJBLocalHome proxy = (EJBLocalHome) Proxy.newProxyInstance
                (loader, new Class[]{IndirectlySerializable.class,
                        ejbOptionalLocalBusinessHomeIntf}, invHandler);

        invHandler.setProxy(proxy);

        homeImpl.setContainer(this);

        return homeImpl;
    }

    protected EJBLocalObjectImpl instantiateEJBLocalObjectImpl()
            throws Exception {
        return instantiateEJBLocalObjectImpl(null);
    }

    protected EJBLocalObjectImpl instantiateEJBLocalObjectImpl(Object key)
            throws Exception {
        EJBLocalObjectImpl localObjImpl = null;
        EJBLocalObjectInvocationHandler handler =
                new EJBLocalObjectInvocationHandler(proxyInvocationInfoMap,
                        localIntf);
        localObjImpl = handler;

        try {
            EJBLocalObject localObjectProxy = (EJBLocalObject) Proxy.newProxyInstance(loader,
                    new Class[]{IndirectlySerializable.class, localIntf}, handler);
            handler.setProxy(localObjectProxy);
        } catch (ClassCastException e) {
            String msg = localStrings.getLocalString("ejb.basecontainer_invalid_local_interface",
                    "Local component interface [{0}] is invalid since it does not extend jakarta.ejb.EJBLocalObject.", localIntf);
            throw new IllegalArgumentException(msg, e);
        }

        localObjImpl.setContainer(this);
        if (key != null) {
            // associate the EJBObject with the key
            localObjImpl.setKey(key);
        }

        return localObjImpl;
    }

    protected EJBLocalObjectImpl instantiateEJBLocalBusinessObjectImpl()
            throws Exception {

        EJBLocalObjectInvocationHandler handler =
                new EJBLocalObjectInvocationHandler(proxyInvocationInfoMap, false);

        EJBLocalObjectImpl localBusinessObjImpl = handler;

        Proxy.newProxyInstance(loader, ejbLocalBusinessProxyInterfaces, handler);

        localBusinessObjImpl.setContainer(this);

        for (Class businessIntfClass : localBusinessIntfs) {
            EJBLocalObjectInvocationHandlerDelegate delegate =
                    new EJBLocalObjectInvocationHandlerDelegate(
                            businessIntfClass, getContainerId(), handler);
            Proxy proxy = (Proxy) Proxy.newProxyInstance(
                    loader, new Class[]{IndirectlySerializable.class,
                            businessIntfClass}, delegate);
            localBusinessObjImpl.mapClientObject(businessIntfClass.getName(),
                    proxy);
        }
        return localBusinessObjImpl;
    }

    protected EJBLocalObjectImpl instantiateOptionalEJBLocalBusinessObjectImpl()
            throws Exception {
        EJBLocalObjectInvocationHandler handler =
                new EJBLocalObjectInvocationHandler(proxyInvocationInfoMap, true);

        EJBLocalObjectImpl localBusinessObjImpl = handler;

        Proxy.newProxyInstance(loader, ejbOptionalLocalBusinessHomeProxyInterfaces, handler);

        localBusinessObjImpl.setContainer(this);

        Class businessIntfClass = ejbGeneratedOptionalLocalBusinessIntfClass;
        EJBLocalObjectInvocationHandlerDelegate delegate = new EJBLocalObjectInvocationHandlerDelegate(
                businessIntfClass, getContainerId(), handler);
        Proxy proxy = (Proxy) Proxy.newProxyInstance(
                loader, new Class[]{IndirectlySerializable.class, businessIntfClass}, delegate);

        String beanSubClassName = ejbGeneratedOptionalLocalBusinessIntfClass.getName() + "__Bean__";

        optIntfClassLoader.generateOptionalLocalInterfaceSubClass(ejbClass, beanSubClassName, ejbGeneratedOptionalLocalBusinessIntfClass);

        optIntfClassLoader.loadClass(ejbGeneratedOptionalLocalBusinessIntfClass.getName());

        Class subClass = optIntfClassLoader.loadClass(beanSubClassName);
        OptionalLocalInterfaceProvider provider = (OptionalLocalInterfaceProvider) subClass.getConstructor().newInstance();
        provider.setOptionalLocalIntfProxy(proxy);
        localBusinessObjImpl.mapClientObject(ejbClass.getName(), provider);

        return localBusinessObjImpl;
    }

    protected EJBObjectImpl instantiateEJBObjectImpl() throws Exception {
        return instantiateEJBObjectImpl(null, null);
    }

    protected EJBObjectImpl instantiateEJBObjectImpl(EJBObject ejbStub, Object key) throws Exception {
        EJBObjectInvocationHandler handler =
                new EJBObjectInvocationHandler(proxyInvocationInfoMap,
                        remoteIntf);
        EJBObjectImpl ejbObjImpl = handler;

        try {
            EJBObject ejbObjectProxy = (EJBObject) Proxy.newProxyInstance(loader, new Class[]{remoteIntf}, handler);
            this.ejbObjectProxyClass = ejbObjectProxyClass == null ? ejbObjectProxy.getClass() : ejbObjectProxyClass;
            handler.setEJBObject(ejbObjectProxy);
        } catch (ClassCastException e) {
            String msg = localStrings.getLocalString("ejb.basecontainer_invalid_remote_interface",
                    "Remote component interface [{0}] is invalid since it does not extend jakarta.ejb.EJBObject.", remoteIntf);
            throw new IllegalArgumentException(msg, e);
        }

        if (ejbStub != null) {
            // associate the EJBObject with the stub
            ejbObjImpl.setStub(ejbStub);
        }

        if (key != null) {
            // associate the EJBObject with the key
            ejbObjImpl.setKey(key);
        }

        ejbObjImpl.setContainer(this);
        return ejbObjImpl;
    }

    protected EJBObjectImpl instantiateRemoteBusinessObjectImpl() throws Exception {

        // There is one EJBObjectImpl instance, which is an instance of
        // the handler.   That handler instance is shared by the dynamic
        // proxy for each remote business interface.  We need to create a
        // different proxy for each remote business interface because
        // otherwise the target object given to the orb will be invalid
        // if the same method happens to be declared on multiple remote
        // business interfaces.
        EJBObjectInvocationHandler handler = new EJBObjectInvocationHandler(proxyInvocationInfoMap);
        EJBObjectImpl ejbBusinessObjImpl = handler;
        for (RemoteBusinessIntfInfo next : remoteBusinessIntfInfo.values()) {

            EJBObjectInvocationHandlerDelegate delegate = //
                    new EJBObjectInvocationHandlerDelegate(next.remoteBusinessIntf, handler);

            Remote ejbBusinessObjectProxy = (Remote) Proxy.newProxyInstance(loader, new Class[]{next.generatedRemoteIntf}, delegate);
            next.proxyClass = ejbBusinessObjectProxy.getClass();
            ejbBusinessObjImpl.setEJBObject(next.generatedRemoteIntf.getName(), ejbBusinessObjectProxy);
        }

        ejbBusinessObjImpl.setContainer(this);
        return ejbBusinessObjImpl;
    }

    // default implementation
    public boolean scanForEjbCreateMethod() {
        return false;
    }

    // default implementation
    @Override
    public void postCreate(EjbInvocation inv, Object primaryKey) throws CreateException {
        throw new EJBException("Internal error");
    }

    // default implementation
    @Override
    public Object postFind(EjbInvocation inv, Object primaryKeys, Object[] findParams) throws FinderException {
        throw new EJBException("Internal error");
    }


    private void setupEnvironment() throws javax.naming.NamingException {
        // call the NamingManager to setup the java:comp/env namespace
        // for this EJB.

        ComponentEnvManager envManager = ejbContainerUtilImpl.getComponentEnvManager();
        componentId = envManager.bindToComponentNamespace(ejbDescriptor);
        invFactory = new EjbInvocationFactory(componentId, this);
        ejbContainerUtilImpl.registerContainer(this);
        // create envProps object to be returned from EJBContext.getEnvironment
        Set env = ejbDescriptor.getEnvironmentProperties();
        SafeProperties safeProps = new SafeProperties();
        safeProps.copy(env);
        envProps = safeProps;
    }

    /**
     * Called from NamingManagerImpl during java:comp/env lookup.
     */
    @Override
    public String getComponentId() {
        return componentId;
    }

    /**
     * Called after all the components in the container's application
     * have deployed successfully.
     */
    @Override
    public void startApplication(boolean deploy) {
        _logger.log(FINE, "Application deployment successful : {0}", this);

        // By now all existing timers should have been restored.
        if (isTimedObject_) {
            scheduleIds.clear();
            // EJBTimerService should be accessed only if needed
            // not to cause it to be loaded if it's not used.
            if (hasPersistenceTimer) {
                Map<TimerPrimaryKey, Method> result;
                EJBTimerService timerService = EJBTimerService.getPersistentTimerService();
                if (timerService != null) {
                    boolean deploy0 = deploy;  //avoid modifying param
                    if (deploy0 && ejbDescriptor.getApplication().getKeepStateResolved()) {
                        deploy0 = false;
                        _logger.log(Level.INFO, KEEPSTATE_IS_TRUE);
                    }
                    result = timerService.recoverAndCreateSchedules(
                            getContainerId(),
                            getApplicationId(),
                            getPersistentSchedules(),
                            deploy0
                    );
                    scheduleIds.putAll(result);
                } else {
                    throw new RuntimeException("EJB Timer Service is not available");
                }
            }
            if (hasNonPersistenceTimer) {
                Map<TimerPrimaryKey, Method> result;
                EJBTimerService timerService = EJBTimerService.getNonPersistentTimerService();
                if (timerService != null) {
                    boolean deploy0 = deploy;
                    if (deploy0 && ejbDescriptor.getApplication().getKeepStateResolved()) {
                        deploy0 = false;
                        _logger.log(Level.INFO, KEEPSTATE_IS_TRUE);
                    }
                    result = timerService.recoverAndCreateSchedules(
                            getContainerId(),
                            getApplicationId(),
                            getNonPersistentSchedules(),
                            deploy0
                    );
                    scheduleIds.putAll(result);
                } else {
                    throw new RuntimeException("EJB Timer Service is not available");
                }
            }
        }

        setStartedState();
    }

    private Map<Method, List<ScheduledTimerDescriptor>> getPersistentSchedules() {
        Map<Method, List<ScheduledTimerDescriptor>> persistentSchedules = new HashMap<>();
        for (Entry<Method, List<ScheduledTimerDescriptor>> entry : schedules.entrySet()) {
            Method method = entry.getKey();
            List<ScheduledTimerDescriptor> descriptors
                    = entry.getValue()
                    .stream()
                    .filter(descriptor -> descriptor.getPersistent())
                    .collect(toList());
            if (!descriptors.isEmpty()) {
                persistentSchedules.put(method, descriptors);
            }
        }
        return persistentSchedules;
    }

    private Map<Method, List<ScheduledTimerDescriptor>> getNonPersistentSchedules() {
        Map<Method, List<ScheduledTimerDescriptor>> nonPersistentSchedules = new HashMap<>();
        for (Entry<Method, List<ScheduledTimerDescriptor>> entry : schedules.entrySet()) {
            Method method = entry.getKey();
            List<ScheduledTimerDescriptor> descriptors
                    = entry.getValue()
                    .stream()
                    .filter(descriptor -> !descriptor.getPersistent())
                    .collect(toList());
            if (!descriptors.isEmpty()) {
                nonPersistentSchedules.put(method, descriptors);
            }
        }
        return nonPersistentSchedules;
    }

    /**
     *
     * @param timerState
     * @param timerService
     * @return
     * @throws java.lang.Exception
     */
    protected boolean callEJBTimeout(RuntimeTimerState timerState,
                                     EJBTimerService timerService) throws Exception {

        boolean redeliver = false;

        if (containerState != CONTAINER_STARTED) {
            throw new EJBException("Attempt to invoke when container is in "
                    + containerStateToString(containerState));
        }

        EjbInvocation inv = createEjbInvocation();

        inv.isTimerCallback = true;

        // Let preInvoke do tx attribute lookup.
        inv.transactionAttribute = Container.TX_NOT_INITIALIZED;

        inv.method = getTimeoutMethod(timerState);
        inv.beanMethod = inv.method;

        ClassLoader originalClassLoader = null;
        try {
            prepareEjbTimeoutParams(inv, timerState, timerService);

            // Delegate to subclass for i.ejbObject / i.isLocal setup.
            doTimerInvocationInit(inv, timerState.getTimedObjectPrimaryKey());

            originalClassLoader = Utility.setContextClassLoader(loader);

            preInvoke(inv);

            // AroundTimeout interceptors will be checked for timeout methods
            intercept(inv);

            if (!isBeanManagedTran && (transactionManager.getStatus() ==
                    Status.STATUS_MARKED_ROLLBACK)) {
                redeliver = true;
                _logger.log(Level.FINE, "ejbTimeout called setRollbackOnly");
            }

        } catch (InvocationTargetException ite) {
            // A runtime exception thrown from ejbTimeout, independent of
            // its transactional setting(CMT, BMT, etc.), should result in
            // a redelivery attempt.  The instance that threw the runtime
            // exception will be destroyed, as per the EJB spec.
            redeliver = true;
            inv.exception = ite.getCause();
            _logger.log(Level.FINE, "ejbTimeout threw Runtime exception",
                    inv.exception);
        } catch (Throwable c) {
            redeliver = true;
            _logger.log(Level.FINE, "Exception while processing ejbTimeout", c);
            inv.exception = c;
        } finally {

            // Only call postEjbTimeout if there are no errors so far.
            if (!redeliver) {
                boolean success = postEjbTimeout(timerState, timerService);
                redeliver = !success;
            }

            postInvoke(inv);

            // If transaction commit fails, set redeliver flag.
            if ((redeliver == false) && (inv.exception != null)) {
                redeliver = true;
            }

            if (originalClassLoader != null) {
                Utility.setContextClassLoader(originalClassLoader);
            }

        }

        return redeliver;
    }

    protected Method getTimeoutMethod(RuntimeTimerState timerState) {
        Method m = scheduleIds.get(timerState.getTimerId());
        return (m != null) ? m : ejbTimeoutMethod;
    }

    protected boolean postEjbTimeout(RuntimeTimerState timerState, EJBTimerService timerService) {
        return timerService.postEjbTimeout(timerState.getTimerId());
    }

    protected void prepareEjbTimeoutParams(EjbInvocation inv, RuntimeTimerState timerState,
                                           EJBTimerService timerService) {
        // Create a TimerWrapper for AroundTimeout and as a method argument.
        jakarta.ejb.Timer timer = new TimerWrapper(timerState.getTimerId(),
                timerService);
        inv.timer = timer;

        if (inv.method.getParameterTypes().length == 1) {
            Object[] args = {timer};
            inv.methodParams = args;
        } else {
            inv.methodParams = null;
        }
    }

    public final void onEnteringContainer() {
        ejbProbeNotifier.ejbContainerEnteringEvent(getContainerId(),
                containerInfo.appName, containerInfo.modName,
                containerInfo.ejbName);
        enteringEjbContainer();
        //callFlowAgent.startTime(ContainerTypeOrApplicationType.EJB_CONTAINER);
    }

    public final void onLeavingContainer() {
        ejbProbeNotifier.ejbContainerLeavingEvent(getContainerId(),
                containerInfo.appName, containerInfo.modName,
                containerInfo.ejbName);
        leavingEjbContainer();
        //callFlowAgent.endTime();
    }

    private void enteringEjbContainer() {
        if (interceptors == null) {
            return;
        }
        for (EjbContainerInterceptor interceptor : interceptors) {
            try {
                interceptor.preInvoke(ejbDescriptor);
            } catch (Throwable th) {
                _logger.log(Level.SEVERE, INTERNAL_ERROR, th);
            }
        }
    }

    private void leavingEjbContainer() {
        if (interceptors == null) {
            return;
        }
        for (EjbContainerInterceptor interceptor : interceptors) {
            try {
                interceptor.postInvoke(ejbDescriptor);
            } catch (Throwable th) {
                _logger.log(Level.SEVERE, INTERNAL_ERROR, th);
            }
        }
    }

    private void initEjbInterceptors() {
        try {
            ServiceLocator services = ejbContainerUtilImpl.getServices();
            interceptors = services.getAllServices(EjbContainerInterceptor.class);
        } catch (Throwable th) {
            _logger.log(Level.SEVERE, FAILED_TO_INITIALIZE_INTERCEPTOR, th);
        }
    }

    final void onEjbMethodStart(int methodIndex) {
        InvocationInfo info = ejbIntfMethodInfo[methodIndex];
        if (info != null) {
            onEjbMethodStart(info.str_method_sig);
        }
    }

    final void onEjbMethodEnd(int methodIndex, Throwable th) {
        InvocationInfo info = ejbIntfMethodInfo[methodIndex];
        if (info != null) {
            onEjbMethodEnd(info.str_method_sig, th);
        }
    }

    final void onEjbMethodStart(String method_sig) {
        ejbProbeNotifier.ejbMethodStartEvent(getContainerId(),
                callFlowInfo.getApplicationName(),
                callFlowInfo.getModuleName(),
                callFlowInfo.getComponentName(),
                method_sig);

        addEjbMethodTraceLog(callFlowInfo, true);
    }

    final void onEjbMethodEnd(String method_sig, Throwable th) {
        ejbProbeNotifier.ejbMethodEndEvent(getContainerId(),
                callFlowInfo.getApplicationName(),
                callFlowInfo.getModuleName(),
                callFlowInfo.getComponentName(),
                th,
                method_sig);
        addEjbMethodTraceLog(callFlowInfo, false);
    }

    private void addEjbMethodTraceLog(CallFlowInfo info, boolean callEnter) {
        if (openTracingService.isEnabled()) {
            Tracer tracer = openTracingService.getTracer(openTracingService.getApplicationName(invocationManager));
            RequestTraceSpanLog spanLog = constructEjbMethodSpanLog(info, callEnter);

            if (tracer != null) {
                Span span = tracer.activeSpan();

                if (span != null) {
                    span.log(spanLog.getTimeMillis(), spanLog.getLogEntries());
                } else {
                    // Traces started in the pre-OpenTracing style won't have an active span, so just attempt to add as
                    // is to thread local trace if there is one
                    requestTracingService.addSpanLog(spanLog);
                }
            } else {
                // If we couldn't get a tracer here, it's because we couldn't get a name from the invocation manager.
                // In such a case, just try to add the span log to the currently active thread local request trace
                requestTracingService.addSpanLog(spanLog);
            }
        }
    }

    private RequestTraceSpanLog constructEjbMethodSpanLog(CallFlowInfo info, boolean callEnter) {
        String eventName = "enterEjbMethodEvent";
        if (!callEnter) {
            eventName = "exitEjbMethodEvent";
        }
        RequestTraceSpanLog spanLog = new RequestTraceSpanLog(eventName);
        spanLog.addLogEntry("ApplicationName", info.getApplicationName());
        spanLog.addLogEntry("ComponentName", info.getComponentName());
        spanLog.addLogEntry("ComponentType", info.getComponentType().toString());
        spanLog.addLogEntry("ModuleName", info.getModuleName());
        spanLog.addLogEntry("EJBClass", this.ejbClass.getCanonicalName());
        spanLog.addLogEntry("EJBMethod", info.getMethod().getName());
        spanLog.addLogEntry("CallerPrincipal", info.getCallerPrincipal());
        spanLog.addLogEntry("TX-ID", info.getTransactionId());
        return spanLog;
    }

    protected Object invokeTargetBeanMethod(Method beanClassMethod, EjbInvocation inv, Object target,
                                            Object[] params)
            throws Throwable {
        try {
            onEjbMethodStart(inv.invocationInfo.str_method_sig);
            if (inv.useFastPath) {
                return inv.getBeanMethod().invoke(inv.ejb, inv.methodParams);
            } else {

                return securityManager.invoke(target, beanClassMethod, params);
            }
        } catch (InvocationTargetException ite) {
            inv.exception = ite.getCause();
            throw ite;
        } catch (Throwable c) {
            inv.exception = c;
            throw c;
        } finally {
            onEjbMethodEnd(inv.invocationInfo.str_method_sig, inv.exception);
        }
    }

    /**
     * This is implemented by concrete containers that support TimedObjects.
     */
    protected void doTimerInvocationInit(EjbInvocation inv, Object primaryKey)
            throws Exception {
        throw new EJBException("This container doesn't support TimedObjects");
    }

    /**
     * Undeploy event.
     * Code must be able to gracefully handle redundant undeploy/shutdown
     * calls for the same container instance.
     *
     */
    @Override
    public final void undeploy() {

        try {

            if (!isUndeployed()) {

                setUndeployedState();

                try {
                    stopTimers();
                } catch (Exception e) {
                    _logger.log(Level.FINE, "Error destroying timers for " +
                            ejbDescriptor.getName(), e);
                }

                // Shutdown with undeploy
                doConcreteContainerShutdown(true);

                // BaseContainer cleanup
                doContainerCleanup();
            }
        } catch (Throwable t) {
            // Make sure we don't propagate an exception since that could
            // prevent the cleanup of some other component.
            _logger.log(Level.FINE, "BsaeContainer::undeploy exception", t);
        }

    }

    /**
     * Container shutdown event. This happens for every kind of
     * shutdown other than undeploy.  It could mean the server
     * is shutting down or that the app has been disabled while
     * the server is still running.  The two cases are handled
     * the same. We must be able to gracefully handle redundant
     * shutdown calls for the same container instance.
     */
    @Override
    public final void onShutdown() {

        try {
            if (!isStopped()) {

                setStoppedState();

                try {
                    stopTimers();
                } catch (Exception e) {
                    _logger.log(Level.FINE, "Error stopping timers for " +
                            ejbDescriptor.getName(), e);
                }
                // Cleanup without undeploy
                doConcreteContainerShutdown(false);

                // BaseContainer cleanup
                doContainerCleanup();
            }
        } catch (Throwable t) {
            // Make sure we don't propagate an exception since that could
            // prevent the cleanup of some other component.
            _logger.log(Level.FINE, "BsaeContainer::onShutdown exception", t);
        }
    }

    // Concrete container shutdown actions
    protected abstract void doConcreteContainerShutdown(boolean appBeingUndeployed);

    /**
     * Perform common container shutdown actions.  NOTE that this should be done
     * defensively so that we attempt to do as much cleanup as possible, even
     * in the face of errors.  This might be called after
     * an unsuccessful deployment, in which case some of the services might
     * not have been initialized.
     */


    private void doContainerCleanup() {

        if (baseContainerCleanupDone) {
            return;
        }

        try {
            if (isWebServiceEndpoint && (webServiceEndpoint != null)) {
                String endpointAddress =
                        webServiceEndpoint.getEndpointAddressUri();
                if (wsejbEndpointRegistry != null) {
                    wsejbEndpointRegistry.unregisterEndpoint(endpointAddress);
                }
            }

            // NOTE : Pipe cleanup that used to done here is now encapsulated within
            // endpoint registry unregisterEndpoint operation

        } catch (Exception e) {
            _logger.log(Level.FINE, "Error unregistering ejb endpoint for " +
                    ejbDescriptor.getName(), e);
        }


        if (hasAsynchronousInvocations) {
            EjbAsyncInvocationManager asyncManager =
                    ((EjbContainerUtilImpl) ejbContainerUtilImpl).getEjbAsyncInvocationManager();
            asyncManager.cleanupContainerTasks(this);
        }


        final Thread currentThread = Thread.currentThread();
        final ClassLoader previousClassLoader =
                currentThread.getContextClassLoader();

        // Unpublish all portable and non-portable JNDI names
        for (Map.Entry<String, JndiInfo> entry : jndiInfoMap.entrySet()) {
            JndiInfo jndiInfo = entry.getValue();

            try {
                jndiInfo.unpublish(this.namingManager);
            } catch (Exception e) {
                _logger.log(Level.FINE, "Error while unbinding JNDI name " + jndiInfo.name +
                        " for EJB : " + this.ejbDescriptor.getName(), e);
            }
        }

        try {
            currentThread.setContextClassLoader(loader);

            if (isRemote) {
                try {

                    if (hasRemoteHomeView) {

                        remoteHomeRefFactory.destroyReference(ejbHomeStub,
                                ejbHome);

                        // Hints to release stub-related meta-data in ORB
                        remoteHomeRefFactory.cleanupClass(homeIntf);
                        remoteHomeRefFactory.cleanupClass(remoteIntf);
                        remoteHomeRefFactory.cleanupClass(ejbHome.getClass());
                        if (ejbObjectProxyClass != null) {
                            remoteHomeRefFactory.cleanupClass(ejbObjectProxyClass);
                        }

                        // destroy the factory itself
                        remoteHomeRefFactory.destroy();
                    }

                    if (hasRemoteBusinessView) {

                        // Home related cleanup
                        RemoteReferenceFactory remoteBusinessRefFactory =
                                remoteBusinessIntfInfo.values().iterator().
                                        next().referenceFactory;
                        remoteBusinessRefFactory.destroyReference
                                (ejbRemoteBusinessHomeStub, ejbRemoteBusinessHome);

                        remoteBusinessRefFactory.cleanupClass(remoteBusinessHomeIntf);
                        remoteBusinessRefFactory.cleanupClass(ejbRemoteBusinessHome.getClass());

                        // Cleanup for each remote business interface
                        for (RemoteBusinessIntfInfo next : remoteBusinessIntfInfo.values()) {

                            next.referenceFactory.cleanupClass(next.generatedRemoteIntf);
                            if (next.proxyClass != null) {
                                next.referenceFactory.cleanupClass(next.proxyClass);
                            }

                            // destroy the factory itself
                            next.referenceFactory.destroy();
                        }

                    }

                } catch (Exception ex) {
                    _logger.log(Level.FINE, "Exception during undeploy", logParams);
                    _logger.log(Level.FINE, "", ex);
                }
            }

            try {
                ejbContainerUtilImpl.getComponentEnvManager().
                        unbindFromComponentNamespace(ejbDescriptor);
            } catch (javax.naming.NamingException namEx) {
                _logger.log(Level.FINE, "Exception during undeploy", logParams);
                _logger.log(Level.FINE, "", namEx);
            }

            ejbContainerUtilImpl.unregisterContainer(this);

            unregisterProbeListeners();

        } finally {
            currentThread.setContextClassLoader(previousClassLoader);
        }


        baseContainerCleanupDone = true;

        _logger.log(Level.FINE, "**** [BaseContainer]: Successfully Undeployed " +
                ejbDescriptor.getName() + " ...");


    }

    private void unregisterProbeListeners() {
        try {
            ejbProbeListener.unregister();
            ProbeProviderFactory probeFactory = ejbContainerUtilImpl.getProbeProviderFactory();
            probeFactory.unregisterProbeProvider(ejbProbeNotifier);

            if (timerProbeListener != null) {
                timerProbeListener.unregister();
                probeFactory.unregisterProbeProvider(timerProbeNotifier);
            }
            if (poolProbeListener != null) {
                poolProbeListener.unregister();
            }
            if (cacheProbeListener != null) {
                cacheProbeListener.unregister();
                if (cacheProbeNotifier != null) {
                    probeFactory.unregisterProbeProvider(cacheProbeNotifier);
                }
            }

            executorProbeListener.unregister();
        } catch (Exception ex) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Error unregistering the ProbeProvider");
            }
        }
    }

    /**
     * Called when server instance is Ready
     */
    @Override
    public void onReady() {
    }


    /**
     * Called when server instance is terminating. This method is the last
     * one called during server shutdown.
     */
    @Override
    public void onTermination() {
    }


    /***************************************************************************
     * The following methods implement transaction management machinery
     * in a reusable way for both SessionBeans and EntityBeans
     **************************************************************************/

    /**
     * This is called from preInvoke before every method invocation
     * on the EJB instance, including ejbCreate, ejbFind*, ejbRemove.
     * Also called from MessageBeanContainer, WebServiceInvocationHandler, etc,
     * so we can't assume that BaseContainer.preInvoke(EjbInvocation) has run.
     * Therefore, handle inv.invocationInfo defensively since it might not have
     * been initialized.
     */
    protected final void preInvokeTx(EjbInvocation inv) throws Exception {
        if (inv.invocationInfo == null) {
            inv.invocationInfo = getInvocationInfo(inv);
            if (inv.invocationInfo == null) {
                throw new EJBException("EjbInvocation Info lookup failed for method " + inv.method);
            } else {
                inv.transactionAttribute = inv.invocationInfo.txAttr;
            }
        }
        containerTransactionManager.preInvokeTx(inv);
    }


    // Called before invoking a bean with no Tx or with a new Tx.
    // Check if the bean is associated with an unfinished tx.
    protected void checkUnfinishedTx(Transaction prevTx, EjbInvocation inv) {
    }

    // Called from preInvokeTx to check if transaction needs to be suspended
    protected boolean suspendTransaction(EjbInvocation inv) throws Exception {
        // Overridden in subclass that needs it
        return false;
    }

    // Called from postInvokeTx if transaction needs to be resumed
    protected boolean resumeTransaction(EjbInvocation inv) throws Exception {
        // Overridden in subclass that needs it
        return false;
    }

    // Called from preInvokeTx before invoking the bean with the client's Tx
    // Also called from EntityContainer.removeBean for cascaded deletes
    protected void useClientTx(Transaction prevTx, EjbInvocation inv) {
        containerTransactionManager.useClientTx(prevTx, inv);
    }

    protected void validateEMForClientTx(EjbInvocation inv, JavaEETransaction t) {
        // Do nothing in general case
    }

    /**
     * postInvokeTx is called after every invocation on the EJB instance,
     * including ejbCreate/ejbFind---/ejbRemove.
     * NOTE: postInvokeTx is called even if the EJB was not invoked
     * because of an exception thrown from preInvokeTx.
     */
    protected void postInvokeTx(EjbInvocation inv) throws Exception {
        containerTransactionManager.postInvokeTx(inv);
    }

    // this is the counterpart of useClientTx
    // Called from postInvokeTx after invoking the bean with the client's Tx
    // Also called from EntityContainer.removeBean for cascaded deletes
    protected Throwable checkExceptionClientTx(EJBContextImpl context, Throwable exception) throws Exception {
        return containerTransactionManager.checkExceptionClientTx(context, exception);
    }

    // Implementation of Container method.
    // Called from UserTransactionImpl after the EJB started a Tx,
    // for TX_BEAN_MANAGED EJBs only.
    @Override
    public final void doAfterBegin(ComponentInvocation ci) {
        EjbInvocation inv = (EjbInvocation) ci;
        try {
            // Associate the context with tx so that on subsequent
            // invocations with the same tx, we can do the appropriate
            // tx.resume etc.
            EJBContextImpl sc = (EJBContextImpl) inv.context;
            Transaction tx = transactionManager.getTransaction();
            if (!isSingleton) {
                sc.setTransaction(tx);
            }

            // Register Synchronization with TM so that we can
            // dissociate the context from tx in afterCompletion
            ejbContainerUtilImpl.getContainerSync(tx).addBean(sc);

            enlistExtendedEntityManagers(sc);
            // Dont call container.afterBegin() because
            // TX_BEAN_MANAGED EntityBeans are not allowed,
            // and SessionSync calls on TX_BEAN_MANAGED SessionBeans
            // are not allowed.
        } catch (SystemException ex) {
            throw new EJBException(ex);
        } catch (RollbackException ex) {
            throw new EJBException(ex);
        } catch (IllegalStateException ex) {
            throw new EJBException(ex);
        }
    }

    // internal APIs, called from ContainerSync, implemented in subclasses
    protected abstract void afterBegin(EJBContextImpl context);

    protected abstract void beforeCompletion(EJBContextImpl context);

    protected abstract void afterCompletion(EJBContextImpl context, int status);

    protected void preInvokeNoTx(EjbInvocation inv) {
        // No-op by default
    }

    protected void postInvokeNoTx(EjbInvocation inv) {
        // No-op by default
    }

    /**
     * Checks if the exception is not null and if it is checked exception
     * - uses ejb descriptor to resolve it.
     *
     * @param exception can be null
     * @return true if the exception is not null and if it is not an unchecked exception
     */
    protected boolean isApplicationException(Throwable exception) {
        return exception != null && !isSystemUncheckedException(exception);
    }

    /**
     * Checks if the exception is not null and if it is a system unchecked exception
     * - uses ejb descriptor to resolve it.
     *
     * @param exception can be null
     * @return true if the exception is not null and if it is an unchecked exception
     */
    protected boolean isSystemUncheckedException(Throwable exception) {
        if (exception == null) {
            return false;
        }
        if (exception instanceof RuntimeException //
                || exception instanceof Error //
                || exception instanceof RemoteException) {

            Class clazz = exception.getClass();
            final String exceptionClassName = clazz.getName();
            final Map<String, EjbApplicationExceptionInfo> appExceptions = //
                    ejbDescriptor.getEjbBundleDescriptor().getApplicationExceptions();
            while (clazz != null) {
                final String eClassName = clazz.getName();
                if (appExceptions.containsKey(eClassName)) {
                    if (exceptionClassName.equals(eClassName)) {
                        // Exact exception is specified as an ApplicationException
                        return false;
                    } else {
                        // Superclass exception is not inherited
                        return !appExceptions.get(eClassName).getInherited();
                    }
                }
                clazz = clazz.getSuperclass();
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean getDebugMonitorFlag() {
        return debugMonitorFlag;
    }

    public void setDebugMonitorFlag(boolean flag) {
        debugMonitorFlag = flag;
    }

    protected static final String containerStateToString(int state) {
        switch (state) {
            case CONTAINER_INITIALIZING:
                return "Initializing";
            case CONTAINER_STARTED:
                return "Started";
            case CONTAINER_STOPPED:
                return "STOPPED";
            case CONTAINER_UNDEPLOYED:
                return "Undeployed";
            case CONTAINER_ON_HOLD:
                return "ON_HOLD";
        }
        return "Unknown Container state: " + state;
    }

    protected final boolean isRemoteInterfaceSupported() {
        return hasRemoteHomeView;
    }

    protected final boolean isLocalInterfaceSupported() {
        return hasLocalHomeView;
    }

    protected int getTxAttrForLifecycleCallback(Set<LifecycleCallbackDescriptor> lifecycleCallbackDescriptors,
                                                int defaultTxAttr, int... validateTxAttr) throws Exception {
        int txAttr = isBeanManagedTran ?
                Container.TX_BEAN_MANAGED : defaultTxAttr;

        if (!isBeanManagedTran) {
            for (LifecycleCallbackDescriptor lcd : lifecycleCallbackDescriptors) {
                if (lcd.getLifecycleCallbackClass().equals(ejbDescriptor.getEjbClassName())) {

                    Method callbackMethod = lcd.getLifecycleCallbackMethodObject(loader);
                    int lcTxAttr = containerTransactionManager.findTxAttr(
                            new MethodDescriptor(callbackMethod, MethodDescriptor.LIFECYCLE_CALLBACK));
                    // Since default attribute is set up, override the value if it's validateTxAttr
                    for (int t : validateTxAttr) {
                        if (lcTxAttr == t) {
                            txAttr = t;
                            if (_logger.isLoggable(Level.FINE)) {
                                _logger.log(Level.FINE, "Found callback method " + ejbDescriptor.getEjbClassName() +
                                        "<>" + callbackMethod + " : " + txAttr);
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Returning attr for " + ejbDescriptor.getEjbClassName() + " : " + txAttr);
        }

        return txAttr;
    }


    /**
     * Called from various places within the container that are responsible
     * for dispatching invocations to business methods.  This method has
     * the exception semantics of Method.invoke().  Any exception that
     * originated from the business method or application code within an
     * interceptor will be propagated as the cause within an
     * InvocationTargetException.
     *
     */
    protected Object intercept(EjbInvocation inv) throws Throwable {
        Object result = null;
        if (inv.mustInvokeAsynchronously()) {
            EjbAsyncInvocationManager asyncManager =
                    ((EjbContainerUtilImpl) ejbContainerUtilImpl).getEjbAsyncInvocationManager();
            Future future = inv.isLocal ?
                    asyncManager.createLocalFuture(inv) :
                    asyncManager.createRemoteFuture(inv, this, (GenericEJBHome) ejbRemoteBusinessHomeStub);
            result = (inv.invocationInfo.method.getReturnType() == void.class)
                    ? null : future;
        } else {
            result = __intercept(inv);
        }
        return result;
    }

    private Object __intercept(EjbInvocation inv)
            throws Throwable {
        Object result = null;
        if (interceptorManager.hasInterceptors()) {
            try {
                onEjbMethodStart(inv.invocationInfo.str_method_sig);
                result = interceptorManager.intercept(inv.getInterceptorChain(), inv);
            } catch (Throwable t) {
                inv.exception = t;
                throw new InvocationTargetException(t);
            } finally {
                onEjbMethodEnd(inv.invocationInfo.str_method_sig, inv.exception);
            }
        } else { // invoke() has the same exc. semantics as Method.invoke
            result = this.invokeTargetBeanMethod(inv.getBeanMethod(), inv, inv.ejb,
                    inv.methodParams);
        }

        return result;
    }

    /**
     * Called from Interceptor Chain to invoke the actual bean method.
     * This method must throw any exception from the bean method *as is*,
     * without being wrapped in an InvocationTargetException.  The exception
     * thrown from this method will be propagated through the application's
     * interceptor code, so it must not be changed in order for any exception
     * handling logic in that code to function properly.
     */
    public Object invokeBeanMethod(EjbInvocation inv)
            throws Throwable {
        try {

            return securityManager.invoke(inv.ejb, inv.getBeanMethod(), inv.getParameters());

        } catch (InvocationTargetException ite) {
            throw ite.getCause();
        }
    }

    protected abstract EjbMonitoringStatsProvider getMonitoringStatsProvider(
            String appName, String modName, String ejbName);

    protected void createMonitoringRegistry() {
        String appName = null;
        String modName = null;
        String ejbName = null;
        boolean isMonitorRegistryMediatorCreated = false;

        try {
            appName = (ejbDescriptor.getApplication().isVirtual())
                    ? null : ejbDescriptor.getApplication().getRegistrationName();
            if (appName == null) {
                modName = ejbDescriptor.getApplication().getRegistrationName();
            } else {
                String archiveuri = ejbDescriptor.getEjbBundleDescriptor().
                        getModuleDescriptor().getArchiveUri();
                modName =
                        com.sun.enterprise.util.io.FileUtils.makeFriendlyFilename(archiveuri);
            }
            ejbName = ejbDescriptor.getName();
            containerInfo = new ContainerInfo(appName, modName, ejbName);

            isMonitorRegistryMediatorCreated = true;
            registerEjbMonitoringProbeProvider(appName, modName, ejbName);
            ejbProbeListener = getMonitoringStatsProvider(appName, modName, ejbName);
            ejbProbeListener.addMethods(getContainerId(), appName, modName, ejbName, getMonitoringMethodsArray());
            ejbProbeListener.register();

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Created MonitoringRegistry: " +
                        EjbMonitoringUtils.getDetailedLoggingName(appName, modName, ejbName));
            }
        } catch (Exception ex) {
            _logger.log(Level.SEVERE, COULD_NOT_CREATE_MONITORREGISTRYMEDIATOR, new Object[]{EjbMonitoringUtils.getDetailedLoggingName(appName, modName, ejbName), ex});
            if (!isMonitorRegistryMediatorCreated) {
                registerEjbMonitoringProbeProvider(appName, modName, ejbName);
            }
        }
    }

    private void registerEjbMonitoringProbeProvider(String appName, String modName, String ejbName) {
        // Always create to avoid NPE
        try {
            ProbeProviderFactory probeFactory = ejbContainerUtilImpl.getProbeProviderFactory();
            String invokerId = EjbMonitoringUtils.getInvokerId(appName, modName, ejbName);
            ejbProbeNotifier = probeFactory.getProbeProvider(EjbMonitoringProbeProvider.class, invokerId);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Got ProbeProvider: " + ejbProbeNotifier.getClass().getName());
            }
        } catch (Exception ex) {
            ejbProbeNotifier = new EjbMonitoringProbeProvider();
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Error getting the EjbMonitoringProbeProvider");
            }
        }
    }

    protected String[] getMonitoringMethodsArray() {
        return getMonitoringMethodsArray((monitoredGeneratedClasses.size() > 0));
    }

    protected String[] getMonitoringMethodsArray(boolean hasGeneratedClasses) {
        String[] method_sigs = null;
        if (hasGeneratedClasses) {
            List<String> methodList = new ArrayList<>();
            for (Class clz : monitoredGeneratedClasses) {
                for (Method m : clz.getDeclaredMethods()) {
                    methodList.add(EjbMonitoringUtils.stringify(m));
                }
            }
            method_sigs = methodList.toArray(new String[methodList.size()]);
        } else {
            Vector methodVec = ejbDescriptor.getMethods();
            int sz = methodVec.size();
            method_sigs = new String[sz];
            for (int i = 0; i < sz; i++) {
                method_sigs[i] = EjbMonitoringUtils.stringify((Method) methodVec.get(i));
            }
        }

        return method_sigs;
    }

    protected void doFlush(EjbInvocation inv) {
    }

    protected void registerMonitorableComponents() {
        createMonitoringRegistry();
        registerTimerMonitorableComponent();
        executorProbeListener = new EjbThreadPoolExecutorStatsProvider(null);

        executorProbeListener.register();
    }

    protected void registerTimerMonitorableComponent() {
        if (isTimedObject()) {
            String invokerId = EjbMonitoringUtils.getInvokerId(containerInfo.appName, containerInfo.modName, containerInfo.ejbName);
            try {
                ProbeProviderFactory probeFactory = ejbContainerUtilImpl.getProbeProviderFactory();
                timerProbeNotifier = probeFactory.getProbeProvider(EjbTimedObjectProbeProvider.class, invokerId);
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Got TimerProbeProvider: " + timerProbeNotifier.getClass().getName());
                }
            } catch (Exception ex) {
                timerProbeNotifier = new EjbTimedObjectProbeProvider();
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Error getting the TimerProbeProvider");
                }
            }
            timerProbeListener = new EjbTimedObjectStatsProvider(
                    containerInfo.appName, containerInfo.modName, containerInfo.ejbName);
            timerProbeListener.register();
        }
        _logger.log(Level.FINE, "[BaseContainer] registered timer monitorable");
    }

    protected void incrementCreatedTimedObject() {
        timerProbeNotifier.ejbTimerCreatedEvent();
    }

    protected void incrementRemovedTimedObject() {
        timerProbeNotifier.ejbTimerRemovedEvent();
    }

    protected void incrementDeliveredTimedObject() {
        timerProbeNotifier.ejbTimerDeliveredEvent();
    }

    private static class JndiInfo {

        private JndiInfo(String name, Object object) {
            this.name = name;
            this.object = object;
        }

        static JndiInfo newPortableLocal(String name, Object obj) {
            JndiInfo jndiInfo = new JndiInfo(name, obj);
            jndiInfo.portable = true;
            return jndiInfo;
        }

        static JndiInfo newPortableRemote(String name, Object obj) {
            JndiInfo jndiInfo = new JndiInfo(name, obj);
            jndiInfo.portable = true;
            jndiInfo.cosNaming = isCosNamingObject(obj);
            return jndiInfo;
        }

        static JndiInfo newNonPortableRemote(String name, Object obj) {
            JndiInfo jndiInfo = new JndiInfo(name, obj);
            jndiInfo.portable = false;
            jndiInfo.cosNaming = isCosNamingObject(obj);
            return jndiInfo;
        }


        void publish(GlassfishNamingManager nm) throws NamingException {

            // If it's a portable name, use rebind since the name is guaranteed
            // to be unique.  Otherwise, use bind() so we detect any clashes.
            // NOTE : Will need to revisit this if we allow a developer-specified
            // portable JNDI name.
            boolean rebind = portable;

            if (cosNaming) {
                nm.publishCosNamingObject(name, object, rebind);
            } else {
                nm.publishObject(name, object, rebind);
            }

            publishedSuccessfully = true;

        }

        void unpublish(GlassfishNamingManager nm) throws NamingException {

            if (publishedSuccessfully) {
                if (cosNaming) {
                    nm.unpublishCosNamingObject(name);
                } else {
                    nm.unpublishObject(name);
                }
            } else {
                _logger.log(Level.FINE, "Skipping unpublish of " + name + " because it was " +
                        "never published successfully in the first place");
            }
        }

        public void setInternal(boolean flag) {
            internal = flag;

        }

        private static boolean isCosNamingObject(Object obj) {
            return ((obj instanceof java.rmi.Remote) ||
                    (obj instanceof org.omg.CORBA.Object));
        }

        String name;
        Object object;
        boolean cosNaming;
        boolean portable;
        boolean internal;
        boolean publishedSuccessfully;


    }

    /**
     * PreInvokeException is used to wrap exceptions thrown
     * from BaseContainer.preInvoke, so it indicates that the bean's
     * method will not be called.
     * <p>
     * Use usual {@link PreInvokeException#getCause()} to get wrapped exception.
     */
    public final static class PreInvokeException extends EJBException {

        private static final long serialVersionUID = -1214474588837446339L;

        public PreInvokeException(Exception ex) {
            super(ex);
        }
    } //PreInvokeException{}

    /**
     * Strings for monitoring info
     */
    public static final class ContainerInfo {
        public String appName;
        public String modName;
        public String ejbName;

        ContainerInfo(String appName, String modName, String ejbName) {
            this.appName = appName;
            this.modName = modName;
            this.ejbName = ejbName;
        }
    } //ContainerInfo

    private static class BeanContext {
        ClassLoader previousClassLoader;
        boolean classLoaderSwitched;
    }
} //BaseContainer{}

final class CallFlowInfoImpl
        implements CallFlowInfo {

    private final BaseContainer container;

    private final EjbDescriptor ejbDescriptor;

    private final String appName;

    private final String modName;

    private final String ejbName;

    private final ComponentType componentType;

    CallFlowInfoImpl(BaseContainer container, EjbDescriptor descriptor,
                     ComponentType compType) {
        this.container = container;
        this.ejbDescriptor = descriptor;

        this.appName = (ejbDescriptor.getApplication().isVirtual()) ? null
                : ejbDescriptor.getApplication().getRegistrationName();
        String archiveuri = ejbDescriptor.getEjbBundleDescriptor()
                .getModuleDescriptor().getArchiveUri();
        this.modName = com.sun.enterprise.util.io.FileUtils
                .makeFriendlyFilename(archiveuri);
        this.ejbName = ejbDescriptor.getName();

        this.componentType = compType;
    }

    @Override
    public String getApplicationName() {
        return appName;
    }

    @Override
    public String getModuleName() {
        return modName;
    }

    @Override
    public String getComponentName() {
        return ejbName;
    }

    @Override
    public ComponentType getComponentType() {
        return componentType;
    }

    @Override
    public java.lang.reflect.Method getMethod() {
        EjbInvocation inv = (EjbInvocation)
                EjbContainerUtilImpl.getInstance().getCurrentInvocation();

        return inv.method;
    }

    @Override
    public String getTransactionId() {
        JavaEETransaction tx = null;
        try {
            tx =
                    (JavaEETransaction) EjbContainerUtilImpl.getInstance().
                            getTransactionManager().getTransaction();
        } catch (Exception ex) {
            //TODO: Log exception
        }

        return (tx == null) ? null : "" + tx; //TODO tx.getTransactionId();
    }

    @Override
    public String getCallerPrincipal() {
        java.security.Principal principal =
                container.getSecurityManager().getCallerPrincipal();

        return (principal != null) ? principal.getName() : null;
    }

    @Override
    public Throwable getException() {
        return ((EjbInvocation) EjbContainerUtilImpl.getInstance().getCurrentInvocation()).exception;
    }
}

final class RemoteBusinessIntfInfo {

    Class generatedRemoteIntf;
    Class remoteBusinessIntf;
    String jndiName;
    RemoteReferenceFactory referenceFactory;
    Class proxyClass;

}


final class SafeProperties extends Properties {
    private static final String errstr =
            "Environment properties cannot be modified";
    private static final String ejb10Prefix = "ejb10-properties/";

    @Override
    public void load(java.io.InputStream inStream) {
        throw new RuntimeException(errstr);
    }

    @Override
    public Object put(Object key, Object value) {
        throw new RuntimeException(errstr);
    }

    @Override
    public void putAll(Map t) {
        throw new RuntimeException(errstr);
    }

    @Override
    public Object remove(Object key) {
        throw new RuntimeException(errstr);
    }

    @Override
    public void clear() {
        throw new RuntimeException(errstr);
    }

    void copy(Set s) {
        Iterator i = s.iterator();
        defaults = new Properties();
        while (i.hasNext()) {
            EnvironmentProperty p = (EnvironmentProperty) i.next();
            if (p.getName().startsWith(ejb10Prefix)) {
                String newName = p.getName().substring(ejb10Prefix.length());
                defaults.put(newName, p.getValue());
            }
        }
    }

    private void readObject(java.io.ObjectInputStream stream)
            throws java.io.IOException, ClassNotFoundException {
        defaults = (Properties) stream.readObject();
    }

    private void writeObject(java.io.ObjectOutputStream stream)
            throws java.io.IOException {
        stream.writeObject(defaults);
    }
} //SafeProperties{}

