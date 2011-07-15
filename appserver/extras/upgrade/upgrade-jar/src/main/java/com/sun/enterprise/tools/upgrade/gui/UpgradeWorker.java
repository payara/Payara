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
import com.sun.enterprise.tools.upgrade.logging.LogService;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 * Worker thread to perform the upgrade and update the GUI.
 *
 * This thread adds a handler to the upgrade tool logger to
 * listen for information that should be presented to the user
 * in the GUI.
 *
 * @author Bobby Bissett
 */
public class UpgradeWorker extends SwingWorker<Void, Void> {

    // GUI components
    private final MainFrame mainFrame;
    private final ProgressPanel progressPanel;

    // Component that performs the upgrade
    private final UpgradeToolMain upgradeToolMain;

    // Logger and handler used to receive messages to output
    private static final Logger systemLogger = LogService.getLogger();

    public UpgradeWorker(MainFrame mainFrame, UpgradeToolMain upgradeToolMain) {
        this.mainFrame = mainFrame;
        this.upgradeToolMain = upgradeToolMain;
        this.progressPanel = mainFrame.getProgressPanel();
    }

    @Override
    protected Void doInBackground() throws Exception {
        GUILogHandler handler = new GUILogHandler(progressPanel);
        Throwable unexpected = null;
        try {
            systemLogger.addHandler(handler);
            upgradeToolMain.performUpgrade();
        } catch (Throwable t) {
            // just to be safe. shouldn't happen
            System.err.println(t.getLocalizedMessage());
            unexpected = t;
        } finally {
            systemLogger.removeHandler(handler);
            if (unexpected != null) {
                systemLogger.log(Level.SEVERE,
                    "Problem in swing worker thread", unexpected);
            }
        }
        return null;
    }

    /*
     * Called by the dispatch thread so it can update the GUI
     * directly.
     */
    @Override
    protected void done() {
        super.done();
        mainFrame.done();
    }

    /*
     * This helper class is used to listen for log messages and send
     * them to the progress panel that will display them.
     */
    private class GUILogHandler extends Handler {

        private final ProgressPanel progressPanel;

        private GUILogHandler(ProgressPanel progressPanel) {
            this.progressPanel = progressPanel;
        }

        @Override
        public void publish(LogRecord record) {
            progressPanel.appendResultString(
                record.getMessage(), record.getLevel());
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void close() throws SecurityException {
            // no-op
        }

    }
}
