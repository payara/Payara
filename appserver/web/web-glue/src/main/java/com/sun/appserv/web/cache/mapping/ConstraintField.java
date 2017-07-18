/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.appserv.web.cache.mapping;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.web.LogFacade;


/** ConstraintField class represents a single Field and constraints on its 
 *  values; Field name and its scope are inherited from the Field class. 
 */
public class ConstraintField extends Field {

    private static final String[] SCOPE_NAMES = {
        "", "context.attribute", "request.header", "request.parameter",
        "request.cookie", "request.attribute", "session.attribute",
        "session.id"
    };

    private static final Logger _logger = LogFacade.getLogger();

    // whether to cache if there was a match
    boolean cacheOnMatch = true;
    // whether to cache if there was a failure to match
    boolean cacheOnMatchFailure = false;

    // field value constraints 
    ValueConstraint constraints[] = new ValueConstraint[0];

    /**
     * create a new cache field, given a string representation of the scope
     * @param name name of this field
     * @param scope scope of this field
     */
    public ConstraintField(String name, String scope) 
            throws IllegalArgumentException {
        super(name, scope);
    }

    /** set whether to cache should the constraints check pass
     * @param cacheOnMatch should the constraint check pass, should we cache?
     */
    public void setCacheOnMatch(boolean cacheOnMatch) {
        this.cacheOnMatch = cacheOnMatch;
    }

    /**
     * @return cache-on-match setting
     */
    public boolean getCacheOnMatch() {
        return cacheOnMatch;
    }

    /** set whether to cache should there be a failure forcing the constraint
     * @param cacheOnMatchFailure should there be a constraint check failure,
     *  enable cache?
     */
    public void setCacheOnMatchFailure(boolean cacheOnMatchFailure) {
        this.cacheOnMatchFailure = cacheOnMatchFailure;
    }

    /**
     * @return cache-on-match-failure setting
     */
    public boolean getCacheOnMatchFailure() {
        return cacheOnMatchFailure;
    }

    /**
     * add a constraint for this field
     * @param constraint one constraint associated with this field
     */
    public void addConstraint(ValueConstraint constraint) {
        if (constraint == null)
            return;

        ValueConstraint results[] = 
            new ValueConstraint[constraints.length + 1];
        for (int i = 0; i < constraints.length; i++)
            results[i] = constraints[i];

        results[constraints.length] = constraint;
        constraints = results;
    }

    /**
     * add an array of constraints for this field
     * @param vcs constraints associated with this field
     */
    public void setValueConstraints(ValueConstraint[] vcs) {
        if (vcs == null)
            return;

        constraints = vcs;
    }

    /** apply the constraints on the value of the field in the given request.
     *  return a true if all the constraints pass; false when the 
     *  field is not found or the field value doesn't pass the caching 
     *  constraints. 
     */ 
    public boolean applyConstraints(ServletContext context,
                                    HttpServletRequest request) {

        Object value = getValue(context, request);
        boolean isFine = _logger.isLoggable(Level.FINE);
        if (value == null) {
            // the field is not present in the request
            if (isFine) {
                _logger.log(Level.FINE, LogFacade.CONSTRAINT_FIELD_NOT_FOUND,
                        new Object[] {name, SCOPE_NAMES[scope], cacheOnMatchFailure});
            }
            return cacheOnMatchFailure;
        } else if (constraints.length == 0) {
            // the field is present but has no value constraints
            if (isFine) {
                _logger.log(Level.FINE, LogFacade.CONSTRAINT_FIELD_FOUND,
                        new Object[] {name, value.toString(), SCOPE_NAMES[scope], cacheOnMatch});
            }
            return cacheOnMatch;
        }

        // apply all the value constraints
        for (int i = 0; i < constraints.length; i++) {
            ValueConstraint c = constraints[i];

            // one of the values matched
            if (c.matches(value)) {
                if (isFine) {
                    _logger.log(Level.FINE, LogFacade.CONSTRAINT_FIELD_MATCH,
                            new Object[] {name, value.toString(), SCOPE_NAMES[scope], cacheOnMatch});
                }
                return cacheOnMatch;
            }
        }

        // none of the values matched; should we cache?
        if (isFine) {
            _logger.log(Level.FINE, LogFacade.CONSTRAINT_FIELD_NOT_MATCH,
                    new Object[] {name, value.toString(), SCOPE_NAMES[scope], cacheOnMatchFailure});
        }
        return cacheOnMatchFailure;
    }
}
