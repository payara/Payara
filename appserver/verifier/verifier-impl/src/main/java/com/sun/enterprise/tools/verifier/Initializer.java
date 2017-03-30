/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;

import com.sun.enterprise.tools.verifier.util.LogDomains;
import com.sun.enterprise.tools.verifier.util.VerifierFormatter;
import com.sun.enterprise.tools.verifier.util.VerifierConstants;
import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * Initialization of arguments and other temporary variables is done here. This
 * class is responsible for creating the framework context and setting proper
 * argument values to it.
 *
 * @author Vikas Awasthi
 */
public class Initializer {

    private LocalStringManagerImpl smh = StringManagerHelper.getLocalStringsManager();
    private Logger logger = LogDomains.getLogger(
            LogDomains.AVK_VERIFIER_LOGGER);

    private VerifierFrameworkContext verifierFrameworkContext = null;
    private String _FOStr = "Failures only"; // NOI18N
    private String _WFOStr = "Warning and Failures only"; // NOI18N
    private String _AOStr = "All"; // NOI18N

    /**
     * Constructor for creating the intitializer object.
     * @param args
     */
    public Initializer(String[] args) {
        this.verifierFrameworkContext = new VerifierFrameworkContext();
        parseArgs(args);
    }

    /**
     *
     * @return returns the initialized FrameworkContext object
     */
    public VerifierFrameworkContext getVerificationContext() {
        return verifierFrameworkContext;
    }

