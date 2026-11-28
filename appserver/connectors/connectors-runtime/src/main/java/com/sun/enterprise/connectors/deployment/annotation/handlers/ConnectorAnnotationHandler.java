/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019-2026] Payara Foundation and/or its affiliates

package com.sun.enterprise.connectors.deployment.annotation.handlers;

import com.sun.enterprise.deployment.AuthMechanism;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.LicenseDescriptor;
import com.sun.enterprise.deployment.OutboundResourceAdapter;
import com.sun.enterprise.deployment.annotation.context.RarBundleContext;
import com.sun.enterprise.deployment.annotation.handlers.AbstractHandler;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.apf.*;
import org.glassfish.apf.impl.HandlerProcessingResultImpl;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jvnet.hk2.annotations.Service;

import jakarta.resource.spi.AuthenticationMechanism;
import jakarta.resource.spi.Connector;
import jakarta.resource.spi.TransactionSupport;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * @author Jagadish Ramu
 */
@Service
@AnnotationHandlerFor(Connector.class)
public class ConnectorAnnotationHandler extends AbstractHandler {

    protected final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(ConnectorAnnotationHandler.class);

    public HandlerProcessingResult processAnnotation(AnnotationInfo element) throws AnnotationProcessorException {
        AnnotatedElementHandler aeHandler = element.getProcessingContext().getHandler();

        List<Class<? extends Annotation>> list = new ArrayList<Class<? extends Annotation>>();
        list.add(getAnnotationType());
/*
        list.add(SecurityPermission.class);
        list.add(AuthenticationMechanism.class);
*/

        if (aeHandler instanceof RarBundleContext) {
            RarBundleContext rarContext = (RarBundleContext) aeHandler;
            ConnectorDescriptor desc = rarContext.getDescriptor();
            ClassInfo annotatedClass = element.getAnnotatedElement().asClass();
            if(desc.getResourceAdapterClass().equals("")){
                desc.addConnectorAnnotation(element);
                return getSuccessfulProcessedResult(list);
            }else if(!isResourceAdapterClass(element.getIndex(), annotatedClass)){
                desc.addConnectorAnnotation(element);
                return getSuccessfulProcessedResult(list);
            }else if(!desc.getResourceAdapterClass().equals(annotatedClass.name().toString())){
                desc.addConnectorAnnotation(element);
                return getSuccessfulProcessedResult(list);
            }else{
                processDescriptor(annotatedClass, element, desc);
                desc.setValidConnectorAnnotationProcessed(true);
            }
        } else {
            String logMessage = "Not a rar bundle context";
            return getFailureResult(element, logMessage, true);
        }
        return getSuccessfulProcessedResult(list);
    }

