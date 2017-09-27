package org.glassfish.admin.rest.logviewer;

import java.util.AbstractList;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "records")
public class LogRecordList extends AbstractList<LogRecord> {

    @XmlElement(name = "record")
    private final List<LogRecord> records;
    
    public LogRecordList(List<LogRecord> records) {
        this.records = records;
    }

    public LogRecordList() {
        this.records = new LinkedList<>();
    }

    @Override
    public LogRecord get(int i) {
        return records.get(i);
    }

    @Override
    public int size() {
        return records.size();
    }
    
    @Override
    public boolean add(LogRecord e) {
        return records.add(e);
    }

}
