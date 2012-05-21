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

package org.glassfish.virtualization.spi;

import org.glassfish.virtualization.config.Template;
import org.jvnet.hk2.annotations.Contract;

import java.io.File;
import java.util.Collection;

/**
 * Repository for virtualization templates
 *
 * @author Jerome Dochez
 */
@Contract
public interface TemplateRepository {

    /**
     * Installs a template in the repository
     *
     * @param config the template configuration as obtained from the user.
     * @return true if installation was successful, false otherwise
     */
    boolean installs(Template config, Collection<File> files);

    /**
     * Deletes a template from the repository
     *
     * @param config the template configuration
     * @return true if the template was properly un-installed
     */
    boolean delete(Template config);

    /**
     * Search the repository for all templates satisfying the passed
     * {@link SearchCriteria}.
     *
     * @param criteria the search criteria for the requested templates
     * @return list of templates satisfying the search criteria.
     */
    Collection<TemplateInstance> get(SearchCriteria criteria);

    /**
     * Returns all the templates registered in the repository.
     *
     * @return the repository content.
     */
    Collection<TemplateInstance>  all();

    /**
     * Lookup a template instance by its name
     *
     * @param name the template name
     * @return the {@link TemplateInstance} if found otherwise null
     */
    TemplateInstance byName(String name);

}
