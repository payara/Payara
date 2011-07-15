/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.tools.verifier.util.LogDomains;

public class CLIPParser {

// NOTE: the current design does not allow LongOptions to have optional arguments. 
// They should either have or not have arguments


    /**
     * helper property to get to the localized strings
     */
    protected static final com.sun.enterprise.util.LocalStringManagerImpl smh =
            StringManagerHelper.getLocalStringsManager();

    /**
     * argument values for options that take an argument are stored here.
     */
    protected String optarg;


    /**
     * When an unrecognized option is encountered, getopt will return a '?'
     * and store the value of the invalid option here.
     */
    protected int optopt = '?';

    /**
     * The next character to be scanned in the option-element
     * in which the last option character we returned was found.
     * This allows us to pick up the scan where we left last.
     * If cn is a null string, advance to next argv
     */
    protected String cn;

    /**
     * This is the string describing the valid short options.
     */
    protected String optstring;

    /**
     * array of LongOption objects describing the valid long options.
     */
    protected LongOption[] longopt;

    /**
     * Stores the index into the longopt array of the long option detected
     */
    protected int longind;

    /**
     * A flag which communicates whether or not processLOpt() did all
     * necessary processing for the current option
     */
    protected boolean processed;

    /**
     * The index of the first non-option in argv[]
     */
    protected int fno = 1;

    /**
     * The index of the last non-option in argv[]
     */
    protected int lno = 1;

    /**
     * Index in argv of the next element to be scanned.
     * This is used for returning to and from the driver
     * and for returning between successive calls to getopt.
     * On entry to getopt zero means this is the first call.
     * When getopt returns -1 this is the index of the first of the
     * non-option elements that the caller should itself scan.
     * Otherwise optind returns from one call to the next
     * how much of argv has been parsed so far.
     */
    protected int optind = 0;

    /**
     * The arguments to be CLIP parsed
     */
    protected String[] argv;

    /**
     * The utility name.
     */
    protected String progname;
    
    Logger logger = LogDomains.getLogger(LogDomains.AVK_VERIFIER_LOGGER);

    /**
     * flag when set to true will result in getopt returning -1 the next time it is called.
     */
    private boolean endparse = false;

    /**
     * @param progname  The name to display as the program name when printing errors
     * @param argv      String array passed as the command line to the program
     * @param optstring String containing a description of the valid short options for this program
     * @param longopt   array of LongOption objects that describes the valid long options for this program
     */
    public CLIPParser(String progname, String[] argv, String optstring,
                      LongOption[] longopt) {
        if (optstring.length() == 0)
            optstring = " "; // NOI18N

        this.progname = progname;
        this.argv = argv;
        this.optstring = optstring;
        this.longopt = longopt;
    }

    /**
     * validity checker for long options.
     *
     * @return the corresponding short option character or  ?.
     */
    protected int processLOpt() {
        LongOption currlopt = null;
        boolean ambiguous;
        boolean exact;
        int eoname;

        processed = true;
        ambiguous = false;
        exact = false;
        longind = -1;

        eoname = cn.indexOf("="); // NOI18N
        if (eoname == -1)
            eoname = cn.length();

        for (int i = 0; i < longopt.length; i++) {

            if (longopt[i].getName().startsWith(cn.substring(0, eoname))) {
                if (longopt[i].getName().equals(cn.substring(0, eoname))) {
                    // Exact match found
                    currlopt = longopt[i];
                    longind = i;
                    exact = true;
                    break;
                } else if (currlopt == null) {
                    // First nonexact match found
                    currlopt = longopt[i];
                    longind = i;
                } else {
                    // Second or subsequent nonexact match found
                    ambiguous = true;
                }
            }
        }

        if (ambiguous && !exact) {
            logger.log(Level.SEVERE, getClass().getName() + ".ambig",
                    new Object[]{progname, argv[optind]});
            cn = "";
            optopt = 0;
            ++optind;

            return '?';
        }

        if (currlopt != null) {
            ++optind;

            if (eoname != cn.length()) {
                if (currlopt.argPresent) {
                    if (cn.substring(eoname).length() > 1)
                        optarg = cn.substring(eoname + 1);
                    else
                        optarg = "";
                } else {
                    if (argv[optind - 1].startsWith("--")) { // NOI18N
                        logger.log(Level.SEVERE, getClass().getName() +
                                ".loptnoarg", // NOI18N
                                new Object[]{progname, currlopt.name});
                    } else {
                        logger.log(Level.SEVERE,
                                getClass().getName() + ".optnoarg", // NOI18N
                                new Object[]{progname,
                                             new Character(
                                                     argv[optind - 1].charAt(0)).toString(),
                                             currlopt.name});
                    }

                    cn = "";
                    optopt = currlopt.value;

                    return '?';
                }
            } else if (currlopt.argPresent) {
                if (optind < argv.length) {
                    optarg = argv[optind];
                    ++optind;
                } else {
                    logger.log(Level.SEVERE, getClass().getName() + ".reqargs",
                            new Object[]{progname, argv[optind - 1]});

                    cn = "";
                    optopt = currlopt.value;
                    if (optstring.charAt(0) == ':')
                        return ':';
                    else
                        return '?';
                }
            }

            cn = "";

            if (currlopt.holder != null) {
                currlopt.holder.setLength(0);
                currlopt.holder.append(currlopt.value);

                return (0);
            }

            return currlopt.value;
        }

        processed = false;

        return (0);
    }

