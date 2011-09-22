/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admingui.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;

/**
 *
 * @author jdlee
 */

@ManagedBean
@ViewScoped
public class WizardBean {
    private int step = 1;
    private List<String> stepLabels = Collections.unmodifiableList(new ArrayList<String>(){{ 
        add("1. Upload Application");
        add("2. Configure Services");
        add("3. Review and Confirm");
    }});
    private List<String> stepPages = Collections.unmodifiableList(new ArrayList<String>(){{ 
        add("/demo/wizard/upload.xhtml");
        add("/demo/wizard/configServices.xhtml");
        add("/demo/wizard/deploy.xhtml");
    }});

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public List<String> getStepLabels() {
        return stepLabels;
    }

    public List<String> getStepPages() {
        return stepPages;
    }
    
    public String previous() {
        step--;
        return null;
    }
    
    public String next() {
        step++;
        return null;
    }
}