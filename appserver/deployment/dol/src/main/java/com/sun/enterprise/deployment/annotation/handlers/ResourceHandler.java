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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2026] Payara Foundation and/or its affiliates
package com.sun.enterprise.deployment.annotation.handlers;

import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.InjectionTarget;
import com.sun.enterprise.deployment.MessageDestinationReferenceDescriptor;
import com.sun.enterprise.deployment.MetadataSource;
import com.sun.enterprise.deployment.ResourceEnvReferenceDescriptor;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.deployment.WSDolSupport;
import com.sun.enterprise.deployment.annotation.context.ResourceContainerContext;
import com.sun.enterprise.deployment.annotation.context.ResourceContainerContextImpl;
import com.sun.enterprise.deployment.util.DOLUtils;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.AnnotationValues;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jvnet.hk2.annotations.Service;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import static com.sun.enterprise.util.StringUtils.ok;

/**
 * This handler is responsible for handling the jakarta.annotation.Resource
 *
 * @author Shing Wai Chan
 */
@Service
@AnnotationHandlerFor(Resource.class)
public class ResourceHandler extends AbstractResourceHandler {

    @Inject
    private ServiceLocator habitat;

    @Inject
    private Provider<WSDolSupport> wSDolSupportProvider;

    /** the types of an env-entry, primitives included, mapped to the name of the wrapper type */
    private static final Map<String, String> envEntryTypes;

    static {
        envEntryTypes = new HashMap<>();

        envEntryTypes.put(String.class.getName(), String.class.getName());
        envEntryTypes.put(Class.class.getName(), Class.class.getName());
        envEntryTypes.put(Character.class.getName(), Character.class.getName());
        envEntryTypes.put("char", Character.class.getName());
        envEntryTypes.put(Byte.class.getName(), Byte.class.getName());
        envEntryTypes.put("byte", Byte.class.getName());
        envEntryTypes.put(Short.class.getName(), Short.class.getName());
        envEntryTypes.put("short", Short.class.getName());
        envEntryTypes.put(Integer.class.getName(), Integer.class.getName());
        envEntryTypes.put("int", Integer.class.getName());
        envEntryTypes.put(Long.class.getName(), Long.class.getName());
        envEntryTypes.put("long", Long.class.getName());
        envEntryTypes.put(Boolean.class.getName(), Boolean.class.getName());
        envEntryTypes.put("boolean", Boolean.class.getName());
        envEntryTypes.put(Double.class.getName(), Double.class.getName());
        envEntryTypes.put("double", Double.class.getName());
        envEntryTypes.put(Float.class.getName(), Float.class.getName());
        envEntryTypes.put("float", Float.class.getName());
        envEntryTypes.put(Number.class.getName(), Number.class.getName());
    }

    private static final Set<String> RESOURCE_REFERENCE_TYPES = Set.of(
            "javax.sql.DataSource",
            "jakarta.jms.ConnectionFactory",
            "jakarta.jms.QueueConnectionFactory",
            "jakarta.jms.TopicConnectionFactory",
            "jakarta.mail.Session",
            "java.net.URL",
            "jakarta.resource.cci.ConnectionFactory",
            "org.omg.CORBA_2_3.ORB",
            "org.omg.CORBA.ORB",
            "jakarta.jms.XAConnectionFactory",
            "jakarta.jms.XAQueueConnectionFactory",
            "jakarta.jms.XATopicConnectionFactory");

    private static final String WEB_SERVICE_CONTEXT = "jakarta.xml.ws.WebServiceContext";

    public ResourceHandler() {
    }