    /**
     * parses the arguments passed to verifier and sets the framework context
     * object.
     * @param argv
     */
    private void parseArgs(String[] argv) {
        int c;
        String arg;
        boolean isVerboseSet = false;
        LongOption[] longopts = new LongOption[20];
        // The set of valid long options for verifier
        StringBuffer sb = new StringBuffer();
        longopts[0] = new LongOption("help", false, null, 'h'); // NOI18N
        longopts[1] = new LongOption("timestamp", false, null, 't'); // NOI18N
        longopts[2] = new LongOption("verbose", false, null, 'v'); // NOI18N
        longopts[3] = new LongOption("version", false, null, 'V'); // NOI18N
        longopts[4] = new LongOption("destdir", true, sb, 'd'); // NOI18N
        longopts[5] = new LongOption("reportlevel", true, sb, 'r'); // NOI18N
        longopts[6] = new LongOption("gui", false, null, 'u'); // NOI18N
        longopts[7] = new LongOption("app", false, null, 'a'); // NOI18N
        longopts[8] = new LongOption("appclient", false, null, 'A'); // NOI18N
        longopts[9] = new LongOption("connector", false, null, 'c'); // NOI18N
        longopts[10] = new LongOption("ejb", false, null, 'e'); // NOI18N
        longopts[11] = new LongOption("web", false, null, 'w'); // NOI18N
        longopts[12] = new LongOption("webservices", false, null, 's'); // NOI18N
        longopts[13] = new LongOption("webservicesclient", false, null, 'l'); // NOI18N
        longopts[14] = new LongOption("persistence", false, null, 'P'); // NOI18N
        longopts[15] = new LongOption("configdir", true, null, 'C'); // NOI18N
        longopts[16] = new LongOption("portability", false, null, 'p'); // NOI18N
        longopts[17] = new LongOption("domain", true, null, 'D'); // NOI18N
        longopts[18] = new LongOption("extDir", true, null, 'E'); // NOI18N
        longopts[19] = new LongOption("mode", true, null, 'm'); // NOI18N

        //The set of valid reportlevel arguments
        String[] levels = new String[6];
        levels[0] = "a"; // NOI18N
        levels[1] = "w"; // NOI18N
        levels[2] = "f"; // NOI18N
        levels[3] = "all"; // NOI18N
        levels[4] = "warnings"; // NOI18N
        levels[5] = "failures"; // NOI18N


        CLIPParser parser = null;
        parser =
                new CLIPParser("verifier", argv, "-:vtVhud:r:aAcewslC:pPm:D:E:", // NOI18N
                        longopts);

        while ((c = parser.getopt()) != -1)
            switch (c) {
                case 0:
                    arg = parser.getOptarg();
                    processValidLongOpt(
                            (char) Integer.parseInt(sb.toString()),
                            arg, levels);
                    break;
                case 'v':
                    logger.setLevel(Level.FINEST);
                    isVerboseSet = true;
                    Handler[] handler = Logger.getLogger("").getHandlers();
                    for (int i = 0; i < handler.length; i++) {
                        handler[i].setLevel(Level.FINEST);
                    }
                    logger.log(Level.FINE,
                            getClass().getName() + ".debug.verboseFlag"); // NOI18N
                    break;

                case 't':
                    verifierFrameworkContext.setUseTimeStamp(true);
                    break;

                case 'h':
                    logger.log(Level.FINE,
                            getClass().getName() + ".debug.helpMessage"); // NOI18N
                    usage();
                    break;

                case 'V':
                    logger.log(Level.INFO, getClass().getName() + ".Version");
                    System.exit(0);
                    break;

                case 'u':
                    logger.log(Level.FINE,
                            getClass().getName() + ".debug.startGUI"); // NOI18N
                    verifierFrameworkContext.setUsingGui(true);
                    break;

                case 'r':
                    arg = parser.getOptarg();
                    if (arg == null) {
                        logger.log(Level.SEVERE,
                                getClass().getName() + ".reqargs", // NOI18N
                                new Object[]{"verifier", "-r"}); // NOI18N
                        usage();

                    } else {
                        boolean validLevel = false;
                        for (int i = 0; i < levels.length; i++) {
                            if (arg.equals(levels[i])) {
                                validLevel = true;
                                break;
                            }
                        }
                        if (!validLevel) {
                            logger.log(Level.SEVERE, getClass().getName() +
                                    ".invalidreplevel", // NOI18N
                                    new Object[]{"verifier", arg}); // NOI18N
                            usage();
                        } else {
                            setReportingLevel(arg.charAt(0));
                        }
                    }
                    break;

                case 'd':
                    arg = parser.getOptarg();
                    if (arg == null) {
                        logger.log(Level.SEVERE, getClass().getName() +
                                ".reqargs", // NOI18N
                                new Object[]{"verifier", "-d"}); // NOI18N
                        usage();
                    } else if (!verifyAndSetResultDir(arg)) {
                        usage();
                    }
                    break;

                case ':':
                    // probably it will never come here
                    logger.log(Level.SEVERE, getClass().getName() + ".reqsarg",
                            new Object[]{"verifier", // NOI18N
                                         new Character(
                                                 (char) parser.getOptopt()).toString()});
                    usage();
                    break;
                case 'a':
                    logger.log(Level.FINE, getClass().getName() + ".debug.app");
                    verifierFrameworkContext.setApp(true);
                    verifierFrameworkContext.setPartition(true);
                    break;

                case 'A':
                    logger.log(Level.FINE,
                            getClass().getName() + ".debug.appclient"); // NOI18N
                    verifierFrameworkContext.setAppClient(true);
                    verifierFrameworkContext.setPartition(true);
                    break;

                case 'c':
                    logger.log(Level.FINE,
                            getClass().getName() + ".debug.connector"); // NOI18N
                    verifierFrameworkContext.setConnector(true);
                    verifierFrameworkContext.setPartition(true);
                    break;

                case 'e':
                    logger.log(Level.FINE, getClass().getName() + ".debug.ejb");
                    verifierFrameworkContext.setEjb(true);
                    verifierFrameworkContext.setPartition(true);
                    break;

                case 'w':
                    logger.log(Level.FINE, getClass().getName() + ".debug.web");
                    verifierFrameworkContext.setWeb(true);
                    verifierFrameworkContext.setPartition(true);
                    break;

                case 's':
                    logger.log(Level.FINE,
                            getClass().getName() + ".debug.webservices"); // NOI18N
                    verifierFrameworkContext.setWebServices(true);
                    verifierFrameworkContext.setPartition(true);
                    break;

                case 'l':
                    logger.log(Level.FINE,
                            getClass().getName() + ".debug.webservicesclient"); // NOI18N
                    verifierFrameworkContext.setWebServicesClient(true);
                    verifierFrameworkContext.setPartition(true);
                    break;

                case 'P':
                    verifierFrameworkContext.setPersistenceUnits(true);
                    verifierFrameworkContext.setPartition(true);
                    break;

                case '?':
                    char x = (char) parser.getOptopt();
                    if (x == '?') {
                        usage();
                    }
                    logger.log(Level.SEVERE,
                            getClass().getName() + ".invalidarg", // NOI18N
                            new Object[]{"verifier", // NOI18N
                                         new Character(x).toString()});
                    usage();
                    break;

                case 'C':
                    arg = parser.getOptarg();
                    if (arg == null || !(new File(arg).exists())) {
                        logger.log(Level.SEVERE, getClass().getName() +
                                ".reqargs", // NOI18N
                                new Object[]{"verifier", "-C"}); // NOI18N
                        usage();
                    }
                    verifierFrameworkContext.setConfigDirStr(arg);
                    break;

                case 'p':
                    verifierFrameworkContext.setPortabilityMode(true);
                    break;

                case 'm':
                    arg = parser.getOptarg();
                    if (arg != null &&
                        (arg.equals(SpecVersionMapper.JavaEEVersion_1_2) ||
                            arg.equals(SpecVersionMapper.JavaEEVersion_1_3) ||
                            arg.equals(SpecVersionMapper.JavaEEVersion_1_4) ||
                            arg.equals(SpecVersionMapper.JavaEEVersion_5))) {
                        verifierFrameworkContext.setJavaEEVersion(arg);
                    } else {
                        logger.log(Level.SEVERE,
                                getClass().getName() + ".invalidjavaeeversion", // NOI18N
                                new Object[]{"verifier", arg}); // NOI18N
                        usage();
                    }
                    logger.log(Level.INFO, getClass().getName() + ".specversion",
                            new Object[]{verifierFrameworkContext.getJavaEEVersion()});
                    break;

                case 'D':
                    arg = parser.getOptarg();
                    if (arg == null) {
                        logger.log(Level.SEVERE, getClass().getName() +
                                ".reqargs", // NOI18N
                                new Object[]{"verifier", "-D"}); // NOI18N
                        usage();
                    }
                    if(!new File(arg).exists()) {
                        logger.log(Level.SEVERE, getClass().getName() +
                                ".log.noDirExists", // NOI18N
                                new Object[]{arg});
                        usage();
                    }
                    verifierFrameworkContext.setDomainDir(arg);
                    break;

                case 'E':
                    arg = parser.getOptarg();
                    if (arg == null) {
                        logger.log(Level.SEVERE, getClass().getName() +
                                ".reqargs", // NOI18N
                                new Object[]{"verifier", "-E"}); // NOI18N
                        usage();
                    }
                    verifierFrameworkContext.setExtDir(arg);
                    break;

                default:
                    logger.log(Level.SEVERE,
                            getClass().getName() + ".invalidarg", // NOI18N
                            new Object[]{"verifier", // NOI18N
                                         new Character((char) c).toString()});
                    usage();
                    break;
            }

        int i = parser.getOptind();
        if (i < argv.length) {
            File jarFile = new File(argv[i]);
            if (!jarFile.exists()) {
                logger.log(Level.SEVERE,
                        getClass().getName() + ".invalidArchive", // NOI18N
                        new Object[]{argv[i]});
                usage();
            }
            verifierFrameworkContext.setJarFileName(jarFile.getPath());
            logger.log(Level.FINE, getClass().getName() + ".debug.jarFileName",
                    new Object[]{verifierFrameworkContext.getJarFileName()});

            i++;
            for (; i < argv.length; i++)
                logger.log(Level.INFO, getClass().getName() + ".extraargs",
                        new Object[]{"verifier", argv[i]}); // NOI18N

        } else {
            if (!verifierFrameworkContext.isUsingGui()) {
                logger.log(Level.SEVERE, getClass().getName() + ".jarmissing",
                        new Object[]{"verifier"}); // NOI18N
                usage();
            }
        }
        if(!isVerboseSet)
            setFormatter();
        logger.log(Level.FINE, getClass().getName() + ".debug.endParseArgs");

    }

