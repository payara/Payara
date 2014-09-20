/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.mejb;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;
import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.management.*;
import javax.management.j2ee.ListenerRegistration;

import org.glassfish.external.amx.AMXGlassfish;

/**
 * @ejbHome <{org.glassfish.admin.mejb.MEJBHome}>
 * @ejbRemote <{org.glassfish.admin.mejb.MEJB}>
 */
public final class MEJBBean implements SessionBean
{
    private static final boolean debug = true;
    private static void debug( final String s ) { if ( debug ) { System.out.println(s); } }

    private volatile SessionContext ctx;
    private final MBeanServer mbeanServer = MEJBUtility.getInstance().getMBeanServer();
    private volatile String mDomain = null;

    public MEJBBean()
    {
    }
    
    public void setSessionContext(SessionContext context) {
        ctx = context;
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
    }
    
    public void ejbRemove() {
    }

    public void ejbCreate() throws CreateException {
        final ObjectName domainRoot = AMXGlassfish.DEFAULT.bootAMX(mbeanServer);
        
        if ( domainRoot == null )
        {
            throw new IllegalStateException( "Impossible: DomainRoot is null" );
        }
        
        mDomain = domainRoot.getDomain();
    }

    /**
        Restrict access to only JSR 77 MBeans
     */
        private Set<ObjectName>
    restrict( final Set<ObjectName> candidates )
    {
        final Set<ObjectName>  allowed = new HashSet<ObjectName>();
        for( final ObjectName candidate : candidates )
        {
            if ( oneOfOurs(candidate) )
            {
                allowed.add(candidate);
            }
        }
        return allowed;
    }

    private boolean oneOfOurs( final ObjectName candidate ) {
        return candidate != null &&
            candidate.getDomain().equals(mDomain) &&
            candidate.getKeyProperty( "j2eeType" ) != null  &&
            candidate.getKeyProperty( "name" ) != null;
    }

    private ObjectName bounce(final ObjectName o) throws InstanceNotFoundException
    {
        if ( ! oneOfOurs(o) )
        {
            throw new InstanceNotFoundException( "" + o );
        }
        return o;
    }
    
    // javax.management.j2ee.Management implementation starts here
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws RemoteException {
        try {
            return restrict( mbeanServer.queryNames(name, query) );
        } catch (Exception ex) {
            throw new RemoteException(this.toString() + "::queryNames", ex);
        }
    }

    public boolean isRegistered(ObjectName name) throws RemoteException {
        try {
            return mbeanServer.isRegistered(name);
        } catch (Exception ex) {
            throw new RemoteException(this.toString() + "::isRegistered", ex);
        }
    }

    public Integer getMBeanCount() throws RemoteException {
        try {
            final ObjectName pattern = new ObjectName( mDomain + ":*" );
            return queryNames( pattern, null ).size();
        } catch (Exception ex) {
            throw new RemoteException(this.toString() + "::getMBeanCount", ex);
        }
    }

    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException,
            IntrospectionException, ReflectionException, RemoteException {
        return mbeanServer.getMBeanInfo( bounce(name) );
    }

    public Object getAttribute(ObjectName name, String attribute) throws MBeanException,
            AttributeNotFoundException, InstanceNotFoundException,
            ReflectionException, RemoteException {
        //debug( "MEJBBean.getAttribute: " + attribute + " on " + name );
        return mbeanServer.getAttribute( bounce(name) , attribute);
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes)
            throws InstanceNotFoundException, ReflectionException, RemoteException {
        //debug( "MEJBBean.getAttributes: on " + name );
        return mbeanServer.getAttributes( bounce(name), attributes);
    }

    public void setAttribute(ObjectName name, Attribute attribute)
            throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException,
            ReflectionException, RemoteException {
        mbeanServer.setAttribute( bounce(name), attribute);
    }

    public AttributeList setAttributes(ObjectName name, AttributeList attributes)
            throws InstanceNotFoundException, ReflectionException, RemoteException {
        return mbeanServer.setAttributes(name, attributes);
    }

    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
            throws InstanceNotFoundException, MBeanException,
            ReflectionException, RemoteException {
        return mbeanServer.invoke( bounce(name), operationName, params, signature);
    }

    /**
     * Returns the default domain used for naming the managed object.
     * The default domain name is used as the domain part in the ObjectName
     * of managed objects if no domain is specified by the user.
     */
    public String getDefaultDomain() throws RemoteException {
        return mDomain;
    }

    public ListenerRegistration getListenerRegistry() throws RemoteException {
        return MEJBUtility.getInstance().getListenerRegistry();
    }
}
