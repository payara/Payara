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

package org.glassfish.webservices;


import java.net.URL;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;

import javax.xml.namespace.QName;
import javax.xml.rpc.Service;
import javax.xml.rpc.handler.HandlerInfo;
import javax.xml.rpc.handler.HandlerRegistry;
import javax.xml.rpc.Stub;
import javax.xml.rpc.Call;


import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.deployment.NameValuePairDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;
import org.glassfish.internal.api.Globals;



/**
 * InvocationHandler used to intercept calls to concrete JAXRPC 
 * Service implementation.  
 *
 * NOTE : This class makes no distinction between "partial" WSDL and
 * "full" WSDL.  If a service-ref's packaged WSDL is "partial", the
 * deployer is required to specify a wsdl-override in the runtime info
 * that points to a final WSDL.  In such a case, the behavior for each
 * method listed in the table in section 4.2.2.7 of the spec is the
 * same as Full WSDL.
 *
 * @author Kenneth Saks
 */
public class ServiceInvocationHandler implements InvocationHandler {

    private ServiceReferenceDescriptor serviceRef;

    // real service instance
    private Service serviceDelegate;

    // used in full wsdl case for DII methods. Lazily instantiated.
    private volatile Service configuredServiceDelegate;

    private ClassLoader classLoader;

    private Method getClientManagedPortMethod;

    // location of full wsdl associated with service-ref
    private URL wsdlLocation;

    private boolean fullWsdl = false;
    
    private WsUtil wsUtil = new WsUtil();
    private org.glassfish.webservices.SecurityService  secServ;

    // Service method types
    private static final int CREATE_CALL_NO_ARGS = 1;
    private static final int CREATE_CALL_PORT = 2;
    private static final int CREATE_CALL_OPERATION_QNAME = 3;
    private static final int CREATE_CALL_OPERATION_STRING = 4;
    private static final int GET_CALLS = 5;
    private static final int GET_HANDLER_REGISTRY = 6;
    private static final int GET_PORT_CONTAINER_MANAGED = 7;
    private static final int GET_PORT_CLIENT_MANAGED = 8;
    private static final int GET_PORTS = 9;
    private static final int GET_SERVICE_NAME = 10;
    private static final int GET_TYPE_MAPPING_REGISTRY = 11;
    private static final int GET_WSDL_LOCATION = 12;
    private static final int GENERATED_SERVICE_METHOD = 13;

    private static Map serviceMethodTypes;
    private static Set fullWsdlIllegalMethods;
    private static Set noWsdlIllegalMethods;

    static {
        init();
    }

    public ServiceInvocationHandler(ServiceReferenceDescriptor descriptor,
                                    Service delegate, ClassLoader loader)
            throws Exception {

        serviceRef = descriptor;
        serviceDelegate = delegate;
        classLoader = loader;

        if( serviceRef.hasWsdlFile() ) {
            wsdlLocation = wsUtil.privilegedGetServiceRefWsdl(serviceRef);
            fullWsdl = true;
        } 

        getClientManagedPortMethod = javax.xml.rpc.Service.class.getMethod
                ("getPort", new Class[] { QName.class, Class.class } );

        if (Globals.getDefaultHabitat() != null) {
            secServ = Globals.get(org.glassfish.webservices.SecurityService.class);
        }
        addMessageSecurityHandler(delegate);
    }

    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {

        // NOTE : be careful with "args" parameter.  It is null
        //        if method signature has 0 arguments.

        if( method.getDeclaringClass() == java.lang.Object.class )  {
            return invokeJavaObjectMethod(this, method, args);
        }

        int methodType = getMethodType(method);

        checkUnsupportedMethods(methodType);

        Object returnValue = null;

        try {

            // Initialize method info for invocation based on arguments.
            // Some/All of this might be overridden below.
            Object serviceToInvoke = serviceDelegate;
            Method methodToInvoke  = method;
            int methodTypeToInvoke = methodType;
            Object[] argsForInvoke = args;

            switch(methodType) {

                case GET_PORT_CONTAINER_MANAGED :
                    Class serviceEndpointInterfaceClass = (Class) args[0];
                    String serviceEndpointInterface =
                            serviceEndpointInterfaceClass.getName();
                    ServiceRefPortInfo portInfo =
                            serviceRef.getPortInfo(serviceEndpointInterface);

                    // If we have a port, use it to call getPort(QName, SEI) instead
                    if( (portInfo != null) && portInfo.hasWsdlPort() ) {
                        methodToInvoke = getClientManagedPortMethod;
                        methodTypeToInvoke = GET_PORT_CLIENT_MANAGED;
                        argsForInvoke  = new Object[] { portInfo.getWsdlPort(),
                                args[0] };
                    } else {
                        // This means the deployer did not resolve the port to
                        // which this SEI is mapped.  Just call getPort(SEI)
                        // method on delegate. This is not guaranteed to work.
                    }
                    break;

                case GET_WSDL_LOCATION :
                    return wsdlLocation;

                case CREATE_CALL_PORT :
                case CREATE_CALL_OPERATION_QNAME :
                case CREATE_CALL_OPERATION_STRING :
                case GET_CALLS :
                case GET_PORTS :

                    serviceToInvoke = getConfiguredServiceDelegate();
                    break;
                default:
                    break;
            } // End switch (methodType)

            returnValue = methodToInvoke.invoke(serviceToInvoke, argsForInvoke);

            if( returnValue instanceof Stub ) {
                Stub stub = (Stub) returnValue;
                setStubProperties(stub, methodTypeToInvoke, methodToInvoke,
                        argsForInvoke);
            } else if( returnValue instanceof Call ) {
                Call[] calls = new Call[1];
                calls[0] = (Call) returnValue;
                setCallProperties(calls, methodTypeToInvoke, argsForInvoke);
            } else if( methodType == GET_CALLS ) {
                Call[] calls = (Call[]) returnValue;
                setCallProperties(calls, methodTypeToInvoke, argsForInvoke);
            }

        } catch(InvocationTargetException ite) {
            throw ite.getCause();
        }

        return returnValue;
    }

