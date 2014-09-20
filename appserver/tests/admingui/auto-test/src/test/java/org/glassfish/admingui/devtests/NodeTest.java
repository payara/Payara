/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.admingui.devtests;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 
 * @author Jeremy Lv
 *
 */
public class NodeTest extends BaseSeleniumTestClass {

    private static final String ID_NODES_TABLE = "propertyForm:nodesTable";
    private static final String ID_NODEHOST_FIELD = "propertyForm:propertySheet:propertSectionTextField:NodeHost:NodeHost";
    private static final String ID_NODEDIRECTORY_FIELD = "propertyForm:propertySheet:propertSectionTextField:NodeHome:NodeHome";
    private static final String ID_NEW_NODE_BUTTON = "propertyForm:nodesTable:topActionsGroup1:newButton";
    private static final String ID_DELETE_NODE_BUTTON = "propertyForm:nodesTable:topActionsGroup1:button1";
    private static final String ID_INSTALLDIR_FIELD = "propertyForm:propertySheet:propertSectionTextField:installdir-ssh:installDirssh";
    private static final String ID_FORCE_FIELD = "propertyForm:propertySheet:sshConnectorSection:force:force";
    private static final String ID_CREATE_NAME_FIELD = "propertyForm:propertySheet:propertSectionTextField:nameProp:name";
    private static final String ID_CONFIG_INSTALLDIR_FIELD = "propertyForm:propertySheet:propertSectionTextField:installdir-config:installDirConfig";
    private static final String ID_TYPE_FIELD = "propertyForm:propertySheet:propertSectionTextField:typeProp:type";
    private static final String ID_CREATE_NODE_BUTTON = "propertyForm:propertyContentPage:topButtons:newButton";
    private static final String ID_STANDALONE_TREE_LINK = "treeForm:tree:standaloneTreeNode:standaloneTreeNode_link";

    private static final String ID_NODE_TREE_LINK = "treeForm:tree:nodeTreeNode:nodeTreeNode_link";


    private static final String NODE_NAME_PREFIX = "testNode-";
    private static final String PSWD_ALIAS_NAME_PREFIX = "pswdalias-";


