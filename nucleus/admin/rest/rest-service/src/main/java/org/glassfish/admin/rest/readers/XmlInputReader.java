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

package org.glassfish.admin.rest.readers;

import java.io.Reader;


/**
 * @author Rajeshwar Patil
 */
public class XmlInputReader extends InputReader {

    /**
     * Construct a XmlInputReader from a string.
     * @param reader     A reader.
     */
    public XmlInputReader(Reader reader) {
        super(reader);
    }


    /**
     * Construct a InputReader from a string.
     * @param s     A source string.
     */
    public XmlInputReader(String s) {
        super(s);
    }


    /**
     * Get the text in the CDATA block.
     * @return The string up to the <code>]]&gt;</code>.
     * @throws InputException If the <code>]]&gt;</code> is not found.
     */
    public String nextCDATA() throws InputException {
        char         c;
        int          i;
        StringBuffer sb = new StringBuffer();
        for (;;) {
            c = next();
            if (c == 0) {
                throw error("Unclosed CDATA");
            }
            sb.append(c);
            i = sb.length() - 3;
            if (i >= 0 && sb.charAt(i) == ']' &&
                          sb.charAt(i + 1) == ']' && sb.charAt(i + 2) == '>') {
                sb.setLength(i);
                return sb.toString();
            }
        }
    }


    /**
     * Get the next XML outer token, trimming whitespace. There are two kinds
     * of tokens: the '<' character which begins a markup tag, and the content
     * text between markup tags.
     *
     * @return  A string, or a '<' Character, or null if there is no more
     * source text.
     * @throws InputException
     */
    public Object nextContent() throws InputException {
        char         c;
        StringBuffer sb;
        do {
            c = next();
        } while (Character.isWhitespace(c));
        if (c == 0) {
            return null;
        }
        if (c == '<') {
            return XmlInputObject.LT;
        }
        sb = new StringBuffer();
        for (;;) {
            if (c == '<' || c == 0) {
                back();
                return sb.toString().trim();
            }
            if (c == '&') {
                sb.append(nextEntity(c));
            } else {
                sb.append(c);
            }
            c = next();
        }
    }


    /**
     * Return the next entity. These entities are translated to Characters:
     *     <code>&amp;  &apos;  &gt;  &lt;  &quot;</code>.
     * @param a An ampersand character.
     * @return  A Character or an entity String if the entity is not recognized.
     * @throws InputException If missing ';' in XML entity.
     */
    public Object nextEntity(char a) throws InputException {
        StringBuffer sb = new StringBuffer();
        for (;;) {
            char c = next();
            if (Character.isLetterOrDigit(c) || c == '#') {
                sb.append(Character.toLowerCase(c));
            } else if (c == ';') {
                break;
            } else {
                throw error("Missing ';' in XML entity: &" + sb);
            }
        }
        String s = sb.toString();
        Object e = entity.get(s);
        return e != null ? e : a + s + ";";
    }


    /**
     * Returns the next XML meta token. This is used for skipping over <!...>
     * and <?...?> structures.
     * @return Syntax characters (<code>< > / = ! ?</code>) are returned as
     *  Character, and strings and names are returned as Boolean. We don't care
     *  what the values actually are.
     * @throws InputException If a string is not properly closed or if the XML
     *  is badly structured.
     */
    public Object nextMeta() throws InputException {
        char c;
        char q;
        do {
            c = next();
        } while (Character.isWhitespace(c));
        switch (c) {
        case 0:
            throw error("Misshaped meta tag");
        case '<':
            return XmlInputObject.LT;
        case '>':
            return XmlInputObject.GT;
        case '/':
            return XmlInputObject.SLASH;
        case '=':
            return XmlInputObject.EQ;
        case '!':
            return XmlInputObject.BANG;
        case '?':
            return XmlInputObject.QUEST;
        case '"':
        case '\'':
            q = c;
            for (;;) {
                c = next();
                if (c == 0) {
                    throw error("Unterminated string");
                }
                if (c == q) {
                    return Boolean.TRUE;
                }
            }
        default:
            for (;;) {
                c = next();
                if (Character.isWhitespace(c)) {
                    return Boolean.TRUE;
                }
                switch (c) {
                case 0:
                case '<':
                case '>':
                case '/':
                case '=':
                case '!':
                case '?':
                case '"':
                case '\'':
                    back();
                    return Boolean.TRUE;
                }
            }
        }
    }


