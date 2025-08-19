package fish.payara.data.core.util;

import fish.payara.data.core.cdi.extension.EntityMetadata;
import fish.payara.data.core.cdi.extension.QueryData;
import jakarta.data.repository.By;
import jakarta.persistence.EntityManager;
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

}