    public HandlerInfo getMessageSecurityHandlerInfo(QName port) throws Exception
    {
        HandlerInfo rvalue = null;

        MessageSecurityBindingDescriptor binding = null;
        ServiceRefPortInfo portInfo = serviceRef.getPortInfoByPort(port);
        if (portInfo != null) {
            binding = portInfo.getMessageSecurityBinding();
        }
        if (secServ != null) {
           rvalue = secServ.getMessageSecurityHandler(binding, serviceRef.getServiceName());
        }
        return rvalue;
    }

    private boolean addMessageSecurityHandler(Service service) throws Exception
    {
        HandlerRegistry registry = service.getHandlerRegistry();
        Iterator ports = null;
        try {
            ports = service.getPorts();
        } catch (Exception e) {
            // FIXME: should make sure that the exception was thrown because
            // the service is not fully defined; but for now just return.
            ports = null;
        }

        while(ports != null && ports.hasNext()) {

            QName nextPort = (QName) ports.next();

            List handlerChain = registry.getHandlerChain(nextPort);

            // append security handler to the end of every handler chain
            // ASSUMPTION 1: that registry.getHandlerChain() never returns null.
            // ASSUMPTION 2: that handlers from ServiceRef have already been added

            HandlerInfo handlerInfo = getMessageSecurityHandlerInfo(nextPort);

            if (handlerInfo != null) {
                handlerChain.add(handlerInfo);
            }
        }

        return ports == null ? false : true;
    }

    private Service getConfiguredServiceDelegate() throws Exception {
        if (configuredServiceDelegate == null) {
            // We need a ConfiguredService to handle these
            // invocations, since the JAXRPC RI Generated Service impl
            // does not.  Configured service is potentially
            // a heavy-weight object so we lazily instantiate it to
            // take advantage of the likelihood that
            // GeneratedService service-refs won't be used for DII.
            Service configuredService =
                    wsUtil.createConfiguredService(serviceRef);
            wsUtil.configureHandlerChain(serviceRef, configuredService,
                    configuredService.getPorts(), classLoader);
            configuredServiceDelegate = configuredService;

            addMessageSecurityHandler(configuredService);
        }
        return configuredServiceDelegate;
    }

    private int getMethodType(Method method) {
        Integer methodType = (Integer) serviceMethodTypes.get(method);
        return (methodType != null) ?
                methodType.intValue() : GENERATED_SERVICE_METHOD;
    }

