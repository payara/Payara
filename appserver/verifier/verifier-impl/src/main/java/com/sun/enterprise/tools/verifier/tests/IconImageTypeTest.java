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

package com.sun.enterprise.tools.verifier.tests;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.SpecVersionMapper;
import com.sun.enterprise.deployment.*;
import org.glassfish.web.deployment.descriptor.ServletFilterDescriptor;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Enumeration;

import org.glassfish.api.deployment.archive.Archive;
import org.glassfish.deployment.common.Descriptor;

/**
 * This test is deried from Java EE platform spec.
 * See javaee_5.xsd
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class IconImageTypeTest extends VerifierTest implements VerifierCheck{
    private Collection<String> smallIconUris = new ArrayList<String>();
    private Collection<String> largeIconUris = new ArrayList<String>();
    private Descriptor descriptor;
    private Result result;
    ComponentNameConstructor compName;
    private static final String[] allowableImageTypesForJavaEEOlderThan5 = {".gif", ".jpg"};
    private static final String[] allowableImageTypesForJavaEE5 = {".gif", ".jpg", ".png"};
    public Result check(Descriptor descriptor) {
        this.descriptor = descriptor;
        compName = getVerifierContext().getComponentNameConstructor();
        result = getInitializedResult();
        result.setStatus(Result.PASSED);
        addGoodDetails(result, compName);
        result.passed(smh.getLocalString
                      (getClass().getName() + ".passed", //NOI18N
                       "No errors were detected.")); // NOI18N

        // Now collect all the Icon URIs that we are going to test
        collectIconURIs();
        testIconURIType();
        testIconURIExistence();
        return result;
    }

    private void testIconURIType() {
        String[] allowableImageTypes;
        String JavaEESchemaVersion = getVerifierContext().getJavaEEVersion();
        if (JavaEESchemaVersion.compareTo(SpecVersionMapper.JavaEEVersion_5) < 0){
            allowableImageTypes = allowableImageTypesForJavaEEOlderThan5;
        } else {
            allowableImageTypes = allowableImageTypesForJavaEE5;
        }

        Collection<String> allURIs = new ArrayList<String>(smallIconUris);
        allURIs.addAll(largeIconUris);
        for(String uri : allURIs){
            boolean passed = false;
            for(String allowableType : allowableImageTypes) {
                if(uri.endsWith(allowableType)) {
                    passed = true;
                    break;
                }
            }
            if(!passed){
                addErrorDetails(result, compName);
                result.failed(smh.getLocalString
                        (getClass().getName() + ".failedImageType",
                                "Error: Unsupported image type used in icon image URI [ {0} ].",
                                new Object[]{uri}));
            }
        }
    }

    private void testIconURIExistence() {
        Collection<String> allURIs = new ArrayList<String>(smallIconUris);
        allURIs.addAll(largeIconUris);
        for(String uri : allURIs){
            Archive moduleArchive = getVerifierContext().getModuleArchive();
            boolean passed = false;
            for(Enumeration entries = moduleArchive.entries(); entries.hasMoreElements();){
                if(uri.equals(entries.nextElement())) {
                    passed = true;
                    break;
                }
            }
            if(!passed){
                addErrorDetails(result, compName);
                result.failed(smh.getLocalString
                        (getClass().getName() + ".failedExistence",
                                "Error: icon image URI [ {0} ] is not packaged inside [ {1} ].",
                                new Object[]{uri, getVerifierContext().getModuleArchive().getURI()}));
            }
        }
    }

    private void collectIconURIs(){
        // in the absence of a proper Visitor pattern I am left with
        // little option but to use instanceof
        if(descriptor instanceof Application)
            collectIconURIs((Application)descriptor);
        else if(descriptor instanceof ApplicationClientDescriptor)
            collectIconURIs((ApplicationClientDescriptor)descriptor);
        else if(descriptor instanceof EjbDescriptor)
            collectIconURIs((EjbDescriptor)descriptor);
        else if(descriptor instanceof ConnectorDescriptor)
            collectIconURIs((ConnectorDescriptor)descriptor);
        else if(descriptor instanceof WebBundleDescriptor)
            collectIconURIs((WebBundleDescriptor)descriptor);
        else if(descriptor instanceof WebServiceEndpoint)
            collectIconURIs((WebServiceEndpoint)descriptor);
        else if(descriptor instanceof ServiceReferenceDescriptor)
            collectIconURIs((ServiceReferenceDescriptor)descriptor);
        else {
            // every time we introduce a new CheckMgrImpl, this will fail
            // that way we can be notified of the fact that this method needs
            // to be modified as well.
            throw new RuntimeException("Unexpected descriptor type.");
        }
    }

    // implementation that is common to descriptors that only contain
    // icon element at top level.
    private void collectIconURIs(Descriptor desc){
        String uri=desc.getSmallIconUri();
        if(uri!=null && uri.length()>0) smallIconUris.add(uri);
        uri = desc.getLargeIconUri();
        if(uri!=null && uri.length()>0) largeIconUris.add(uri);
    }

    private void collectIconURIs(WebBundleDescriptor webBundleDescriptor) {
        // this is for itself
        collectIconURIs((Descriptor)webBundleDescriptor);
        // now collect for each servlet
        for (WebComponentDescriptor o : webBundleDescriptor.getWebComponentDescriptors()){
            collectIconURIs(o);
        }
        // now collect for each servlet filter
        for (Object o : webBundleDescriptor.getServletFilterDescriptors()) {
            collectIconURIs(ServletFilterDescriptor.class.cast(o));
        }
    }

    private void collectIconURIs(WebServiceEndpoint webServiceEndpoint) {
        // WebService.xml is organised like this:
        // WebServicesDescriptor->WebService->WebServiceEndpoint
        // Since we don't have a CheckMgr that runs test for WebService.xml,
        // a work around would be to collect all Icons for all the parents
        // and test them here.
        // This means a problem there would be as many times as there are
        // end points.
        collectIconURIs(webServiceEndpoint.getWebService().getWebServicesDescriptor());
        collectIconURIs(webServiceEndpoint.getWebService());

        // this is for itself
        collectIconURIs((Descriptor)webServiceEndpoint);
        // now collect for each port-compont_handler in handler-chain
        for (Object o : webServiceEndpoint.getHandlers()){
            collectIconURIs(WebServiceHandler.class.cast(o));
        }
    }

    private void collectIconURIs(EjbDescriptor desc){
        // Since we don't have a CheckMgr that runs test for ejb-jar.xml,
        // a work around would be to collect all Icons for the parent
        // and test them here.
        // This means a problem there would be as many times as there are
        // beans.
        collectIconURIs(desc.getEjbBundleDescriptor());
        // this is for itself
        collectIconURIs((Descriptor)descriptor);
    }
}