    /**
     * Get the next XML Token. These tokens are found inside of angle
     * brackets. It may be one of these characters: <code>/ > = ! ?</code> or it
     * may be a string wrapped in single quotes or double quotes, or it may be a
     * name.
     * @return a String or a Character.
     * @throws InputException If the XML is not well formed.
     */
    public Object nextToken() throws InputException {
        char c;
        char q;
        StringBuffer sb;
        do {
            c = next();
        } while (Character.isWhitespace(c));
        switch (c) {
        case 0:
            throw error("Misshaped element");
        case '<':
            throw error("Misplaced '<'");
        case '>':
            return XmlInputObject.GT;
        case '/':
            return XmlInputObject.SLASH;
        case '=':
            return XmlInputObject.EQ;
        case '!':
            return XmlInputObject.BANG;
        case '?':
            return XmlInputObject.QUEST;
        // Quoted string
        case '"':
        case '\'':
            q = c;
            sb = new StringBuffer();
            for (;;) {
                c = next();
                if (c == 0) {
                    throw error("Unterminated string");
                }
                if (c == q) {
                    return sb.toString();
                }
                if (c == '&') {
                    sb.append(nextEntity(c));
                } else {
                    sb.append(c);
                }
            }
        default:
            // Name
            sb = new StringBuffer();
            for (;;) {
                sb.append(c);
                c = next();
                if (Character.isWhitespace(c)) {
                    return sb.toString();
                }
                switch (c) {
                case 0:
                	return sb.toString();
                case '>':
                case '/':
                case '=':
                case '!':
                case '?':
                case '[':
                case ']':
                    back();
                    return sb.toString();
                case '<':
                case '"':
                case '\'':
                    throw error("Bad character in a name");
                }
            }
        }
    }
    
    
    /**
     * Skip characters until past the requested string.
     * If it is not found, we are left at the end of the source with a result of false.
     * @param to A string to skip past.
     * @throws InputException
     */
    public boolean skipPast(String to) throws InputException {
    	boolean b;
    	char c;
    	int i;
    	int j;
    	int offset = 0;
    	int n = to.length();
        char[] circle = new char[n];
        
        /*
         * First fill the circle buffer with as many characters as are in the
         * to string. If we reach an early end, bail.
         */
        
    	for (i = 0; i < n; i += 1) {
    		c = next();
    		if (c == 0) {
    			return false;
    		}
    		circle[i] = c;
    	}
    	/*
    	 * We will loop, possibly for all of the remaining characters.
    	 */
    	for (;;) {
    		j = offset;
    		b = true;
    		/*
    		 * Compare the circle buffer with the to string. 
    		 */
    		for (i = 0; i < n; i += 1) {
    			if (circle[j] != to.charAt(i)) {
    				b = false;
    				break;
    			}
    			j += 1;
    			if (j >= n) {
    				j -= n;
    			}
    		}
    		/*
    		 * If we exit the loop with b intact, then victory is ours.
    		 */
    		if (b) {
    			return true;
    		}
    		/*
    		 * Get the next character. If there isn't one, then defeat is ours.
    		 */
    		c = next();
    		if (c == 0) {
    			return false;
    		}
    		/*
    		 * Shove the character in the circle buffer and advance the 
    		 * circle offset. The offset is mod n.
    		 */
    		circle[offset] = c;
    		offset += 1;
    		if (offset >= n) {
    			offset -= n;
    		}
    	}
    }


   /** The table of entity values. It initially contains Character values for
    * amp, apos, gt, lt, quot.
    */
   public static final java.util.HashMap entity;

   static {
       entity = new java.util.HashMap(8);
       entity.put("amp",  XmlInputObject.AMP);
       entity.put("apos", XmlInputObject.APOS);
       entity.put("gt",   XmlInputObject.GT);
       entity.put("lt",   XmlInputObject.LT);
       entity.put("quot", XmlInputObject.QUOT);
   }
}
