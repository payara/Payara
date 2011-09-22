/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admingui.console.beans;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.faces.bean.*;
import java.util.*;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import org.glassfish.admingui.console.rest.RestUtil;

@ManagedBean(name="loggingBean")
@ViewScoped
public class LoggingBean {

    private String instanceName;
    private String startIndex;
    private String searchForward;
    private String firstRecord;
    private String lastRecord;
    private List<SelectItem> selectionList;
    private List<Map> records;
    private String selectedIndex;
    private boolean getRecords;

    @ManagedProperty(value="#{environmentBean}")
    private EnvironmentBean environmentBean;

    public static final String TIME_FORMAT = " HH:mm:ss.SSS";
    
    public LoggingBean() {
        
    }

    public void setEnvironmentBean(EnvironmentBean env) {
        this.environmentBean = env;
        List<String> instanceList = environmentBean.getInstanceNames();
        selectionList = new ArrayList<SelectItem>();
        for (String instance : instanceList) {
                selectionList.add(new SelectItem(instance, instance));
        }
        if (selectionList.size() > 0) {
            instanceName = (String)selectionList.get(0).getValue();
            setSelectedIndex(instanceName);
        }
        firstRecord = "0";
        lastRecord = "0";
        searchForward = "false";
        getRecords = true;
    }

    public LoggingBean(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(String instance) {
        selectedIndex = instance;
    }

    public List<SelectItem> getSelectionList() {
        return selectionList;
    }

    public List<Map> getLogMessages() {
        String endPoint = "http://localhost:4848/management/domain/view-log/details.json";
        Map attrs = new HashMap();
        attrs.put("instanceName", instanceName);
        attrs.put("startIndex", startIndex);
        attrs.put("searchForward", searchForward);
        if (instanceName != null && getRecords) {
            Map data = (Map)RestUtil.restRequest(endPoint, attrs, "GET", null, null, false, true).get("data");
            records = (List<Map>) data.get("records");
            records = processLogRecords(records);
            getRecords = false;
        }
        return records;
    }

    private List<Map> processLogRecords(List<Map> records) {
        for (Map<String, Object> record : records) {
            record.put("loggedDateTimeInMS", formatDateForDisplay(Locale.US,
                new Date(new Long(record.get("loggedDateTimeInMS").toString()))));
        }
        if ((records != null) && (records.size() > 0)) {
            firstRecord   = records.get(0).get("recordNumber").toString();
            lastRecord    = records.get(records.size()-1).get("recordNumber").toString();
            if (firstRecord == null)
                firstRecord = "0";
            if (lastRecord == null)
                lastRecord = "0";
            int firstRow = 0;
            try {
                firstRow = Integer.parseInt(firstRecord);
                int lastRow = Integer.parseInt(lastRecord);
                if (firstRow > lastRow) {
                    String temp = firstRecord;
                    firstRecord = lastRecord;
                    lastRecord = temp;
                }
            } catch (NumberFormatException ex) {
                // ignore
            }

	} else {
	    firstRecord = "-1";
            lastRecord  = "-1";
            HashMap noRec = new HashMap();
            noRec.put("loggedDateTimeInMS", "No items Found.");
            records.add(noRec);
	}
        return records;
    }

    public String valueChange(ValueChangeEvent valueChangeEvent) {
        instanceName = (String) valueChangeEvent.getNewValue();
        firstRecord = "0";
        lastRecord = "0";
        searchForward = "false";
        getRecords = true;
        return null;
    }

    public String previous() {
        searchForward = "false";
        startIndex = firstRecord;
        getRecords = true;
        return null;
    }

    public String next() {
        searchForward = "true";
        startIndex = lastRecord;
        getRecords = true;
        return null;
    }

    private String formatDateForDisplay(Locale locale, Date date) {
	DateFormat dateFormat = DateFormat.getDateInstance(
	    DateFormat.MEDIUM, locale);
	if (dateFormat instanceof SimpleDateFormat) {
	    SimpleDateFormat fmt = (SimpleDateFormat)dateFormat;
	    fmt.applyLocalizedPattern(fmt.toLocalizedPattern()+TIME_FORMAT);
	    return fmt.format(date);
	} else {
	    dateFormat = DateFormat.getDateTimeInstance(
		DateFormat.MEDIUM, DateFormat.LONG, locale);
	    return dateFormat.format(date);
	}
    }
}