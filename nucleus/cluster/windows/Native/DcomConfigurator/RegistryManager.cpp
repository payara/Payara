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
// RegistryManager.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include "RegUtils.h"
#include "RegItem.h"
#include "Persona.h"

#define MAX_KEY_LENGTH 255
#define MAX_VALUE_NAME 16383

// messy but fast.  Need to start using a C++ String class...

static LPCTSTR REG_SCRIPTING = TEXT("CLSID\\{72C24DD5-D70A-438B-8A42-98424B88AFB8}");
static LPCTSTR REG_WMI = TEXT("CLSID\\{76A64158-CB41-11D1-8B02-00600806D9B6}");
static LPCTSTR REG_SCRIPTING_FULL = TEXT("CLASSES_ROOT\\CLSID\\{72C24DD5-D70A-438B-8A42-98424B88AFB8}");
static LPCTSTR REG_WMI_FULL = TEXT("CLASSES_ROOT\\CLSID\\{76A64158-CB41-11D1-8B02-00600806D9B6}");

//static LPCTSTR REG_SCRIPTING_FULL = TEXT("MACHINE\\SOFTWARE\\Classes\\CLSID\\{72C24DD5-D70A-438B-8A42-98424B88AFB8}");
//static LPCTSTR REG_WMI_FULL = TEXT("MACHINE\\SOFTWARE\\Classes\\CLSID\\{76A64158-CB41-11D1-8B02-00600806D9B6}");

void QueryKey(HKEY hKey);
BOOL TakeOwnership(LPTSTR lpszOwnFile);
BOOL TakeOwnership(LPTSTR key, bool noReflection) ;
BOOL TakeOwnershipNoRelection(LPTSTR key) ;

BOOL is64()
{
	SYSTEM_INFO si;

	GetNativeSystemInfo(&si);

	if (si.wProcessorArchitecture && PROCESSOR_ARCHITECTURE_INTEL)
		return FALSE;
	else
		return TRUE;
}

/*
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
*/

void junk() 
{

}

void readKeys(LPCTSTR *keys, int numKeys, REGSAM sam)
{
	//RegSetKeySecurity(NULL,NULL,NULL);
	SECURITY_INFORMATION setOwner = OWNER_SECURITY_INFORMATION;
	HKEY hkey;
	DWORD psdsize = 1;
	sam |= KEY_ENUMERATE_SUB_KEYS;

	for(int i = 0; i < numKeys; i++) {
		LONG err = RegOpenKeyEx(HKEY_CLASSES_ROOT, keys[i], 0, sam, &hkey);

		if(err == 0 && hkey != 0) {
			PSID owner;
			BOOL ownerDefaulted;
			LPTSTR ownerSidString;
			// first call gets the %$#@ size!
			RegGetKeySecurity(hkey, OWNER_SECURITY_INFORMATION, NULL, &psdsize); 



			PSECURITY_DESCRIPTOR psd = LocalAlloc(LMEM_FIXED, psdsize);
			RegGetKeySecurity(hkey, OWNER_SECURITY_INFORMATION, psd, &psdsize); 
			GetSecurityDescriptorOwner(psd, &owner, &ownerDefaulted);
			ConvertSidToStringSid(owner, &ownerSidString);
			wprintf(TEXT("Key = %s, SID of owner = %s\n"), keys[i], ownerSidString);


			//SetSecurityDescriptorOwner(psd, 

			//RegSetKeySecurity(hkey, setOwner, psd);

			//SetSecurityDescriptorOwner


			LocalFree(psd);
		}
		else
			displayError(err, keys[i]);
	}
}


void takeOwnership(HKEY root, LPCTSTR subkey, Persona &admin)
{
	//RegSetKeySecurity(NULL,NULL,NULL);
	SECURITY_INFORMATION setOwner = OWNER_SECURITY_INFORMATION;
	HKEY hkey;
	DWORD psdsize = 1;

	LONG err = RegOpenKeyEx(root, subkey, 0, KEY_ALL_ACCESS /*KEY_READ*/, &hkey);

	if(err == 0 && hkey != 0) {
		BOOL ownerDefaulted = 0;

		// first call gets the %$#@ size!
		RegGetKeySecurity(hkey, OWNER_SECURITY_INFORMATION, NULL, &psdsize); 

		PSECURITY_DESCRIPTOR psd = LocalAlloc(LMEM_FIXED, psdsize);
		RegGetKeySecurity(hkey, OWNER_SECURITY_INFORMATION, psd, &psdsize); 
		SetSecurityDescriptorOwner(psd, admin.getSid(), 0);
		RegSetKeySecurity(hkey, setOwner, psd);
		LocalFree(psd);
	}
	else
		displayError(err, subkey);
}

