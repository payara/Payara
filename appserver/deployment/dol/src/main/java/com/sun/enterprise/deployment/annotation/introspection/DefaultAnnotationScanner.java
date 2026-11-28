/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2026] Payara Foundation and/or its affiliates
package com.sun.enterprise.deployment.annotation.introspection;

import com.sun.enterprise.deployment.annotation.factory.SJSASFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.annotations.Service;

import java.util.Set;

/**
 * This class contains the names of all the annotation types the deployment layer has handlers
 * for, i.e. the annotations whose presence makes a class subject to annotation processing.
 *
 * @author Jerome Dochez
 */
@Service(name="default")
@Singleton
public class DefaultAnnotationScanner implements PostConstruct {

    @Inject
    SJSASFactory factory;

    private Set<String> annotations;
    private Set<String> annotationsMetaDataComplete;

    @Override
    public void postConstruct() {
        annotations = factory.getAnnotations(false);
        annotationsMetaDataComplete = factory.getAnnotations(true);
    }

    /**
     * @return the fully qualified names of the handled annotation types, the subset still
     * processed for metadata-complete modules when so requested
     */
    public Set<String> getAnnotations(boolean isMetaDataComplete) {
        return isMetaDataComplete ? annotationsMetaDataComplete : annotations;
    }

    public Set<String> getAnnotations() {
        return getAnnotations(false);
    }
}