    @Test
    public void testCreateAndDeleteSSHNodewithKeyFile() {
        gotoDasPage();
        final String nodeName = NODE_NAME_PREFIX + generateRandomString();

        clickAndWait(ID_NODE_TREE_LINK);
        clickAndWait(ID_NEW_NODE_BUTTON);

        isElementPresent(ID_CREATE_NAME_FIELD);
        setFieldValue(ID_CREATE_NAME_FIELD, nodeName);
        setFieldValue(ID_NODEHOST_FIELD, "NodeHOST");
        setFieldValue(ID_NODEDIRECTORY_FIELD, "NodeDirectory");
        //enterText(ID_INSTALLDIR_FIELD, "${com.sun.aas.productRoot}");
        if (!driver.findElement(By.id(ID_FORCE_FIELD)).isSelected()){
            clickByIdAction(ID_FORCE_FIELD);
        }
        setFieldValue("propertyForm:propertySheet:sshConnectorSection:sshport:sshport", "24");
        Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:sshConnectorSection:psSelected:psSelected")));
        select.selectByVisibleText("Key File");
        setFieldValue("propertyForm:propertySheet:sshConnectorSection:Keyfile:Keyfile", "/sshKeyFileLocation");
        clickAndWait(ID_CREATE_NODE_BUTTON);
        // Verify nodes information in table
        String prefix = getTableRowByValue(ID_NODES_TABLE, nodeName, "col1");
        assertEquals(nodeName, getText(prefix + "col1:link"));
        assertEquals("NodeHOST", getText(prefix + "col2"));
        assertEquals("SSH", getText(prefix + "colType"));

        //Verify the node is created with the value specified.
        
        String clickId = prefix + "col1:link";
        clickByIdAction(clickId);
        
        assertTrue(getText("propertyForm:propertySheet:propertSectionTextField:staticnameProp:IdStatic").equals(nodeName));

        Select select1 = new Select(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:typeProp:type")));
        assertTrue(select1.getFirstSelectedOption().getAttribute("value").equals("SSH"));
        assertEquals("NodeHOST", getValue(ID_NODEHOST_FIELD, "value"));
        assertEquals("NodeDirectory", getValue(ID_NODEDIRECTORY_FIELD, "value"));
        assertEquals("${com.sun.aas.productRoot}", getValue(ID_INSTALLDIR_FIELD, "value"));

        assertEquals("24", getValue("propertyForm:propertySheet:sshConnectorSection:sshport:sshport", "value"));
        assertEquals("/sshKeyFileLocation", getValue("propertyForm:propertySheet:sshConnectorSection:Keyfile:Keyfile", "value"));
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");

        //Test Delete Node
        deleteRow(ID_DELETE_NODE_BUTTON, "propertyForm:nodesTable", nodeName);
    }

    @Test
    public void testCreateAndDeleteSSHNodewithPassword() {
        gotoDasPage();
        final String nodeName = NODE_NAME_PREFIX + generateRandomString();

        clickAndWait(ID_NODE_TREE_LINK);
        clickAndWait(ID_NEW_NODE_BUTTON);

        setFieldValue(ID_CREATE_NAME_FIELD, nodeName);
        setFieldValue(ID_NODEHOST_FIELD, "NodeHOST2");
        setFieldValue(ID_NODEDIRECTORY_FIELD, "NodeDirectory2");
        setFieldValue(ID_INSTALLDIR_FIELD, "ProductRoot");
        if (!driver.findElement(By.id(ID_FORCE_FIELD)).isSelected()){
            clickByIdAction(ID_FORCE_FIELD);
        }
        setFieldValue("propertyForm:propertySheet:sshConnectorSection:sshport:sshport", "34");
        
        Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:sshConnectorSection:psSelected:psSelected")));
        select.selectByVisibleText("Password");

        setFieldValue("propertyForm:propertySheet:sshConnectorSection:newPasswordProp:NewPassword", "abcde");
//        setFieldValue("propertyForm:propertySheet:sshConnectorSection:confirmPasswordProp:ConfirmPassword", "abcde");
        clickAndWait(ID_CREATE_NODE_BUTTON);
        // Verify nodes information in table
        String prefix = getTableRowByValue(ID_NODES_TABLE, nodeName, "col1");
        assertEquals(nodeName, getText(prefix + "col1:link"));
        assertEquals("NodeHOST2", getText(prefix + "col2"));
        assertEquals("SSH", getText(prefix + "colType"));

        //Verify the node is created with the value specified.
        String clickId = prefix + "col1:link";
        clickByIdAction(clickId);
        
        assertTrue(getText("propertyForm:propertySheet:propertSectionTextField:staticnameProp:IdStatic").equals(nodeName));

        Select select1 = new Select(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:typeProp:type")));
        assertTrue(select1.getFirstSelectedOption().getAttribute("value").equals("SSH"));
        assertEquals("NodeHOST2", getValue(ID_NODEHOST_FIELD, "value"));
        assertEquals("NodeDirectory2", getValue(ID_NODEDIRECTORY_FIELD, "value"));
        assertEquals("ProductRoot", getValue(ID_INSTALLDIR_FIELD, "value"));

        assertEquals("34", getValue("propertyForm:propertySheet:sshConnectorSection:sshport:sshport", "value"));
        assertEquals("abcde", getValue("propertyForm:propertySheet:sshConnectorSection:newPasswordProp:NewPassword", "value"));
//        assertEquals("abcde", getValue("propertyForm:propertySheet:sshConnectorSection:confirmPasswordProp:ConfirmPassword", "value"));
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");

        //Test Delete Node
        deleteRow(ID_DELETE_NODE_BUTTON, "propertyForm:nodesTable", nodeName);
    }

    @Test
    public void testCreateAndDeleteSSHNodewithPswdAlias() {
        gotoDasPage();
        final String nodeName = NODE_NAME_PREFIX + generateRandomString();
        final String pswdAliasName = PSWD_ALIAS_NAME_PREFIX + generateRandomString();
        //create PasswordAlias
        clickAndWait("treeForm:tree:nodes:nodes_link");
        clickAndWait("propertyForm:domainTabs:pswdAliases");
        clickAndWait("propertyForm:aliases:topActionsGroup1:newButton");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:aliasNameNew:aliasNameNew", pswdAliasName);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:newPasswordProp:NewPassword", "abcde");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:confirmPasswordProp:ConfirmPassword", "abcde");
        clickAndWait(ID_CREATE_NODE_BUTTON);

        clickAndWait(ID_NODE_TREE_LINK);
        clickAndWait(ID_NEW_NODE_BUTTON);
        setFieldValue(ID_CREATE_NAME_FIELD, nodeName);
        setFieldValue(ID_NODEHOST_FIELD, "NodeHOST3");
        setFieldValue(ID_NODEDIRECTORY_FIELD, "NodeDirectory3");
        setFieldValue(ID_INSTALLDIR_FIELD, "ProductRoot3");
        if (!driver.findElement(By.id(ID_FORCE_FIELD)).isSelected()){
            clickByIdAction(ID_FORCE_FIELD);
        }
        setFieldValue("propertyForm:propertySheet:sshConnectorSection:sshport:sshport", "34");
        Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:sshConnectorSection:psSelected:psSelected")));
        select.selectByVisibleText("Password Alias");
        Select select1 = new Select(driver.findElement(By.id("propertyForm:propertySheet:sshConnectorSection:pswdAlias:pswdAlias")));
        select1.selectByVisibleText(pswdAliasName);
        clickAndWait(ID_CREATE_NODE_BUTTON);


        // Verify nodes information in table
        String prefix = getTableRowByValue(ID_NODES_TABLE, nodeName, "col1");
        assertEquals(nodeName, getText(prefix + "col1:link"));
        assertEquals("NodeHOST3", getText(prefix + "col2"));
        assertEquals("SSH", getText(prefix + "colType"));

        //Verify the node is created with the value specified.
        String clickId = prefix + "col1:link";
        clickByIdAction(clickId);

        assertTrue(getText("propertyForm:propertySheet:propertSectionTextField:staticnameProp:IdStatic").equals(nodeName));
        Select select2 = new Select(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:typeProp:type")));
        assertTrue(select2.getFirstSelectedOption().getAttribute("value").equals("SSH"));
        assertEquals("NodeHOST3", getValue(ID_NODEHOST_FIELD, "value"));
        assertEquals("NodeDirectory3", getValue(ID_NODEDIRECTORY_FIELD, "value"));
        assertEquals("ProductRoot3", getValue(ID_INSTALLDIR_FIELD, "value"));

        assertEquals("34", getValue("propertyForm:propertySheet:sshConnectorSection:sshport:sshport", "value"));
        Select select3 = new Select(driver.findElement(By.id("propertyForm:propertySheet:sshConnectorSection:psSelected:psSelected")));
        Select select4 = new Select(driver.findElement(By.id("propertyForm:propertySheet:sshConnectorSection:pswdAlias:pswdAlias")));
        assertTrue(select3.getFirstSelectedOption().getAttribute("value").equals("3"));
        assertTrue(select4.getFirstSelectedOption().getAttribute("value").equals(pswdAliasName));
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");

        //Test Delete Node
//        gotoDasPage();
//        clickAndWait("treeForm:tree:nodes:nodes_link");
        deleteRow(ID_DELETE_NODE_BUTTON, "propertyForm:nodesTable", nodeName);

        //Delete Pswd Alias created
        gotoDasPage();
        clickAndWait("treeForm:tree:nodes:nodes_link");
        clickAndWait("propertyForm:domainTabs:pswdAliases");
        String delId = getTableRowByValue("propertyForm:aliases", pswdAliasName, "col1")+"col0:select";
        clickByIdAction(delId);
        clickByIdAction("propertyForm:aliases:topActionsGroup1:button1");
        closeAlertAndGetItsText();
    }


    @Test
    public void testCreateAndDeleteCONFIGNodes() {
        gotoDasPage();
        final String nodeName = NODE_NAME_PREFIX + generateRandomString();

        clickAndWait(ID_NODE_TREE_LINK);
        clickAndWait(ID_NEW_NODE_BUTTON);

        Select select = new Select(driver.findElement(By.id(ID_TYPE_FIELD)));
        select.selectByVisibleText("CONFIG");

        setFieldValue(ID_CREATE_NAME_FIELD, nodeName);
        setFieldValue(ID_NODEHOST_FIELD, "NodeHOSTCC");
        setFieldValue(ID_NODEDIRECTORY_FIELD, "NodeDirectoryCC");
        setFieldValue(ID_CONFIG_INSTALLDIR_FIELD, "/ProductRoot");
        clickAndWait(ID_CREATE_NODE_BUTTON);

        // Verify nodes information in table
        String prefix = getTableRowByValue(ID_NODES_TABLE, nodeName, "col1");
        assertEquals(nodeName, getText(prefix + "col1:link"));
        assertEquals("NodeHOSTCC", getText(prefix + "col2"));
        assertEquals("CONFIG", getText(prefix + "colType"));

        //Verify the node is created with the value specified.
        String clickId = prefix + "col1:link";
        clickByIdAction(clickId);
        assertTrue(getText("propertyForm:propertySheet:propertSectionTextField:staticnameProp:IdStatic").equals(nodeName));
        Select select1 = new Select(driver.findElement(By.id(ID_TYPE_FIELD)));
        assertTrue(select1.getFirstSelectedOption().getAttribute("value").equals("CONFIG"));
        assertEquals("NodeHOSTCC", getValue(ID_NODEHOST_FIELD, "value"));
        assertEquals("NodeDirectoryCC", getValue(ID_NODEDIRECTORY_FIELD, "value"));
        assertEquals("/ProductRoot", getValue(ID_CONFIG_INSTALLDIR_FIELD, "value"));

        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");

        //Test Delete Node
        deleteRow(ID_DELETE_NODE_BUTTON, "propertyForm:nodesTable", nodeName);
    }


    @Test
    public void testUpdateCONFIGNode() {
        gotoDasPage();
        final String nodeName = NODE_NAME_PREFIX + generateRandomString();

        clickAndWait(ID_NODE_TREE_LINK);
        clickAndWait(ID_NEW_NODE_BUTTON);

        //create the config node
        Select select = new Select(driver.findElement(By.id(ID_TYPE_FIELD)));
        select.selectByVisibleText("CONFIG");
        setFieldValue(ID_CREATE_NAME_FIELD, nodeName);
        setFieldValue(ID_NODEHOST_FIELD, "NodeHOSTCC");
        setFieldValue(ID_NODEDIRECTORY_FIELD, "NodeDirectoryCC");
        setFieldValue(ID_CONFIG_INSTALLDIR_FIELD, "/ProductRoot");
        clickAndWait(ID_CREATE_NODE_BUTTON);

        // Verify nodes information in table
        String prefix = getTableRowByValue(ID_NODES_TABLE, nodeName, "col1");
        assertEquals(nodeName, getText(prefix + "col1:link"));

        //Verify the node is created with the value specified.
        String clickId = prefix + "col1:link";
        clickByIdAction(clickId);

        Select select1 = new Select(driver.findElement(By.id(ID_TYPE_FIELD)));
        assertTrue(select1.getFirstSelectedOption().getAttribute("value").equals("CONFIG"));
        assertEquals("NodeHOSTCC", getValue(ID_NODEHOST_FIELD, "value"));
        setFieldValue(ID_NODEHOST_FIELD, "new-NodeHOSTCC");
        
        assertEquals("NodeDirectoryCC", getValue(ID_NODEDIRECTORY_FIELD, "value"));
        setFieldValue(ID_NODEDIRECTORY_FIELD, "new-NodeDirectoryCC");
        
        assertEquals("/ProductRoot", getValue(ID_CONFIG_INSTALLDIR_FIELD, "value"));
        setFieldValue(ID_CONFIG_INSTALLDIR_FIELD, "/new-ProductRoot");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));

        assertEquals("new-NodeHOSTCC", getValue(ID_NODEHOST_FIELD , "value"));
        assertEquals("new-NodeDirectoryCC", getValue(ID_NODEDIRECTORY_FIELD, "value"));
        assertEquals("/new-ProductRoot", getValue(ID_CONFIG_INSTALLDIR_FIELD, "value"));

        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");
        deleteRow(ID_DELETE_NODE_BUTTON, "propertyForm:nodesTable", nodeName);
    }

