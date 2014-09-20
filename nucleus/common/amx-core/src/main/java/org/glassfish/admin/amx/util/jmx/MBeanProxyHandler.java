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

package org.glassfish.admin.amx.util.jmx;

import org.glassfish.admin.amx.util.AMXDebug;
import org.glassfish.admin.amx.util.ObjectUtil;
import org.glassfish.admin.amx.util.Output;
import org.glassfish.admin.amx.util.StringUtil;
import org.glassfish.admin.amx.util.ClassUtil;

import javax.management.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
Implementation of a proxy <i>handler</i> that supports Attribute names which are not legal
Java identifiers.  It does so by mapping illegal Java identifiers into legal
names.  Any interface supplied needs to take mapped names into account.
<b>THREAD SAFE, but not clear if parent class
javax.management.MBeanServerInvocationHandler is thread-safe</b>
 */
public class MBeanProxyHandler extends MBeanServerInvocationHandler
{
    protected final static String GET = "get";

    protected final static String SET = "set";

    protected final static String IS = "is";

    protected final static int GET_PREFIX_LENGTH = GET.length();

    protected final static int IS_PREFIX_LENGTH = IS.length();

    /** MBeanInfo we first obtained */
    protected final MBeanInfo mInitialMBeanInfo;

    /** whether the target MBean is valid */
    private volatile boolean mTargetValid;

    /** Sets and Maps are used frequently, keep hash code constant time */
    private final Integer mHashCode;

    protected final Output mDebug;

    @Override
    public int hashCode()
    {
        return ObjectUtil.hashCode(mInitialMBeanInfo, mDebug) ^ ObjectUtil.hashCode(mTargetValid);
    }

    @Override
    public boolean equals(final Object rhs)
    {
        if (rhs == this)
        {
            return true;
        }

        if (!(rhs instanceof MBeanProxyHandler))
        {
            return false;
        }

        final MBeanProxyHandler other = (MBeanProxyHandler) rhs;

        boolean equals = getObjectName().equals(other.getObjectName());
        if (equals)
        {
            try
            {
                equals = getMBeanServerConnection() == other.getMBeanServerConnection();
            }
            catch (Exception e)
            {
                equals = false;
            }
        }

        return equals;
    }

    /**
    Normally created through MBeanProxyFactory.newProxyInstance().  Creates a new instance to be used
    as a <i>handler<i> object for Proxy.newProxyInstance.

    @param conn         the connection
    @param objectName   the ObjectName of the proxied MBean
    @param mbeanInfo    will be fetched if null
     */
    public MBeanProxyHandler(
            final MBeanServerConnection conn,
            final ObjectName objectName,
            final MBeanInfo mbeanInfo)
            throws IOException
    {
        super(conn, objectName);

        mDebug = AMXDebug.getInstance().getOutput(getClass().getName());
        //debugMethod( "MBeanProxyHandler", objectName );
        mTargetValid = true;

        if (mbeanInfo != null)
        {
            mInitialMBeanInfo = mbeanInfo;
        }
        else
        {
            try
            {
                mInitialMBeanInfo = conn.getMBeanInfo(objectName);
            }
            catch (final Exception e)
            {
                throw new RuntimeException("Could not get MBeanInfo for: " + getObjectName(), e);
            }
        }

        mHashCode = this.hashCode();
    }

    public boolean isInvariantMBeanInfo()
    {
        final Object value = mInitialMBeanInfo.getDescriptor().getFieldValue("immutableInfo");
        return Boolean.parseBoolean("" + value);
    }

    public String interfaceName()
    {
        final String value = (String) getMBeanInfo().getDescriptor().getFieldValue("interfaceName");
        return value;
    }

    public final void targetUnregistered()
    {
        debugMethod(getObjectName().toString(), "targetUnregistered");
        mTargetValid = false;
    }

    public final void connectionBad()
    {
        debugMethod("connectionBad");
        mTargetValid = false;
    }

