#!/usr/bin/env python3

from pathlib import Path
from os import path
import os
import sys
from subprocess import PIPE,call, check_call, CalledProcessError,Popen
import argparse
import shutil
import re

ASADMIN_PATH="./asadmin"
WEB_ROOT_PATH=Path("/tmp/payara_le_war")
LE_LIVE_PATH=Path("/etc/letsencrypt/live/")
APP_NAME="le"
FNULL = open(os.devnull, 'w')
OKGREEN = '\033[92m'
WARNING = '\033[93m'
FAIL = '\033[91m'
ENDC = '\033[0m'

def kill_process():
	pass

def make_output_dir(dir_path) -> Path:
	dir_path.mkdir(exist_ok=True, parents=True)

def create_le_war():
	make_output_dir(WEB_ROOT_PATH / "WEB-INF")

def restart_listener(listener_name):
	check_call([ASADMIN_PATH, "set", "server.network-config.network-listeners.network-listener.%s.enabled=false" % listener_name])
	check_call([ASADMIN_PATH, "set", "server.network-config.network-listeners.network-listener.%s.enabled=true" % listener_name])

def upload_keypair(key_path, cert_path, alias, gf_domain_name, gf_domain_dir=None):
	print("Uploading keypair using asadmin: ", end='')
	try:
		if gf_domain_dir is None:
			check_call([ASADMIN_PATH, "add-pkcs8", "--domain_name", gf_domain_name, "--destalias", alias, "--priv-key-path", key_path, "--cert-chain-path", cert_path], stdout=FNULL, stderr=FNULL)
		else:
			check_call([ASADMIN_PATH, "add-pkcs8", "--domain_name", gf_domain_name, "--domaindir", gf_domain_dir, "--destalias", alias, "--priv-key-path", key_path, "--cert-chain-path", cert_path], stdout=FNULL, stderr=FNULL)
	except CalledProcessError: 
		print('[' + FAIL + "FAIL" + ENDC + ']' + "\n", sys.exc_info()[1])
		return 1

	print('[' + OKGREEN + " OK " + ENDC + ']')
	return 0

def configure_listener_alias(listener_name, alias):
	check_call([ASADMIN_PATH, "set", "configs.config.server-config.network-config.protocols.protocol.%s.ssl.cert-nickname=%s" % (listener_name, alias)])

def deploy_war():
	print("Attempting to deploy an EMPTY WAR to the ROOT context: ", end='')
	try:
		check_call([ASADMIN_PATH, "deploy", "--name", APP_NAME, "--force", "--contextroot", "/",  WEB_ROOT_PATH], stdout=FNULL, stderr=FNULL)
		print('[' + OKGREEN + " OK " + ENDC + ']')
	except CalledProcessError: 
		print('[' + FAIL + "FAIL" + ENDC + ']' + " Is the server up and running?\n", sys.exc_info()[1])
		shutil.rmtree(WEB_ROOT_PATH)
		return 1

	return 0

def undeploy_war():
	print("Undeploying WAR, doing cleanup: ", end='')
	try:
		check_call([ASADMIN_PATH, "undeploy", APP_NAME], stdout=FNULL, stderr=FNULL)
		print('[' + OKGREEN + " OK " + ENDC + ']')
		shutil.rmtree(WEB_ROOT_PATH)
	except CalledProcessError: 
		print('[' + FAIL + "FAIL" + ENDC + ']' + " Is the server up and running?\n", sys.exc_info()[1])
		shutil.rmtree(WEB_ROOT_PATH)
		return 1

	return 0

def invoke_certbot(domain_names):
	certbot_call_args = ["certbot", "certonly", "--webroot", "-w", WEB_ROOT_PATH]
	for d in domain_names:
		certbot_call_args += ["-d", d]
	
	try:
		check_call(certbot_call_args)
		print('Calling certbot: [' + OKGREEN + " OK " + ENDC + ']')
	except CalledProcessError: 
		print(sys.exc_info()[1], '\nCalling certbot: [' + FAIL + "FAIL" + ENDC +  ']')
		return 1

	return 0

def check_http_port():
	proc = Popen([ASADMIN_PATH, "get", "server.network-config.network-listeners.network-listener.*.port"], stdout=PIPE)
	ports = proc.stdout.read()
	if not '.port=80\n'.encode() in ports:
		print(WARNING + "WARNING: " + ENDC + "None of the listeners of Payara are running on the standard HTTP port (80).\nUnless there is a port mapping, "
			"a reverse-proxy exposing port 80 or other solution making the deployed web-app visible through port 80, the following invocation of certbot will "
			"likely fail (due to a failing 'HTTP Challenge' of the ACME protocol; see Chapter 8.3 on https://ietf-wg-acme.github.io/acme/draft-ietf-acme-acme.html). "
			"Proceeding nevertheless.")


if __name__=="__main__":
	parser = argparse.ArgumentParser(description="Payara Let's Encrypt integration script. This script deploys an empty WAR, calls (and requires) certbot "
		"to retrieve your certificate and uploads the certificate to the default keystore of the Payara domain. In addition, this script configures the listener "
		"with the given alias and restarts it (the listener only, not the whole domain), so that the new certificate is effective. Afterwards the WAR is undeployed. "
		"CertBot requires root, and as a consequence, so does this script. NOTE: In order for the web-challenge to be successful, the deployed application must be "
		"visible through the standard HTTP port (80) of the provided certification domain name (see parameter --cert-domain below).")
	parser.add_argument('-c','--cert-domain', action="append", help="The FQDN of the domain(s) the certificate will be bound too. You may use this arg multiple times.", required=True)
	parser.add_argument('-n','--name', help="The name of the payara-domain where the certificate will be uploaded.", required=False, default='production')
	parser.add_argument('-d','--domain-dir', '--domaindir', help="The directory where payara domains are defined. Necessary to provide only when the domains are in non-standard locations.", required=False)
	parser.add_argument('-l','--listener', help="HTTP Listener's name. By default http-listener-2", required=False, default="http-listener-2")
	parser.add_argument('-a','--alias', help="The alias that is used to import the keypar and the listener will be configured to use this alias. "
		"The default is constructed like: 'le_{listener}'", required=False)
	
	args = parser.parse_args()
	alias = "le_" + args.cert_domain[0] if args.alias is None else args.alias
	create_le_war()
	if deploy_war() != 0:
		exit(1)

	check_http_port()
	code = invoke_certbot(args.cert_domain)
	undeploy_war()

	if code != 0:
		exit(code)

	key_path = LE_LIVE_PATH / args.cert_domain[0] / "privkey.pem"
	cert_path = LE_LIVE_PATH / args.cert_domain[0] / "fullchain.pem"
	code = upload_keypair(key_path, cert_path, alias, args.name, args.domain_dir)
	if code == 0:
		code = configure_listener_alias(alias)
		if code == 0:
			restart_listener(args.listener)
	
	exit(code)
	