package org.glassfish.admin.rest.resources.custom;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "logNames")
public class LogNameList {
    
    private final List<String> logNames;
    
    public LogNameList(List<String> logNames) {
        this.logNames = logNames;
    }
    
    public LogNameList() {
        this.logNames = new ArrayList<>();
    }
    
    @XmlElement(name = "logName")
    public List<String> getLogNames() {
        return logNames;
    }
    
    public boolean add(String e) {
        return logNames.add(e);
    }

}
