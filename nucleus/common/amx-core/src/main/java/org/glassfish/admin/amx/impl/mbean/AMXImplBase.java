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
package org.glassfish.admin.amx.impl.mbean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.*;
import org.glassfish.admin.amx.base.DomainRoot;
import org.glassfish.admin.amx.base.MBeanTrackerMBean;
import org.glassfish.admin.amx.base.Utility;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.AMX_SPI;
import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.core.proxy.ProxyFactory;
import org.glassfish.admin.amx.impl.AMXStartupService;
import org.glassfish.admin.amx.impl.util.ImplUtil;
import org.glassfish.admin.amx.impl.util.MBeanInfoSupport;
import org.glassfish.admin.amx.impl.util.ObjectNameBuilder;
import org.glassfish.admin.amx.util.*;
import org.glassfish.admin.amx.util.jmx.AttributeChangeNotificationBuilder;
import org.glassfish.admin.amx.util.jmx.JMXUtil;
import org.glassfish.admin.amx.util.jmx.stringifier.AttributeChangeNotificationStringifier;
import org.glassfish.admin.amx.util.stringifier.SmartStringifier;
import org.glassfish.external.amx.AMX;

/**
Base class from which all AMX MBeans should derive (but not "must").
<p>
Note that even though this base class implements a number of interfaces,
the actual MBean interface supplied by the subclass construction-time
determines which of these is actually exposed in the MBeanInfo.
<p>
A subclass should generally <b>not</b> implement get/setAttribute(s) as these
calls are processed in this base class--
<p>
If a subclass implements a getter or setter Method it will be invoked automatically.
If there is no getter or setter Method, then the getAttributeManually() or
setAttributeManually() methods will be invoked; the subclass should implement
these methods instead.
<p>
Method invocation is also handled automatically. If a Method cannot be found,
the invokeManually() method is called; the subclass should implement this method.
<p>
Note that various optimizations are possible, but not implemented. These include
caching Methods for each Attribute and for operations as well.  Careful testing
should be done before complicating the code with such optimizations.
 */
