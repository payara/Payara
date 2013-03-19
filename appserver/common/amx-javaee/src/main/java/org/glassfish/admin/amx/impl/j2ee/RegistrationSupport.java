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

package org.glassfish.admin.amx.impl.j2ee;

import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.BindableResource;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.ResourcePool;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.archivist.Archivist;
import com.sun.enterprise.deployment.archivist.ArchivistFactory;
import com.sun.enterprise.deployment.io.DeploymentDescriptorFile;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.*;
import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.impl.config.ConfigBeanRegistry;
import org.glassfish.admin.amx.impl.j2ee.loader.J2EEInjectedValues;
import org.glassfish.admin.amx.impl.util.InjectedValues;
import org.glassfish.admin.amx.impl.util.ObjectNameBuilder;
import org.glassfish.admin.amx.j2ee.*;
import org.glassfish.admin.amx.util.ClassUtil;
import org.glassfish.admin.amx.util.MapUtil;
import org.glassfish.admin.amx.util.jmx.JMXUtil;
import org.glassfish.api.admin.config.Named;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.jvnet.hk2.config.ConfigBeanProxy;

/**
    Handles registrations of resources and applications associated with a J2EEServer.
    There must be one and only one of these instances per J2EEServer.
 */
final class RegistrationSupport
{
    private static void cdebug(Object o)
    {
        System.out.println("" + o);
    }

    /**
        Associates the ObjectName of a ResourceRef or ApplicationRef with its corresponding
        top-level JSR 77 MBean. Children of those JSR 77 MBeans come and go with their parent.
     */
    private final Map<ObjectName, ObjectName> mConfigRefTo77 = new HashMap<ObjectName, ObjectName>();

    private final J2EEServer mJ2EEServer;

    private final MBeanServer mMBeanServer;

    private final RefListener mResourceRefListener;

    /** The Server config for this J2EEServer */
    private final Server mServer;
    
    /** type of any resource ref */
    private final String mResourceRefType;
    
    /** type of any application ref */
    private final String mApplicationRefType;

    private final Logger mLogger = AMXEELoggerInfo.getLogger();
    
    public RegistrationSupport(final J2EEServer server)
    {
        mJ2EEServer = server;
        mMBeanServer = (MBeanServer) server.extra().mbeanServerConnection();

        mResourceRefType    = Util.deduceType(ResourceRef.class);
        mApplicationRefType = Util.deduceType(ApplicationRef.class);
        mServer = getDomain().getServers().getServer( mJ2EEServer.getName() );

        mResourceRefListener = new RefListener();

        registerApplications();
    }

    protected void cleanup()
    {
        mResourceRefListener.stopListening();
    }

    public void start()
    {
        mResourceRefListener.startListening();
    }

    /** Maps configuration MBean type to J2EE type */
    public static final Map<String, Class> CONFIG_RESOURCE_TYPES =
            MapUtil.toMap(new Object[]
            {
                "jdbc-resource", JDBCResourceImpl.class,
                "java-mail-resource", JavaMailResourceImpl.class,
                "jca-resource", JCAResourceImpl.class,
                "jms-resource", JMSResourceImpl.class,
                "jndi-resource", JNDIResourceImpl.class,
                "jta-resource", JTAResourceImpl.class,
                "rmi-iiop-resource", RMI_IIOPResourceImpl.class,
                "url-resource", URLResourceImpl.class
            },
            String.class, Class.class);

    private Domain getDomain()
    {
    	return InjectedValues.getInstance().getHabitat().getService(Domain.class);
    }
    
    private ObjectName getObjectName(ConfigBeanProxy cbp)
    {
    	return ConfigBeanRegistry.getInstance().getObjectNameForProxy(cbp);
    }

    private String getDeploymentDescriptor(
        final BundleDescriptor bundleDesc )
    {
        final ArchivistFactory archivistFactory = J2EEInjectedValues.getInstance().getArchivistFactory();
        
        String dd = "unavailable";
        ByteArrayOutputStream out = null;
        try
        {
            final Archivist moduleArchivist = archivistFactory.getArchivist(bundleDesc.getModuleDescriptor().getModuleType());
            final DeploymentDescriptorFile ddFile =  moduleArchivist.getStandardDDFile();
            
            out = new ByteArrayOutputStream();
            ddFile.write(bundleDesc, out);
            final String charsetName = "UTF-8";
            dd = out.toString(charsetName);
        }
        catch( final Exception e )
        {
            dd = null;
        }
        finally
        {
            if ( out != null )
            {
                try { out.close(); } catch( Exception ee) {}
            }
        }
                
        return dd;
    }
  
