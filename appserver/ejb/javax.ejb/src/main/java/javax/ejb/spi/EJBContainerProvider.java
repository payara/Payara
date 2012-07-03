/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package javax.ejb.spi;

import java.util.Map;
import javax.ejb.embeddable.EJBContainer;
import javax.ejb.EJBException;

/**
 * The EJBContainerProvider SPI is used by the embeddable container bootstrap
 * class to initialize a suitable embeddable container.
 *
 * @since EJB 3.1
 */
public interface EJBContainerProvider {

    /**
     * Called by the embeddable container bootstrap process to find a
     * suitable embeddable container implementation.  An embeddable
     * container provider may deem itself as appropriate for the
     * embeddable application if any of the following are true :
     * 
     * <ul>
     * <li>
     *   The <code>javax.ejb.embeddable.provider</code> property was
     *   included in the Map passed to <code>createEJBContainer</code>
     *   and the value of the property is the provider's
     *   implementation class.
     *
     * <li>
     *   No <code>javax.ejb.embeddable.provider</code> property was specified.
     *</ul>
     *
     * If a provider does not qualify as the provider for the
     * embeddable application, it must return null.
     *
     * @param properties Spec-defined and/or vendor-specific
     * properties, that were passed to <code>javax.ejb.embeddable.EJBContainer#createEJBContainer(Map&#60;&#63;,&#63;&#62;)</code> call
     * 
     * @return EJBContainer instance or null
     */
    public EJBContainer createEJBContainer(Map<?,?> properties) throws EJBException;

}