    /** return true if the MBean is local (in process) */
    public boolean isLocal()
    {
        return getMBeanServerConnection() instanceof MBeanServer;
    }

    public final boolean isValid()
    {
        if (mTargetValid)
        {
            try
            {
                mTargetValid = getMBeanServerConnection().isRegistered(getObjectName());
            }
            catch (Exception e)
            {
                debug("checkValid: connection failed");
                mTargetValid = false;
            }
        }
        return (mTargetValid);
    }

    public synchronized Logger getProxyLogger()
    {
        return Logger.getLogger(this.getClass().getName());
    }

    protected String extractAttributeNameFromMethod(String methodName)
    {
        assert (methodName.startsWith(GET) ||
                methodName.startsWith(SET) ||
                methodName.startsWith(IS));
        final int startIndex = methodName.startsWith(GET) || methodName.startsWith(SET) ? GET_PREFIX_LENGTH : IS_PREFIX_LENGTH;
        return (methodName.substring(startIndex, methodName.length()));
    }

    private synchronized MBeanInfo _getMBeanInfo()
            throws IOException,
                   InstanceNotFoundException, ReflectionException, IntrospectionException
    {
        MBeanInfo info = mInitialMBeanInfo;

        if (!isInvariantMBeanInfo())
        {
            info = getMBeanServerConnection().getMBeanInfo(getObjectName());
        }

        return info;
    }

