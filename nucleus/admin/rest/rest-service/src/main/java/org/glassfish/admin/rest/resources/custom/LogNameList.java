package org.glassfish.admin.rest.resources.custom;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "logNames")
public class LogNameList extends AbstractList<String> {
    
    @XmlElement(name = "logName")
    private final List<String> logNames;
    
    public LogNameList(List<String> logNames) {
        this.logNames = logNames;
    }
    
    public LogNameList() {
        this.logNames = new ArrayList<>();
    }

    @Override
    public String get(int i) {
        return logNames.get(i);
    }

    @Override
    public int size() {
        return logNames.size();
    }
    
    @Override
    public boolean add(String e) {
        return logNames.add(e);
    }

}
