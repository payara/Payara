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
#include "stdafx.h"

LPCTSTR REG_SCRIPTING = TEXT("CLSID\\{72C24DD5-D70A-438B-8A42-98424B88AFB8}");
LPCTSTR REG_WMI = TEXT("CLSID\\{76A64158-CB41-11D1-8B02-00600806D9B6}");
LPCTSTR REG_SCRIPTING_FULL = TEXT("CLASSES_ROOT\\CLSID\\{72C24DD5-D70A-438B-8A42-98424B88AFB8}");
LPCTSTR REG_WMI_FULL = TEXT("CLASSES_ROOT\\CLSID\\{76A64158-CB41-11D1-8B02-00600806D9B6}");

LPCTSTR REG_SCRIPTING_WOW = TEXT("Wow6432Node\\CLSID\\{72C24DD5-D70A-438B-8A42-98424B88AFB8}");
LPCTSTR REG_WMI_WOW = TEXT("Wow6432Node\\CLSID\\{76A64158-CB41-11D1-8B02-00600806D9B6}");
BOOL verbose = false;

void setVerbose() {
	verbose = TRUE;
}

bool is64()
{
	SYSTEM_INFO si;

	GetNativeSystemInfo(&si);

	if (si.wProcessorArchitecture && PROCESSOR_ARCHITECTURE_INTEL)
		return false;
	else
		return true;
}

BOOL TakeOwnership(LPTSTR key) 
{
	bool forceOwnershipChange = true;
	BOOL bRetval = FALSE;
	HANDLE hToken = NULL; 
	PSID pSIDAdmin = NULL;
	PSID pSIDEveryone = NULL;
	PACL pACL = NULL;
	SID_IDENTIFIER_AUTHORITY SIDAuthWorld =
		SECURITY_WORLD_SID_AUTHORITY;
	SID_IDENTIFIER_AUTHORITY SIDAuthNT = SECURITY_NT_AUTHORITY;
	const int NUM_ACES  = 2;
	EXPLICIT_ACCESS ea[NUM_ACES];
	DWORD dwRes;

	// Specify the DACL to use.
	// Create a SID for the Everyone group.
	if (!AllocateAndInitializeSid(&SIDAuthWorld, 1,
		SECURITY_WORLD_RID,
		0,
		0, 0, 0, 0, 0, 0,
		&pSIDEveryone)) 
	{
		wprintf(L"AllocateAndInitializeSid (Everyone) error %u\n",
			GetLastError());
		
goto Cleanup;
	}

	// Create a SID for the BUILTIN\Administrators group.
	if (!AllocateAndInitializeSid(&SIDAuthNT, 2,
		SECURITY_BUILTIN_DOMAIN_RID,
		DOMAIN_ALIAS_RID_ADMINS,
		0, 0, 0, 0, 0, 0,
		&pSIDAdmin)) 
	{
		wprintf(L"AllocateAndInitializeSid (Admin) error %u\n",
			GetLastError());
		goto Cleanup;
	}

	ZeroMemory(&ea, NUM_ACES * sizeof(EXPLICIT_ACCESS));



	// Set read access for Everyone.

	ea[0].grfAccessPermissions = GENERIC_READ;
	ea[0].grfAccessMode = SET_ACCESS;
	ea[0].grfInheritance = SUB_CONTAINERS_AND_OBJECTS_INHERIT;
	ea[0].Trustee.TrusteeForm = TRUSTEE_IS_SID;
	ea[0].Trustee.TrusteeType = TRUSTEE_IS_WELL_KNOWN_GROUP;
	ea[0].Trustee.ptstrName = (LPTSTR) pSIDEveryone;


	// Set full control for Administrators.
	//ea[1].grfAccessPermissions = GENERIC_READ | GENERIC_WRITE | GENERIC_EXECUTE;

	ea[1].grfAccessPermissions = SPECIFIC_RIGHTS_ALL | STANDARD_RIGHTS_ALL;
	ea[1].grfAccessMode = SET_ACCESS;
	ea[1].grfInheritance = SUB_CONTAINERS_AND_OBJECTS_INHERIT;
	ea[1].Trustee.TrusteeForm = TRUSTEE_IS_SID;
	ea[1].Trustee.TrusteeType = TRUSTEE_IS_GROUP;
	ea[1].Trustee.ptstrName = (LPTSTR) pSIDAdmin;

	DWORD err = 0;
	err = SetEntriesInAcl(2,  ea , NULL, &pACL);
	if (err != ERROR_SUCCESS)
	{
		printf("Failed SetEntriesInAcl\n");
		displayError(err, key);
		goto Cleanup;
	}


	// Try to modify the object's DACL.
	dwRes = SetNamedSecurityInfo(
		key,                 // name of the object
		SE_REGISTRY_KEY,              // type of object
		DACL_SECURITY_INFORMATION,   // change only the object's DACL
		NULL, NULL,                  // do not change owner or group
		pACL,                        // DACL specified
		NULL);                       // do not change SACL

	if (ERROR_SUCCESS == dwRes) 
	{
		if(verbose)
			printf("Successfully changed DACL\n");

		bRetval = TRUE;
		// No more processing needed.

		if(!forceOwnershipChange)
			goto Cleanup;
	}
	else if (dwRes != ERROR_ACCESS_DENIED)
	{
		printf("First SetNamedSecurityInfo call failed: %u\n",
			dwRes); 
		displayError(dwRes, key);
		goto Cleanup;
	}

	displayError(dwRes, key);
	// If the preceding call failed because access was denied, 
	// enable the SE_TAKE_OWNERSHIP_NAME privilege, create a SID for 
	// the Administrators group, take ownership of the object, and 
	// disable the privilege. Then try again to set the object's DACL.

	// Open a handle to the access token for the calling process.
	if (!OpenProcessToken(GetCurrentProcess(), 
		TOKEN_ADJUST_PRIVILEGES, 
		&hToken)) 
	{
		printf("OpenProcessToken failed: %u\n", GetLastError()); 
		goto Cleanup; 
	} 

	// Enable the SE_TAKE_OWNERSHIP_NAME privilege.
	if (!SetPrivilege(hToken, SE_TAKE_OWNERSHIP_NAME, TRUE)) 
	{
		printf("You must be logged on as Administrator.\n");
		goto Cleanup; 
	}

	// Set the owner in the object's security descriptor.
	dwRes = SetNamedSecurityInfo(
		key,                 // name of the object
		SE_REGISTRY_KEY,              // type of object
		OWNER_SECURITY_INFORMATION,  // change only the object's owner
		pSIDAdmin,                   // SID of Administrator group
		NULL,
		NULL,
		NULL); 

	if (dwRes != ERROR_SUCCESS) 
	{
		printf("Could not set owner. Error: %u\n", dwRes); 
		goto Cleanup;
	}

	// Disable the SE_TAKE_OWNERSHIP_NAME privilege.
	if (!SetPrivilege(hToken, SE_TAKE_OWNERSHIP_NAME, FALSE)) 
	{
		printf("Failed SetPrivilege call unexpectedly.\n");
		goto Cleanup;
	}


	// Try again to modify the object's DACL,
	// now that we are the owner.
	dwRes = SetNamedSecurityInfo(
		key,                 // name of the object
		SE_REGISTRY_KEY,              // type of object
		DACL_SECURITY_INFORMATION,   // change only the object's DACL
		NULL, NULL,                  // do not change owner or group
		pACL,                        // DACL specified
		NULL);                       // do not change SACL

	if (dwRes == ERROR_SUCCESS)
	{
		printf("Successfully changed DACL\n");
		bRetval = TRUE; 
	}
	else
	{
		printf("Second SetNamedSecurityInfo call failed: %u\n",
			dwRes); 
	}

Cleanup:

	if (pSIDAdmin)
		FreeSid(pSIDAdmin); 

	if (pSIDEveryone)
		FreeSid(pSIDEveryone); 

	if (pACL)
		LocalFree(pACL);

	if (hToken)
		CloseHandle(hToken);

	return bRetval;

}

