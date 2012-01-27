#pragma once
class Persona
{
public:
	Persona(WELL_KNOWN_SID_TYPE builtinSid);
	~Persona(void);
	PSID getSid();
	LPCTSTR getSidString();
private:
	LPTSTR				sidString;
	PSID				psid;
	DWORD				sidSize;
};