    public MBeanInfo getMBeanInfo()
    {
        try
        {
            return (_getMBeanInfo());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
    Same as XAttributesAccess.getAttribute, but with exceptions
     */
    public Object getAttribute(final String attributeName)
            throws InstanceNotFoundException, ReflectionException,
                   MBeanException, AttributeNotFoundException, IOException
    {
        final Object result =
                getMBeanServerConnection().getAttribute(getObjectName(), attributeName);

        postGetAttributeHook(attributeName, result);

        return (result);
    }

    protected Object getAttributeNoThrow(final String name)
    {
        try
        {
            return getAttribute(name);
        }
        catch (final Exception e)
        {
            throw new RuntimeException("getAttribute failed for " + name, e);
        }
    }

    /**
    Same as XAttributesAccess.getAttributes, but with exceptions
     */
    public AttributeList getAttributes(final String[] attrNames)
            throws IOException, InstanceNotFoundException, ReflectionException
    {
        final AttributeList results =
                getMBeanServerConnection().getAttributes(getObjectName(), attrNames);

        postGetAttributesHook(attrNames, results);

        return (results);
    }

    /**
    Same as XAttributesAccess.setAttribute, but with exceptions
     */
    public void setAttribute(final Attribute attr)
            throws IOException, InstanceNotFoundException, ReflectionException,
                   AttributeNotFoundException, MBeanException, InvalidAttributeValueException
    {
        getMBeanServerConnection().setAttribute(getObjectName(), attr);

        postSetAttributeHook(attr);
    }

    /**
    Same as XAttributesAccess.setAttributes, but with exceptions
     */
    public AttributeList setAttributes(final AttributeList requested)
            throws IOException, InstanceNotFoundException, ReflectionException
    {
        final AttributeList results = getMBeanServerConnection().setAttributes(getObjectName(), requested);

        postSetAttributesHook(requested, results);

        return (results);
    }

    private final String LOG_LEVEL_NAME = "LogLevel";

    protected void postGetAttributeHook(
            final String name,
            final Object value)
    {
    }

    protected void postGetAttributesHook(
            final String[] requested,
            final AttributeList actual)
    {
    }

    protected void postSetAttributeHook(final Attribute attr)
    {
    }

    protected void postSetAttributesHook(
            final AttributeList requested,
            final AttributeList actual)
    {
    }

    /**  JMX direct invoke */
    public Object invoke(String methodName, Object[] params, String[] signature)
            throws InstanceNotFoundException, MBeanException, ReflectionException, IOException
    {
        return getMBeanServerConnection().invoke(getObjectName(), methodName, params, signature);
    }

    /**
    Invoke the specified method.  This implementation supports additional functionality
    over the JMX MBeanServerInvocationHandler:
    (1) It supports mapped Attribute names (ones that are not legal Java names)
    (2) it supports XAttributesAccess, which otherwise does not work correctly
    <p>
    For anything else, the behavior of MBeanServerInvocationHandler is used.
     */
    public Object invoke(
            Object proxy,
            Method method,
            Object[] args)
            throws java.lang.Throwable
    {
        final String methodName = method.getName();
        final int numArgs = args == null ? 0 : args.length;

        debugMethod(method.getName(), args);

        Object result = null;

        final boolean isGetter = JMXUtil.isIsOrGetter(method);
        final boolean isSetter = isGetter ? false : JMXUtil.isSetter(method);

        boolean handled = false;

        if (methodName.equals("getObjectName"))
        {
            handled = true;
            result = getObjectName();
        }
        else if (methodName.equals("getMBeanInfo") && numArgs == 0)
        {
            handled = true;
            result = getMBeanInfo();
        }
        else if ((isGetter || isSetter))
        {
            handled = true;

            final String javaName = extractAttributeNameFromMethod(methodName);

            String attributeName = javaName;

            //trace( "MBeanProxyHandler.invoke: mapped attribute: " + javaName + " => " + attributeName );

            if (isGetter)
            {
                result = getAttribute(attributeName);
            }
            else
            {
                final Attribute attr = new Attribute(attributeName, args[ 0]);
                setAttribute(attr);
            }
        }
        else if (methodName.indexOf("etAttribute") == 1)
        {
            handled = true;

            // likely one of getAttribute(), getAttributes(), setAttribute(), setAttributes()

            //p( "MBeanProxyHandler.invoke: " + method.getName() + " " + numArgs + " args." );
            if (JMXUtil.isGetAttribute(method))
            {
                final String attrName = (String) args[ 0];
                result = getAttribute(attrName);
            }
            else if (JMXUtil.isGetAttributes(method))
            {
                final String[] attrNames = (String[]) args[ 0];
                result = getAttributes(attrNames);
            }
            else if (JMXUtil.isSetAttribute(method))
            {
                final Attribute attr = (Attribute) args[ 0];
                setAttribute(attr);
            }
            else if (JMXUtil.isSetAttributes(method))
            {
                final AttributeList requested = (AttributeList) args[ 0];
                result = setAttributes(requested);
            }
            else
            {
                handled = false;
            }
        }
        else if (methodName.equals("hashCode"))
        {
            /*
            java.lang.reflect.Proxy will route all calls through invoke(),
            even hashCode().  To avoid newing up an Integer every time,
            just return a stored version.  hashCode() is called frequently
            when proxies are inserted into Sets or Maps.  toString() and
            equals() don't seem to get called however.
             */
            result = mHashCode;
            handled = true;
        }
        else if (methodName.equals("toString"))
        {
            result = "proxy to " + getObjectName();
            handled = true;
        }
        else if (methodName.equals("equals") && numArgs == 1)
        {
            result = this.equals(args[ 0]);
            handled = true;
        }

        if (!handled)
        {
            debugMethod(getObjectName().toString(), "super.invoke",
                    method.getName(), args);

            result = super.invoke(proxy, method, args);
        }

        return (result);
    }

    protected boolean getDebug()
    {
        return AMXDebug.getInstance().getDebug(getClass().getName());
    }

    protected void debugMethod(final String methodName, final Object... args)
    {
        if (getDebug())
        {
            mDebug.println(AMXDebug.methodString(methodName, args));
        }
    }

    protected void debugMethod(
            final String msg,
            final String methodName,
            final Object... args)
    {
        if (getDebug())
        {
            mDebug.println(AMXDebug.methodString(methodName, args) + ": " + msg);
        }
    }

    protected void debug(final Object... args)
    {
        if (getDebug())
        {
            mDebug.println(StringUtil.toString("", args));
        }
    }

    protected void debug(final Object o)
    {
        mDebug.println(o);
    }

}