    private ObjectName createAppMBeans(
    	com.sun.enterprise.config.serverbeans.Application appConfig,
        final Application application,
        final MetadataImpl meta)
    {
        final String appLocation = appConfig.getLocation();

        final boolean isStandalone = application.isVirtual();
        ObjectName parentMBean = null;
        ObjectName top = null;
        if (isStandalone)
        {
            parentMBean = mJ2EEServer.objectName();
        }
        else
        {
            final String xmlDesc = getDeploymentDescriptor(application);
            if ( xmlDesc != null )
            {
                meta.setDeploymentDescriptor(xmlDesc);
            }
            parentMBean = registerJ2EEChild(mJ2EEServer.objectName(), meta, J2EEApplication.class, J2EEApplicationImpl.class, application.getName());
            top = parentMBean;
        }

        for (final EjbBundleDescriptor desc : application.getBundleDescriptors(EjbBundleDescriptor.class))
        {
            final ObjectName objectName = registerEjbModuleAndItsComponents(parentMBean, meta, appConfig, desc);
            if (isStandalone)
            {
                assert (top == null);
                top = objectName;
            }
        }

        for (final WebBundleDescriptor desc : application.getBundleDescriptors(WebBundleDescriptor.class))
        {
            final ObjectName objectName = registerWebModuleAndItsComponents(parentMBean, meta, appConfig, desc);
            if (isStandalone)
            {
                assert (top == null);
                top = objectName;
            }
        }

        for (final ConnectorDescriptor desc : application.getBundleDescriptors(ConnectorDescriptor.class))
        {
            assert top == null;
            top = registerResourceAdapterModuleAndItsComponents(parentMBean, meta, appConfig, desc, appLocation);
        }

        for (final ApplicationClientDescriptor desc : application.getBundleDescriptors(ApplicationClientDescriptor.class))
        {
            assert top == null;
            top = registerAppClient(parentMBean, meta, appConfig, desc);
        }

        mLogger.fine("Registered JSR 77 MBeans for application/module: " + top);
        return top;
    }
    
        private com.sun.enterprise.config.serverbeans.Module
    getModuleConfig( final com.sun.enterprise.config.serverbeans.Application appConfig, final String name)
    {
        if ( appConfig.getModule(name) == null )
        {
            throw new IllegalArgumentException( "Can't find module named " + name + " in " + appConfig );
        }
        
        return appConfig.getModule(name);
    }

    /* Register ejb module and its' children ejbs which is part of an application */
    private ObjectName registerEjbModuleAndItsComponents(
            final ObjectName parentMBean,
            final MetadataImpl meta,
            final com.sun.enterprise.config.serverbeans.Application appConfig,
            final EjbBundleDescriptor ejbBundleDescriptor )
    {
        final String xmlDesc = getDeploymentDescriptor(ejbBundleDescriptor);
        if ( xmlDesc != null )
        {
            meta.setDeploymentDescriptor( xmlDesc );
        }
        final String moduleName = ejbBundleDescriptor.getModuleName();
        
        final com.sun.enterprise.config.serverbeans.Module moduleConfig = getModuleConfig(appConfig, moduleName );
        meta.setCorrespondingConfig(getObjectName(moduleConfig));
        
        final ObjectName ejbModuleObjectName = registerJ2EEChild(parentMBean, meta, EJBModule.class, EJBModuleImpl.class, moduleName);
        
        meta.remove( Metadata.CORRESPONDING_CONFIG );   // none for an EJB MBean
        meta.remove( Metadata.DEPLOYMENT_DESCRIPTOR );   // none for an EJB MBean
        for (final EjbDescriptor desc : ejbBundleDescriptor.getEjbs())
        {
            createEJBMBean(ejbModuleObjectName, meta, desc);
        }
        return ejbModuleObjectName;
    }