static BOOL SetPrivilege(
	HANDLE hToken,          // access token handle
	LPCTSTR lpszPrivilege,  // name of privilege to enable/disable
	BOOL bEnablePrivilege   // to enable or disable privilege
	) 
{
	TOKEN_PRIVILEGES tp;
	LUID luid;

	if ( !LookupPrivilegeValue( 
		NULL,            // lookup privilege on local system
		lpszPrivilege,   // privilege to lookup 
		&luid ) )        // receives LUID of privilege
	{
		printf("LookupPrivilegeValue error: %u\n", GetLastError() ); 
		return FALSE; 
	}
	tp.PrivilegeCount = 1;
	tp.Privileges[0].Luid = luid;
	if (bEnablePrivilege)
		tp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;
	else
		tp.Privileges[0].Attributes = 0;

	// Enable the privilege or disable all privileges.

	if ( !AdjustTokenPrivileges(
		hToken, 
		FALSE, 
		&tp, 
		sizeof(TOKEN_PRIVILEGES), 
		(PTOKEN_PRIVILEGES) NULL, 
		(PDWORD) NULL) )
	{ 
		printf("AdjustTokenPrivileges error: %u\n", GetLastError() ); 
		return FALSE; 
	} 

	if (GetLastError() == ERROR_NOT_ALL_ASSIGNED)

	{
		printf("The token does not have the specified privilege. \n");
		return FALSE;
	} 

	return TRUE;
}

void displayError(DWORD dwError, LPCTSTR key)
{
	LPVOID lpvMessageBuffer;

	if(dwError == ERROR_SUCCESS)
		return;

	// can you believe this junk?
	if (!FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,
		NULL, dwError,
		MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), //The user default language
		(LPTSTR)&lpvMessageBuffer, 0, NULL))
	{
		wprintf (L"FormatMessage failed: 0x%x\n", GetLastError());
		return;
	}

	wprintf(L"key: %s, error num: %d, error msg: %s\n", key, dwError, (LPTSTR)lpvMessageBuffer);
	LocalFree(lpvMessageBuffer);
}

LPCTSTR getOwnerString(HKEY root, LPCTSTR subkey)
{
	SECURITY_INFORMATION setOwner = OWNER_SECURITY_INFORMATION;
	HKEY hkey;
	DWORD psdsize = 1;
	REGSAM sam = KEY_READ | KEY_ENUMERATE_SUB_KEYS;
	LPTSTR ownerSidString = NULL;

	LONG err = RegOpenKeyEx(root, subkey, 0, sam, &hkey);

	if(err == 0 && hkey != 0) {
		PSID owner;
		BOOL ownerDefaulted = 0;
		// first call gets the %$#@ size!
		RegGetKeySecurity(hkey, OWNER_SECURITY_INFORMATION, NULL, &psdsize); 
		PSECURITY_DESCRIPTOR psd = LocalAlloc(LMEM_FIXED, psdsize);
		RegGetKeySecurity(hkey, OWNER_SECURITY_INFORMATION, psd, &psdsize); 
		GetSecurityDescriptorOwner(psd, &owner, &ownerDefaulted);
		ConvertSidToStringSid(owner, &ownerSidString);
		LocalFree(psd);
	}
	RegCloseKey(hkey);
	return ownerSidString;
}

BOOL Equal(LPCTSTR s1, LPCTSTR s2) {
	return wcscmp(s1, s2) == 0;
}

