/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.jvnet.hk2.config;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.HK2Loader;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.HK2LoaderImpl;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.tiger_types.Types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Describes the configuration model for a particular class (called "target type" in this class.)
 *
 * TODO: we need to remember if element values are single-valued or multi-valued.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ConfigModel {
    /**
     * Reference to the {@link ConfigInjector} used to inject values to
     * objects of this model.
     */
    public final ActiveDescriptor<? extends ConfigInjector> injector;

    /**
     * Legal attribute names.
     */
    final Map<String,AttributeLeaf> attributes = new HashMap<String,AttributeLeaf>();

    /**
     * Legal child element names and how they should be handled
     */
    final Map<String,Property> elements = new HashMap<String,Property>();

    final Map<Method,Method> duckMethods = new HashMap<Method, Method>();

    /**
     * Cache to map methods to properties
     */
    final Map<Method,Property> methodCache = new HashMap<Method,Property>();
    
    /**
     * Contracts under which the inhabitant should be registered.
     */
    final List<String> contracts;

    /**
     * Type names for which this type creates a symbol space.
     */
    final Set<String> symbolSpaces;

    /**
     * The element name of this model itself, if this element can appear globally.
     * Otherwise null.
     * <p>
     * Note that in many circumstances the tag name is determined by the parent element,
     * even if a {@link ConfigModel} has a tag name.
     */
    final String tagName;

    /**
     * getter for tagName
     * @return the element name of this model itself, if this element can appear globally
     * Otherwise null
     */
    public String getTagName() {
        return tagName;
    }

    /**
     * Deferred reference to the class loader that loaded the injector.
     * This classloader can also load the configurable object.
     */
    public final HK2Loader classLoaderHolder;

    /**
     * Fully-qualified name of the target type that this injector works on.
     */
    public final String targetTypeName;

    private Class targetTypeClass;

    /**
     * Fully-qualified name under which this type is indexed.
     * This is the class name where the key property is defined.
     *
     * <p>
     * Null if this type is not keyed.
     */
    public final String keyedAs;

    /**
     * If this model has any property that works as a key.
     *
     * @see ConfigMetadata#KEY
     */
    public final String key;
    
    private final ServiceLocator locator;

    /**
     * Returns the set of possible attributes names on this configuration model.
     *
     * @return the set of all possible attributes names on this model
     */
    public Set<String> getAttributeNames() {
        return Collections.unmodifiableSet( attributes.keySet() );
    }

    /**
     * Return the proxy type for this model
     * @param <T> the proxy type
     * @return the class object for this proxy type
     */
    public <T extends ConfigBeanProxy> Class<T> getProxyType() {
        if (targetTypeClass == null) {
            targetTypeClass = (Class<T>) classLoaderHolder.loadClass(targetTypeName);
        }

        return targetTypeClass;
    }

    /**
     * Returns the list of all the leaf attribute names on this model
     *
     * @return the list of all leaf attribute names.
     */
    public Set<String> getLeafElementNames() {
        final Set<String> results = new HashSet<String>();
        for (Map.Entry<String,Property> prop : elements.entrySet()) {
            if (prop.getValue().isLeaf()) {
                results.add(prop.getKey());
            }
        }
        return Collections.unmodifiableSet(results);
    }

    /**
     * Returns the list of all posible elements names on this model
     *
     * @return the list of all posible elements names.
     */
    public Set<String> getElementNames() {
        return Collections.unmodifiableSet(elements.keySet());
    }

    /**
     * Returns the Property model for an element associated with this model
     * or null of the element name is not known,
     * @param elementName element name identifying the property
     * @return the Property instance describing the element
     */
    public Property getElement(String elementName) {
        return elements.get(elementName);
    }


    public Property getElementFromXMlName( final String xmlName ) {
        final ConfigModel.Property cmp = findIgnoreCase(xmlName);
        if (cmp == null) {
            throw new IllegalArgumentException( "Illegal name: " + xmlName );
        }
        return cmp;
    }    

    /**
     * Performs injection to the given object.
     */
    /*package*/ void inject(Dom dom, Object target) {
        try {
            ServiceLocatorUtilities.<ConfigInjector>getService(locator, injector).inject(dom,target);
        } catch (ConfigurationException e) {
            e.setLocation(dom.getLocation());
            throw e;
        }
    }
    
    public Map<String, List<String>>  getMetadata() {
        return injector.getMetadata();
    }

    /**
     * Obtains the duck method implementation from a method on the {@link ConfigBeanProxy}-derived interface.
     */
    public Method getDuckMethod(Method method) throws ClassNotFoundException, NoSuchMethodException {
        synchronized (duckMethods) {
            Method duckMethod = duckMethods.get(method);
            if(duckMethod!=null)    return duckMethod;

            final Class<?> clz = method.getDeclaringClass();
            ClassLoader cl = System.getSecurityManager()==null?clz.getClassLoader():
                    AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                        @Override
                        public ClassLoader run() {
                            return clz.getClassLoader();
                        }
                    });
            Class<?> duck = cl.loadClass(clz.getName() + "$Duck");

            Class<?>[] types = method.getParameterTypes();
            Class[] paramTypes = new Class[types.length+1];
            System.arraycopy(types,0,paramTypes,1,types.length);
            paramTypes[0] = clz;
            duckMethod = duck.getMethod(method.getName(), paramTypes);
            duckMethods.put(method,duckMethod);

            return duckMethod;
        }
    }

    /**
     * Obtain XML names (like "abc-def") from strings like "getAbcDef" and "hasAbcDef".
     * <p>
     * The conversion rule uses the model to find a good match.
     */
    public ConfigModel.Property toProperty(Method method) {
        Property prop = methodCache.get(method);
        if (prop != null) {
            return prop;
        }
        
        String name = method.getName();

        // check annotations first
        Element e = method.getAnnotation(Element.class);
        if(e!=null) {
            String en = e.value();
            if(en.length()>0) {
                prop = elements.get(en);
                methodCache.put(method, prop);
                return prop;
            }
        }
        Attribute a = method.getAnnotation(Attribute.class);
        if(a!=null) {
            String an = a.value();
            if(an.length()>0) {
                prop = attributes.get(an);
                methodCache.put(method, prop);
                return prop;
            }
        }
        // TODO: check annotations on the getter/setter

        // first, trim off the prefix
        for (String p : Dom.PROPERTY_PREFIX) {
            if(name.startsWith(p)) {
                name = name.substring(p.length());
                break;
            }
        }

        name = camelCaseToXML(name);

        // at this point name should match XML names in the model, modulo case.
        prop = findIgnoreCase(name);
        methodCache.put(method, prop);
        return prop;
    }

    public static String trimPrefix(String name) {

        // first, trim off the prefix
        for (String p : Dom.PROPERTY_PREFIX) {
            if(name.startsWith(p)) {
                name = name.substring(p.length());
                break;
            }
        }
        // lowecase first letter.
        return name.toLowerCase(Locale.ENGLISH).charAt(0) + name.substring(1);

    }

    public static String camelCaseToXML(String camelCase) {
        
        // tokenize by finding 'x|X' and 'X|Xx' then insert '-'.
        StringBuilder buf = new StringBuilder(camelCase.length()+5);
        for(String t : Dom.TOKENIZER.split(camelCase)) {
            if(buf.length()>0)  buf.append('-');
            buf.append(t.toLowerCase(Locale.ENGLISH));
        }
        return buf.toString();
    }

    public abstract static class Property {

        public final List<String> annotations = new ArrayList<String>();

        /**
         * @see #xmlName()
         */
        public final String xmlName;

        protected Property(String xmlName) {
            this.xmlName = xmlName;
        }

        /**
         * XML name of the property, like "abc-def".
         */
        public final String xmlName() {
            return xmlName;
        }

        public abstract boolean isLeaf();

        /**
         * Is multiple values allowed?
         */
        public abstract boolean isCollection();

        /**
         * Gets the value from {@link Dom} in the specified type.
         *
         * @param dom
         *      The DOM instance to get the value from.
         * @param returnType
         *      The expected type of the returned object.
         *      Valid types are (1) primitive and 'leaf' Java types, such as {@link String},
         *      (2) {@link ConfigBeanProxy}, (3) {@link Dom}, and (4) collections of any of above.
         */
        public abstract Object get(Dom dom, Type returnType);

        /**
         * Sets the value to {@link Dom}.
         *
         * @param arg
         *      The new value. See the return type of the get method for the discussion of
         *      possible types.
         */
        public abstract void set(Dom dom, Object arg);

        public List<String> getAnnotations() {
            return annotations;
        }
    }

    public static abstract class Node extends Property {
        final ConfigModel model;
        public Node(ConfigModel model, String xmlName) {
            super(xmlName);
            this.model = model;
        }

        public boolean isLeaf() {
            return false;
        }

        /**
         * Returns the config model for this Node
         * 
         * @return
         */
        public ConfigModel getModel() {
            return model;
        }

        /**
         * Coerce the given type to {@link Dom}.
         * Only handles those types that are valid to the {@link #set(Dom, Object)} method.
         */
        protected final Dom toDom(Object arg) {
            if(arg==null)
                return null;
            if(arg instanceof Dom)
                return (Dom)arg;
            if(arg instanceof ConfigBeanProxy)
                return Dom.unwrap((ConfigBeanProxy)arg);
            throw new IllegalArgumentException("Unexpected type "+arg.getClass()+" for "+xmlName);
        }
    }

    static final class CollectionNode extends Node {
        CollectionNode(ConfigModel model, String xmlName) {
            super(model, xmlName);
        }

        public boolean isCollection() {
            return true;
        }

        public Object get(final Dom dom, Type returnType) {
            // TODO: perhaps support more collection types?


            if(!(returnType instanceof ParameterizedType))
                throw new IllegalArgumentException("List needs to be parameterized");
            final Class itemType = Types.erasure(Types.getTypeArgument(returnType,0));

            final List<Dom> v = ("*".equals(xmlName)?dom.domNodeByTypeElements(itemType):dom.nodeElements(xmlName));

            if(itemType==Dom.class)
                // TODO: this returns a view, not a live list
                return v;
            if(ConfigBeanProxy.class.isAssignableFrom(itemType)) {
                // return a live list
                return new AbstractList<Object>() {
                    public Object get(int index) {
                        return v.get(index).createProxy();
                    }

                    public void add(int index, Object element) {
                        // update the master children list, as well as this view 'v'
                        Dom child = Dom.unwrap((ConfigBeanProxy) element);
                        dom.insertAfter( index==0 ? null : v.get(index-1), xmlName, child);
                        v.add(index,child);
                    }

                    public Object remove(int index) {
                        Dom child = v.get(index);
                        dom.removeChild(child);
                        v.remove(index);
                        return child.createProxy();
                    }

                    public Object set(int index, Object element) {
                        Dom child = Dom.unwrap((ConfigBeanProxy) element);
                        String name = "*".equals(xmlName) ? child.model.injector.getName() : xmlName;
                        dom.replaceChild(v.get(index), name, child);
                        return v.set(index,child).createProxy();
                    }

                    public int size() {
                        return v.size();
                    }
                };
            }

            // TODO: error check needs to be improved,
            // as itemType might be inconsistent with the actual type
            return new AbstractList() {
                public Object get(int index) {
                    return v.get(index).get();
                }

                public int size() {
                    return v.size();
                }
            };
        }

        public void set(Dom dom, Object _arg) {
            if(!(_arg instanceof List))
                throw new IllegalArgumentException("Expecting a list but found "+_arg);
            List arg = (List)_arg;

            Dom[] values = new Dom[arg.size()];
            int i=0;
            for (Object o : arg)
                values[i++] = toDom(o);

            dom.setNodeElements(xmlName,values);
        }
    }

    static final class SingleNode extends Node {
        SingleNode(ConfigModel model, String xmlName) {
            super(model, xmlName);
        }

        public boolean isCollection() {
            return false;
        }

        public Object get(Dom dom, Type returnType) {
            Dom v = dom.nodeElement(xmlName);
            if(v==null)     return null;

            if(returnType==Dom.class)
                return v;

            Class rt = Types.erasure(returnType);
            if(ConfigBeanProxy.class.isAssignableFrom(rt))
                return v.createProxy();

            throw new IllegalArgumentException("Invalid type "+returnType+" for "+xmlName);
        }

        public void set(Dom dom, Object arg) {
            Dom child = toDom(arg);

            if(child==null) // remove
                dom.setNodeElements(xmlName);
            else // replace
                dom.setNodeElements(xmlName,child);
        }
    }

    static abstract class Leaf extends Property {
        public Leaf(String xmlName) {
            super(xmlName);
        }

        public boolean isLeaf() {
            return true;
        }

        /**
         * Converts a single value from string to the specified target type.
         *
         * @return
         *      Instance of the given 'returnType'
         */
        protected Object convertLeafValue(Dom parent, Class<?> returnType, String v) {
            if(v==null)
                // TODO: default value handling
                // TODO: if primitive types, report an error
                return null;

            if(returnType==String.class) {
                return v;
            }
            if(returnType==Long.class || returnType==long.class) {
                return Long.valueOf(v);
            }
            if(returnType==Integer.class || returnType==int.class) {
                return Integer.valueOf(v);
            }
            if(returnType==Boolean.class || returnType==boolean.class) {
                return BOOLEAN_TRUE.contains(v);
            }
            throw new IllegalArgumentException("Don't know how to handle "+returnType);
        }

        private static final Set<String> BOOLEAN_TRUE = new HashSet<String>(Arrays.asList("true","yes","on","1"));
    }

    static class CollectionLeaf extends Leaf {
        CollectionLeaf(String xmlName) {
            super(xmlName);
        }

        public boolean isCollection() {
            return true;
        }

        String elementValue(Object element) {
            return element.toString();
        }

        public Object get(final Dom dom, Type returnType) {
            // TODO: perhaps support more collection types?
            final List<String> v = dom.leafElements(xmlName);
            if(!(returnType instanceof ParameterizedType))
                throw new IllegalArgumentException("List needs to be parameterized");
            final Class itemType = Types.erasure(Types.getTypeArgument(returnType,0));

            // return a live list
            return new AbstractList<Object>() {
                public Object get(int index) {
                    return convertLeafValue(dom, itemType, v.get(index));
                }

                public void add(int index, Object element) {
                    // update the master children list, as well as this view 'v'
                    dom.addLeafElement(xmlName, elementValue(element));
                    v.add(index, element.toString());
                }

                public Object remove(int index) {
                    dom.removeLeafElement(xmlName, v.get(index));
                    return v.remove(index);
                }

                public Object set(int index, Object element) {
                    dom.changeLeafElement(xmlName, v.get(index), elementValue(element));
                    return v.set(index, element.toString());
                }

                public int size() {
                    return v.size();
                }
            };
        }

        public void set(Dom dom, Object arg) {
            if (arg instanceof List) {
                String[] strings = new String[((List) arg).size()];
                dom.setLeafElements(xmlName, (String[]) ((List)arg).toArray(strings));
            } else {//TODO -- I hope this is OK for now (km@dev.java.net 25 Mar 2008)
                throw new UnsupportedOperationException();
            }
        }
    }

    final class ReferenceCollectionLeaf extends CollectionLeaf {

        ReferenceCollectionLeaf(String xmlName) {
            super(xmlName);
        }
        protected Object convertLeafValue(Dom parent, Class<?> returnType, String v) {
            // let's look first the fast way.
            Object candidate = parent.getHabitat().getService(returnType, v);
            if (candidate!=null) {
                return returnType.cast(candidate);
            }

            parent = parent.getSymbolSpaceRoot(v);
            Dom dom = parent.resolveReference(v,returnType.getName());
            if (dom!=null) {
                return returnType.cast(dom.get());
            } else {
                throw new IllegalArgumentException("Cannot find an instance of " + returnType + " named " + v);
            }
        }

        @Override
        String elementValue(Object element) {
            return Dom.unwrap((ConfigBeanProxy) element).getKey();
        }
    }

    static class AttributeLeaf extends Leaf {

        public final String dataType;
        
        AttributeLeaf(String xmlName, String dataType) {
            super(xmlName);
            this.dataType = dataType;
        }

        /**
         * Is multiple values allowed?
         */
        public boolean isCollection() {
            return false;
        }

        /**
         * is is a reference ?
         */
        public boolean isReference() {
            return false;
        }

        /**
         * Gets the value from {@link Dom} in the specified type.
         *
         * @param dom        The DOM instance to get the value from.
         * @param returnType The expected type of the returned object.
         *                   Valid types are (1) primitive and 'leaf' Java types, such as {@link String},
         *                   (2) {@link ConfigBeanProxy}, (3) and its collections.
         */
        public Object get(Dom dom, Type returnType) {
            String v = dom.attribute(xmlName);
            return convertLeafValue(dom, Types.erasure(returnType), v);
        }

        /**
         * Sets the value to {@link Dom}.
         */
        public void set(Dom dom, Object arg) {
            dom.attribute(xmlName, arg==null?null:arg.toString());
        }

        public String getDefaultValue() {
            return null;
        }
    }

    static final class AttributeLeafWithDefaultValue extends AttributeLeaf {
        public final String dv;
        AttributeLeafWithDefaultValue(String xmlName, String dataType, String dv) {
            super(xmlName, dataType);
            this.dv = dv;
        }
        @Override
        public Object get(Dom dom, Type rt) {
            Object value = super.get(dom, rt);
            if (value == null)
                return (dv);
            return value;
        }
        public String getDefaultValue() {
            return dv;
        }
    }

    static final class ReferenceAttributeLeaf extends AttributeLeaf {
        ReferenceAttributeLeaf(String xmlName, String dataType) {
            super(xmlName, dataType);
        }

        @Override
        public boolean isReference() {
            return true;
        }

        @Override
        public Object get(Dom dom, Type returnType) {
            String id = dom.attribute(xmlName);
            // since the id is supposed to be the element's key, if no key, no element.
            if (id==null) {
                return null;
            }

            Class<?> type = Types.erasure(returnType);
            
            // let's look first the fast way.
            Object candidate = dom.getHabitat().getService(type, id);
            if (candidate!=null) {
                return type.cast(candidate);
            }

            dom = dom.getSymbolSpaceRoot(id);
            return type.cast(dom.resolveReference(id,type.getName()).get());
        }

        @Override
        public void set(Dom dom, Object arg) {
            Dom target = (Dom) arg;
            dom.attribute(xmlName, arg==null?null:target.getKey());
        }
    }
    
    static final class SingleLeaf extends Leaf {
        SingleLeaf(String xmlName) {
            super(xmlName);
        }

        public boolean isCollection() {
            return false;
        }

        public Object get(Dom dom, Type returnType) {
            // leaf types
            String v = dom.leafElement(xmlName);
            return convertLeafValue(dom, Types.erasure(returnType), v);
        }

        public void set(Dom dom, Object arg) {
            if(arg==null) {
                dom.removeLeafElement(xmlName, dom.leafElement(xmlName));
            } else {
                dom.setLeafElements(xmlName,arg.toString());
            }
        }
    }

    static final class ReferenceElementLeaf extends Leaf {
        public ReferenceElementLeaf(String xmlName) {
            super(xmlName);
        }

        @Override
        public boolean isCollection() {
            return false;
        }

        @Override
        public Object get(Dom dom, Type returnType) {
            String id = dom.leafElement(xmlName);
            Class<?> type = Types.erasure(returnType);

            // let's look first the fast way.
            Object candidate = dom.getHabitat().getService(type, id);
            if (candidate!=null) {
                return type.cast(candidate);
            }

            dom = dom.getSymbolSpaceRoot(id);
            return type.cast(dom.resolveReference(id,type.getName()).get());
        }

        @Override
        public void set(Dom dom, Object arg) {
            if(arg==null) {
                dom.removeLeafElement(xmlName, dom.leafElement(xmlName));
            } else {
                dom.setLeafElements(xmlName,((Dom) arg).getKey());
            }
        }
    }
    
    private final static String INDEX_KEY = "index";

    /**
     * @param description
     *      The description of the model as written in {@link InhabitantsFile the inhabitants file}.
     */
    public ConfigModel(DomDocument document,
            ActiveDescriptor<? extends ConfigInjector> injector,
            Map<String, List<String>> description,
            ServiceLocator locator) {
        if(description==null)
            throw new ConfigurationException("%s doesn't have any metadata",injector.getImplementation());

        document.models.put(injector,this); // register now so that cyclic references are handled correctly.
        this.injector = injector;
        this.classLoaderHolder = new SafeHk2Loader((injector.getLoader() == null) ?
                new HK2LoaderImpl() : injector.getLoader() );
        
        this.locator = locator;
        String targetTypeName=null,indexTypeName=null;
        String key = null;
        for (Map.Entry<String, List<String>> e : description.entrySet()) {
            String name = e.getKey();
            String value = e.getValue().size()>0 ? e.getValue().get(0) : null;
            if(name.startsWith("@")) {
                // TODO: handle value.equals("optional") and value.equals("required") distinctively.
                String attributeName = name.substring(1);
                String dv = getMetadataFieldKeyedAs(e.getValue(), "default:");  //default value
                String dt = getMetadataFieldKeyedAs(e.getValue(), "datatype:"); //type
                AttributeLeaf leaf = null;
                if (dv == null) {
                    if (e.getValue().contains("reference")) {
                        leaf = new ReferenceAttributeLeaf(attributeName, dt);
                    } else {
                        leaf = new AttributeLeaf(attributeName, dt);
                    }
                } else
                    leaf = new AttributeLeafWithDefaultValue(attributeName, dt, dv);
                attributes.put(attributeName, leaf);
            } else
            if(name.startsWith("<")) {
                if (e.getValue().size()>0) {
                    String elementName = name.substring(1, name.length() - 1);
                   elements.put(elementName,parseValue(elementName,document,e.getValue()));
                }
            } else
            if(name.equals(ConfigMetadata.TARGET))
                targetTypeName = value;
            else
            if(name.equals(ConfigMetadata.KEYED_AS))
                indexTypeName = value;
            else
            if(name.equals(ConfigMetadata.KEY))
                key = value;
        }
        if(targetTypeName==null)
            throw new ConfigurationException("%s doesn't have the mandatory '%s' metadata", injector.getImplementation(), ConfigMetadata.TARGET);
        if(key==null ^ indexTypeName==null)
            throw new ConfigurationException("%s has inconsistent '%s=%s' and '%s=%s' metadata",
                ConfigMetadata.KEY, key, ConfigMetadata.TARGET, indexTypeName);
        this.targetTypeName = targetTypeName;
        this.keyedAs = indexTypeName;
        this.key = key;
        this.contracts = getMetadataFromDescription(description, ConfigMetadata.TARGET_CONTRACTS);
        this.symbolSpaces = new HashSet<String>(getMetadataFromDescription(description, "symbolSpaces"));

        String tagName = null;
        for (String v : getMetadataFromDescription(description, INDEX_KEY)) {
            if(v.startsWith(ELEMENT_NAME_PREFIX))
                tagName = v.substring(ELEMENT_NAME_PREFIX.length());
        }
        this.tagName = tagName == null ? injector.getName() : tagName;
    }

    private static final String ELEMENT_NAME_PREFIX = ConfigInjector.class.getName() + ':';

    /**
     * Finds the {@link Property} from either {@link #elements} or {@link #attributes}.
     * @param xmlName
     *      XML name to be searched.
     * @return null
     *      if none is found.
     */
    public Property findIgnoreCase(String xmlName) {
        // first try the exact match to take our chance
        Property a = attributes.get(xmlName);
        if(a!=null)     return a;
        a = elements.get(xmlName);
        if(a!=null)     return a;

        // exhaustive search
        a = _findIgnoreCase(xmlName, attributes);
        if(a!=null)     return a;
        return _findIgnoreCase(xmlName, elements);
    }

    private Property _findIgnoreCase(String name, Map<String, ? extends Property> map) {
        for (Map.Entry<String, ? extends Property> i : map.entrySet())
            if(i.getKey().equalsIgnoreCase(name))
                return i.getValue();
        return null;
    }

    /**
     * Parses {@link Property} object from a value in the metadata description.
     */
    private Property parseValue(String elementName, DomDocument document, List<String> values) {
        String value = values.size()>0 ? values.get(0) : null;
        if (value==null)
            return null;



        boolean collection = false;
        if(value.startsWith("collection:")) {
            collection = true;
            value = value.substring(11);
        }

        boolean reference = values.contains("reference");
        Property prop;
        if(value.equals("leaf")) {
            prop = collection?reference?new ReferenceCollectionLeaf(elementName):new CollectionLeaf(elementName):
                    reference?new ReferenceElementLeaf(elementName):new SingleLeaf(elementName);
        } else {
            // this element is a reference to another configured inhabitant.
            // figure that out.
            ConfigModel model = document.buildModel(value);
            prop = collection?new CollectionNode(model,elementName):new SingleNode(model,elementName);
        }

        for (String s  : values) {
            if (s.startsWith("@")) {
                 String annotationType = s.substring(1);
                 prop.annotations.add(annotationType);               
            }
        }
        return prop;
    }
    
    private static String getMetadataFieldKeyedAs(List<String> strings, String name) {
        if (strings == null || strings.size() == 0 || name == null)
            return ( null );
        String dv = null;
        for (String s : strings) {
            if (s.startsWith(name)) {
                dv = s.substring(name.length());
                break;
            }
        }
        return ( dv );        
    }
    private static List<String> getMetadataFromDescription(Map<String, List<String>> map, String key) {
        return map.get(key) == null ? Collections.EMPTY_LIST : map.get(key);
    }
    
    private static class SafeHk2Loader implements HK2Loader {
        private final HK2Loader delegate;
        
        private SafeHk2Loader(HK2Loader delegate) {
            this.delegate = delegate;
        }

        /* (non-Javadoc)
         * @see org.glassfish.hk2.api.HK2Loader#loadClass(java.lang.String)
         */
        @Override
        public Class<?> loadClass(final String className) throws MultiException {
            return AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {

                @Override
                public Class<?> run() {
                    return delegate.loadClass(className);
                }
                
            });
        }
        
    }
}
