/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package org.glassfish.web.deployment.descriptor;

import com.sun.enterprise.deployment.web.MultipartConfig;
import org.glassfish.deployment.common.Descriptor;

/**
 * This represents the multipart-config resided in web.xml.
 *
 * @author Shing Wai Chan
 */

public class MultipartConfigDescriptor extends Descriptor implements MultipartConfig {
    private String location = null;
    private Long maxFileSize = null;
    private Long maxRequestSize = null;
    private Integer fileSizeThreshold = null;

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public Long getMaxFileSize() {
        return maxFileSize;
    }

    @Override
    public void setMaxFileSize(Long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    @Override
    public Long getMaxRequestSize() {
        return maxRequestSize;
    }

    @Override
    public void setMaxRequestSize(Long maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    @Override
    public Integer getFileSizeThreshold() {
        return fileSizeThreshold;
    }

    @Override
    public void setFileSizeThreshold(Integer fileSizeThreshold) {
        this.fileSizeThreshold = fileSizeThreshold;
    }

    @Override
    public void print(StringBuilder toStringBuilder) {
        if (location != null) {
            toStringBuilder.append("\n multipart location ").append(location);
        }
        toStringBuilder.append("\n multipart maxFileSize ").append(maxFileSize);
        toStringBuilder.append("\n multipart maxRequestSize ").append(maxRequestSize);
        toStringBuilder.append("\n multipart fileSizeThreshold ").append(fileSizeThreshold);
    }
}
