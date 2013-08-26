/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.cli;

import java.io.*;
import java.util.*;
import java.lang.annotation.Annotation;
import java.util.logging.*;

import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.InjectionManager;
import org.jvnet.hk2.config.InjectionResolver;
import org.jvnet.hk2.config.UnsatisfiedDependencyException;

import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.CommandModel.ParamModel;
import org.glassfish.common.util.admin.CommandModelImpl;
import org.glassfish.common.util.admin.MapInjectionResolver;
import org.glassfish.common.util.admin.ManPageFinder;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;

import com.sun.enterprise.admin.util.CommandModelData.ParamModelData;
import com.sun.enterprise.admin.cli.remote.RemoteCommand;
import com.sun.enterprise.admin.util.LineTokenReplacer;
import com.sun.enterprise.admin.util.TokenValue;
import com.sun.enterprise.admin.util.TokenValueSet;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.glassfish.ASenvPropertyReader;
import com.sun.appserv.server.util.Version;
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.admin.remote.Metrix;

import javax.inject.Inject;
import javax.inject.Scope;
import javax.inject.Singleton;


/**
 * Base class for a CLI command.  An instance of a subclass of this
 * class is created using the getCommand method with the name of the
 * command and the information about its environment.
 * <p>
 * A command is executed with a list of arguments using the execute
 * method.  The implementation of the execute method in this class
 * saves the arguments in the protected argv field, then calls the
 * following protected methods in order: prepare, parse, validate,
 * and executeCommand.  A subclass must implement the prepare method
 * to initialize the metadata that specified the valid options for
 * the command, and the executeCommand method to actually perform the
 * command.  The parse and validate method may also be overridden if
 * needed.  Or, the subclass may override the execute method and
 * provide the complete implementation for the command, including
 * option parsing.
 *
 * @author Bill Shannon
 */
@Contract
@PerLookup
public abstract class CLICommand implements PostConstruct {
    public static final int ERROR = CLIConstants.ERROR;
    public static final int CONNECTION_ERROR = 2;
    public static final int INVALID_COMMAND_ERROR = 3;
    public static final int SUCCESS = CLIConstants.SUCCESS;
    public static final int WARNING = CLIConstants.WARNING;

    private static final Set<String> unsupported;
    private static final String UNSUPPORTED_CMD_FILE_NAME =
                                    "unsupported-legacy-command-names";
    private static final String PACKAGE_NAME = "com.sun.enterprise.admin.cli";

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(CLICommand.class);

    private static final Map<String,String> systemProps = 
            Collections.unmodifiableMap(new ASenvPropertyReader().getProps());

    protected static final Logger logger =
            Logger.getLogger(CLICommand.class.getPackage().getName());

    // InjectionManager is completely stateless with only one method that
    // operates on its arguments, so we can share a single instance.
    private static final InjectionManager injectionMgr = new InjectionManager();

    private static String commandScope = null;

    // tokens that are substituted in manual pages
    // the tokens are delimited with {}
    // the tokens and tokenValues arrays must be kept in sync.  See the 
    // expandManPage method for where the tokenValues are assigned.
    private static String manpageTokens[] = { 
        "cname",            // the command name
        "cprefix",          // the environment variable prefix
        "product---name",   // the product name
    };
    private String manpageTokenValues[] = new String[manpageTokens.length];


    /**
     * The name of the command.
     * Initialized in the constructor.
     */
    protected String name;

    /**
     * The program options for the command.
     * Initialized in the constructor.
     */
    @Inject
    protected ProgramOptions programOpts;

    /**
     * The environment for the command.
     * Initialized in the constructor.
     */
    @Inject
    protected Environment env;

    /**
     * The command line arguments for this execution.
     * Initialized in the execute method.
     */
    protected String[] argv;

    /**
     * The metadata describing the command's options and operands.
     */
    protected CommandModel commandModel;

    protected StringBuilder metadataErrors;

    /**
     * The options parsed from the command line.
     * Initialized by the parse method.  The keys
     * are the parameter names from the command model,
     * not the "forced to all lower case" names that
     * are presented to the user.
     */
    protected ParameterMap options;

    /**
     * The operands parsed from the command line.
     * Initialized by the parse method.
     */
    protected List<String> operands;

    /**
     * The passwords read from the password file.
     * Initialized by the initializeCommandPassword method.
     */
    protected Map<String, String> passwords;
    
    static {
        Set<String> unsup = new HashSet<String>();
        file2Set(UNSUPPORTED_CMD_FILE_NAME, unsup);
        unsupported = Collections.unmodifiableSet(unsup);
    }
    
    private static boolean useRest() {
        //return environment != null && environment.getBooleanOption("USE_REST");
        return true;
    }

