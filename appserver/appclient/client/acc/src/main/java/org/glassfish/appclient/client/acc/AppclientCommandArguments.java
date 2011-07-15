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

package org.glassfish.appclient.client.acc;

import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates handling of appclient script command arguments and options.
 * <p>
 * This class processes a list of strings which are the ACC and client arguments
 * passed on the appclient script.  It makes each ACC argument available
 * explicitly by its own method ({@link #getTargetServer} for example).  The arguments
 * to be passed to the app client itself are available using {@link #getAppArgs }.
 *
 *
 * @author tjquinn
 */
public class AppclientCommandArguments {

    private final static Logger logger = Logger.getLogger(AppclientCommandArguments.class.getName());
    private final static String LINE_SEP = System.getProperty("line.separator");
    private final static Logger accLogger = LogDomains.getLogger(
            AppclientCommandArguments.class,
            LogDomains.ACC_LOGGER);

    /*
     * names of appclient options.
     *
     * Note that the scripts themselves handle the appclient "-client" option so
     * -client is not one of the arguments managed by this class.
     */
    private final static String TEXTAUTH = "textauth";
    private final static String NOAPPINVOKE = "noappinvoke";
    private final static String MAINCLASS = "mainclass";
    private final static String NAME = "name";
    private final static String XML = "xml";
    private final static String CONFIGXML = "configxml";
    private final static String USER = "user";
    private final static String PASSWORD = "password";
    private final static String PASSWORDFILE = "passwordfile";
    private final static String TARGETSERVER = "targetserver";
    private final static String USAGE = "usage";
    private final static String HELP = "help";
    
    final static String PASSWORD_FILE_PASSWORD_KEYWORD = "PASSWORD";

    /* names of options that take a value */
    private final static String[] valuedArgNames =
            new String[] {MAINCLASS, NAME, XML, CONFIGXML, USER, PASSWORD, PASSWORDFILE, TARGETSERVER};

    /* names of options that take no value */
    private final static String[] unvaluedArgNames =
            new String[] {TEXTAUTH, NOAPPINVOKE, USAGE, HELP};

    private static LocalStringManager localStrings = new LocalStringManagerImpl(AppclientCommandArguments.class);;

    private Map<String,AtomicReference<String>> valuedArgs = initValuedArgs();
    private Map<String,AtomicBoolean> unvaluedArgs = initUnvaluedArgs();

    private List<String> unrecognizedArgs = new ArrayList<String>();

    private char[] password = null;
    private boolean isPasswordOptionUsed = false;

    private String configPathToUse = null;

    /**
     * Initializes the list of valued arguments with nulls for all values.
     * @return
     */
    private Map<String,AtomicReference<String>> initValuedArgs() {
        HashMap<String,AtomicReference<String>> result =
                new HashMap<String,AtomicReference<String>>();

        for (String s : valuedArgNames) {
            result.put(s, new AtomicReference<String>(null));
        }
        return result;
    }

    /**
     * Initializes the list of unvalued arguments with false for all values,
     * indicating that none of the options has been seen (yet).
     * @return
     */
    private Map<String,AtomicBoolean> initUnvaluedArgs() {
        HashMap<String,AtomicBoolean> result =
                new HashMap<String,AtomicBoolean>();

        for (String s : unvaluedArgNames) {
            result.put(s, new AtomicBoolean(false));
        }
        return result;
    }

    /**
     * Creates and returns a new AppclientCommandArguments object from which
     * the ACC argument settings and the arguments to be passed to the app
     * client can be retrieved.
     *
     * @param appclientCommandArgs appclient command arguments to use in
     * populating the launch info object
     * @return
     */
    public static AppclientCommandArguments newInstance(List<String> appclientCommandArgs) throws UserError {
        AppclientCommandArguments result = new AppclientCommandArguments();

        result.processAppclientArgs(appclientCommandArgs);

        return result;
    }

    private AppclientCommandArguments() {
    }


    public boolean isTextauth() {
        return unvaluedArgs.get(TEXTAUTH).get();
    }

    public boolean isNoappinvoke() {
        return unvaluedArgs.get(NOAPPINVOKE).get();
    }

    public boolean isUsage() {
        return unvaluedArgs.get(USAGE).get();
    }

    public boolean isHelp() {
        return unvaluedArgs.get(HELP).get();
    }

    public String getUser() {
        return valuedArgs.get(USER).get();
    }

    public char[] getPassword() {
        return password;
    }

//    public String getPasswordFile() {
//        return valuedArgs.get(PASSWORDFILE).get();
//    }

    public String getName() {
        return valuedArgs.get(NAME).get();
    }

    public String getTargetServer() {
        return valuedArgs.get(TARGETSERVER).get();
    }

    private String getXML() {
        return valuedArgs.get(XML).get();
    }

    private  String getConfigXML() {
        return valuedArgs.get(CONFIGXML).get();
    }

    public String getConfigFilePath() {
        if (configPathToUse == null) {
            configPathToUse = chooseConfigFilePath();
        }
        return configPathToUse;
    }

    private String chooseConfigFilePath() {
        boolean isConfig = logger.isLoggable(Level.CONFIG);
        String pathToUse = null;
        if ((pathToUse = getXML()) != null) {
            if (isConfig) {
                logger.log(Level.CONFIG, "Choosing app client container config from -xml option: {0}", pathToUse);
            }
        } else if ((pathToUse = getConfigXML()) != null ) {
            if (isConfig) {
                logger.log(Level.CONFIG, "Choosing app client container config from -configxml option: {0}", pathToUse);
            }
//        } else if (isJWS) {
//            /*
//             *Neither -xml nor -configxml were present and this is a Java
//             *Web Start invocation.  Use
//             *the alternate mechanism to create the default config.
//             */
//            try {
//                String jwsACCConfigXml = prepareJWSConfig();
//                if (jwsACCConfigXml != null) {
//                    pathToUse = jwsACCConfigXml;
//                    if (isFine) {
//                        logger.fine(localStrings.getString("appclient.configFromJWSTemplate"));
//                    }
//                }
//            } catch (Throwable thr) {
//                throw new RuntimeException(localStrings.getString("appclient.errorPrepConfig"), thr);
//            }
        }
        return pathToUse;

    }

    public String getMainclass() {
        return valuedArgs.get(MAINCLASS).get();
    }

    public List<String> getAppArgs() {
        return unrecognizedArgs;
    }

    private void processAppclientArgs(final List<String> commandArgs) throws UserError {
        final boolean isConfig = logger.isLoggable(Level.CONFIG);
        final StringBuilder sb = (isConfig ?
            new StringBuilder("Arguments from appclient command:" ) : null);
        for (int slot = 0; slot < commandArgs.size(); slot++) {
            String arg = commandArgs.get(slot);
            if (arg.charAt(0) == '-') {
                arg = arg.substring(1);
                 if (valuedArgs.containsKey(arg)) {
                     if (slot < commandArgs.size()) {
                         String value = commandArgs.get(++slot);
                         /*
                          * The scripts might use " " to enclose argument values,
                          * so if that's the case strip the quotes off.
                          */
                         if ((value.length() > 1) && 
                                 (value.charAt(0) == '\"') &&
                                 (value.charAt(value.length() - 1) == '\"')
                             ) {
                             value = value.substring(1, value.length() - 1);
                         }
                         if (arg.equals(PASSWORD)) {
                             password = value.toCharArray();
                             isPasswordOptionUsed = true;
                             if (isConfig) {
                                 sb.append(LINE_SEP).append("  ").append(arg).append("=???");
                             }
                         } else {
                             if (isConfig) {
                                 sb.append(LINE_SEP).append("  ").append(arg).append("=").append(value);
                             }
                         }
                         valuedArgs.get(arg).set(value);
                         if (arg.equals(PASSWORDFILE)) {
                             processPasswordFile(value);
                         }
                     } else {
                         throw new IllegalArgumentException(arg+"=?");
                     }
                 } else if (unvaluedArgs.containsKey(arg)) {
                     unvaluedArgs.get(arg).set(Boolean.TRUE);
                     if (isConfig) {
                         sb.append(LINE_SEP).append("  ").append(arg);
                     }
                 } else {
                     unrecognizedArgs.add(arg);
                 }
            } else {
                unrecognizedArgs.add(arg);
            }
        }
        if (isConfig) {
            logger.config(sb.toString());
        }
        validateOptions();
    }

    private void processPasswordFile(final String passwordFilePath) throws UserError {
        try {
            final File pwFile = Util.verifyFilePath(passwordFilePath);

            /*
             * The file path looks good - read the password from it.
             */
            password = loadPasswordFromFile(pwFile);
        } catch (IOException e) {
            String msg = localStrings.getLocalString(getClass(),
                    "appclient.errorReadingFromPasswordFile",
                    "Error reading the password from the password file {0}",
                    new Object[] {passwordFilePath});
            throw new UserError(msg, e);
        }
    }

    private char[] loadPasswordFromFile(final File pwFile)
            throws IOException, UserError {
        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(pwFile));
            // XXX Should change this so we don't store the pw in a String even temporarily (as in the Props object)
            Properties props = new Properties();
            props.load(inputStream);
            if (props.containsKey(PASSWORD_FILE_PASSWORD_KEYWORD)) {
                return props.getProperty(PASSWORD_FILE_PASSWORD_KEYWORD).toCharArray();
            } else {
                final String msg = localStrings.getLocalString(getClass(),
                        "appclient.noPasswordInFile",
                        "The file {0} specified by the -passwordfile option should contain a setting PASSWORD=client-password-to-use but does not",
                        new Object[] {pwFile.getAbsolutePath()});
                throw new UserError(msg);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private void validateOptions() throws UserError {
        ensureAtMostOneOfNameAndMainClass();
        warnAboutPasswordUsage();
    }
    /**
     * Makes sure that at most one of the -name and -mainclass arguments
     * appeared on the command line.
     * @throws IllegalArgumentException if both appeared
     */
    private void ensureAtMostOneOfNameAndMainClass() throws UserError {
        if ((getMainclass() != null) && (getName() != null)) {
            throw new UserError(localStrings.getLocalString(
                    getClass(), 
                    "appclient.mainclassOrNameNotBoth",
                    "Specify either -mainclass or -name but not both to identify the app client to be run"));
        }
    }

    /**
     * Logs a warning if the user specified the -password command line
     * option, which is discouraged and deprecated.
     */
    private void warnAboutPasswordUsage() {
        if (isPasswordOptionUsed) {
            final String msg = accLogger.getResourceBundle().getString("appclient.password.deprecated");
            logger.warning(msg);
        }
    }

}
