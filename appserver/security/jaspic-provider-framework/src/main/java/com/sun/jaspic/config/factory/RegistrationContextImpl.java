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
 */
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.jaspic.config.factory;

import jakarta.security.auth.message.config.AuthConfigFactory.RegistrationContext;

/**
 * Class used by {@link BaseAuthConfigFactory}, {@link EntryInfo} and {@link RegStoreFileParser}
 *
 * This class will *not* be used outside of its package.
 */
final class RegistrationContextImpl implements RegistrationContext {

    private final String messageLayer;
    private final String appContext;
    private final String description;
    private final boolean isPersistent;

    RegistrationContextImpl(String messageLayer, String appContext, String description, boolean persistent) {
        this.messageLayer = messageLayer;
        this.appContext = appContext;
        this.description = description;
        this.isPersistent = persistent;
    }

    // helper method to create impl class
    RegistrationContextImpl(RegistrationContext registrationContext) {
        this.messageLayer = registrationContext.getMessageLayer();
        this.appContext = registrationContext.getAppContext();
        this.description = registrationContext.getDescription();
        this.isPersistent = registrationContext.isPersistent();
    }

    @Override
    public String getMessageLayer() {
        return messageLayer;
    }

    @Override
    public String getAppContext() {
        return appContext;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isPersistent() {
        return isPersistent;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof RegistrationContext)) {
            return false;
        }

        RegistrationContext target = (RegistrationContext) o;

        return (EntryInfo.matchStrings(messageLayer, target.getMessageLayer()) &&
                EntryInfo.matchStrings(appContext, target.getAppContext()) &&
                isPersistent() == target.isPersistent());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (this.messageLayer != null ? this.messageLayer.hashCode() : 0);
        hash = 17 * hash + (this.appContext != null ? this.appContext.hashCode() : 0);
        hash = 17 * hash + (this.isPersistent ? 1 : 0);

        return hash;
    }
}
