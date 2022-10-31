#!/bin/python3
import os
import subprocess
import time
import urllib.request

global PAYARA_PATH_BIN
global ATTEMPTS
ATTEMPTS = 30

# Test
TEST_PATH = os.getcwd()
print("Current path: "+TEST_PATH)

# title - simple textual description
# directory - directory of reproducer, this scripts build it by maven
# fileToDeploy - name of the app, used with extension to deploy, itself to undeploy
# extension - extension to the fileToDeploy
# verifications - list of pairs: url + text to check
# postbootcommands - additional commands to be stored in the postbootcommand file before the deploy
def doOneTest(title, directory, fileToDeploy, extension, verifications, postbootcommands):
	print("Build "+title)
	if directory is not None:
		os.chdir(directory)
		subprocess.run(['mvn', 'clean', 'package'])
		os.chdir("target")
	CDIReproducerTargetPath = os.getcwd()

	print("Prepare postbootcommandfile");
	os.chdir(TEST_PATH)
	postbootcommandfile = open("postboot.asadmin", "w")
	
	for postbootcommand in postbootcommands:
		postbootcommandfile.write(postbootcommand+"\n")

	if fileToDeploy is not None:
		postbootcommandfile.write("deploy "+CDIReproducerTargetPath+"/"+fileToDeploy+"."+extension+"\n")
	postbootcommandfile.close()

	print("Run Payara with postbootcommand file parameter")
	print("Search log for 'Reading in commands from .../postboot.asadmin' right after server start")
	os.chdir(PAYARA_PATH_BIN)
	subprocess.run(['./asadmin', 'start-domain', '--postbootcommandfile', TEST_PATH+'/postboot.asadmin'])

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
		while attempts < ATTEMPTS:
			error = None
			try:
				f = urllib.request.urlopen(url)
				pageContent = f.read().decode('utf-8')
				#print("Page content: >>"+pageContent+"<<")
			except:
				error = "ERROR: Unable to open the url: "+url

			if textToVerify in pageContent:
				print("OK: "+urlToDownload+" deployed and works")
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

	#input("Press enter to shutdown")

	if fileToDeploy is not None:
		print("Undeploying")
		subprocess.run(['./asadmin', 'undeploy', fileToDeploy])

	print("Shutdown Payara")
	subprocess.run(['./asadmin', 'stop-domain'])
	# cleanup
	os.chdir(TEST_PATH)
	os.remove("postboot.asadmin") 
	return testErrors
