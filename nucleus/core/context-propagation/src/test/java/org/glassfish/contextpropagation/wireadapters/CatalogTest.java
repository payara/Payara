/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.contextpropagation.wireadapters;

import static org.junit.Assert.*;

import org.junit.Test;

public class CatalogTest {

  @Test
  public void testUpdateMeta() {
    byte dot = '.';
    Catalog cat = new Catalog();
    cat.add((short) 5);
    cat.add((short) 0x02FF);
    byte[] bytes = "..xxxx....".getBytes();
    cat.updateCatalogMetadata(bytes);
    assertEquals(dot, bytes[0]);
    assertEquals(dot, bytes[1]);
    assertEquals((byte) 0, bytes[2]);
    assertEquals((byte) 5, bytes[3]);
    assertEquals((byte) 2, bytes[4]);
    assertEquals((byte) 0xFF, bytes[5]);
  }
  
  @Test public void testSetMeta() {
    Catalog cat = new Catalog();
    cat.setMeta(0xABCD1234);
    assertEquals((short) 0xABCD, cat.start);
    assertEquals((short) 0x1234, cat.end);
  }

}
