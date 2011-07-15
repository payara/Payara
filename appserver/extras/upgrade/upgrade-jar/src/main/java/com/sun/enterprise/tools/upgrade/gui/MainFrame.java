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

package com.sun.enterprise.tools.upgrade.gui;

import com.sun.enterprise.tools.upgrade.UpgradeToolMain;
import com.sun.enterprise.tools.upgrade.common.Branding;
import com.sun.enterprise.tools.upgrade.common.CommonInfoModel;
import com.sun.enterprise.tools.upgrade.common.DirectoryMover;
import com.sun.enterprise.tools.upgrade.common.UpgradeConstants;
import com.sun.enterprise.tools.upgrade.common.UpgradeUtils;
import com.sun.enterprise.tools.upgrade.common.arguments.ARG_source;
import com.sun.enterprise.tools.upgrade.common.arguments.ARG_target;
import com.sun.enterprise.tools.upgrade.gui.util.Utils;
import com.sun.enterprise.tools.upgrade.logging.LogService;
import com.sun.enterprise.util.i18n.StringManager;
import java.awt.CardLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;
import java.util.logging.Logger;
import javax.help.CSH;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

/**
 * Main frame of the upgrade tool GUI. Built with NB's "Matisse"
 * builder. Contains the image and button panels internally.
 * The data collection and results panels are in separate classes,
 * added to a card layout component here.
 *
 * For information on how results get to the text area in
 * the GUI, see the comments in UpgradeWorker.java.
 *
 * @author Bobby Bissett
 */
public class MainFrame extends javax.swing.JFrame implements DirectoryMover {

    private static final Logger logger = LogService.getLogger();
    private StringManager stringManager =
        StringManager.getManager(MainFrame.class);
    private static final CommonInfoModel commonInfoModel =
        CommonInfoModel.getInstance();

    // contained in the card layout of mainPanel
    private enum Panels { DATA_COLLECTION_PANEL, PROGRESS_PANEL }

    // needed to pass to worker thread to do the upgrade
    private final UpgradeToolMain upgradeToolMain;

    private final DataCollectionPanel dataCollectionPanel =
        new DataCollectionPanel(this);
    private final ProgressPanel progressPanel =
        new ProgressPanel();

    // values could be rebranded
    private String titleMessage;

    // image used in image panel
    private ImageIcon upgradeIcon;