    /**
     * Convert invocation method to a constant for easier processing.
     */
    private static void init() {

        serviceMethodTypes     = new HashMap();
        fullWsdlIllegalMethods = new HashSet();
        noWsdlIllegalMethods   = new HashSet();

        try {

            Class noParams[]   = new Class[0];
            String createCall  = "createCall";
            Class serviceClass = javax.xml.rpc.Service.class;

            //
            // Map Service method to method type.
            //

            Method createCallNoArgs =
                    serviceClass.getDeclaredMethod(createCall, noParams);
            serviceMethodTypes.put(createCallNoArgs,
                    Integer.valueOf(CREATE_CALL_NO_ARGS));

            Method createCallPort =
                    serviceClass.getDeclaredMethod(createCall,
                            new Class[] { QName.class });
            serviceMethodTypes.put(createCallPort,
                     Integer.valueOf(CREATE_CALL_PORT));

            Method createCallOperationQName =
                    serviceClass.getDeclaredMethod
                            (createCall, new Class[] { QName.class, QName.class });
            serviceMethodTypes.put(createCallOperationQName,
                     Integer.valueOf(CREATE_CALL_OPERATION_QNAME));

            Method createCallOperationString =
                    serviceClass.getDeclaredMethod
                            (createCall, new Class[] { QName.class, String.class });
            serviceMethodTypes.put(createCallOperationString,
                    Integer.valueOf(CREATE_CALL_OPERATION_STRING));

            Method getCalls = serviceClass.getDeclaredMethod
                    ("getCalls", new Class[] { QName.class });
            serviceMethodTypes.put(getCalls,  Integer.valueOf(GET_CALLS));

            Method getHandlerRegistry = serviceClass.getDeclaredMethod
                    ("getHandlerRegistry", noParams);
            serviceMethodTypes.put(getHandlerRegistry,
                     Integer.valueOf(GET_HANDLER_REGISTRY));

            Method getPortContainerManaged = serviceClass.getDeclaredMethod
                    ("getPort", new Class[] { Class.class });
            serviceMethodTypes.put(getPortContainerManaged,
                    Integer.valueOf(GET_PORT_CONTAINER_MANAGED));

            Method getPortClientManaged = serviceClass.getDeclaredMethod
                    ("getPort", new Class[] { QName.class, Class.class });
            serviceMethodTypes.put(getPortClientManaged,
                    Integer.valueOf(GET_PORT_CLIENT_MANAGED));

            Method getPorts = serviceClass.getDeclaredMethod
                    ("getPorts", noParams);
            serviceMethodTypes.put(getPorts, Integer.valueOf(GET_PORTS));

            Method getServiceName = serviceClass.getDeclaredMethod
                    ("getServiceName", noParams);
            serviceMethodTypes.put(getServiceName,
                     Integer.valueOf(GET_SERVICE_NAME));

            Method getTypeMappingRegistry = serviceClass.getDeclaredMethod
                    ("getTypeMappingRegistry", noParams);
            serviceMethodTypes.put(getTypeMappingRegistry,
                    Integer.valueOf(GET_TYPE_MAPPING_REGISTRY));

            Method getWsdlLocation = serviceClass.getDeclaredMethod
                    ("getWSDLDocumentLocation", noParams);
            serviceMethodTypes.put(getWsdlLocation,
                     Integer.valueOf(GET_WSDL_LOCATION));
        } catch(NoSuchMethodException nsme) {}

        // Implementation of table 4.2.2.7.  All "No WSDL" column cells
        // with value Unspecified throw UnsupportedOperationException

        fullWsdlIllegalMethods.add( GET_HANDLER_REGISTRY);
        fullWsdlIllegalMethods.add(GET_TYPE_MAPPING_REGISTRY);

        noWsdlIllegalMethods.add( CREATE_CALL_PORT);
        noWsdlIllegalMethods.add( CREATE_CALL_OPERATION_QNAME);
        noWsdlIllegalMethods.add(CREATE_CALL_OPERATION_STRING);
        noWsdlIllegalMethods.add(GET_CALLS);
        noWsdlIllegalMethods.add(GET_HANDLER_REGISTRY);
        noWsdlIllegalMethods.add(GET_PORT_CONTAINER_MANAGED);
        noWsdlIllegalMethods.add(GET_PORT_CLIENT_MANAGED);
        noWsdlIllegalMethods.add(GET_PORTS);
        noWsdlIllegalMethods.add(GET_SERVICE_NAME);
        noWsdlIllegalMethods.add(GET_TYPE_MAPPING_REGISTRY);
        noWsdlIllegalMethods.add(GET_WSDL_LOCATION);

        // This case shouldn't happen since if service-ref has generated
        // service and no WSDL it won't get past deployment, but it's here
        // for completeness.
        noWsdlIllegalMethods.add(Integer.valueOf(GENERATED_SERVICE_METHOD));
    }

    private void checkUnsupportedMethods(int methodType)
            throws UnsupportedOperationException {

        Set illegalMethods = fullWsdl ?
                fullWsdlIllegalMethods : noWsdlIllegalMethods;

        if( illegalMethods.contains( Integer.valueOf(methodType)) ) {
            throw new UnsupportedOperationException();
        }

        return;
    }

