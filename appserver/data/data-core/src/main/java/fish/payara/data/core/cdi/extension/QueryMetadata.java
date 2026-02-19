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

    public Method getMethod() {
        return method;
    }

    public Class<?> getEntityParamType() {
        return entityParamType;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public Class<?> getDeclaredEntityClass() {
        return declaredEntityClass;
    }

    public EntityMetadata getEntityMetadata() {
        return entityMetadata;
    }
}
