package org.glassfish.webservices.monitoring;

import com.oracle.webservices.api.databinding.JavaCallInfo;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.model.SOAPSEIModel;

public interface MonitorContext {
    
    Class<?> getImplementationClass();

    JavaCallInfo getCallInfo();

    SOAPSEIModel getSeiModel();

    WSDLPort getWsdlModel();

    WSEndpoint<?> getOwnerEndpoint();

    WebServiceEndpoint getEndpoint();

}