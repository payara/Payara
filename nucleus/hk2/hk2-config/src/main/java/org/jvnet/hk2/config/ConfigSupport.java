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

import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ProxyCtl;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.api.ConfigurationUtilities;

import javax.inject.Inject;

import java.beans.PropertyVetoException;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;

import org.jvnet.tiger_types.Types;

/**
  * <p>
  * Helper class to execute some code on configuration objects while taking
  * care of the transaction boiler plate code.
  * </p>
  * <p>
  * Programmers that wish to apply some changes to configuration objects
  * can use these convenience methods to reduce the complexity of handling
  * transactions.
  * </p>
  * <p>
  * For instance, say a programmer need to change the HttpListener port from
  * 8080 to 8989, it just needs to do :
  * </p>
  * <pre>
  *     ... in his code somewhere ...
  *     HttpListener httpListener = domain.get...
  *
  *     // If the programmer tries to modify the httpListener directly
  *     // it will get an exception
  *     httpListener.setPort("8989"); // will generate a PropertyVetoException
  *
  *     // instead he needs to use a transaction and can use the helper services
  *     ConfigSupport.apply(new SingleConfigCode<HttpListener>() {
  *         public Object run(HttpListener okToChange) throws PropertyException {
  *             okToChange.setPort("8989"); // good...
  *             httpListener.setPort("7878"); // not good, exceptions still raised...
  *             return null;
  *         });
  *
  *     // Note that after this code
  *     System.out.println("Port is " + httpListener.getPort());
  *     // will display 8989
  * }
  * </pre>
  * @author Jerome Dochez
  */
@Service
public class ConfigSupport implements ConfigurationUtilities {

    @Inject
    ServiceLocator habitat;

    public static int lockTimeOutInSeconds=Integer.getInteger("org.glassfish.hk2.config.locktimeout", 3);
 
    /**
     * Execute some logic on one config bean of type T protected by a transaction
     *
     * @param code code to execute
     * @param param config object participating in the transaction
     * @return list of events that represents the modified config elements.
     * @throws TransactionFailure when code did not run successfully
     */
    public static <T extends ConfigBeanProxy> Object apply(final SingleConfigCode<T> code, T param)
        throws TransactionFailure {
        
        //ConfigBeanProxy[] objects = { param };
        return apply((new ConfigCode() {
            @SuppressWarnings("unchecked")
            public Object run(ConfigBeanProxy... objects) throws PropertyVetoException, TransactionFailure {
                return code.run((T) objects[0]);
            }
        }), param);
    }
    
    /**
     * Executes some logic on some config beans protected by a transaction.
     *
     * @param code code to execute
     * @param objects config beans participating to the transaction
     * @return list of property change events
     * @throws TransactionFailure when the code did run successfully due to a
     * transaction exception
     */
    public static Object apply(ConfigCode code, ConfigBeanProxy... objects)
            throws TransactionFailure {

        ConfigBean source = (ConfigBean) ConfigBean.unwrap(objects[0]);
        return source.getHabitat().<ConfigSupport>getService(ConfigSupport.class)._apply(code, objects);
    }

