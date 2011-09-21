/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admingui.console.beans;

import org.glassfish.admingui.console.event.DragDropEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.List;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import org.apache.myfaces.trinidad.model.ChildPropertyTreeModel;
import org.apache.myfaces.trinidad.model.TreeModel;

@ManagedBean
@SessionScoped
public class ServiceConfigBean {

    public List<String> databases = new ArrayList<String>();
    public List<String> javaEEInstances = new ArrayList<String>();
    public List<String> loadBalancers = new ArrayList<String>();

    public List<String>
            selectedDatabases = new ArrayList<String>(),
            selectedJavaEEInstances = new ArrayList<String>(),
            selectedLoadBalancers = new ArrayList<String>();

    public ServiceConfigBean() {
        databases.add("Oracle - HR");
        databases.add("Oracle - Finance");
        databases.add("mySQL");

        javaEEInstances.add("Template1");
        javaEEInstances.add("Template2");

        loadBalancers.add("lb1");
        loadBalancers.add("lb2");
        loadBalancers.add("lb3");


    }
    public List<String> getDatabases() {
        return databases;
    }

    public void setDatabases(List<String> databases) {
        this.databases = databases;
    }

    public List<String> getSelectedDatabases() {
        return selectedDatabases;
    }

    public List<String> getJavaEEInstances() {
        return javaEEInstances;
    }

    public void setJavaEEInstances(List<String> javaEEInstances) {
        this.javaEEInstances = javaEEInstances;
    }


    public List<String> getSelectedJavaEEInstances() {
        return selectedJavaEEInstances;
    }


    public List<String> getLoadBalancers() {
        return loadBalancers;
    }

    public void setLoadBalancers(List<String> javaEEInstances) {
        this.loadBalancers = loadBalancers;
    }


    public List<String> getSelectedLoadBalancers() {
        return selectedLoadBalancers;
    }

    public List<String> databaseDropListener(DragDropEvent event) {
        String value = (String) event.getData();
        databases.add(value);
        selectedDatabases.remove(value);
        Collections.sort(databases);
        Collections.sort(selectedDatabases);
        return databases;
    }

    public List<String> javaEEDropListener(DragDropEvent event) {
        String value = (String) event.getData();
        javaEEInstances.add(value);
        selectedJavaEEInstances.remove(value);
        Collections.sort(javaEEInstances);
        Collections.sort(selectedJavaEEInstances);
        return javaEEInstances;
    }

    public List<String> lbDropListener(DragDropEvent event) {
        String value = (String) event.getData();
        loadBalancers.add(value);
        selectedLoadBalancers.remove(value);
        Collections.sort(loadBalancers);
        Collections.sort(selectedLoadBalancers);
        return loadBalancers;
    }
}
