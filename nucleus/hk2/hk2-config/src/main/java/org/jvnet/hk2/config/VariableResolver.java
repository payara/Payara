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
 * {@link Translator} that does variable resolution in the Ant style.
 *
 * <p>
 * This implementation looks for variables in the string like
 * "${xyz}" or "${abc.DEF.ghi}". The {@link #getVariableValue(String)} method
 * is then used to obtain the actual value for the variable.
 *
 * <p>
 * "$$" works as the escape of "$", so for example "$${abc}" expands to "${abc}"
 * where "${abc}" would have expanded to "value-of-abc".
 * A lone "$" is left as-is, so "$abc" expands to "$abc".
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class VariableResolver implements Translator {
    public String translate(String str) throws TranslationException {
        if(str.indexOf('$')==-1)
            return str; // fast path for the common case

        int idx = 0;
        StringBuilder buf = new StringBuilder();
        while(true) {
            int s = str.indexOf('$',idx);
            if(s==-1) {
                buf.append(str,idx,str.length());
                return buf.toString();
            }

            // copy until this '$'
            buf.append(str,idx,s);

            if(s+1==str.length()) {
                buf.append('$');    // '$' was the last char
                idx=s+1;
                continue;
            }

            char second = str.charAt(s + 1);
            switch(second) {
            case '{': // variable
                int e = str.indexOf('}',s+2);
                if(e ==-1)
                    throw new TranslationException("Missing '}' at the end of \""+str+"\"");
                String varName = str.substring(s+2, e);
                String value;
                try {
                    value = getVariableValue(varName);
                } catch (TranslationException x) {
                    throw new TranslationException("Failed to expand variable ${"+varName+'}',x);
                }
                if (value == null)
                    throw new TranslationException(String.format("Undefined variable ${%s} in \"%s\"",
                        varName, str));
                buf.append(value);
                idx = e+1;
                break;
            case '$': // $ escape
                buf.append('$');
                idx=s+2;
                break;
            default:
                buf.append('$');
                idx=s+1;
                break;
            }
        }
    }

    /**
     * Returns the value of the variable.
     *
     * This class will not try to further expand variables in the returned value.
     * If the implementation wants to do so, that is the implementation's responsibility. 
     *
     * @return
     *      null if the variable is not found. The caller will report an error.
     *      When the variable is not found, it's also legal to throw {@link TranslationException},
     *      which is an useful technique if the implementation would like to report
     *      additional errors. 
     */
    protected abstract String getVariableValue(String varName) throws TranslationException;
}