    /**
     * Executes some logic on some config beans protected by a transaction.
     *
     * @param code code to execute
     * @param objects config beans participating to the transaction
     * @return list of property change events
     * @throws TransactionFailure when the code did run successfully due to a
     * transaction exception
     */    
    public Object _apply(ConfigCode code, ConfigBeanProxy... objects)
                throws TransactionFailure {

        // the fools think they operate on the "real" object while I am
        // feeding them with writeable view. Only if the transaction succeed
        // will I apply the "changes" to the real ones.
        WriteableView[] views = new WriteableView[objects.length];

        ConfigBeanProxy[] proxies = new ConfigBeanProxy[objects.length];

        // create writeable views.
        for (int i=0;i<objects.length;i++) {
            proxies[i] = getWriteableView(objects[i]);
            views[i] = (WriteableView) Proxy.getInvocationHandler(proxies[i]);
        }

        // Of course I am not locking the live objects but the writable views.
        // if the user try to massage the real objects, he will get
        // a well deserved nasty exception
        Transaction t = new Transaction();
        for (WriteableView view : views) {
            if (!view.join(t)) {
                t.rollback();
                throw new TransactionFailure("Cannot enlist " + view.getMasterView().getProxyType()
                    + " in transaction", null);
            }
        }
        
        try {
            final Object toReturn = code.run(proxies);
            try {
                t.commit();
                if (toReturn instanceof WriteableView) {
                    return ((WriteableView) toReturn).getMasterView();
                } else {
                    return toReturn;
                }
            } catch (RetryableException e) {
                System.out.println("Retryable...");
                // TODO : do something meaninful here
                t.rollback();
                return null;
            } catch (TransactionFailure e) {
                t.rollback();
                throw e;
            }

        } catch (java.lang.reflect.UndeclaredThrowableException e) {
            t.rollback();
            Throwable throwable = e.getCause();
            if (throwable instanceof PropertyVetoException) throw new TransactionFailure(throwable.toString(), throwable);
            throw new TransactionFailure(e.toString(), e);

        } catch(TransactionFailure e) {
            t.rollback();
            throw e;
        } catch (Exception e) {
            t.rollback();
            throw new TransactionFailure(e.getMessage(), e);
        }

    }

    static <T extends ConfigBeanProxy> WriteableView getWriteableView(T s, ConfigBean sourceBean)
        throws TransactionFailure {

        WriteableView f = new WriteableView(s);
        try {
            if (sourceBean.getLock().tryLock(lockTimeOutInSeconds, TimeUnit.SECONDS)) {
                sourceBean.setWriteableView(f);
                return f;
            }
        } catch(InterruptedException e) {
            // ignore, will throw a TransactionFailure exception
        }
        throw new TransactionFailure("Config bean already locked " + sourceBean, null);
    }

    /**
     * Returns a writeable view of a configuration object
     * @param source the configured interface implementation
     * @return the new interface implementation providing write access
     * @throws TransactionFailure if the object cannot be enrolled (probably already enrolled in
     * another transaction).
     */
    public <T extends ConfigBeanProxy> T getWriteableView(final T source)
        throws TransactionFailure {
        T configBeanProxy = revealProxy(source);
        ConfigView sourceBean = (ConfigView) Proxy.getInvocationHandler(configBeanProxy);
        WriteableView writeableView = getWriteableView(configBeanProxy, (ConfigBean) sourceBean.getMasterView());
        return (T) writeableView.getProxy(sourceBean.getProxyType());
    }

    /**
     * Return the main implementation bean for a proxy.
     * @param source configuration interface proxy
     * @return the implementation bean
     */
    public static ConfigView getImpl(ConfigBeanProxy source) {

        Object bean = Proxy.getInvocationHandler(source);
        if (bean instanceof ConfigView) {
            return ((ConfigView) bean).getMasterView();
        } else {
            return (ConfigBean) bean;
        }
        
    }

    /**
     * Returns the type of configuration object this config proxy represents.
     * @param element is the configuration object
     * @return the configuration interface class
     */
    public static <T extends ConfigBeanProxy> Class<T> proxyType(T element) {
        ConfigView bean = getImpl(element);
        return bean.getProxyType();
    }

