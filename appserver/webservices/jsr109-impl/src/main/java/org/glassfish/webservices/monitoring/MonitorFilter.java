package org.glassfish.webservices.monitoring;

import org.jvnet.hk2.annotations.Contract;

import com.sun.xml.ws.api.message.Packet;

@Contract
public interface MonitorFilter {
    
    void filterRequest(Packet pipeRequest, MonitorContext monitorContext);
    
    void filterResponse(Packet pipeRequest, Packet pipeResponse, MonitorContext monitorContext);

}
