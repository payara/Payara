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

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.LogRecord;

import com.sun.enterprise.tools.verifier.ResultManager;
import com.sun.enterprise.tools.verifier.StringManagerHelper;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.VerifierFrameworkContext;

public class MainPanel extends JPanel implements Runnable {

    static com.sun.enterprise.util.LocalStringManagerImpl smh =
            StringManagerHelper.getLocalStringsManager();
    ResultsPanel resultsPanel = new ResultsPanel();
    ControlPanel verifierControls;

    JLabel statusLabel = new JLabel((smh.getLocalString
            ("com.sun.enterprise.tools.verifier.gui.MainPanel" + // NOI18N
            ".Status_Idle", // NOI18N
                    "Idle"))); // NOI18N
    Verifier verifier;
    Enumeration components;
    Thread running = null;

    /**
     * This is the top-level panel of the Verifier GUI
     */
    public MainPanel(JFrame parent) {
        this(parent, null, null);
    }

    public MainPanel(JFrame parent, String jarFileName, Verifier verifier) {
        super(new BorderLayout(), true);

        // 508 compliance
        this.getAccessibleContext().setAccessibleName(smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.MainPanel" + // NOI18N
                ".panelName", // NOI18N
                        "Panel")); // NOI18N
        this.getAccessibleContext().setAccessibleDescription(smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.MainPanel" + // NOI18N
                ".PanelDesc", // NOI18N
                        "This is a panel")); // NOI18N
        statusLabel.getAccessibleContext().setAccessibleName(smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.MainPanel" + // NOI18N
                ".labelName", // NOI18N
                        "Label")); // NOI18N
        statusLabel.getAccessibleContext().setAccessibleDescription
                (smh.getLocalString("com.sun.enterprise.tools.verifier.gui.MainPanel" + // NOI18N
                ".labelDesc", // NOI18N
                        "This is a label")); // NOI18N

        //verifier.setFrame(parent);
        verifierControls = new ControlPanel(parent);
        // set the initial jar in file list
        setJarFilename(jarFileName);
        this.verifier = verifier;

        add("North", verifierControls); // NOI18N
        add("Center", resultsPanel); // NOI18N
        add("South", statusLabel); // NOI18N


        verifierControls.okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (getFileList().hasMoreElements()) {
                    resultsPanel.clearOldResults();
                    start();
                } else {
                    JOptionPane.showMessageDialog(verifierControls, (smh.getLocalString
                            ("com.sun.enterprise.tools.verifier.gui.MainPanel" + // NOI18N
                            ".optionPane.okButtonNoFiles", // NOI18N
                                    "You must first select file to verify.")) + // NOI18N
                            "\n" + // NOI18N
                            (smh.getLocalString
                            ("com.sun.enterprise.tools.verifier.gui.MainPanel" + // NOI18N
                            ".optionPane.okButtonNoFiles2", // NOI18N
                                    "Use the Add button to select file to be verified," + // NOI18N
                            " then click on OK button."))); // NOI18N
                }
            }
        });

        verifierControls.closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (MainFrame.getExitOnClose()) {
                    System.exit(0);
                } else {
                    stop();
                    enableOK();
                    enableClose();
                    reset();
                    try {
                        Class cls = Class.forName("javax.swing.JFrame"); // NOI18N
                        JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(cls, resultsPanel);
                        frame.setVisible(false);

                        //frame.dispose();
                    } catch (ClassNotFoundException ex) {
                        // ??can this happen
                        ex.getMessage();
                    }
                }
            }
        });
    }

    /**
     * Retrieve the verifierControls panel of the Verifier GUI
     */
    public ControlPanel getVerifierControlPanel() {
        return verifierControls;
    }

    public void setJarFilename(String jarFileName) {
        // set the initial jar in file list
        if (jarFileName != null) {
            File jarFile = new File(jarFileName);
            if (jarFile.exists()) {
                getVerifierControlPanel().addJarFile(jarFile);
            }
        }
    }

    private Verifier getVerifier() {
        return verifier;
    }

    public void reset() {
        resultsPanel.clearOldResults();
        resultsPanel.clearResults();
        verifierControls.removeAllJarFiles();
    }

    public void setStatus(String stat) {
        statusLabel.setText(stat);
    }

    public Enumeration getFileList() {
        return verifierControls.listModel.elements();
    }

    public void clearResults() {
        resultsPanel.clearResults();
    }

    void disableOK() {
        verifierControls.okButton.setEnabled(false);
    }

    void enableOK() {
        verifierControls.okButton.setEnabled(true);
    }

    void disableClose() {
        verifierControls.closeButton.setEnabled(false);
    }

    void enableClose() {
        verifierControls.closeButton.setEnabled(true);
    }

    public void start() {

        if (running == null) {
            components = getFileList();
            clearResults();
            running = new Thread(this);
            running.setPriority(Thread.MIN_PRIORITY);
            running.start();
        }
    }

    public void stop() {
        if (running != null) {
            running = null;
            setStatus((smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.gui.MainPanel" + // NOI18N
                    ".Status_Closed", // NOI18N
                            "Closed"))); // NOI18N
        }
    }

    public void run() {
        try {
            disableOK();

            while (components.hasMoreElements() && running != null) {
                File jarFile = ((File) components.nextElement());
                String jarName = ((jarFile).getPath());
                try {
                    setStatus((smh.getLocalString
                            ("com.sun.enterprise.tools.verifier.gui.MainPanel" + // NOI18N
                            ".Status_Verifying", // NOI18N
                                    "Verifying archive {0}...", // NOI18N
                                    new Object[]{jarName})));
                    VerifierFrameworkContext vfc = new VerifierFrameworkContext();
                    vfc.setJarFileName(jarFile.getAbsolutePath());
                    getVerifier().init(vfc);
                    getVerifier().verify();
                    Iterator itr = vfc.getResultManager().getError().iterator();
                    while (itr.hasNext()) {
                        LogRecord log = (LogRecord) itr.next();
                        log.setLoggerName(jarFile.getName());
                        resultsPanel.addError(log);
                    }
                    setStatus((smh.getLocalString
                            ("com.sun.enterprise.tools.verifier.gui.MainPanel" + // NOI18N
                            ".Status_WritingReport", // NOI18N
                                    "Writing report..."))); // NOI18N
                    verifier.generateReports();

                    if (vfc.getResultManager().getFailedCount() +
                            vfc.getResultManager().getErrorCount() ==
                            0) { // this code might not be called
                        resultsPanel.addDetailText((smh.getLocalString
                                ("com.sun.enterprise.tools.verifier.gui.MainPanel" + // NOI18N
                                ".AllTestsPassed", // NOI18N
                                        "{0}: All tests passed.", // NOI18N
                                        new Object[]{jarFile.getName()})) +
                                "\n"); // NOI18N
                    } else {
                        resultsPanel.addDetailText((smh.getLocalString
                                ("com.sun.enterprise.tools.verifier.gui.MainPanel" + // NOI18N
                                ".SomeTestsFailed", // NOI18N
                                        "{0}: Some tests failed.", // NOI18N
                                        new Object[]{jarFile.getName()})) +
                                "\n"); // NOI18N
                    }

                } catch (Throwable t) {
                    JOptionPane.showMessageDialog(this,
                            (smh.getLocalString
                            ("com.sun.enterprise.tools.verifier.gui.MainPanel" + // NOI18N
                            ".ErrorLoading", // NOI18N
                                    "Error verifying {0}: {1}", // NOI18N
                                    new Object[]{jarName, t.getMessage()})));
                    resultsPanel.addDetailText((smh.getLocalString
                            ("com.sun.enterprise.tools.verifier.gui.MainPanel" + // NOI18N
                            ".ErrorLoading", // NOI18N
                                    "Error verifying {0}: {1}", // NOI18N
                                    new Object[]{jarName, t.getMessage()})) +
                            "\n"); // NOI18N
                }
            }

            setStatus((smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.gui.MainPanel" + // NOI18N
                    ".Status_Idle", // NOI18N
                            "Idle"))); // NOI18N
            enableOK();
            enableClose();
        } finally {
            running = null;
        }
    }
}