    private ObjectName createEJBMBean(
            final ObjectName parentMBean,
            final MetadataImpl meta,
            final EjbDescriptor ejbDescriptor)
    {
        final String ejbName = ejbDescriptor.getName();
        final String ejbType = ejbDescriptor.getType();
        final String ejbSessionType = ejbType.equals("Session") ? ((EjbSessionDescriptor) ejbDescriptor).getSessionType() : null;

        Class<? extends EJB> intf = null;
        Class<? extends EJBImplBase> impl = null;
        if (ejbType.equals("Entity"))
        {
            intf = EntityBean.class;
            impl = EntityBeanImpl.class;
        }
        else if (ejbType.equals("Message-driven"))
        {
            intf = MessageDrivenBean.class;
            impl = MessageDrivenBeanImpl.class;
        }
        else if (ejbType.equals("Session"))
        {
            if (ejbSessionType.equals("Stateless"))
            {
                intf = StatelessSessionBean.class;
                impl = StatelessSessionBeanImpl.class;
            }
            else if (ejbSessionType.equals("Stateful"))
            {
                intf = StatefulSessionBean.class;
                impl = StatefulSessionBeanImpl.class;
            }
            else if (ejbSessionType.equals("Singleton")) // EJB 3.1
            {
                intf = SingletonSessionBean.class;
                impl = SingletonSessionBeanImpl.class;
            }
            else
            {
                throw new IllegalArgumentException( "Unknown ejbSessionType: " + ejbSessionType + ", expected Stateless or Stateful");
            }
        }

        return registerJ2EEChild(parentMBean, meta, intf, impl, ejbName);
    }

    /* Register web module and its' children which is part of an application */
    private ObjectName registerWebModuleAndItsComponents(
            final ObjectName parentMBean,
            final MetadataImpl meta,
            final com.sun.enterprise.config.serverbeans.Application appConfig,
            final WebBundleDescriptor webBundleDescriptor )
    {
        final String xmlDesc = getDeploymentDescriptor(webBundleDescriptor);
        if ( xmlDesc != null )
        {
            meta.setDeploymentDescriptor( xmlDesc );
        }
        
        final String moduleName = webBundleDescriptor.getModuleName();
        
        final com.sun.enterprise.config.serverbeans.Module moduleConfig = getModuleConfig(appConfig, moduleName );
        meta.setCorrespondingConfig(getObjectName(moduleConfig));

        final ObjectName webModuleObjectName = registerJ2EEChild(parentMBean, meta, WebModule.class, WebModuleImpl.class, moduleName);

        meta.remove( Metadata.CORRESPONDING_CONFIG );   // none for a Servlet
        meta.remove( Metadata.DEPLOYMENT_DESCRIPTOR );   // none for an Servlet
        for (final WebComponentDescriptor desc : webBundleDescriptor.getWebComponentDescriptors())
        {
            final String servletName = desc.getCanonicalName();
            
            registerJ2EEChild(webModuleObjectName, meta, Servlet.class, ServletImpl.class, servletName);
        }

        return webModuleObjectName;
    }

    public ObjectName registerResourceAdapterModuleAndItsComponents(
            final ObjectName parentMBean,
            final MetadataImpl meta,
            final com.sun.enterprise.config.serverbeans.Application appConfig,
            final ConnectorDescriptor bundleDesc,
            final String appLocation)
    {
        meta.setCorrespondingConfig(getObjectName(appConfig));
        final ObjectName objectName = createRARModuleMBean(parentMBean, meta, appConfig, bundleDesc);
        
        final com.sun.enterprise.config.serverbeans.Module moduleConfig = getModuleConfig(appConfig, bundleDesc.getModuleName() );
        meta.setCorrespondingConfig(getObjectName(moduleConfig));
        
        registerJ2EEChild(objectName, meta, ResourceAdapter.class, ResourceAdapterImpl.class, bundleDesc.getName());

        return objectName;
    }

    private ObjectName createRARModuleMBean(
            final ObjectName parentMBean,
            final MetadataImpl meta,
            final com.sun.enterprise.config.serverbeans.Application appConfig,
            final ConnectorDescriptor bundleDesc )
    {
        final String xmlDesc = getDeploymentDescriptor(bundleDesc);
        if ( xmlDesc != null )
        {
            meta.setDeploymentDescriptor( xmlDesc );
        }
        final String resAdName = bundleDesc.getModuleName();

        final ObjectName objectName = registerJ2EEChild(parentMBean, meta, ResourceAdapterModule.class, ResourceAdapterModuleImpl.class, resAdName);

        return objectName;
    }

