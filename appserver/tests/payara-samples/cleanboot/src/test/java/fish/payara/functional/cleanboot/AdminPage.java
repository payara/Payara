/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fish.payara.functional.cleanboot;

import com.microsoft.playwright.Page;

/**
 *
 * @author SimonLaden
 */
public class AdminPage {

    static public void gotoHomepage(Page page) {
        // Open the admin page
        page.navigate("http://localhost:4848");
        page.waitForSelector("div[id='treeForm:tree_children']");
    }

}
