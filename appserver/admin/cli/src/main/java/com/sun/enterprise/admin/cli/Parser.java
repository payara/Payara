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

package com.sun.enterprise.admin.cli;

import java.io.*;
import java.util.*;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.CommandModel.ParamModel;
import com.sun.enterprise.admin.util.*;
import com.sun.enterprise.admin.util.CommandModelData.ParamModelData;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;


/**
 * The <code>Parser</code> object is used to parse the
 * command line and verify that the command line is CLIP compliant.
 */
public class Parser {
    // MultiMap of options and values from command-line
    private ParameterMap optionsMap = new ParameterMap();
    
    // Array of operands from command-line
    private List<String> operands = new ArrayList<String>();

    // The valid options for the command we're parsing
    private Collection<ParamModel> options;

    // Ignore unknown options when parsing?
    private boolean ignoreUnknown;

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(Parser.class);

    /*
     * TODO:
     *  option types shouldn't be string literals here
     */

    /**
     * Parse the given command line arguments
     *
     * @param args  command line arguments
     * @param start index in args to start parsing
     * @param options the valid options to consider while parsing
     * @param ignoreUnknown if true, unknown options are considered operands
     *        instead of generating an exception
     * @throws CommandValidationException if command line parsing fails
     */
    public Parser(String[] args, int start,
            Collection<ParamModel> options, boolean ignoreUnknown)
            throws CommandValidationException {
        this.options = options;
        this.ignoreUnknown = ignoreUnknown;
        parseCommandLine(args, start);
    }

    /**
     * Parse the command line arguments according to CLIP.
     *
     * @param argv  command line arguments
     * @throws CommandValidationException if command line is invalid
     */
    private void parseCommandLine(final String[] argv, final int start)
        throws CommandValidationException {

        for (int si = start; si < argv.length; si++) {
            String arg = argv[si];
            if (arg.equals("--")) {             // end of options
                // if we're ignoring unknown options, we include this
                // delimiter as an operand, it will be eliminated later
                // when we process all remaining options
                if (!ignoreUnknown)
                    si++;
                while (si < argv.length)
                    operands.add(argv[si++]);
                break;
            }

            // is it an operand or option value?
            if (!arg.startsWith("-") || arg.length() <= 1) {
                operands.add(arg);
                if (ignoreUnknown)
                    continue;
                si++;
                while (si < argv.length)
                    operands.add(argv[si++]);
                break;
            }

            // at this point it's got to be an option of some sort
            ParamModel opt = null;
            String name = null;
            String value = null;
            if (arg.charAt(1) == '-') { // long option
                int ns = 2;
                boolean sawno = false;
                if (arg.startsWith("--no-")) {
                    sawno = true;
                    value = "false";
                    ns = 5;             // skip prefix
                }
                // if of the form "--option=value", extract value
                int ne = arg.indexOf('=');
                if (ne < 0)
                    name = arg.substring(ns);
                else {
                    if (value != null)
                        throw new CommandValidationException(
                            strings.get("parser.noValueAllowed", arg));
                    name = arg.substring(ns, ne);
                    value = arg.substring(ne + 1);
                }
                opt = lookupLongOption(name);
                if (sawno && optionRequiresOperand(opt))
                    throw new CommandValidationException(
                        strings.get("parser.illegalNo", opt.getName()));
            } else {                            // short option
                /*
                 * possibilities are:
                 *      -f
                 *      -f value
                 *      -f=value
                 *      -fxyz   (multiple single letter boolean options
                 *              with no arguments)
                 */
                if (arg.length() <= 2) { // one of the first two cases
                    opt = lookupShortOption(arg.charAt(1));
                    name = arg.substring(1);
                } else {                        // one of the last two cases
                    if (arg.charAt(2) == '=') { // -f=value case
                        opt = lookupShortOption(arg.charAt(1));
                        value = arg.substring(3);
                    } else {                            // -fxyz case
                        for (int i = 1; i < arg.length(); i++) {
                            opt = lookupShortOption(arg.charAt(i));
                            if (opt == null) {
                                if (!ignoreUnknown)
                                    throw new CommandValidationException(
                                        strings.get("parser.invalidOption",
                                        Character.toString(arg.charAt(i))));
                                // unknown option, skip all the rest
                                operands.add(arg);
                                break;
                            }
                            if (opt.getType() == Boolean.class ||
                                opt.getType() == boolean.class)
                                setOption(opt, "true");
                            else {
                                if (!ignoreUnknown)
                                    throw new CommandValidationException(
                                      strings.get("parser.nonbooleanNotAllowed",
                                      Character.toString(arg.charAt(i)), arg));
                                // unknown option, skip all the rest
                                operands.add(arg);
                                break;
                            }
                        }
                        continue;
                    }
                }
            }

            // is it a known option?
            if (opt == null) {
                if (!ignoreUnknown)
                    throw new CommandValidationException(
                        strings.get("parser.invalidOption", arg));
                // unknown option, skip it
                operands.add(arg);
                continue;
            }

            // find option value, if needed
            if (value == null) {
                // if no valid options were specified, we use the next argument
                // as an option as long as it doesn't look like an option
                if (options == null) {
                    if (si + 1 < argv.length && !argv[si + 1].startsWith("-"))
                        value = argv[++si];
                    else
                        ((ParamModelData)opt).type = Boolean.class; // fake it
                } else if (optionRequiresOperand(opt)) {
                    if (++si >= argv.length)
                        throw new CommandValidationException(
                            strings.get("parser.missingValue", name));
                    value = argv[si];
                } else if (opt.getType() == Boolean.class ||
                            opt.getType() == boolean.class) {
                    /*
                     * If it's a boolean option, the following parameter
                     * might be the value for the option; peek ahead to
                     * see if it looks like a boolean value.
                     */
                    if (si + 1 < argv.length) {
                        String val = argv[si + 1];
                        if (val.equalsIgnoreCase("true") ||
                                val.equalsIgnoreCase("false")) {
                            // yup, it's a boolean value, consume it
                            si++;
                            value = val;
                        }
                    }
                }
            }
            setOption(opt, value);
        }
    }

