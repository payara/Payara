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

/*
 *  AutoPersistentNewDeleted.java    March 10, 2000    Steffi Rauschenbach
 */

package com.sun.jdo.spi.persistence.support.sqlstore.state;

import com.sun.jdo.api.persistence.support.JDOUserException;
import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.spi.persistence.support.sqlstore.ActionDesc;

import java.util.ResourceBundle;

public class AutoPersistentNewDeleted extends LifeCycleState {
    /**
     * I18N message handler
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
            AutoPersistentNewDeleted.class.getClassLoader());

    public AutoPersistentNewDeleted() {
        // these flags are set only in the constructor
        // and shouldn't be changed afterwards
        // (cannot make them final since they are declared in superclass
        // but their values are specific to subclasses)
        isPersistent = true;
        isPersistentInDataStore = false;
        isAutoPersistent = true;
        isTransactional = true;
        isDirty = true;
        isNew = true;
        isDeleted = true;
        isNavigable = false;
        isRefreshable = false;
        isBeforeImageUpdatable = false;
        needsRegister = true;
        needsReload = false;
        needsRestoreOnRollback = true;
        updateAction = ActionDesc.LOG_NOOP;

        // The following flag allows merge
        needMerge = false;

        stateType = AP_NEW_DELETED;
    }

    public LifeCycleState transitionMakePersistent() {
        return changeState(P_NEW_DELETED);
    }

    public LifeCycleState transitionCommit(boolean retainValues) {
        return changeState(TRANSIENT);
    }

    public LifeCycleState transitionRollback(boolean retainValues) {
        return changeState(TRANSIENT);
    }

    public LifeCycleState transitionReadField(boolean optimisitic, boolean nontransactonalRead,
                                              boolean transactionActive) {
        // Cannot read a deleted object
        throw new JDOUserException(I18NHelper.getMessage(messages,
                "jdo.lifecycle.deleted.accessField")); // NOI18N
    }

    public LifeCycleState transitionWriteField(boolean transactionActive) {
        // Cannot update a deleted object
        throw new JDOUserException(I18NHelper.getMessage(messages,
                "jdo.lifecycle.deleted.accessField")); // NOI18N
    }

    public boolean needsRestoreOnRollback(boolean retainValues) {
        //
        // This is a special case where retores doesn't depend on
        // retainValues.
        //
        return needsRestoreOnRollback;
    }
}