    /**
     * @param annotatedClass the class carrying the {@code @Connector} annotation
     * @param connector the annotation, resolved in the index of the deployment it was found in
     */
    public static void processDescriptor(ClassInfo annotatedClass, AnnotationInfo connector, ConnectorDescriptor desc) {
        //TODO don't use deprecated methods

        //TODO For *all annotations* need to ignore "default" or unspecified attributes
        //TODO make sure that the annotation defined defaults are the defaults of DD and DOL

        IndexView index = connector.getIndex();

        String[] description = connector.stringArrayValue("description");
        if (desc.getDescription().equals("") && description.length > 0) {
            desc.setDescription(convertStringArrayToStringBuilder(description));
        }

        String[] displayName = connector.stringArrayValue("displayName");
        if (desc.getDisplayName().equals("") && displayName.length > 0) {
            desc.setDisplayName(convertStringArrayToStringBuilder(displayName));
        }

        String[] smallIcon = connector.stringArrayValue("smallIcon");
        if ((desc.getSmallIconUri() == null || desc.getSmallIconUri().equals(""))
                && smallIcon.length > 0) {
            desc.setSmallIconUri(convertStringArrayToStringBuilder(smallIcon));
        }

        String[] largeIcon = connector.stringArrayValue("largeIcon");
        if ((desc.getLargeIconUri() == null || desc.getLargeIconUri().equals(""))
                && largeIcon.length > 0) {
            desc.setLargeIconUri(convertStringArrayToStringBuilder(largeIcon));
        }

        String vendorName = connector.stringValue("vendorName");
        if (desc.getVendorName().equals("") && !vendorName.equals("")) {
            desc.setVendorName(vendorName);
        }

        String eisType = connector.stringValue("eisType");
        if (desc.getEisType().equals("") && !eisType.equals("")) {
            desc.setEisType(eisType);
        }

        String version = connector.stringValue("version");
        if (desc.getVersion().equals("") && !version.equals("")) {
            desc.setVersion(version);
        }

/*
        if(!desc.isModuleNameSet() && !connector.moduleName().equals("")){
            desc.getModuleDescriptor().setModuleName(connector.moduleName());
        }
*/

        if (desc.getLicenseDescriptor() == null) {
            // We will be able to detect whether license description is specified in annotation
            // or not, but "license required" can't be detected. Hence taking the annotated values *always*
            // if DD does not have an equivalent
            String[] licenseDescriptor = connector.stringArrayValue("licenseDescription");
            boolean licenseRequired = connector.booleanValue("licenseRequired");
            LicenseDescriptor ld = new LicenseDescriptor();
            ld.setDescription(convertStringArrayToStringBuilder(licenseDescriptor));
            ld.setLicenseRequired(licenseRequired);
            desc.setLicenseDescriptor(ld);
        }

        AnnotationInstance[] auths = connector.nestedArrayValue("authMechanisms");
        if (auths.length > 0) {
            for (AnnotationInstance auth : auths) {
                String authMechString = AnnotationValues.string(index, auth, "authMechanism");
                int authMechInt = AuthMechanism.getAuthMechInt(authMechString);

                // check whether the same auth-mechanism is defined in DD also,
                // possible change could be with auth-mechanism's credential-interface for a particular
                // auth-mechanism-type
                boolean ignore = false;
                OutboundResourceAdapter ora = getOutbound(desc);
                Set ddAuthMechanisms = ora.getAuthMechanisms();

                for (Object o : ddAuthMechanisms) {
                    AuthMechanism ddAuthMechanism = (AuthMechanism) o;
                    if (ddAuthMechanism.getAuthMechType().equals(authMechString)) {
                        ignore = true;
                        break;
                    }
                }

                // if it was not specified in DD, add it to connector-descriptor
                if (!ignore) {
                    AuthenticationMechanism.CredentialInterface credentialInterface = AnnotationValues.enumValue(
                            index, auth, "credentialInterface", AuthenticationMechanism.CredentialInterface.class);
                    String credentialInterfaceName = OutboundResourceAdapter.getCredentialInterfaceName(credentialInterface);
                    //XXX: Siva: For now use the first provided description
                    String[] authDescription = AnnotationValues.strings(index, auth, "description");
                    String firstDesc = "";
                    if(authDescription.length > 0){
                        firstDesc = authDescription[0];
                    }
                    AuthMechanism authM = new AuthMechanism(firstDesc, authMechInt, credentialInterfaceName);
                    ora.addAuthMechanism(authM);
                }
            }
        }

        // merge DD and annotation entries of security-permission
        AnnotationInstance[] perms = connector.nestedArrayValue("securityPermissions");
        if (perms.length > 0) {
            for (AnnotationInstance perm : perms) {
                String permissionSpec = AnnotationValues.string(index, perm, "permissionSpec");
                boolean ignore = false;
                // check whether the same permission is defined in DD also,
                // though it does not make any functionality difference except possible
                // "Description" change
                Set ddSecurityPermissions = desc.getSecurityPermissions();
                for (Object o : ddSecurityPermissions) {
                    com.sun.enterprise.deployment.SecurityPermission ddSecurityPermission =
                            (com.sun.enterprise.deployment.SecurityPermission) o;
                    if (ddSecurityPermission.getPermission().equals(permissionSpec)) {
                        ignore = true;
                        break;
                    }
                }

                // if it was not specified in DD, add it to connector-descriptor
                if (!ignore) {
                    com.sun.enterprise.deployment.SecurityPermission sp =
                            new com.sun.enterprise.deployment.SecurityPermission();
                    sp.setPermission(permissionSpec);
                    //XXX: Siva for now use the first provided Description
                    String[] permDescription = AnnotationValues.strings(index, perm, "description");
                    String firstDesc = "";
                    if(permDescription.length > 0) firstDesc = permDescription[0];
                    sp.setDescription(firstDesc);
                    desc.addSecurityPermission(sp);
                }
            }
        }

        //we should not create outbound resource adapter unless it is required.
        //this is necessary as the default value processing in the annotation may
        //result in outbound to be defined without any connection-definition which is an issue.

        //if reauth is false, we can ignore it as default value in dol is also false.
        boolean reauthenticationSupport = connector.booleanValue("reauthenticationSupport");
        if(reauthenticationSupport){
            OutboundResourceAdapter ora = getOutbound(desc);
            if(!ora.isReauthenticationSupportSet()){
                ora.setReauthenticationSupport(reauthenticationSupport);
            }
        }
        //if transaction-support is no-transaction, we can ignore it as default value in dol is also no-transaction.
        TransactionSupport.TransactionSupportLevel transactionSupport =
                connector.enumValue("transactionSupport", TransactionSupport.TransactionSupportLevel.class);
        if(!transactionSupport.equals(TransactionSupport.TransactionSupportLevel.NoTransaction)){
            OutboundResourceAdapter ora = getOutbound(desc);
            if(!ora.isTransactionSupportSet()){
                ora.setTransactionSupport(transactionSupport.toString());
            }
        }

        //merge the DD & annotation specified values of required-inflow-contexts
        //merge involves simple union of class-names of inflow-contexts of DD and annotation

        //due to the above approach, its not possible to switch off one of the required-inflow-contexts ?

        //TODO need to check support and throw exception ?

        for (String ic : connector.classArrayValue("requiredWorkContexts")) {
            desc.addRequiredWorkContext(ic);
        }

        if (desc.getResourceAdapterClass().equals("")) {
            if (isResourceAdapterClass(index, annotatedClass)) {
                desc.setResourceAdapterClass(annotatedClass.name().toString());
            }
        }
    }

