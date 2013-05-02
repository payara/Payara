/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.kernel.bean_validator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;

import org.glassfish.api.naming.NamespacePrefixes;
import org.glassfish.api.naming.NamedNamingObjectProxy;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

@Service
@NamespacePrefixes({BeanValidatorNamingProxy.nameForValidator, BeanValidatorNamingProxy.nameForValidatorFactory})
public class BeanValidatorNamingProxy implements NamedNamingObjectProxy {

    static final String nameForValidator = "java:comp/Validator";
    static final String nameForValidatorFactory = "java:comp/ValidatorFactory";

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @Inject @Named("ValidationNamingProxy") @Optional
    private NamedNamingObjectProxy cdiNamingProxy;

    public Object handle(String name) throws NamingException {
        Object result = null;

        // see if CDI is active, use BeanManager to obtain Validator/ValidatorFactory
        if (cdiNamingProxy != null) {
            result = cdiNamingProxy.handle(name);

            if (result != null) {
                return result;
            }
        }

        if (nameForValidator.equals(name)) {
            result = getValidator();
        } else if (nameForValidatorFactory.equals(name)) {
            result = getValidatorFactory();
        }
        return result;
    }

    private Validator getValidator() throws NamingException {
        if (null == validator) {
            try {
                ValidatorFactory factory = getValidatorFactory();
                ValidatorContext validatorContext = factory.usingContext();
                validator = validatorContext.getValidator();
            } catch (Throwable t) {
                NamingException ne = new NamingException("Error retrieving Validator for " + nameForValidator + " lookup");
                ne.initCause(t);
                throw ne;
            }
        }
        return validator;
    }

    private ValidatorFactory getValidatorFactory() throws NamingException {

        if (null == validatorFactory) {
            try {
                validatorFactory = Validation.buildDefaultValidatorFactory();
            } catch (Throwable t) {
                NamingException ne = new NamingException("Error retrieving ValidatorFactory for " + nameForValidatorFactory + " lookup");
                ne.initCause(t);
                throw ne;
            }
        }

        return validatorFactory;
    }



}
