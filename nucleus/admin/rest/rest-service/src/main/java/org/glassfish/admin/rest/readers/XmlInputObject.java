/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.readers;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rajeshwar Patil
 */
public class XmlInputObject extends InputObject {

    /**
     * Construct a XmlInputObjectfrom a input stream.
     * @param inputstream an input stream 
     * @exception InputException If there is a syntax error in the source
     *  input stream or a duplicate key.
     */
    public XmlInputObject(InputStream inputstream) throws InputException, IOException {
        this(readAsString(inputstream));
    }


    /**
     * Construct a XmlInputObjectfrom a XML text string.
     * @param source    A XML text string
     * @exception InputException If there is a syntax error in the source
     *  string or a duplicated key.
     */
    public XmlInputObject(String source) throws InputException {
        this(new XmlInputReader(source));
    }


    public XmlInputObject(XmlInputReader xmlReader) throws InputException {
        this.xmlReader = xmlReader;
        map = new HashMap();
    }


    /**
     * Construct and returns a map of input key-value pairs
     * @throws InputException If there is a syntax error in the source string
     *  or a duplicated key.
     */
    @Override
    public Map initializeMap() throws InputException {
        while (xmlReader.more() && xmlReader.skipPast("<")) {
            parse(xmlReader, this, null);
        }
       return map;
    }


    public Map getMap() throws InputException {
        return map;
    }


    /**
     * Scan the content following the named tag, attaching it to the context.
     * @param x       The XmlInputReader containing the source string.
     * @param context The XmlInputObject that will include the new material.
     * @param name    The tag name.
     * @return true if the close tag is processed.
     * @throws InputException
     */
    private static boolean parse(XmlInputReader reader, XmlInputObject context,
                                 String name) throws InputException {
        char       character;
        int        i;
        String     n;
        XmlInputObject subContext; 
        String     string;
        Object     token;

        // Test for and skip past these forms:
        // <!-- ... -->   <!   ...   >   <![  ... ]]>   <?   ...  ?>
        // Report errors for these forms:   <>   <=   <<

        token = reader.nextToken();

        // <!
        if (BANG.equals(token) ) {
            character = reader.next();
            if (character == '-') {
                if (reader.next() == '-') {
                    reader.skipPast("-->");
                    return false;
                }
                reader.back();
            } else if (character == '[') {
                token = reader.nextToken();
                if ("CDATA".equals(token) ) {
                    if (reader.next() == '[') {
                        string = reader.nextCDATA();
                        if (string.length() > 0) {
                            context.put("content", string);
                        }
                        return false;
                    }
                }
                throw reader.error("Expected 'CDATA['");
            }

            i = 1;
            do {
                token = reader.nextMeta();
                if (token == null) {
                    throw reader.error("Missing '>' after '<!'.");
                } else if (LT.equals(token) ) {
                    i += 1;
                } else if (GT.equals(token) ) {
                    i -= 1;
                }
            } while (i > 0);
            return false;
        } else if (QUEST.equals(token) ) { 
            // <?
            reader.skipPast("?>");
            return false;
        } else if (SLASH.equals(token) ) {
            // Close tag </
            token = reader.nextToken();
            if (name == null) {
                throw reader.error("Mismatched close tag " + token);
            }            
            if (!name.equals(token) ) {
                throw reader.error("Mismatched " + name + " and " + token);
            }
            if (!GT.equals(reader.nextToken())) {
                throw reader.error("Misshaped close tag");
            }
            return true;

        } else if (token instanceof Character) {
            throw reader.error("Misshaped tag");

        // Open tag <
        } else {
            n = (String)token;
            token = null;
            subContext = new XmlInputObject(reader);
            for (;;) {
                if (token == null) {
                    token = reader.nextToken();
                }

                // attribute = value
                if (token instanceof String) {
                    string = (String)token;
                    token = reader.nextToken();
                    if (EQ.equals(token)) {
                        token = reader.nextToken();
                        if (!(token instanceof String)) {
                            throw reader.error("Missing value");
                        }
                        subContext.put(string, XmlInputObject.stringToValue((String)token));
                        token = null;
                    } else {
                        subContext.put(string, "");
                    }

                // Empty tag <.../>
                } else if (SLASH.equals(token) ) {
                    if (!reader.nextToken().equals(GT)) {
                        throw reader.error("Misshaped tag");
                    }
                    context.putMap(n, subContext.getMap());
                    return false;

                // Content, between <...> and </...>
                } else if (GT.equals(token) ) {
                    for (;;) {
                        token = reader.nextContent();
                        if (token == null) {
                            if (n != null) {
                                throw reader.error("Unclosed tag " + n);
                            }
                            return false;
                        } else if (token instanceof String) {
                            string = (String)token;
                            if (string.length() > 0) {
                                subContext.put("content", XmlInputObject.stringToValue(string));
                            }

                        // Nested element
                        } else if (LT.equals(token) ) {
                            if (parse(reader, subContext, n)) {
                                if (subContext.length() == 0) {
                                    context.put(n, "");
                                } else if (subContext.length() == 1 &&
                                       subContext.get("content") != null) {
                                    context.put(n, subContext.get("content"));
                                } else {
                                    context.putMap(n, subContext.getMap());
                                }
                                return false;
                            }
                        }
                    }
                } else {
                    throw reader.error("Misshaped tag");
                }
            }
        }
    }


    /** The Character '&'. */
    public static final Character AMP   = Character.valueOf('&');

    /** The Character '''. */
    public static final Character APOS  = Character.valueOf('\'');

    /** The Character '!'. */
    public static final Character BANG  = Character.valueOf('!');

    /** The Character '='. */
    public static final Character EQ    = Character.valueOf('=');

    /** The Character '>'. */
    public static final Character GT    = Character.valueOf('>');

    /** The Character '<'. */
    public static final Character LT    = Character.valueOf('<');

    /** The Character '?'. */
    public static final Character QUEST = Character.valueOf('?');

    /** The Character '"'. */
    public static final Character QUOT  = Character.valueOf('"');

    /** The Character '/'. */
    public static final Character SLASH = Character.valueOf('/');

    private XmlInputReader xmlReader;
}
