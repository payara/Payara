#!/bin/python3
from playwright.sync_api import Page, expect

def create_instance(page: Page, name_instance):
	# Open the admin page 
	page.goto('http://localhost:4848')
	page.wait_for_selector('div[id="treeForm:tree_children"]')
	
	# Click on the option Instances in the menu
	instance_button = page.get_by_role("link",name="Instances", exact=True)
	instance_button.click()
	page.wait_for_selector('input[value=" Save "]')

	# Expect the title to contain Instances
	expect(page).to_have_title("Payara Server Instances")

	# Create new instance
	page.get_by_role("Button", name="New...").click()
	page.wait_for_selector('input[id="propertyForm:propertySheet:propertSectionTextField:NameTextProp:NameText"]')
	page.get_by_role("textbox").fill(name_instance)
	page.get_by_role("Button", name="OK").click()
	page.wait_for_selector('input[value=" Save "]')

	# Check for the presence of the new instance in the table
	instance_link = page.get_by_role("link", name=name_instance, exact=True)
	expect(instance_link).to_be_visible()

	# return to homepage
	page.goto('http://localhost:4848')

def start_instance(page: Page, name_instance):
	# Open the admin page 
	page.goto('http://localhost:4848')
	page.wait_for_selector('div[id="treeForm:tree_children"]')

	# Click on the option Instances in the menu
	instance_button = page.get_by_role("link",name="Instances", exact=True)
	instance_button.click()
	page.wait_for_selector('input[value=" Save "]')
	
	# Open the page of the instance to start it
	instance_link = page.get_by_role("link", name=name_instance, exact=True)
	instance_link.click()
	page.wait_for_selector('div[id="propertyForm:propertyContentPage"]')

	# Confirm the dialog window
	page.on("dialog", lambda dialog: dialog.accept())

	start_button = page.locator('input[value="Start"]')
	start_button.click()

	# wait for modal to appear and disappear
	page.wait_for_selector('input[value="Processing..."]')
	page.wait_for_selector('div#ajaxPanelBody')
	page.wait_for_selector('input[value="Processing..."]', timeout=1200000,state="hidden")
	page.wait_for_selector('div#ajaxPanelBody', timeout=600000,state="hidden")

	# return to homepage
	page.goto('http://localhost:4848')
	
def start_instances(page: Page):
	# Open the admin page 
	page.goto('http://localhost:4848')
	page.wait_for_selector('div[id="treeForm:tree_children"]')

	# Click on the option Instances in the menu
	instance_button = page.get_by_role("link",name="Instances", exact=True)
	instance_button.click()
	page.wait_for_selector('input[value=" Save "]')
	
	# Select all instances and start
	select_all_button = page.get_by_role("link",name="Select All", exact=True)
	select_all_button.click()

	# Confirm the dialog window
	page.on("dialog", lambda dialog: dialog.accept())

	start_button = page.locator('input[value="Start"]')
	start_button.click()

	# wait for modal to appear and disappear
	page.wait_for_selector('input[value="Processing..."]')
	page.wait_for_selector('div#ajaxPanelBody')
	page.wait_for_selector('input[value="Processing..."]', timeout=1200000,state="hidden")
	page.wait_for_selector('div#ajaxPanelBody', timeout=600000,state="hidden")

	# return to homepage
	page.goto('http://localhost:4848')

def stop_instances(page: Page):
	# Open the admin page 
	page.goto('http://localhost:4848')
	page.wait_for_selector('div[id="treeForm:tree_children"]')

	# Click on the option Instances in the menu
	instance_button = page.get_by_role("link",name="Instances", exact=True)
	instance_button.click()
	page.wait_for_selector('input[value=" Save "]')

	# Confirm the dialog window
	page.on("dialog", lambda dialog: dialog.accept())

	# Select all instances and Stop
	select_all_button = page.get_by_role("link",name="Select All", exact=True)
	select_all_button.click()
	stop_button = page.get_by_role("button",name=" Stop ")
	stop_button.click()
	# wait for modal to appear and disappear
	page.wait_for_selector('input[value="Processing..."]')
	page.wait_for_selector('div#ajaxPanelBody')
	page.wait_for_selector('input[value="Processing..."]', timeout=1200000,state="hidden")
	page.wait_for_selector('div#ajaxPanelBody', timeout=600000,state="hidden")

def delete_instances(page: Page):
	# Open the admin page 
	page.goto('http://localhost:4848')
	page.wait_for_selector('div[id="treeForm:tree_children"]')
	
	# Click on the option Instances in the menu
	instance_button = page.get_by_role("link",name="Instances", exact=True)
	instance_button.click()
	page.wait_for_selector('input[value=" Save "]')
	
	# Delete all instances
	select_all_button = page.get_by_role("link",name="Select All", exact=True)
	select_all_button.click()
	delete_button = page.get_by_role("button",name="Delete")
	delete_button.click()
	# wait for modal to appear and disappear
	page.wait_for_selector('input[value="Processing..."]')
	#page.wait_for_selector('div#ajaxPanelBody')
	page.wait_for_selector('input[value="Processing..."]', timeout=1200000,state="hidden")
	page.wait_for_selector('div#ajaxPanelBody', timeout=1200000,state="hidden")

	# return to homepage
	page.goto('http://localhost:4848')

def collect_logs(page: Page, name_instance, log_levels):
	logs = [name_instance + " : \n"]
	# Open the admin page 
	page.goto('http://localhost:4848')
	page.wait_for_selector('div[id="treeForm:tree_children"]')
	
	# Click on the option Instances in the menu
	instance_button = page.get_by_role("link",name="Instances", exact=True)
	instance_button.click()
	page.wait_for_selector('input[value=" Save "]')
	
	# Open the page of the instance and open the logs
	instance_link = page.get_by_role("link", name=name_instance, exact=True)
	instance_link.click()
	page.wait_for_selector('div[id="propertyForm:propertyContentPage"]')
	view_log_button = page.locator('input[value="View Log Files"]')
	
	with page.context.expect_page() as log_page_event:
		view_log_button.click()

	log_page = log_page_event.value

	log_page.wait_for_load_state()
	log_page.wait_for_selector('div[id="propertyForm:basicTable"]')

	log_level_combobox = log_page.locator('select[id="propertyForm:propertyContentPage:propertySheet:propertSectionTextField:logLevelProp:logLevel"]')
	log_level_combobox.click();
	for log_level in log_levels:
		# Change the log level to the desired value and filter logs on that level
		log_page.get_by_label("Log Level:").select_option(value=log_level, force=True)
		search_button = log_page.locator('input[id="propertyForm:propertyContentPage:bottomButtons:searchButtonBottom"]')
		search_button.click()

		# create list of every details buttons displayed
		details_buttons = log_page.get_by_role("link", name="(details)").all()
		for details_button in details_buttons:
			with page.context.expect_page() as detail_page_event:
				details_button.click()
			log_detail_page = detail_page_event.value
			log_entry_level = log_detail_page.locator('span[id*="logLevel"]').text_content()
			log_entry_message = log_detail_page.locator('span[id*="completeMessage"]').text_content()
			logs.append(log_entry_level + " - " + log_entry_message + " \n")
			log_detail_page.close()
	log_page.close()
	logs.append(" \n")
	logs = ' '.join(logs)
	return logs