    /**
     * Returns a Map with all the options.
     * The Map is indexed by the long name of the option.
     *
     * @return options
     */
    public ParameterMap getOptions() {
        return optionsMap;
    }

    /**
     * Returns the list of operands.
     *
     * @return list of operands
     */
    public List<String> getOperands() {
        return operands;
    }

    public String toString() {
        return "CLI parser: Options = " + optionsMap +
                "; Operands = " + operands;
    }
    
    /**
     * Get ParamModel for long option name.
     */
    private ParamModel lookupLongOption(String s) {
	if (s == null || s.length() == 0)
	    return null;
        // XXX - for now, fake it if no options
        if (options == null) {
            // no valid options specified so everything is valid
            return new ParamModelData(s, String.class, true, null);
        }
        for (ParamModel od : options) {
            if (od.getParam().primary())
		continue;
            if (s.equalsIgnoreCase(od.getName()))
                return od;
	    if (s.equalsIgnoreCase(od.getParam().alias()))
                return od;
        }
        return null;
    }

    /**
     * Get ParamModel for short option name.
     */
    private ParamModel lookupShortOption(char c) {
        // XXX - for now, fake it if no options
        if (options == null)
            return null;
        String sc = Character.toString(c);
        for (ParamModel od : options) {
            if (od.getParam().shortName().equals(sc))
                return od;
        }
        return null;
    }

    /**
     * Does this option require an operand?
     */
    private static boolean optionRequiresOperand(ParamModel opt) {
        return opt != null && opt.getType() != Boolean.class &&
                                opt.getType() != boolean.class;
    }

    /**
     * Set the value for the option.
     */
    private void setOption(ParamModel opt, String value)
            throws CommandValidationException {
        // VERY basic validation
        if (opt == null)
            throw new NullPointerException("null option name");
        if (value != null)
            value = value.trim();

        String name = opt.getName();
        if (opt.getType() == File.class) {
            File f = new File(value);
            // allow the pseudo-file name of "-" to mean stdin
            if (!value.equals("-") && !(f.isFile() || f.canRead())) {
                // get a real exception for why it's no good
                InputStream is = null;
                try {
                    is = new FileInputStream(f);
                } catch (IOException ioex) {
                    throw new CommandValidationException(
                        strings.get("parser.invalidFileEx",
                                    name, ioex.toString()));
                } finally {
                    if (is != null)
                        try {
                            is.close();
                        } catch (IOException cex) { }
                }
                throw new CommandValidationException(
                    strings.get("parser.invalidFile", name, value));
            }
        } else if (opt.getType() == Boolean.class ||
                    opt.getType() == boolean.class) {
            if (value == null)
                value = "true";
            else if (!(value.toLowerCase(Locale.ENGLISH).equals("true") ||
                    value.toLowerCase(Locale.ENGLISH).equals("false")))
                throw new CommandValidationException(
                    strings.get("parser.invalidBoolean", name, value));
        } else if (opt.getParam().password())
            throw new CommandValidationException(
                strings.get("parser.passwordNotAllowed", opt.getName()));

        if (!opt.getParam().multiple()) {
            // repeats not allowed
            if (optionsMap.containsKey(name)) {
                throw new CommandValidationException(
                        strings.get("parser.noRepeats", name));
            }
        }

        optionsMap.add(name, value);
    }
}