    public static boolean isResourceAdapterClass(IndexView index, ClassInfo claz){
        return ClassHierarchy.isSubtype(index, claz, jakarta.resource.spi.ResourceAdapter.class);
    }

    public static String convertStringArrayToStringBuilder(String[] stringArray) {
        StringBuilder result = new StringBuilder();
        if (stringArray != null) {
            for (String string : stringArray) {
                result.append(string);
            }
        }
        return result.toString();
    }

    /**
     * @return a default processed result
     */
    protected HandlerProcessingResult getSuccessfulProcessedResult(List<Class<? extends Annotation>> annotationTypes) {

        HandlerProcessingResultImpl result = new HandlerProcessingResultImpl();

        for(Class<? extends Annotation> annotation : annotationTypes){
            result.addResult(annotation, ResultType.PROCESSED);
        }
        return result;
    }


    public Class<? extends Annotation>[] getTypeDependencies() {
        return null;
    }

    public static OutboundResourceAdapter getOutbound(ConnectorDescriptor desc) {
        if (!desc.getOutBoundDefined()) {
            desc.setOutboundResourceAdapter(new OutboundResourceAdapter());
        }
        return desc.getOutboundResourceAdapter();
    }

    private HandlerProcessingResultImpl getFailureResult(AnnotationInfo element, String message, boolean doLog) {
        HandlerProcessingResultImpl result = new HandlerProcessingResultImpl();
        result.addResult(getAnnotationType(), ResultType.FAILED);
        if (doLog) {
            String className = element.getDeclaringClass().name().toString();
            Object args[] = new Object[]{
                element.getAnnotation(),
                className,
                message,
            };
            String localString = localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.connectorannotationfailure",
                    "failed to handle annotation [ {0} ] on class [ {1} ], reason : {2}", args);
            logger.log(Level.WARNING, localString);
        }
        return result;
    }
}
