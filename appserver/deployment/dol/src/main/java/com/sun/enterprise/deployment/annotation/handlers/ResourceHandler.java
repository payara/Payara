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

package com.sun.enterprise.deployment.annotation.handlers;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.annotation.context.ResourceContainerContext;
import com.sun.enterprise.deployment.annotation.context.ResourceContainerContextImpl;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import javax.inject.Provider;

import static com.sun.enterprise.util.StringUtils.ok;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * This handler is responsible for handling the javax.annotation.Resource
 * annotation.
 */
@Service
@AnnotationHandlerFor(Resource.class)
public class ResourceHandler extends AbstractResourceHandler {

    @Inject
    private ServiceLocator habitat;

    @Inject 
    private Provider<WSDolSupport> wSDolSupportProvider;

    // Map of all @Resource types that map to env-entries and their
    // corresponding types.  
    // XXX - this needs to be synchronized with the list in
    // com.sun.enterprise.deployment.EnvironmentProperty
    private static final Map<Class, Class> envEntryTypes;

    static {

        envEntryTypes = new HashMap<Class, Class>();

        envEntryTypes.put(String.class, String.class);

        envEntryTypes.put(Class.class, Class.class);

        envEntryTypes.put(Character.class, Character.class);
        envEntryTypes.put(Character.TYPE, Character.class);
        envEntryTypes.put(char.class, Character.class);

        envEntryTypes.put(Byte.class, Byte.class);
        envEntryTypes.put(Byte.TYPE, Byte.class);
        envEntryTypes.put(byte.class, Byte.class);

        envEntryTypes.put(Short.class, Short.class);
        envEntryTypes.put(Short.TYPE, Short.class);
        envEntryTypes.put(short.class, Short.class);

        envEntryTypes.put(Integer.class, Integer.class);
        envEntryTypes.put(Integer.TYPE, Integer.class);
        envEntryTypes.put(int.class, Integer.class);

        envEntryTypes.put(Long.class, Long.class);        
        envEntryTypes.put(Long.TYPE, Long.class);        
        envEntryTypes.put(long.class, Long.class);        

        envEntryTypes.put(Boolean.class, Boolean.class);
        envEntryTypes.put(Boolean.TYPE, Boolean.class);
        envEntryTypes.put(boolean.class, Boolean.class);

        envEntryTypes.put(Double.class, Double.class);
        envEntryTypes.put(Double.TYPE, Double.class);
        envEntryTypes.put(double.class, Double.class);

        envEntryTypes.put(Float.class, Float.class);
        envEntryTypes.put(Float.TYPE, Float.class);
        envEntryTypes.put(float.class, Float.class);

        envEntryTypes.put(Number.class, Number.class);
    }
        
    public ResourceHandler() {
    }

