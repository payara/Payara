#include "StdAfx.h"
#include "Persona.h"

// WinBuiltinAdministratorsSid

Persona::Persona(WELL_KNOWN_SID_TYPE builtinSid)
{
	sidSize = SECURITY_MAX_SID_SIZE;
	psid = (SID*)LocalAlloc(LMEM_FIXED, sidSize);
	CreateWellKnownSid(builtinSid, NULL, psid, &sidSize);
	ConvertSidToStringSid(psid, &sidString);
}



Persona::~Persona(void)
{
	LocalFree(sidString);
	LocalFree(psid);
}
PSID Persona::getSid() {
	return psid;
}

LPCTSTR Persona::getSidString() {
	return sidString;
}
