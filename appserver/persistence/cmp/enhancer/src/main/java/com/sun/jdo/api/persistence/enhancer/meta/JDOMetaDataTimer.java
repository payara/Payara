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

package com.sun.jdo.api.persistence.enhancer.meta;

import com.sun.jdo.api.persistence.enhancer.util.Support;


//@olsen: new class
public final class JDOMetaDataTimer
    extends Support
    implements JDOMetaData
{
    // delegate
    final protected JDOMetaData delegate;

    /**
     * Creates an instance.
     */
    public JDOMetaDataTimer(JDOMetaData delegate)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        affirm(delegate);
        this.delegate = delegate;
    }

    public boolean isPersistenceCapableClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        try {
            timer.push("JDOMetaData.isPersistenceCapableClass(String)",//NOI18N
                       "JDOMetaData.isPersistenceCapableClass(" + classPath + ")");//NOI18N
            return delegate.isPersistenceCapableClass(classPath);
        } finally {
            timer.pop();
        }
    }

    public boolean isTransientClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        try {
            timer.push("JDOMetaData.isTransientClass(String)",//NOI18N
                       "JDOMetaData.isTransientClass(" + classPath + ")");//NOI18N
            return delegate.isTransientClass(classPath);
        } finally {
            timer.pop();
        }
    }

    public boolean isPersistenceCapableRootClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        try {
            timer.push("JDOMetaData.isPersistenceCapableRootClass(String)",//NOI18N
                       "JDOMetaData.isPersistenceCapableRootClass(" + classPath + ")");//NOI18N
            return delegate.isPersistenceCapableRootClass(classPath);
        } finally {
            timer.pop();
        }
    }

    public String getSuperClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        try {
            timer.push("JDOMetaData.getSuperClass(String)",//NOI18N
                       "JDOMetaData.getSuperClass(" + classPath + ")");//NOI18N
            return delegate.getSuperClass(classPath);
        } finally {
            timer.pop();
        }
    }

    public String getPersistenceCapableRootClass(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        try {
            timer.push("JDOMetaData.getPersistenceCapableRootClass(String)",//NOI18N
                       "JDOMetaData.getPersistenceCapableRootClass(" + classPath + ")");//NOI18N
            return delegate.getPersistenceCapableRootClass(classPath);
        } finally {
            timer.pop();
        }
    }

    public boolean isSecondClassObjectType(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        try {
            timer.push("JDOMetaData.isSecondClassObjectType(String)",//NOI18N
                       "JDOMetaData.isSecondClassObjectType(" + classPath + ")");//NOI18N
            return delegate.isSecondClassObjectType(classPath);
        } finally {
            timer.pop();
        }
    }

    public boolean isMutableSecondClassObjectType(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        try {
            timer.push("JDOMetaData.isMutableSecondClassObjectType(String)",//NOI18N
                       "JDOMetaData.isMutableSecondClassObjectType(" + classPath + ")");//NOI18N
            return delegate.isMutableSecondClassObjectType(classPath);
        } finally {
            timer.pop();
        }
    }

    public boolean isPersistentField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        try {
            timer.push("JDOMetaData.isPersistentField(String,String)",//NOI18N
                       "JDOMetaData.isPersistentField(" + classPath//NOI18N
                       + ", " + fieldName + ")");//NOI18N
            return delegate.isPersistentField(classPath, fieldName);
        } finally {
            timer.pop();
        }
    }

    public boolean isTransactionalField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        try {
            timer.push("JDOMetaData.isTransactionalField(String,String)",//NOI18N
                       "JDOMetaData.isTransactionalField(" + classPath//NOI18N
                       + ", " + fieldName + ")");//NOI18N
            return delegate.isTransactionalField(classPath, fieldName);
        } finally {
            timer.pop();
        }
    }

    public boolean isPrimaryKeyField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        try {
            timer.push("JDOMetaData.isPrimaryKeyField(String,String)",//NOI18N
                       "JDOMetaData.isPrimaryKeyField(" + classPath//NOI18N
                       + ", " + fieldName + ")");//NOI18N
            return delegate.isPrimaryKeyField(classPath, fieldName);
        } finally {
            timer.pop();
        }
    }

    public boolean isDefaultFetchGroupField(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        try {
            timer.push("JDOMetaData.isDefaultFetchGroupField(String,fieldName)",//NOI18N
                       "JDOMetaData.isDefaultFetchGroupField(" + classPath//NOI18N
                       + ", " + fieldName + ")");//NOI18N
            return delegate.isDefaultFetchGroupField(classPath, fieldName);
        } finally {
            timer.pop();
        }
    }

    public int getFieldNo(String classPath, String fieldName)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        try {
            timer.push("JDOMetaData.getFieldNo(String, String)",//NOI18N
                       "JDOMetaData.getFieldNo(" + classPath//NOI18N
                       + ", " + fieldName + ")");//NOI18N
            return delegate.getFieldNo(classPath, fieldName);
        } finally {
            timer.pop();
        }
    }

    public String[] getManagedFields(String classPath)
        throws JDOMetaDataUserException, JDOMetaDataFatalError
    {
        try {
            timer.push("JDOMetaData.getPersistentFields(String)",//NOI18N
                       "JDOMetaData.getPersistentFields(" + classPath + ")");//NOI18N
            return delegate.getManagedFields(classPath);
        } finally {
            timer.pop();
        }
    }
}