    /**
     * This entry point is used both for a single @Resource and iteratively
     * from a compound @Resources processor.
     */
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
            ResourceContainerContext[] rcContexts)
            throws AnnotationProcessorException {

        Resource resourceAn = (Resource)ainfo.getAnnotation();
        return processResource(ainfo, rcContexts, resourceAn);
    }

    protected HandlerProcessingResult processResource(AnnotationInfo ainfo,
                                   ResourceContainerContext[] rcContexts, 
                                   Resource resourceAn)
        throws AnnotationProcessorException {

        ResourceReferenceDescriptor resourceRefs[] = null;

        String defaultLogicalName = null;
        Class defaultResourceType = null;
        InjectionTarget target = null;

        if (ElementType.FIELD.equals(ainfo.getElementType())) {
            Field f = (Field)ainfo.getAnnotatedElement();
            String targetClassName = f.getDeclaringClass().getName();

            defaultLogicalName = targetClassName + "/" + f.getName();

            defaultResourceType = f.getType();

            target = new InjectionTarget();
            target.setFieldName(f.getName());
            target.setClassName(targetClassName);
            target.setMetadataSource(MetadataSource.ANNOTATION);

        } else if (ElementType.METHOD.equals(ainfo.getElementType())) {

            Method m = (Method)ainfo.getAnnotatedElement();
            String targetClassName = m.getDeclaringClass().getName();

            validateInjectionMethod(m, ainfo);

            // Derive javabean property name.
            String propertyName = getInjectionMethodPropertyName(m, ainfo);

            // prefixing with fully qualified type name 
            defaultLogicalName = targetClassName + "/" + propertyName;

            defaultResourceType = m.getParameterTypes()[0];

            target = new InjectionTarget();
            target.setMethodName(m.getName());
            target.setClassName(targetClassName);
            target.setMetadataSource(MetadataSource.ANNOTATION);

        } else if (ElementType.TYPE.equals(ainfo.getElementType())) {
            // name() and type() are required for TYPE-level @Resource
            if (resourceAn.name().equals("") ||
                    resourceAn.type() == Object.class) {
                Class c = (Class) ainfo.getAnnotatedElement();
                log(Level.SEVERE, ainfo,
                    localStrings.getLocalString(
        "enterprise.deployment.annotation.handlers.invalidtypelevelresource",
                    "Invalid TYPE-level @Resource with name() = [{0}] and " +
                    "type = [{1}] in {2}. Each TYPE-level @Resource must " +
                    "specify both name() and type().",
                    new Object[] { resourceAn.name(), resourceAn.type(), c }));
                return getDefaultFailedResult();
            }
        } else {
            // can't happen
            return getDefaultFailedResult();
        }

        // NOTE that default value is Object.class, not null
        Class resourceType = (resourceAn.type() == Object.class) ?
                defaultResourceType : resourceAn.type();
        String logicalName = resourceAn.name().equals("") ?
                defaultLogicalName : resourceAn.name();

        /*
         * Get corresponding class type.  This does the appropriate
         * mapping for primitives.  For everything else, the type is
         * unchanged.  Really onlt need to do this for simple env-entries,
         * but it shouldn't hurt to do it for everything.
         */
        if (envEntryTypes.containsKey(resourceType))
            resourceType = envEntryTypes.get(resourceType);

        EnvironmentProperty[] descriptors =
            getDescriptors(resourceType, logicalName, rcContexts, resourceAn);

        for (EnvironmentProperty desc : descriptors) {
            if (target != null)
                desc.addInjectionTarget(target);

            if (!ok(desc.getName())) { // a new one
                desc.setName(logicalName);
            }
            if (!ok(desc.getInjectResourceType())) {              
                // if the optional resource type is not set, 
                // set it using the resource type of field/method
                desc.setInjectResourceType(resourceType.getName());
            }

            // merge description
            if (!ok(desc.getDescription()) && ok(resourceAn.description()))
                desc.setDescription(resourceAn.description());

            // merge lookup-name and mapped-name
            if (!desc.hasLookupName() && !desc.isSetValueCalled() &&
                    ok(getResourceLookupValue(resourceAn, ainfo)))
                desc.setLookupName(getResourceLookupValue(resourceAn, ainfo));
            if (!ok(desc.getMappedName()) && ok(resourceAn.mappedName()))
                desc.setMappedName(resourceAn.mappedName());

            // merge authentication-type and shareable
            if (desc instanceof ResourceReferenceDescriptor) {
                ResourceReferenceDescriptor rdesc =
                    (ResourceReferenceDescriptor)desc;
                if (!rdesc.hasAuthorization()) {
                    switch (resourceAn.authenticationType()) {
                    case APPLICATION:
                        rdesc.setAuthorization(
                         ResourceReferenceDescriptor.APPLICATION_AUTHORIZATION);
                        break;
                    case CONTAINER:
                        rdesc.setAuthorization(
                         ResourceReferenceDescriptor.CONTAINER_AUTHORIZATION);
                        break;
                    default:    // should never happen
                        Class c = (Class) ainfo.getAnnotatedElement();
                        log(Level.SEVERE, ainfo,
                            localStrings.getLocalString(
        "enterprise.deployment.annotation.handlers.invalidauthenticationtype",
                            "Invalid AuthenticationType [{0}] in @Resource " +
                            "with name() = [{1}] and " +
                            "type = [{1}] in {2}.",
                            new Object[] { resourceAn.authenticationType(),
                                    resourceAn.name(), resourceAn.type(), c }));
                        return getDefaultFailedResult();
                    }
                }
                if (!rdesc.hasSharingScope()) {
                    rdesc.setSharingScope(resourceAn.shareable() ?
                         ResourceReferenceDescriptor.RESOURCE_SHAREABLE :
                         ResourceReferenceDescriptor.RESOURCE_UNSHAREABLE);
                }
            }
        }

        return getDefaultProcessedResult();
    }

    private EnvironmentProperty[] getDescriptors(Class resourceType,
        String logicalName, ResourceContainerContext[] rcContexts, Resource resourceAn) {
            
        Class webServiceContext = null;
        try {

            WSDolSupport support  = wSDolSupportProvider.get(); 
            if (support!=null) {
                webServiceContext = support.getType("javax.xml.ws.WebServiceContext");
            }
        }   catch(Exception e) {
            // we don't care, either we don't have the class, ot the bundled is not installed
        }
        if (resourceType.getName().equals("javax.jms.Queue") ||
                resourceType.getName().equals("javax.jms.Topic")) {
            return getMessageDestinationReferenceDescriptors(
                                                    logicalName, rcContexts);
        } else if (envEntryTypes.containsKey(resourceType) ||
                resourceType.isEnum()) {
            return getEnvironmentPropertyDescriptors(logicalName, rcContexts,
                                                    resourceAn);
        } else if (resourceType == javax.sql.DataSource.class ||
                resourceType.getName().equals("javax.jms.ConnectionFactory") ||
                resourceType.getName().equals("javax.jms.QueueConnectionFactory") ||
                resourceType.getName().equals("javax.jms.TopicConnectionFactory") ||
                resourceType == webServiceContext ||
                resourceType.getName().equals("javax.mail.Session") || 
                resourceType.getName().equals("java.net.URL") ||
                resourceType.getName().equals("javax.resource.cci.ConnectionFactory") ||
                resourceType == org.omg.CORBA_2_3.ORB.class || 
                resourceType == org.omg.CORBA.ORB.class || 
                resourceType.getName().equals("javax.jms.XAConnectionFactory") ||
                resourceType.getName().equals("javax.jms.XAQueueConnectionFactory") ||
                resourceType.getName().equals("javax.jms.XATopicConnectionFactory") || 
                DOLUtils.isRAConnectionFactory(habitat, resourceType.getName(), ((ResourceContainerContextImpl)rcContexts[0]).getAppFromDescriptor()) ) {
            return getResourceReferenceDescriptors(logicalName, rcContexts);
        } else {
            return getResourceEnvReferenceDescriptors(logicalName,
                                                            rcContexts);
        }
    }

    /**
     * Return ResourceReferenceDescriptors with given name if exists or a new
     * one without name being set.
     * @param logicalName
     * @param rcContexts
     * @return an array of ResourceReferenceDescriptor
     */
    private ResourceReferenceDescriptor[] getResourceReferenceDescriptors(
            String logicalName, ResourceContainerContext[] rcContexts) {
        ResourceReferenceDescriptor resourceRefs[] =
                new ResourceReferenceDescriptor[rcContexts.length];
        for (int i = 0; i < rcContexts.length; i++) {
            ResourceReferenceDescriptor resourceRef =
                rcContexts[i].getResourceReference(logicalName);
            if (resourceRef == null) {
                resourceRef = new ResourceReferenceDescriptor();
                rcContexts[i].addResourceReferenceDescriptor(resourceRef);
            }
            resourceRefs[i] = resourceRef;
        }

        return resourceRefs;
    }

    /**
     * Return MessageDestinationReferenceDescriptors with given name 
     * if exists or a new one without name being set.
     * @param logicName
     * @param rcContexts
     * @return an array of message destination reference descriptors
     */
    private MessageDestinationReferenceDescriptor[] 
        getMessageDestinationReferenceDescriptors
        (String logicName, ResourceContainerContext[] rcContexts) {
            
        MessageDestinationReferenceDescriptor msgDestRefs[] =
                new MessageDestinationReferenceDescriptor[rcContexts.length];
        for (int i = 0; i < rcContexts.length; i++) {
            MessageDestinationReferenceDescriptor msgDestRef =
                rcContexts[i].getMessageDestinationReference(logicName);
            if (msgDestRef == null) {
               msgDestRef = new MessageDestinationReferenceDescriptor();
               rcContexts[i].addMessageDestinationReferenceDescriptor(
                   msgDestRef);
            }
            msgDestRefs[i] = msgDestRef;
        }

        return msgDestRefs;
    }

    /**
     * Return ResourceEnvReferenceDescriptors with given name
     * if exists or a new one without name being set.
     * @param logicName
     * @param rcContexts
     * @return an array of resource env reference descriptors
     */
    private ResourceEnvReferenceDescriptor[]
        getResourceEnvReferenceDescriptors
        (String logicName, ResourceContainerContext[] rcContexts) {

        ResourceEnvReferenceDescriptor resourceEnvRefs[] =
                new ResourceEnvReferenceDescriptor[rcContexts.length];
        for (int i = 0; i < rcContexts.length; i++) {
            ResourceEnvReferenceDescriptor resourceEnvRef =
                rcContexts[i].getResourceEnvReference(logicName);
            if (resourceEnvRef == null) {
               resourceEnvRef = new ResourceEnvReferenceDescriptor();
               rcContexts[i].addResourceEnvReferenceDescriptor(
                   resourceEnvRef);
            }
            resourceEnvRefs[i] = resourceEnvRef;
        }

        return resourceEnvRefs;
    }

    /**
     * Return EnvironmentProperty descriptors with the given name 
     * if it exists or a new one without name being set.
     *
     * @param logicalName       the JNDI name
     * @param rcContexts
     * @return an array of EnvironmentProperty descriptors
     */
    private EnvironmentProperty[] getEnvironmentPropertyDescriptors(
                                    String logicalName,
                                    ResourceContainerContext[] rcContexts,
                                    Resource annotation) {

        Collection<EnvironmentProperty> envEntries =
            new ArrayList<EnvironmentProperty>();

        for (int i = 0; i < rcContexts.length; i++) {
            EnvironmentProperty envEntry =
                rcContexts[i].getEnvEntry(logicalName);
            // For @Resource declarations that map to env-entries, if there
            // is no corresponding deployment descriptor entry that has a
            // value and no lookup(), it's treated as if the declaration
            // doesn't exist.
            // A common case is that the @Resource is applied to a field
            // with a default value which was not overridden by the deployer.
            if (envEntry != null) {
                envEntries.add(envEntry);
            } else {
                envEntry = new EnvironmentProperty();
                envEntries.add(envEntry);
                rcContexts[i].addEnvEntryDescriptor(envEntry);
            }
        }

        return envEntries.toArray(new EnvironmentProperty[envEntries.size()]);
    }

    /**
     * Return the value of the "lookup" element of the @Resource annotation.
     * This method handles the case where the Resource class is an older
     * version before the lookup element was added; in that case access to
     * the lookup element will cause a NoSuchMethodError, which is caught
     * and ignored (with a warning message).
     *
     * @return the value of the lookup element
     */
    private String getResourceLookupValue(Resource annotation,
                                            AnnotationInfo ainfo) {
        String lookupValue = "";
        try {
            lookupValue = annotation.lookup();
        } catch(NoSuchMethodError nsme) {
            // Probably means lib endorsed dir is not set and an older version
            // of Resource is being picked up from JDK.
            // Don't treat this as a fatal error.
            try {
                log(Level.WARNING, ainfo,
                    localStrings.getLocalString(
                "enterprise.deployment.annotation.handlers.wrongresourceclass",
                        "Incorrect @Resource annotation class definition - " +
                        "missing lookup attribute"));
            } catch (AnnotationProcessorException ex) { }
        }
        return lookupValue;
    }
}
