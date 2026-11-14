/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices.connector.annotation.handlers;

import jakarta.xml.ws.spi.WebServiceFeatureAnnotation;

import java.util.Map;

import java.util.HashMap;

import java.util.Collection;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Modifier;

import jakarta.xml.ws.WebServiceClient;
import jakarta.xml.ws.RespectBinding;
import jakarta.xml.ws.soap.MTOM;
import jakarta.xml.ws.soap.Addressing;

import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.AnnotationValues;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.apf.ResultType;
import org.glassfish.apf.impl.HandlerProcessingResultImpl;

import com.sun.enterprise.deployment.annotation.context.AppClientContext;
import com.sun.enterprise.deployment.annotation.context.ServiceReferenceContainerContext;
import com.sun.enterprise.deployment.InjectionTarget;
import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.types.ServiceReferenceContainer;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jvnet.hk2.annotations.Service;
import com.sun.enterprise.deployment.annotation.handlers.AbstractHandler;

import static com.sun.enterprise.util.StringUtils.ok;

/**
 * This annotation handler is responsible for processing the jakarta.jws.WebServiceRef annotation type.
 *
 * @author Jerome Dochez
 */
@Service
@AnnotationHandlerFor(jakarta.xml.ws.WebServiceRef.class)
public class WebServiceRefHandler extends AbstractHandler  {

    /**
     * @return an array of annotation types this annotation handler would
     * require to be processed (if present) before it processes it's own
     * annotation type.
     */
    @Override
    public Class<? extends Annotation>[] getTypeDependencies() {
        // it is easier if we return the array of component type. That
        // way, the @WebServiceRef is processed after the component
        // has been added to the DOL and the right EjbContext is
        // on the context stack. It won't hurt when @WebServiceRef
        // is used in appclients or web app since references are
        // declared at the bundle level.
        return getEjbAndWebAnnotationTypes();
    }

    protected HandlerProcessingResult processAWsRef(AnnotationInfo annInfo,
                AnnotationInstance annotation) throws AnnotationProcessorException {
        AnnotatedElementHandler annCtx =
            annInfo.getProcessingContext().getHandler();
        AnnotationTarget annElem = annInfo.getAnnotatedElement();
        IndexView index = annInfo.getIndex();

        String annotatedTypeName = null;
        ClassInfo declaringClass = null;
        InjectionTarget target = null;
        String defaultServiceRefName = null;
        String name = AnnotationValues.string(index, annotation, "name");

        if (annInfo.getElementType().equals(ElementType.FIELD)) {
            // this is a field injection
            FieldInfo annotatedField = annElem.asField();

            // check this is a valid field
            if (annCtx instanceof AppClientContext){
                if (!Modifier.isStatic(annotatedField.flags())){
                    throw new AnnotationProcessorException(
                            localStrings.getLocalString(
                            "enterprise.deployment.annotation.handlers.injectionfieldnotstatic",
                            "Injection fields for application clients must be declared STATIC"),
                            annInfo);
                }
            }
            
            annotatedTypeName = ClassHierarchy.javaName(annotatedField.type());
            declaringClass = annotatedField.declaringClass();
            defaultServiceRefName = declaringClass.name() + "/" +
                                        annotatedField.name();
            target = new InjectionTarget();
            target.setFieldName(annotatedField.name());
            target.setClassName(declaringClass.name().toString());
        } else if (annInfo.getElementType().equals(ElementType.METHOD)) {
            // this is a method injection
            MethodInfo annotatedMethod = annElem.asMethod();
            validateInjectionMethod(annotatedMethod, annInfo);
            
            if (annCtx instanceof AppClientContext){
                if (!Modifier.isStatic(annotatedMethod.flags())){
                    throw new AnnotationProcessorException(
                            localStrings.getLocalString(
                            "enterprise.deployment.annotation.handlers.injectionmethodnotstatic",
                            "Injection methods for application clients must be declared STATIC"),
                            annInfo);
                }
            }
            
            annotatedTypeName = ClassHierarchy.javaName(annotatedMethod.parameterType(0));
            declaringClass = annotatedMethod.declaringClass();
            // Derive javabean property name.
            String propertyName =
                getInjectionMethodPropertyName(annotatedMethod, annInfo);
            // prefixing with fully qualified type name
            defaultServiceRefName = declaringClass.name() + "/" +
                                        propertyName;
            target = new InjectionTarget();
            target.setMethodName(annotatedMethod.name());
            target.setClassName(declaringClass.name().toString());
        } else if (annInfo.getElementType().equals(ElementType.TYPE)) {
            // name must be specified.
            if (!ok(name)) {
                throw new AnnotationProcessorException(
                        localStrings.getLocalString(
                        "enterprise.deployment.annotation.handlers.nonametypelevel",
                        "TYPE-Level annotation  must specify name member."),  annInfo);                
            }
            // this is a dependency declaration, we need the service interface
            // to be specified
            annotatedTypeName = AnnotationValues.className(index, annotation, "type");
            if (annotatedTypeName.equals(Object.class.getName())) {
                throw new AnnotationProcessorException(
                        localStrings.getLocalString(
                        "enterprise.deployment.annotation.handlers.typenotfound",
                        "TYPE-level annotation symbol must specify type member."),  
                         annInfo);
            }
            declaringClass = annElem.asClass();
        } else {    
            throw new AnnotationProcessorException(
                    localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.invalidtype",
                    "annotation not allowed on this element."),  annInfo);
            
        }
        ClassInfo annotatedType = index.getClassByName(DotName.createSimple(annotatedTypeName));

