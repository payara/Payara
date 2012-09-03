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

package org.glassfish.admin.amx.core;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.management.*;
import javax.management.openmbean.OpenType;
import org.glassfish.admin.amx.base.DomainRoot;
import org.glassfish.admin.amx.base.MBeanTrackerMBean;
import org.glassfish.admin.amx.base.Pathnames;
import org.glassfish.admin.amx.config.AMXConfigProxy;
import static org.glassfish.admin.amx.core.PathnameConstants.LEGAL_NAME_PATTERN;
import static org.glassfish.admin.amx.core.PathnameConstants.LEGAL_TYPE_PATTERN;
import org.glassfish.admin.amx.core.proxy.ProxyFactory;
import org.glassfish.admin.amx.util.CollectionUtil;
import org.glassfish.admin.amx.util.ExceptionUtil;
import org.glassfish.admin.amx.util.SetUtil;
import org.glassfish.admin.amx.util.StringUtil;
import org.glassfish.admin.amx.util.jmx.JMXUtil;
import static org.glassfish.external.amx.AMX.*;
import org.glassfish.external.amx.AMXGlassfish;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

/**
Validation of key behavioral requirements of AMX MBeans.
These tests do not validate any MBean-specific semantics, only general requirements for all AMX MBeans.
<p>
Note that all tests have to account for the possibility that an MBean can be unregistered while
the validation is in progress— that is not a test failure, since it is perfectly legal.
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
public final class AMXValidator
{
    /** we can run in the client or server, so use a fixed-name Logger */
    private static final Logger sLogger = Logger.getLogger( AMXValidator.class.getName() );
    private static void log(
        final Level     level,
        final String    msg,
        final Throwable t)
    {
        sLogger.log(level, msg, t);
    }
    private static void logWarning(
        final String    msg,
        final Throwable t)
    {
        log(Level.WARNING, msg, t);
    }
    private static void logInfo(
        final String    msg,
        final Throwable t)
    {
        log(Level.INFO, msg, t);
    }
    private static void progress(
        final Object... args)
    {
        if ( sLogger.isLoggable(Level.FINE) )
        {
            log(Level.FINE, toString(args), null);
        }
    }
    
    private static String toString(final Object... args)
    {
        final StringBuilder buf = new StringBuilder();
        for( final Object o : args )
        {
            buf.append("").append( o);
        }
        return buf.toString();
    }
    
    private static final Level LEVEL_DEBUG = Level.FINE;
    private static void debug(final Object... args)
    {
        if ( sLogger.isLoggable(LEVEL_DEBUG) )
        {
            log( LEVEL_DEBUG, toString(args), null );
        }
    }
    

    private static final String NL = StringUtil.NEWLINE();

    private final MBeanServerConnection mMBeanServer;

    private final ProxyFactory mProxyFactory;

    private final DomainRoot mDomainRoot;
    
    // created if needed
    private MBeanTrackerMBean  mMBeanTracker;

    private volatile boolean  mUnregisterNonCompliant;
    private volatile boolean  mLogInaccessibleAttributes;
    private volatile String   mValidationLevel;
    
    public AMXValidator(
        final MBeanServerConnection conn,
        final String    validationLevel,
        final boolean   unregisterNonCompliant,
        final boolean   logInaccessibleAttributes )
    {
        mMBeanServer = conn;

        mProxyFactory = ProxyFactory.getInstance(conn);
        mDomainRoot = mProxyFactory.getDomainRootProxy(false);
        
        mValidationLevel = validationLevel;
        mUnregisterNonCompliant = unregisterNonCompliant;
        mLogInaccessibleAttributes = logInaccessibleAttributes;
    }
    
    /**
        Return a Set containing ObjectNames that appear to be AMX-compliant MBeans
     */
    public Set<ObjectName> filterAMX(final Set<ObjectName> candidates)
    {
        final Set<ObjectName> amxSet = new HashSet<ObjectName>();
        for( final ObjectName cand : candidates )
        {
            if ( cand.getKeyProperty(TYPE_KEY) == null ) continue;
            
            // for now, require matching jmx domain "amx"
            if ( cand.getDomain().equals( AMXGlassfish.DEFAULT_JMX_DOMAIN) )
            {
                amxSet.add(cand);
            }
            
        }
        return amxSet;
    }
    
    /**
        Find all MBeans that appear to be AMX MBeans
     */
    public Set<ObjectName> findAllAMXCompliant()
    {
        // query for all MBeans in all domains
        // for now, any MBean with Parent/Name and metadata we'll guess is AMX
        final ObjectName pattern = Util.newObjectNamePattern("*", "*");
        Set<ObjectName> theWorld = null;
        try
        {
            theWorld = mMBeanServer.queryNames(pattern, null);
        }
        catch( final IOException e )
        {
            throw new RuntimeException(e);
        }
        
        return filterAMX(theWorld);
    }

    private static final class IllegalClassException extends Exception
    {
        private final Class<?> mClass;

        public IllegalClassException(final Class<?> clazz)
        {
            super("Class " + clazz.getName() + " not allowed for AMX MBeans");
            mClass = clazz;
        }

        public Class<?> clazz()
        {
            return mClass;
        }

        @Override
        public String toString()
        {
            return super.getMessage();
        }

    }

    private static final class ValidationFailureException extends Exception
    {
        private final ObjectName mObjectName;

        public ValidationFailureException(final ObjectName objectName, final String msg)
        {
            super(msg);
            mObjectName = objectName;
        }

        public ValidationFailureException(final AMXProxy amx, final String msg)
        {
            this(amx.objectName(), msg);
        }

        public ObjectName objectName()
        {
            return mObjectName;
        }

        @Override
        public String toString()
        {
            return getMessage() + ", " + mObjectName;
        }

    }
    
    /** keeps track of all validation failures */
    private static final class Failures
    {
        private final ConcurrentMap<ObjectName, ProblemList> mFailures = new ConcurrentHashMap<ObjectName, ProblemList>();

        private AtomicInteger mNumTested = new AtomicInteger();

        public Failures()
        {
        }

        public int getNumTested()
        {
            return mNumTested.get();
        }

        public int getNumFailures()
        {
            return mFailures.keySet().size();
        }

        public Map<ObjectName, ProblemList> getFailures()
        {
            return mFailures;
        }

        void result( final ProblemList problems)
        {
            mNumTested.incrementAndGet();

            if ( problems.hasProblems() )
            {
                if ( problems.instanceNotFound() )
                {
                    return;
                }
            
                mFailures.put( problems.getObjectName(), problems);
            }
        }

        @Override
        public String toString()
        {
            final StringBuilder builder = new StringBuilder();

            for (final ObjectName badBoy : mFailures.keySet())
            {
                final ProblemList problems = mFailures.get(badBoy);

                builder.append(badBoy).append(NL);
                builder.append(CollectionUtil.toString( problems.getProblems(), NL));
                builder.append(NL);
                builder.append(NL);
            }
            builder.append(mFailures.size()).append(" failures.");

            return builder.toString() + NL + mNumTested + " MBeans tested.";
        }
    }
    
    public static final class ProblemList
    {
        final ObjectName   mObjectName;
        final List<String> mProblems;
        boolean            mInstanceNotFound;
        
        public ProblemList( final ObjectName objectName )
        {
            mObjectName = objectName;
            mProblems = new ArrayList<String>();
            mInstanceNotFound = false;
        }
        
        public List<String> getProblems() { return mProblems; }
        public ObjectName getObjectName() { return mObjectName; }
        
        public boolean hasProblems() { return !mProblems.isEmpty(); }
        
        
        public boolean instanceNotFound()
        {
            return mInstanceNotFound;
        }
        
        private void add( final String msg )
        {
            try
            {
                add( msg, null);
            }
            catch( final InstanceNotFoundException e )
            {
                // can't happen
            }
        }
        
        private void add( final Throwable t) throws InstanceNotFoundException { add( "", t); }

        private void add( final String msg, final Throwable t )
            throws InstanceNotFoundException
        {
            if ( t == null )
            {
                mProblems.add( msg );
            }
            else
            {
                // it's not an issue if the MBean went missing
                final Throwable rootCause = ExceptionUtil.getRootCause(t);
                if ( AMXValidator.instanceNotFound(rootCause) )
                {
                    mInstanceNotFound = true;
                    // abort validation by throwing InstanceNotFoundException
                    throw new InstanceNotFoundException( "" + mObjectName );
                }
                else
                {
                    mProblems.add( msg + "\n" + ExceptionUtil.toString(rootCause) );
                }
            }
        }
        
        @Override
        public String toString()
        {
            if ( mInstanceNotFound )
            {
                return "MBean " + mObjectName + " unregistered while being validated";
            }
            
            return "MBean " + mObjectName + " problems: " + NL + CollectionUtil.toString( mProblems, NL);
        }
    }

    /** types that are not open types, but that we deem acceptable for a remote API */
    private static Set<Class> EXTRA_ALLOWED_TYPES = SetUtil.newTypedSet(
        // any special-case exceptions go here
    );

    private static boolean isAcceptableRemoteType(final Class<?> c)
    {
        if (c.isPrimitive() ||
            EXTRA_ALLOWED_TYPES.contains(c) ||
            OpenType.ALLOWED_CLASSNAMES_LIST.contains(c.getName()) ||
            c.getName().startsWith("javax.management.") )
        {
            return true;
        }

        // quick checks for other common cases
        if (c.isArray() && isAcceptableRemoteType(c.getComponentType()))
        {
            return true;
        }

        return false;
    }

    /**
    "best effort"<p>
    Attributes that cannot be sent to generic clients are not allowed.
    More than OpenTypes are allowed eg messy stuff like JSR 77 Stats and Statistics.
     */
    private static void checkLegalForRemote(final Object value) throws IllegalClassException
    {
        if (value == null)
        {
            return;
        }
        final Class<?> clazz = value.getClass();
        if (isAcceptableRemoteType(clazz))
        {
            return;
        }

        // would these always be disallowed?
        if (clazz.isSynthetic() || clazz.isLocalClass() || clazz.isAnonymousClass() || clazz.isMemberClass())
        {
            throw new IllegalClassException(clazz);
        }

        if (clazz.isArray())
        {
            if (!isAcceptableRemoteType(clazz.getComponentType()))
            {
                final Object[] a = (Object[]) value;
                for (final Object o : a)
                {
                    checkLegalForRemote(o);
                }
            }
        }
        else if (Collection.class.isAssignableFrom(clazz))
        {
            final Collection<?> items = (Collection) value;
            for (final Object o : items)
            {
                checkLegalForRemote(o);
            }
        }
        else if (Map.class.isAssignableFrom(clazz))
        {
            final Map<?, ?> items = (Map) value;
            for (final Map.Entry me : items.entrySet())
            {
                checkLegalForRemote(me.getKey());
                checkLegalForRemote(me.getValue());
            }
        }
        else
        {
            throw new IllegalClassException(clazz);
        }
    }

    static boolean instanceNotFound(final Throwable t )
    {
        return ExceptionUtil.getRootCause(t) instanceof InstanceNotFoundException;
    }
    
            
    private void _validate(final AMXProxy proxy, final ProblemList problems) throws InstanceNotFoundException
    {
        progress( "Validate: ", proxy.objectName() );

        try
        {
            validateObjectName(proxy);
        }
        catch (final Exception t)
        {
            problems.add( t);
        }

        try
        {
            validateMetadata(proxy, problems);
        }
        catch (final Exception t)
        {
            problems.add(t);
        }

        try
        {
            validateRequiredAttributes(proxy);
        }
        catch (final Exception t)
        {
            problems.add(t);
        }


        // test required attributes
        try
        {
            final String name = proxy.getName();
        }
        catch (final Exception t)
        {
            problems.add( "Proxy access to 'Name' failed: ", t);
        }

        try
        {
            final ObjectName parent = proxy.getParent();
        }
        catch (final Exception t)
        {
            problems.add( "Proxy access to 'Parent' failed: ", t);
        }
        try
        {
            final ObjectName[] children = proxy.getChildren();
        }
        catch (final Exception t)
        {
            problems.add( "Proxy access to 'Children' failed: ", t);
        }


        // test path resolution
        final Pathnames paths = mDomainRoot.getPathnames();
        if ( paths == null )
        {
            throw new IllegalStateException("Pathnames MBean does not exist");
        }
        
        try
        {
            final String path = proxy.path();
            final ObjectName actualObjectName = proxy.objectName();

            final ObjectName o = paths.resolvePath(path);
            if (o == null)
            {
                if ( proxy.valid() )   // could have been unregistered
                {
                    problems.add("Path " + path + " does not resolve to any ObjectName, should resolve to: " + actualObjectName);
                }
            }
            else if (!actualObjectName.equals(o))
            {
                problems.add("Path " + path + " does not resolve to ObjectName: " + actualObjectName);
            }
        }
        catch (final Exception t)
        {
            problems.add(t);
        }

        // test attributes
        final Set<String> attributeNames = proxy.extra().attributeNames();
        for (final String attrName : attributeNames)
        {
            try
            {
                final Object result = proxy.extra().getAttribute(attrName);

                checkLegalForRemote(result);
            }
            catch (final Exception t)
            {
                if ( attrName.equals(ATTR_NAME) || attrName.equals(ATTR_PARENT) || attrName.equals(ATTR_CHILDREN) )
                {
                    problems.add( "Attribute failed: '" + attrName + "': ", t);
                }
                else   // too stringer to consider the MBean non-compliant because of a general attribute failure.
                {
                    // this code can run in a client; a logger is not advisable
                    logWarning( "Attribute '" + attrName + "' failed for " + proxy.objectName(), ExceptionUtil.getRootCause(t));
                }
            }
        }

        try
        {
            validateChildren(proxy);
        }
        catch (final Exception t)
        {
            problems.add(t);
        }

        // test proxy methods
        try
        {
            final AMXProxy parent = proxy.parent();
            if (parent == null && !proxy.type().equals(Util.deduceType(DomainRoot.class)))
            {
                final ObjectName parentObjectName = proxy.getParent();
                final boolean exists = mMBeanServer.isRegistered( proxy.objectName() );
                problems.add("Null parent for " + proxy.objectName() +
                    ", isRegistered(self) = " + exists + ", parent = " + parentObjectName);
            }

            final Set<AMXProxy> childrenSet = proxy.childrenSet();
            final Map<String, Map<String, AMXProxy>> childrenMaps = proxy.childrenMaps();
            final Map<String, Object> attributesMap = proxy.attributesMap();
            final Set<String> attrNames = proxy.attributeNames();
            if (!attrNames.equals(attributesMap.keySet()))
            {
                final Set<String>  keys = new HashSet<String>(attributesMap.keySet());
                keys.removeAll(attrNames);
                if ( !keys.isEmpty() )
                {
                    throw new Exception("Attributes Map contains attributes not found in the MBeanInfo: " + keys);
                }
                
                if ( mLogInaccessibleAttributes )
                {
                    final Set<String> missing = new HashSet<String>(attrNames);
                    missing.removeAll(attributesMap.keySet());
                    
                    logInfo("Inaccessible attributes: " + missing + " in " + proxy.objectName(), null);
                }
            }

            for (final AMXProxy child : childrenSet)
            {
                if (child.extra().singleton())
                {
                    final String childType = child.type();
                    if (!child.objectName().equals(proxy.child(childType).objectName()))
                    {
                        throw new Exception("Child type " + childType + " cannot be found via child(type)");
                    }
                }
            }

            for (final String type : childrenMaps.keySet())
            {
                final Map<String, AMXProxy> m = proxy.childrenMap(type);
                if (m.keySet().isEmpty())
                {
                    throw new Exception("Child type " + type + " has nothing in Map");
                }
            }

        }
        catch (final Exception t)
        {
            problems.add( "General test failure: ", t);
        }


        try
        {
            validateAMXConfig(proxy, problems);
        }
        catch (final Exception t)
        {
            if ( proxy.valid() )
            {
                problems.add( "General test failure in validateAMXConfig: ", t);
            }
        }
    }
    
    
    private void fail(final ObjectName objectName, final String msg)
            throws ValidationFailureException
    {
        throw new ValidationFailureException(objectName, msg);
    }

    private void fail(final AMXProxy amx, final String msg)
            throws ValidationFailureException
    {
        throw new ValidationFailureException(amx, msg);
    }

    private void validateAMXConfig(final AMXProxy proxy, final ProblemList problems) throws InstanceNotFoundException
    {
        if (!AMXConfigProxy.class.isAssignableFrom(proxy.extra().genericInterface()))
        {
            return;
        }
        final AMXConfigProxy config = proxy.as(AMXConfigProxy.class);

        // All AMXConfig must be descendants of Domain
        if ( ! config.type().equals( "domain" ) )   // hard-coded type, we can't import Domain.class here
        {
            // verify that all its ancestors are also AMXConfig
            // Do a quick check, ultimately if all AMXConfig have an AMXConfig as a parent,
            // then they all have DomainConfig as a parent.
            if ( ! AMXConfigProxy.class.isAssignableFrom(config.parent().extra().genericInterface() ) )
            {
                problems.add("AMXConfig MBean is not a descendant of Domain: " + config.objectName() + ", it has parent " + config.getParent() );
            }
        }
        
        // check default values support
        final Map<String, String> defaultValues = config.getDefaultValues(false);
        final Map<String, String> defaultValuesAMX = config.getDefaultValues(true);
        if (defaultValues.keySet().size() != defaultValuesAMX.keySet().size())
        {
            problems.add("Default values for AMX names differ in number from XML names: " + defaultValues.keySet().size() + " != " + defaultValuesAMX.keySet().size());
        }
        for (final Map.Entry<String,String> me : defaultValues.entrySet())
        {
            final Object value = me.getValue();
            if (value == null)
            {
                problems.add("Default value of null for: " + me.getKey());
            }
            else if (!(value instanceof String))
            {
                problems.add("Default value is not a String for: " + me.getKey());
            }
        }

        final String[] subTypes = config.extra().subTypes();
        if (subTypes != null)
        {
            for (final String subType : subTypes)
            {
                config.getDefaultValues(subType, false);
            }
        }
    }

    private static final Pattern TYPE_PATTERN = Pattern.compile(LEGAL_TYPE_PATTERN);

    private static final Pattern NAME_PATTERN = Pattern.compile(LEGAL_NAME_PATTERN);

    private void validateObjectName(final AMXProxy proxy)
            throws ValidationFailureException
    {
        final ObjectName objectName = proxy.objectName();

        final String type = objectName.getKeyProperty("type");
        if (type == null || type.length() == 0)
        {
            fail(objectName, "type property required in ObjectName");
        }
        if (!TYPE_PATTERN.matcher(type).matches())
        {
            fail(objectName, "Illegal type \"" + type + "\", does not match " + TYPE_PATTERN.pattern());
        }

        final String nameProp = objectName.getKeyProperty("name");
        if (nameProp != null)
        {
            if (nameProp.length() == 0)
            {
                fail(objectName, "name property of ObjectName may not be empty");
            }
            if (!NAME_PATTERN.matcher(nameProp).matches())
            {
                fail(objectName, "Illegal name \"" + nameProp + "\", does not match " + NAME_PATTERN.pattern());
            }
        }
        else
        {
            // no name property, it's by definition a singleton
            if (!proxy.extra().singleton())
            {
                fail(objectName, "Metadata claims named (non-singleton), but no name property present in ObjectName");
            }
        }

        if (proxy.parent() != null)
        {
            if (!proxy.parentPath().equals(proxy.parent().path()))
            {
                fail(objectName, "Parent path of " + proxy.parentPath() + " does not match parent's path for  parent " + proxy.parent().objectName());
            }
        }
    }

    /** verify that the children/parent relationship exists */
    private void validateChildren(final AMXProxy proxy)
            throws ValidationFailureException
    {
        final Set<String> attrNames = proxy.attributeNames();
        if (!attrNames.contains(ATTR_CHILDREN))
        {
            // must NOT supply Children
            try
            {
                fail(proxy, "MBean has no Children attribute in its MBeanInfo, but supplies the attribute");
            }
            catch (Exception e)
            {
                // good, the Attribute must not exist
            }
        }
        else
        {
            // must supply Children
            try
            {
                final ObjectName[] children = proxy.getChildren();
                if (children == null)
                {
                    fail(proxy, "Children attribute must be non-null");
                }
                final Set<ObjectName> childrenSet = SetUtil.newSet(children);
                if ( childrenSet.size() != children.length )
                {
                    fail(proxy, "Children contains duplicates");
                }
                if ( childrenSet.contains(null) )
                {
                    fail(proxy, "Children contains null");
                }

                // verify that each child is non-null and references its parent
                for (final ObjectName childObjectName : children)
                {
                    if (childObjectName == null)
                    {
                        fail(proxy, "Child in Children array is null");
                    }
                    final AMXProxy child = mProxyFactory.getProxy(childObjectName);
                    if (!proxy.objectName().equals(child.parent().objectName()))
                    {
                        fail(proxy, "Child’s Parent of " + child.parent().objectName() +
                                    " does not match the actual parent of " + proxy.objectName());
                    }
                }

                // verify that the children types do not differ only by case-sensitivity
                final Set<String> caseSensitiveTypes = new HashSet<String>();
                final Set<String> caseInsensitiveTypes = new HashSet<String>();
                for (final ObjectName o : children)
                {
                    caseSensitiveTypes.add(Util.getTypeProp(o));
                    caseInsensitiveTypes.add(Util.getTypeProp(o).toLowerCase(Locale.ENGLISH));
                }
                if (caseSensitiveTypes.size() != caseInsensitiveTypes.size())
                {
                    fail(proxy, "Children types must be case-insensitive");
                }
                
                // verify that the MBeanTracker agrees with the parent MBean
                final Set<ObjectName> tracked = getMBeanTracker().getChildrenOf(proxy.objectName()); 
                if ( childrenSet.size() != children.length )
                {
                    // try again, in case it's a timing issue
                    final Set<ObjectName> childrenSetNow = SetUtil.newSet( proxy.getChildren() );
                    if ( ! tracked.equals( childrenSetNow ) )
                    {
                        fail(proxy, "MBeanTracker has different MBeans than the MBean: {" + 
                            CollectionUtil.toString(tracked, ", ") + "} vs MBean having {" +
                            CollectionUtil.toString(childrenSetNow, ", ") + "}");
                    }
                }
            }
            catch (final Exception e)
            {
                if ( ! instanceNotFound(e) )
                {
                    fail(proxy, "MBean failed to supply Children attribute");
                }
            }

            // children of the same type must have the same MBeanInfo
            try
            {
                final Map<String, Map<String, AMXProxy>> maps = proxy.childrenMaps();

                for (final Map.Entry<String, Map<String,AMXProxy>> me : maps.entrySet())
                {
                    final Map<String, AMXProxy> siblings = me.getValue();
                    if (siblings.keySet().size() > 1)
                    {
                        final Iterator<AMXProxy> iter = siblings.values().iterator();
                        final MBeanInfo mbeanInfo = iter.next().extra().mbeanInfo();
                        while (iter.hasNext())
                        {
                            final AMXProxy next = iter.next();
                            if (!mbeanInfo.equals(next.extra().mbeanInfo()))
                            {
                                fail(proxy, "Children of type=" + me.getKey() + " must  have the same MBeanInfo: " + siblings.values() );
                            }
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                if ( ! instanceNotFound(e) )
                {
                    logWarning( "MBean failed validating the MBeanInfo of children", e );
                    fail(proxy, "MBean failed validating the MBeanInfo of children with Exception: " + e.getMessage() );
                }
            }
        }
    }
    
    private MBeanTrackerMBean getMBeanTracker() {
        if ( mMBeanTracker == null )
        {
            mMBeanTracker = MBeanServerInvocationHandler.newProxyInstance(
                mMBeanServer, MBeanTrackerMBean.MBEAN_TRACKER_OBJECT_NAME, MBeanTrackerMBean.class, false);
        }
        return mMBeanTracker;
    }   
    
    private static final class MetadataValidator
    {
        private final Descriptor mDescriptor;

        private final Set<String> mFieldNames;

        private final ProblemList mProblems;

        public MetadataValidator(final Descriptor d, final ProblemList problems) throws InstanceNotFoundException
        {
            mDescriptor = d;
            mFieldNames = SetUtil.newSet(d.getFieldNames());
            mProblems = problems;

            validateRemote();
        }
        
        // Descriptor fields must be remotable
        void validateRemote() throws InstanceNotFoundException
        {
            for (final String fieldName : mFieldNames)
            {
                try
                {
                    checkLegalForRemote(mDescriptor.getFieldValue(fieldName));
                }
                catch (final IllegalClassException e)
                {
                    mProblems.add("Descriptor field " + fieldName + " uses a remote-unfriendly class: " + e.clazz().getName());
                }
            }
        }

        void validateMetadataBoolean(final String fieldName) throws InstanceNotFoundException
        {
            if (mFieldNames.contains(fieldName))
            {
                final Object value = mDescriptor.getFieldValue(fieldName);
                if (value == null)
                {
                    mProblems.add("Descriptor field " + fieldName + " must not be null");
                }
                else if (!((value instanceof Boolean) || value.equals("true") || value.equals("false")))
                {
                    mProblems.add("Descriptor field " + fieldName + " must be set to 'true' or 'false', value is " + value);
                }
            }
        }

        void validateMetadataString(final String fieldName) throws InstanceNotFoundException
        {
            if (mFieldNames.contains(fieldName))
            {
                final Object value = mDescriptor.getFieldValue(fieldName);
                if ( value != null )
                {
                    if ( ! (value instanceof String) )
                    {
                        mProblems.add("Descriptor field " + fieldName + " must be a String!" );
                    }
                }
            }
        }

        void validate(final String fieldName, final Class<?> clazz) throws InstanceNotFoundException
        {
            if (mFieldNames.contains(fieldName))
            {
                final Object value = mDescriptor.getFieldValue(fieldName);
                if (value == null || (!(clazz.isAssignableFrom(value.getClass()))))
                {
                    mProblems.add("Descriptor field " + fieldName + " must be of class " + clazz.getSimpleName());
                }
            }
        }
    }
    
        private static boolean
    isLegalClassname( final String s )
    {
        if ( s.length()== 0 || s.indexOf(" ") >= 0 )
        {
            return false;   // detect totally bogus name
        }
            
        return true;
    }

    
    private void checkLegalAttributeType(final String clazz, final String attrName, final ProblemList problems )
        throws InstanceNotFoundException
    {
        if ( ! isLegalClassname(clazz) )
        {
            problems.add( "Illegal classname for attribute " + StringUtil.quote(attrName) + ": " + StringUtil.quote(clazz) );
        }
    }
    
    private void checkLegalReturnType(final String clazz, final String operation, final ProblemList problems )
        throws InstanceNotFoundException
    {
        if ( ! isLegalClassname(clazz) )
        {
            problems.add( "Illegal return type for " + operation + "(): " + StringUtil.quote(clazz) );
        }
    }

    private void validateMetadata(final AMXProxy proxy, final ProblemList problems)
        throws InstanceNotFoundException
    {
        final MBeanInfo mbeanInfo = proxy.extra().mbeanInfo();
        final Descriptor d = mbeanInfo.getDescriptor();

        // verify that no extraneous field exist
        final Set<String> LEGAL_AMX_DESCRIPTORS = SetUtil.newStringSet(
                DESC_GENERIC_INTERFACE_NAME, DESC_IS_SINGLETON, DESC_IS_GLOBAL_SINGLETON, DESC_GROUP, DESC_SUPPORTS_ADOPTION, DESC_SUB_TYPES);
        for (final String fieldName : d.getFieldNames())
        {
            if (fieldName.startsWith(DESC_PREFIX) && !LEGAL_AMX_DESCRIPTORS.contains(fieldName))
            {
                problems.add("Illegal/unknown AMX metadata field: " + fieldName + " = " + d.getFieldValue(fieldName));
            }
        }

        final MetadataValidator val = new MetadataValidator(d, problems);
        // verify data types
        val.validateMetadataBoolean(DESC_IS_SINGLETON);
        val.validateMetadataBoolean(DESC_SUPPORTS_ADOPTION);
        val.validateMetadataBoolean(DESC_STD_IMMUTABLE_INFO);

        val.validateMetadataString(DESC_STD_INTERFACE_NAME);
        val.validateMetadataString(DESC_GENERIC_INTERFACE_NAME);
        val.validateMetadataString(DESC_GROUP);

        val.validate(DESC_SUB_TYPES, String[].class);

        for (final MBeanAttributeInfo attrInfo : mbeanInfo.getAttributes())
        {
            checkLegalAttributeType( attrInfo.getType(), attrInfo.getName(), problems );

            new MetadataValidator(attrInfo.getDescriptor(), problems);
        }

        for (final MBeanOperationInfo opInfo : mbeanInfo.getOperations())
        {
            checkLegalReturnType( opInfo.getReturnType(), opInfo.getName(), problems );
            
            new MetadataValidator(opInfo.getDescriptor(), problems);
        }

        for (final MBeanConstructorInfo cosntructorInfo : mbeanInfo.getConstructors())
        {
            new MetadataValidator(cosntructorInfo.getDescriptor(), problems);
        }

        for (final MBeanNotificationInfo notifInfo : mbeanInfo.getNotifications())
        {
            new MetadataValidator(notifInfo.getDescriptor(), problems);
        }

        if ( proxy.extra().globalSingleton() )
        {
            final ObjectName objectName = proxy.objectName();
            //debug( "Global singleton type = " + Util.getTypeProp(objectName) );
            // don't use Query MBean, it might not exist
            final ObjectName pattern = Util.newObjectNamePattern( objectName.getDomain(), Util.makeTypeProp(Util.getTypeProp(objectName)) );
            try
            {
                final Set<ObjectName>  instances = mMBeanServer.queryNames( pattern, null);
                
                if ( instances.size() > 1 )
                {
                    problems.add( "Global singleton " + objectName +
                        " conflicts with other MBeans of the same type: " +
                        CollectionUtil.toString(instances, ", "));
                }
            }
            catch( final Exception e )
            {
                throw new RuntimeException(e);
            }
        }
    }

    private void validateRequiredAttributes(final AMXProxy proxy)
            throws ValidationFailureException
    {
        final ObjectName objectName = proxy.objectName();
        // verify that the required attributes are present
        final Map<String, MBeanAttributeInfo> infos = JMXUtil.attributeInfosToMap(proxy.extra().mbeanInfo().getAttributes());
        final Set<String> attrNames = infos.keySet();
        if (!attrNames.contains("Name"))
        {
            fail(objectName, "MBeanInfo does not contain Name attribute");
        }
        if (!attrNames.contains("Parent"))
        {
            fail(objectName, "MBeanInfo does not contain Parent attribute");
        }

        if (attrNames.contains("Children"))
        {
            // must contain a non-null list of children
            try
            {
                if (proxy.getChildren() == null)
                {
                    fail(objectName, "value of Children attribute must not be null");
                }
            }
            catch (final AMXException e)
            {
                throw e;
            }
            catch (final Exception e)
            {
                if ( ! instanceNotFound(e) )
                {
                    fail(objectName, "does not supply children correctly");
                }
            }
        }
        else
        {
            // must NOT contain children, we expect an exception
            try
            {
                proxy.getChildren();
                fail(objectName, "Children attribute is present, but not listed in MBeanInfo");
            }
            catch (final Exception e)
            {
                // good, this is expected
            }
        }
    }

    public static final class ValidationResult
    {
        private final String mDetails;

        private final int mNumTested;

        private final int mNumFailures;
        
        private final Map<ObjectName, ProblemList> mProblems;

        private ValidationResult( final Failures failures )
        {
            mNumTested = failures.getNumTested();
            mNumFailures = failures.getNumFailures();
            mDetails = failures.toString();
            mProblems = failures.getFailures();
        }

        public String details()
        {
            return mDetails;
        }

        public Map<ObjectName,ProblemList> failures()
        {
            return mProblems;
        }

        public int numTested()
        {
            return mNumTested;
        }

        public int numFailures()
        {
            return mNumFailures;
        }

        @Override
        public String toString()
        {
            return details();
        }
    }
        
    private void unregisterNonCompliantMBean( final ObjectName objectName)
    {
        if ( mUnregisterNonCompliant )
        {
            try {
                mMBeanServer.unregisterMBean(objectName);
                logWarning( "Unregistered non-compliant MBean " + objectName, null);
            }
            catch( final Exception ignore ) {
                logWarning( "Unable to unregister non-compliant MBean " + objectName, null);
            }
        }
    }
    
    public ValidationResult validate(final Collection<ObjectName> c)
    {
        final ObjectName[] targets = CollectionUtil.toArray( c, ObjectName.class );
        return validate( targets );
    }
    
    public ValidationResult validate(final ObjectName[] targets)
    {
        final Failures failures = new Failures();

        // list them in order
        for (final ObjectName objectName : targets)
        {
            progress( "AMXValidator.validate(), begin: " + objectName );
            final ProblemList problems = new ProblemList(objectName);
            AMXProxy     amx = null;
            
            try
            {
                // certain failures prevent even the proxy from being created, a fatal error
                amx = mProxyFactory.getProxy(objectName);
                if ( amx == null )
                {
                    continue;    // not found
                }
                //debug( "VALIDATING: got proxy for: " + objectName );
            }
            catch( final Exception e )
            {
                if ( instanceNotFound(e) )
                {
                    progress( "AMXValidator.validate(), InstanceNotFound: " + objectName );
                    continue;
                }
                
                final String msg = "Cannot create AMXProxy for MBean \"" + objectName;
                progress( msg );
                problems.add(msg);
            }
            
            if ( amx != null )
            {
                try
                {
                    _validate(amx, problems);
                }
                catch( final InstanceNotFoundException e )
                {
                    continue;   // can't be tested, it's gone
                }
                catch( final Exception e )
                {
                    logWarning( "AMXValidator.validate(): got exception from _validate for " + objectName, e);
                    problems.add( "Validation failure for MBean " + objectName + e);
                }
            }

            if ( problems.hasProblems() && ! problems.instanceNotFound() )
            {
                debug( "AMXValidator.validate(): got problems from _validate for " + objectName + " : " + problems );
                
                //debug( "Calling unregisterNonCompliantMBean(): " + objectName + " for problems: " + problems );
                unregisterNonCompliantMBean(objectName);

                failures.result(problems);
            }
            progress( "AMXValidator.validate(): validated: " + objectName );
        }
        final ValidationResult result = new ValidationResult( failures );
        return result;
    }

    public ValidationResult validate(final ObjectName objectName)
    {
        return validate( new ObjectName[] { objectName } );
    }

    public ValidationResult validate()
    {
        final List<ObjectName> all = Util.toObjectNameList( mDomainRoot.getQueryMgr().queryAll() );

        return validate(CollectionUtil.toArray(all, ObjectName.class));
    }

}






































