/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.connector;

import java.io.Serializable;
import java.util.Objects;
import java.util.ResourceBundle;

import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.MappingMatch;

import org.glassfish.grizzly.http.server.util.MappingData;

import org.apache.catalina.LogFacade;

public class MappingImpl implements HttpServletMapping, Serializable {

    private static final long serialVersionUID = -5134622427867249518L;

    private String matchValue;

    private String pattern;
    
    private String servletName;

    private MappingMatch mappingMatch;   
    
    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    public MappingImpl(MappingData mappingData) {
        if (null == mappingData) {
            throw new NullPointerException(rb.getString(LogFacade.MAPPING_ERROR_EXCEPTION));
        }
        
        // Trim leading "/"
        matchValue = (null != mappingData.matchedPath) &&
                     (mappingData.matchedPath.length() >= 2) ? mappingData.matchedPath.substring(1) : "";
        pattern = null != mappingData.descriptorPath ? mappingData.descriptorPath : "";
        servletName = null != mappingData.servletName ? mappingData.servletName : "";
        
        switch (mappingData.mappingType) {
            case MappingData.CONTEXT_ROOT:
                mappingMatch = MappingMatch.CONTEXT_ROOT;
                break;
            case MappingData.DEFAULT:
                mappingMatch = MappingMatch.DEFAULT;
                matchValue = "";
                break;
            case MappingData.EXACT:
                mappingMatch = MappingMatch.EXACT;
                break;
            case MappingData.EXTENSION:
                mappingMatch = MappingMatch.EXTENSION;
                // Ensure pattern is valid
                if (null != pattern && '*' == pattern.charAt(0)) {
                    // Mutate matchValue to mean "what * was matched with".
                    int i = matchValue.indexOf(pattern.substring(1));
                    if (-1 != i) {
                        matchValue = matchValue.substring(0, i);
                    }
                }
                break;
            case MappingData.PATH:
                mappingMatch = MappingMatch.PATH;
                // Ensure pattern is valid
                if (null != pattern) {
                    int patternLen = pattern.length();
                    if (0 < patternLen && '*' == pattern.charAt(patternLen-1)) {
                        int indexOfPatternStart = patternLen - 2;
                        int matchValueLen = matchValue.length();
                        if (0 <= indexOfPatternStart && indexOfPatternStart < matchValueLen) {
                            // Remove the pattern from the end of matchValue
                            matchValue = matchValue.substring(indexOfPatternStart);
                        }
                    }
                }
                break;
        }

    }
    
    @Override
    public String getMatchValue() {
        return matchValue;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public String getServletName() {
        return servletName;
    }
    
    
    
    @Override
    public MappingMatch getMappingMatch() {
        return mappingMatch;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.matchValue);
        hash = 29 * hash + Objects.hashCode(this.pattern);
        hash = 29 * hash + Objects.hashCode(this.servletName);
        hash = 29 * hash + Objects.hashCode(this.mappingMatch);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MappingImpl other = (MappingImpl) obj;
        if (!Objects.equals(this.matchValue, other.matchValue)) {
            return false;
        }
        if (!Objects.equals(this.pattern, other.pattern)) {
            return false;
        }
        if (!Objects.equals(this.servletName, other.servletName)) {
            return false;
        }
        if (this.mappingMatch != other.mappingMatch) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MappingImpl{" + "matchValue=" + matchValue 
                + ", pattern=" + pattern 
                + ", servletName=" + servletName 
                + ", mappingMatch=" + mappingMatch + '}';
    }


    
}