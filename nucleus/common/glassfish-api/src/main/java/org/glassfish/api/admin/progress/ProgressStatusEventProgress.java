/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.api.admin.ProgressStatus;

/** Progress method was called.
 *
 * @author martinmares
 */
public class ProgressStatusEventProgress extends ProgressStatusEvent implements ProgressStatusMessage {

    private int steps;
    private String message;
    private boolean spinner;

    public ProgressStatusEventProgress(String progressStatusId, int steps, String message, boolean spinner) {
        super(progressStatusId);
        this.steps = steps;
        this.message = message;
        this.spinner = spinner;
    }

    public ProgressStatusEventProgress(String sourceId) {
        super(sourceId);
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSpinner(boolean spinner) {
        this.spinner = spinner;
    }

    public int getSteps() {
        return steps;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public boolean isSpinner() {
        return spinner;
    }

    @Override
    public ProgressStatus apply(ProgressStatus ps) {
        ps.progress(steps, message, spinner);
        return ps;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + this.steps;
        hash = 97 * hash + (this.message != null ? this.message.hashCode() : 0);
        hash = 97 * hash + (this.spinner ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProgressStatusEventProgress other = (ProgressStatusEventProgress) obj;
        if (this.steps != other.steps) {
            return false;
        }
        if ((this.message == null) ? (other.message != null) : !this.message.equals(other.message)) {
            return false;
        }
        if (this.spinner != other.spinner) {
            return false;
        }
        return true;
    }
    
    
    
}
