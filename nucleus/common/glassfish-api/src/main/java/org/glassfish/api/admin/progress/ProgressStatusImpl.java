/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.api.admin.progress;

import java.io.Serializable;

/** {@code ProgressStatus} implementation suggested for {@code AdminCommand}
 * implementation.
 *
 * @author mmares
 */
//TODO: Move to admin-utils if possible. It is now in API only because ProgressStatusImpl is here, too
public class ProgressStatusImpl extends ProgressStatusBase implements Serializable {
    
    private static final long serialVersionUID = 1;
    
    /** Constructor for instancing dummy (without propagation) instance.
     */
    public ProgressStatusImpl() {
        this(null, -1, null, "no-id");
    }
    
    /** Construct unnamed {@code ProgressStatusImpl}
     * 
     * @param parent Parent {@code ProgressStatusBase}
     * @param id Is useful for event transfer
     */  
    protected ProgressStatusImpl(ProgressStatusBase parent, String id) {
        super(null, -1, parent, id);
    }
    
    /** Construct named {@code ProgressStatusImpl}.
     * 
     * @param name of the {@code ProgressStatus} implementation is used 
     *        to identify source of progress messages.
     * @param parent Parent {@code ProgressStatusBase}
     * @param id Is useful for event transfer
     */
    protected ProgressStatusImpl(String name, ProgressStatusBase parent, String id) {
        super(name, -1, parent, id);
    }
    
    /** Construct named {@code ProgressStatusImpl} with defined expected count 
     * of steps.
     * 
     * @param name of the {@code ProgressStatus} implementation is used 
     *        to identify source of progress messages.
     * @param totalStepCount How many steps are expected in this 
     *        {@code ProgressStatus}
     * @param parent Parent {@code ProgressStatusBase}
     * @param id Is useful for event transfer
     */
    protected ProgressStatusImpl(String name, int totalStepCount, ProgressStatusBase parent, String id) {
        super(name, totalStepCount, parent, id);
    }

    @Override
    protected ProgressStatusBase doCreateChild(String name, int totalStepCount) {
        String childId = (id == null ? "" : id) + "." + (children.size() + 1);
        return new ProgressStatusImpl(name, totalStepCount, this, childId);
    }  

}
