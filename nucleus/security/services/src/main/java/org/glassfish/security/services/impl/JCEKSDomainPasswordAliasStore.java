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
package org.glassfish.security.services.impl;

import com.sun.enterprise.security.store.DomainScopedPasswordAliasStore;
import com.sun.enterprise.security.store.IdentityManagement;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.io.File;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.security.services.common.Secure;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

/**
 * Exposes as a service the JCEKS implementation of the
 * domain-scoped password alias store.
 * @author tjquinn
 */
@Service
@Named("JCEKS")
@PerLookup
@Secure(accessPermissionName = "security/service/credential/provider/jceks")
public class JCEKSDomainPasswordAliasStore extends JCEKSPasswordAliasStore implements DomainScopedPasswordAliasStore  {
    
    private static final String PASSWORD_ALIAS_KEYSTORE = "domain-passwords";

    @Inject @Optional
    private IdentityManagement idm;
    
    @PostConstruct
    private void initStore() {
        try {
            init(pathToDomainAliasStore(), getMasterPassword());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private char[] getMasterPassword() {
        return idm == null ? null : idm.getMasterPassword();
    }
    
    private static String pathToDomainAliasStore() {
        return System.getProperty(SystemPropertyConstants.INSTANCE_ROOT_PROPERTY) +
                File.separator + "config" + File.separator + PASSWORD_ALIAS_KEYSTORE;
    }
}
