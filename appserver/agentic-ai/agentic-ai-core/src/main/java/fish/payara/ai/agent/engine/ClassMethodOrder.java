/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ai.agent.engine;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses a Java class file to determine the source-file declaration order of
 * its methods.
 *
 * <p>The Jakarta Agentic AI specification mandates that {@code @Decision} and
 * {@code @Action} phase methods execute in the order they appear in the source
 * file when no explicit {@code @Priority} or {@code order()} is provided.
 * {@link Class#getDeclaredMethods()} does not guarantee source order, so this
 * utility reads the {@code methods} table of the {@code .class} file directly,
 * where the JVM specification requires methods to be stored in declaration
 * order.</p>
 */
public class ClassMethodOrder {

    public ClassMethodOrder() {}

    /**
     * Returns a map from method name to its 0-based position in the class
     * file's methods table (i.e., source-file declaration order).
     *
     * <p>For overloaded methods only the first occurrence is recorded. Returns
     * an empty map when the class file cannot be read (the caller falls back
     * to {@link Class#getDeclaredMethods()} order in that case).</p>
     */
    public static Map<String, Integer> of(Class<?> clazz) {
        String resource = clazz.getName().replace('.', '/') + ".class";
        ClassLoader loader = clazz.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in == null) return Map.of();
            return parse(new DataInputStream(in));
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static Map<String, Integer> parse(DataInputStream dis) throws Exception {
        dis.readInt();           // magic
        dis.readShort();         // minor_version
        dis.readShort();         // major_version

        // Read constant pool — needed to look up UTF-8 method names
        int cpCount = dis.readUnsignedShort();
        String[] utf8 = new String[cpCount];
        for (int i = 1; i < cpCount; i++) {
            int tag = dis.readUnsignedByte();
            switch (tag) {
                case 1  -> utf8[i] = dis.readUTF();                           // CONSTANT_Utf8
                case 3, 4 -> dis.readInt();                                   // Integer, Float
                case 5, 6 -> { dis.readLong(); i++; }                         // Long, Double (two slots)
                case 7, 8, 16, 19, 20 -> dis.readUnsignedShort();             // Class, String, MethodType, Module, Package
                case 9, 10, 11, 12, 17, 18 -> { dis.readUnsignedShort(); dis.readUnsignedShort(); } // refs, NameAndType, Dynamic
                case 15 -> { dis.readUnsignedByte(); dis.readUnsignedShort(); } // MethodHandle
                default -> throw new IllegalStateException("Unknown constant pool tag: " + tag);
            }
        }

        // Skip access_flags, this_class, super_class
        dis.readUnsignedShort();
        dis.readUnsignedShort();
        dis.readUnsignedShort();

        // Skip interfaces
        int ifCount = dis.readUnsignedShort();
        for (int i = 0; i < ifCount; i++) dis.readUnsignedShort();

        // Skip fields
        int fieldCount = dis.readUnsignedShort();
        for (int i = 0; i < fieldCount; i++) {
            dis.readUnsignedShort(); dis.readUnsignedShort(); dis.readUnsignedShort();
            skipAttributes(dis);
        }

        // Read methods in declaration order
        int methodCount = dis.readUnsignedShort();
        Map<String, Integer> order = new HashMap<>(methodCount);
        for (int i = 0; i < methodCount; i++) {
            dis.readUnsignedShort();                   // access_flags
            int nameIdx = dis.readUnsignedShort();     // name_index
            dis.readUnsignedShort();                   // descriptor_index
            skipAttributes(dis);
            String name = utf8[nameIdx];
            if (name != null && !name.startsWith("<")) { // skip <init> and <clinit>
                order.putIfAbsent(name, order.size());
            }
        }
        return order;
    }

    private static void skipAttributes(DataInputStream dis) throws Exception {
        int count = dis.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            dis.readUnsignedShort();         // attribute_name_index
            int length = dis.readInt();      // attribute_length
            dis.skipBytes(length);
        }
    }
}
