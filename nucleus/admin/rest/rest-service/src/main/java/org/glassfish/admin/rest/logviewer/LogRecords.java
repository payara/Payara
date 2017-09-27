package org.glassfish.admin.rest.logviewer;

import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "records")
public class LogRecords {

    private final List<LogRecord> records;

    public LogRecords() {
        records = new LinkedList<>();
    }

    @XmlElement(name = "record")
    public List<LogRecord> getRecords() {
        return records;
    }

    public void addRecord(LogRecord record) {
        records.add(record);
    }

}