    /* Register application client module */
    public ObjectName registerAppClient(
            final ObjectName parentMBean,
            final MetadataImpl meta,
            final com.sun.enterprise.config.serverbeans.Application appConfig,
            final ApplicationClientDescriptor bundleDesc)
    {
        final String xmlDesc = getDeploymentDescriptor(bundleDesc);
        if ( xmlDesc != null )
        {
            meta.setDeploymentDescriptor( xmlDesc );
        }

        final String moduleName = bundleDesc.getModuleDescriptor().getModuleName();

        return registerJ2EEChild(parentMBean, meta, AppClientModule.class, AppClientModuleImpl.class, moduleName);
    }

  
    protected void registerApplications()
    {
        final List<ApplicationRef> appRefs = mServer.getApplicationRef();
        for (final ApplicationRef ref : appRefs)
        {
            try
            {
                processApplicationRef(ref);
            }
            catch( final Exception e )
            {
                // log it: we want to continue with other apps, even if this one had a problem
                mLogger.log( Level.INFO, AMXEELoggerInfo.registeringApplicationException, 
                        new Object[] { ref.getRef(), e});
            }
        }
    }
    
   /**
        Examine the MBean to see if it is a ResourceRef that should be manifested under this server,
        and if so, register a JSR 77 MBean for it.
     */
    public ObjectName processApplicationRef(final ApplicationRef ref)
    {
        // find all applications
        final ApplicationRegistry appRegistry = J2EEInjectedValues.getInstance().getApplicationRegistry();

        final MetadataImpl meta = new MetadataImpl();
        meta.setCorrespondingRef(getObjectName(ref));
        
        final String appName = ref.getRef();
        
        final ApplicationInfo appInfo = appRegistry.get(appName);
        if (appInfo == null)
        {
            mLogger.fine("Unable to get ApplicationInfo for application: " + appName);
            return null;
        }
        final Application app = appInfo.getMetaData(Application.class);
        if ( app == null )
        {
            if ( appInfo.isJavaEEApp() )
            {
                mLogger.log(Level.WARNING, AMXEELoggerInfo.nullAppinfo, appName);
            }
            return null;
        }
        
        final com.sun.enterprise.config.serverbeans.Application appConfig = getDomain().getApplications().getApplication(appName);
        if ( appConfig == null )
        {
            mLogger.log(Level.WARNING, AMXEELoggerInfo.errorGetappconfig, appName);
            return null;
        }
        
        meta.setCorrespondingConfig( getObjectName(appConfig) );
        final ObjectName mbean77 = createAppMBeans(appConfig, app, meta);
        synchronized (mConfigRefTo77)
        {
            mConfigRefTo77.put(getObjectName(ref), mbean77);
        }

        return mbean77;
    }


    protected <I extends J2EEManagedObject, C extends J2EEManagedObjectImplBase> ObjectName registerJ2EEChild(
            final ObjectName parent,
            final Metadata metadataIn,
            final Class<I> intf,
            final Class<C> clazz,
            final String name)
    {
        ObjectName objectName = null;
        
        final String j2eeType = Util.deduceType(intf);
        
        // must make a copy! May be an input value that is reused by caller
        final Metadata metadata = new MetadataImpl(metadataIn);
        try
        {
            final Constructor<C> c = clazz.getConstructor(ObjectName.class, Metadata.class);
            final J2EEManagedObjectImplBase impl = c.newInstance(parent, metadata);
            objectName = new ObjectNameBuilder(mMBeanServer, parent).buildChildObjectName(j2eeType, name);
            objectName = mMBeanServer.registerMBean( impl, objectName ).getObjectName();
        }
        catch (final Exception e)
        {
            throw new RuntimeException( "Cannot register " + j2eeType + "=" + name + " as child of " + parent, e);
        }

        return objectName;
    }

      
    /**
        Examine the MBean to see if it is a ResourceRef that should be manifested under this server,
        and if so, register a JSR 77 MBean for it.
     */
    public ObjectName processResourceRef(final ResourceRef ref)
    {
        if (ref == null)
        {
            throw new IllegalArgumentException("resource-ref is null");
        }

        if ( ! mServer.getName().equals(ref.getParent(Server.class).getName()))
        {
            cdebug("ResourceRef is not a child of server " + getObjectName(mServer));
            return null;
        }

        // find the referenced resource
        Resource res = null;
        List<Resource> resources = getDomain().getResources().getResources();
        for (Resource resource : resources)
        {
            String name = null;
            if (resource instanceof BindableResource) {
                name = ((BindableResource) resource).getJndiName();
            }
            if (resource instanceof Named) {
                name = ((Named) resource).getName();
            }
            if (resource instanceof ResourcePool) {
                name = ((ResourcePool) resource).getName();
            }
        	if (name != null && name.equals(ref.getRef()))
        		res = resource;
        }
        if (res == null)
        {
            throw new IllegalArgumentException("ResourceRef refers to non-existent resource: " + ref);
        }

        final String configType = Util.getTypeProp(getObjectName(res));
        final Class<J2EEManagedObjectImplBase> implClass = CONFIG_RESOURCE_TYPES.get(configType);
        if (implClass == null)
        {
            mLogger.fine("Unrecognized resource type for JSR 77 purposes: " + getObjectName(res));
            return null;
        }
        final Class<J2EEManagedObject> intf = (Class) ClassUtil.getFieldValue(implClass, "INTF");

        ObjectName mbean77 = null;
        try
        {
            final MetadataImpl meta = new MetadataImpl();
            meta.setCorrespondingRef(getObjectName(ref));
            meta.setCorrespondingConfig(getObjectName(res));
            
            mbean77 = registerJ2EEChild(mJ2EEServer.objectName(), meta, intf, implClass, Util.getNameProp(getObjectName(res)));
            synchronized (mConfigRefTo77)
            {
                mConfigRefTo77.put(getObjectName(ref), mbean77);
            }
        }
        catch (final Exception e)
        {
            mLogger.log( Level.INFO, AMXEELoggerInfo.cantRegisterMbean, new Object[] { getObjectName(ref), e });
        }
    //cdebug( "Registered " + child + " for  config resource " + amx.objectName() );
        return mbean77;
    }


