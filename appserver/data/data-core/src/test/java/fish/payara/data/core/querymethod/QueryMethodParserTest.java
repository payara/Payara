/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
package fish.payara.data.core.querymethod;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * Tests for Jakarta Data Query by name parser.
 *
 * @author Petr Aubrecht <aubrecht@asoftware.cz>
 */
public class QueryMethodParserTest {

    public QueryMethodParserTest() {
    }

    // -----------------  Basic Tests  ---------------------
    @Test
    public void testFind() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("find");
        List<String> tokens = parser.parse().getTokens();

        assertEquals(QueryMethodParser.Action.FIND, parser.getAction());
        assertEquals(List.of(), tokens);
    }

    @Test
    public void testFirst10() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findFirst10");
        List<String> tokens = parser.parse().getTokens();

        assertEquals(QueryMethodParser.Action.FIND, parser.getAction());
        assertEquals(List.of("First", "10"), tokens);
        assertEquals(Integer.valueOf(10), parser.getLimit());
    }

    @Test
    public void testFirstFailure() {
        QueryMethodParser parser = new QueryMethodParser("findFirstTen"); // A number is expected after First
        try {
            parser.parse();
            fail("Expected QueryMethodSyntaxException was not thrown.");
        } catch (QueryMethodSyntaxException ex) {
            assertTrue(ex.getMessage().startsWith("Expected number"));
        }
    }

    @Test
    public void testIgnoredText() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findWhateverCanBeHere");
        List<String> tokens = parser.parse().getTokens();

        assertEquals(QueryMethodParser.Action.FIND, parser.getAction());
        assertEquals(List.of("Whatever", "Can", "Be", "Here"), tokens);
        assertEquals(List.of(), parser.getOrderBy());
        assertEquals(List.of(), parser.getConditions());
        assertEquals(null, parser.getLimit());
    }

    @Test
    public void testIgnoredTextWithBy() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findWhateverCanBeHereByXyz");
        List<String> tokens = parser.parse().getTokens();

        assertEquals(QueryMethodParser.Action.FIND, parser.getAction());
        assertEquals(List.of("Whatever", "Can", "Be", "Here", "By", "Xyz"), tokens);
    }

    @Test
    public void testIgnoredTextWithOrderBy() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findWhateverCanBeHereOrderByXyz");
        parser.parse();

        assertEquals(QueryMethodParser.Action.FIND, parser.getAction());
        assertEquals(List.of(new QueryMethodParser.OrderBy("xyz", null)), parser.getOrderBy());
    }

    @Test
    public void testFindByName() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByName").parse();

        assertEquals(QueryMethodParser.Action.FIND, parser.getAction());
        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, false, null)
                ),
                parser.getConditions());
    }

    @Test
    public void testFindByNumber5() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByNumber5").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "number5", false, false, null)
                ),
                parser.getConditions());
    }

    @Test
    public void testCountByNumber() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("countByNumber").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "number", false, false, null)
                ),
                parser.getConditions());
    }

    @Test
    public void testCountByNumberWithIgnoredTest() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("countSomeIgnoredTextWithNumber5ByNumber").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "number", false, false, null)
                ),
                parser.getConditions());
    }

    @Test
    public void testDeleteByNameLikeAndPriceLessThan() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("deleteByNameLikeAndPriceLessThan").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, false, "Like"),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.AND, "price", false, false, "LessThan")
                ),
                parser.getConditions());
    }

    @Test
    public void testExistsByNameLikeAndPriceLessThan() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("existsByNameLikeAndPriceLessThan").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, false, "Like"),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.AND, "price", false, false, "LessThan")
                ),
                parser.getConditions());
    }

    @Test
    public void testFindOrderByPriceDesc() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findOrderByPriceDesc").parse();

        assertEquals(List.of(new QueryMethodParser.OrderBy("price", "Desc")), parser.getOrderBy());
    }

    @Test
    public void testFindOrderByPriceDescDateAsc() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findOrderByPriceDescDateAsc").parse();

        assertEquals(List.of(
                new QueryMethodParser.OrderBy("price", "Desc"),
                new QueryMethodParser.OrderBy("date", "Asc")
        ), parser.getOrderBy());
    }

    @Test
    public void testFindOrderByPriceDescDate() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findOrderByPriceDescDate").parse();

        assertEquals(List.of(
                new QueryMethodParser.OrderBy("price", "Desc"),
                new QueryMethodParser.OrderBy("date", null)
        ), parser.getOrderBy());
    }

    // -----------------  Tests from the Spec ---------------------

    @Test
    public void testFindByNameLike() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByNameLike").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, false, "Like")
                ),
                parser.getConditions());
    }

    @Test
    public void testFindByNameLikeAndPriceLessThanOrderByPriceDesc() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByNameLikeAndPriceLessThanOrderByPriceDesc").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, false, "Like"),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.AND, "price", false, false, "LessThan")
                ),
                parser.getConditions());
        assertEquals(List.of(new QueryMethodParser.OrderBy("price", "Desc")), parser.getOrderBy());
    }

    @Test
    public void testFindByAgeGreaterThan() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByAgeGreaterThan").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "age", false, false, "GreaterThan")
                ),
                parser.getConditions());
    }

    @Test
    public void testFindByAuthorName() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByAuthorName").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "authorName", false, false, null)
                ),
                parser.getConditions());
    }

    @Test
    public void testFindByCategoryNameAndPriceLessThan() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByCategoryNameAndPriceLessThan").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "categoryName", false, false, null),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.AND, "price", false, false, "LessThan")
                ),
                parser.getConditions());
    }

    @Test
    public void testFindByNameLikeOrderByPriceDescIdAsc() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByNameLikeOrderByPriceDescIdAsc").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, false, "Like")
                ),
                parser.getConditions());
        assertEquals(List.of(
                new QueryMethodParser.OrderBy("price", "Desc"),
                new QueryMethodParser.OrderBy("id", "Asc")
        ), parser.getOrderBy());
    }

    @Test
    public void testFindByNameLikeAndYearMadeBetweenAndPriceLessThan() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByNameLikeAndYearMadeBetweenAndPriceLessThan").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, false, "Like"),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.AND, "yearMade", false, false, "Between"),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.AND, "price", false, false, "LessThan")
                ),
                parser.getConditions());
    }

    @Test
    public void testFindByAddressZipCode() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByAddressZipCode").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "addressZipCode", false, false, null)
                ),
                parser.getConditions());
    }

    @Test
    public void testFindByAddress_zipcode() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByAddress_zipcode").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "address.zipcode", false, false, null)
                ),
                parser.getConditions());
    }

    // ----------------- Aditional Tests ---------------------
    @Test
    public void testFindByTitleOrAuthor() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByTitleOrAuthor").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "title", false, false, null),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.OR, "author", false, false, null)
                ),
                parser.getConditions());
    }

    @Test
    public void testFindByPriceLessThanOrNameLikeAndPublishedTrue() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByPriceLessThanOrNameLikeAndPublishedTrue").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "price", false, false, "LessThan"),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.OR, "name", false, false, "Like"),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.AND, "published", false, false, "True")
                ),
                parser.getConditions());
    }

    @Test
    public void testFindByNameNotLike() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByNameNotLike").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, true, "Like")
                ),
                parser.getConditions());
    }

    @Test
    public void testFindByNameNot() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByNameNot").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, true, null)
                ),
                parser.getConditions());
    }

    @Test
    public void testFindByNameIgnoreCase() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByNameIgnoreCase").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", true, false, null)
                ),
                parser.getConditions());
    }

    @Test
    public void testFindByTitleIgnoreCaseContains() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findByTitleIgnoreCaseContains").parse();
        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "title", true, false, "Contains")
                ),
                parser.getConditions());
    }
}