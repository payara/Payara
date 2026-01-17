#!/bin/python3
import os
import admingrouplib
import admininstancelib
import asadminlib
from playwright.sync_api import Page
from datetime import datetime


# Setup
asadminlib.PAYARA_PATH_BIN = os.path.abspath(os.environ["PAYARA_HOME"] if "PAYARA_HOME" in os.environ else "../../../../appserver/distributions/payara/target/stage/payara7")


def test_start_payara():
	asadminlib.start_domain()

def test_instance(page: Page):
	logs = str(datetime.now()) + " \n"
	admininstancelib.create_instance(page, "testInstance1")
	admininstancelib.start_instance(page, "testInstance1")
	logs += admininstancelib.collect_logs(page, "testInstance1", ["WARNING", "SEVERE", "ALERT", "EMERGENCY"])
	admininstancelib.stop_instances(page)
	admininstancelib.delete_instances(page)
	logs += str(datetime.now()) + " \n"
	file = open("resultsInstance.txt", "w")
	file.write(logs)
	file.close()
        
def test_deployment_group(page: Page):
	logs = str(datetime.now()) + " \n"
	admingrouplib.create_group(page, "group1", ["groupInstance1", "groupInstance2"])
	admingrouplib.start_groups(page)
	logs += admininstancelib.collect_logs(page, "groupInstance1", ["WARNING", "SEVERE", "ALERT", "EMERGENCY"])
	logs += admininstancelib.collect_logs(page, "groupInstance2", ["WARNING", "SEVERE", "ALERT", "EMERGENCY"])
	admingrouplib.stop_groups(page)
	admingrouplib.delete_groups(page)
	admininstancelib.delete_instances(page)
	logs += str(datetime.now()) + " \n"
	file = open("resultsGroup.txt", "w")
	file.write(logs)
	file.close()

def test_stop_payara():
	asadminlib.stop_domain()