    /**
     * validates long cli options
     * @param c
     * @param arg
     * @param levels
     */
    private void processValidLongOpt(int c, String arg, String[] levels) {

        switch (c) {
            case 'v':
                logger.setLevel(Level.FINEST);
                Handler[] handler = Logger.getLogger("").getHandlers();
                for (int i = 0; i < handler.length; i++) {
                    handler[i].setLevel(Level.FINEST);
                }
                logger.log(Level.FINE,
                        getClass().getName() + ".debug.verboseFlag"); // NOI18N
                break;

            case 't':
                verifierFrameworkContext.setUseTimeStamp(true);
                break;

            case 'h':
                logger.log(Level.FINE,
                        getClass().getName() + ".debug.helpMessage"); // NOI18N
                usage();
                break;

            case 'V':
                logger.log(Level.INFO, getClass().getName() + ".Version");
                System.exit(0);
                break;

            case 'u':
                logger.log(Level.FINE,
                        getClass().getName() + ".debug.startGUI"); // NOI18N
                verifierFrameworkContext.setUsingGui(true);
                break;

            case 'r':
                if (arg == null) {
                    logger.log(Level.SEVERE, getClass().getName() + ".reqargs",
                            new Object[]{"verifier", "-r"}); // NOI18N
                    usage();
                } else {
                    boolean validLevel = false;
                    for (int i = 0; i < levels.length; i++) {
                        if (arg.equals(levels[i])) {
                            validLevel = true;
                            break;
                        }
                    }
                    if (!validLevel) {
                        logger.log(Level.SEVERE, getClass().getName() +
                                ".invalidreplevel", // NOI18N
                                new Object[]{"verifier", arg}); // NOI18N
                        usage();
                    } else {
                        setReportingLevel(arg.charAt(0));
                    }
                }
                break;

            case 'd':
                if (arg == null) {
                    logger.log(Level.SEVERE, getClass().getName() + ".reqargs",
                            new Object[]{"verifier", "-d"}); // NOI18N
                    usage();
                } else if (!verifyAndSetResultDir(arg)) {
                    usage();
                }
                break;

            default:
                // should never come here
                logger.log(Level.SEVERE, getClass().getName() + ".invalidarg",
                        new Object[]{"verifier", // NOI18N
                                     new Character((char) c).toString()});
                usage();
                break;
        }
    }

