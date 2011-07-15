/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier.tests.persistence;

import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.persistence.AVKPersistenceUnitInfoImpl;
import com.sun.enterprise.tools.verifier.tests.VerifierCheck;
import com.sun.enterprise.tools.verifier.tests.VerifierTest;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import java.util.logging.Level;
import java.util.Properties;

import org.eclipse.persistence.exceptions.IntegrityException;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.glassfish.api.deployment.InstrumentableClassLoader;

/**
 * This test uses TopLink Essential to do the validation.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class DefaultProviderVerification extends VerifierTest
        implements VerifierCheck {
    public Result check(Descriptor descriptor) {
        PersistenceUnitDescriptor pu =
                PersistenceUnitDescriptor.class.cast(descriptor);
        Result result = getInitializedResult();
        result.setStatus(Result.PASSED);
        PersistenceProvider provider;
        final String appLocation =
                getVerifierContext().getAbstractArchive().getURI().getPath();
        final InstrumentableClassLoader cl =
                InstrumentableClassLoader.class.cast(pu.getParent().getClassLoader());
        PersistenceUnitInfo pi = new AVKPersistenceUnitInfoImpl(pu, appLocation, cl);
        logger.fine("PersistenceInfo for PU is :\n" + pi);
        Properties props = new Properties();
        // This property is set to indicate that TopLink should only
        // validate the descriptors etc. and not try to login to database.
        props.put(PersistenceUnitProperties.VALIDATION_ONLY_PROPERTY,
                "TRUE"); // NOI18N
        // This property is used so that TopLink throws validation exceptions
        // as opposed to printing CONFIG level messages to console.
        // e.g. if mapping file does not exist, we will get an exception.
        props.put(PersistenceUnitProperties.THROW_EXCEPTIONS,
                "TRUE"); // NOI18N

        // the following property is needed as it initializes the logger in TL
        props.put(PersistenceUnitProperties.TARGET_SERVER,
                      "SunAS9"); // NOI18N

        // Turn off enhancement during verification. For details,
        // refer to http://glassfish.dev.java.net/issues/show_bug.cgi?id=3295
        props.put(PersistenceUnitProperties.WEAVING, "FALSE");

        provider = new org.eclipse.persistence.jpa.PersistenceProvider();
        EntityManagerFactory emf = null;
        try {
            emf = provider.createContainerEntityManagerFactory(pi, props);
            logger.logp(Level.FINE, "DefaultProviderVerification", "check",
                    "emf = {0}", emf);
        } catch(IntegrityException ie){
            result.setStatus(Result.FAILED);
            addErrorDetails(result, getVerifierContext().getComponentNameConstructor());
            for(Object o: ie.getIntegrityChecker().getCaughtExceptions()){
                Exception e = Exception.class.cast(o);
                result.addErrorDetails(e.getMessage());
            }
        } catch (ValidationException ve) {
            addErrorDetails(result, getVerifierContext().getComponentNameConstructor());
            result.failed(ve.getMessage());
            logger.logp(Level.FINE, "DefaultProviderVerification", "check", "Following exception occurred", ve);
        } catch(DatabaseException de) {
            addErrorDetails(result, getVerifierContext().getComponentNameConstructor());
            result.failed(de.getMessage());
            logger.logp(Level.FINE, "DefaultProviderVerification", "check", "Following exception occurred", de);
        } catch(PersistenceException pe) {
            addErrorDetails(result, getVerifierContext().getComponentNameConstructor());
            result.failed(pe.getMessage());
            logger.logp(Level.FINE, "DefaultProviderVerification", "check", "Following exception occurred", pe);
        } finally {
            if(emf != null) {
                emf.close();
            }
        }
        return result;
    }

}