LPCTSTR getOwnerString(HKEY root, LPCTSTR subkey)
{
	SECURITY_INFORMATION setOwner = OWNER_SECURITY_INFORMATION;
	HKEY hkey;
	DWORD psdsize = 1;
	REGSAM sam = KEY_READ | KEY_ENUMERATE_SUB_KEYS;

	LONG err = RegOpenKeyEx(root, subkey, 0, sam, &hkey);

	if(err == 0 && hkey != 0) {
		PSID owner;
		BOOL ownerDefaulted = 0;
		LPTSTR ownerSidString;
		// first call gets the %$#@ size!
		RegGetKeySecurity(hkey, OWNER_SECURITY_INFORMATION, NULL, &psdsize); 
		PSECURITY_DESCRIPTOR psd = LocalAlloc(LMEM_FIXED, psdsize);
		RegGetKeySecurity(hkey, OWNER_SECURITY_INFORMATION, psd, &psdsize); 
		GetSecurityDescriptorOwner(psd, &owner, &ownerDefaulted);
		ConvertSidToStringSid(owner, &ownerSidString);
		return ownerSidString;
		LocalFree(psd);
	}
	else
		return NULL;
}

void enumKeys(HKEY hkeyparent, LPCTSTR subkey) 
{
	//TCHAR    subname[MAX_KEY_LENGTH];   // buffer for subkey name
	int i = 0;
	HKEY hkey;
	DWORD psdsize = 1;
	REGSAM sam = KEY_READ | KEY_ENUMERATE_SUB_KEYS;
	//FILETIME ftLastWriteTime;      // last write time 

	LONG err = RegOpenKeyEx(hkeyparent, subkey, 0, sam, &hkey);

	if(err != 0 || hkey == 0)
		printf("ERROR in enumKeys()");

	QueryKey(hkey);

	/*
	do 
	{
	cbName = MAX_KEY_LENGTH;
	long result = RegEnumKeyEx(hkey, i, subname, &cbName, NULL, NULL, NULL, &ftLastWriteTime);

	if(result == ERROR_NO_MORE_ITEMS) 
	break;

	if(result == ERROR_SUCCESS)
	wprintf(L"ENUM==> name: %s\n", subname); 
	else
	printf("Error enumming\n");
	}
	while(++i > 0);
	*/
}


void QueryKey(HKEY hKey) 
{ 
	TCHAR    achKey[MAX_KEY_LENGTH];   // buffer for subkey name
	DWORD    cbName;                   // size of name string 
	TCHAR    achClass[MAX_PATH] = TEXT("");  // buffer for class name 
	DWORD    cchClassName = MAX_PATH;  // size of class string 
	DWORD    cSubKeys=0;               // number of subkeys 
	DWORD    cbMaxSubKey;              // longest subkey size 
	DWORD    cchMaxClass;              // longest class string 
	DWORD    cValues;              // number of values for key 
	DWORD    cchMaxValue;          // longest value name 
	DWORD    cbMaxValueData;       // longest value data 
	DWORD    cbSecurityDescriptor; // size of security descriptor 
	FILETIME ftLastWriteTime;      // last write time 

	DWORD i, retCode; 

	TCHAR  achValue[MAX_VALUE_NAME]; 
	DWORD cchValue = MAX_VALUE_NAME; 

	// Get the class name and the value count. 
	retCode = RegQueryInfoKey(
		hKey,                    // key handle 
		achClass,                // buffer for class name 
		&cchClassName,           // size of class string 
		NULL,                    // reserved 
		&cSubKeys,               // number of subkeys 
		&cbMaxSubKey,            // longest subkey size 
		&cchMaxClass,            // longest class string 
		&cValues,                // number of values for this key 
		&cchMaxValue,            // longest value name 
		&cbMaxValueData,         // longest value data 
		&cbSecurityDescriptor,   // security descriptor 
		&ftLastWriteTime);       // last write time 

	// Enumerate the subkeys, until RegEnumKeyEx fails.

	if (cSubKeys)
	{
		printf( "\nNumber of subkeys: %d\n", cSubKeys);

		for (i=0; i<cSubKeys; i++) 
		{ 
			cbName = MAX_KEY_LENGTH;
			retCode = RegEnumKeyEx(hKey, i,
				achKey, 
				&cbName, 
				NULL, 
				NULL, 
				NULL, 
				&ftLastWriteTime); 
			if (retCode == ERROR_SUCCESS) 
			{
				_tprintf(TEXT("(%d) %s\n"), i+1, achKey);
			}
		}
	} 

	// Enumerate the key values. 

	if (cValues) 
	{
		printf( "\nNumber of values: %d\n", cValues);

		for (i=0, retCode=ERROR_SUCCESS; i<cValues; i++) 
		{ 
			cchValue = MAX_VALUE_NAME; 
			achValue[0] = '\0'; 
			retCode = RegEnumValue(hKey, i, 
				achValue, 
				&cchValue, 
				NULL, 
				NULL,
				NULL,
				NULL);

			if (retCode == ERROR_SUCCESS ) 
			{ 
				_tprintf(TEXT("(%d) %s\n"), i+1, achValue); 
			} 
		}
	}
}