    /**
     * Get a CLICommand object representing the named command.
     */
    public static CLICommand getCommand(ServiceLocator serviceLocator, String name)
            throws CommandException {
        // first, check if it's a known unsupported command
        checkUnsupportedLegacyCommand(name);
        // next, try to load our own implementation of the command
        ProgramOptions po = serviceLocator.getService(ProgramOptions.class);
        CLICommand cmd = serviceLocator.getService(CLICommand.class, name);
        if (cmd != null) {
            po.removeDetach();
            return cmd;
        }
        // nope, must be a remote command
        if (logger.isLoggable(Level.FINER))
            logger.finer("Assuming it's a remote command: " + name);
        return getRemoteCommand(name, po, serviceLocator.getService(Environment.class));
    }
    
    public static CLICommand getCommand(CLIContainer cLIContainer, String name)
            throws CommandException {
        // first, check if it's a known unsupported command
        checkUnsupportedLegacyCommand(name);
        // next, try to load our own implementation of the command
        ProgramOptions po = cLIContainer.getProgramOptions();
        CLICommand cmd = cLIContainer.getLocalCommand(name);
        if (cmd != null) {
            po.removeDetach();
            return cmd;
        }
        // nope, must be a remote command
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Assuming it's a remote command: " + name);
        }
        return getRemoteCommand(name, po, cLIContainer.getEnvironment());
    }
    
    private static CLICommand getRemoteCommand(String name, ProgramOptions po, Environment env) throws CommandException {
        if (useRest()) {
            return new RemoteCLICommand(name, po, env);
        } else {
            return new RemoteCommand(name, po, env);
        }
    }

    /**
     * Constructor used by subclasses when instantiated by HK2.
     * ProgramOptions and Environment are injected.  name is set here.
     */
    protected CLICommand() {
        Service service = this.getClass().getAnnotation(Service.class);

        if (service == null)
            name = "unknown-command";   // should never happen
        else
            name = service.name();
    }

    /**
     * Initialize the logger after being instantiated by HK2.
     */
    public void postConstruct() {
        initializeLogger();
    }

    /**
     * Constructor used by subclasses to save the name, program options,
     * and environment information into corresponding protected fields.
     * Finally, this constructor calls the initializeLogger method.
     */
    protected CLICommand(String name, ProgramOptions programOpts,
            Environment env) {
        this.name = name;
        this.programOpts = programOpts;
        this.env = env;
        initializeLogger();
    }

    /**
     * Execute this command with the given arguemnts.
     * The implementation in this class saves the passed arguments in
     * the argv field and calls the initializePasswords method.
     * Then it calls the prepare, parse, and validate methods, finally
     * returning the result of calling the executeCommand method.
     * Note that argv[0] is the command name.
     *
     * @throws CommandException if execution of the command fails
     * @throws CommandValidationException if there's something wrong
     *          with the options or arguments
     */
    public int execute(String... argv) throws CommandException {
        this.argv = argv;
        initializePasswords();
        logger.finer("Prepare");
        prepare();
        logger.finer("Process program options");
        processProgramOptions();
        logger.finer("Parse command options");
        parse();
        if (checkHelp())
            return 0;
        logger.finer("Prevalidate command options");
        prevalidate();
        logger.finer("Inject command options");
        inject();
        logger.finer("Validate command options");
        validate();
        if (programOpts.isEcho()) {
            logger.info(echoCommand());
            // In order to avoid echoing commands used intenally to the
            // implementation of *this* command, we turn off echo after
            // having echoed this command.
            programOpts.setEcho(false);
        } else if (logger.isLoggable(Level.FINER))
            logger.finer(echoCommand());
        logger.finer("Execute command");
        return executeCommand();
    }

    /**
     * Return the name of this command.
     */
    public String getName() {
        return name;
    }
    
    /*
     * Return the command scope for this command.  The command scope is 
     * a name space in which commands are defined. Command clients can specify a scope
     * to use in looking up a command. Currently this is only used for remote
     * commands. By default, the context is null.
     */
    public static String getCommandScope() {
        return commandScope;
    }

    /*
     * Set the command scope for this command.  
     */
    public static void setCommandScope(String ctx) {
        commandScope = ctx;
    }

    /**
     * Returns the program options associated with this command.
     * 
     * @return the command's program options
     */
    public ProgramOptions getProgramOptions() {
        return programOpts;
    }

    /**
     * Return a BufferedReader for the man page for this command,
     * or null if not found.
     */
    public BufferedReader getManPage() {
        String commandName = getName();
        if (commandName.length() == 0)
            throw new IllegalArgumentException("Command name cannot be empty");

        // special case "help" --> help for the command
        if (commandName.equals("help"))
            commandName = programOpts.getCommandName();

        return ManPageFinder.getCommandManPage(
                                commandName,
                                getClass().getName(),
                                Locale.getDefault(),
                                getClass().getClassLoader(),
                                logger);
    }
    
    /**
     * Return a man page for this command that has the tokens substituted
     */
    public BufferedReader expandManPage(Reader r) {
        manpageTokenValues[0] = programOpts.getCommandName();
        manpageTokenValues[1] = Environment.getPrefix();
        manpageTokenValues[2] = Version.getBriefProductName();
        TokenValueSet tvs = new TokenValueSet();
        for (int i = 0; i < manpageTokens.length; i++) {
            tvs.add(new TokenValue(manpageTokens[i], manpageTokenValues[i], "{", "}"));
        }
        return new BufferedReader(new LineTokenReplacer(tvs).getReader(r));
    }

    /**
     * Get the usage text for the subcommand. This method shows the details for
     * the subcommand options but does not provide details about the command 
     * options.
     *
     * @return usage text
     */
    public String getUsage() {
        String usage;
        if (commandModel != null && ok(usage = commandModel.getUsageText())) {
            StringBuffer usageText = new StringBuffer();
            usageText.append(
                strings.get("Usage", strings.get("Usage.brief", programOpts.getCommandName())));
            usageText.append(" ");
            usageText.append(usage);
            return usageText.toString();
        } else {
            return generateUsageText();
        }
    }

    private String generateUsageText() {
        StringBuilder usageText = new StringBuilder();
        usageText.append(strings.get("Usage", strings.get("Usage.brief", programOpts.getCommandName())));
        usageText.append(" ");
        usageText.append(getName());
        int len = usageText.length();
        StringBuilder optText = new StringBuilder();
        String lsep = System.getProperty("line.separator");
        for (ParamModel opt : usageOptions()) {
            optText.setLength(0);
            final String optName = lc(opt.getName());
            // "--terse" is part of asadmin utility options
            if (optName.equals("terse"))
                continue;
            // skip "hidden" options
            if (optName.startsWith("_"))
                continue;
            // do not want to display password as an option
            if (opt.getParam().password())
                continue;
            // also do not want to display obsolete options
            if (opt.getParam().obsolete())
                continue;
            // primary parameter is the operand, not an option
            if (opt.getParam().primary())
                continue;
            boolean optional = opt.getParam().optional();
            String defValue = opt.getParam().defaultValue();
            if (optional)
                optText.append("[");
            String sn = opt.getParam().shortName();
            if (ok(sn))
                optText.append('-').append(sn).append('|');
            optText.append("--").append(optName);

            if (opt.getType() == Boolean.class ||
                opt.getType() == boolean.class) {
                // canonicalize default value
                if (ok(defValue) && Boolean.parseBoolean(defValue))
                    defValue = "true";
                else
                    defValue = "false";
                optText.append("[=<").append(optName);
                optText.append(strings.get("Usage.default", defValue));
                optText.append(">]");
            } else {    // STRING or FILE
                if (ok(defValue)) {
                    optText.append(" <").append(optName);
                    optText.append(strings.get("Usage.default", defValue));
                    optText.append('>');
                } else
                    optText.append(" <").append(optName).append('>');
            }
            if (optional)
                optText.append("]");

            if (len + 1 + optText.length() > 80) {
                usageText.append(lsep).append('\t');
                len = 8;
            } else {
                usageText.append(' ');
                len++;
            }
            usageText.append(optText);
            len += optText.length();
        }

        // add --help text
        String helpText = "[-?|--help[=<help(default:false)>]]";
        if (len + 1 + helpText.length() > 80) {
            usageText.append(lsep).append('\t');
            len = 8;
        } else {
            usageText.append(' ');
            len++;
        }
        usageText.append(helpText);
        len += helpText.length();

        optText.setLength(0);
        ParamModel operandParam = getOperandModel();
        String opname = operandParam != null ?
	    	lc(operandParam.getName()) : null;
        if (!ok(opname))
            opname = "operand";

        int operandMin = 0;
        int operandMax = 0;
        if (operandParam != null) {
            operandMin = operandParam.getParam().optional() ? 0 : 1;
            operandMax = operandParam.getParam().multiple() ?
                                                        Integer.MAX_VALUE : 1;
        }
        if (operandMax > 0) {
            if (operandMin == 0) {
                optText.append("[").append(opname);
                if (operandMax > 1)
                    optText.append(" ...");
                optText.append("]");
            } else {
                optText.append(opname);
                if (operandMax > 1)
                    optText.append(" ...");
            }
        }
        if (len + 1 + optText.length() > 80) {
            usageText.append(lsep).append('\t');
            len = 8;
        } else {
            usageText.append(' ');
            len++;
        }
        usageText.append(optText);
        return usageText.toString();
    }

    /**
     * Subclasses can override this method to supply additional
     * or different options that should be part of the usage text.
     * Most commands will never need to do this, but the create-domain
     * command uses it to include the --user option as a required option.
     */
    protected Collection<ParamModel> usageOptions() {
        return commandModel.getParameters();
    }
    
    /**
     * Get the usage text for the command.  This usage text shows the details
     * of the command options but does not show the details for the subcommand
     * options. The subcommand argument is used to fill in the subcommand name
     * in the usage text.
     * @return usage text for the command
     */
    public String getCommandUsage() {
        return strings.get("Usage.full", programOpts.getCommandName());
    }
    
    public String getBriefCommandUsage() {
        return strings.get("Usage.brief", programOpts.getCommandName());
    }

    @Override
    public String toString() {
        return echoCommand();
    }


    /**
     * Return a string representing the command line used with this command.
     */
    private String echoCommand() {
        StringBuilder sb = new StringBuilder();

        // first, the program options
        sb.append(programOpts.getCommandName());
        sb.append(' ');
        sb.append(programOpts.toString()).append(' ');

        // now the subcommand options and operands
        sb.append(name).append(' ');

        // have we parsed any options yet?
        if (options != null && operands != null) {
            for (ParamModel opt : commandModel.getParameters()) {
                if (opt.getParam().password())
                    continue;       // don't print passwords
                if (opt.getParam().primary())
                    continue;
                // include every option that was specified on the command line
                // and every option that has a default value                               
                if (opt.getParam().multiple()) {
                    List<String> paramValues = getOptions(opt.getName());
                    for (String v : paramValues) {
                        appendEchoOption(sb, opt, v);
                    }             
                } else {    
                    String value = getOption(opt.getName());
                    if (value != null) {
                        appendEchoOption(sb, opt, value);
                    }
                }
            }
            for (String o : operands)
                sb.append(quote(o)).append(' ');
        } else if (argv != null) {
            // haven't parsed any options, include raw arguments, if any
            for (String arg : argv)
                sb.append(quote(arg)).append(' ');
        }

        sb.setLength(sb.length() - 1);  // strip trailing space
        return sb.toString();
    }
    
    private void appendEchoOption(StringBuilder sb, ParamModel opt, String value) {
        sb.append("--").append(lc(opt.getName()));
        if (opt.getType() == Boolean.class ||
            opt.getType() == boolean.class) {
            sb.append("=").append(Boolean.toString(Boolean.parseBoolean(value))); 
        } else {    // STRING or FILE
            sb.append(" ").append(quote(value));
        }
        sb.append(' ');
    }

    /**
     * Quote a value, if the value contains any special characters.
     *
     * @param   value   value to be quoted
     * @return          the possibly quoted value
     */
    public static String quote(String value) {
        int len = value.length();
        if (len == 0)
            return "\"\"";      // an empty string is handled specially

        /*
         * Look for any special characters.  Escape and
         * quote the entire string if necessary.
         */
        boolean needQuoting = false;
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\' || c == '\r' || c == '\n') {
                // need to escape them and then quote the whole string
                StringBuffer sb = new StringBuffer(len + 3);
                sb.append('"');
                sb.append(value.substring(0, i));
                int lastc = 0;
                for (int j = i; j < len; j++) {
                    char cc = value.charAt(j);
                    if ((cc == '"') || (cc == '\\') || 
                        (cc == '\r') || (cc == '\n'))
                        if (cc == '\n' && lastc == '\r')
                            ;   // do nothing, CR was already escaped
                        else
                            sb.append('\\');    // Escape the character
                    sb.append(cc);
                    lastc = cc;
                }
                sb.append('"');
                return sb.toString();
            } else if (c <= 040 || c >= 0177)
                // These characters cause the string to be quoted
                needQuoting = true;
        }

        if (needQuoting) {
            StringBuffer sb = new StringBuffer(len + 2);
            sb.append('"').append(value).append('"');
            return sb.toString();
        } else 
            return value;
    }

    /**
     * If the program options haven't already been set, parse them
     * on the command line and remove them from the command line.
     * Subclasses should call this method in their prepare method
     * after initializing commandOpts (so usage is available on failure)
     * if they want to allow program options after the command name.
     * Currently RemoteCommand does this, as well as the local commands
     * that also need to talk to the server.
     */
    protected void processProgramOptions() throws CommandException {
        /*
         * asadmin options and command options are intermixed.
         * Parse the entire command line for asadmin options,
         * removing them from the command line, and ignoring
         * unknown options.
         */
        Collection<ParamModel> model = ProgramOptions.getValidOptions();
        if (programOpts.isOptionsSet()) {
            model = ProgramOptions.getHelpOption();
        }
        Parser rcp = new Parser(argv, 0, model, true);
        ParameterMap params = rcp.getOptions();
        List<String> oprds = rcp.getOperands();
        argv = oprds.toArray(new String[oprds.size()]);
        if (params.size() > 0) {
            // at least one program option specified after command name
            logger.finer("Update program options");
            programOpts.updateOptions(params);
            initializeLogger();
            initializePasswords();
            if (!programOpts.isTerse() &&
                    !(params.size() == 1 && params.get("help") != null)) {
                // warn about deprecated use of program options
                // (except --help)
                // XXX - a lot of work for a nice message...
                Collection<ParamModel> programOptions =
                        ProgramOptions.getValidOptions();
                StringBuilder sb = new StringBuilder();
                sb.append(programOpts.getCommandName());
                for (Map.Entry<String,List<String>> p : params.entrySet()) {
                    // find the corresponding ParamModel
                    ParamModel opt = null;
                    for (ParamModel vo : programOptions) {
                        if (vo.getName().equalsIgnoreCase(p.getKey())) {
                            opt = vo;
                            break;
                        }
                    }
                    if (opt == null) {
                        continue;
                    }

                    // format the option appropriately
                    sb.append(" --").append(p.getKey());
                    List<String> pl = p.getValue();
                    // XXX - won't handle multi-values
                    if (opt.getType() == Boolean.class ||
                        opt.getType() == boolean.class) {
                        if (!pl.get(0).equalsIgnoreCase("true")) {
                            sb.append("=false");
                        }
                    } else if (pl != null && pl.size() > 0) {
                        sb.append(" ").append(pl.get(0));
                    }
                }
                sb.append(" ").append(name).append(" [options] ...");
                logger.info(strings.get("DeprecatedSyntax"));
                logger.info(sb.toString());
            }
        }
    }

    /**
     * Initialize the state of the logger based on any program options.
     */
    protected void initializeLogger() {
        if (!logger.isLoggable(Level.FINER)) {
            if (programOpts.isTerse())
                logger.setLevel(Level.INFO);
            else
                logger.setLevel(Level.FINE);
        }
    }

    /**
     * Initialize the passwords field based on the password
     * file specified in the program options, and initialize the
     * program option's password if available in the password file.
     */
    protected void initializePasswords() throws CommandException {
        passwords = new HashMap<String, String>();
        String pwfile = programOpts.getPasswordFile();

        if (ok(pwfile)) {
            passwords = CLIUtil.readPasswordFileOptions(pwfile, true);
            logger.finer("Passwords were read from password file: " +
                                        pwfile);
            String password = passwords.get(
                    Environment.getPrefix() + "PASSWORD");
            if (password != null && programOpts.getPassword() == null)
                programOpts.setPassword(password,
                    ProgramOptions.PasswordLocation.PASSWORD_FILE);
        }
    }

    /**
     * The prepare method must ensure that the commandModel field is set.
     */
    protected void prepare() throws CommandException {
        commandModel = new CommandModelImpl(this.getClass());
    }

    /**
     * The parse method sets the options and operands fields
     * based on the content of the command line arguments.
     * If the program options say this is a help request,
     * we set options and operands as if "--help" had been specified.
     *
     * @throws CommandException if execution of the command fails
     * @throws CommandValidationException if there's something wrong
     *          with the options or arguments
     */
    protected void parse() throws CommandException {
        /*
         * If this is a help request, we don't need the command
         * metadata and we throw away all the other options and
         * fake everything else.
         */
        if (programOpts.isHelp()) {
            options = new ParameterMap();
            options.set("help", "true");
            operands = Collections.emptyList();
        } else {
            Parser rcp =
                new Parser(argv, 1, commandModel.getParameters(),
                    commandModel.unknownOptionsAreOperands());
            options = rcp.getOptions();
            operands = rcp.getOperands();

            /*
             * In the case where we're accepting unknown options as
             * operands, the special "--" delimiter will also be
             * accepted as an operand.  We eliminate it here.
             */
            if (commandModel.unknownOptionsAreOperands() &&
                    operands.size() > 0 && operands.get(0).equals("--"))
                operands.remove(0);
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("params: " + options);
            logger.finer("operands: " + operands);
        }
    }

    /**
     * Check if the current request is a help request, either because
     * --help was specified as a program option or a command option.
     * If so, get the man page using the getManPage method, copy the
     * content to System.out, and return true.  Otherwise return false.
     * Subclasses may override this method to perform a different check
     * or to use a different method to display the man page.
     * If this method returns true, the validate and executeCommand methods
     * won't be called.
     */
    protected boolean checkHelp() throws CommandException {
        if (programOpts.isHelp()) {
            BufferedReader br = getManPage();
            if (br == null) {
                throw new CommandException(strings.get("ManpageMissing", name));
            }
            br = expandManPage(br);
            String line;
            try {
                while ((line = br.readLine()) != null)
                    System.out.println(line);
            } catch (IOException ioex) {
                throw new CommandException(
                            strings.get("ManpageMissing", name), ioex);
            } finally {
                try {
                    br.close();
                } catch (IOException ex) {
                }
            }
            return true;
        } else
            return false;
    }
    
    private Class<? extends Annotation> getScope(Class<?> onMe) {
    	if (onMe == null) return null;
    	
    	for (Annotation anno : onMe.getAnnotations()) {
    		if (anno.annotationType().isAnnotationPresent(Scope.class)) {
    			return anno.annotationType();
    		}
    	}
    	
    	return null;
    }

    /**
     * The prevalidate method supplies missing options from
     * the environment.  It also supplies passwords from the password
     * file or prompts for them if interactive.
     *
     * @throws CommandException if execution of the command fails
     * @throws CommandValidationException if there's something wrong
     *          with the options or arguments
     */
    protected void prevalidate() throws CommandException {
        /*
         * First, check that the command has the proper scope.
         * (Could check this in getCommand(), but at that point we
         * don't have the CommandModel yet.)
         * Remote commands are checked on the server.
         */
        if (!(this instanceof RemoteCommand) && !(this instanceof RemoteCLICommand)) {
        	Class<? extends Annotation> myScope = getScope(this.getClass());
            if (myScope == null) {
                throw new CommandException(strings.get("NoScope", name));
            } else if (Singleton.class.equals(myScope)) {
                // check that there are no parameters for this command
                if (commandModel.getParameters().size() > 0) {
                    throw new CommandException(strings.get("HasParams", name));
                }
            }
        }

        /*
         * Check for missing options and operands.
         */
        Console cons = programOpts.isInteractive() ? System.console() : null;

        boolean missingOption = false;
        for (ParamModel opt : commandModel.getParameters()) {
            if (opt.getParam().password())
                continue;       // passwords are handled later
	    if (opt.getParam().obsolete() && getOption(opt.getName()) != null)
		logger.info(
			strings.get("ObsoleteOption", opt.getName()));
            if (opt.getParam().optional())
                continue;
            if (opt.getParam().primary())
                continue;
            // if option isn't set, prompt for it (if interactive)
            if (getOption(opt.getName()) == null && cons != null &&
                    !missingOption) {
                cons.printf("%s",
                    strings.get("optionPrompt", lc(opt.getName())));
                String val = cons.readLine();
                if (ok(val))
                    options.set(opt.getName(), val);
            }
            // if it's still not set, that's an error
            if (getOption(opt.getName()) == null) {
                missingOption = true;
                logger.info(
                        strings.get("missingOption", "--" + opt.getName()));
            }
	    if (opt.getParam().obsolete())	// a required obsolete option?
		logger.info(
			strings.get("ObsoleteOption", opt.getName()));
        }
        if (missingOption)
            throw new CommandValidationException(
                    strings.get("missingOptions", name));

        int operandMin = 0;
        int operandMax = 0;
        ParamModel operandParam = getOperandModel();
        if (operandParam != null) {
            operandMin = operandParam.getParam().optional() ? 0 : 1;
            operandMax = operandParam.getParam().multiple() ?
                                                        Integer.MAX_VALUE : 1;
        }

        if (operands.size() < operandMin && cons != null) {
            cons.printf("%s",
                strings.get("operandPrompt", operandParam.getName()));
            String val = cons.readLine();
            if (ok(val)) {
                operands = new ArrayList<String>();
                operands.add(val);
            }
        }
        if (operands.size() < operandMin)
            throw new CommandValidationException(
                    strings.get("notEnoughOperands", name,
                                operandParam.getType()));
        if (operands.size() > operandMax) {
            if (operandMax == 0)
                throw new CommandValidationException(
                    strings.get("noOperandsAllowed", name));
            else if (operandMax == 1)
                throw new CommandValidationException(
                    strings.get("tooManyOperands1", name));
            else
                throw new CommandValidationException(
                    strings.get("tooManyOperands", name, operandMax));
        }

        initializeCommandPassword();
    }

    /**
     * Inject this instance with the final values of all the command
     * parameters.
     *
     * @throws CommandException if execution of the command fails
     * @throws CommandValidationException if there's something wrong
     *          with the options or arguments
     */
    protected void inject() throws CommandException {
        // injector expects operands to be in the ParameterMap with the key
        // "DEFAULT"
        options.set("DEFAULT", operands);

        // if command has a "terse" option, set it from ProgramOptions
        if (commandModel.getModelFor("terse") != null)
            options.set("terse", Boolean.toString(programOpts.isTerse()));

        // initialize the injector.
        InjectionResolver<Param> injector =
                    new MapInjectionResolver(commandModel, options);

        // inject
        try {
            injectionMgr.inject(this, injector);
        } catch (UnsatisfiedDependencyException e) {
            throw new CommandValidationException(e.getMessage(), e);
        }
    }

    /**
     * The validate method can be used by a subclass to validate
     * that the type and quantity of parameters and operands matches
     * the requirements for this command.
     *
     * @throws CommandException if execution of the command fails
     * @throws CommandValidationException if there's something wrong
     *          with the options or arguments
     */
    protected void validate() throws CommandException {
    }

    /**
     * Execute the command using the options in options and the
     * operands in operands.
     *
     * @return the exit code
     * @throws CommandException if execution of the command fails
     * @throws CommandValidationException if there's something wrong
     *          with the options or arguments
     */
    protected abstract int executeCommand() throws CommandException;

    /**
     * Initialize all the passwords required by the command.
     *
     * @throws CommandException
     */
    private void initializeCommandPassword() throws CommandException {
        /*
         * Go through all the valid options and check for required password
         * options that weren't specified in the password file.  If option
         * is missing and we're interactive, prompt for it.  Store the
         * password as if it was a parameter.
         */
        for (ParamModel opt : commandModel.getParameters()) {
            if (!opt.getParam().password())
                continue;
            String pwdname = opt.getName();
            String pwd = getPassword(opt, null, true);
            if (pwd == null) {
                if (opt.getParam().optional())
                    continue;       // not required, skip it
                // if not terse, provide more advice about what to do
                String msg;
                if (programOpts.isTerse())
                    msg = strings.get("missingPassword", name, passwordName(opt));
                else
                    msg = strings.get("missingPasswordAdvice", name, passwordName(opt));
                throw new CommandValidationException(msg);
            }
            options.set(pwdname, pwd);
        }
    }

    protected String getPassword(String paramname, String localizedPrompt, 
            String localizedPromptConfirm, boolean create) throws CommandValidationException {
        ParamModelData po = new ParamModelData(paramname, String.class, false, null);
        po.prompt = localizedPrompt;
        po.promptAgain = localizedPromptConfirm;
        po.param._password = true;
        return getPassword(po, null, create);
    }
    
    /**
     * Get a password for the given option.
     * First, look in the passwords map.  If found, return it.
     * If not found, and not required, return null;
     * If not interactive, return null.  Otherwise, prompt for the
     * password.  If create is true, prompt twice and compare the two values
     * to make sure they're the same.  If the password meets other validity
     * criteria (i.e., length) returns the password.  If defaultPassword is
     * not null, "Enter" selects this default password, which is returned.
     */
    protected String getPassword(ParamModel opt, String defaultPassword,
            boolean create) throws CommandValidationException {

        String passwordName = passwordName(opt);
        String password = passwords.get(passwordName);
        if (password != null)
            return password;

        if (opt.getParam().optional())
            return null;        // not required

        if (!programOpts.isInteractive())
            return null;        // can't prompt for it

        String prompt = null;
        String promptAgain = null;
        if (opt instanceof ParamModelData) {
            prompt = ((ParamModelData)opt).getPrompt();
            promptAgain = ((ParamModelData)opt).getPromptAgain();
        }
        String newprompt;
        if (ok(prompt)) {
            if (defaultPassword != null) {
                if (defaultPassword.length() == 0)
                    newprompt =
                        strings.get("NewPasswordDescriptionDefaultEmptyPrompt",
                                            prompt);
                else
                    newprompt =
                        strings.get("NewPasswordDescriptionDefaultPrompt",
                                            prompt, defaultPassword);
            } else
                newprompt =
                    strings.get("NewPasswordDescriptionPrompt", prompt);
        } else {
            if (defaultPassword != null) {
                if (defaultPassword.length() == 0)
                    newprompt =
                        strings.get("NewPasswordDefaultEmptyPrompt",
                                            passwordName);
                else
                    newprompt =
                        strings.get("NewPasswordDefaultPrompt",
                                            passwordName, defaultPassword);
            } else
                newprompt = strings.get("NewPasswordPrompt", passwordName);
        }

        String newpassword = readPassword(newprompt);

        /*
         * If we allow for a default password, and the user just hit "Enter",
         * return the default password.  No need to prompt twice or check
         * for validity.
         */
        if (defaultPassword != null) {
            if (newpassword == null)
                newpassword = "";
            if (newpassword.length() == 0) {
                newpassword = defaultPassword;
                passwords.put(passwordName, newpassword);
                return newpassword;
            }
        }

        /*
         * If not creating a new password, don't need to verify that
         * the user typed it correctly by making them type it twice,
         * and don't need to check it for validity.  Just return what
         * we have.
         */
        if (!create) {
            passwords.put(passwordName, newpassword);
            return newpassword;
        }

        String confirmationPrompt;
        if (ok(promptAgain)) {
            confirmationPrompt =
                strings.get("NewPasswordDescriptionPrompt", promptAgain);
        } else {
            confirmationPrompt =
                strings.get("NewPasswordConfirmationPrompt", passwordName);
        }
        String newpasswordAgain = readPassword(confirmationPrompt);
        if (!newpassword.equals(newpasswordAgain)) {
            throw new CommandValidationException(
                strings.get("OptionsDoNotMatch",
                            ok(prompt) ? prompt : passwordName));
        }
        passwords.put(passwordName, newpassword);
        return newpassword;
    }

    private String passwordName(ParamModel opt) {
        return Environment.getPrefix() + opt.getName().toUpperCase(Locale.ENGLISH);
    }

    /**
     * Display the given prompt and read a password without echoing it.
     * Returns null if no console available.
     */
    protected String readPassword(String prompt) {
        String password = null;
        Console cons = System.console();
        if (cons != null) {
            char[] pc = cons.readPassword("%s", prompt);
            // yes, yes, yes, it would be safer to not keep it in a String
            password = new String(pc);
        }
        return password;
    }

    /**
     * Get the ParamModel that corresponds to the operand
     * (primary parameter).  Return null if none.
     */
    protected ParamModel getOperandModel() {
        for (ParamModel pm : commandModel.getParameters()) {
            if (pm.getParam().primary())
                return pm;
        }
        return null;
    }

    /**
     * Get an option value, that might come from the command line
     * or from the environment.  Return the default value for the
     * option if not otherwise specified.
     */
    protected String getOption(String name) {
        String val = options.getOne(name);
        if (val == null) {
            val = env.getStringOption(name);
        }
        if (val == null) {
            // no value, find the default
            ParamModel opt = commandModel.getModelFor(name);
            // if no value was specified and there's a default value, return it
            if (opt != null) {
                String def = opt.getParam().defaultValue();
                if (ok(def)) {
                    val = def;
                }
            }
        }
        return val;
    }

    /**
     * Get option values, that might come from the command line
     * or from the environment.  Return the default value for the
     * option if not otherwise specified. This method works with options
     * for with multiple() is true.
     */
    protected List<String> getOptions(String name) {
        List<String> val = options.get(name);
        if (val.isEmpty()) {
            String v = env.getStringOption(name);
            if (v != null) {
                val.add(v);
            }
        }
        if (val.isEmpty()) {
            // no value, find the default
            ParamModel opt = commandModel.getModelFor(name);
            // if no value was specified and there's a default value, return it
            if (opt != null) {
                String def = opt.getParam().defaultValue();
                if (ok(def)) {
                    val.add(def);
                }
            }
        }
        return val;
    }
    /**
     * Get a boolean option value, that might come from the command line
     * or from the environment.
     */
    protected boolean getBooleanOption(String name) {
        String val = getOption(name);
        return val != null && Boolean.parseBoolean(val);
    }

    /**
     * Return the named system property, or property
     * set in asenv.conf.
     */
    protected String getSystemProperty(String name) {
        return systemProps.get(name);
    }

    /**
     * Return all the system properties and properties set
     * in asenv.conf.  The returned Map may not be modified.
     */
    protected Map<String,String> getSystemProperties() {
        return systemProps;
    }

    /**
     * If this is an unsupported command, throw an exception.
     */
    private static void checkUnsupportedLegacyCommand(String cmd)
            throws CommandException {
        for (String c : unsupported) {
            if (c.equals(cmd)) {
                throw new CommandException(
                            strings.get("UnsupportedLegacyCommand", cmd));
            }
        }
        // it is a supported command; do nothing
    }

    /**
     * Prints the exception message with level as FINER.
     *
     * @param e the exception object to print
     */
    protected void printExceptionStackTrace(java.lang.Throwable e) {
        if (logger.isLoggable(Level.FINER)) {
            /*
            java.lang.StackTraceElement[] ste = e.getStackTrace();
            for (int ii = 0; ii < ste.length; ii++)
                printDebugMessage(ste[ii].toString());
            */
            final ByteArrayOutputStream output = new ByteArrayOutputStream(512);
            e.printStackTrace(new java.io.PrintStream(output));
            try {
                output.close();
            } catch (IOException ex) {
                // ignore
            }
            logger.finer(output.toString());
        }
    }

    protected static boolean ok(String s) {
        return s != null && s.length() > 0;
    }

    // shorthand for this too-verbose operation
    private static String lc(String s) {
    	return s.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Read the named resource file and add the first token on each line
     * to the set.  Skip comment lines.
     */
    private static void file2Set(String file, Set<String> set) {
        BufferedReader reader = null;
        try {
            InputStream is = CLICommand.class.getClassLoader().
                                getResourceAsStream(file);
            if (is == null)
                return;     // in case the resource doesn't exist
            reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#"))
                    continue; // # indicates comment
                StringTokenizer tok = new StringTokenizer(line, " ");
                // handles with or without space, rudimendary as of now
                String cmd = tok.nextToken();
                set.add(cmd);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ee) {
                    // ignore
                }

            }
        }
    }
}
