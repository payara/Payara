/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config;

/**
 * Used to perform string pre-processing on values found in the configuration file.
 *
 * <p>
 * This hook allows applications to support variable
 * expansions like Ant in the configuration file.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Translator {
    String translate(String str) throws TranslationException;

    /**
     * {@link Translator} that does nothing.
     */
    public static final Translator NOOP = new Translator() {
        public String translate(String str) {
            return str;
        }
    };
    /**
     * A translator that does translation from the system properties. Thus, any reference to an existing
     * System Property like "${name}" will be replaced by its value, i.e. System.getProperty("name"). All
     * found references are translated. If a System Property is not defined, its reference is returned verbatim.
     * No escape sequences are handled.
     */
    public static final Translator SYS_PROP_TR = new Translator() {
        public String translate(String s) {
            StringBuilder sb = new StringBuilder();
            int length = s.length();
            int i = 0;
            while(i < length) {
                char c = s.charAt(i);
                if (c == '$' && (i+1) < length && s.charAt(i+1) == '{') {
                i += 2;
                char cc='\0';
                StringBuilder prop = new StringBuilder();
                while (i < length && (cc=s.charAt(i)) != '}') {
                  prop.append(cc);
                  i++;
                }
                if (cc == '}') {
                  String value = System.getProperty(prop.toString());
                  if (value != null)
                      sb.append(value);
                  else //return reference to non-existent system-property verbatim
                      sb.append("${" + prop + "}");
                  i++;
                } else { //we reached the end, no } found
                  sb.append("${").append(prop);
                }
              } else {
                sb.append(c);
                i++;
              }
          }
          return sb.toString();
        }
    };
}