public class AMXImplBase extends MBeanImplBase
        implements DynamicMBean, NotificationEmitter, AMX_SPI {

    /** console debug */
    protected static void cdebug(final String s) {
        System.out.println(s);
    }

    protected static final String GET = "get";
    protected static final String SET = "set";
    private final ObjectName mParent;
    // subclasses should set this value
    protected volatile MBeanInfo mMBeanInfo;
    /** Whether AttributeChangeNotifications aree mitted. */
    private final boolean mEmitAttributeChangeNotifications = true;

    private static final Logger logger = AMXLoggerInfo.getLogger();

    public AMXImplBase(
            final ObjectName parentObjectName,
            final Class<? extends AMX_SPI> intf) {
        this(parentObjectName, MBeanInfoSupport.getMBeanInfo(intf));
    }


    public AMXImplBase(final ObjectName parentObjectName) {
        this(parentObjectName, (MBeanInfo) null);
    }


    public AMXImplBase(
            final ObjectName parentObjectName,
            final MBeanInfo mbeanInfo) {
        super();

        mParent = parentObjectName;
        mMBeanInfo = mbeanInfo; // might be null
    }


    public MBeanInfo getMBeanInfo() {
        return mMBeanInfo;
    }


    protected final boolean shouldEmitNotifications() {
        return (mEmitAttributeChangeNotifications && getListenerCount() != 0);
    }
    private static final MBeanNotificationInfo[] EMPTY_NOTIFICATIONS = new MBeanNotificationInfo[0];

    public MBeanNotificationInfo[] getNotificationInfo() {
        return (EMPTY_NOTIFICATIONS);
    }

    protected ProxyFactory getProxyFactory() {
        //return( ProxyFactory.getInstance( mConnectionSource, true ) );
        return (ProxyFactory.getInstance(getMBeanServer()));
    }


    protected <T extends AMXProxy> T getProxy(final ObjectName objectName, final Class<T> intf) {
        return getProxyFactory().getProxy(getObjectName(), intf);
    }


    protected final <T extends AMXProxy> T getSelf(final Class<T> intf) {
        return getProxyFactory().getProxy(getObjectName(), intf);
    }


    protected AMXProxy getSelf() {
        return getProxyFactory().getProxy(getObjectName(), AMXProxy.class);
    }


    public final ObjectName getParent() {
        return mParent;
    }


    protected ObjectName getAncestorByType(final String type) {
        return Util.getAncestorByType(getMBeanServer(), getObjectName(), type);
    }


    public final AMXProxy getParentProxy() {
        final ObjectName parent = getParent();
        return parent == null ? null : getProxyFactory().getProxy(parent, AMXProxy.class);
    }


    public MBeanTrackerMBean getMBeanTracker() {
        return AMXStartupService.getMBeanTracker(getMBeanServer());
    }


    public ObjectName[] getChildren() {
        final Set<ObjectName> children = getMBeanTracker().getChildrenOf(getObjectName());
        if (children == null) {
            return null;
        }

        return CollectionUtil.toArray(children, ObjectName.class);
    }


    public ObjectName[] getChildren(final Class<?> clazz) {
        return getChildren(Util.deduceType(clazz));
    }


    public ObjectName[] getChildren(final String type) {
        return getChildren(SetUtil.newSingletonSet(type));
    }


    public ObjectName[] getChildren(final Set<String> types) {
        final ObjectName[] children = getChildren();

        final Set<ObjectName> matching = new HashSet<ObjectName>();

        for (final ObjectName child : children) {
            if (types.contains(Util.getTypeProp(child))) {
                matching.add(child);
            }
        }
        return CollectionUtil.toArray(matching, ObjectName.class);
    }


    public Map<String, ObjectName> getChildrenMap(final String type) {
        return Util.filterByType(getChildren(), type);
    }


    protected boolean supportsChildren() {
        // return getSelf().extra().subTypes() != null;
        return true;
    }


    protected ObjectName child(final String type, final String name) {
        final Map<String, ObjectName> c = getChildrenMap(type);
        return c.get(name);
    }


    protected ObjectName child(final String type) {
        final Collection<ObjectName> c = getChildrenMap(type).values();
        final int num = c.size();
        if (num > 1) {
            throw new IllegalArgumentException("More than one child of type " + type);
        }
        return num == 0 ? null : c.iterator().next();
    }


    protected ObjectName child(final Class<?> intf) {
        final String type = Util.deduceType(intf);
        //System.out.println( "child: deduceType = " + type );
        return child(type);
    }


    protected static boolean isUtilityMBean(final Class mbeanInterface) {
        return (Utility.class.isAssignableFrom(mbeanInterface));
    }


    public final Set<String> getAttributeNames() {
        return getAttributeInfos().keySet();
    }


    /**
    An operation has not been implemented. Deal with appropriately.
     */
    protected final void unimplementedOperation(final String operation) {
        final String msg = "UNIMPLEMENTED OPERATION: " + operation + " in " + getObjectName();

        logInfo(msg);

        throw new UnsupportedOperationException(operation);
    }


    /**
    An Attribute has not been implemented.
     */
    protected final Object unimplementedAttribute(final String attrName) {
        final String msg = "UNIMPLEMENTED ATTRIBUTE: " + attrName + " in " + getObjectName();
        logInfo(msg);

        return (null);
    }


    /**
    The impossible has happened.
     */
    protected final void impossible(final Throwable t) {
        logSevere("AMXImplBase.impossible: " + t.getMessage());
        assert (false);
        throw new RuntimeException(t);
    }


    protected Object getAttributeNoThrow(String name) {
        Object result = null;

        try {
            result = getAttribute(name);
        } catch (Exception e) {
            throw new RuntimeException(new ThrowableMapper(e).map());
        }
        return (result);
    }


    protected synchronized Map<String, MBeanAttributeInfo> getAttributeInfos() {
        return JMXUtil.attributeInfosToMap(getMBeanInfo().getAttributes());
    }


    protected MBeanAttributeInfo getAttributeInfo(final String name) {
        return getAttributeInfos().get(name);
    }


    protected boolean isReadOnlyAttribute(final String name) {
        final MBeanAttributeInfo info = getAttributeInfo(name);

        return info == null ? true : !info.isWritable();
    }


    public Logger getLogger() {
        return AMXLoggerInfo.getLogger();
    }


    /**
    Get an Attribute value, first by looking for a getter method
    of the correct name and signature, then by looking for a delegate,
    and finally by calling getAttributeManually(), which a subclass
    is expected to override.

    @param name	name of the Attribute
    @return value of the Attribute
     */
    public final Object getAttribute(final String name)
            throws AttributeNotFoundException {
        Object result = null;

        if (name == null) {
            throw new AttributeNotFoundException("Illegal/unknown attribute: null for " + getObjectName());
        }

        try {
            result = getAttributeInternal(name);
        } catch (AttributeNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new AttributeNotFoundException(name);
        }

        return (result);
    }


    protected boolean attributeTypeMatches(final String attributeName, final Class<?> clazz) {
        boolean matches = false;
        final MBeanAttributeInfo info = getAttributeInfo(attributeName);
        if (info != null) {
            if (clazz.getName().equals(info.getType())) {
                matches = true;
            }
        }

        return matches;
    }


    protected Object getAttributeInternal(final String name)
            throws AttributeNotFoundException, ReflectionException, MBeanException {
        Object result = null;
        boolean handleManually = false;
        //cdebug( "AMXImplBase.getAttributeInternal: " + name );

        // A getter always takes priority
        final Method m = findGetter(name);
        if (m != null) {
            //cdebug( "getAttributeInternal: found getter method for: " + name );
            result = getAttributeByMethod(name, m);
            //debug( "getAttribute: " + name + " CALLED GETTER: " + m + " = " + result);
        } else if (attributeTypeMatches(name, ObjectName.class)) {
            result = getObjectNameAttribute(name);
        } else if (attributeTypeMatches(name, ObjectName[].class)) {
            result = getObjectNamesForAttribute(name);
        } else {
            result = getAttributeManually(name);
        }

        return (result);
    }


    /**
    Bulk get.  Note that is is important for this implementation to
    call getAttribute() for each name so that each may be processed
    appropriately; some Attributes may be in this MBean itself.

    @param names	array of Attribute names
    @return AttributeList of Attributes successfully fetched
     */
    public AttributeList getAttributes(String[] names) {
        trace("AMXImplBase.getAttributes: " + SmartStringifier.toString(names));

        final AttributeList attrs = new AttributeList();

        for (int i = 0; i < names.length; ++i) {
            try {
                trace("%%% calling getAttribute: " + names[i] + " on " + getObjectName());
                final Object value = getAttribute(names[i]);
                // System.out.println ( "getAttributes: " + names[i] + " = " + value  );
                attrs.add(new Attribute(names[i], value));
            } catch (Exception e) {
                //System.out.println( "### AttributeNotFoundException: " + names[ i ] + " for " + getObjectName() );
                // ignore, as per spec
            }
        }
        return (attrs);
    }


    private final void rethrowAttributeNotFound(
            final Throwable t,
            final String attrName)
            throws AttributeNotFoundException {
        final Throwable rootCause = ExceptionUtil.getRootCause(t);
        if(rootCause.getMessage() != null && rootCause.getMessage().contains("Failed to retrieve RMIServer stub:")) {
            trace("Attribute could not be found at a remote server. This is most likely due to a clustered or remote instance being down");
        } else  if (rootCause instanceof AttributeNotFoundException) {
            throw (AttributeNotFoundException) rootCause;
        }

        /* final String msg = "Attribute not found: " + StringUtil.quote(attrName) +
                " of MBean " + getObjectName() + "[" + rootCause.getMessage() + "]";
        ;
        throw new AttributeNotFoundException(msg); */
    }


    /**
    Set an Attribute by invoking the supplied method.
     */
    protected Object getAttributeByMethod(final String attrName, final Method m)
            throws AttributeNotFoundException {
        Object result = null;

        try {
            //trace( "getAttributeByMethod: " + attrName  );
            result = m.invoke(this, (Object[]) null);
        } catch (InvocationTargetException e) {
            trace("InvocationTargetException: " + attrName + " by " + m);
            rethrowAttributeNotFound(ExceptionUtil.getRootCause(e), attrName);
        } catch (IllegalAccessException e) {
            trace("ILLEGAL ACCESS TO: " + attrName + " by " + m);
            rethrowAttributeNotFound(ExceptionUtil.getRootCause(e), attrName);
        } catch (Exception e) {
            trace("Exception: " + attrName + " by " + m);
            rethrowAttributeNotFound(ExceptionUtil.getRootCause(e), attrName);
        }

        return (result);
    }


    protected void setAttributeByMethod(final Attribute attr, final Method m)
            throws AttributeNotFoundException, InvalidAttributeValueException {
        try {
            // trace( "setAttributeByMethod: " + m );
            m.invoke(this, new Object[]{attr.getValue()});
        } catch (InvocationTargetException e) {
            trace("setAttributeByMethod: InvocationTargetException: " + e);

            final Throwable t = ExceptionUtil.getRootCause(e);
            if (t instanceof InvalidAttributeValueException) {
                throw (InvalidAttributeValueException) t;
            }

            rethrowAttributeNotFound(e, attr.getName());
        } catch (IllegalAccessException e) {
            trace("setAttributeByMethod: IllegalAccessException: " + e);
            rethrowAttributeNotFound(e, attr.getName());
        } catch (Exception e) {
            trace("setAttributeByMethod: Exception: " + e);
            rethrowAttributeNotFound(e, attr.getName());
        }
    }


    /** Supply possible types to be tried based on attribute name */
    protected String[] attributeNameToType(final String attributeName) {
        return new String[]{attributeName, Util.typeFromName(attributeName)};
    }

    protected static final ObjectName[] EMPTY_OBJECT_NAMES = new ObjectName[0];


    /** get child ObjectNameBuilder corresponding to the AttributeName  */
    protected ObjectName[] getObjectNamesForAttribute(final String attributeName) {
        final String[] types = attributeNameToType(attributeName);

        ObjectName[] result = null;
        Map<String, ObjectName> children = null;
        for (final String type : types) {
            children = getChildrenMap(type);
            if (children.keySet().size() != 0) {
                break;
            }
        }

        if (children == null || children.keySet().size() == 0) {
            result = EMPTY_OBJECT_NAMES;
        } else {
            result = new ObjectName[children.keySet().size()];
            children.values().toArray(result);
        }

        return result;
    }


    /** get child ObjectName corresponding to the AttributeName  */
    protected ObjectName getObjectNameAttribute(final String attributeName) {
        final String[] types = attributeNameToType(attributeName);
        //cdebug( "getObjectNameAttribute: " + attributeName + ", look for types: " + StringUtil.toString(types) );

        ObjectName result = null;
        for (final String type : types) {
            if ((result = child(type)) != null) {
                break;
            }
        }

        //cdebug( "getObjectNameAttribute: " + attributeName + " = " + result);
        return result;
    }


    /**
    Subclasses should override this to handle getAttribute( attrName ). It will
    be called if no appropriate getter is found.	*/
    protected Object getAttributeManually(final String attributeName)
            throws AttributeNotFoundException, ReflectionException, MBeanException {
        throw new AttributeNotFoundException(attributeName);
    }


    /**
    Subclasses should override this to handle setAttribute( attr ). It will
    be called if no appropriate setter is found.
     */
    protected void setAttributeManually(final Attribute attr)
            throws AttributeNotFoundException, InvalidAttributeValueException {
        throw new AttributeNotFoundException(attr.getName());
    }


    public void setAttribute(final Attribute attr)
            throws AttributeNotFoundException, InvalidAttributeValueException {
        final String name = attr.getName();

        if (isReadOnlyAttribute(name)) {
            throw new IllegalArgumentException("Attribute is read-only: " + attr.getName());
        }

        boolean failure = true;

        try {
            setAttributeInternal(attr);
            failure = false;
        } catch (AttributeNotFoundException e) {
            throw e;
        } catch (InvalidAttributeValueException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
    Set an Attribute value, first by looking for a setter method
    of the correct name and signature, then by looking for a delegate,
    and finally by calling setAttributeManually(), which a subclass
    is expected to override.

    @param attr	the Attribute
     */
    protected void setAttributeInternal(final Attribute attr)
            throws AttributeNotFoundException, InvalidAttributeValueException,
            ReflectionException, MBeanException {
        trace("setAttribute: " + attr.getName() + " = " + attr.getValue());

        boolean handleManually = false;
        final Method m = findSetter(attr);

        boolean shouldEmitNotifications = shouldEmitNotifications();
        // note that this will fail if an Attribute is write-only
        final Object oldValue = shouldEmitNotifications ? getAttribute(attr.getName()) : null;

        if (m != null) {
            setAttributeByMethod(attr, m);
        } else {
            setAttributeManually(attr);
        }

        if (shouldEmitNotifications) {
            final String attrType = getAttributeType(attr.getName());

            sendAttributeChangeNotification("", attr.getName(), attrType, System.currentTimeMillis(), oldValue, attr.getValue());
        }
    }

    protected final String getAttributeType(final String attrName) {
        final String amxName = attrName;

        final MBeanAttributeInfo info =
                JMXUtil.getMBeanAttributeInfo(getMBeanInfo(), amxName);

        // attributes might be illegal names...
        if (info == null) {
            logWarning("getAttributeType: unknown attribute: " + attrName);
        }

        return (info == null ? String.class.getName() : info.getType());
    }

    protected synchronized void sendAttributeChangeNotification(
            final String msg,
            final String name,
            final String attrType,
            final long when,
            final Object oldValue,
            final Object newValue) {
        //
        // do not send a Notification when nothing has changed
        //
        if (oldValue != null && !oldValue.equals(newValue)) {
            final AttributeChangeNotificationBuilder builder =
                    (AttributeChangeNotificationBuilder) getNotificationBuilder(AttributeChangeNotification.ATTRIBUTE_CHANGE);

            final AttributeChangeNotification n =
                    builder.buildAttributeChange(msg, name, attrType, when, oldValue, newValue);

            //System.out.println("AttributeChangeNotification: " + AttributeChangeNotificationStringifier.DEFAULT.stringify(n));
            logger.log(Level.INFO, AMXLoggerInfo.attributeChangeNotification, AttributeChangeNotificationStringifier.DEFAULT.stringify(n));
            sendNotification(n);
        }
    }

    /**
    Note that the default implementation sets attributes one at a time, but that
    MBeans with transactional requirements (eg configuration) may wish to set them as a group.
     */
    public AttributeList setAttributes(final AttributeList attrs) {
        final AttributeList successList = new AttributeList();

        for (int i = 0; i < attrs.size(); ++i) {
            final Attribute attr = (Attribute) attrs.get(i);
            try {
                setAttribute(attr);
                successList.add(attr);
            } catch (Exception e) {
                // ignore, as per spec
                debug(ExceptionUtil.toString(e));
            }
        }
        return (successList);
    }

    /**
    Find a method.

    @param methodName
    @param sig
    @return a Method or null if not found
     */
    protected final Method findMethod(String methodName, final Class[] sig) {
        return (ClassUtil.findMethod(this.getClass(), methodName, sig));
    }
    /**
    Find a getXXX() method that matches the Attribute

    @param name the name to which "get" will be prepended
    @return a Method or null if not found
     */
    static private final Class[] GETTER_SIG = new Class[0];

    protected final Method findGetter(String name) {
        final String methodName = GET + name;

        Method m = findMethod(methodName, GETTER_SIG);
        if (m == null) {
            m = findMethod("is" + name, GETTER_SIG);
        }

        return (m);
    }

    /**
    Find a setXXX() method that matches the Attribute.

    @param attr	an Attribute for which a matching setter should be located
    @return a Method or null if not found
     */
    protected final Method findSetter(final Attribute attr) {
        final Object value = attr.getValue();
        Class valueClass = null;
        if (value == null) {
            final MBeanAttributeInfo info = getAttributeInfos().get(attr.getName());
            if (info != null) {
                try {
                    valueClass = ClassUtil.getClassFromName(info.getType());
                } catch (Exception e) {
                }
            }
        } else {
            valueClass = value.getClass();
        }

        if (valueClass == null) {
            return null;
        }

        final String methodName = SET + attr.getName();
        Class[] sig = new Class[]{valueClass};
        Method setter = findMethod(methodName, sig);

        final Class primitiveClass = ClassUtil.objectClassToPrimitiveClass(valueClass);
        if (setter == null && primitiveClass != valueClass) {
            //trace( "findSetter: retrying for primitive class: " + primitiveClass );
            // the Attribute value is always an object.  But it may be
            // that the setter takes a primitive type.  So for example,
            // the Attribute may contain a value of type Boolean, but the setter
            // may required type boolean
            sig[ 0] = primitiveClass;
            setter = findMethod(methodName, sig);
        }

        return (setter);
    }
    protected static final String GET_PREFIX = "get";
    protected static final String OBJECT_NAME_SUFFIX = "ObjectName";
    // protected static final String OBJECT_NAME_MAP_SUFFIX    = "ObjectNameMap";

    protected boolean operationNameMatches(
            final String operationName,
            final String prefix,
            final String suffix) {
        return operationName.startsWith(prefix) &&
                operationName.endsWith(suffix);
    }

    protected boolean getterNameMatches(
            final String operationName,
            final String suffix) {
        return operationNameMatches(operationName, GET_PREFIX, suffix);
    }

    protected void handleException(final Exception e)
            throws MBeanException, ReflectionException {
        final ThrowableMapper mapper = new ThrowableMapper(e);
        final Throwable mapped = mapper.map();

        if (mapped instanceof ReflectionException) {
            throw (ReflectionException) mapped;
        } else if (mapped instanceof MBeanException) {
            throw (MBeanException) mapped;
        } else if (!(mapped instanceof Exception)) {
            // wrap the Throwable in an Exception
            final Exception wrapper = new Exception(mapped);
            throw new MBeanException(wrapper);
        } else {
            throw new MBeanException((Exception) mapped);
        }
    }

    protected void handleGetAttributeException(final Exception e)
            throws MBeanException, ReflectionException, AttributeNotFoundException {
        if (e instanceof AttributeNotFoundException) {
            // AttributeNotFoundException can never contain anything non-standard
            throw (AttributeNotFoundException) e;
        } else {
            handleException(e);
        }
    }

    protected void handleInvokeThrowable(final Exception e)
            throws MBeanException, ReflectionException {
        handleException(e);
    }

    /**
    Generic handling of invoke(). Converts the types[] to a Class[], then attempts
    to locate a suitable Method.  If a suitable Method is found, it is invoked.
    If not found the subclass is expected to handle it in invokeManually();
     */
    public final Object invoke(
            String operationName,
            Object[] args,
            String[] types)
            throws MBeanException, ReflectionException {
        Object result = null;
        boolean unimplemented = false;
        final int numArgs = args != null ? args.length : 0;
        //cdebug("invoke: " + operationName + ", num args = " + numArgs );

        try {
            final Class[] signature = ClassUtil.signatureFromClassnames(types);
            final Method m = findMethod(operationName, signature);
            if (m != null) {
                debugMethod("invoking method: " + operationName, args);
                result = m.invoke(this, args);
            } else if (operationName.equals("toString") && numArgs == 0) {
                result = toString();
            } else {
                //cdebug( "No method found for " + operationName );
                result = invokeManually(operationName, args, types);
            }
        } catch (Exception e) {
            debug(ExceptionUtil.toString(e));
            handleInvokeThrowable(e);
        }

        return (result);
    }

    /**
    An operation is being invoked manually, meaning that it is missing as a method.
    invokeManually() will be called only if no appropriate Method is found.
    <p>
    Subclasses may override this to handle invoke(), though usually it's just
    easier to write the appropriate method directly, which will be found and called
    if present.
     */
    protected Object invokeManually(
            String operationName,
            Object[] args,
            String[] types)
            throws MBeanException, ReflectionException, NoSuchMethodException, AttributeNotFoundException {
        throw new NoSuchMethodException("no operation " + operationName +
                toString(types) + " in " + getObjectName());
    }

    /**
    A subclass might need to override this method if its name contains characters
    that are illegal for the ObjectName.
     */
    public String getName() {
        String name = Util.getNameProp(getObjectName());

        // names must not be null, even if they are omitted from ObjectName
        return name == null ? AMX.NO_NAME : name;
    }

    /**
    O the ObjectName by adding to it:
    <ul>
    <li>adding AMX.FULL_TYPE_KEY property</li>
    <li></li>
    </ul>
     */
    protected ObjectName preRegisterModifyName(
            final MBeanServer server,
            final ObjectName nameIn) {
        return nameIn;
    }

    /*
    Note that this method is 'synchronized'--to force visibility of all fields it affects.
    Since it's called only once (per instance) for an MBean Registration, it has no performance
    impact on later use, but guarantees visibility of all non-final instance variables.
     */
    public final synchronized ObjectName preRegister(
            final MBeanServer server,
            final ObjectName nameIn)
            throws Exception {
        final ObjectName nameFromSuper = super.preRegister(server, nameIn);

        //mConnectionSource	= new MBeanServerConnectionSource( server );

        mSelfObjectName = preRegisterModifyName(server, nameFromSuper);
        mSelfObjectName = preRegisterHook(server, mSelfObjectName);

        preRegisterDone();

        return (mSelfObjectName);
    }

    /**
    This is an opportunity for a subclass to do initialization
    and optionally to modify the ObjectName one last time.
     */
    protected ObjectName preRegisterHook(MBeanServer server, final ObjectName selfObjectName)
            throws Exception {
        // subclass may do something
        return selfObjectName;
    }

    protected void preRegisterDone()
            throws Exception {
        debug("AMXImplBase.preRegister() done for: ", getObjectName());
    }

    /** Hook for a subclass when registration is complete */
    protected MBeanInfo postRegisterModifyMBeanInfo(final MBeanInfo info) {
        return info;
    }

    /** Important:  must be synchronized so that preDeregisterHook cannot be called prior to 
    existing postRegisterHook()
     */
    protected synchronized void postRegisterHook(final Boolean registrationSucceeded) {
        if (registrationSucceeded.booleanValue()) {
            mMBeanInfo = postRegisterModifyMBeanInfo(mMBeanInfo);
            registerChildren();
        }
    }

    // hook for subclasses
    protected void registerChildren() {
    }

    /** Important:  must be synchronized so that it cannot be called prior to exiting postRegisterHook() */
    @Override
    protected synchronized void preDeregisterHook()
            throws Exception {
        super.preDeregisterHook();

        unregisterChildren();
    }

    // hook for subclasses
    protected void unregisterChildren() {
        final ObjectName[] children = getChildren();
        for (final ObjectName child : children) {
            try {
                getMBeanServer().unregisterMBean(child);
            } catch (final Throwable t) {
                // note it, and move on, we must unregister remaining ones
                logger.log(Level.INFO, AMXLoggerInfo.unregisterMbean, new Object[] { child, t});
            }
        }
    }

    public final DomainRoot getDomainRootProxy() {
        return (getProxyFactory().getDomainRootProxy(false));
    }

    public final ObjectName getDomainRoot() {
        return getProxyFactory().getDomainRootObjectName();
    }

    protected String stringify(Object o) {
        return (SmartStringifier.toString(o));
    }

    public String toString() {
        return java();
    }

    public String java() {
        return getDomainRootProxy().getTools().java(getObjectName());
    }

    protected ObjectName registerChild(final Object mbean, final ObjectName childObjectName) {
        try {            
            final ObjectName objectName = getMBeanServer().registerMBean(mbean, childObjectName).getObjectName();

            return objectName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected ObjectNameBuilder getObjectNames() {
        return new ObjectNameBuilder(getMBeanServer(), getObjectName());
    }
}








