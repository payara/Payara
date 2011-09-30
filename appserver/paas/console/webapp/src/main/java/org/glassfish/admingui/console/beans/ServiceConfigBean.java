/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

    public void setLoadBalancers(List<String> loadBalancers) {
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
