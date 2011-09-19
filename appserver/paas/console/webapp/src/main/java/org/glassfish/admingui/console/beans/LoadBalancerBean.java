/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admingui.console.beans;

import javax.faces.bean.*;
import javax.faces.context.FacesContextFactory;
import javax.faces.*;
import javax.faces.context.FacesContext;
import java.util.*;

@ManagedBean(name="loadBalancerBean")
@ViewScoped
public class LoadBalancerBean {

    private String httpPort = "80";
    private String sslEnabled = "Disabled";
    private String httpsPort = "OFF";
    
    
    public LoadBalancerBean() {
        FacesContextFactory factory = (FacesContextFactory)
            FactoryFinder.getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
        Map requestMap =
                FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    }

    public String getHttpPort() {
        return httpPort;
    }

    public String getSslEnabled() {
        return sslEnabled;
    }

    public String getHttpsPort() {
        return httpsPort;
    }
}