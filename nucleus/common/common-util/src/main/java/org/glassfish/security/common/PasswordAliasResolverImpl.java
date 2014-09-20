/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.security.common;

import org.glassfish.api.admin.PasswordAliasResolver;
import org.glassfish.api.admin.PasswordAliasStore;

/**
 * Provides password alias resolution, using an internal password alias store
 * to actually resolve an alias if one is specified.
 * 
 * @author tjquinn
 */
public class PasswordAliasResolverImpl implements PasswordAliasResolver {

    private static final String ALIAS_TOKEN = "ALIAS";
    private static final String STARTER = "${" + ALIAS_TOKEN + "="; //no space is allowed in starter
    private static final String ENDER = "}";
    
    private final PasswordAliasStore store;
    public PasswordAliasResolverImpl(final PasswordAliasStore store) {
        this.store = store;
    }
    
    @Override
    public char[] resolvePassword(String aliasExpressionOrPassword) {
        final String alias = getAlias(aliasExpressionOrPassword);
        if (alias != null) {
            return store.get(alias);
        }
        return aliasExpressionOrPassword.toCharArray();
    }
    
    /**
     * check if a given property name matches AS alias pattern ${ALIAS=aliasname}.
     * if so, return the aliasname, otherwise return null.
     * @param propName The property name to resolve. ex. ${ALIAS=aliasname}.
     * @return The aliasname or null.
     */    
    private static String getAlias(String pwOrAliasExpression)
    {
       String aliasName=null;

       pwOrAliasExpression = pwOrAliasExpression.trim();
       if (pwOrAliasExpression.startsWith(STARTER) && pwOrAliasExpression.endsWith(ENDER) ) {
           pwOrAliasExpression = pwOrAliasExpression.substring(STARTER.length() );
           int lastIdx = pwOrAliasExpression.length() - 1;
           if (lastIdx > 1) {
              pwOrAliasExpression = pwOrAliasExpression.substring(0,lastIdx);
              if (pwOrAliasExpression!=null) {
                   aliasName = pwOrAliasExpression.trim();
               }
           }
       } 
       return aliasName;    
    }
}