        AnnotationInstance mtom = ClassHierarchy.annotation(index, annElem, MTOM.class);
        AnnotationInstance addressing = ClassHierarchy.annotation(index, annElem, Addressing.class);
        AnnotationInstance respectBinding = ClassHierarchy.annotation(index, annElem, RespectBinding.class);

        // the attribute values of the other annotations denoting web service features, by annotation type name
        Map<String, Map<String, Object>> otherAnnotations = new HashMap<>();
        Collection<AnnotationInstance> elementAnnotations = annElem.kind() == AnnotationTarget.Kind.CLASS
                ? ClassHierarchy.classAnnotations(index, annElem.asClass()) : annElem.declaredAnnotations();
        for (AnnotationInstance a : elementAnnotations) {
            if (a.name().equals(DotName.createSimple(MTOM.class)) || a.name().equals(DotName.createSimple(Addressing.class))
                    || a.name().equals(DotName.createSimple(RespectBinding.class))) {
                continue;
            }
            ClassInfo annotationType = index.getClassByName(a.name());
            if (annotationType != null && ClassHierarchy.annotation(index, annotationType, WebServiceFeatureAnnotation.class) != null) {
                otherAnnotations.putIfAbsent(a.name().toString(), AnnotationValues.attributes(index, a));
            }
        }

        String serviceRefName = !ok(name) ? defaultServiceRefName : name;
        ServiceReferenceContainer[] containers = null;
        if (annCtx instanceof ServiceReferenceContainerContext) {
            containers = ((ServiceReferenceContainerContext) annCtx).getServiceRefContainers();
        }

