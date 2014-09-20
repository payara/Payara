/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.client;

import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.OS;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.glassfish.appclient.client.acc.UserError;

/**
 *
 * Constructs a java command to launch the ACC with the correct agent and
 * command line arguments, based on the current operating environment and
 * the user's own command-line arguments.
 * <p>
 * The user might have specified JVM options as well as ACC options as
 * well as arguments to be passed to the client.  Further, we need to make
 * sure that the GlassFish extension libraries directories and endorsed
 * directories are included in java.ext.dirs and java.endorsed.dirs,
 * regardless of whether the user specified any explicitly.
 * <p>
 * This program emits a java command line that will run the ACC so that it
 * will launch the client.  The emitted command will need to look like this:
 * <pre>
 * {@code
 * java \
 *   (user-specified JVM options except -jar) \
 *   (settings for java.ext.dirs and java.endorsed.dirs) \
 *   -javaagent:(path-to-gf-client.jar)=(option string for our agent) \
 *   (main class setting: "-jar x.jar" or "a.b.Main" or "path-to-file.class")
 *   (arguments to be passed to the client)
 * }</pre>
 * <p>
 * The general design of this class uses several inner classes, CommandLineElement
 * and its extensions.  These classes have slightly different behavior depending
 * on the specific type of command line element each represents.  Each has
 * a regex pattern which it uses to decide whether it recognizes a particular
 * command line element or not.  Each also implements (or inherits) the
 * processValue method which actually consumes the command line element being
 * handled -- and sometimes the next one as well if the element takes a value
 * (such as -classpath).
 * 
 * @author Tim Quinn
 */
public class CLIBootstrap {
    
    public final static String FILE_OPTIONS_INTRODUCER = "argsfile=";

    private final static String COMMA_IN_ARG_PLACEHOLDER = "+-+-+-+";
    private final static boolean isDebug = System.getenv("AS_DEBUG") != null;
    private final static String INPUT_ARGS = System.getenv("inputArgs");

    final static String ENV_VAR_PROP_PREFIX = "acc.";

    /** options to the ACC that take a value */
    private final static String ACC_VALUED_OPTIONS_PATTERN = 
            "-mainclass|-name|-xml|-configxml|-user|-password|-passwordfile|-targetserver";

    /** options to the ACC that take no value */
    private final static String ACC_UNVALUED_OPTIONS_PATTERN =
            "-textauth|-noappinvoke|-usage|-help";

    private final static String JVM_VALUED_OPTIONS_PATTERN =
            "-classpath|-cp";

    private final static String INSTALL_ROOT_PROPERTY_EXPR = "-Dcom.sun.aas.installRoot=";
    private final static String SECURITY_POLICY_PROPERTY_EXPR = "-Djava.security.policy=";
    private final static String SECURITY_AUTH_LOGIN_CONFIG_PROPERTY_EXPR = "-Djava.security.auth.login.config=";
    private final static String SYSTEM_CLASS_LOADER_PROPERTY_EXPR =
            "-Djava.system.class.loader=org.glassfish.appclient.client.acc.agent.ACCAgentClassLoader";

    private final static String[] ENV_VARS = {
        "_AS_INSTALL", "APPCPATH", "VMARGS"};

    private final static String EXT_DIRS_INTRODUCER = "-Djava.ext.dirs";
    private final static String ENDORSED_DIRS_INTRODUCER = "-Djava.endorsed.dirs";

    private static final LocalStringManager localStrings = new LocalStringManagerImpl(CLIBootstrap.class);

    private JavaInfo java;

    private GlassFishInfo gfInfo;

    private UserVMArgs userVMArgs;

    /**
     * set up during init with various subtypes of command line elements
     */
    private CommandLineElement
            extDirs, endorsedDirs, accValuedOptions, accUnvaluedOptions,
            jvmPropertySettings, jvmValuedOptions, otherJVMOptions, arguments;

    /** arguments passed to the ACC Java agent */
    private final AgentArgs agentArgs = new AgentArgs();

    /** records how the user specifies the main class: -jar xxx.jar, -client xxx.jar, or a.b.MainClass */
    private final JVMMainOption jvmMainSetting = new JVMMainOption();

    /** command line elements from most specific to least specific matching pattern */
    private CommandLineElement[] elementsInScanOrder;
    
