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
 * ConstraintOperation.java
 *
 * Create on March 3, 2000
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.sql.constraint;

import com.sun.jdo.spi.persistence.support.sqlstore.ActionDesc;

/**
 */
public class ConstraintOperation extends ConstraintNode {
    public static final int PROP_ORDERBY = 1;

    public static final int PROP_ORDERBY_DESC = 2;

    public int operation;

    public ConstraintOperation() {
        super();
    }

    public ConstraintOperation(int operation) {
        super();

        this.operation = operation;
    }


    @Override
    public int hasProperty(int propertyKey) {
        // RESOLVE:  We should probably get rid of this and standardize on the
        // SqlRetrieveDesc.GetOperationInfo method.
        if (propertyKey == PROP_ORDERBY) {
            if (operation == ActionDesc.OP_ORDERBY
                    || operation == ActionDesc.OP_ORDERBY_DESC) {
                return ConstraintNode.PROP_TRUE;
            } else {
                return ConstraintNode.PROP_FALSE;
            }
        }
        return ConstraintNode.PROP_UNKNOWN;
    }
}