    /**
     * sort events and dispatch the changes. There will be only one notification of event
     * per event type, per object, meaning that if an object has had 3 attributes changes, the
     * Changed interface implementation will get notified only once.
     *
     * @param events of events that resulted of a successful configuration transaction
     * @param target the intended receiver of the changes notification
     * @param logger to log any issues.
     */
    public static UnprocessedChangeEvents sortAndDispatch(PropertyChangeEvent[] events, Changed target, Logger logger) {
        if ( logger == null ) throw new IllegalArgumentException();
        
        List<UnprocessedChangeEvent> unprocessed = new ArrayList<UnprocessedChangeEvent>();
        List<Dom> added = new ArrayList<Dom>();
        List<Dom> changed = new ArrayList<Dom>();

        for (PropertyChangeEvent event : events) {

            if (event.getOldValue()==null && event.getNewValue() instanceof ConfigBeanProxy) {
                // something was added
                try {
                    final ConfigBeanProxy proxy =  ConfigBeanProxy.class.cast(event.getNewValue());
                    added.add(Dom.unwrap(proxy));
                    final NotProcessed nc = target.changed(Changed.TYPE.ADD, proxyType(proxy), proxy);
                    if ( nc != null ) {
                        unprocessed.add( new UnprocessedChangeEvent(event, nc.getReason() ) );
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Exception while processing config bean changes : ", e);
                }
            }
        }

        for (PropertyChangeEvent event : events) {

            try {
                Dom eventSource = Dom.unwrap((ConfigBeanProxy) event.getSource());
                if (added.contains(eventSource)) {
                    // we don't really send the changed events for new comers.
                    continue;
                }
                ConfigBeanProxy proxy =  null;
                if (event.getNewValue()==null) {
                    try {
                        // getOldValue() can be null, we will notify a CHANGE event
                        proxy = ConfigBeanProxy.class.cast(event.getOldValue());
                    } catch (ClassCastException e) {
                        // this is ok, the old value was probably a string or something like this...
                        // we will notify the event.getSource() that it changed.
                    }
                    // new value is null, but not old value, we removed something
                    if (proxy!=null) {
                        final NotProcessed nc = target.changed(Changed.TYPE.REMOVE, proxyType(proxy), proxy );
                        if ( nc != null ) {
                            unprocessed.add( new UnprocessedChangeEvent(event, nc.getReason() ) );
                        }
                        continue;
                    }
                }
                // we fall back in this case, the newValue was nullm the old value was also null  or was not a ConfigBean,
                // we revert to just notify of the change.
                // when the new value is not null, we also send the CHANGE  on the source all the time, unless this was
                // and added config bean.
                if (!changed.contains(eventSource)) {
                    proxy =  ConfigBeanProxy.class.cast(event.getSource());
                    changed.add(eventSource);
                    final NotProcessed nc = target.changed(Changed.TYPE.CHANGE, proxyType(proxy), proxy);
                    if ( nc != null ) {
                        unprocessed.add( new UnprocessedChangeEvent(event, nc.getReason() ) );
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception while processing config bean changes : ", e);
            }
        }
        
        return new UnprocessedChangeEvents( unprocessed );
    }

    // kind of insane, just to get the proper return type for my properties.
    static private List<String> defaultPropertyValue() {
        return null;
    }

    public void apply(Map<ConfigBean, Map<String, String>> mapOfChanges) throws TransactionFailure {


        Transaction t = new Transaction();
        try {
            for (Map.Entry<ConfigBean, Map<String, String>> configBeanChange : mapOfChanges.entrySet()) {

                ConfigBean source = configBeanChange.getKey();
                ConfigBeanProxy readableView = source.getProxy(source.getProxyType());
                WriteableView writeable = getWriteableView(readableView, source);
                if (!writeable.join(t)) {
                    t.rollback();
                    throw new TransactionFailure("Cannot enlist " + source.getProxyType() + " in transaction", null);
                }
                for (Map.Entry<String, String> change : configBeanChange.getValue().entrySet()) {
                    String xmlName = change.getKey();
                    ConfigModel.Property prop = writeable.getProperty(xmlName);
                    if (prop == null) {
                        throw new TransactionFailure("Unknown property name " + xmlName + " on " + source.getProxyType(), null);
                    }
                    if (prop.isCollection()) {
                        try {
                            List<String> values = (List<String>) writeable.getter(prop,
                                    ConfigSupport.class.getDeclaredMethod("defaultPropertyValue", null).getGenericReturnType());
                            values.add(change.getValue());
                        } catch (NoSuchMethodException e) {
                            throw new TransactionFailure("Unknown property name " + xmlName + " on " + source.getProxyType(), null);
                        }
                    } else {
                        writeable.setter(prop, change.getValue(), String.class);
                    }
                }
            }
        } catch(TransactionFailure e) {
            t.rollback();
            throw e;
        } catch (Exception e) {
            t.rollback();
            throw new TransactionFailure(e.getMessage(), e);
        }
        try {
            t.commit();
        } catch (RetryableException e) {
            System.out.println("Retryable...");
            // TODO : do something meaninful here
            t.rollback();
            throw new TransactionFailure(e.getMessage(), e);
        } catch (TransactionFailure e) {
            System.out.println("failure, not retryable...");
            t.rollback();
            throw e;
        }
    }

    /**
     * Returns the list of sub-elements supported by a ConfigBean
     * @return array of classes reprensenting the sub elements of a particular
     * @throws ClassNotFoundException for severe errors with the model associated
     * with the passed config bean.
     */
    public static Class<?>[] getSubElementsTypes(ConfigBean bean)
        throws ClassNotFoundException {

        List<Class<?>> subTypes = new ArrayList<Class<?>>();
        for (ConfigModel.Property element : bean.model.elements.values()) {
            if (!element.isLeaf()) {
                ConfigModel elementModel =  ((ConfigModel.Node) element).model;
                Class<?> subType = elementModel.classLoaderHolder.loadClass(elementModel.targetTypeName);
                subTypes.add(subType);
            } else {
                if (element.isCollection()) {
                    subTypes.add(List.class);
                }
            }
        }
        return subTypes.toArray(new Class[subTypes.size()]);
    }

    /**
     * Returns the list of attributes names by the passed ConfigBean
     * @return array of String for all the attributes names
     */
    public String[] getAttributesNames(ConfigBean bean) {
        return xmlNames(bean.model.attributes.values());
    }


    /**
     * Returns the list of elements names by the passed ConfigBean
     * @return array of String for all the elements names
     */
    public String[] getElementsNames(ConfigBean bean) {
        return xmlNames(bean.model.elements.values());
    }

    private String[] xmlNames(Collection<? extends ConfigModel.Property> properties) {

        List<String> names = new ArrayList<String>();
        for (ConfigModel.Property attribute : properties) {
            names.add(attribute.xmlName());
        }
        return names.toArray(new String[names.size()]);

    }


    /**
     * Creates a new child of the passed child and add it to the parent's live
     * list of elements. The child is also initialized with the attributes passed
     * where each key represent the xml property name for the attribute and the value
     * represent the attribute's value.
     *
     * This code will be executed within a Transaction and can therefore throw
     * a TransactionFailure when the creation or settings of attributes failed.
     *
     * Example creating a new http-listener element under http-service
     *      ConfigBean httpService = ... // got it from somwhere.
     *      Map<String, String> attributes = new HashMap<String, String>();
     *      attributes.put("id", "jerome-listener");
     *      attributes.put("enabled", "true");
     *      ConfigSupport.createAndSet(httpService, HttpListener.class, attributes);
     *
     * @param parent parent config bean to which the child will be added.
     * @param childType child type
     * @param attributes map of key value pair to set on the newly created child
     * @throws TransactionFailure if the creation or attribute settings failed
     */
    public static ConfigBean createAndSet(
                final ConfigBean parent,
                final Class<? extends ConfigBeanProxy> childType,
                final Map<String, String> attributes,
                final TransactionCallBack<WriteableView> runnable)
        throws TransactionFailure {

        return createAndSet(parent, childType, convertMapToAttributeChanges(attributes), runnable);
        
    }
    /**
     * Creates a new child of the passed child and add it to the parent's live
     * list of elements. The child is also initialized with the attributes passed
     * where each key represent the xml property name for the attribute and the value
     * represent the attribute's value.
     *
     * This code will be executed within a Transaction and can therefore throw
     * a TransactionFailure when the creation or settings of attributes failed.
     *
     * Example creating a new http-listener element under http-service
     *      ConfigBean httpService = ... // got it from somwhere.
     *      Map<String, String> attributes = new HashMap<String, String>();
     *      attributes.put("id", "jerome-listener");
     *      attributes.put("enabled", "true");
     *      ConfigSupport.createAndSet(httpService, HttpListener.class, attributes);
     *
     * @param parent parent config bean to which the child will be added.
     * @param childType child type
     * @param attributes list of attribute changes to apply to the newly created child
     * @param runnable code that will be invoked as part of the transaction to add
     * more attributes or elements to the newly create type
     * @throws TransactionFailure if the creation or attribute settings failed
     */
    public static ConfigBean createAndSet(
                final ConfigBean parent,
                final Class<? extends ConfigBeanProxy> childType,
                final List<AttributeChanges> attributes,
                final TransactionCallBack<WriteableView> runnable)
        throws TransactionFailure {

        return parent.getHabitat().<ConfigSupport>getService(ConfigSupport.class).
                _createAndSet(parent, childType, attributes, runnable);
    }

    private ConfigBean _createAndSet(
                    final ConfigBean parent,
                    final Class<? extends ConfigBeanProxy> childType,
                    final List<AttributeChanges> attributes,
                    final TransactionCallBack<WriteableView> runnable)
            throws TransactionFailure {

        ConfigBeanProxy readableView = parent.getProxy(parent.getProxyType());
        ConfigBeanProxy readableChild = (ConfigBeanProxy)
                apply(new SingleConfigCode<ConfigBeanProxy>() {
            public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                return addChildWithAttributes(param, parent, childType, attributes, runnable);
            }
        }, readableView);
        return (ConfigBean) Dom.unwrap(readableChild);
    }

    /**
     * Creates a new child of the passed child and add it to the parent's live
     * list of elements. The child is also initialized with the attributes passed
     * where each key represent the xml property name for the attribute and the value
     * represent the attribute's value.
     *
     * This code will be executed within a Transaction and can therefore throw
     * a TransactionFailure when the creation or settings of attributes failed.
     *
     * Example creating a new http-listener element under http-service
     *      ConfigBean httpService = ... // got it from somwhere.
     *      Map<String, String> attributes = new HashMap<String, String>();
     *      attributes.put("id", "jerome-listener");
     *      attributes.put("enabled", "true");
     *      ConfigSupport.createAndSet(httpService, HttpListener.class, attributes);
     *
     * @param parent parent config bean to which the child will be added.
     * @param childType child type
     * @param attributes list of attributes changes to apply to the new created child
     * @throws TransactionFailure if the creation or attribute settings failed
     */
    public static ConfigBean createAndSet(
                final ConfigBean parent,
                final Class<? extends ConfigBeanProxy> childType,
                final Map<String, String> attributes)
            throws TransactionFailure {

        return createAndSet(parent, childType, attributes, null);        
    }

    public ConfigBean createAndSet(
                final ConfigBean parent,
                final Class<? extends ConfigBeanProxy> childType,
                final List<AttributeChanges> attributes)
            throws TransactionFailure {

        return createAndSet(parent, childType, attributes, null);
    }
    
    public static void deleteChild(
                final ConfigBean parent,
                final ConfigBean child)
        throws TransactionFailure {


        ConfigBeanProxy readableView = parent.getProxy(parent.getProxyType());
        apply(new SingleConfigCode<ConfigBeanProxy>() {

            public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {


                // remove the child from the parent.
                WriteableView writeableParent = (WriteableView) Proxy.getInvocationHandler(param);

                _deleteChild(parent, writeableParent, child);
                return child;
            }
        }, readableView);
    }

    /**
     * Unprotected child deletion, caller must start a transaction before calling this
     * method.
     *
     * @param parent the parent element
     * @param writeableParent the writeable view of the parent element
     * @param child the child to delete
     * @throws TransactionFailure if something goes wrong.
     */
    public static void _deleteChild(
                final ConfigBean parent,
                final WriteableView writeableParent,
                final ConfigBean child)
        throws TransactionFailure {


        final Class<? extends ConfigBeanProxy> childType = child.getProxyType();


                // get the child
                ConfigBeanProxy childProxy = child.getProxy(childType);

                // get the parent type
                Class parentProxyType = parent.getProxyType();

                // first we need to find the element associated with this type
                ConfigModel.Property element = null;
                for (ConfigModel.Property e : parent.model.elements.values()) {
                    if (!(e instanceof ConfigModel.Node)) {
                        continue;
                    }

                    ConfigModel elementModel = ((ConfigModel.Node) e).model;
                    try {
                        final Class<?> targetClass = parent.model.classLoaderHolder.loadClass(elementModel.targetTypeName);
                        if (targetClass.isAssignableFrom(childType)) {
                            element = e;
                            break;
                        }
                    } catch(Exception ex) {
                        // ok.
                    }
                }
                // now depending whether this is a collection or a single leaf,
                // we need to process this setting differently
                if (element != null) {
                    if (element.isCollection()) {
                        // this is kind of nasty, I have to find the method that returns the collection
                        // object because trying to do a element.get without having the parametized List
                        // type will not work.
                        for (Method m : parentProxyType.getMethods()) {
                            final Class returnType = m.getReturnType();
                            if (Collection.class.isAssignableFrom(returnType)  && (m.getParameterTypes().length == 0)) {
                                // this could be it...
                                if (!(m.getGenericReturnType() instanceof ParameterizedType))
                                    throw new IllegalArgumentException("List needs to be parameterized");
                                final Class itemType = Types.erasure(Types.getTypeArgument(m.getGenericReturnType(), 0));
                                if (itemType.isAssignableFrom(childType)) {
                                    List list = null;
                                    try {
                                        list = (List) m.invoke(writeableParent.getProxy(writeableParent.<ConfigBeanProxy>getProxyType()), null);
                                    } catch (IllegalAccessException e) {
                                        throw new TransactionFailure("Exception while adding to the parent", e);
                                    } catch (InvocationTargetException e) {
                                        throw new TransactionFailure("Exception while adding to the parent", e);
                                    }
                                    if (list != null) {
                                        list.remove(childProxy);
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        // much simpler, I can use the setter directly.
                        writeableParent.setter(element, null, childType);
                    }
                } else {
                    throw new TransactionFailure("Parent " + parent.getProxyType() + " does not have a child of type " + childType);
                }
    }
    
    public static List<AttributeChanges> convertMapToAttributeChanges(Map<String, String> values) {
        if (values==null) {
            return null;
        }
        List<AttributeChanges> changes = new ArrayList<AttributeChanges>();
        for(Map.Entry<String, String> entry : values.entrySet()) {
            changes.add(new SingleAttributeChange(entry.getKey(), entry.getValue()));
        }
        return changes;
    }
    
    public static class SingleAttributeChange extends AttributeChanges {
        final String[] values = new String[1];
        
        public SingleAttributeChange(String name, String value) {
            super(name);
            values[0] = value;
        }

        String[] values() {
            return values;
        }
    }

    public static class MultipleAttributeChanges extends AttributeChanges {
        final String[] values;

        public MultipleAttributeChanges(String name, String[] values) {
            super(name);
            this.values = values;
        }

        String[] values() {
            return values;
        }
    }

    public static Class<? extends ConfigBeanProxy> getElementTypeByName(ConfigBeanProxy parent, String elementName)
        throws ClassNotFoundException {

        final Dom parentDom = Dom.unwrap(parent);
        DomDocument document = parentDom.document;


        ConfigModel.Property a = parentDom.model.elements.get(elementName);
        if (a!=null) {
            if (a.isLeaf()) {
                // dochez : I am not too sure, but that should be a String @Element
                return null;
            } else {
                ConfigModel childModel = ((ConfigModel.Node) a).model;
                return (Class<? extends ConfigBeanProxy>) childModel.classLoaderHolder.loadClass(childModel.targetTypeName);
            }
        }
        // global lookup
        ConfigModel model = document.getModelByElementName(elementName);
        if (model!=null) {
            return (Class<? extends ConfigBeanProxy>) model.classLoaderHolder.loadClass(model.targetTypeName);
        }

        return null;
    }

    /**
     * Unwrap HK2 proxy to ConfigBeanProxy.
     * @param ConfigBeanProxy probably proxied by HK2.
     * @return actual ConfigBeanProxy.
     * @throws MultiException If there was an error resolving the proxy.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ConfigBeanProxy> T revealProxy(T proxy) {
        if (proxy instanceof ProxyCtl) {
            ProxyCtl proxyCtl = (ProxyCtl) proxy;
            proxy = (T) proxyCtl.__make();
        }
        return proxy;
    }
    
    @Override
    public Object addChildWithAttributes(ConfigBeanProxy param,
            ConfigBean parent,
            Class<? extends ConfigBeanProxy> childType,
            List<AttributeChanges> attributes,
            TransactionCallBack<WriteableView> runnable) throws TransactionFailure {

        // create the child
        ConfigBeanProxy child = param.createChild(childType);
        Dom dom = Dom.unwrap(child);

        // add the child to the parent.
        WriteableView writeableParent = (WriteableView) Proxy.getInvocationHandler(param);
        Class parentProxyType = parent.getProxyType();

        // first we need to find the element associated with this type
        ConfigModel.Property element = null;
        for (ConfigModel.Property e : parent.model.elements.values()) {
            if (e.isLeaf()) {
                continue;
            }
            ConfigModel elementModel =  ((ConfigModel.Node) e).model;

            if (Logger.getAnonymousLogger().isLoggable(Level.FINE)) {
                Logger.getAnonymousLogger().fine( "elementModel.targetTypeName = " + elementModel.targetTypeName +
                    ", collection: " + e.isCollection() + ", childType.getName() = " + childType.getName() );
            }
                    
            if (elementModel.targetTypeName.equals(childType.getName())) {
                element = e;
                break;
            }
            else if ( e.isCollection() ) {
                try {
                    final Class<?> tempClass = elementModel.classLoaderHolder.loadClass(elementModel.targetTypeName);
                    if ( tempClass.isAssignableFrom( childType ) ) {
                        element = e;
                        break;
                    }
                } catch (Exception ex ) { 
                    throw new TransactionFailure("EXCEPTION getting class for " + elementModel.targetTypeName, ex);
                }
            }
        }
        
        // now depending whether this is a collection or a single leaf,
        // we need to process this setting differently
        if (element != null) {
            if (element.isCollection()) {
                // this is kind of nasty, I have to find the method that returns the collection
                // object because trying to do a element.get without having the parametized List
                // type will not work.
                for (Method m : parentProxyType.getMethods()) {
                    final Class returnType = m.getReturnType();
                    if (Collection.class.isAssignableFrom(returnType) && (m.getParameterTypes().length == 0)) {
                        // this could be it...
                        if (!(m.getGenericReturnType() instanceof ParameterizedType))
                            throw new IllegalArgumentException("List needs to be parameterized");
                        final Class itemType = Types.erasure(Types.getTypeArgument(m.getGenericReturnType(), 0));
                        if (itemType.isAssignableFrom(childType)) {
                            List list = null;
                            try {
                                list = (List) m.invoke(param, null);
                            } catch (IllegalAccessException e) {
                                throw new TransactionFailure("Exception while adding to the parent", e);
                            } catch (InvocationTargetException e) {
                                throw new TransactionFailure("Exception while adding to the parent", e);
                            }
                            if (list != null) {
                                list.add(child);
                                break;
                            }
                        }
                    }
                }
            } else {
                // much simpler, I can use the setter directly.
                writeableParent.setter(element, dom.<ConfigBeanProxy>createProxy(), childType);
            }
        } else {
            throw new TransactionFailure("Parent " + parent.getProxyType() + " does not have a child of type " + childType);
        }

        WriteableView writeableChild = (WriteableView) Proxy.getInvocationHandler(child);
        applyProperties(writeableChild, attributes);

        dom.addDefaultChildren();
        dom.register();
        
        if (runnable!=null) {
            runnable.performOn(writeableChild);
        }

        return child;
    }
    
    private static void applyProperties(WriteableView target, List<? extends AttributeChanges> changes)
            throws TransactionFailure {

        if (changes != null) {
            for (AttributeChanges change : changes) {

                ConfigModel.Property prop = target.getProperty(change.getName());
                if (prop == null) {
                    throw new TransactionFailure("Unknown property name " + change.getName() + " on " + target.getProxyType());
                }
                if (prop.isCollection()) {
                    // we need access to the List
                    try {
                        List list = (List) target.getter(prop, ConfigSupport.class.getDeclaredMethod("defaultPropertyValue", null).getGenericReturnType());
                        for (String value : change.values()) {
                            list.add(value);
                        }
                    } catch (NoSuchMethodException e) {
                        throw new TransactionFailure(e.getMessage(), e);
                    }
                } else {
                    target.setter(prop, change.values()[0], String.class);
                }
            }
        }

    }
 }
