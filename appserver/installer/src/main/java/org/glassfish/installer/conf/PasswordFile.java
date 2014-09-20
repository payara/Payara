/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.installer.conf;

import java.util.Arrays;

/** PasswordFile to be used for asadmin create-domain command.
 * @author sathyan
 */
public class PasswordFile {

    private static final String ADMIN_PASSWORD_KEY = "AS_ADMIN_PASSWORD=";
    private static final String MASTER_PASSWORD_KEY = "AS_ADMIN_MASTERPASSWORD=";

    /* Domain administrator password. */
    private char[] adminPassword;
    /* Master password. */
    private char[] masterPassword;
    /* Password file content */
    private char[] fileContent;

    public PasswordFile(char[] adminPassword, char[] masterPassword) {
        this.adminPassword = adminPassword;
        this.masterPassword = masterPassword;
        this.fileContent = null;
    }

    /* Assemble password file content
     * @return boolean true if successful, false otherwise.
     */
    public boolean setupPasswordFile() {
        boolean retStatus = true;
        try {
            
            char[] newline = { '\n' };
            /* The password could be null to support unauthenticated logins. */
            if (adminPassword != null && adminPassword.length > 0) {
                fileContent = concat(ADMIN_PASSWORD_KEY.toCharArray(), 
                    adminPassword);
            } else {
                fileContent = ADMIN_PASSWORD_KEY.toCharArray();
            }
            fileContent = concat(fileContent, newline); 
            fileContent = concat(fileContent, MASTER_PASSWORD_KEY.toCharArray());
            fileContent = concat(fileContent, masterPassword);
            fileContent = concat(fileContent, newline);
         
        } catch (Exception ex) {
            retStatus = false;
        }
        return true;
    }

    /* @return char[] domain administrator password. */
    public char[] getAdminPassword() {
        return adminPassword;
    }

    /* @param adminPassword domain administrator password. */
    public void setAdminPassword(char[] adminPassword) {
        this.adminPassword = adminPassword;
    }

    /* @return char[] Password file content. */
    public char[] getFileContent() {
        return fileContent;
    }

    /* @return char[] master password. */
    public char[] getMasterPassword() {
        return masterPassword;
    }

    /* @param masterPassword master password. */
    public void setMasterPassword(char[] masterPassword) {
        this.masterPassword = masterPassword;
    }

    private static char[] concat(char[] first, char[] second) {
        char[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public void emptyPasswordArrays() {
        Arrays.fill(this.adminPassword, ' ');
        Arrays.fill(this.masterPassword, ' ');
        Arrays.fill(this.fileContent, ' ');
    }

}
