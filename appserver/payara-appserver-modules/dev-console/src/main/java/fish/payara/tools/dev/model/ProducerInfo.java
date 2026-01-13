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
package fish.payara.tools.dev.model;

import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.BeanManager;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ProducerInfo extends BeanInfo {

    public enum Kind {
        FIELD,
        METHOD
    }

    private final String memberSignature;   // field or method signature without class name
    private final String producedType;      // type as String
    private final Kind kind;                // FIELD or METHOD
    private final AtomicInteger producedCount = new AtomicInteger(0); // Counts how many times CDI executed this producer
    private final AtomicReference<Instant> lastProduced = new AtomicReference<>(null);

    private final AtomicInteger disposedCount = new AtomicInteger(0); // Counts how many times CDI executed this disposer
    private final AtomicReference<Instant> lastDisposed = new AtomicReference<>(null);

    public ProducerInfo(AnnotatedMember<?> member, Type producedType, Kind kind, BeanManager bm) {
        super(member.getJavaMember().getDeclaringClass().getName());
        Member javaMember = member.getJavaMember();
        this.producedType = producedType.getTypeName(); // store as String for JSON

        // Remove class name prefix from member signature
        String fullSignature = javaMember.toString();
        String classNamePrefix = javaMember.getDeclaringClass().getName() + ".";
        if (fullSignature.contains(classNamePrefix)) {
            this.memberSignature = simplifySignature(fullSignature.replace(classNamePrefix, ""));
        } else {
            this.memberSignature = simplifySignature(fullSignature);
        }
        this.kind = kind;
    }

    /**
     * Increment whenever the CDI extension's wrapped producer is invoked and
     * record the last produced timestamp.
     */
    public void incrementProducedCount() {
        producedCount.incrementAndGet();
        lastProduced.set(Instant.now());
    }

    /**
     * Returns the timestamp when this producer last created an instance.
     */
    public Instant getLastProduced() {
        return lastProduced.get();
    }

    /**
     * Returns how many times this producer created an instance
     *
     * @return
     */
    public int getProducedCount() {
        return producedCount.get();
    }

    /**
     * Increment whenever the CDI disposer method is invoked and record the last
     * disposed timestamp.
     */
    public void incrementDisposedCount() {
        disposedCount.incrementAndGet();
        lastDisposed.set(Instant.now());
    }

    /**
     * Returns the timestamp when this producer last disposed an instance.
     */
    public Instant getLastDisposed() {
        return lastDisposed.get();
    }

    /**
     * Returns how many times this producer disposed an instance.
     *
     * @return
     */
    public int getDisposedCount() {
        return disposedCount.get();
    }

    static String simplifySignature(String signature) {
        int parenIndex = signature.indexOf("(");

        String beforeParams;
        String params = "";

        if (parenIndex != -1) {
            beforeParams = signature.substring(0, parenIndex).trim();
            params = signature.substring(parenIndex + 1, signature.length() - 1).trim();
        } else {
            // no params case (e.g. field or no-arg method without parentheses)
            beforeParams = signature.trim();
        }

        // split into parts
        String[] parts = beforeParams.split("\\s+");
        StringBuilder simplified = new StringBuilder();

        // modifiers + return type
        for (int i = 0; i < parts.length - 1; i++) {
            simplified.append(simpleName(parts[i])).append(" ");
        }

        // method name or field name
        simplified.append(parts[parts.length - 1]);

        // only add () if it’s a method
        if (parenIndex != -1) {
            simplified.append("(");
            if (!params.isEmpty()) {
                String[] paramList = params.split(",");
                for (int i = 0; i < paramList.length; i++) {
                    simplified.append(simpleName(paramList[i].trim()));
                    if (i < paramList.length - 1) {
                        simplified.append(", ");
                    }
                }
            }
            simplified.append(")");
        }

        return simplified.toString().trim();
    }

    private static String simpleName(String fqcn) {
        fqcn = fqcn.trim();
        int idx = fqcn.lastIndexOf('.');
        return (idx == -1) ? fqcn : fqcn.substring(idx + 1);
    }

    public String getMemberSignature() {
        return memberSignature;
    }

    public String getProducedType() {
        return producedType;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return "ProducerInfo{"
                + "className='" + className + '\''
                + ", memberSignature='" + memberSignature + '\''
                + ", producedType='" + producedType + '\''
                + ", kind=" + kind
                + ", producedCount=" + producedCount
                + ", disposedCount=" + disposedCount
                + '}';
    }
}
