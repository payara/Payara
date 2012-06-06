/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
// DcomConfigurator.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"

static WCHAR BUF[1024];

int _tmain(int argc, _TCHAR* argv[])
{
	DcomConfigurator con(argc, argv);
	if(con.isHelp()) {
		con.usage();
		return 0;
	}
	return con.configure();
}

DcomConfigurator::DcomConfigurator(int argc, _TCHAR* argv[]) {
	verbose = false;
	help = false;
	force = false;
	admin = new Persona(WinBuiltinAdministratorsSid);
	adminOwnerId = admin->getSidString();
	scriptingOwnerId = getOwnerString(HKEY_CLASSES_ROOT, REG_SCRIPTING);
	wmiOwnerId = getOwnerString(HKEY_CLASSES_ROOT, REG_WMI);
	parse(argc, argv);
	
	if(verbose)
		setVerbose(); // let utils know
}

DcomConfigurator::~DcomConfigurator() {
	delete admin;
}

void DcomConfigurator::usage()
{
	verbose = true;
	p("Usage: DCOMConfigurator [-h|--help] [-v|--verbose] [-f|--force] [-n|--dry-run]\n");
	p("\n");
	p("DCOM Configurator attempts to change permissions and possibly the ownership of 2 Registry keys.\nThe keys allow access to WMI and Scripting.\n");
	p("\n");
	p("[-h|--help]    Show this help\n");
	p("[-v|--verbose] Explain what happened\n");
	p("[-f|--force]   Forces Ownership Takeover of a Registry Key\n");
	p("[-n|--dry-run] Don't really do it\n");
	
	
	bool b = is64();
	if(b) {
		p("\nYou are running on 64 bit Windows.  The 2 keys are automatically mapped to 32-bit registry keys.\n");
		p("This is of no concern unless you use the Registry Editor (regedit.exe).  In that case you need to look in (1) instead of (2)\n");
		p("(1) HKEY_CLASSES_ROOT\\Wow6432Node\\CLSID\\...\n");
		p("(2) HKEY_CLASSES_ROOT\\CLSID\\...\n");
	}
	else
		p("\nYou are running on 32 bit Windows.");
}

/**
  * Ugly and fast to develop.  Feel free to improve it...
  * Probably should re-work if an item #5 or 6 is added.  
  */

void DcomConfigurator::parse(int argc, _TCHAR* argv[])
{
	for(int i = 1; i < argc; i++) {
		// only do this code below 0 or 1 time
		if(verbose == false) {
			if(!lstrcmpi(L"-v", argv[i]) || !lstrcmpi(L"--verbose", argv[i])) {
				verbose = true;
				wp(L">>>>>>>>>>>>>                          <<<<<<<<<<<<<\n");
				wp(L">>>>>>>>>>>>>   Set to Verbose Mode    <<<<<<<<<<<<<\n");
				wp(L">>>>>>>>>>>>>                          <<<<<<<<<<<<<\n");
			}
		}
		if(!lstrcmpi(L"-h", argv[i]) || !lstrcmpi(L"--help", argv[i])) {
			help = true;
		}
		if(!lstrcmpi(L"-f", argv[i]) || !lstrcmpi(L"--force", argv[i])) {
			force = true;
		}
	}
}

int DcomConfigurator::configure() {
	Persona admin(WinBuiltinAdministratorsSid);

	if(verbose)
		printOwners();

	int ret = configureRegKeys() == TRUE ? 0 : 1;
 	
	if(verbose)
		cout <<  message;
	
	return ret;
}

void DcomConfigurator::printOwners() {
	wprintf(L"Administrators group SID: [%s]\n", adminOwnerId);
	wprintf(L"Key: [%s]  Owner: [%s]\n", REG_SCRIPTING, scriptingOwnerId);
	wprintf(L"Key: [%s]  Owner: [%s]\n", REG_WMI, wmiOwnerId);

	if(is64()) {
	wprintf(L"Redirected Key: [%s]\n", REG_SCRIPTING_WOW, scriptingOwnerId);
	wprintf(L"Redirected Key: [%s]\n", REG_WMI_WOW, wmiOwnerId);
	}
}

BOOL DcomConfigurator::configureRegKeys() {
	BOOL ret = TRUE;	
	BOOL ret2 = TRUE;

	if(!Equal(adminOwnerId, scriptingOwnerId) || force) {
		ret = TakeOwnership((LPTSTR)REG_SCRIPTING_FULL);
		message += "Took ownership and adjusted permissions of Scripting Registry Key.\n";
	}
	else
		message += "No need to adjust the Scripting Registry Key.\n";

	
	if(!Equal(adminOwnerId, wmiOwnerId) || force) {
		ret2 = TakeOwnership((LPTSTR)REG_WMI_FULL);
		message += "Took ownership and adjusted permissions of WMI Registry Key.\n";
	}
	else
		message += "No need to adjust the WMI Registry Key.\n";

	return ret && ret2;
}

void DcomConfigurator::p(LPCSTR msg) {
	if(verbose)
		printf(msg);
}

void DcomConfigurator::wp(LPCWSTR wmsg) {
	if(verbose)
		wprintf(wmsg);
}

