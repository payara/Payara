#pragma once

extern LPCTSTR REG_SCRIPTING;
extern LPCTSTR REG_WMI;
extern LPCTSTR REG_SCRIPTING_FULL;
extern LPCTSTR REG_WMI_FULL;
extern LPCTSTR REG_SCRIPTING_WOW;
extern LPCTSTR REG_WMI_WOW;


extern bool is64();
extern void displayError(DWORD dwError, LPCTSTR key);
extern LPCTSTR getOwnerString(HKEY root, LPCTSTR subkey);
extern BOOL Equal(LPCTSTR s1, LPCTSTR s2);
extern BOOL SetPrivilege(HANDLE hToken, LPCTSTR lpszPrivilege, BOOL bEnablePrivilege);
BOOL TakeOwnership(LPTSTR key);
extern void setVerbose();