void foo();

int main1()
{

	//foo();
	//return 0;



	//RegSetKeySecurity(NULL,NULL,NULL);
	Persona admin(WinBuiltinAdministratorsSid);
	//HKEY hkey;
	//DWORD psdsize = 1;
	LPCTSTR keys[2];
	int num = sizeof(keys) / sizeof(*keys);

	keys[0] =  REG_SCRIPTING;
	keys[1] =  REG_WMI;

	wprintf(L"Administrators group SID: [%s]\n", admin.getSidString());
	wprintf(L"Key: [%s]  Owner: [%s]\n", REG_SCRIPTING, getOwnerString(HKEY_CLASSES_ROOT, REG_SCRIPTING));
	wprintf(L"Key: [%s]  Owner: [%s]\n", REG_WMI, getOwnerString(HKEY_CLASSES_ROOT, REG_WMI));

	TakeOwnership((LPTSTR)REG_SCRIPTING_FULL);
	TakeOwnership((LPTSTR)REG_WMI_FULL);
	TakeOwnershipNoRelection((LPTSTR)REG_WMI_FULL);

	//TakeOwnership(L"HKEY_CLASSES_ROOT\\CLSID\\{730F6CDC-2C86-11D2-8773-92E220524153}");


	TakeOwnership(L"HKEY_CLASSES_ROOT\\CLSID\\AAAAA");
	TakeOwnershipNoRelection(L"HKEY_CLASSES_ROOT\\CLSID\\AAAAA");
	TakeOwnership(L"HKEY_LOCAL_MACHINE\\SOFTWARE\\Elf");
	return 0;
}

int main2()
{
	if(false){
		main1();
		return 0;
	}

	RegItem regScripting(HKEY_CLASSES_ROOT, REG_SCRIPTING);
	RegItem regWMI(HKEY_CLASSES_ROOT, REG_WMI);
	RegItem reg1(HKEY_CLASSES_ROOT, TEXT("Interface"));
	RegItem reg2(HKEY_CLASSES_ROOT, TEXT("Interface\\{766BF2AF-D650-11D1-9811-00C04FC31D2E}"));
	RegItem reg3(reg1.getHkey(), TEXT("{766BF2AF-D650-11D1-9811-00C04FC31D2E}"));
	RegItem reg4(HKEY_CLASSES_ROOT, TEXT("CLSID\\{0000002F-0000-0000-C000-000000000046}"));
	regScripting.dump();
	regWMI.dump();
	reg1.dump();
	reg2.dump();
	reg3.dump();
	reg4.dump();
	return 0;
}


void main3() {
	main1();
	setPermissions((LPTSTR)REG_WMI_FULL);
	setPermissions((LPTSTR)REG_SCRIPTING_FULL);
} 

int main(int argc, char* argv[])
{
	printf("------------------------------------\n");
	printf("---- DCOM Registry Configurator ----\n");
	printf("------------------------------------\n");
	printf("\n");
	printf("OS bits: %d\n", (is64() ? 64 : 32));

	main3();
}

void foo() {

	PACL pOldDACL = NULL; 
	PACL pNewDACL = NULL;
	PSECURITY_DESCRIPTOR pSD = NULL;

	DWORD dwRes = GetNamedSecurityInfo(REG_SCRIPTING_FULL, SE_REGISTRY_KEY,
		DACL_SECURITY_INFORMATION, 
		NULL, NULL, &pOldDACL, NULL, &pSD);

	if (ERROR_SUCCESS != dwRes) {
		printf( "GetNamedSecurityInfo Error %u\n", dwRes );
	}  

	if(pSD == NULL) {
		printf("coudn't get security descriptor");
		return;
	}

	/* useful for debugging
	SECURITY_DESCRIPTOR sd = *((SECURITY_DESCRIPTOR*) pSD);
	*/

}


// RegSetKeySecurity 
// RegGetKeySecurity 
// OWNER_SECURITY_INFORMATION
// SECURITY_INFORMATION 
// SetSecurityDescriptorOwner 
/***

When you call the RegOpenKeyEx function, the system checks the requested access rights against the key's security descriptor. If the user does not have the correct access to the registry key, the open operation fails. If an administrator needs access to the key, the solution is to enable the SE_TAKE_OWNERSHIP_NAME privilege and open the registry key with WRITE_OWNER access. For more information, see Enabling and Disabling Privileges.

You can request the ACCESS_SYSTEM_SECURITY access right to a registry key if you want to read or write the key's system access control list (SACL). For more information, see Access-Control Lists (ACLs) and SACL Access Right.

To view the current access rights for a key, including the predefined keys, use the Registry Editor (Regedt32.exe). After navigating to the desired key, go to the Edit menu and select Permissions. 
*/
