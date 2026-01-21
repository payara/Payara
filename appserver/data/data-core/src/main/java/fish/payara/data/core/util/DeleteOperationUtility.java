/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2025-2026] Payara Foundation and/or its affiliates. All rights reserved.
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
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
import fish.payara.data.core.cdi.extension.QueryData;
import jakarta.data.repository.By;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.RollbackException;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import static fish.payara.data.core.util.DataCommonOperationUtility.endTransaction;
import static fish.payara.data.core.util.DataCommonOperationUtility.startTransactionAndJoin;
import static fish.payara.data.core.util.FindOperationUtility.getIDParameterName;

/**
 * Utility class used to process Jakarta Data delete operations
 */
public class DeleteOperationUtility {

    public static final Logger logger = Logger.getLogger(DeleteOperationUtility.class.getName());

    public static int processDeleteByOperation(Object[] args, Class<?> declaredEntityClass, TransactionManager tm,
                                               EntityManager em, QueryData dataForQuery, Method method)
            throws SystemException, NotSupportedException, HeuristicRollbackException,
            HeuristicMixedException, RollbackException, jakarta.transaction.RollbackException {

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
                    attributeValue = preprocessAttributeName(dataForQuery.getEntityMetadata(), attributeValue);
                }
                builder.append("o.").append(attributeValue).append("=?").append(queryPosition);
            }
            queryPosition++;
        }

        if (hasWhere) {
            builder.append(")");
        }

        startTransactionAndJoin(tm, em, dataForQuery);
        Query q = em.createQuery(builder.toString());
        for (int i = 0; i < args.length; i++) {
            q.setParameter(i + 1, args[i]);
        }
        int rowsAffected = q.executeUpdate();
        endTransaction(tm, em, dataForQuery);

        // Clear cache for the affected entity after DELETE
        clearCaches(em, declaredEntityClass);

        logger.info("Rows affected from delete operation: " + rowsAffected);
        return rowsAffected;
    }

    private static String preprocessAttributeName(EntityMetadata entityMetadata, String attributeName) {
        if (attributeName.endsWith(")")) {
            return getIDParameterName(entityMetadata, attributeName);
        }
        if (entityMetadata.getAttributeNames().containsKey(attributeName.toLowerCase())) {
            return entityMetadata.getAttributeNames().get(attributeName.toLowerCase());
        }
        throw new IllegalArgumentException("The attribute " + attributeName +
                " is not mapped on the entity " + entityMetadata.getEntityName());
    }

    private static void clearCaches(EntityManager entityManager, Class<?> entityClass) {
        EntityManagerFactory factory = entityManager.getEntityManagerFactory();
        if (factory != null) {
            Cache cache = factory.getCache();
            if (cache != null) {
                // Only evict the affected entity type, not all entities
                cache.evict(entityClass);
            }
        }
        entityManager.clear();
    }

}
