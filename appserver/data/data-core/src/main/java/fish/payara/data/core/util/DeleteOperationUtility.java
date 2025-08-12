/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2025] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.data.core.util;

import fish.payara.data.core.cdi.extension.EntityMetadata;
import jakarta.data.repository.By;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import static fish.payara.data.core.util.FindOperationUtility.getIDParameterName;

/**
 * Utility class used to process Jakarta Data delete operations
 */
public class DeleteOperationUtility {

    public static final Logger logger = Logger.getLogger(DeleteOperationUtility.class.getName());

    public static int processDeleteByOperation(Object[] args, Class<?> declaredEntityClass, TransactionManager tm,
                                               EntityManager em, EntityMetadata entityMetadata, Method method)
            throws SystemException, NotSupportedException, HeuristicRollbackException,
            HeuristicMixedException, RollbackException {

        StringBuilder builder = new StringBuilder();
        builder.append("DELETE FROM ").append(declaredEntityClass.getSimpleName())
                .append(" o");

        String attributeValue = null;
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        int queryPosition = 1;
        boolean hasWhere = false;

        for (Annotation[] annotations : parameterAnnotations) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof By) {
                    attributeValue = ((By) annotation).value();
                }
                if (!hasWhere) {
                    builder.append(" WHERE (");
                    hasWhere = true;
                } else {
                    builder.append(" AND ");
                }

                if (attributeValue != null) {
                    attributeValue = preprocessAttributeName(entityMetadata, attributeValue);
                }
                builder.append("o.").append(attributeValue).append("=?").append(queryPosition);
            }
            queryPosition++;
        }

        if (hasWhere) {
            builder.append(")");
        }

        tm.begin();
        em.joinTransaction();
        Query q = em.createQuery(builder.toString());
        for (int i = 0; i < args.length; i++) {
            q.setParameter(i + 1, args[i]);
        }
        int rowsAffected = q.executeUpdate();
        em.flush();
        tm.commit();

        logger.info("Rows affected from delete operation: " + rowsAffected);
        return rowsAffected;
    }

    private static String preprocessAttributeName(EntityMetadata entityMetadata, String attributeName) {
        if (attributeName.endsWith(")")) {
            return getIDParameterName(attributeName);
        }
        if (entityMetadata.getAttributeNames().containsKey(attributeName.toLowerCase())) {
            return entityMetadata.getAttributeNames().get(attributeName.toLowerCase());
        }
        throw new IllegalArgumentException("The attribute " + attributeName +
                " is not mapped on the entity " + entityMetadata.getEntityName());
    }

    public static int processDeleteByIdOperation(Object[] args, Class<?> declaredEntityClass, TransactionManager tm,
                                                 EntityManager em, String idNameValue) throws SystemException,
            NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        String query = createDeleteOperationSingleEntity(declaredEntityClass, idNameValue);
        tm.begin();
        em.joinTransaction();
        Query q = em.createQuery(query);
        q.setParameter(1, args[0]);
        int rowsAffected = q.executeUpdate();
        em.flush();
        tm.commit();
        logger.info("Rows affected from delete operation:" + rowsAffected);
        return rowsAffected;
    }


    public static String createDeleteOperationSingleEntity(Class<?> entityClass, String idNameValue) {
        StringBuilder builder = new StringBuilder();
        builder.append("DELETE FROM ").append(entityClass.getSimpleName())
                .append(" ").append("o").append(" WHERE ").append("(")
                .append("o.").append(getIDParameterName(idNameValue)).append("=?1)");
        return builder.toString();
    }

}
