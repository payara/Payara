/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.enterprise.tools.verifier.gui;


import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Vector;
import java.util.logging.LogRecord;

import com.sun.enterprise.tools.verifier.CheckMgr;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.StringManagerHelper;
import com.sun.enterprise.tools.verifier.VerifierEventsListener;
import com.sun.enterprise.tools.verifier.util.VerifierConstants;

public class ResultsPanel extends JPanel implements VerifierEventsListener {

    JTable table;
    DefaultTableModel tableModel;
    JScrollPane tableScrollPane;
    JScrollPane textScrollPane;
    JTextArea detailText;
    Vector<String> details = new Vector<String>();
    private Vector<Result> passResults = new Vector<Result>();
    private Vector<Result> failResults = new Vector<Result>();
    private Vector<LogRecord> errorResults = new Vector<LogRecord>();
    private Vector<Result> warnResults = new Vector<Result>();
    private Vector<Result> naResults = new Vector<Result>();
    private Vector<Result> notImplementedResults = new Vector<Result>();
    private Vector<Result> notRunResults = new Vector<Result>();
    private Vector<Result> defaultResults = new Vector<Result>();
    private static final com.sun.enterprise.util.LocalStringManagerImpl smh =
            StringManagerHelper.getLocalStringsManager();

    //final String[] columnNames = {"Item", "Test Name", "Result"};
    final String[] columnNames = {
        (smh.getLocalString(
                "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
            ".columnName1", // NOI18N
                "Item")), // NOI18N
        (smh.getLocalString(
                "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
            ".columnName2", // NOI18N
                "Test Name")), // NOI18N
        (smh.getLocalString(
                "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
            ".columnName3", // NOI18N
                "Result"))}; // NOI18N
    final String errStr = smh.getLocalString(getClass().getName()+".errStr", // NOI18N
                                            "Error during verification"); // NOI18N
    final String errStr1 = smh.getLocalString(getClass().getName()+".errStr1", // NOI18N
                                            "ERROR"); // NOI18N
    

