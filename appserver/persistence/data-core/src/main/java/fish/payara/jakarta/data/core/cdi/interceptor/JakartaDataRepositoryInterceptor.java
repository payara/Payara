package fish.payara.jakarta.data.core.cdi.interceptor;

import jakarta.annotation.Priority;
import jakarta.data.repository.Repository;
import jakarta.enterprise.inject.Intercepted;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@JakartaDataRepositoryAnnotationBinding
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 1)
public class JakartaDataRepositoryInterceptor {

    @Inject
    @Intercepted
    protected Bean<?> bean;
    
    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Repository repository = (Repository) context.getTarget();
        
        return context.proceed();
    }
}
