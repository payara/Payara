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

import javax.swing.*;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import com.sun.enterprise.tools.verifier.StringManagerHelper;
import com.sun.enterprise.tools.verifier.Verifier;

public class MainFrame extends JFrame {


    /**
     * Deploytool gui entry point (acessed via reflection)
     */
    private static MainFrame verifierPanel = null;
    private static boolean exitOnClose = false;
    MainPanel mp = null;

    /**
     * Constructor.
     */
    public MainFrame() {
        this(null);
    }

    public MainFrame(String jarFileName) {
        this(jarFileName, false, null);
    }

    public MainFrame(String jarFileName, boolean exitOnClose,
                     Verifier verifier) {
        super((StringManagerHelper.getLocalStringsManager().getLocalString
                ("com.sun.enterprise.tools.verifier.gui.MainFrame" + // NOI18N
                ".WindowTitle", // NOI18N
                        "Verify Specification Compliance"))); // NOI18N
        setExitOnClose(exitOnClose);

        // 508 compliance for the JFrame
        this.getAccessibleContext().setAccessibleName(StringManagerHelper.getLocalStringsManager()
                .getLocalString("com.sun.enterprise.tools.verifier.gui.MainFrame" + // NOI18N
                ".jfName", // NOI18N
                        "Main Window")); // NOI18N
        this.getAccessibleContext().setAccessibleDescription(StringManagerHelper.getLocalStringsManager()
                .getLocalString("com.sun.enterprise.tools.verifier.gui.MainFrame" + // NOI18N
                ".jfDesc", // NOI18N
                        "This is the main window of the verifier tool")); // NOI18N

        if (exitOnClose) {
            this.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
        }
        Container contentPane = getContentPane();
        mp = new MainPanel(this, jarFileName, verifier);
        contentPane.add(mp);
        JOptionPane.showMessageDialog(this,
                StringManagerHelper.getLocalStringsManager()
                .getLocalString("com.sun.enterprise.tools.verifier.gui.Deprecation", // NOI18N
                        "\nThis GUI has been deprecated. Please use the GUI that comes with NetBeans."), // NOI18N
                "WARNING", JOptionPane.WARNING_MESSAGE); // NOI18N
    }

    public static JFrame getDeploytoolVerifierFrame(File jarFile) {
        if (verifierPanel == null) {
            verifierPanel = new MainFrame();
        } else {
            verifierPanel.getMainPanel().reset();
        }
        if (jarFile != null) {
            verifierPanel.getMainPanel().setJarFilename(
                    jarFile.getAbsolutePath());
        }
        return verifierPanel;
    }


    public MainPanel getMainPanel() {
        return mp;
    }

    public static boolean getExitOnClose() {
        return exitOnClose;
    }

    public static void setExitOnClose(boolean b) {
        exitOnClose = b;
    }
}