    private void setStubProperties(Stub stub, int methodType, Method method,
                                   Object[] args) {

        // Port info lookup will be based on SEI or port.
        QName port = null;
        String serviceEndpointInterface = null;

        switch(methodType) {
            case GET_PORT_CONTAINER_MANAGED :

                serviceEndpointInterface = ((Class) args[0]).getName();
                break;

            case GET_PORT_CLIENT_MANAGED :

                port = (QName) args[0];
                serviceEndpointInterface = ((Class) args[1]).getName();
                break;

            case GENERATED_SERVICE_METHOD :

                // java.rmi.Remote get<Name_of_wsdl:port>()
                String portLocalPart = method.getName().startsWith("get") ?
                        method.getName().substring(3) : null;
                if( portLocalPart != null ) {
                    QName serviceName = serviceRef.getServiceName();
                    port = new QName(serviceName.getNamespaceURI(), portLocalPart);
                }
                serviceEndpointInterface = method.getReturnType().getName();

                break;

            default :
                return;
        }

        ServiceRefPortInfo portInfo = null;

        // If port is known, it takes precedence in lookup.
        if( port != null ) {
            portInfo = serviceRef.getPortInfoByPort(port);
        }
        if( portInfo == null ) {
            portInfo = serviceRef.getPortInfoBySEI(serviceEndpointInterface);
        }

        if( portInfo != null ) {
            Set properties = portInfo.getStubProperties();

            for(Iterator iter = properties.iterator(); iter.hasNext();) {
                NameValuePairDescriptor next = (NameValuePairDescriptor)
                        iter.next();

                stub._setProperty(next.getName(), next.getValue());

            }

            // If this port has a resolved target endpoint address due to a
            // port-component-link, set it on stub.  However, if the runtime
            // info has an entry for target endpoint address, that takes 
            // precedence.
            if( portInfo.hasTargetEndpointAddress() ) {
                if(!portInfo.hasStubProperty(Stub.ENDPOINT_ADDRESS_PROPERTY)) {
                    stub._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY,
                            portInfo.getTargetEndpointAddress());
                }
            }
        }
    }

    private void setCallProperties(Call[] calls, int methodType, Object[] args){

        Set callProperties = getPropertiesForCall(methodType, args);

        if( callProperties != null ) {
            for(int callIndex = 0; callIndex < calls.length; callIndex++) {
                setCallProperties(calls[callIndex], callProperties);
            }
        }
    }

    private Set getPropertiesForCall(int methodType, Object args[]) {

        Set callProperties = null;
        switch(methodType) {

            case CREATE_CALL_PORT :
            case CREATE_CALL_OPERATION_QNAME :
            case CREATE_CALL_OPERATION_STRING :
            case GET_CALLS :

                // Each of these methods has port as first argument.
                QName port = (QName) args[0];

                // Check if call properties are set at the port level.
                ServiceRefPortInfo portInfo =
                        serviceRef.getPortInfoByPort(port);
                if( portInfo != null ) {
                    callProperties = portInfo.getCallProperties();
                }

                break;

            case CREATE_CALL_NO_ARGS :

                callProperties = serviceRef.getCallProperties();
                break;

            default:
                break;
        }

        return callProperties;
    }

    private void setCallProperties(Call call, Set callProperties) {
        for(Iterator iter = callProperties.iterator(); iter.hasNext();) {
            NameValuePairDescriptor next = (NameValuePairDescriptor)
                    iter.next();
            call.setProperty(next.getName(), next.getValue());
        }
    }

    private Object invokeJavaObjectMethod(InvocationHandler handler,
                                          Method method, Object[] args)
            throws Throwable {

        Object returnValue = null;

        // Can only be one of : 
        //     boolean java.lang.Object.equals(Object)
        //     int     java.lang.Object.hashCode()
        //     String  java.lang.Object.toString()
        //
        // Optimize by comparing as few characters as possible.

        switch( method.getName().charAt(0) ) {
            case 'e' :
                Object other = Proxy.isProxyClass(args[0].getClass()) ?
                        Proxy.getInvocationHandler(args[0]) : args[0];
                returnValue = Boolean.valueOf(handler.equals(other));
                break;
            case 'h' :
                returnValue = Integer.valueOf(handler.hashCode());
                break;
            case 't' :
                returnValue = handler.toString();
                break;
            default :
                throw new Throwable("Object method " + method.getName() +
                        "not found");
        }

        return returnValue;
    }

    /* TODO remove me if none of the cts use this
    private void setJBIProperties(Object stubOrCall, ServiceRefPortInfo portInfo) {
        // Check if the target service is a JBI service, and get its QName
        QName svcQName = serviceRef.getServiceName();
        if ( svcQName == null )
            return;

        if ( stubOrCall instanceof Stub ) {
            com.sun.xml.rpc.spi.runtime.StubBase stub =
                    (com.sun.xml.rpc.spi.runtime.StubBase)stubOrCall;

            try {
                // This statement is getting executed only
                //because jbi-enabled property on the stub is set to true
                //TODO BM fix this later
                 ServiceEngineUtil.setJBITransportFactory(portInfo, stub, true);

            } catch(Throwable e) {
                // Do nothing
                //logger.severe("Exception raised while setting transport " +
                //      "factory to NMR : " + e.getMessage());
            }
            return;
        }
    }*/
}
