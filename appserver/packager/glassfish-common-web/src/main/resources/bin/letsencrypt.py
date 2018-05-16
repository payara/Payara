#!/usr/bin/env python3

from pathlib import Path
from os import path
from subprocess import call, check_call, CalledProcessError
import argparse

ASADMIN_PATH="./asadmin"
WEB_ROOT_PATH=Path(path.expanduser("~")) / "payara_le_war"
LE_LIVE_PATH=Path("/etc/letsencrypt/live/")
APP_NAME="le"

def kill_process():
	pass

def make_output_dir(dir_path) -> Path:
	dir_path.mkdir(exist_ok=True, parents=True)

def create_le_war():
	make_output_dir(WEB_ROOT_PATH / "WEB-INF")

def restart_listener(listener_name):
	check_call([ASADMIN_PATH, "set", "server.network-config.network-listeners.network-listener.%s.enabled=false" % listener_name])
	check_call([ASADMIN_PATH, "set", "server.network-config.network-listeners.network-listener.%s.enabled=true" % listener_name])

def upload_keypair(key_path, cert_path, alias, gf_domain=None):
	check_call([ASADMIN_PATH, "add-pkcs8", "--destalias", alias, "--priv-key-path", key_path, "--cert-chain-path", cert_path])

def configure_listener_alias(listener_name, alias):
	check_call([ASADMIN_PATH, "set", "configs.config.server-config.network-config.protocols.protocol.%s.ssl.cert-nickname=%s" % (listener_name, alias)])

def update_cert(listener_name, alias, domains):
	# Glassfish must be running for this to work
	result_code = 0

	check_call([ASADMIN_PATH, "deploy", "--name", APP_NAME, "--force", "--contextroot", "/",  WEB_ROOT_PATH])
	certbot_call_args = ["certbot", "certonly", "--webroot", "-w", WEB_ROOT_PATH]
	for d in domains:
		certbot_call_args += ["-d", d]
	
	try:
		#check_call(certbot_call_args)

		# retrieve the keys/certificates produced by LE
		key_path = LE_LIVE_PATH / domains[0] / "privkey.pem"
		cert_path = LE_LIVE_PATH / domains[0] / "fullchain.pem"
		upload_keypair(key_path, cert_path, alias)
		configure_listener_alias(alias)
		restart_listener(args.listener)
	except CalledProcessError: 
		# certificate update failed
		result_code = 1

	check_call([ASADMIN_PATH, "undeploy", APP_NAME])
	return result_code


if __name__=="__main__":
	parser = argparse.ArgumentParser(description="Payara Let's Encrypt integration script. This script deploys an empty WAR, calls (and requires) certbot "
		"to retrieve your certificate and uploads the certificate to the default keystore of the Payara domain. In addition, this script configures the listener "
		"with the given alias and restarts it (the listener only, not the whole domain), so that the new certificate is effective. Afterwards the WAR is undeployed. "
		"CertBot requires root, and as a consequence, so does this script.")
	parser.add_argument('-d','--domain', action="append", help="The name of the domain the certificate will be bound too. You may use this arg multiple times", required=True)
	parser.add_argument('-l','--listener', help="HTTP Listener's name. By default http-listener-2", required=False, default="http-listener-2")
	parser.add_argument('-a','--alias', help="The alias that is used to import the keypar and the listener will be configured to use this alias. "
		"The default is constructed like: 'le_{listener}'", required=False)
	
	args = parser.parse_args()
	alias = "le_" + args.domain[0] if args.alias is None else args.alias
	create_le_war()
	code = update_cert(args.listener, alias, args.domain)
	exit(code)
	