    /** command line elements in the order they should appear on the generated
     * command line
     */
    private CommandLineElement[] elementsInOutputOrder;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            /*
             * Convert env vars to properties.  (This makes testing easier.)
             */
            envToProps();
            final CLIBootstrap boot = new CLIBootstrap();
            /*
             * Because of how Windows passes arguments, the calling Windows
             * script assigned the input arguments to an environment variable.
             * Parse that variable's value into the actual arguments.
             */
            if (INPUT_ARGS != null) {
                args = convertInputArgsVariable(INPUT_ARGS);
            }
            final String outputCommandLine = boot.run(args);
            if (isDebug) {
                System.err.println(outputCommandLine);
            }
            /*
             * Write the generated java command to System.out.  The calling
             * shell script will execute this command.
             * 
             * Using print instead of println seems to work better.  Using
             * println added a \r to the end of the last command-line argument
             * on Windows under cygwin.  
             */
            System.out.print(outputCommandLine);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        } catch (UserError ue) {
            ue.displayAndExit();
        }
    }

    /**
     * Replaces commas in an argument value (which can confuse the ACC agent
     * argument parsing because shells strip out double-quotes) with a special
     * sequence.
     *
     * @param s string to encode
     * @return encoded string
     */
    public static String encodeArg(final String s) {
        return s.replace(",", COMMA_IN_ARG_PLACEHOLDER);
    }

    /**
     * Replaces occurrences of comma encoding with commas.
     *
     * @param s possibly encoded string
     * @return decoded string
     */
    public static String decodeArg(final String s) {
        return s.replace(COMMA_IN_ARG_PLACEHOLDER, ",");
    }

    private static String[] convertInputArgsVariable(final String inputArgs) {
        /*
         * The pattern matches a quoted string (double quotes around a string
         * containing no double quote) or a non-quoted string (a string containing
         * no white space or quotes).
         */
        final Pattern argPattern = Pattern.compile("\"([^\"]+)\"|([^\"\\s]+)");

        final Matcher matcher = argPattern.matcher(inputArgs);
        final List<String> argList = new ArrayList<String>();
        while (matcher.find()) {
            final String arg = (matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
            argList.add(arg);
            if (isDebug) {
                System.err.println("Captured argument " + arg);
            }
        }
        return argList.toArray(new String[argList.size()]);
    }

    private static void envToProps() {
        for (String envVar : ENV_VARS) {
            final String value = System.getenv(envVar);
            if (value != null) {
                System.setProperty(ENV_VAR_PROP_PREFIX + envVar, value);
                if (isDebug) {
                    System.err.println(ENV_VAR_PROP_PREFIX + envVar + " set to " + value);
                }
            }
        }
    }

    CLIBootstrap() throws UserError {
        init();
        }

    private void init() throws UserError {
        java = initJava();
        gfInfo = new GlassFishInfo();
        userVMArgs = new UserVMArgs(System.getProperty(ENV_VAR_PROP_PREFIX + "VMARGS"));

        /*
         * Assign the various command line element matchers.  See the
         * descriptions of each subtype for what each is used for.
         */
        extDirs = new OverridableDefaultedPathBasedOption(
            EXT_DIRS_INTRODUCER,
            userVMArgs.evExtDirs,
            java.ext().getAbsolutePath(),
            gfInfo.extPaths());

        endorsedDirs = new OverridableDefaultedPathBasedOption(
            ENDORSED_DIRS_INTRODUCER,
            userVMArgs.evEndorsedDirs,
            java.endorsed().getAbsolutePath(),
            gfInfo.endorsedPaths());

        accValuedOptions = new ACCValuedOption(ACC_VALUED_OPTIONS_PATTERN);

        accUnvaluedOptions = new ACCUnvaluedOption(ACC_UNVALUED_OPTIONS_PATTERN);

        jvmPropertySettings = new JVMOption("-D.*", userVMArgs.evJVMPropertySettings);

        jvmValuedOptions = new JVMValuedOption(JVM_VALUED_OPTIONS_PATTERN,
                userVMArgs.evJVMValuedOptions);

        otherJVMOptions = new JVMOption("-.*", userVMArgs.evOtherJVMOptions);

        arguments = new CommandLineElement(".*", Pattern.DOTALL);

        initCommandLineElements();
    }

    /**
     * Populates the command line elements collection to contain the elements
     * from most specific matching pattern to least specific.
     */
    private void initCommandLineElements() {
        /*
         * Add the elements in this order so the regex patterns will match
         * the correct elements.  In this arrangement, the patterns are from
         * most specific to most general.
         */
        elementsInScanOrder = new CommandLineElement[] {
            extDirs,
            endorsedDirs,
            accValuedOptions,
            accUnvaluedOptions,
            jvmValuedOptions,
            jvmPropertySettings,
            jvmMainSetting,
            otherJVMOptions,
            arguments};
        
        /*
         * Add the elements in this order so they appear in the generated
         * java command in the correct positions.
         */
        elementsInOutputOrder = new CommandLineElement[] {
            jvmValuedOptions,
            jvmPropertySettings,
            otherJVMOptions,
            extDirs,
            endorsedDirs,
            accUnvaluedOptions,
            accValuedOptions,
            jvmMainSetting,
            arguments
            };
    }

    /**
     * Places double quote marks around a string if the string is not already
     * so enclosed.
     * @param s
     * @return the string wrapped in double quotes if not already that way; the original string otherwise
     */
    private static String quote(final String s) {
        if (s.length() > 2 && s.charAt(0) != '"' && s.charAt(s.length() - 1) != '"' ) {
            return '\"' + s + '\"';
        } else {
            return s;
        }
    }

    /**
     * Quotes the string, on non-Windows systems quoting individually any
     * $.  The shell will have replaced any env. var. placeholders with
     * their values before invoking this program. Anything that looks like a
     * placeholder now is an odd but legal name that should not be substituted
     * again.
     *
     * @param s
     * @return
     */
    private static String quoteSuppressTokenSubst(final String s) {
        return (OS.isWindows() ? quote(s) : quote(s.replace("$", "\\$")));
    }

    /**
     * Manages the arguments which will be passed to the ACC Java agent.
     */
    private static class AgentArgs {
        private final StringBuilder args = new StringBuilder("=mode=acscript");
        private char sep = ',';

        AgentArgs() {
            final String appcPath = System.getProperty(ENV_VAR_PROP_PREFIX + "APPCPATH");
            if (appcPath != null && appcPath.length() > 0) {
                add("appcpath=" + quote(appcPath));
            }
        }

        /**
         * Adds an item to the Java agent arguments.
         * @param item
         */
        final void add(final String item) {
            args.append(sep).append(item);
        }

        /**
         * Adds an ACC argument to the Java agent arguments.
         * @param accArg
         */
        final void addACCArg(final String accArg) {
            add("arg=" + encodeArg(accArg));
        }

        @Override
        public String toString() {
            return args.toString();
        }
    }

    /**
     * A command-line element.  Various subtypes have some different behavior
     * for some of the methods.
     */
    private class CommandLineElement {

        private final Pattern pattern;
        Matcher matcher;

        private final Pattern whiteSpacePattern = Pattern.compile("[\\r\\n]");

        /** allows multiple values; not all command line elements support this*/
        final List<String> values = new ArrayList<String>();

        CommandLineElement(String patternString) {
            this(patternString, 0);
        }

        CommandLineElement(String patternString, int flags) {
            pattern = Pattern.compile(patternString, flags);
        }

        final boolean matchesPattern(final String element) {
            matcher = pattern.matcher(element);
            return matcher.matches();
        }

        boolean matches(final String element) {
            return matchesPattern(element);
        }

        /**
         * Processes the command line element at args[slot].
         * <p>
         * Subclass implementations might consume the next element as well.
         * @param args
         * @param slot
         * @return next slot to be processed
         * @throws UserError if the user specified an option that requires a
         * value but provided no value (either the next command line element is
         * another option or there is no next element)
         */
        int processValue(String[] args, int slot) throws UserError {
            /*
             * Ignore an argument that is just unquoted white space.
             */
            final Matcher m = whiteSpacePattern.matcher(args[slot]);
            if ( ! m.matches()) {
                values.add(args[slot++]);
            } else {
                slot++;
            }
            return slot;
        }
        
        /**
         * Adds the command-line element to the Java agent arguments, if
         * appropriate.
         *
         * @param element
         */
        void addToAgentArgs(final String element) {
        }

        /**
         * Returns whether there is a next argument.
         * @param args
         * @param currentSlot
         * @return
         */
        boolean isNextArg(String[] args, int currentSlot) {
            return currentSlot < args.length - 1;
        }

        /**
         * Returns the next argument in the array, without advancing
         * the pointer into the array.
         *
         * @param args
         * @param currentSlot
         * @return
         */
        String nextArg(String[] args, int currentSlot){
            return args[currentSlot + 1];
        }

        /**
         * Makes sure that there is a next argument and that its value does
         * not start with a "-" which would indicate an option, rather than
         * the value for the option we are currently processing.
         *
         * @param args
         * @param currentSlot
         * @throws UserError
         */
        void ensureNonOptionNextArg(final String[] args, final int currentSlot) throws UserError {
            if ((currentSlot >= args.length - 1) || (args[currentSlot + 1].charAt(0) == '-')) {
                throw new UserError("Command line element " + args[currentSlot] + " requires non-option value");
            }
        }

        /**
         * Adds a representation for this command-line element to the output
         * command line.
         *
         * @param commandLine
         * @return true if any values from this command-line element
         * was added to the command line, false otherwise
         */
        boolean format(final StringBuilder commandLine) {
            return format(commandLine, true);
        }

        /**
         * Adds a representation for this command-line element to the output
         * command line, quoting the value if requested.
         *
         * @param commandLine
         * @param useQuotes
         * @return true if any values from this command-line element
         * were added to the command line; false otherwise
         */
        boolean format(final StringBuilder commandLine, boolean useQuotes) {
            boolean needSep = false;
            for (String value : values) {
                if (needSep) {
                    commandLine.append(valueSep());
                }
                format(commandLine, useQuotes, value);
                needSep = true;
            }
            return ! values.isEmpty();
        }

        /**
         * Returns the separator character to be inserted in the emitted
         * command line between values stored in the same instance of this
         * command line element.
         * @return
         */
        char valueSep() {
            return ' ';
        }

        /**
         * Adds a representation for the specified value to the output
         * command line, quoting the value if required and
         * @param commandLine
         * @param useQuotes
         * @param v
         * @return
         */
        StringBuilder format(final StringBuilder commandLine, 
                final boolean useQuotes, final String v) {
            if (commandLine.length() > 0) {
                commandLine.append(' ');
            }
            commandLine.append((useQuotes ? quoteSuppressTokenSubst(v) : v));
            return commandLine;
        }
    }

    /**
     * A command-line option (an element which starts with "-").
     */
    private class Option extends CommandLineElement {

        Option(String patternString) {
            super(patternString);
        }
    }

    /**
     * A JVM command-line option. Only JVM options which appear before the
     * main class setting are propagated to the output command line as
     * JVM options.  If they appear after the main class setting then they
     * are treated as arguments to the client.
     * <p>
     * This type of command line element can include values specified using
     * the VMARGS environment variable.
     *
     */
    private class JVMOption extends Option {
        
        JVMOption(final String patternString,
                final CommandLineElement vmargsJVMOptionElement) {
            super(patternString);
            if (vmargsJVMOptionElement != null) {
                values.addAll(vmargsJVMOptionElement.values);
            }
        }

        @Override
        boolean matches(final String element) {
            /*
             * Although the element might match the pattern (-.*) we do
             * not treat this as JVM option if we have already processed
             * the main class determinant.
             */
            return ( ! jvmMainSetting.isSet()) && super.matches(element);
        }
    }

    /**
     * ACC options match anywhere on the command line unless and until we
     * see "-jar xxx" in which case we impose the Java-style restriction that
     * anything which follows the specification of the main class is an
     * argument to be passed to the application.
     * <p>
     * We do not impose the same restriction if the user specified -client xxx.jar
     * in order to preserve backward compatibility with earlier releases, in
     * which ACC options and client arguments could be intermixed anywhere on
     * the command line.
     */
    private class ACCUnvaluedOption extends Option {
        ACCUnvaluedOption(final String patternString) {
            super(patternString);
        }

        @Override
        boolean matches(final String element) {
            return ( ! jvmMainSetting.isJarSetting()) && super.matches(element);
        }

        @Override
        int processValue(String[] args, int slot) throws UserError {
            final int result = super.processValue(args, slot);
            agentArgs.addACCArg(values.get(values.size() - 1));
            return result;
        }

        @Override
        boolean format(final StringBuilder commandLine) {
            /*
             * We do not send ACC arguments to the Java command line.  They
             * are placed into the agent argument string instead.
             */
            return false;
        }
    }

    /**
     * An option that takes a value as the next command line element.
     */
    private class ValuedOption extends Option {

        class OptionValue {
            private String option;
            private String value;

            OptionValue(final String option, final String value) {
                this.option = option;
                this.value = value;
            }
        }

        List<OptionValue> optValues = new ArrayList<OptionValue>();

        ValuedOption(final String patternString) {
            super(patternString);
        }
        
        @Override
        int processValue(String[] args, int slot) throws UserError {
            ensureNonOptionNextArg(args, slot);
            optValues.add(new OptionValue(args[slot++], args[slot++]));

            return slot;
        }
        
        @Override
        boolean format(final StringBuilder commandLine) {
            for (OptionValue ov : optValues) {
                format(commandLine, false /* useQuotes */, ov.option);
                format(commandLine, true /* useQuotes */, ov.value);
            }
            return ! optValues.isEmpty();
        }
    }

    private class JVMValuedOption extends ValuedOption {

        JVMValuedOption(final String patternString,
                final CommandLineElement vmargsJVMValuedOption) {
            super(patternString);
            if (vmargsJVMValuedOption != null) {
                values.addAll(vmargsJVMValuedOption.values);
            }
        }

        @Override
        boolean matches(final String element) {
            return ( ! jvmMainSetting.isJarSetting()) && super.matches(element);
        }
    }

    /**
     * ACC options can appear until "-jar xxx" on the command line.
     */
    private class ACCValuedOption extends ValuedOption {
        ACCValuedOption(final String patternString) {
            super(patternString);
        }

        @Override
        boolean matches(final String element) {
            return ( ! jvmMainSetting.isJarSetting()) && super.matches(element);
        }

        @Override
        int processValue(String[] args, int slot) throws UserError {
            final int result = super.processValue(args, slot);
            final OptionValue newOptionValue = optValues.get(optValues.size() - 1);
            agentArgs.addACCArg(newOptionValue.option);
            agentArgs.addACCArg(quote(newOptionValue.value));
            return result;
        }

        @Override
        boolean format(final StringBuilder commandLine) {
            /*
             * We do not send ACC arguments to the Java command line.  They
             * are placed into the agent argument string instead.
             */
            return false;
        }
    }

    /**
     * Command line element(s) with which the user specified the client
     * to be run.  Note that once "-jar xxx" is specified then all
     * subsequent arguments are passed to the client as arguments.
     * Once "-client xxx" is specified then subsequent arguments are treated
     * as ACC options (if they match) or arguments to the client.
     */
    private class JVMMainOption extends CommandLineElement {
        private static final String JVM_MAIN_PATTERN =
                "-jar|-client|[^-][^\\s]*";

        private String introducer = null;

        JVMMainOption() {
            super(JVM_MAIN_PATTERN);
        }

        boolean isJarSetting() {
            return "-jar".equals(introducer);
        }

        boolean isClientSetting() {
            return "-client".equals(introducer);
        }

        boolean isClassSetting() {
            return ( ! isJarSetting() && ! isClientSetting() && isSet());
        }

        boolean isSet() {
            return ! values.isEmpty();
        }

        @Override
        boolean matches(String element) {
            /*
             * For backward compatibility, the -client element can appear
             * multiple times with the last appearance overriding earlier ones.
             */
            return  (( ! isSet()) ||
                     ( (isClientSetting() && element.equals("-client")))
                    )
                    && super.matches(element);
        }

        @Override
        int processValue(String[] args, int slot) throws UserError {
            /*
             * We only care about the most recent setting.
             */
            values.clear();

            /*
             * If arg[slot] is -jar or -client we expect the
             * next value to be the file.  Make sure there is
             * a next item and that it does not start with -.
             */
            if (args[slot].charAt(0) == '-') {
                if (nextLooksOK(args, slot)) {
                    introducer = args[slot++];
                    final int result = super.processValue(args, slot);
                    final String path = values.get(values.size() - 1);
                    final File clientSpec = new File(path);
                    if (clientSpec.isDirectory()) {
                        /*
                         * Record in the agent args that the user is launching
                         * a directory. Set the main class launch info to
                         * launch the ACC JAR.
                         */
                        agentArgs.add("client=dir=" + quote(clientSpec.getAbsolutePath()));
                        introducer = "-jar";
                        values.set(values.size() - 1, gfInfo.agentJarPath());
                    } else {
                        agentArgs.add("client=jar=" + quote(path));
                        /*
                         * The client path is not a directory.  It should be a
                         * .jar or a .ear file.  If an EAR, then we want Java to
                         * launch our ACC jar.  If a JAR, then we will launch
                         * that JAR.
                         */
                        if (path.endsWith(".ear")) {
                            introducer = "-jar";
                            values.set(values.size() - 1, gfInfo.agentJarPath());
                        }
                    }
                    return result;
                } else {
                    throw new UserError("-jar or -client requires value but missing");
                }
            } else {
                /*
                 * This must be a main class specified on the command line.
                 */
                final int result = super.processValue(args, slot);
                agentArgs.add("client=class=" + values.get(values.size() - 1));
                return result;
                
            }
        }
        
        @Override
        boolean format(final StringBuilder commandLine) {
            if (introducer != null) {
                /*
                 * In the generated command we always use "-jar" to indicate
                 * the JAR to be launched, even if the user specified "-client"
                 * on the appclient command line.
                 */
                super.format(commandLine, false /* useQuotes */, "-jar");
                return super.format(commandLine, true /* useQuotes */);
            }
            return super.format(commandLine, false /* useQuotes */);
        }

        private boolean nextLooksOK(final String[] args, final int slot) {
            return (isNextArg(args, slot) && (args[slot+1].charAt(0) != '-'));
        }
    }

    /**
     * A JVM option that uses values from the GlassFish installation plus default
     * value(s) from the Java installation.  If the user specifies one of these
     * options on the command line then we discard the Java installation values
     * and append the GlassFish values to the user's values.
     * <p>
     * This is used for handling java.ext.dirs and java.endorsed.dirs property
     * settings.  If the user does not specify the property then the user would
     * expect the Java-provided directories to be used. We need to
     * specify the GlassFish ones, so that means we need combine the GlassFish
     * ones and the default JVM ones explicitly.
     * <p>
     * On the other hand, if the user specifies the property then the JVM
     * defaults are out of play. We still need the GlassFish directories to be
     * used though.
     */
    private class OverridableDefaultedPathBasedOption extends JVMOption {

        private final String defaultValue;
        private final List<String> gfValues;
        private final String introducer;
        private boolean hasCommandLineValueAppeared = false;
        
        OverridableDefaultedPathBasedOption(final String introducer,
                final CommandLineElement settingsFromEnvVar,
                final String defaultValue,
                final String... gfValues) {
            super(introducer + "=.*", null);
            /*
             * Preload the values for this option from the ones the user
             * provides in the environment variable, if any.  These values
             * will be overwritten if the user also provides values on the
             * command line.
             */
            if (settingsFromEnvVar != null) {
                values.addAll(settingsFromEnvVar.values);
            }
            this.introducer = introducer;
            this.defaultValue = defaultValue;
            this.gfValues = Arrays.asList(gfValues);
        }

        @Override
        int processValue(String[] args, int slot) throws UserError {
            /*
             * Once we see a user-provided value on the command line itself
             * for the first time, clear out any pre-loaded values from the
             * environment variable.
             */
            if ( ! hasCommandLineValueAppeared) {
                values.clear();
                hasCommandLineValueAppeared = true;
            }
            values.addAll(Arrays.asList(args[slot++].substring(introducer.length() + 1).split(java.pathSeparator())));
            return slot;
        }
        
        @Override
        boolean format(final StringBuilder commandLine) {
            final List<String> combinedValues = new ArrayList<String>();
            /*
             *
             */
            if (values.isEmpty()) {
                /*
                 * The user did not specify this property, so we use
                 * the GlassFish value(s) plus the JVM default.
                 */
                combinedValues.addAll(gfValues);
                combinedValues.add(defaultValue);
            } else {
                /*
                 * The user did specify this property, so we use
                 * the user's value plus the GlassFish value(s).
                 */
                combinedValues.addAll(values);
                combinedValues.addAll(gfValues);
            }
            commandLine.append(introducer).append("=");
            boolean needSep = false;
            for (String value : combinedValues) {
                if (needSep) {
                    commandLine.append(java.pathSeparator());
                }
                commandLine.append(quoteSuppressTokenSubst(value));
                needSep = true;
            }
            return true;
        }
    }

    /**
     * Adds JVM properties for various ACC settings.
     * @param command
     */
    private void addProperties(final StringBuilder command) {

        command.append(' ').append(INSTALL_ROOT_PROPERTY_EXPR).append(quote(gfInfo.home().getAbsolutePath()));
        command.append(' ').append(SECURITY_POLICY_PROPERTY_EXPR).append(quote(gfInfo.securityPolicy().getAbsolutePath()));
        command.append(' ').append(SYSTEM_CLASS_LOADER_PROPERTY_EXPR);
        command.append(' ').append(SECURITY_AUTH_LOGIN_CONFIG_PROPERTY_EXPR).append(quote(gfInfo.loginConfig().getAbsolutePath()));
        
    }

    /**
     * Processes the user-provided command-line elements and creates the
     * resulting output string.
     *
     * @param args
     * @throws UserError
     */
    private String run(String[] args) throws UserError {


        java = initJava();
        gfInfo = new GlassFishInfo();

        final String[] augmentedArgs = new String[args.length + 2];
        augmentedArgs[0] = "-configxml";
        augmentedArgs[1] = gfInfo.configxml().getAbsolutePath();
        System.arraycopy(args, 0, augmentedArgs, 2, args.length);

        /*
         * Process each command-line argument by the first CommandLineElement
         * which matches the argument.
         */
        for (int i = 0; i < augmentedArgs.length; ) {
            boolean isMatched = false;
            for (CommandLineElement cle : elementsInScanOrder) {
                if (isMatched = cle.matches(augmentedArgs[i])) {
                    i = cle.processValue(augmentedArgs, i);
                    break;
                }
            }
            if ( ! isMatched) {
                throw new UserError("arg " + i + " = " + augmentedArgs[i] + " not recognized");
            }
        }
        
        final StringBuilder command = new StringBuilder(quote(java.javaExe));

        addProperties(command);

        /*
         * The user does not specify the -javaagent option we need, so we
         * provide it here.  (It is added to the appropriate command-line 
         * element object so, when formatted, that command-line element
         * includes the -javaagent option.)
         */
        addAgentOption();
        
        /*
         * If the user did not specify a client or usage or help then add the -usage option.
         */
        if ( ! jvmMainSetting.isSet() &&
                ! isHelp() &&
                ! isUsage()) {
            accUnvaluedOptions.processValue(new String[] {"-usage"}, 0);
        }
        
        boolean needSep = true;
        for (CommandLineElement e : elementsInOutputOrder) {
            needSep = processCommandElement(command, e, needSep);
        }
            
        return command.toString();
    }
    
    private boolean processCommandElement(
            final StringBuilder command, 
            final CommandLineElement e, 
            final boolean needSep) {
        if (needSep) {
            command.append(' ');
        }
        return e.format(command);
    }
    
    private boolean isHelp() {
        return accUnvaluedOptions.values.contains("-help");
    }

    private boolean isUsage() {
        return accUnvaluedOptions.values.contains("-usage");
    }

    /**
     * Adds the -javaagent option to the command line.
     * 
     */
    private void addAgentOption() throws UserError {
        otherJVMOptions.processValue(new String[] {
            "-javaagent:" + quote(gfInfo.agentJarPath())  + agentOptionsFromFile()},
            0);
    }
    
    private String agentOptionsFromFile() {
        try {
            final File argsFile = fileContainingAgentArgs();
            return '=' + FILE_OPTIONS_INTRODUCER + quote(argsFile.getAbsolutePath());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private File fileContainingAgentArgs() throws IOException {
        final File argsFile = File.createTempFile("acc", ".dat");
        final PrintStream ps = new PrintStream(argsFile);
        ps.println(agentArgs.toString());
        ps.close();
        return argsFile;
    }

    /**
     * Encapsulates information about the GlassFish installation, mostly useful
     * directories within the installation.
     * <p>
     * Note that we use the property acc._AS_INSTALL to find the installation.
     */
    static class GlassFishInfo {

        private final File home;
        private final File modules;
        private final File lib;
        private final File libAppclient;
        private static final String ACC_CONFIG_PREFIX = "domains/domain1/config";

        GlassFishInfo() {
            final String asInstallPath = System.getProperty(ENV_VAR_PROP_PREFIX + "_AS_INSTALL");
            if (asInstallPath == null || asInstallPath.length() == 0) {
                throw new IllegalArgumentException("_AS_INSTALL == null");
            }
            this.home = new File(asInstallPath);
            modules = new File(home, "modules");
            lib = new File(home, "lib");
            libAppclient = new File(lib, "appclient");
        }

        File home() {
            return home;
        }

        File modules() {
            return modules;
        }

        File lib() {
            return lib;
        }

        File configxml() {
                        /*
             * Try using glassfish-acc.xml.  If that does not exist then the user
             * might have done an in-place upgrade from an earlier version that
             * used sun-acc.xml.
             */
            final File configXMLFile = new File(new File(home, ACC_CONFIG_PREFIX), "glassfish-acc.xml");
            if (configXMLFile.canRead()) {
                return configXMLFile;
            }
            final File sunACCXMLFile = new File(new File(home, ACC_CONFIG_PREFIX), "sun-acc.xml");
            if (sunACCXMLFile.canRead()) {
                return sunACCXMLFile;
            }
            /*
             * We found neither, but when an error is reported we want it to
             * report the glassfish-acc.xml file is missing.
             */
            return configXMLFile;
        }

        String[] endorsedPaths() {
            return new String[] {
                    new File(lib, "endorsed").getAbsolutePath(),
                    new File(modules, "endorsed").getAbsolutePath()};
        }

        String extPaths() {
            return new File(lib, "ext").getAbsolutePath();
        }

        String agentJarPath() {
            return new File(lib, "gf-client.jar").getAbsolutePath();
        }

        File securityPolicy() {
            return new File(libAppclient, "client.policy");
        }

        File loginConfig() {
            return new File(libAppclient, "appclientlogin.conf");
        }
    }

    JavaInfo initJava() {
        return new JavaInfo();
    }

    /**
     * Collects information about the current Java implementation.
     * <p>
     * The user might have defined AS_JAVA or JAVA_HOME, or simply relied on
     * the current PATH setting to choose which Java to use.  Regardless, once
     * this code is running SOME Java has been successfully chosen.  Use
     * the java.home property to find the JRE's home, which we need for the
     * library directory (for example).
     */
    static class JavaInfo {

        private final static String CYGWIN_PROP_NAME = "org.glassfish.isCygwin";
        private final static String SHELL_PROP_NAME = "org.glassfish.appclient.shell";

        /*
         * The appclient and appclient.bat scripts set ACCJava.
         * Properties would be nicer instead of env vars, but the Windows
         * script handling of command line args in the for statement treats
         * the = in -Dprop=value as an argument separator and breaks the
         * property assignment apart into two arguments.
         */
        private final static String ACCJava_ENV_VAR_NAME = "ACCJava";

        private final boolean useWindowsSyntax = File.separatorChar == '\\' &&
                (System.getProperty(SHELL_PROP_NAME) == null);


        protected String javaExe;
        protected File jreHome;

        private JavaInfo() {
            init();
        }

        private void init() {
            jreHome = new File(System.getProperty("java.home"));
            javaExe = javaExe();
        }

        protected boolean isValid() {
            return javaExe != null && new File(javaExe).canExecute();
        }

        protected File javaBinDir() {
            return new File(jreHome, "bin");
        }

        String javaExe() {
            return System.getenv(ACCJava_ENV_VAR_NAME);
        }

        File endorsed() {
            return new File(lib(), "endorsed");
        }

        File ext() {
            return new File(lib(), "ext");
        }

        File lib() {
            return new File(jreHome, "lib");
        }

        String pathSeparator() {
            return useWindowsSyntax ? ";" : ":";
        }
    }

    /**
     * Handles user-specified VM arguments passed by the environment variable
     * VMARGS.
     * <p>
     * This is very much like the handling of the arguments on the more
     * general command line, except that we expect only valid VM arguments
     * here.
     * <p>
     * Some of the "main" CommandLineElements processed earlier in the class will
     * use the inner command line elements here to augment the values they process.
     */
    class UserVMArgs {

        private CommandLineElement evExtDirs, evEndorsedDirs,
                evJVMPropertySettings, evJVMValuedOptions, evOtherJVMOptions;

        private final List<CommandLineElement> evElements = new ArrayList<CommandLineElement>();

        UserVMArgs(final String vmargs) throws UserError {

            if (isDebug) {
                System.err.println("VMARGS = " + (vmargs == null ? "null" : vmargs));
            }
            evExtDirs = new OverridableDefaultedPathBasedOption(
                EXT_DIRS_INTRODUCER,
                null,
                java.ext().getAbsolutePath(),
                gfInfo.extPaths());

            evEndorsedDirs = new OverridableDefaultedPathBasedOption(
                ENDORSED_DIRS_INTRODUCER,
                null,
                java.endorsed().getAbsolutePath(),
                gfInfo.endorsedPaths());

            evJVMPropertySettings = new JVMOption("-D.*", null);

            evJVMValuedOptions = new JVMValuedOption(JVM_VALUED_OPTIONS_PATTERN, null);

            evOtherJVMOptions = new JVMOption("-.*", null);

            initEVCommandLineElements();

            if (vmargs == null) {
                return;
            }
            
            processEVCommandLineElements(convertInputArgsVariable(vmargs));
        }

        private void initEVCommandLineElements() {
            evElements.add(evExtDirs);
            evElements.add(evEndorsedDirs);
            evElements.add(evJVMPropertySettings);
            evElements.add(evJVMValuedOptions);
            evElements.add(evOtherJVMOptions);
        }

        private void processEVCommandLineElements(final String[] envVarJVMArgs) throws UserError {
            /*
             * Process each command-line argument by the first CommandLineElement
             * which matches the argument.
             */
            for (int i = 0; i < envVarJVMArgs.length; ) {
                boolean isMatched = false;
                for (CommandLineElement cle : evElements) {
                    if (isMatched = cle.matches(envVarJVMArgs[i])) {
                        i = cle.processValue(envVarJVMArgs, i);
                        break;
                    }
                }
                if ( ! isMatched) {
                    throw new UserError("arg " + i + " = " + envVarJVMArgs[i] + " not recognized");
                }
            }
        }
    }
}
