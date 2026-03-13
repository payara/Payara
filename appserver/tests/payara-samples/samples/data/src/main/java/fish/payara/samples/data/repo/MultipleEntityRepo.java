/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2025-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.data.repo;

import fish.payara.samples.data.entity.Box;
import fish.payara.samples.data.entity.Coordinate;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository with multiple entity types and no primary entity type parameter.
 * Based on MultipleEntityRepo from the Jakarta Data 1.0 TCK.
 *
 * <p>The entity type for {@link #deleteIfPositive()} can only be determined by
 * parsing the JPQL query string — it is Coordinate, not Box. Our implementation
 * currently infers Box from {@link #addAll(Box...)} which is wrong.</p>
 *
 * <p>Related issue: FISH-13049</p>
 */
@Repository
public interface MultipleEntityRepo { // Do not add a primary entity type.

    // Methods for Box entity:

    @Insert
    Box[] addAll(Box... boxes);

    // Methods for Coordinate entity:

    @Insert
    Coordinate create(Coordinate c);

    @Query("DELETE FROM Coordinate WHERE x > 0.0d AND y > 0.0f")
    long deleteIfPositive();

    @Query("DELETE WHERE name = ?1")
    long deletePrimaryEntityType(String name);
}
