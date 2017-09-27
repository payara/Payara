package org.glassfish.admin.rest.logviewer;

import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "records")
public class LogRecordList {

    private final List<LogRecord> records;
    
    public LogRecordList(List<LogRecord> records) {
        this.records = records;
    }

    public LogRecordList() {
        this.records = new LinkedList<>();
    }
    
    @XmlElement(name = "record")
    public List<LogRecord> getRecords() {
        return records;
    }
    
    public boolean add(LogRecord e) {
        return records.add(e);
    }

}