    /**
     * @return The character representing the current option that has been
     *         parsed from the command line. If the option takes an argument,
     *         then the member optarg is set to contain the value of the
     *         argument. If an invalid option is found  CLIP parser prints an
     *         error message and a ? character is returned. Member variable
     *         optopt will as usual store the option character. A -1 is returned
     *         when end of option parsing is detected. Upon return optind will
     *         be pointing to the first non-option argument in argv
     */
    public int getopt() {
        optarg = null;

        if (endparse == true)
            return (-1);

        if ((cn == null) || (cn.equals(""))) {
            if (lno > optind)
                lno = optind;
            if (fno > optind)
                fno = optind;

            if ((optind != argv.length) && argv[optind].equals("--")) { // NOI18N
                optind++;

                if (fno == lno)
                    fno = optind;

                lno = argv.length;
                optind = argv.length;
            }

            if (optind == argv.length) {
                // Set the next arg index to point at the non-options
                // that we previously skipped.
                if (fno != lno)
                    optind = fno;

                return -1;
            }

            // If we have come to a non-option stop the scan
            if (argv[optind].equals("") || (argv[optind].charAt(0) != '-') ||
                    argv[optind].equals("-")) { // NOI18N
                return -1;

            }

            // We have found another.
            if (argv[optind].startsWith("--")) // NOI18N
                cn = argv[optind].substring(2);
            else
                cn = argv[optind].substring(1);
        }

        if ((longopt != null) && (argv[optind].startsWith("--"))) { // NOI18N
            int c = processLOpt();

            if (processed)
                return c;

            // unrecognized long/short options
            if (argv[optind].startsWith("--")) { // NOI18N
                logger.log(Level.SEVERE, getClass().getName() + ".loptunknown",
                        new Object[]{progname, argv[optind]});
            } else {
                logger.log(Level.SEVERE, getClass().getName() + ".optunknown",
                        new Object[]{progname,
                                     new Character(argv[optind].charAt(0)).toString(),
                                     cn});
            }

            cn = "";
            ++optind;
            optopt = 0;

            return '?';
        }

        // handle the next short option-character
        int c = cn.charAt(0);
        if (cn.length() > 1)
            cn = cn.substring(1);
        else
            cn = "";

        String ct = null;
        if (optstring.indexOf(c) != -1)
            ct = optstring.substring(optstring.indexOf(c));

        if (cn.equals(""))
            ++optind;

        if ((ct == null) || (c == ':')) {
            /* if ( c != '?') {
             log(smh.getLocalString(getClass().getName() + ".invalidopt",
                     "{0}: invalid option [-{1} ].",
                      new Object[] {progname, new Character((char)c).toString()}));
             }*/

            optopt = c;

            return '?';
        }

        if ((ct.length() > 1) && (ct.charAt(1) == ':')) {
            if ((ct.length() > 2) && (ct.charAt(2) == ':'))
            // option that accepts an optional argument
            {
                if (!cn.equals("")) {
                    optarg = cn;
                    ++optind;
                } else {
                    optarg = null;
                }

                cn = null;
            } else {
                if (!cn.equals("")) {
                    optarg = cn;
                    ++optind;
                } else if (optind == argv.length) {
                    logger.log(Level.SEVERE, getClass().getName() + ".reqsarg",
                            new Object[]{progname,
                                         new Character((char) c).toString()});

                    optopt = c;

                    if (optstring.charAt(0) == ':')
                        return ':';
                    else
                        return '?';
                } else {
                    optarg = argv[optind];
                    ++optind;
                }

                cn = null;
            }
        }

        return c;
    }

    public int getOptind() {
        return optind;
    }


    public String getOptarg() {
        return optarg;
    }

    public int getOptopt() {
        return optopt;
    }

    public int getLongind() {
        return longind;
    }

} 


