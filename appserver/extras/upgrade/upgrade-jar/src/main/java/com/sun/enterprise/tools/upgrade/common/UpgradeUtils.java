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

package com.sun.enterprise.tools.upgrade.common;

import com.sun.enterprise.tools.upgrade.logging.LogService;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.i18n.StringManager;

import java.io.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

// todo: fix formatting in separate commit
public class UpgradeUtils {
	
    private final StringManager stringManager =
        StringManager.getManager(UpgradeUtils.class);
    private static final Logger logger = LogService.getLogger();
    
    private static UpgradeUtils upgradeUtils;
    private static CommonInfoModel common;
	
    /*
     * UpgradeUtils private constructor
     */
    private UpgradeUtils(CommonInfoModel common) {
        UpgradeUtils.common = common;
    }
	
    public static UpgradeUtils getUpgradeUtils(CommonInfoModel cim) {
        synchronized (UpgradeUtils.class) {
            if (upgradeUtils == null) {
                upgradeUtils = new UpgradeUtils(cim);
            } else {
                common = cim;
            }
            return upgradeUtils;
        }
    }
	
    public void cloneDomain(String original, String destination) {
        File domainDir = new File(original);
        try {
            File cloneDirLoc = new File(destination);
            cloneDirLoc.mkdirs();
            copyDirectory(domainDir, cloneDirLoc);
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                stringManager.getString("upgrade.common.copy_directory_failed",
                domainDir.getAbsolutePath(), destination), e);
            System.exit(1);
        }
    }

    public static void copyFile(String source, String target) throws IOException {
        FileUtils.copy(source, target);
    }

    /**
     * Copies the entire tree to a new location except the symbolic links
     * Invokes the FileUtils.java to do the same
     *
     * @param   sourceDir  File pointing at root of tree to copy
     * @param   targetDir    File pointing at root of new tree
     *
     * If target directory does not exist, it will be created.
     *
     * @exception  IOException  if an error while copying the content
     */
    public static void copyDirectory(File sourceDir, File targetDir) throws IOException {
        File[] srcFiles = sourceDir.listFiles();
        if (srcFiles != null) {
            for (int i = 0; i < srcFiles.length; i++) {
                File dest = new File(targetDir, srcFiles[i].getName());
                if (srcFiles[i].isDirectory() && FileUtils.safeIsRealDirectory(srcFiles[i])) {
                    if (!dest.exists()) {
                        dest.mkdirs();
                    }
                    copyDirectory(srcFiles[i], dest);
                } else {
                    if (!dest.exists()) {
                        dest.createNewFile();
                    }
                    copyFile(srcFiles[i].getAbsolutePath(), new File(targetDir,
                        srcFiles[i].getName()).getAbsolutePath());
                }
            }
        }
    }

	/*
	 * We don't need to validate the xml document now as that is the
	 * job of the upgrade code in the application server. We're only
	 * interested in the version information here, and if that is
	 * somehow wrong then the upgrade process will fail downstream
	 * as the document is parsed into serverbeans objects. Since
	 * we don't need to validate the document, we set our own
	 * entity resolver to avoid the DTD lookup.
	 */
    public Document getDomainDocumentElement(String domainFileName) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document resultDoc = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(new EntityResolver() {
                @Override
                public InputSource resolveEntity(String s, String s1)
                    throws SAXException, IOException {
                    return new InputSource(new StringReader(""));
                }
            });
            resultDoc = builder.parse(new File(domainFileName));
        } catch (Exception ex) {
            logger.log(Level.WARNING,
                stringManager.getString("upgrade.common.could_not_create_doc",
                    new Object [] {domainFileName, ex.toString()}));
        }
        return resultDoc;
    }
	
    /*
     * Code used by DirectoryMover implementations to actually
     * rename the directory if the user agrees. This code adds a '.original'
     * extension unless one exists already. If so, further append .0, .1,
     * etc.
     *
     * If someone is rerunning the upgrade tool, the source domain is
     * already in the target directory. So renaming it makes the information
     * in SourceAppSrvObject invalid. In this case, let SourceAppSrvObject
     * know the new source information.
     */
    public void rename(File dir) {
        assert (dir.exists());
        File tempFile = new File(dir.getAbsolutePath() + ".original");
        if (tempFile.exists()) {
            String baseName = tempFile.getAbsolutePath();
            int count = 0;
            while (tempFile.exists()) {
                tempFile = new File(baseName + "." + count++);
            }
        }
        if (common.getSource().getDomainRoot().equals(
                common.getTarget().getInstallDir())) {
            
            // backup source domain and upgrade original in place
            logger.info(stringManager.getString("enterprise.tools.upgrade.util.copyDir", dir, tempFile));
            cloneDomain(dir.getAbsolutePath(), tempFile.getAbsolutePath());
            common.setAlreadyCloned(true);
        } else {
            logger.info(stringManager.getString(
                "enterprise.tools.upgrade.util.moveDir", dir, tempFile));
            dir.renameTo(tempFile);
        }
    }

    /*
     * This is called from ARG_passwordfile, which shouldn't know about
     * how the password is actually stored. To be honest, even I don't
     * want to know how it's stored. To avoid ever having password
     * information stored in a String, don't use the --passwordfile
     * option.
     *
     * This should only be called after a check that the file
     * exists and is not a directory.
     */
    public void parseStoreMasterPassword(File file) {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(file));
            if (props.getProperty(UpgradeConstants.PASSWORD_KEY) != null) {
                common.getSource().setMasterPassword(props.getProperty(
                    UpgradeConstants.PASSWORD_KEY).toCharArray());
            }
        } catch (FileNotFoundException fnfe) {
            // really?
        } catch (IOException ioe) {
            logger.warning(stringManager.getString("could.not.parse.password",
                ioe.getLocalizedMessage()));
        }
    }
}
