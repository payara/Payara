package org.glassfish.concurrent.runtime.deployment.annotation.handlers;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.deployment.annotation.context.ResourceContainerContext;
import com.sun.enterprise.deployment.annotation.handlers.AbstractResourceHandler;
import com.sun.enterprise.util.SystemPropertyConstants;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.inject.Inject;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.concurrent.admin.ManagedExecutorServiceManager;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.glassfish.resources.admin.cli.ResourceConstants;
import org.jvnet.hk2.annotations.Service;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.resources.admin.cli.ResourceConstants.MAXIMUM_POOL_SIZE;
import static org.glassfish.resources.admin.cli.ResourceConstants.TASK_QUEUE_CAPACITY;

@Service
@AnnotationHandlerFor(ManagedExecutorDefinition.class)
public class ManagedExecutorDefinitionHandler extends AbstractResourceHandler {

    private static final Logger logger = Logger.getLogger(ManagedExecutorDefinitionHandler.class.getName());

    @Inject
    private ManagedExecutorServiceManager managedExecutorServiceMgr;

    @Inject
    private Domain domain;

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo annotationInfo,
                                                        ResourceContainerContext[] resourceContainerContexts)
            throws AnnotationProcessorException {
        logger.log(Level.INFO, "Entering processAnnotation");
        ManagedExecutorDefinition managedExecutorDefinition = (ManagedExecutorDefinition) annotationInfo.getAnnotation();
        return processAnnotation(managedExecutorDefinition, annotationInfo, resourceContainerContexts);
    }

    protected HandlerProcessingResult processAnnotation(ManagedExecutorDefinition managedExecutorDefinition,
                                                        AnnotationInfo annotationInfo,
                                                        ResourceContainerContext[] resourceContainerContexts) {
        logger.log(Level.INFO, "Trying to create custom executor service by annotation");
        ResourceStatus rs;
        HashMap attrList = new HashMap();
        setAttributeList(attrList, managedExecutorDefinition);
        try {
            rs = managedExecutorServiceMgr.create(domain.getResources(), attrList, null, SystemPropertyConstants.DAS_SERVER_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return getDefaultProcessedResult();
    }

    protected void setAttributeList(HashMap attrList, ManagedExecutorDefinition managedExecutorDefinition) {
        //this is temporal I need to check how to set the correct values
        attrList.put(ResourceConstants.JNDI_NAME, managedExecutorDefinition.name());
        attrList.put(ResourceConstants.CONTEXT_INFO_ENABLED, Boolean.TRUE.toString());
        attrList.put(ResourceConstants.CONTEXT_INFO, managedExecutorDefinition.context());
        attrList.put(ResourceConstants.THREAD_PRIORITY,
                "3");
        attrList.put(ResourceConstants.LONG_RUNNING_TASKS,
                "true");
        attrList.put(ResourceConstants.HUNG_AFTER_SECONDS,
                "100");
        attrList.put(ResourceConstants.CORE_POOL_SIZE,
                "1");
        attrList.put(ResourceConstants.KEEP_ALIVE_SECONDS,
                "88");
        attrList.put(ResourceConstants.THREAD_LIFETIME_SECONDS,
                "99");
        attrList.put(ServerTags.DESCRIPTION, "Executor annotation test");
        attrList.put(ResourceConstants.ENABLED, "true");
        attrList.put(MAXIMUM_POOL_SIZE, "5");
        attrList.put(TASK_QUEUE_CAPACITY, "1234");
    }
}
