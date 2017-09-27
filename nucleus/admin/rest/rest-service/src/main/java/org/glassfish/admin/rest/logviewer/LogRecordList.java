package org.glassfish.admin.rest.logviewer;

import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Stores the details of the each log object in a format which allows automatic
 * parsing to JSON or XML formats. Acts functionally as a list, but couldn't
 * extend AbstractList because that causes incorrect JSON conversion.
 */
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
