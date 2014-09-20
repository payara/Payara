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


import javax.help.CSH;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.tools.verifier.util.LogDomains;
import com.sun.enterprise.tools.verifier.StringManagerHelper;
import com.sun.enterprise.tools.verifier.util.VerifierConstants;

public class ControlPanel extends JPanel {

    static com.sun.enterprise.util.LocalStringManagerImpl smh =
            StringManagerHelper.getLocalStringsManager();
    static String allString =
            (smh.getLocalString
            ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
            ".AllRadioButton", // NOI18N
                    "All Results")); // NOI18N
    static String failString =
            (smh.getLocalString
            ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
            ".FailuresOnlyRadioButton", // NOI18N
                    "Failures Only")); // NOI18N
    static String warnString =
            (smh.getLocalString
            ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
            ".FailuresWarningRadioButton", // NOI18N
                    "Failures and Warnings only")); // NOI18N

    // Strings used for 508 compliance
    static String buttonName =
            (smh.getLocalString
            ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
            ".buttonName", // NOI18N
                    "Button")); // NOI18N
    static String buttonDesc =
            (smh.getLocalString
            ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
            ".buttonDesc", // NOI18N
                    "This is a button used to select an action")); // NOI18N
    static String radioButtonName =
            (smh.getLocalString
            ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
            ".radioButtonName", // NOI18N
                    "Radio Button")); // NOI18N
    static String radioButtonDesc =
            (smh.getLocalString
            ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
            ".radioButtonDesc", // NOI18N
                    "This is a radio button used to select one from option from many choices")); // NOI18N
    static String panelName =
            (smh.getLocalString
            ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
            ".panelName", // NOI18N
                    "Panel")); // NOI18N
    static String panelDesc =
            (smh.getLocalString
            ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
            ".panelDesc", // NOI18N
                    "This is a panel in the verifier window")); // NOI18N

    static JRadioButton allButton = new JRadioButton(allString);
    static JRadioButton failButton = new JRadioButton(failString);
    static JRadioButton warnButton = new JRadioButton(warnString);
    static final String helpsetName = "Help"; // NOI18N


    // Logger to log messages
    private static Logger logger = LogDomains.getLogger(LogDomains.AVK_VERIFIER_LOGGER);

    // Main HelpSet & Broker
    private static HelpBroker mainHB = null;

    // Defaults for Main Help, - don't i18n, must be called this
    private static int reportLevel = VerifierConstants.ALL;

    final DefaultListModel listModel; // the list of files to verify
    
    JButton okButton;
    JButton closeButton;
    JButton helpButton;

    JFrame parent;
    HelpSet mainHS = null;

   /**
     * This is the control panel of the Verifier GUI
     */
    public ControlPanel(JFrame p) {
        parent = p;

        // 508 for this panel
        this.getAccessibleContext().setAccessibleName(panelName);
        this.getAccessibleContext().setAccessibleDescription(panelDesc);
        allButton.getAccessibleContext().setAccessibleName(radioButtonName);
        allButton.getAccessibleContext().setAccessibleDescription(radioButtonDesc);
        failButton.getAccessibleContext().setAccessibleName(radioButtonName);
        failButton.getAccessibleContext().setAccessibleDescription(radioButtonDesc);
        warnButton.getAccessibleContext().setAccessibleName(radioButtonName);
        warnButton.getAccessibleContext().setAccessibleDescription(radioButtonDesc);

        // set up title border
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                (smh.getLocalString("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".ItemsToBeVerifierPanelLabel", // NOI18N
                        "Items to be Verified")))); // NOI18N
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        //Create the verification list U

        //buttons
        JButton addButton = new JButton((smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".AddButton", // NOI18N
                        "Add..."))); // NOI18N
        JButton deleteButton = new JButton((smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".DeleteButton", // NOI18N
                        "Delete"))); // NOI18N

        // 508 compliance for the above buttons
        addButton.getAccessibleContext().setAccessibleName(buttonName);
        addButton.getAccessibleContext().setAccessibleDescription(buttonDesc);
        addButton.setMnemonic('A');
        deleteButton.getAccessibleContext().setAccessibleName(buttonName);
        deleteButton.getAccessibleContext().setAccessibleDescription(buttonDesc);
        deleteButton.setMnemonic('D');

        //main part of the dialog
        listModel = new DefaultListModel();
        final JList list = new JList(listModel);
        // 508 for JList
        list.getAccessibleContext().setAccessibleName(smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".listName", // NOI18N
                        "List")); // NOI18N
        list.getAccessibleContext().setAccessibleDescription(smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".listDesc", // NOI18N
                        "This is a list")); // NOI18N

        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        JScrollPane listScroller = new JScrollPane(list);
        // 508 for JScrollPane
        listScroller.getAccessibleContext().setAccessibleName(smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".scrName", // NOI18N
                        "Scroll Pane")); // NOI18N
        listScroller.getAccessibleContext().setAccessibleDescription
                (smh.getLocalString("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".scrDesc", // NOI18N
                        "This is a scroll pane that helps to scroll the list")); // NOI18N
        listScroller.setPreferredSize(new Dimension(250, 80));
        //XXX: Must do the following, too, or else the scroller thinks
        //XXX: it's taller than it is:
        listScroller.setMinimumSize(new Dimension(250, 80));
        listScroller.setAlignmentX(LEFT_ALIGNMENT);

        //Create a container so that we can add a title around
        //the scroll pane.  Can't add a title directly to the
        //scroll pane because its background would be white.
        //Lay out the label and scroll pane from top to button.
        JPanel listPane = new JPanel();
        // 508 for this panel
        listPane.getAccessibleContext().setAccessibleName(panelName);
        listPane.getAccessibleContext().setAccessibleDescription(panelDesc);
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
        //JLabel label = new JLabel("Selected Items");
        //label.setLabelFor(list);
        //listPane.add(label);
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        // 508 for this panel
        buttonPane.getAccessibleContext().setAccessibleName(panelName);
        buttonPane.getAccessibleContext().setAccessibleDescription(panelDesc);
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(addButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(deleteButton);

        //Put everything together, using the content pane's BorderLayout.
        JPanel listPanel = new JPanel();
        // 508 for this panel
        listPanel.getAccessibleContext().setAccessibleName(panelName);
        listPanel.getAccessibleContext().setAccessibleDescription(panelDesc);
        listPanel.setLayout(new BorderLayout());
        listPanel.add(listPane, BorderLayout.CENTER);
        listPanel.add(buttonPane, BorderLayout.SOUTH);

        // create the file chooser
        final JFileChooser fileChooser = new JFileChooser();

        // Add 508 compliance
        fileChooser.getAccessibleContext().setAccessibleName(smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".fcName", // NOI18N
                        "FileChooser")); // NOI18N
        fileChooser.getAccessibleContext().setAccessibleDescription(smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".fcDesc", // NOI18N
                        "This dialog box enables selection of files")); // NOI18N
        fileChooser.setApproveButtonMnemonic('O');

        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("ApproveSelection")) { // NOI18N
                    File[] files = fileChooser.getSelectedFiles();
                    for (int i = 0; i < files.length; i++) {
                        if (!listModel.contains(files[i])) {//don't allow duplicates
                            listModel.addElement(files[i]);
                        }
                    }
                    //select the last one selected for verification.
                    list.setSelectedIndex(listModel.getSize() - 1);
                }
            }
        });

        // set up file chooser button listeners
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser.rescanCurrentDirectory();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.showOpenDialog(null);
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!list.isSelectionEmpty()) {
                    Object[] selections = list.getSelectedValues();
                    for (int i = 0; i < selections.length; i++) {
                        listModel.removeElement(selections[i]);
                    }
                } else {
                    JOptionPane.showMessageDialog(parent,
                            (smh.getLocalString
                            ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                            ".optionPane.deleteButtonNoFiles", // NOI18N
                                    "You must first select file to delete.")) + // NOI18N
                            "\n" + // NOI18N
                            (smh.getLocalString
                            ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                            ".optionPane.deleteButtonNoFiles2", // NOI18N
                                    "Then click on the Delete button, to delete " + // NOI18N
                            "file from list of files to be verified.")), // NOI18N
                            (smh.getLocalString
                            ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                            ".optionPane.deleteTitle", // NOI18N
                                    "ERROR")), // NOI18N
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // set-up the radio buttons.
        allButton.setMnemonic(KeyEvent.VK_L);
        allButton.setActionCommand(allString);
        allButton.setSelected(getReportLevel() == VerifierConstants.ALL);

        failButton.setMnemonic(KeyEvent.VK_F);
        failButton.setActionCommand(failString);
        failButton.setSelected(getReportLevel() == VerifierConstants.FAIL);

        warnButton.setMnemonic(KeyEvent.VK_W);
        warnButton.setActionCommand(warnString);
        warnButton.setSelected(getReportLevel() == VerifierConstants.WARN);

        // Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(allButton);
        group.add(failButton);
        group.add(warnButton);


        // Put the radio buttons in a column in a panel
        JPanel radioPanel = new JPanel();
        // 508 for this panel
        radioPanel.getAccessibleContext().setAccessibleName(panelName);
        radioPanel.getAccessibleContext().setAccessibleDescription(panelDesc);
        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
        JLabel d = new JLabel((smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".RadioButtonLabel", // NOI18N
                        "Display:"))); // NOI18N
        d.setVerticalAlignment(SwingConstants.BOTTOM);
        // 508 compliance for the JLabel
        d.getAccessibleContext().setAccessibleName(smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".labelName", // NOI18N
                        "Label")); // NOI18N
        d.getAccessibleContext().setAccessibleDescription(smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".labelDesc", // NOI18N
                        "This is a label")); // NOI18N
        radioPanel.add(d);
        radioPanel.add(allButton);
        radioPanel.add(failButton);
        radioPanel.add(warnButton);

        // create the control buttons
        okButton =
                new JButton((smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".OKButton", // NOI18N
                        "OK"))); // NOI18N
        closeButton =
                new JButton((smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".CloseButton", // NOI18N
                        "Close"))); // NOI18N
        helpButton =
                new JButton((smh.getLocalString
                ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                ".HelpButton", // NOI18N
                        "Help"))); // NOI18N

        // 508 compliance for the above buttons
        okButton.getAccessibleContext().setAccessibleName(buttonName);
        okButton.getAccessibleContext().setAccessibleDescription(buttonDesc);
        okButton.setMnemonic('O');
        closeButton.getAccessibleContext().setAccessibleName(buttonName);
        closeButton.getAccessibleContext().setAccessibleDescription(buttonDesc);
        closeButton.setMnemonic('C');
        helpButton.getAccessibleContext().setAccessibleName(buttonName);
        helpButton.getAccessibleContext().setAccessibleDescription(buttonDesc);
        helpButton.setMnemonic('H');


        boolean mainHelpSetWoes = false;
        boolean usingDeployTool = false;

        if (!usingDeployTool) {
            // Create the main HelpBroker
            try {
                ClassLoader cl = ControlPanel.class.getClassLoader();
                URL url = HelpSet.findHelpSet(cl, helpsetName);
                mainHS = new HelpSet(cl, url);
            } catch (Exception ee) {
                logger.log(Level.WARNING,
                        "com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                        ".debug.helpSetMissing", // NOI18N
                        new Object[]{helpsetName});
                JOptionPane.showMessageDialog(this,
                        (smh.getLocalString
                        ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                        ".optionPane.helpSetMissing1", // NOI18N
                                "Could not find Help Set for {0}.", // NOI18N
                                new Object[]{helpsetName})) +
                        "\n" + // NOI18N
                        (smh.getLocalString
                        ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                        ".optionPane.helpSetMissing2", // NOI18N
                                "Please consult your host administrator. " + // NOI18N
                        "Starting Verifier with JavaHelp disabled."))); // NOI18N
                mainHelpSetWoes = true;
            } catch (ExceptionInInitializerError ex) {
                logger.log(Level.WARNING,
                        "com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                        ".debug.ExceptionInInitializerError"); // NOI18N
                JOptionPane.showMessageDialog(this,
                        (smh.getLocalString
                        ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                        ".optionPane.helpSetMissing1", // NOI18N
                                "Could not find Help Set for {0}.", // NOI18N
                                new Object[]{helpsetName})) +
                        "\n" + // NOI18N
                        (smh.getLocalString
                        ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                        ".optionPane.helpSetMissing2", // NOI18N
                                "Please consult your host administrator.  " + // NOI18N
                        "Starting Verifier with JavaHelp disabled."))); // NOI18N
                mainHelpSetWoes = true;
            }

            if (!mainHelpSetWoes) {
                mainHB = mainHS.createHelpBroker();
                setMainHelpBroker(mainHB);

                // hook this into "top" equivalent for verifier
                mainHB.enableHelpKey(this, "Verifier", null); // NOI18N

                CSH.setHelpIDString(helpButton, "Verifier"); // NOI18N

                // set up help button listener
                helpButton.addActionListener(new CSH.DisplayHelpFromSource(mainHB));
            } else {
                // grey out help button since there was woes...
                helpButton.setEnabled(false);
            }
        } else {
            // using deploytool, turn off javahelp button.
            helpButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(parent,
                            (smh.getLocalString
                            ("com.sun.enterprise.tools.verifier.gui.ControlPanel" + // NOI18N
                            ".optionPane.helpDisabled", // NOI18N
                                    "Verifier online help disabled."))); // NOI18N
                }
            });
        }


        // put the control buttons on a panel
        JPanel buttonPanel = new JPanel();
        // 508 for this panel
        buttonPanel.getAccessibleContext().setAccessibleName(panelName);
        buttonPanel.getAccessibleContext().setAccessibleDescription(panelDesc);
        GridLayout gl = new GridLayout(0, 1);
        gl.setVgap(10);
        gl.setHgap(5);
        buttonPanel.setLayout(gl);
        buttonPanel.add(okButton);
        buttonPanel.add(closeButton);
        buttonPanel.add(helpButton);
        buttonPanel.add(new JLabel(""));

        // Add the controls to the Panel
        add(listPanel);
        add(radioPanel);
        add(buttonPanel);

        // Register a listener for the report level radio buttons.
        RadioListener myListener = new RadioListener();
        addRadioButtonListener((ActionListener) myListener);
    }

    public static void setMainHelpBroker(HelpBroker hb) {
        mainHB = hb;
        Thread t = new Thread() {
            public void run() {
                if (mainHB != null) {
                    mainHB.initPresentation();//reduce help first time startup
                }
            }
        };
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private static void setReportLevel(int rl) {
        reportLevel = rl;
    }

    static int getReportLevel() {
        return reportLevel;
    }

    class RadioListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == allButton) {
                setReportLevel(VerifierConstants.ALL);
            }
            if (e.getSource() == failButton) {
                setReportLevel(VerifierConstants.FAIL);
            }
            if (e.getSource() == warnButton) {
                setReportLevel(VerifierConstants.WARN);
            }
        }
    }

    // add a listener to the three radio buttons
    public static void addRadioButtonListener(ActionListener r) {
        allButton.addActionListener(r);
        failButton.addActionListener(r);
        warnButton.addActionListener(r);
    }

    // add a Jar file to the items to verify list
    public void addJarFile(File jarFile) {
        listModel.addElement(jarFile);
    }

    // remove all Jar files from the items to verify list
    public void removeAllJarFiles() {
        listModel.clear();
    }
}

