/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admingui.console;

import java.util.ArrayList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;

/**
 *
 * @author jdlee
 */
@ManagedBean
@SessionScoped
// This is an ugly, ugly hack. Post-OOW, when we hopefully are using a better
// component set, this can be made "right".
public class TabBean {
    private String currentTab = "Applications";

    public boolean isActiveTab(String tab) {
        return getCurrentTab().equalsIgnoreCase(tab);
    }

    public String getCurrentTab() {
        return currentTab;
    }

    public void setCurrentTab(String currentTab) {
        this.currentTab = currentTab;
    }

    public void pageLoadListener(ComponentSystemEvent event) throws javax.faces.event.AbortProcessingException {
        String activeTab = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("_activeTab");
        if (activeTab != null) {
            setCurrentTab(activeTab);
        }
    }
}
