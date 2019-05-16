package org.glassfish.webservices.monitoring;

import com.oracle.webservices.api.databinding.JavaCallInfo;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.model.SOAPSEIModel;

public class MonitorContextImpl implements MonitorContext {
   
    private final JavaCallInfo callInfo;
    private final SOAPSEIModel seiModel;
    private final WSDLPort wsdlModel;
    private final WSEndpoint<?> ownerEndpoint;
    private final WebServiceEndpoint endpoint;
    
    public MonitorContextImpl(JavaCallInfo callInfo, SOAPSEIModel seiModel, WSDLPort wsdlModel, WSEndpoint<?> ownerEndpoint, WebServiceEndpoint endpoint) {
        this.callInfo = callInfo;
        this.seiModel = seiModel;
        this.wsdlModel = wsdlModel;
        this.ownerEndpoint = ownerEndpoint;
        this.endpoint = endpoint;
    }
    
    @Override
    public Class<?> getImplementationClass() {
        String className;
        
        if (endpoint.getEjbComponentImpl() != null) {
           className = endpoint.getEjbComponentImpl().getEjbClassName();
        } else {
            className = endpoint.getWebComponentImpl().getWebComponentImplementation();
        }
        
        try {
            return Thread.currentThread()
                         .getContextClassLoader()
                         .loadClass(className);
        } catch (Exception e) {
            
        }
        
        return null;
    }

    @Override
    public JavaCallInfo getCallInfo() {
        return callInfo;
    }

    @Override
    public SOAPSEIModel getSeiModel() {
        return seiModel;
    }

    @Override
    public WSDLPort getWsdlModel() {
        return wsdlModel;
    }

    @Override
    public WSEndpoint<?> getOwnerEndpoint() {
        return ownerEndpoint;
    }

    @Override
    public WebServiceEndpoint getEndpoint() {
        return endpoint;
    }

}
