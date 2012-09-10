/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.String;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.j2ee.Management;
import javax.management.j2ee.ManagementHome;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.impl.mbean.AMXImplBase;
import org.glassfish.admin.amx.impl.util.InjectedValues;
import org.glassfish.admin.amx.impl.util.ObjectNameBuilder;
import org.glassfish.admin.amx.j2ee.J2EEManagedObject;
import org.glassfish.admin.amx.j2ee.J2EEServer;
import org.glassfish.admin.amx.j2ee.StateManageable;
import org.glassfish.admin.amx.util.SetUtil;
import org.glassfish.admin.amx.util.StringUtil;
import org.glassfish.admin.amx.util.jmx.JMXUtil;

import org.glassfish.admin.amx.j2ee.J2EETypes;
import static org.glassfish.admin.amx.j2ee.J2EETypes.*;



/**
 */
public abstract class J2EEManagedObjectImplBase extends AMXImplBase {

    protected final long mStartTime;
    private final boolean mStateManageable;
    private final boolean mStatisticsProvider;
    private final boolean mEventProvider;

    private final Metadata mMeta;

    public J2EEManagedObjectImplBase(
            final ObjectName parentObjectName,
            final Metadata meta,
            final Class<? extends J2EEManagedObject> intf) {
        this(parentObjectName, meta, intf, false, false, false);
    }

    public J2EEManagedObjectImplBase(
            final ObjectName parentObjectName,
            final Metadata meta,
            final Class<? extends J2EEManagedObject> intf,
            final boolean stateManageable,
            final boolean statisticsProvider,
            final boolean evengProvider) {
        super(parentObjectName, intf);

        mMeta =meta;

        mStateManageable = stateManageable;
        mStatisticsProvider = statisticsProvider;
        mEventProvider = evengProvider;

        mStartTime = System.currentTimeMillis();
    }

    protected MetadataImpl defaultChildMetadata()
    {
        final Map<String,Object> meta = new HashMap<String,Object>();
        meta.put( Metadata.PARENT, this);
        final MetadataImpl impl = new MetadataImpl(meta);

        return impl;
    }

    protected Metadata metadata()
    {
        return mMeta;
    }
    
    
    public ObjectName getCorrespondingConfig()
    {
        return metadata().getCorrespondingConfig();
    }

    protected Domain
    getDomain()
    {
        final Domain domain = InjectedValues.getInstance().getHabitat().getService(Domain.class);
        return domain;
    }
    
    /**
        JSR 77 requires an ancestor hierarchy via properties; this is in addition
        to the basic AMX requirements.
     */
    @Override
		protected  ObjectName
	preRegisterModifyName(
		final MBeanServer	server,
		final ObjectName	nameIn )
	{
        final String props = getExtraObjectNameProps(server,nameIn);
        if ( props == null || props.length() == 0)
        {
            return nameIn;
        }

        return JMXUtil.newObjectName( nameIn.toString() + "," + props );
	}

    /** types that require a J2EEApplication ancestor, even if null */
    private static final Set<String> REQUIRES_J2EE_APP	=
        SetUtil.newUnmodifiableStringSet(
            WEB_MODULE,
            RESOURCE_ADAPTER_MODULE,
            APP_CLIENT_MODULE,
            STATEFUL_SESSION_BEAN,
            STATELESS_SESSION_BEAN,
            ENTITY_BEAN,
            MESSAGE_DRIVEN_BEAN,
            SERVLET,
            RESOURCE_ADAPTER
        );
   /** the required null J2EEApplication ancestor property */
   private static final String NULL_APP_PROP = Util.makeProp( J2EE_APPLICATION, null);

    /**
     * Deals with the special-case requirements of JSR 77: ancestor properties as well as
     * some types require a J2EEApplication=null
     * @param server
     * @param nameIn
     * @return
     */
    protected String getExtraObjectNameProps(final MBeanServer server, final ObjectName nameIn)
    {
        final String type =  Util.getTypeProp(nameIn);
        String props = Util.makeProp( J2EETypes.J2EE_TYPE_KEY,type );

        // now add ancestors, per JSR 77 spec
        // skip the first two: DomainRoot and  J2EEDomain
        final List<ObjectName> ancestors = ObjectNameBuilder.getAncestors(server, getParent());
        boolean foundApp = false;
        for( int i = 2; i < ancestors.size(); ++ i)
        {
            final ObjectName ancestor = ancestors.get(i);
            final String ancestorType = ancestor.getKeyProperty(J2EETypes.J2EE_TYPE_KEY);
            final String ancestorName = Util.unquoteIfNeeded(ancestor.getKeyProperty(J2EETypes.NAME_KEY));
            final String ancestorProp = Util.makeProp( ancestorType, Util.quoteIfNeeded(ancestorName ));
            props = Util.concatenateProps( props, ancestorProp);

            if ( ancestorType.equals(J2EE_APPLICATION) )
            {
                foundApp = true;
            }
        }

        // special case demanded by JSR 77 spec: standalone modules require J2EEApplication=null
        if ( REQUIRES_J2EE_APP.contains(type) && (! foundApp)   )
        {
            props = Util.concatenateProps(props, NULL_APP_PROP );
        }

        return props;
    }


    protected String j2eeType(final ObjectName objectName) {
        return objectName.getKeyProperty("j2eeType");
    }

    public long getstartTime() {
        return mStartTime;
    }

    protected String getServerName() {
        return (getObjectName().getKeyProperty("J2EEServer"));
    }

    public String getobjectName() {
        return getObjectName().toString();
    }

    public boolean isstatisticsProvider() {
        return mEventProvider;
    }

    public boolean iseventProvider() {
        return mStatisticsProvider;
    }

    public boolean isstateManageable() {
        return mStateManageable;
    }

    public J2EEServer getJ2EEServer() {
        return getProxyFactory().getProxy(getServerObjectName(), J2EEServer.class);
    }

    public ObjectName getServerObjectName() {
        final String serverType = Util.deduceType(J2EEServer.class);
        return getAncestorByType(serverType);
    }

    private static final Set<String> DEPLOYED_TYPES	= SetUtil.newUnmodifiableStringSet(
        J2EE_APPLICATION,
        WEB_MODULE,
        EJB_MODULE,
        APP_CLIENT_MODULE,
        RESOURCE_ADAPTER_MODULE
        );
     
    public String[] getdeployedObjects() {
        return getChildrenAsStrings( DEPLOYED_TYPES );
    }
    
    public int getstate()
    {
        return StateManageable.STATE_STOPPED;
    }


    protected String[] getChildrenAsStrings( final Set<String> types )
    {
        final ObjectName[] children = getChildren(types);
        return StringUtil.toStringArray( children );
    }


    protected String[] getChildrenAsStrings( final String... args )
    {
        if ( args.length == 0 )
        {
            throw new IllegalArgumentException( "Must specify at least one child" );
        }
        
        final Set<String> types = SetUtil.newStringSet(args);
        return getChildrenAsStrings(types);
    }

    public final Management getMEJB() {
        Management mejb = null;
        try {
            final Context ic = new InitialContext();
            final String ejbName = System.getProperty("mejb.name", "ejb/mgmt/MEJB");
            final Object objref = ic.lookup(ejbName);
            final ManagementHome home = (ManagementHome) PortableRemoteObject.narrow(objref, ManagementHome.class);
            mejb = home.create();
        } catch (Exception ex) {
            throw new RuntimeException("Can't find MEJB", ex);
        }
        return mejb;
    }

}







