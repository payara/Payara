/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.modularity.customization;

/**
 * Will carry a set of four strings which will be used during domain creation to find what initial values are required by a config bean to acquire them during the domain creation process.
 *
 * @author Masoud Kalali
 */
public class ConfigCustomizationToken {

    public enum CustomizationType {PORT, FILE, STRING}

    private String name;
    private String title;
    private String description;
    private String value;
    private String validationExpression;
    private TokenTypeDetails tokenTypeDetails;
    private CustomizationType customizationType;

    public ConfigCustomizationToken(String name, String title, String description, String value,
                                    String validationExpression, TokenTypeDetails tokenTypeDetails, CustomizationType customizationType) {
        this.name = name;
        this.title = title;
        this.description = description;
        this.value = value;
        this.validationExpression = validationExpression;
        this.tokenTypeDetails = tokenTypeDetails;
        this.customizationType = customizationType;
    }

    public String getValidationExpression() {
        return validationExpression;
    }

    public TokenTypeDetails getTokenTypeDetails() {
        return tokenTypeDetails;
    }

    public CustomizationType getCustomizationType() {
        return customizationType;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String tokenValue){
        this.value=tokenValue;
    }
}

