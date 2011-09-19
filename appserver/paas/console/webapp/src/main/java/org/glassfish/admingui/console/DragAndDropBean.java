/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