    public MainFrame(UpgradeToolMain upgradeToolMain) {
        this.upgradeToolMain = upgradeToolMain;
        
        titleMessage = Branding.getString("upgrade.gui.mainframe.titleMessage",
            stringManager,
            commonInfoModel.getTarget().getVersion());

	String imageURLString =
            "com/sun/enterprise/tools/upgrade/gui/Appserv_upgrade_wizard.gif";
        URL imageURL = Branding.getWizardUrl(imageURLString);
        if (imageURL != null) {
            upgradeIcon = new ImageIcon(imageURL,
                "upgrade wizard icon");
        } else {
            // This shouldn't happen, but just in case we'd like to be told.
            System.err.println(String.format(
                "Cannot find image %s", imageURLString));
        }
        initComponents();

        // add listener to close app when window closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                doCancelAction();
            }
        });

        // enable help button
        if (Utils.getHelpBroker() != null) {
            Utils.getHelpBroker().enableHelpOnButton(
                helpButton, "WIZARD_FIRST", null);
        }

        // update status of 'next' button by checking state of panel
        dataCollectionPanel.checkState();
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JPanel imagePanel = new javax.swing.JPanel();
        imageLabel = new javax.swing.JLabel(upgradeIcon);
        mainPanel = new javax.swing.JPanel();
        javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
        javax.swing.JPanel centeredPanel = new javax.swing.JPanel();
        backButton = new javax.swing.JButton();
        nextButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        helpButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(titleMessage);
    setMinimumSize(new java.awt.Dimension(725, 545));

    imagePanel.setLayout(new javax.swing.BoxLayout(imagePanel, javax.swing.BoxLayout.LINE_AXIS));
    imagePanel.add(imageLabel);

    mainPanel.setLayout(new java.awt.CardLayout());

    backButton.setText(stringManager.getString("upgrade.gui.mainframe.backbutton"));
    backButton.setEnabled(false);
    backButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            backButtonActionPerformed(evt);
        }
    });

    nextButton.setText(stringManager.getString("upgrade.gui.mainframe.nextbutton"));
    nextButton.setEnabled(false);
    nextButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            nextButtonActionPerformed(evt);
        }
    });

    cancelButton.setText(stringManager.getString("upgrade.gui.mainframe.cancelbutton"));
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            cancelButtonActionPerformed(evt);
        }
    });

    helpButton.setText(stringManager.getString("upgrade.gui.mainframe.helpbutton"));
    helpButton.setToolTipText("Help");
    helpButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            helpButtonActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout centeredPanelLayout = new javax.swing.GroupLayout(centeredPanel);
    centeredPanel.setLayout(centeredPanelLayout);
    centeredPanelLayout.setHorizontalGroup(
        centeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(centeredPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(backButton)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(nextButton)
            .addGap(90, 90, 90)
            .addComponent(cancelButton)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(helpButton)
            .addContainerGap())
    );

    centeredPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {backButton, cancelButton, helpButton, nextButton});

    centeredPanelLayout.setVerticalGroup(
        centeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(centeredPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(centeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(nextButton)
                .addComponent(cancelButton)
                .addComponent(backButton)
                .addComponent(helpButton))
            .addContainerGap())
    );

    javax.swing.GroupLayout buttonPanelLayout = new javax.swing.GroupLayout(buttonPanel);
    buttonPanel.setLayout(buttonPanelLayout);
    buttonPanelLayout.setHorizontalGroup(
        buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, buttonPanelLayout.createSequentialGroup()
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(centeredPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap(65, Short.MAX_VALUE))
    );
    buttonPanelLayout.setVerticalGroup(
        buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, buttonPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(centeredPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
    );

    // custom "Pre-Adding Code"
    mainPanel.add(dataCollectionPanel, Panels.DATA_COLLECTION_PANEL.name());
    mainPanel.add(progressPanel, Panels.PROGRESS_PANEL.name());

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(imagePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 571, Short.MAX_VALUE))
                .addComponent(buttonPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addContainerGap())
    );
    layout.setVerticalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(mainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 444, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(imagePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(buttonPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 49, Short.MAX_VALUE)
            .addContainerGap())
    );

    pack();
    }// </editor-fold>//GEN-END:initComponents

    private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
        CardLayout layoutManager = (CardLayout) mainPanel.getLayout();
        layoutManager.show(mainPanel, Panels.DATA_COLLECTION_PANEL.name());
        nextButton.setText(
            stringManager.getString("upgrade.gui.mainframe.nextbutton"));
        backButton.setEnabled(false);
        nextButton.setEnabled(true);
        cancelButton.setEnabled(true);

        // todo: does not appear to be working
        CSH.setHelpIDString(helpButton, "WIZARD_FIRST");
    }//GEN-LAST:event_backButtonActionPerformed

    /*
     * If current panel is data collection panel, check arguments and
     * continue. Change button to "Finish" which simply exits the application.
     */
    private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
        // in this case, it's the "finish" button
        if (progressPanel.isVisible()) {
            performExit();
        }
        if (!dataCollectionPanel.isVisible()) {
            // this shouldn't happen, but might as well be safe
            throw new AssertionError("No expected panel is visible");
        }

        // validate inputs
        if (!this.processArguments()) {
            return;
        }
        printArguments();

        // UI changes
        CardLayout layoutManager = (CardLayout) mainPanel.getLayout();
        layoutManager.show(mainPanel, Panels.PROGRESS_PANEL.name());
        nextButton.setText(
            stringManager.getString("upgrade.gui.mainframe.finishbutton"));
        nextButton.setEnabled(false);
        cancelButton.setEnabled(false);
        progressPanel.getProgressBar().setIndeterminate(true);
        CSH.setHelpIDString(helpButton, "WIZARD_RESULT");

        // start worker thread
        UpgradeWorker upgradeWorker = new UpgradeWorker(this, upgradeToolMain);
        upgradeWorker.execute();
    }//GEN-LAST:event_nextButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        doCancelAction();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void helpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpButtonActionPerformed
        // nothing needed here
    }//GEN-LAST:event_helpButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton helpButton;
    private javax.swing.JLabel imageLabel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton nextButton;
    // End of variables declaration//GEN-END:variables

    @Override
    public boolean moveDirectory(File dir) {
        String message = String.format(stringManager.getString(
            "upgrade.gui.util.domainRenameOption", dir.getName()));
        String title = stringManager.getString(
            "upgrade.gui.util.domainNameConflict");
        int retVal = JOptionPane.showConfirmDialog(this, message, title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (JOptionPane.OK_OPTION != retVal) {
            return false;
        }
        UpgradeUtils.getUpgradeUtils(commonInfoModel).rename(dir);
        return true;
    }

    // called from data collection panel when requred information is present
    void allowContinue(boolean b) {
        nextButton.setEnabled(b);
    }

    /*
     * Called by the worker class (in the dispatch
     * thread) when the upgrade has finished.
     */
    void done() {
        JLabel progressLabel = progressPanel.getProgressLabel();
        progressLabel.setText(stringManager.getString(
            "upgrade.gui.progresspanel.progressLabel.DONE"));
        JProgressBar jpBar = progressPanel.getProgressBar();
        jpBar.setIndeterminate(false);
        jpBar.setValue(jpBar.getMaximum()); // use default
        backButton.setEnabled(true);
        nextButton.setEnabled(true); // really the "finish" button now
    }

    /*
     * Used by worker thread to append text.
     */
    ProgressPanel getProgressPanel() {
        return progressPanel;
    }

    // called by cancel button and main window listener
    private void doCancelAction() {
        int retVal = JOptionPane.showConfirmDialog(this,
            stringManager.getString("upgrade.gui.mainframe.exitMessage"),
            stringManager.getString("upgrade.gui.mainframe.exitMessageTitle"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        if (retVal == JOptionPane.NO_OPTION) {
            return;
        }
        performExit();
    }

    // called by cancel or finish button
    private void performExit() {
        // original code called listener in UpgradeToolMain that did sys exit
        System.exit(0);
    }

    /*
     * Method to validate source and target directory inputs on the GUI panel
     * Also checks whether the upgrade path is supported or not.
     */
    private boolean processArguments() {
        ARG_source argSource = new ARG_source();
        argSource.setRawParameters(dataCollectionPanel.getSourceDirPath());
        if (argSource.isValidParameter()) {
            argSource.exec();
        } else {
            // pop up error message
            JOptionPane.showMessageDialog(this,
                stringManager.getString("upgrade.gui.mainframe.invalidSourceMsg"),
                stringManager.getString("upgrade.gui.mainframe.invalidSourceTitle"),
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // in the GUI case, we'll allow users to fix domain name clashes
        commonInfoModel.getTarget().setDirectoryMover(this);
        ARG_target t = new ARG_target();
        t.setRawParameters(dataCollectionPanel.getDestinationDirPath());
        if (t.isValidParameter()) {
            t.exec();
        } else {
            // pop up error message
            JOptionPane.showMessageDialog(this,
                stringManager.getString("upgrade.gui.mainframe.invalidTargetMsg"),
                stringManager.getString("upgrade.gui.mainframe.invalidTargetTitle"),
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!commonInfoModel.isUpgradeSupported()) {
            JOptionPane.showMessageDialog(this,
                stringManager.getString("upgrade.gui.mainframe.versionNotSupportedMsg"),
                stringManager.getString("upgrade.gui.mainframe.versionNotSupportedTitle"),
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // store credentials if given in GUI and not already set
        if (commonInfoModel.getSource().getMasterPassword() == null) {
            char [] masterPassword = dataCollectionPanel.getMasterPassword();
            if (masterPassword.length > 0) {
                commonInfoModel.getSource().setMasterPassword(masterPassword);
            }
        }

        return true;
    }

    /*
     * Print user input but do not reveal the passwords.
     */
    private void printArguments() {
        logger.info(UpgradeConstants.ASUPGRADE + " -s " +
            commonInfoModel.getSource().getInstallDir() +
            "\t -t " + commonInfoModel.getTarget().getInstallDir());
    }

}
