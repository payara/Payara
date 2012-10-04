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

package com.sun.enterprise.connectors;

import com.sun.enterprise.connectors.authentication.RuntimeSecurityMap;
import com.sun.enterprise.deployment.ResourcePrincipal;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;


/**
 * Information about the ConnectorConnectionPool.
 * Stored inofrmation is:
 * 1. Default Subject
 * 2. MCF Instance
 * 3. Password, UserName
 * 4. The transaction-support attribute level in case of connector
 * connection pools
 * 5. The allow-non-component-callers, non-trasnactional-connections
 * attribs for jdbc connection pools
 *
 * @author Binod P.G., Aditya Gore
 */

public class PoolMetaData {

    private ManagedConnectionFactory mcf = null;
    private PoolInfo poolInfo = null;
    private Subject subj = null;
    private ResourcePrincipal prin_;
    private int txSupport_;
    private boolean isPM_ = false;
    private boolean isNonTx_ = false;
    private RuntimeSecurityMap runtimeSecurityMap;
    private boolean lazyEnlistable_ = false;
    private boolean lazyAssoc_ = false;
    private boolean isAuthCredentialsDefinedInPool_ = true;

    public PoolMetaData(PoolInfo poolInfo, ManagedConnectionFactory mcf,
                        Subject s, int txSupport, ResourcePrincipal prin,
                        boolean isPM, boolean isNonTx, boolean lazyEnlistable,
                        RuntimeSecurityMap runtimeSecurityMap, boolean lazyAssoc) {
        this.poolInfo = poolInfo;
        this.mcf = mcf;
        this.subj = s;
        txSupport_ = txSupport;
        prin_ = prin;
        isPM_ = isPM;
        isNonTx_ = isNonTx;
        lazyEnlistable_ = lazyEnlistable;
        lazyAssoc_ = lazyAssoc;
        this.runtimeSecurityMap = runtimeSecurityMap;
    }

    public ManagedConnectionFactory getMCF() {
        return this.mcf;
    }

    public Subject getSubject() {
        return this.subj;
    }

    public int getTransactionSupport() {
        return txSupport_;
    }

    public ResourcePrincipal getResourcePrincipal() {
        return prin_;
    }


    public void setIsNonTx(boolean flag) {
        isNonTx_ = flag;
    }


    public boolean isNonTx() {
        return isNonTx_;
    }


    public void setIsPM(boolean flag) {
        isPM_ = flag;
    }


    public boolean isPM() {
        return isPM_;
    }

    public RuntimeSecurityMap getRuntimeSecurityMap() {
        return this.runtimeSecurityMap;
    }

    public void setLazyEnlistable(boolean flag) {
        lazyEnlistable_ = flag;
    }

    public boolean isLazyEnlistable() {
        return lazyEnlistable_;
    }

    public void setLazyAssoc(boolean flag) {
        lazyAssoc_ = flag;
    }

    public boolean isLazyAssociatable() {
        return lazyAssoc_;
    }

    public void setAuthCredentialsDefinedInPool(boolean authCred) {
        this.isAuthCredentialsDefinedInPool_ = authCred;
    }

    public boolean isAuthCredentialsDefinedInPool() {
        return this.isAuthCredentialsDefinedInPool_;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("PoolMetaData : " + poolInfo);
        sb.append("\ntxSupport => " + txSupport_);
        sb.append("\nisPM_     => " + isPM_);
        sb.append("\nisNonTx_  => " + isNonTx_);
        sb.append("\nisLazyEnlistable_  => " + lazyEnlistable_);
        sb.append("\nisLazyAssociatable  => " + lazyAssoc_);
        sb.append("\nsecurityMap => " + runtimeSecurityMap.toString());
        return sb.toString();
    }
}
