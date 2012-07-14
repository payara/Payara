/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.common.util.admin;

import java.util.StringTokenizer;

import javax.inject.Singleton;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.ValidationInformation;
import org.glassfish.hk2.api.ValidationService;
import org.glassfish.hk2.api.Validator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.jvnet.hk2.annotations.Service;

/**
 * This file is used to turn on HK2 bind and unbind tracing
 * 
 * @author jwells
 *
 */
@Service
@Singleton
public class HK2BindTracingService implements ValidationService {
    private final static Filter ALL_FILTER = BuilderHelper.allFilter();
    private final static Filter NONE_FILTER = new Filter() {

        @Override
        public boolean matches(Descriptor d) {
            return false;
        }
        
    };
    private final static boolean TRACE_BINDS = Boolean.parseBoolean(
            System.getProperty("org.glassfish.hk2.tracing.binds", "false"));
    private final static String TRACE_BINDS_PATTERN =
            System.getProperty("org.glassfish.hk2.tracing.bindsPattern");
    private final static boolean TRACE_LOOKUPS = Boolean.parseBoolean(
            System.getProperty("org.glassfish.hk2.tracing.lookups", "false"));
    private final static String TRACE_LOOKUPS_PATTERN =
            System.getProperty("org.glassfish.hk2.tracing.lookupsPattern");
    
    private final static String STACK_PATTERN =
            System.getProperty("org.glassfish.hk2.tracing.binds.stackPattern");
    
    private final static Validator VALIDATOR = new ValidatorImpl();

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.ValidationService#getLookupFilter()
     */
    @Override
    public Filter getLookupFilter() {
        if (TRACE_LOOKUPS == true) return ALL_FILTER;
        
        return NONE_FILTER;
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.ValidationService#getValidator()
     */
    @Override
    public Validator getValidator() {
        return VALIDATOR;
    }
    
    private static boolean matchesPattern(String pattern, ActiveDescriptor<?> descriptor) {
        if (pattern == null) return true;
        
        StringTokenizer st = new StringTokenizer(pattern, "|");
        
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            
            if (descriptor.getImplementation().contains(token)) {
                return true;
            }
            
            for (String contract : descriptor.getAdvertisedContracts()) {
                if (contract.contains(token)) {
                    return true;
                }
            }
        }
        
        return false;        
    }
    
    private static class ValidatorImpl implements Validator {

        /* (non-Javadoc)
         * @see org.glassfish.hk2.api.Validator#validate(org.glassfish.hk2.api.ValidationInformation)
         */
        @Override
        public boolean validate(ValidationInformation info) {
            if (!TRACE_BINDS && !TRACE_LOOKUPS) return true;
            
            switch (info.getOperation()) {
            case BIND:
                if (TRACE_BINDS && matchesPattern(TRACE_BINDS_PATTERN, info.getCandidate())) {
                    System.out.println("HK2 Tracing (BIND): " + info.getCandidate());
                }
                break;
            case UNBIND:
                if (TRACE_BINDS && matchesPattern(TRACE_BINDS_PATTERN, info.getCandidate())) {
                    System.out.println("HK2 Tracing (UNBIND): " + info.getCandidate());
                }
                break;
            case LOOKUP:
                if (TRACE_LOOKUPS && matchesPattern(TRACE_LOOKUPS_PATTERN, info.getCandidate())) {
                    System.out.println("HK2 Tracing (LOOKUP) Candidate: " + info.getCandidate());
                    if (info.getInjectee() != null) {
                        System.out.println("HK2 Tracing (LOOKUP) Injectee: " + info.getInjectee());
                    }
                }
                break;
            default:
                // Do nothing
                    
            }
            
            if ((STACK_PATTERN != null) && matchesPattern(STACK_PATTERN, info.getCandidate())) {
                Thread.dumpStack();
            }
            
            return true;
        }
        
    }

}