    /* Create a Node,  create an instance with this node,  delete this node will cause error */
    @Test
    public void testDeleteWithInstance(){
        gotoDasPage();
        final String nodeName = NODE_NAME_PREFIX + generateRandomString();
        final String instanceName = "testInstance" + generateRandomString();

        createSSHNode(nodeName);
        createInstance(instanceName, nodeName);
        clickAndWait(ID_NODE_TREE_LINK);
        // This part should fail?
        deleteRow(ID_DELETE_NODE_BUTTON, "propertyForm:nodesTable", nodeName);
        isClassPresent("label_sun4");
        assertTrue(driver.findElement(By.className("label_sun4")).getText().equals(("An error has occurred")));

        //cleanup
        clickAndWait(ID_STANDALONE_TREE_LINK);
        deleteRow("propertyForm:instancesTable:topActionsGroup1:button1", "propertyForm:instancesTable", instanceName);

        clickAndWait(ID_NODE_TREE_LINK);
        deleteRow(ID_DELETE_NODE_BUTTON, "propertyForm:nodesTable", nodeName);
    }

    private void createSSHNode(String nodeName){
        clickAndWait(ID_NODE_TREE_LINK);
        clickAndWait(ID_NEW_NODE_BUTTON);

        setFieldValue(ID_CREATE_NAME_FIELD, nodeName);
        setFieldValue(ID_NODEHOST_FIELD, "localhost");
        if (!driver.findElement(By.id(ID_FORCE_FIELD)).isSelected()){
            clickByIdAction(ID_FORCE_FIELD);
        }
        clickAndWait(ID_CREATE_NODE_BUTTON);
        String prefix = getTableRowByValue(ID_NODES_TABLE, nodeName, "col1");
        assertEquals(nodeName, getText(prefix + "col1:link"));
    }

    private void createInstance(String instanceName, String nodeName){
        clickAndWait(ID_STANDALONE_TREE_LINK);
        clickAndWait("propertyForm:instancesTable:topActionsGroup1:newButton");
        isElementPresent("propertyForm:propertySheet:propertSectionTextField:NameTextProp:NameText");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:NameTextProp:NameText", instanceName);
        Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:node:node")));
        select.selectByVisibleText(nodeName);
        Select select1 = new Select(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:configProp:Config")));
        select1.selectByVisibleText("default-config");
        if (!driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:configOptionProp:optC")).isSelected()){
            clickByIdAction("propertyForm:propertySheet:propertSectionTextField:configOptionProp:optC");
        }
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");
        String prefix = getTableRowByValue("propertyForm:instancesTable", instanceName, "col1");
        assertEquals(instanceName, getText(prefix + "col1:link"));
    }
}
