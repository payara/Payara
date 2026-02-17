package fish.payara.data.core.cdi.extension;

import java.lang.reflect.Method;

public class QueryMetadata {
    protected Class<?> repositoryInterface;
    protected Method method;
    protected Class<?> entityParamType;
    protected Class<?> declaredEntityClass;
    protected QueryType queryType;
    protected EntityMetadata entityMetadata;

    public QueryMetadata(Class<?> repositoryInterface, Method method, Class<?> declaredEntityClass, Class<?> entityParamType, QueryType queryType, EntityMetadata entityMetadata) {
        this.repositoryInterface = repositoryInterface;
        this.method = method;
        this.declaredEntityClass = declaredEntityClass;
        this.entityParamType = entityParamType;
        this.queryType = queryType;
        this.entityMetadata = entityMetadata;
    }

    public Class<?> getRepositoryInterface() {
        return repositoryInterface;
    }

    public void setRepositoryInterface(Class<?> repositoryInterface) {
        this.repositoryInterface = repositoryInterface;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Class<?> getEntityParamType() {
        return entityParamType;
    }

    public void setEntityParamType(Class<?> entityParamType) {
        this.entityParamType = entityParamType;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public void setQueryType(QueryType queryType) {
        this.queryType = queryType;
    }

    public Class<?> getDeclaredEntityClass() {
        return declaredEntityClass;
    }

    public void setDeclaredEntityClass(Class<?> declaredEntityClass) {
        this.declaredEntityClass = declaredEntityClass;
    }

    public EntityMetadata getEntityMetadata() {
        return entityMetadata;
    }

    public void setEntityMetadata(EntityMetadata entityMetadata) {
        this.entityMetadata = entityMetadata;
    }
}