    /**
    Listen for registration/unregistration of {@link ResourceRef},
    and associate them with JSR 77 MBeans for this J2EEServer.
    Resources belong to a J2EEServer via ResourceRefs.  So we can stay in the AMX
    world by tracking registration and unregistration of AMX config MBeans of
    type ResourceRef.
     */
    private final class RefListener implements NotificationListener
    {
        public RefListener()
        {
        }
      
        public void handleNotification(final Notification notifIn, final Object handback)
        {
            if (!(notifIn instanceof MBeanServerNotification))
            {
                return;
            }

            final MBeanServerNotification notif = (MBeanServerNotification) notifIn;
            final ObjectName objectName = notif.getMBeanName();
            if ( ! mJ2EEServer.objectName().getDomain().equals(objectName.getDomain()))
            {
                return;
            }
            
            final String type = Util.getTypeProp(objectName);

            if (notif.getType().equals(MBeanServerNotification.REGISTRATION_NOTIFICATION))
            {
                if ( type.equals( mResourceRefType ) )
                {
                    mLogger.fine("New ResourceRef MBEAN registered: " + objectName);
                    final ResourceRef ref = (ResourceRef) ConfigBeanRegistry.getInstance().getConfigBean(objectName);
                    processResourceRef(ref);
                }
                else if ( type.equals( mApplicationRefType ) )
                {
                    mLogger.fine( "NEW ApplicationRef MBEAN registered: " + objectName);
                    final ApplicationRef ref = (ApplicationRef) ConfigBeanRegistry.getInstance().getConfigBean(objectName);
                    processApplicationRef(ref);
                }
            }
            else if (notif.getType().equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION))
            {
                // determine if it's a config for which a JSR 77  MBean is registered
                synchronized (mConfigRefTo77)
                {
                    final ObjectName mbean77 = mConfigRefTo77.remove(objectName);
                    if (mbean77 != null)
                    {
                        mLogger.fine( "Unregistering MBEAN for ref: " + objectName);
                        try
                        {
                            mMBeanServer.unregisterMBean(mbean77);
                        }
                        catch (final Exception e)
                        {
                            mLogger.log( Level.WARNING, AMXEELoggerInfo.cantUnregisterMbean, objectName);
                            mLogger.log( Level.WARNING, null, e);
                        }
                    }
                }
            }
        }

        public void startListening()
        {
            // important: processResourceRef a listener *first* so that we don't miss anything
            try
            {
                mMBeanServer.addNotificationListener(JMXUtil.getMBeanServerDelegateObjectName(), this, null, null);
            }
            catch (final JMException e)
            {
                throw new RuntimeException(e);
            }

            // register all existing 
            final List<ResourceRef> resourceRefs = mServer.getResourceRef();
            for (final ResourceRef ref : resourceRefs)
            {
                processResourceRef(ref);
            }
        }

        public void stopListening()
        {
            try
            {
                mMBeanServer.removeNotificationListener(JMXUtil.getMBeanServerDelegateObjectName(), this);
            }
            catch (final JMException e)
            {
                throw new RuntimeException(e);
            }
        }

    }
}





















