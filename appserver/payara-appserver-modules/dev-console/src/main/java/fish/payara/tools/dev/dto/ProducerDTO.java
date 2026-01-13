/*
 *
 * Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.tools.dev.dto;

import fish.payara.tools.dev.model.ProducerInfo;
import java.time.Instant;
import java.util.List;

/**
 *
 * @author Gaurav Gupta
 */
public class ProducerDTO {

    private final String className;
    private final String memberSignature;
    private final String producedType;
    private final String kind; // FIELD or METHOD
    private final int producedCount; // how many beans of that type currently exist
    private final Instant lastProduced;

    private final int disposedCount;    // number of times the disposer was invoked
    private final Instant lastDisposed;

    private List<fish.payara.tools.dev.model.Record> creationRecords;
    private List<fish.payara.tools.dev.model.Record> destructionRecords;

    public ProducerDTO(ProducerInfo info) {
        this.className = info.getClassName();
        this.memberSignature = info.getMemberSignature();
        this.producedType = info.getProducedType();
        this.kind = info.getKind().name();

        this.producedCount = info.getProducedCount();
        this.lastProduced = info.getLastProduced();

        this.disposedCount = info.getDisposedCount();
        this.lastDisposed = info.getLastDisposed();
    }

    // getters
    public String getClassName() {
        return className;
    }

    public String getMemberSignature() {
        return memberSignature;
    }

    public String getProducedType() {
        return producedType;
    }

    public String getKind() {
        return kind;
    }

    public int getProducedCount() {
        return producedCount;
    }

    public Instant getLastProduced() {
        return lastProduced;
    }

    public int getDisposedCount() {
        return disposedCount;
    }

    public Instant getLastDisposed() {
        return lastDisposed;
    }

    public List<fish.payara.tools.dev.model.Record> getCreationRecords() {
        return creationRecords;
    }

    public void setCreationRecords(List<fish.payara.tools.dev.model.Record> creationRecords) {
        this.creationRecords = creationRecords;
    }

    public List<fish.payara.tools.dev.model.Record> getDestructionRecords() {
        return destructionRecords;
    }

    public void setDestructionRecords(List<fish.payara.tools.dev.model.Record> destructionRecords) {
        this.destructionRecords = destructionRecords;
    }
}