    public ResultsPanel() {

        setLayout(new BorderLayout());
        setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(Color.black),
                        (smh.getLocalString(
                                "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".ResultsPanelLabel", // NOI18N
                                "Results: (Click on Item to show test Details below)")))); // NOI18N

        // 508 compliance
        this.getAccessibleContext().setAccessibleName(
                smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".panelName", // NOI18N
                        "Panel")); // NOI18N
        this.getAccessibleContext().setAccessibleDescription(
                smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".PanelDesc", // NOI18N
                        "This is a panel")); // NOI18N

        CheckMgr.addVerifierEventsListener(this);

        // set up result table
        tableModel = new DefaultTableModel(columnNames, 0);
        table = new JTable(tableModel);
        // 508 for JTable
        table.getAccessibleContext().setAccessibleName(
                smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".tableName", // NOI18N
                        "Table")); // NOI18N
        table.getAccessibleContext().setAccessibleDescription(
                smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".tableDesc", // NOI18N
                        "This is a table of items")); // NOI18N
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableScrollPane = new JScrollPane(table);
        // 508 for JScrollPane
        tableScrollPane.getAccessibleContext().setAccessibleName(
                smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".scrName1", // NOI18N
                        "Scroll Pane")); // NOI18N
        tableScrollPane.getAccessibleContext().setAccessibleDescription(
                smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".scrDesc1", // NOI18N
                        "This is a scroll pane that helps to scroll the list")); // NOI18N
        sizeTableColumns();
        // make the cells uneditable
        JTextField field = new JTextField();
        // 508 for JTextField
        field.getAccessibleContext().setAccessibleName(
                smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".fielsName", // NOI18N
                        "Text Field")); // NOI18N
        field.getAccessibleContext().setAccessibleDescription(
                smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".fieldDesc", // NOI18N
                        "This is a text field")); // NOI18N
        table.setDefaultEditor(Object.class, new DefaultCellEditor(field) {
            public boolean isCellEditable(EventObject anEvent) {
                return false;
            }
        });
        // add action listener to table to show details
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    if (table.getSelectionModel().isSelectedIndex(
                            e.getLastIndex())) {
                        setDetailText(
                                (String) details.elementAt(e.getLastIndex()));
                    } else if (table.getSelectionModel().isSelectedIndex(
                            e.getFirstIndex())) {
                        setDetailText(
                                (String) details.elementAt(e.getFirstIndex()));
                    }
                }
            }
        });

        // create detail text area
        detailText = new JTextArea(4, 50);
        // 508 for JTextArea
        detailText.getAccessibleContext().setAccessibleName(
                smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".areaName", // NOI18N
                        "Text Area")); // NOI18N
        detailText.getAccessibleContext().setAccessibleDescription(
                smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".areaDesc", // NOI18N
                        "This is a text area")); // NOI18N
        detailText.setEditable(false);
        textScrollPane = new JScrollPane(detailText);
        // 508 for JScrollPane
        textScrollPane.getAccessibleContext().setAccessibleName(
                smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".scrName2", // NOI18N
                        "Scroll Pane")); // NOI18N
        textScrollPane.getAccessibleContext().setAccessibleDescription(
                smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".scrDesc2", // NOI18N
                        "This is a scroll pane that helps to scroll the list")); // NOI18N
        textScrollPane.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(),
                        (smh.getLocalString(
                                "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".DetailsPanelLabel", // NOI18N
                                "Details:")))); // NOI18N

        //add the components to the panel
        add("Center", tableScrollPane); // NOI18N
        add("South", textScrollPane); // NOI18N
	
        // Register a listener for the report level radio buttons.
        // to allow post-processing filtering
        RadioListener myListener = new RadioListener();
        ControlPanel.addRadioButtonListener((ActionListener) myListener);
    }

    class RadioListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == ControlPanel.allButton) {
                if ((getPassResultsForDisplay().size() > 0) ||
                        (getFailResultsForDisplay().size() > 0) ||
                        (getErrorResultsForDisplay().size() > 0) ||
                        (getWarnResultsForDisplay().size() > 0) ||
                        (getNaResultsForDisplay().size() > 0) ||
                        (getNotImplementedResultsForDisplay().size() > 0) ||
                        (getNotRunResultsForDisplay().size() > 0) ||
                        (getDefaultResultsForDisplay().size() > 0)) {
                    upDateDisplay(VerifierConstants.ALL);
                } else {
                    clearResults();
                }
            }
            if (e.getSource() == ControlPanel.failButton) {
                if (getFailResultsForDisplay().size() > 0 ||
                        getErrorResultsForDisplay().size() > 0) {
                    upDateDisplay(VerifierConstants.FAIL);
                } else {
                    clearResults();
                }
            }
            if (e.getSource() == ControlPanel.warnButton) {
                if ((getFailResultsForDisplay().size() > 0) ||
                        (getErrorResultsForDisplay().size() > 0) ||
                        (getWarnResultsForDisplay().size() > 0)) {
                    upDateDisplay(VerifierConstants.WARN);
                } else {
                    clearResults();
                }
            }
        }
    }

    public void setDetailText(String details) {
        detailText.setText(details);
        JScrollBar scrollBar = textScrollPane.getVerticalScrollBar();
        if (scrollBar != null) {
            scrollBar.setValue(0);
        }
    }

    public void addDetailText(String details) {
        detailText.append(details);
    }

    public void clearResults() {
        //clear the table
        tableModel = new DefaultTableModel(columnNames, 0);
        table.setModel(tableModel);
        sizeTableColumns();
        //clear the detail text
        setDetailText("");
        //clear the details Vector
        details = new Vector<String>();
    }

    void sizeTableColumns() {
        table.getColumn(
                (smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".columnName1", // NOI18N
                        "Item"))) // NOI18N
                .setMinWidth(150);
        table.getColumn(
                (smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".columnName1", // NOI18N
                        "Item"))) // NOI18N
                .setMaxWidth(200);
        table.getColumn(
                (smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".columnName1", // NOI18N
                        "Item"))) // NOI18N
                .setPreferredWidth(180);
        table.getColumn(
                (smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".columnName2", // NOI18N
                        "Test Name"))) // NOI18N
                .setMinWidth(150);
        table.getColumn(
                (smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".columnName2", // NOI18N
                        "Test Name"))) // NOI18N
                .setPreferredWidth(180);
        table.getColumn(
                (smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".columnName3", // NOI18N
                        "Result"))) // NOI18N
                .setMinWidth(120);
        table.getColumn(
                (smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".columnName3", // NOI18N
                        "Result"))) // NOI18N
                .setMaxWidth(200);
        table.getColumn(
                (smh.getLocalString(
                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                ".columnName3", // NOI18N
                        "Result"))) // NOI18N
                .setPreferredWidth(160);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.sizeColumnsToFit(0);
    }

    private void upDateDisplayAll() {
        upDateDisplayFail();
        upDateDisplayWarn();
        upDateDisplayPass();
        upDateDisplayNa();
        upDateDisplayNotImplemented();
        upDateDisplayNotRun();
        upDateDisplayDefault();
        upDateDisplayError();
    }


    private void updateTableRows(Vector results) {
        String status;
        // update display approriately
        for (int i = 0; i < results.size(); i++) {
            Result r = ((Result) results.elementAt(i));
            StringBuffer s = new StringBuffer(
                    "Assertion:" + r.getAssertion() + "\n"); // NOI18N
            switch (r.getStatus()) {
                case Result.PASSED:
                    {
                        status =
                                (smh.getLocalString(
                                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                                ".Status_Passed", // NOI18N
                                        "Passed")); // NOI18N
                        Enumeration e = r.getGoodDetails().elements();
                        while (e.hasMoreElements()) {
                            s.append((String) e.nextElement());
                            s.append("\n"); // NOI18N
                        }
                        break;
                    }
                case Result.FAILED:
                    {
                        status =
                                (smh.getLocalString(
                                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                                ".Status_Failed", // NOI18N
                                        "FAILED")); // NOI18N
                        Enumeration e = r.getErrorDetails().elements();
                        while (e.hasMoreElements()) {
                            s.append((String) e.nextElement());
                            s.append("\n"); // NOI18N
                        }
                        break;
                    }
                case Result.WARNING:
                    {
                        status =
                                (smh.getLocalString(
                                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                                ".Status_Warning", // NOI18N
                                        "WARNING")); // NOI18N
                        Enumeration e = r.getWarningDetails().elements();
                        while (e.hasMoreElements()) {
                            s.append((String) e.nextElement());
                            s.append("\n"); // NOI18N
                        }
                        break;
                    }
                case Result.NOT_APPLICABLE:
                    {
                        status =
                                (smh.getLocalString(
                                        "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                                ".Status_NotApplicable", // NOI18N
                                        "Not Applicable")); // NOI18N
                        Enumeration e = r.getNaDetails().elements();
                        while (e.hasMoreElements()) {
                            s.append((String) e.nextElement());
                            s.append("\n"); // NOI18N
                        }
                        break;
                    }
                case Result.NOT_IMPLEMENTED:
                    status =
                            (smh.getLocalString(
                                    "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                            ".Status_NotImplemented", // NOI18N
                                    "Not Implemented")); // NOI18N
                    break;
                case Result.NOT_RUN:
                    status =
                            (smh.getLocalString(
                                    "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                            ".Status_NotRun", // NOI18N
                                    "Not Run")); // NOI18N
                    break;
                default:
                    status =
                            (smh.getLocalString(
                                    "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                            ".Status_Unknown", // NOI18N
                                    "Unknown")); // NOI18N
                    break;
            }
            details.add(s.toString());
            Object[] row = {r.getComponentName(), r.getTestName(), status};
            tableModel.addRow(row);
        }//for
        table.sizeColumnsToFit(0);
    }

    private void upDateDisplayPass() {
        updateTableRows(getPassResultsForDisplay());
    }

    private void upDateDisplayFail() {
        updateTableRows(getFailResultsForDisplay());
    }

    private void upDateDisplayError() {
        Vector errors = getErrorResultsForDisplay();
        for (int i = 0; i < errors.size(); i++) {
            LogRecord r = (LogRecord) errors.elementAt(i);
            details.add(r.getMessage() + "\n" + r.getThrown().getMessage()); // NOI18N
            Object[] row = {r.getLoggerName(), errStr, errStr1};
            tableModel.addRow(row);
        }
        table.sizeColumnsToFit(0);
    }

    private void upDateDisplayWarn() {
        updateTableRows(getWarnResultsForDisplay());
    }

    private void upDateDisplayNa() {
        updateTableRows(getNaResultsForDisplay());
    }

    private void upDateDisplayNotImplemented() {
        updateTableRows(getNotImplementedResultsForDisplay());
    }

    private void upDateDisplayNotRun() {
        updateTableRows(getNotRunResultsForDisplay());
    }

    private void upDateDisplayDefault() {
        updateTableRows(getDefaultResultsForDisplay());
    }

    void addError(LogRecord r) {
        saveErrorResultsForDisplay(r);
        details.add(r.getMessage() + "\n" + r.getThrown().getMessage()); // NOI18N
        // create a table row for this result
        Object[] row = {r.getLoggerName(), errStr, errStr1};
        tableModel.addRow(row);
        table.sizeColumnsToFit(0);
    }

    public void upDateDisplay(int status) {
        // update display approriately
        clearResults();
        if (status == VerifierConstants.ALL) {
            upDateDisplayAll();
        }
        if (status == VerifierConstants.FAIL) {
            upDateDisplayError();
            upDateDisplayFail();
        }
        if (status == VerifierConstants.WARN) {
            upDateDisplayError();
            upDateDisplayFail();
            upDateDisplayWarn();
        }
    }

    private void savePassResultsForDisplay(Result r) {
        passResults.addElement(r);
    }

    private void saveWarnResultsForDisplay(Result r) {
        warnResults.addElement(r);
    }

    private void saveFailResultsForDisplay(Result r) {
        failResults.addElement(r);
    }

    private void saveErrorResultsForDisplay(LogRecord r) {
        errorResults.addElement(r);
    }

    private void saveNaResultsForDisplay(Result r) {
        naResults.addElement(r);
    }

    private void saveNotRunResultsForDisplay(Result r) {
        notRunResults.addElement(r);
    }

    private void saveNotImplementedResultsForDisplay(Result r) {
        notImplementedResults.addElement(r);
    }

    private void saveDefaultResultsForDisplay(Result r) {
        defaultResults.addElement(r);
    }

    private Vector getPassResultsForDisplay() {
        return passResults;
    }

    private Vector getWarnResultsForDisplay() {
        return warnResults;
    }

    private Vector getFailResultsForDisplay() {
        return failResults;
    }

    private Vector getErrorResultsForDisplay() {
        return errorResults;
    }

    private Vector getNaResultsForDisplay() {
        return naResults;
    }

    private Vector getNotImplementedResultsForDisplay() {
        return notImplementedResults;
    }

    private Vector getNotRunResultsForDisplay() {
        return notRunResults;
    }

    private Vector getDefaultResultsForDisplay() {
        return defaultResults;
    }

    public void clearOldResults() {
        passResults = new Vector<Result>();
        failResults = new Vector<Result>();
        errorResults = new Vector<LogRecord>();
        warnResults = new Vector<Result>();
        naResults = new Vector<Result>();
        notImplementedResults = new Vector<Result>();
        notRunResults = new Vector<Result>();
        defaultResults = new Vector<Result>();
    }


    // We are a ChangeListener of the test harness CheckMgrs
    public void testFinished(EventObject evt) {
        Result r = (Result) evt.getSource();
        StringBuffer s = new StringBuffer("Assertion:" + r.getAssertion() + "\n"); // NOI18N
        String status;
        switch (r.getStatus()) {
            case Result.PASSED:
                {
                    status =
                            (smh.getLocalString(
                                    "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                            ".Status_Passed", // NOI18N
                                    "Passed")); // NOI18N
                    savePassResultsForDisplay(r);
                    Enumeration e = r.getGoodDetails().elements();
                    while (e.hasMoreElements()) {
                        s.append((String) e.nextElement());
                        s.append("\n"); // NOI18N
                    }
                    break;
                }
            case Result.FAILED:
                {
                    status =
                            (smh.getLocalString(
                                    "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                            ".Status_Failed", // NOI18N
                                    "FAILED")); // NOI18N
                    saveFailResultsForDisplay(r);
                    Enumeration e = r.getErrorDetails().elements();
                    while (e.hasMoreElements()) {
                        s.append((String) e.nextElement());
                        s.append("\n"); // NOI18N
                    }
                    break;
                }
            case Result.WARNING:
                {
                    status =
                            (smh.getLocalString(
                                    "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                            ".Status_Warning", // NOI18N
                                    "WARNING")); // NOI18N
                    saveWarnResultsForDisplay(r);
                    Enumeration e = r.getWarningDetails().elements();
                    while (e.hasMoreElements()) {
                        s.append((String) e.nextElement());
                        s.append("\n"); // NOI18N
                    }
                    break;
                }
            case Result.NOT_APPLICABLE:
                {
                    status =
                            (smh.getLocalString(
                                    "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                            ".Status_NotApplicable", // NOI18N
                                    "Not Applicable")); // NOI18N
                    saveNaResultsForDisplay(r);
                    Enumeration e = r.getNaDetails().elements();
                    while (e.hasMoreElements()) {
                        s.append((String) e.nextElement());
                        s.append("\n"); // NOI18N
                    }
                    break;
                }
            case Result.NOT_IMPLEMENTED:
                {
                    status =
                            (smh.getLocalString(
                                    "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                            ".Status_NotImplemented", // NOI18N
                                    "Not Implemented")); // NOI18N
                    saveNotImplementedResultsForDisplay(r);
                    break;
                }
            case Result.NOT_RUN:
                {
                    status =
                            (smh.getLocalString(
                                    "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                            ".Status_NotRun", // NOI18N
                                    "Not Run")); // NOI18N
                    saveNotRunResultsForDisplay(r);
                    break;
                }
            default:
                {
                    status =
                            (smh.getLocalString(
                                    "com.sun.enterprise.tools.verifier.gui.ResultsPanel" + // NOI18N
                            ".Status_Unknown", // NOI18N
                                    "Unknown")); // NOI18N
                    saveDefaultResultsForDisplay(r);
                    break;
                }
        }
        // create a table row for this result
        Object[] row = {r.getComponentName(), r.getTestName(), status};
        if (ControlPanel.getReportLevel() == VerifierConstants.FAIL &&
                r.getStatus() == Result.FAILED) {
            details.add(s.toString());
            tableModel.addRow(row);
        }

        if (ControlPanel.getReportLevel() == VerifierConstants.WARN &&
                (r.getStatus() == Result.FAILED ||
                r.getStatus() == Result.WARNING)) {
            details.add(s.toString());
            tableModel.addRow(row);
        }

        if (ControlPanel.getReportLevel() == VerifierConstants.ALL) {
            details.add(s.toString());
            tableModel.addRow(row);
        }
    }

    public void allTestsFinished(EventObject e) {
        // do nothing for now
    }
}
