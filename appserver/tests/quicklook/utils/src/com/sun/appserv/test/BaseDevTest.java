/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

 /**
 * This is the same class as the one in v2/appserv-tests/util/reportbuilder/src/main/java/com/sun/appserv/test.
 *
 * Please see the original svn logs for the contributions.
 */

package com.sun.appserv.test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.sun.appserv.test.util.process.ProcessManager;
import com.sun.appserv.test.util.process.ProcessManagerException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public abstract class BaseDevTest {

    @SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
    public BaseDevTest() {
    }

    protected abstract String getTestName();

    protected abstract String getTestDescription();

    public boolean report(String step, boolean success) {
        return success;
    }

    public boolean report(String step, AsadminReturn ret) {
        return ret.returnValue;
    }

    /**
     * Runs the command with the args given
     *
     * @param args
     *
     * @return true if successful
     */
    public boolean asadmin(final String... args) {
        lastAsadminReturn = asadminWithOutput(args);
        write(lastAsadminReturn.out);
        write(lastAsadminReturn.err);
        return lastAsadminReturn.returnValue;
    }

    /**
     * Runs the command with the args given
     * Returns the precious output strings for further processing.
     *
     * @param args
     *
     * @return true if successful
     */
    public AsadminReturn asadminWithOutput(final String... args) {
        AsadminReturn ret = new AsadminReturn();
        String cmd = isWindows() ? "/bin/asadmin.bat" : "/bin/asadmin";
        List<String> command = new ArrayList<String>();
        String gf_home = System.getProperty("glassfish.home");
        command.add(gf_home + cmd);
        command.addAll(Arrays.asList(antProp("as.props").split(" ")));
        command.addAll(Arrays.asList(args));

        ProcessManager pm = new ProcessManager(command);

        // the tests may be running unattended -- don't wait forever!
        pm.setTimeoutMsec(DEFAULT_TIMEOUT_MSEC);

        pm.setEcho(false);
        int exit;

        try {
            exit = pm.execute();
        }
        catch (ProcessManagerException ex) {
            exit = 1;
        }

        ret.out = pm.getStdout();
        ret.err = pm.getStderr();
        ret.outAndErr = ret.out + ret.err;
        ret.returnValue = exit == 0 && validResults(ret.out,
                String.format("Command %s failed.", args[0]), "list-commands");

        return ret;
    }

    protected boolean validResults(String text, String... invalidResults) {
        for (String result : invalidResults) {
            if (text.contains(result)) {
                return false;
            }
        }
        return true;
    }

    public boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public String antProp(final String key) {
        String value = System.getProperty(key);
        if (value == null) {
            try {
                Properties props = new Properties();
                String apsHome = System.getProperty("BASEDIR");
                FileReader reader = new FileReader(new File(apsHome, "/build.properties"));
                try {
                    props.load(reader);
                }
                finally {
                    reader.close();

                }
                System.getProperties().putAll(props);
                System.setProperty("as.props", String.format("--host %s --port %s" +
                        " --echo=true --terse=true", antProp("glassfish.http.host"), antProp("glassfish.admin.port")));
                value = System.getProperty(key);
                int index;
                while ((index = value.indexOf("${env.")) != -1) {
                    int end = value.indexOf("}", index);
                    String var = value.substring(index, end + 1);
                    final String name = var.substring(6, var.length() - 1);
                    value = value.replace(var, System.getenv(name));
                    System.setProperty(key, value);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return value;
    }

    protected final void write() {
        if (verbose) {
            write(lastAsadminReturn.out);
            write(lastAsadminReturn.err);
        }
    }

    public void write(final String text) {
        if (verbose)
            System.out.println(text);
    }

    protected final void writeFailure() {
        System.out.println(FAILURE_START);
        if (lastAsadminReturn != null) {
            System.out.println(lastAsadminReturn.out);
            System.out.println(lastAsadminReturn.err);
        }
    }

    /**
     * Evaluates the Xpath expression
     *
     * @param expr The expression to evaluate
     * @param f The file to parse
     * @param ret The return type of the expression  can be
     *
     * XPathConstants.NODESET XPathConstants.BOOLEAN XPathConstants.NUMBER XPathConstants.STRING XPathConstants.NODE
     *
     * @return the object after evaluation can be of type number maps to a java.lang.Double string maps to a
     *         java.lang.String boolean maps to a java.lang.Boolean node-set maps to an org.w3c.dom.NodeList
     *
     * @throws XPathExpressionException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public Object evalXPath(String expr, File f, QName ret) {
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true); // never forget this!
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document doc = builder.parse(f);
            write("Parsing" + f.getAbsolutePath());
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression xexpr = xpath.compile(expr);
            Object result = xexpr.evaluate(doc, ret);
            write("Evaluating" + f.getAbsolutePath());
            return result;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }


    }

    /**
     * Evaluates the Xpath expression by parsing the DAS's domain.xml
     *
     * @param expr The Xpath expression to evaluate
     *
     * @return the object after evaluation can be of type number maps to a java.lang.Double string maps to a
     *         java.lang.String boolean maps to a java.lang.Boolean node-set maps to an org.w3c.dom.NodeList
     *
     * @throws XPathExpressionException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public Object evalXPath(String expr, QName ret) {
        return evalXPath(expr, getDASDomainXML(), ret);

    }

    /**
     * Gets the domains folder for DAS
     *
     * @return GF_HOME/domains/domain1
     */
    public File getDASDomainDir() {
        return new File(new File(getGlassFishHome(), "domains"), "domain1");
    }

    /**
     * Gets the domain.xml for DAS
     *
     * @return GF_HOME/domains/domain1/config/domain.xml
     */
    public File getDASDomainXML() {
        return new File(new File(getDASDomainDir(), "config"), "domain.xml");
    }

    /**
     * Get the Glassfish home from the environment variable S1AS_HOME
     *
     * @return
     */
    public File getGlassFishHome() {
        String home = System.getProperty("glassfish.home");
        System.out.println("GF_HOME : "+home);
        if (home == null) {
            throw new IllegalStateException("No S1AS_HOME set!");
        }
        File glassFishHome = new File(home);
        try {
            glassFishHome = glassFishHome.getCanonicalFile();
        }
        catch (Exception e) {
            glassFishHome = glassFishHome.getAbsoluteFile();
        }
        if (!glassFishHome.isDirectory()) {
            throw new IllegalStateException("S1AS_HOME is not pointing at a real directory!");
        }
        return glassFishHome;

    }

    /**
     * Implementations can override this method to do the cleanup for eg deleting instances, deleting clusters etc
     */
    public void cleanup() {
    }

    protected final boolean isVerbose() {
        return verbose;
    }

    protected final void setVerbose(boolean b) {
        verbose = b;
    }

    protected final AsadminReturn getLastAsadminReturn() {
        return lastAsadminReturn;
    }

    // simple C-struct -- DIY
    public static class AsadminReturn {

        public boolean returnValue;
        public String out;
        public String err;
        public String outAndErr;
    }
    private static final int DEFAULT_TIMEOUT_MSEC = 120000; // 2 minutes
    private boolean verbose = true;
    // in case the command fails so that it can be printed (Hack bnevins)
    private AsadminReturn lastAsadminReturn;
    private static final String FAILURE_START = "#########    FAILURE   #########";
}