    /**
     * set the reporting level of verifier based on the option used while
     * invoking verifier.
     * @param c
     */
    private void setReportingLevel(char c) {

        boolean setWarningLevelOnce = false;
        switch (c) {
            case 'w':
                if (!setWarningLevelOnce) {
                    logger.log(Level.FINE,
                            getClass().getName() +
                            ".debug.displayWarningFailures"); // NOI18N
                    verifierFrameworkContext.setReportLevel(VerifierConstants.WARN);
                    setWarningLevelOnce = true;
                } else {
                    logger.log(Level.FINE,
                            getClass().getName() + ".debug.reportLevel", // NOI18N
                            new Object[]{
                                getReportLevelString(
                                        verifierFrameworkContext.getReportLevel())});
                }
                break;

            case 'f':
                if (!setWarningLevelOnce) {
                    logger.log(Level.FINE,
                            getClass().getName() + ".debug.displayFailures"); // NOI18N
                    verifierFrameworkContext.setReportLevel(VerifierConstants.FAIL);
                    setWarningLevelOnce = true;
                } else {
                    logger.log(Level.FINE,
                            getClass().getName() + ".debug.reportLevel", // NOI18N
                            new Object[]{
                                getReportLevelString(
                                        verifierFrameworkContext.getReportLevel())});
                }
                break;

            case 'a':
                if (!setWarningLevelOnce) {
                    logger.log(Level.FINE,
                            getClass().getName() + ".debug.displayAll"); // NOI18N
                    verifierFrameworkContext.setReportLevel(VerifierConstants.ALL);
                    setWarningLevelOnce = true;
                } else {
                    logger.log(Level.FINE,
                            getClass().getName() + ".debug.reportLevel", // NOI18N
                            new Object[]{
                                getReportLevelString(
                                        verifierFrameworkContext.getReportLevel())});
                }
                break;

            default:
                // should never come here
                logger.log(Level.SEVERE,
                        getClass().getName() + ".invalidreplevel", // NOI18N
                        new Object[]{"verifier", new Character(c).toString()}); // NOI18N
                usage();
        }
    }

    /**
     * Display usage message to user upon encountering invalid option
     */
    private void usage() {
        if (!verifierFrameworkContext.isUsingGui()) {
            logger.log(Level.INFO, getUsageString());
            //If any argument is found invalid then Veriifer will bail out.
            System.exit(1);
        }
    }

