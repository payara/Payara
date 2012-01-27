#pragma once
#include "Persona.h"
#include <string>

class DcomConfigurator
{
public:
	DcomConfigurator(int argc, _TCHAR* argv[]);
	~DcomConfigurator();
	void configure();
	inline bool isHelp() { return help; }
	void usage();
protected:
	bool test;
	bool help;
private:
	void printOwners();
	void printOwners(Persona& admin);
	void configureRegKeys();
	void DcomConfigurator::parse(int argc, _TCHAR* argv[]);
	bool verbose;
	bool force;
	LPCTSTR scriptingOwnerId;
	LPCTSTR wmiOwnerId;
	LPCTSTR adminOwnerId;
	Persona* admin;
	string message;
};


