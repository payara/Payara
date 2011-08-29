/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDLGPL_1_1.html
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

package org.glassfish.virtualization.util;

import org.glassfish.virtualization.spi.SearchCriteria;
import org.glassfish.virtualization.spi.TemplateCondition;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple implementation of SearchCriteria.
 *
 * @author bhavanishankar@java.net
 */
public class SimpleSearchCriteria implements SearchCriteria {

    Set<TemplateCondition> andConditions = new HashSet<TemplateCondition>();
    Set<TemplateCondition> orConditions = new HashSet<TemplateCondition>();
    Set<TemplateCondition> optionallyConditions = new HashSet<TemplateCondition>();

    public SearchCriteria and(TemplateCondition... indexes) {
        return add(andConditions, indexes);
    }

    public Collection<TemplateCondition> and() {
        return andConditions;
    }

    public SearchCriteria or(TemplateCondition... indexes) {
        return add(orConditions, indexes);
    }

    public Collection<TemplateCondition> or() {
        return orConditions;
    }

    public SearchCriteria optionally(TemplateCondition... indexes) {
        return add(optionallyConditions, indexes);
    }

    public Collection<TemplateCondition> optionally() {
        return optionallyConditions;
    }

    private SearchCriteria add(Set<TemplateCondition> conditionSet,
                               TemplateCondition... indexes) {
        for (TemplateCondition templateCondition : indexes) {
            conditionSet.add(templateCondition);
        }
        return this;
    }

}
