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
import org.openinstaller.util.ExecuteCommand;

/* This class is the windows implementation to manage short cuts.
 * Current implementation relies on Windows Scripting Host.
 */
public class WindowsScriptManager {

	// Script code to create short cut of type ".lnk"
	protected String CREATE_LNK_SHORTCUT_SCRIPT_CODE = "Dim WSHShell, LinkFile, StrUserDesktop"
			+ "\n"
			+ "Set WSHShell = WScript.CreateObject(\"WScript.Shell\")"
			+ "\n"
			+ "StrUserDesktop = WshShell.SpecialFolders(\"StartMenu\")"
			+ "\n \n"
			+

			"LinkFile = StrUserDesktop & \"\\%%FOLDER_NAME%%\\%%NAME%%.lnk\""
			+ "\n"
			+ "Set Link = WSHShell.CreateShortcut(LinkFile)"
			+ "\n"
			+ "Link.TargetPath = \"%%TARGET_PATH%%\""
			+ "\n"
			+ "Link.Description = \"%%DESCRIPTION%%\""
			+ "\n"
			+ "Link.Arguments = \"%%ARGUMENTS%%"
			+ "\n"
			+ "Link.IconLocation = \"%%ICON_FILE_PATH%%\""
			+ "\n"
			+ "Link.WorkingDirectory = \"%%WORKING_DIRECTORY%%\""
			+ "\n"
			+ "Link.WindowStyle = \"%%WINDOW_STYLE%%\""
			+ "\n"
			+ "Link.Save"
			+ "\n";

	// Script code to create short cut of type ".url"
	protected String CREATE_URL_SHORTCUT_SCRIPT_CODE = "Dim WSHShell, LinkFile, StrUserDesktop"
			+ "\n"
			+ "Set WSHShell = WScript.CreateObject(\"WScript.Shell\")"
			+ "\n"
			+ "StrUserDesktop = WshShell.SpecialFolders(\"StartMenu\")"
			+ "\n \n"
			+

			"LinkFile = StrUserDesktop & \"\\%%FOLDER_NAME%%\\%%NAME%%.url\""
			+ "\n"
			+ "Set Link = WSHShell.CreateShortcut(LinkFile)"
			+ "\n"
			+ "Link.TargetPath = \"%%TARGET_PATH%%\""
			+ "\n"
			+ "Link.Save"
			+ "\n";

	// Script code to delete the entire program group folder.
	protected String DELETE_FOLDER_SCRIPT_CODE =

	"Dim WSHShell, StrUserDesktop, FsObj"
			+ "\n"
			+ "Set WSHShell = WScript.CreateObject(\"WScript.Shell\")"
			+ "\n"
			+ "StrUserDesktop = WshShell.SpecialFolders(\"StartMenu\")"
			+ "\n \n"
			+ "Set FsObj = CreateObject(\"Scripting.FileSystemObject\")"
			+ "\n"
			+ "If FsObj.FolderExists(StrUserDesktop & \"\\%%FOLDER_NAME%%\") Then"
			+ "\n"
			+ "	FsObj.DeleteFolder(StrUserDesktop & \"\\%%FOLDER_NAME%%\")"
			+ "\n" + "End If" + "\n";

	// Script code to create the entire program group folder.
	protected String CREATE_FOLDER_SCRIPT_CODE = "Dim WSHShell, StrUserDesktop, FsObj"
			+ "\n"
			+ "Set WSHShell = WScript.CreateObject(\"WScript.Shell\")"
			+ "\n"
			+ "StrUserDesktop = WshShell.SpecialFolders(\"StartMenu\")"
			+ "\n \n"
			+ "Set FsObj = CreateObject(\"Scripting.FileSystemObject\")"
			+ "\n"
			+ "If Not FsObj.FolderExists(StrUserDesktop & \"\\%%FOLDER_NAME%%\") Then"
			+ "\n"
			+ "	FsObj.CreateFolder(StrUserDesktop & \"\\%%FOLDER_NAME%%\")"
			+ "\n" + "End If" + "\n";

	// Script code to delete individual shortcuts.
        protected String DELETE_ITEM_SCRIPT_CODE =
            "Dim WSHShell, StrUserDesktop, FsObj"
            + "\n"
            + "Set WSHShell = WScript.CreateObject(\"WScript.Shell\")"
            + "\n"
            + "StrUserDesktop = WshShell.SpecialFolders(\"StartMenu\")"
            + "\n \n"
            + "Set FsObj = CreateObject(\"Scripting.FileSystemObject\")"
            + "\n"
            + "If FsObj.FileExists(StrUserDesktop & \"\\%%FOLDER_NAME%%\\%%NAME%%\") Then"
            + "\n"
            + "	FsObj.DeleteFile(StrUserDesktop & \"\\%%FOLDER_NAME%%\\%%NAME%%\")"
            + "\n" + "End If" + "\n";



	/*
	 * Generic execute method, that takes in the cscript commands, convert it to
	 * a temporary .vbs script, then execute it. The temporary file will be
	 * deleted.
	 */
	protected boolean execute(String scriptCode) {
		final File theTempFile;
		FileWriter theWriter = null;
		boolean retValue = true;
		String theOutput = "";
		try {
			theTempFile = File.createTempFile("wshscript", ".vbs");
			theWriter = new FileWriter(theTempFile);
			theWriter.write(scriptCode);
			theWriter.close();
			// Use the cscript command to execute the VBScript file.
			final String[] theCmd = new String[] { "cscript",
					"\"" + theTempFile.getAbsolutePath() + "\"", "//NOLOGO" };
			final ExecuteCommand theExec = new ExecuteCommand(theCmd);
			theExec.execute();
			theOutput = theExec.getAllOutput();
			// Delete the temporary file
			theTempFile.delete();
		} catch (Exception theIOE) {
			return retValue;
		}
		return retValue;
	}
}
