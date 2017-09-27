package org.glassfish.admin.rest.resources.custom;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Stores the names of the log files in a format which allows automatic parsing
 * to JSON or XML formats. Acts functionally as a list, but couldn't extend
 * AbstractList because that causes incorrect JSON conversion.
 */
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