        if (containers == null || containers.length == 0) {
            annInfo.getProcessingContext().getErrorHandler().fine(
                    new AnnotationProcessorException(
                    localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.invalidannotationforthisclass",
                    "Illegal annotation symbol for this class will be ignored"),
                    annInfo));
            return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.PROCESSED);
        }

        String wsdlLocation = AnnotationValues.string(index, annotation, "wsdlLocation");
        String mappedName = AnnotationValues.string(index, annotation, "mappedName");
        String lookup = AnnotationValues.string(index, annotation, "lookup");
        String value = AnnotationValues.className(index, annotation, "value");

        // now process the annotation for all the containers.
        for (ServiceReferenceContainer container : containers) {
            ServiceReferenceDescriptor aRef = null;
            try {
                aRef = container.getServiceReferenceByName(serviceRefName);
            } catch(Throwable t) {} // ignore

            if (aRef == null) {
                // time to create it...
                aRef = new ServiceReferenceDescriptor();
                aRef.setName(serviceRefName);
                container.addServiceReferenceDescriptor(aRef);
            }

            // merge other annotations
            for (Map.Entry<String, Map<String, Object>> entry : otherAnnotations.entrySet()) {
                aRef.getOtherAnnotations().putIfAbsent(entry.getKey(), entry.getValue());
            }

            // merge wsdlLocation
            if (!ok(aRef.getWsdlFileUri()) && ok(wsdlLocation))
                aRef.setWsdlFileUri(wsdlLocation);

            if (!aRef.hasMtomEnabled() && mtom != null) {
                aRef.setMtomEnabled(AnnotationValues.bool(index, mtom, "enabled"));
                aRef.setMtomThreshold(AnnotationValues.integer(index, mtom, "threshold"));
            }

            // check Addressing annotation
            if (aRef.getAddressing() == null && addressing != null) {
                aRef.setAddressing(new com.sun.enterprise.deployment.Addressing(
                                        AnnotationValues.bool(index, addressing, "enabled"),
                                        AnnotationValues.bool(index, addressing, "required"),
                                        AnnotationValues.enumName(index, addressing, "responses")));
            }

            // check RespectBinding annotation
            if (aRef.getRespectBinding() == null && respectBinding != null) {
                aRef.setRespectBinding(
                        new com.sun.enterprise.deployment.RespectBinding(
                                AnnotationValues.bool(index, respectBinding, "enabled")));
            }

            // Store mapped name that is specified
            if (!ok(aRef.getMappedName()) && ok(mappedName))
                    aRef.setMappedName(mappedName);

            // Store lookup name that is specified
            if (!aRef.hasLookupName() && ok(lookup))
                aRef.setLookupName(lookup);

            aRef.setInjectResourceType("jakarta.jws.WebServiceRef");

            if (target != null)
                aRef.addInjectionTarget(target);
 
            // Read the WebServiceClient annotation for the service name space
            // uri and wsdl (if required)
            AnnotationInstance wsclientAnn;

            // The JAX-WS 2.1 default value was "Object", the JAX-WS 2.2
            // default value is "Service".  Check whether the value is one
            // of these default values.
            if (!Object.class.getName().equals(value) &&
                    (!jakarta.xml.ws.Service.class.getName().equals(value))) {
                // a value was provided, which should be the Service
                // interface, the requested injection is therefore on the
                // port.
                if (aRef.getServiceInterface() == null) {
                    aRef.setServiceInterface(value);
                }
                
                if (aRef.getPortInfoBySEI(annotatedTypeName) == null) {
                    ServiceRefPortInfo portInfo = new ServiceRefPortInfo();
                    portInfo.setServiceEndpointInterface(annotatedTypeName);
                    aRef.addPortInfo(portInfo);
                }
                // set the port type requested for injection
                if (aRef.getInjectionTargetType() == null) {
                    aRef.setInjectionTargetType(annotatedTypeName);
                }
                wsclientAnn = ClassHierarchy.annotation(index,
                        index.getClassByName(DotName.createSimple(value)), WebServiceClient.class);
            } else {
                // no value provided in the annotation
                wsclientAnn = ClassHierarchy.annotation(index, annotatedType, WebServiceClient.class);
            }
            if (wsclientAnn == null) {
                throw new AnnotationProcessorException(
                        localStrings.getLocalString(
                        "enterprise.deployment.annotation.handlers.classnotannotated",
                        "Class must be annotated with a {1} annotation\n symbol : {1}\n location: {0}",
                        new Object[] { annotatedTypeName, WebServiceClient.class.toString() }));
            }

            // If wsdl file was not specified in a descriptor and not in the
            // annotation, get it from WebServiceClient annotation
            if (aRef.getWsdlFileUri() == null) {
                aRef.setWsdlFileUri(AnnotationValues.string(index, wsclientAnn, "wsdlLocation"));
            }

            // Set service name space URI and service local part
            if (aRef.getServiceName() == null) {
                aRef.setServiceNamespaceUri(AnnotationValues.string(index, wsclientAnn, "targetNamespace"));
                aRef.setServiceLocalPart(AnnotationValues.string(index, wsclientAnn, "name"));
            }

            if (aRef.getServiceInterface() == null) {
                aRef.setServiceInterface(annotatedTypeName);
            }
        }
        // Now force a HandlerChain annotation processing
        // This is to take care of the case where the client class does not
        // have @HandlerChain but the SEI has one specified through JAXWS customization
        if(ClassHierarchy.annotation(index, annElem, jakarta.jws.HandlerChain.class) == null) {
            return (new HandlerChainHandler()).processHandlerChainAnnotation(annInfo, annCtx, annotatedType, declaringClass, false);
        }
        return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.PROCESSED);        
    }
    
    @Override
    public HandlerProcessingResult processAnnotation(AnnotationInfo annInfo)
            throws AnnotationProcessorException {
        return(processAWsRef(annInfo, annInfo.getAnnotation()));
    }
}
