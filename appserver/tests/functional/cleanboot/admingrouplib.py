#!/bin/python3
from playwright.sync_api import Page, expect


def create_group(page: Page, name_group, name_instances):
	# Open the admin page 
	page.goto('http://localhost:4848')
	page.wait_for_selector('div[id="treeForm:tree_children"]')
	
	# Click on the option Deployment Groups in the menu
	instance_button = page.get_by_role("link",name="Deployment Groups", exact=True)
	instance_button.click()
	page.wait_for_selector('div[id="propertyForm:dgTable"]')

	# Expect the title to contain Instances
	expect(page).to_have_title("Deployment Groups")

	# Create new instance
	page.get_by_role("Button", name="New...").click()
	page.wait_for_selector('input[id="propertyForm:propertySheet:propertySectionTextField:NameTextProp:NameText"]')
	page.get_by_role("textbox").fill(name_group)
	page.get_by_role("Button", name="OK").click()
	page.wait_for_selector('input[value="New..."]')

	# Check for the presence of the new instance in the table
	group_link = page.get_by_role("link", name=name_group, exact=True)
	expect(group_link).to_be_visible()
	group_link.click()
	page.wait_for_selector('table.Tab1TblNew_sun4')

	# Create instances in the group
	for name_instance in name_instances:
		group_tabs = page.locator('table.Tab1TblNew_sun4')
		group_instance_tab = group_tabs.get_by_role("link", name="Instances", exact=True)
		group_instance_tab.click()

		# Create new instance
		page.get_by_role("Button", name="New...").click()
		page.wait_for_selector('input[id="propertyForm:propertySheet:propertSectionTextField:NameTextProp:NameText"]')
		page.get_by_role("textbox").fill(name_instance)
		page.get_by_role("Button", name="OK").click()
		page.wait_for_selector('input[value=" Save "]')

		# Check for the presence of the new instance in the table
		instance_link = page.get_by_role("link", name=name_instance, exact=True)
		expect(instance_link).to_be_visible()

		group_general_tab = group_tabs.get_by_role("link", name="General", exact=True)
		group_general_tab.click()


	# return to homepage
	page.goto('http://localhost:4848')

def start_groups(page: Page):
	# Open the admin page 
	page.goto('http://localhost:4848')
	page.wait_for_selector('div[id="treeForm:tree_children"]')
	
	# Click on the option Deployment Groups in the menu
	instance_button = page.get_by_role("link",name="Deployment Groups", exact=True)
	instance_button.click()
	page.wait_for_selector('div[id="propertyForm:dgTable"]')

	# Expect the title to contain Instances
	expect(page).to_have_title("Deployment Groups")

	# Confirm the dialog window
	page.on("dialog", lambda dialog: dialog.accept())

	select_all_button = page.get_by_role("link",name="Select All", exact=True)
	select_all_button.click()
	start_button = page.get_by_role("button",name="Start Deployment Group")
	start_button.click()

	# wait for modal to appear and disappear
	page.wait_for_selector('input[value="Processing..."]')
	page.wait_for_selector('div#ajaxPanelBody')
	page.wait_for_selector('input[value="Processing..."]', timeout=1200000,state="hidden")
	page.wait_for_selector('div#ajaxPanelBody', timeout=1200000,state="hidden")

	# return to homepage
	page.goto('http://localhost:4848')

def stop_groups(page: Page):
	# Open the admin page 
	page.goto('http://localhost:4848')
	page.wait_for_selector('div[id="treeForm:tree_children"]')

	# Click on the option Deployment Groups in the menu
	instance_button = page.get_by_role("link",name="Deployment Groups", exact=True)
	instance_button.click()
	page.wait_for_selector('div[id="propertyForm:dgTable"]')

	# Confirm the dialog window
	page.on("dialog", lambda dialog: dialog.accept())

	# Select all groups and Stop
	select_all_button = page.get_by_role("link",name="Select All", exact=True)
	select_all_button.click()
	stop_button = page.get_by_role("button",name="Stop Deployment Group")
	stop_button.click()
	# wait for modal to appear and disappear
	page.wait_for_selector('input[value="Processing..."]')
	page.wait_for_selector('input[value="Processing..."]', timeout=1200000,state="hidden")
	page.wait_for_selector('div#ajaxPanelBody', timeout=1200000,state="hidden")

def delete_groups(page: Page):
	# Open the admin page 
	page.goto('http://localhost:4848')
	page.wait_for_selector('div[id="treeForm:tree_children"]')
	
	# Click on the option Deployment Groups in the menu
	instance_button = page.get_by_role("link",name="Deployment Groups", exact=True)
	instance_button.click()
	page.wait_for_selector('div[id="propertyForm:dgTable"]')
	
	# Delete all groups
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