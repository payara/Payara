#!/bin/python3
import os
import subprocess
from pathlib import Path
import platform

global PAYARA_PATH_BIN



def start_domain():	
	current_path = os.getcwd()
	print("Current path: "+current_path)
	print("Changing path to Payara: "+PAYARA_PATH_BIN)
	print("Going to Payara dir: '"+os.path.join(PAYARA_PATH_BIN, "bin")+"'")
	if not os.path.isdir(PAYARA_PATH_BIN):
		raise Exception("Unable to find Payara, used: '" + PAYARA_PATH_BIN + "', try to set PAYARA_HOME env. variable") 
	os.chdir(os.path.join(PAYARA_PATH_BIN, "bin"))
	asadmin_cmd = os.path.join('.', 'asadmin')
	if platform.system() == "Windows":
		asadmin_cmd = asadmin_cmd + ".bat"
	subprocess.run([asadmin_cmd , 'start-domain'])
	os.chdir(current_path)

def stop_domain():
	current_path = os.getcwd()
	if not os.path.isdir(PAYARA_PATH_BIN):
		raise Exception("Unable to find Payara, used: '" + PAYARA_PATH_BIN + "', try to set PAYARA_HOME env. variable") 
	os.chdir(os.path.join(PAYARA_PATH_BIN, "bin"))
	asadmin_cmd = os.path.join('.', 'asadmin')
	if platform.system() == "Windows":
		asadmin_cmd = asadmin_cmd + ".bat"
	print("Shutdown Payara")
	subprocess.run([asadmin_cmd, 'stop-domain'])
	os.chdir(current_path)
