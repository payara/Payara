#!/bin/python3
import os
import subprocess
import time
import urllib.request
from pathlib import Path
import platform

global PAYARA_PATH_BIN
global ATTEMPTS
ATTEMPTS = 30

# Test
TEST_PATH = os.getcwd()
print("Current path: "+TEST_PATH)

def findMaven():
	mvnpath = "mvn"
	if platform.system() == "Windows":
		mvnpath = mvnpath + ".cmd"
	if "MAVEN_HOME" in os.environ:
		mvnpath = os.path.join(os.environ["MAVEN_HOME"], "bin", mvnpath)
	try:
		print("Trying to run Maven: '" + mvnpath + "'")
		subprocess.run([mvnpath, '-version'])
	except:
		raise Exception("Unable to run Maven, tried binary: "+mvnpath) 
	return mvnpath

# title - simple textual description
# directory - directory of reproducer, this scripts build it by maven
# fileToDeploy - name of the app, used with extension to deploy, itself to undeploy
# extension - extension to the fileToDeploy
# verifications - list of pairs: url + text to check
# postbootcommands - additional commands to be stored in the postbootcommand file before the deploy
def start_deploy_with_postboot(title, directory, fileToDeploy, extension, verifications, postbootcommands):
	if directory is not None:
		print("Build "+title)
		mvn_path = findMaven()
		os.chdir(directory)
		subprocess.run([mvn_path, 'clean', 'package'])
		os.chdir("target")
	CDIReproducerTargetPath = os.getcwd()

	print("Prepare postbootcommandfile");
	os.chdir(TEST_PATH)
	postbootcommandfile = open("postboot.asadmin", "w")
	
	for postbootcommand in postbootcommands:
		postbootcommandfile.write(postbootcommand+"\n")

	if fileToDeploy is not None:
		postbootcommandfile.write("deploy "+os.path.join(CDIReproducerTargetPath, fileToDeploy+"."+extension)+"\n")
	postbootcommandfile.close()

	print("Run Payara with postbootcommand file parameter")
	print("Search log for 'Reading in commands from .../postboot.asadmin' right after server start")
	print("Changing path to Payara: "+PAYARA_PATH_BIN)
	print("Going to Payara dir: '"+os.path.join(PAYARA_PATH_BIN, "bin")+"'")
	if not os.path.isdir(PAYARA_PATH_BIN):
		raise Exception("Unable to find Payara, used: '" + PAYARA_PATH_BIN + "', try to set PAYARA_HOME env. variable") 
	os.chdir(os.path.join(PAYARA_PATH_BIN, "bin"))
	asadmin_cmd = os.path.join('.', 'asadmin')
	if platform.system() == "Windows":
		asadmin_cmd = asadmin_cmd + ".bat"
	subprocess.run([asadmin_cmd , 'start-domain', '--postbootcommandfile', os.path.join(TEST_PATH, 'postboot.asadmin')])

	testErrors = 0;
	for verification in verifications:
		urlToDownload = verification[0]
		textToVerify = verification[1]
		url = urlToDownload
		if not url.startswith("http"):
			url = "http://localhost:8080/"+urlToDownload
		print("Trying to load url: "+url)
		pageContent = ""
		attempts = 1
		while True:
			error = None
			try:
				f = urllib.request.urlopen(url)
				pageContent = f.read().decode('utf-8')
				#print("Page content: >>"+pageContent+"<<")
			except:
				error = "ERROR: Unable to open the url: "+url

			if textToVerify in pageContent:
				print("OK: "+urlToDownload)
				break
			else:
				error = textToVerify + " not found in the output, was: "+pageContent
				
			if error is not None:
				if attempts<ATTEMPTS:
					print("Attempt #" + str(attempts) + ": " + error)
					attempts=attempts+1
					time.sleep(1) # wait a second before another attempt
				else:
					print("ERROR! " + error)
					# add error if all attempts failed
					testErrors += 1
					break

	#input("Press enter to shutdown")

	if fileToDeploy is not None:
		print("Undeploying")
		subprocess.run([asadmin_cmd, 'undeploy', fileToDeploy])

	print("Shutdown Payara")
	subprocess.run([asadmin_cmd, 'stop-domain'])
	# cleanup
	os.chdir(TEST_PATH)
	os.remove("postboot.asadmin") 
	return testErrors
