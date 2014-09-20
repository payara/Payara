/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.installer.util;

import java.io.*;
import java.util.*;
import java.lang.*;
import org.openinstaller.util.ExecuteCommand;
import org.glassfish.installer.util.WindowsScriptManager;

/* This class accepts method paramters to Manage Windows short cut program
 * group and items. Extends the Impl. ScriptManager that actuall does the
 * work.
 */
public class WindowsShortcutManager extends WindowsScriptManager {

	/*
	 * Create short cut of type .LNK, this is the standard shortcut that points
	 * to executables and batch files on windows.
	 */
	public boolean createShortCut(String tFolderName, String tLinkName,
			String tTargetPath, String tLinkDescription, String tArgumentList,
			String tIconLocation, String tWorkingDirectory, String tWindowStyle) {

		boolean retStatus = true;
		WindowsScriptManager wsMgr = new WindowsScriptManager();

		wsMgr.CREATE_LNK_SHORTCUT_SCRIPT_CODE = wsMgr.CREATE_LNK_SHORTCUT_SCRIPT_CODE
				.replaceAll("%%FOLDER_NAME%%", tFolderName).replaceAll(
						"%%NAME%%", tLinkName).replaceAll("%%TARGET_PATH%%",
						tTargetPath).replaceAll("%%DESCRIPTION%%",
						tLinkDescription).replaceAll("%%ARGUMENTS%%",
						tArgumentList).replaceAll("%%ICON_FILE_PATH%%",
						tIconLocation).replaceAll("%%WORKING_DIRECTORY%%",
						tWorkingDirectory).replaceAll("%%WINDOW_STYLE%%",
						tWindowStyle);
		try {

			wsMgr.execute(wsMgr.CREATE_LNK_SHORTCUT_SCRIPT_CODE);
		} catch (Exception ex) {
			// Should we log this?
			retStatus = false;
		}
		return retStatus;
	}

	/*
	 * Create short cut of type .URL, overridden method to handle htmls and web
	 * links.
	 */
	public boolean createShortCut(String tFolderName, String tLinkName,
			String tTargetPath) {

		boolean retStatus = true;
		WindowsScriptManager wsMgr = new WindowsScriptManager();

		wsMgr.CREATE_URL_SHORTCUT_SCRIPT_CODE = wsMgr.CREATE_URL_SHORTCUT_SCRIPT_CODE
				.replaceAll("%%FOLDER_NAME%%", tFolderName).replaceAll(
						"%%NAME%%", tLinkName).replaceAll("%%TARGET_PATH%%",
						tTargetPath);
		try {

			wsMgr.execute(wsMgr.CREATE_URL_SHORTCUT_SCRIPT_CODE);
		} catch (Exception ex) {
			// Should we log this?
			retStatus = false;
		}
		return retStatus;
	}

	/* Create the folder that holds the shortcut. */
	public boolean createFolder(String tFolderName) {

		boolean retStatus = true;
		WindowsScriptManager wsMgr = new WindowsScriptManager();

		wsMgr.CREATE_FOLDER_SCRIPT_CODE = wsMgr.CREATE_FOLDER_SCRIPT_CODE
				.replaceAll("%%FOLDER_NAME%%", tFolderName);

		try {
			wsMgr.execute(wsMgr.CREATE_FOLDER_SCRIPT_CODE);
		} catch (Exception ex) {
			// Should we log this?
			retStatus = false;
		}
		;
		return retStatus;
	}

	/* Delete the folder that holds the shortcut. */
	public boolean deleteFolder(String tFolderName) {

		boolean retStatus = true;
		WindowsScriptManager wsMgr = new WindowsScriptManager();

		wsMgr.DELETE_FOLDER_SCRIPT_CODE = wsMgr.DELETE_FOLDER_SCRIPT_CODE
				.replaceAll("%%FOLDER_NAME%%", tFolderName);
		try {

			wsMgr.execute(wsMgr.DELETE_FOLDER_SCRIPT_CODE);
		} catch (Exception ex) {
			// Should we log this?
			retStatus = false;
		}
		return retStatus;
	}


   	/* Delete the folder that holds the shortcut. */
	public boolean deleteShortCut(String tFolderName, String tLinkName) {

		boolean retStatus = true;
		WindowsScriptManager wsMgr = new WindowsScriptManager();

		wsMgr.DELETE_ITEM_SCRIPT_CODE = wsMgr.DELETE_ITEM_SCRIPT_CODE
				.replaceAll("%%FOLDER_NAME%%", tFolderName)
                                .replaceAll("%%NAME%%", tLinkName);
                try {

			wsMgr.execute(wsMgr.DELETE_ITEM_SCRIPT_CODE);
		} catch (Exception ex) {
			// Should we log this?
			retStatus = false;
		}

       	return retStatus;
	}

}