    /**
     *
     * @return string
     */
    private String getUsageString() {
        String usage = "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine1", // NOI18N
                        "usage: VERIFIER [optional_params] <jarFile>")) // NOI18N
                + "\n\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine2", // NOI18N
                        "[optional_params]: can be:")) // NOI18N
                + "\n\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine5", // NOI18N
                        "    -d|--destdir <destination dir> : Verifier " + // NOI18N
                "results are put in the specified existing directory")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine35", // NOI18N
                        "   -D|--domain      : Absolute path of the domain directory. Domain directory will be ignored if verifier is run with -p option. The default domain directory is <AS_INSTALL_DIR>/domains/domain1")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine36", // NOI18N
                        "   -p|--portability : Verifier will be run in portability mode with this option. Verifier runs in appserver mode by default. In the default mode verifier additionally checks correct usage of Sun application server features.")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine16", // NOI18N
                        "   -h|--help|-?     : display verifier help")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine34", // NOI18N
                        "   -t|--timestamp   : verifer results are generated with timestamp appended to it")) // NOI18N
                + "\n\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine7", // NOI18N
                        "    -u|--gui      : use Verifier GUI")) // NOI18N
                + "\n\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine4", // NOI18N
                        "     -v|--verbose : Display more execution information ")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine17", // NOI18N
                        "   -V|--version   : display verifier tool version")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine8", // NOI18N
                        "          (Note: Overrides default behavior,required jarFile not needed) ")) // NOI18N
                + "\n\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine10", // NOI18N
                        "   -r|--reportlevel : result reporting level")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine11", // NOI18N
                        "   level=   a|all : set output report level to " + // NOI18N
                "display all results")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine14", // NOI18N
                        "            f|failures : set output report level results " + // NOI18N
                "to display only failure")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine12", // NOI18N
                        "            w|warnings : set output report level failure results " + // NOI18N
                "to display only warning and")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine27", // NOI18N
                        "   -a|--app         : run only the Application tests")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine28", // NOI18N
                        "   -A|--appclient   : run only the Application Client tests")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine29", // NOI18N
                        "   -c|--connector   : run only the Connector tests")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine30", // NOI18N
                        "   -e|--ejb         : run only the EJB tests")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine31", // NOI18N
                        "   -w|--web         : run only the Web tests")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine32", // NOI18N
                        "   -s|--webservices : run only the WebServices tests")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine33", // NOI18N
                        "   -l|--webservicesclient : run only the WebServicesClient tests")) // NOI18N
                + "\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine37", // NOI18N
                        "   -P|--persistence : run only the EJB 3.0 persistence tests")) // NOI18N
                + "\n\n\n" + // NOI18N
                (smh.getLocalString
                (getClass().getName() + ".usageLine19", // NOI18N
                        "Note: The default mode is non-verbose mode and the " + // NOI18N
                "default reportlevel is to display only warning and failure results.\n")) // NOI18N
                //*****
                + "\n\n" +
                (smh.getLocalString
                (getClass().getName() + ".usageLine20",
                        "<jarFile>: ear/war/jar/rar file to perform static " +
                "verification on "))
                + "\n\n" +
                (smh.getLocalString
                (getClass().getName() + ".usageLine21",
                        "Below is an example which runs verifier in verbose mode and writes all the results of static verification of file sample.ear to the destination directory /verifierresults"))
                + "\n\n" +
                (smh.getLocalString
                (getClass().getName() + ".usageLine24",
                        " verifier -v -ra -d /verifierresults sample.ear"))
                + "\n\n" +
                (smh.getLocalString
                (getClass().getName() + ".usageLine25",
                        " The results get generated in both text and xml format."))
                + "\n" +
                (smh.getLocalString
                (getClass().getName() + ".usageLine26",
                        " Two result files, sample.ear.txt and sample.ear.xml, " +
                "will be created."))
                + "\n\n";

        return usage;
    }

    /**
     * verifies and sets the output directory for keeping the result files.
     * @param name The output directory to keep the result files
     * @return true if output directory is writable, otherwise false.
     */
    private boolean verifyAndSetResultDir(String name) {

        File outputDir = new File(name);
        if (outputDir.exists()) {
            if (outputDir.isDirectory()) {
                if (outputDir.canWrite()) {
                    verifierFrameworkContext.setOutputDirName(name);
                    return true;
                } else {
                    logger.log(Level.SEVERE, getClass().getName() +
                            ".log.noPermissions", new Object[]{name}); // NOI18N
                    return false;
                }
            } else {
                logger.log(Level.SEVERE, getClass().getName() +
                        ".log.notADir", new Object[]{name}); // NOI18N
                return false;

            }
        } else {
            logger.log(Level.SEVERE, getClass().getName() +
                    ".log.noDirExists", new Object[]{name}); // NOI18N
            return false;
        }
    }
    
    /**
     * This method is only used in standalone invocation. For backend 
     * invocations this method should not be called. The logger should continue
     * to use the default formatter when verifier is invoked from backend.
     */ 
    private void setFormatter() {
        Handler[] handlers = logger.getParent().getHandlers();
        for (int i = 0; i < handlers.length; i++) 
            if(handlers[i] instanceof ConsoleHandler)
                handlers[i].setFormatter(new VerifierFormatter());
    }

    /**
     * @param rl
     * @return reporting level string. 
     */
    private String getReportLevelString(int rl) {
        String rls = "";
        if (rl == VerifierConstants.FAIL) {
            rls = _FOStr;
        } else if (rl == VerifierConstants.WARN) {
            rls = _WFOStr;
        } else if (rl == VerifierConstants.ALL) {
            rls = _AOStr;
        }
        return rls;
    }

}
