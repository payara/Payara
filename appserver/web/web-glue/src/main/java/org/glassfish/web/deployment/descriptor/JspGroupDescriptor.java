/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.deployment.descriptor;

import com.sun.enterprise.deployment.OrderedSet;
import org.glassfish.deployment.common.Descriptor;

import java.util.Set;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;

/**
 */
public class JspGroupDescriptor extends Descriptor
        implements JspPropertyGroupDescriptor {
    
    private String elIgnored;
    private String scriptingInvalid;
    private String isXml;
    private String deferredSyntaxAllowedAsLiteral;
    private String trimDirectiveWhitespaces;
    private Set<String> urlPatterns = null;
    private Set<String> includePreludes = null;
    private Set<String> includeCodas = null;
    private String pageEncoding = null;
    private String defaultContentType = null;
    private String buffer = null;
    private String errorOnUndeclaredNamespace;
    
    /**
     * Return the set of URL pattern aliases for this group.
     */
    public Set<String> getUrlPatterns() {
        if (this.urlPatterns == null) {
            this.urlPatterns = new OrderedSet<String>();
        }
        return this.urlPatterns;
    }

    /**
     * Adds an alias to this jsp group.
     */
    public void addUrlPattern(String urlPattern) {
        this.getUrlPatterns().add(urlPattern);

    }
   
    /**
     * Removes a URL pattern from this jsp group.
     */
    public void removeUrlPattern(String urlPattern) {
        this.getUrlPatterns().remove(urlPattern);

    }

    /**
     * Return an Iterable over the include prelude elements for this group.
     */
    public Set<String> getIncludePreludes() {
        if (this.includePreludes == null) {
            this.includePreludes = new OrderedSet<String>();
        }
        return this.includePreludes;
    }

    /**
     * Adds an element
     */
    public void addIncludePrelude(String prelude) {
        this.getIncludePreludes().add(prelude);

    }
   
    /**
     * Removes an element 
     */
    public void removeIncludePrelude(String prelude) {
        this.getIncludePreludes().remove(prelude);

    }

    /**
     * Return an Iterable over include coda elements for this group.
     */
    public Set<String> getIncludeCodas() {
        if (this.includeCodas == null) {
            this.includeCodas = new OrderedSet<String>();
        }
        return this.includeCodas;
    }

    /**
     * Adds an element
     */
    public void addIncludeCoda(String coda) {
        this.getIncludeCodas().add(coda);

    }
   
    /**
     * Removes an element 
     */
    public void removeIncludeCoda(String coda) {
        this.getIncludeCodas().remove(coda);

    }

    /**
     * elIgnored
     */
    public void setElIgnored(String value) {
        elIgnored = value;
    }

    public String getElIgnored() {
        return elIgnored;
    }
    
    /**
     * enable/disable scripting
     */
    public void setScriptingInvalid(String value) {
        scriptingInvalid = value;
    }

    public String getScriptingInvalid() {
        return scriptingInvalid;
    }

    /**
     * enable/disable xml 
     */
    public void setIsXml(String value) {
        isXml = value;
    }
    
    public String getIsXml() {
        return isXml;
    }

    /**
     * enable/disable deferredSyntaxAllowedAsLiteral
     */
    public void setDeferredSyntaxAllowedAsLiteral(String value) {
        deferredSyntaxAllowedAsLiteral = value;
    }

    public String getDeferredSyntaxAllowedAsLiteral() {
        return deferredSyntaxAllowedAsLiteral;
    }

    /**
     * enable/disable trimDirectiveWhitespaces
     */
    public void setTrimDirectiveWhitespaces(String value) {
        trimDirectiveWhitespaces = value;
    }

    public String getTrimDirectiveWhitespaces() {
        return trimDirectiveWhitespaces;
    }
    
    /**
     * get display name.
     */
    public String getDisplayName() {
        // bug#4745178  other code requires the
        // display name to be localized.
        return super.getName();
    }

    /**
     * set display name.
     */
    public void setDisplayName(String name) {
        // bug#4745178  other code requires the
        // display name to be localized.
        super.setName(name);
    }

    public String getPageEncoding() {
	return pageEncoding;
    }

    public void setPageEncoding(String encoding) {
	pageEncoding = encoding;
    }

    /**
     * get defaultContentType
     */
    public String getDefaultContentType() {
        return defaultContentType;
    }

    /**
     * set defaultContentType
     */
    public void setDefaultContentType(String defaultContentType) {
        this.defaultContentType = defaultContentType;
    }

    /**
     * get buffer
     */
    public String getBuffer() {
        return buffer;
    }

    /**
     * set buffer
     */
    public void setBuffer(String value) {
        buffer = value;
    }

    /**
     * set errorOnUndeclaredNamespace
     */
    public void setErrorOnUndeclaredNamespace(String value) {
        errorOnUndeclaredNamespace = value;
    }

    public String getErrorOnUndeclaredNamespace() {
        return errorOnUndeclaredNamespace;
    }

    /**
     * @return a string describing the values I hold
     */
    public void print(StringBuffer toStringBuffer) {
        toStringBuffer.append("\n JspGroupDescriptor");
        toStringBuffer.append( "\n");
        super.print(toStringBuffer);
        toStringBuffer.append( "\n DisplayName:").append(this.getDisplayName());
        toStringBuffer.append( "\n PageEncoding:").append(pageEncoding);
        toStringBuffer.append( "\n El-Ignored:").append(elIgnored);
        toStringBuffer.append( "\n Scripting Invalid:").append(scriptingInvalid);
        toStringBuffer.append( "\n urlPatterns: ").append(urlPatterns);
        toStringBuffer.append( "\n includePreludes: ").append(includePreludes);
        toStringBuffer.append( "\n includeCoda: ").append(includeCodas);
        toStringBuffer.append( "\n Is XML:").append(isXml);
        toStringBuffer.append( "\n DeferredSyntaxAllowedAsLiteral: ").append(deferredSyntaxAllowedAsLiteral);
        toStringBuffer.append( "\n TrimDirectiveWhitespaces:").append(trimDirectiveWhitespaces);
        toStringBuffer.append( "\n defaultContentType: ").append(defaultContentType);
        toStringBuffer.append( "\n buffer: ").append(buffer);
        toStringBuffer.append( "\n errorOnUndeclaredNamespace: ").append(errorOnUndeclaredNamespace);
    }
}