    /**
     * This entry point is used both for a single @Resource and iteratively
     * from a compound @Resources processor.
     */
    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo, ResourceContainerContext[] rcContexts)
            throws AnnotationProcessorException {
        return processResource(ainfo, rcContexts, ainfo.getAnnotation());
    }

    protected HandlerProcessingResult processResource(AnnotationInfo ainfo, ResourceContainerContext[] rcContexts,
                                                      AnnotationInstance resourceAn)
            throws AnnotationProcessorException {
        IndexView index = ainfo.getIndex();
        String name = AnnotationValues.string(index, resourceAn, "name");
        String type = AnnotationValues.className(index, resourceAn, "type");
        boolean defaultType = type.equals(Object.class.getName());

        String defaultLogicalName = null;
        String defaultResourceType = null;
        InjectionTarget target = null;

        if (ElementType.FIELD.equals(ainfo.getElementType())) {
            FieldInfo f = ainfo.getAnnotatedElement().asField();
            String targetClassName = f.declaringClass().name().toString();

            defaultLogicalName = targetClassName + "/" + f.name();

            defaultResourceType = ClassHierarchy.javaName(f.type());

            target = new InjectionTarget();
            target.setFieldName(f.name());
            target.setClassName(targetClassName);
            target.setMetadataSource(MetadataSource.ANNOTATION);

        } else if (ElementType.METHOD.equals(ainfo.getElementType())) {

            MethodInfo m = ainfo.getAnnotatedElement().asMethod();
            String targetClassName = m.declaringClass().name().toString();

            validateInjectionMethod(m, ainfo);

            // Derive javabean property name.
            String propertyName = getInjectionMethodPropertyName(m, ainfo);

            // prefixing with fully qualified type name
            defaultLogicalName = targetClassName + "/" + propertyName;

            defaultResourceType = ClassHierarchy.javaName(m.parameterTypes().get(0));

            target = new InjectionTarget();
            target.setMethodName(m.name());
            target.setClassName(targetClassName);
            target.setMetadataSource(MetadataSource.ANNOTATION);

        } else if (ElementType.TYPE.equals(ainfo.getElementType())) {
            // name() and type() are required for TYPE-level @Resource
            if (name.equals("") || defaultType) {
                ClassInfo c = ainfo.getAnnotatedElement().asClass();
                log(Level.SEVERE, ainfo,
                    localStrings.getLocalString(
        "enterprise.deployment.annotation.handlers.invalidtypelevelresource",
                    "Invalid TYPE-level @Resource with name() = [{0}] and " +
                    "type = [{1}] in {2}. Each TYPE-level @Resource must " +
                    "specify both name() and type().",
                    new Object[] { name, type, c }));
                return getDefaultFailedResult();
            }
        } else {
            // can't happen
            return getDefaultFailedResult();
        }

        // NOTE that default value is Object.class, not null
        String resourceType = defaultType ? defaultResourceType : type;
        String logicalName = name.equals("") ? defaultLogicalName : name;

        /*
         * Get corresponding class type.  This does the appropriate
         * mapping for primitives.  For everything else, the type is
         * unchanged.  Since the @Resource.type() attribute can't be a
         * primitive, the default type is used.
         */
        if (envEntryTypes.containsKey(resourceType)) {
            resourceType = envEntryTypes.get(resourceType);
        }

        EnvironmentProperty[] descriptors = getDescriptors(index, resourceType, logicalName, rcContexts);

        String lookup = AnnotationValues.string(index, resourceAn, "lookup");
        String description = AnnotationValues.string(index, resourceAn, "description");
        String mappedName = AnnotationValues.string(index, resourceAn, "mappedName");
        for (EnvironmentProperty desc : descriptors) {
            if (target != null)
                desc.addInjectionTarget(target);

            if (!ok(desc.getName())) { // a new one
                desc.setName(logicalName);
            }
            if (!ok(desc.getInjectResourceType())) {
                // if the optional resource type is not set,
                // set it using the resource type of field/method
                desc.setInjectResourceType(resourceType);
            }

            if (target != null && target.getFieldName() != null && !ok(desc.getValue())) {
                String valueFound = searchValueIfAvailable(target.getFieldName(), descriptors);
                if (!valueFound.isEmpty()) {
                    desc.setValue(valueFound);
                }
            }

            if (!ok(desc.getDescription()) && ok(description))
                desc.setDescription(description);

            // merge lookup-name and mapped-name
            if (!desc.hasLookupName() && !desc.isSetValueCalled() && ok(lookup))
                desc.setLookupName(lookup);
            if (!ok(desc.getMappedName()) && ok(mappedName))
                desc.setMappedName(mappedName);

            if (desc instanceof ResourceReferenceDescriptor) {
                ResourceReferenceDescriptor rdesc = (ResourceReferenceDescriptor)desc;
                if (!rdesc.hasAuthorization()) {
                    String authenticationType = AnnotationValues.enumName(index, resourceAn, "authenticationType");
                    switch (authenticationType) {
                    case "APPLICATION":
                        rdesc.setAuthorization(ResourceReferenceDescriptor.APPLICATION_AUTHORIZATION);
                        break;
                    case "CONTAINER":
                        rdesc.setAuthorization(ResourceReferenceDescriptor.CONTAINER_AUTHORIZATION);
                        break;
                    default:    // should never happen
                        log(Level.SEVERE, ainfo,
                            localStrings.getLocalString(
        "enterprise.deployment.annotation.handlers.invalidauthenticationtype",
                            "Invalid AuthenticationType [{0}] in @Resource " +
                            "with name() = [{1}] and " +
                            "type = [{1}] in {2}.",
                            new Object[] { authenticationType, name, type, ainfo.getAnnotatedElement() }));
                        return getDefaultFailedResult();
                    }
                }
                if (!rdesc.hasSharingScope()) {
                    rdesc.setSharingScope(AnnotationValues.bool(index, resourceAn, "shareable") ?
                         ResourceReferenceDescriptor.RESOURCE_SHAREABLE :
                         ResourceReferenceDescriptor.RESOURCE_UNSHAREABLE);
                }
            }
        }

        return getDefaultProcessedResult();
    }

    private String searchValueIfAvailable(String fieldName, EnvironmentProperty[] descriptors) {
        return Arrays.stream(descriptors)
                .filter(d -> d.getDisplayName().equalsIgnoreCase(fieldName) ||
                        fieldName.toLowerCase().contains(d.getDisplayName().toLowerCase()))
                .filter(EnvironmentProperty::hasAValue)
                .findFirst()
                .map(EnvironmentProperty::getValue)
                .orElse("");
    }

    /**
     * Return ResourceReferenceDescriptors with given name if exists or a new
     * one without name being set.
     * @param resourceType the name of the type of the resource
     * @param logicalName
     * @param rcContexts
     * @return an array of ResourceReferenceDescriptor
     */
    private EnvironmentProperty[] getDescriptors(IndexView index, String resourceType, String logicalName,
                                                 ResourceContainerContext[] rcContexts) {

        boolean webServiceContextAvailable = false;
        try {
            WSDolSupport support  = wSDolSupportProvider.get();
            if (support!=null) {
                webServiceContextAvailable = support.getType(WEB_SERVICE_CONTEXT) != null;
            }
        }   catch(Exception e) {
            // we don't care, either we don't have the class, ot the bundled is not installed
        }
        if (resourceType.equals("jakarta.jms.Queue") || resourceType.equals("jakarta.jms.Topic")) {
            return getMessageDestinationReferenceDescriptors(logicalName, rcContexts);
        } else if (envEntryTypes.containsKey(resourceType) || isEnum(index, resourceType)) {
            return getEnvironmentPropertyDescriptors(logicalName, rcContexts);
        } else if (RESOURCE_REFERENCE_TYPES.contains(resourceType) ||
                webServiceContextAvailable && resourceType.equals(WEB_SERVICE_CONTEXT) ||
                DOLUtils.isRAConnectionFactory(habitat, resourceType, ((ResourceContainerContextImpl)rcContexts[0]).getAppFromDescriptor()) ) {
            return getResourceReferenceDescriptors(logicalName, rcContexts);
        } else {
            return getResourceEnvReferenceDescriptors(logicalName, rcContexts);
        }
    }

    private static boolean isEnum(IndexView index, String typeName) {
        ClassInfo type = index.getClassByName(DotName.createSimple(typeName));
        return type != null && type.isEnum();
    }

    /**
     * Return ResourceReferenceDescriptors with given name if exists or a new
     * one without name being set.
     * @param logicalName
     * @param rcContexts
     * @return an array of ResourceReferenceDescriptor
     */
    private ResourceReferenceDescriptor[] getResourceReferenceDescriptors(String logicalName,
            ResourceContainerContext[] rcContexts) {
        ResourceReferenceDescriptor[] resourceRefs = new ResourceReferenceDescriptor[rcContexts.length];
        for (int i = 0; i < rcContexts.length; i++) {
            ResourceReferenceDescriptor resourceRef = rcContexts[i].getResourceReference(logicalName);
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
    private MessageDestinationReferenceDescriptor[] getMessageDestinationReferenceDescriptors(String logicName,
            ResourceContainerContext[] rcContexts) {

        MessageDestinationReferenceDescriptor[] msgDestRefs = new MessageDestinationReferenceDescriptor[rcContexts.length];
        for (int i = 0; i < rcContexts.length; i++) {
            MessageDestinationReferenceDescriptor msgDestRef = rcContexts[i].getMessageDestinationReference(logicName);
            if (msgDestRef == null) {
               msgDestRef = new MessageDestinationReferenceDescriptor();
               rcContexts[i].addMessageDestinationReferenceDescriptor(msgDestRef);
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
    private ResourceEnvReferenceDescriptor[] getResourceEnvReferenceDescriptors(String logicName,
            ResourceContainerContext[] rcContexts) {

        ResourceEnvReferenceDescriptor[] resourceEnvRefs = new ResourceEnvReferenceDescriptor[rcContexts.length];
        for (int i = 0; i < rcContexts.length; i++) {
            ResourceEnvReferenceDescriptor resourceEnvRef = rcContexts[i].getResourceEnvReference(logicName);
            if (resourceEnvRef == null) {
               resourceEnvRef = new ResourceEnvReferenceDescriptor();
               rcContexts[i].addResourceEnvReferenceDescriptor(resourceEnvRef);
            }
            resourceEnvRefs[i] = resourceEnvRef;
        }

        return resourceEnvRefs;
    }

    /**
     * Return EnvironmentProperty descriptors with given name
     * if exists or a new one without name being set.
     * @param logicalName
     * @param rcContexts
     * @return an array of EnvironmentProperty descriptors
     */
    private EnvironmentProperty[] getEnvironmentPropertyDescriptors(String logicalName,
            ResourceContainerContext[] rcContexts) {

        Collection<EnvironmentProperty> envEntries = new ArrayList<>();

        for (ResourceContainerContext rcContext : rcContexts) {
            EnvironmentProperty envEntry = rcContext.getEnvEntry(logicalName);
            // For @Resource declarations that map to env-entries, if there
            // is no corresponding deployment descriptor entry, it's ok to
            // treat it as an env-entry whose value is given by the field
            // or method's injected value.
            if (envEntry != null) {
                envEntries.add(envEntry);
            } else {
                envEntry = new EnvironmentProperty();
                envEntries.add(envEntry);
                rcContext.addEnvEntryDescriptor(envEntry);
            }
        }

        return envEntries.toArray(new EnvironmentProperty[envEntries.size()]);
    }
}
