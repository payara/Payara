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
package org.glassfish.admingui.console;

import org.glassfish.admingui.console.event.DragDropEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author jdlee
 */
@ManagedBean
@SessionScoped
public class DragAndDropBean {

    private List<String> available = new ArrayList<String>() {{
        add("server1");
        add("server2");
        add("server3");
        add("server4");
        add("server5");
    }};
    private List<String> selected = new ArrayList<String>();

    public DragAndDropBean() {
    }

    public List<String> getAvailable() {
        return available;
    }

    public void setAvailable(List<String> available) {
        this.available = available;
    }

    public List<String> getSelected() {
        return selected;
    }

    public void setSelected(List<String> selected) {
        this.selected = selected;
    }

    
    /*
    public String availableDropListener() {
        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletRequest myRequest = (HttpServletRequest) fc.getExternalContext().getRequest();
        String value = myRequest.getParameter("droppedValue");
        if (!available.contains(value)) {
            available.add(value);
            selected.remove(value);
            Collections.sort(available);
            Collections.sort(selected);
        }

        return null;
    }

    public String selectedDropListener() {
        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletRequest myRequest = (HttpServletRequest) fc.getExternalContext().getRequest();
        String value = myRequest.getParameter("droppedValue");
        available.remove(value);
        selected.add(value);
        Collections.sort(available);
        Collections.sort(selected);

        return null;
    }
    */

    public String availableDropListener(DragDropEvent event) {
        String value = (String) event.getData();
        available.add(value);
        selected.remove(value);
        Collections.sort(available);
        Collections.sort(selected);

        return null;
    }
    
    public String selectedDropListener(DragDropEvent event) {
        String value = (String) event.getData();
        available.remove(value);
        selected.add(value);
        Collections.sort(available);
        Collections.sort(selected);

        return null;
    }
}
