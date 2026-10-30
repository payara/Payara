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
        QueryMethodParser.ParseResult parseResult = parser.parse();
        List<String> tokens = parser.getTokens();

        assertEquals(QueryMethodParser.Action.FIND, parseResult.action());
        assertEquals(List.of(), tokens);
    }

    @Test
    public void testFirst10() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findFirst10");
        QueryMethodParser.ParseResult parseResult = parser.parse();
        List<String> tokens = parser.getTokens();

        assertEquals(QueryMethodParser.Action.FIND, parseResult.action());
        assertEquals(List.of("First", "10"), tokens);
        assertEquals(Integer.valueOf(10), parseResult.limit());
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
        QueryMethodParser.ParseResult parseResult = parser.parse();
        List<String> tokens = parser.getTokens();

        assertEquals(QueryMethodParser.Action.FIND, parseResult.action());
        assertEquals(List.of("Whatever", "Can", "Be", "Here"), tokens);
        assertEquals(List.of(), parseResult.orderBy());
        assertEquals(List.of(), parseResult.conditions());
        assertEquals(null, parseResult.limit());
    }

    @Test
    public void testIgnoredTextWithBy() throws QueryMethodSyntaxException {
        QueryMethodParser parser = new QueryMethodParser("findWhateverCanBeHereByXyz");
        QueryMethodParser.ParseResult parseResult = parser.parse();
        List<String> tokens = parser.getTokens();

        assertEquals(QueryMethodParser.Action.FIND, parseResult.action());
        assertEquals(List.of("Whatever", "Can", "Be", "Here", "By", "Xyz"), tokens);
    }

    @Test
    public void testIgnoredTextWithOrderBy() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findWhateverCanBeHereOrderByXyz").parse();

        assertEquals(QueryMethodParser.Action.FIND, parseResult.action());
        assertEquals(List.of(new QueryMethodParser.OrderBy("xyz", null)), parseResult.orderBy());
    }

    @Test
    public void testFindByName() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByName").parse();

        assertEquals(QueryMethodParser.Action.FIND, parseResult.action());
        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, false, null)
                ),
                parseResult.conditions());
    }

    @Test
    public void testFindByNumber5() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByNumber5").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "number5", false, false, null)
                ),
                parseResult.conditions());
    }

    @Test
    public void testCountByNumber() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("countByNumber").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "number", false, false, null)
                ),
                parseResult.conditions());
    }

    @Test
    public void testCountByNumberWithIgnoredTest() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("countSomeIgnoredTextWithNumber5ByNumber").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "number", false, false, null)
                ),
                parseResult.conditions());
    }

    @Test
    public void testDeleteByNameLikeAndPriceLessThan() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("deleteByNameLikeAndPriceLessThan").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, false, "Like"),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.AND, "price", false, false, "LessThan")
                ),
                parseResult.conditions());
    }

    @Test
    public void testExistsByNameLikeAndPriceLessThan() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("existsByNameLikeAndPriceLessThan").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, false, "Like"),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.AND, "price", false, false, "LessThan")
                ),
                parseResult.conditions());
    }

    @Test
    public void testFindOrderByPriceDesc() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findOrderByPriceDesc").parse();

        assertEquals(List.of(new QueryMethodParser.OrderBy("price", "Desc")), parseResult.orderBy());
    }

    @Test
    public void testFindOrderByPriceDescDateAsc() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findOrderByPriceDescDateAsc").parse();

        assertEquals(List.of(
                new QueryMethodParser.OrderBy("price", "Desc"),
                new QueryMethodParser.OrderBy("date", "Asc")
        ), parseResult.orderBy());
    }

    @Test
    public void testFindOrderByPriceDescDate() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findOrderByPriceDescDate").parse();

        assertEquals(List.of(
                new QueryMethodParser.OrderBy("price", "Desc"),
                new QueryMethodParser.OrderBy("date", null)
        ), parseResult.orderBy());
    }

    // -----------------  Tests from the Spec ---------------------

    @Test
    public void testFindByNameLike() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByNameLike").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, false, "Like")
                ),
                parseResult.conditions());
    }

    @Test
    public void testFindByNameLikeAndPriceLessThanOrderByPriceDesc() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByNameLikeAndPriceLessThanOrderByPriceDesc").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, false, "Like"),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.AND, "price", false, false, "LessThan")
                ),
                parseResult.conditions());
        assertEquals(List.of(new QueryMethodParser.OrderBy("price", "Desc")), parseResult.orderBy());
    }

    @Test
    public void testFindByAgeGreaterThan() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByAgeGreaterThan").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "age", false, false, "GreaterThan")
                ),
                parseResult.conditions());
    }

    @Test
    public void testFindByAuthorName() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByAuthorName").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "authorName", false, false, null)
                ),
                parseResult.conditions());
    }

    @Test
    public void testFindByCategoryNameAndPriceLessThan() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByCategoryNameAndPriceLessThan").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "categoryName", false, false, null),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.AND, "price", false, false, "LessThan")
                ),
                parseResult.conditions());
    }

    @Test
    public void testFindByNameLikeOrderByPriceDescIdAsc() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByNameLikeOrderByPriceDescIdAsc").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, false, "Like")
                ),
                parseResult.conditions());
        assertEquals(List.of(
                new QueryMethodParser.OrderBy("price", "Desc"),
                new QueryMethodParser.OrderBy("id", "Asc")
        ), parseResult.orderBy());
    }

    @Test
    public void testFindByNameLikeAndYearMadeBetweenAndPriceLessThan() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByNameLikeAndYearMadeBetweenAndPriceLessThan").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, false, "Like"),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.AND, "yearMade", false, false, "Between"),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.AND, "price", false, false, "LessThan")
                ),
                parseResult.conditions());
    }

    @Test
    public void testFindByAddressZipCode() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByAddressZipCode").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "addressZipCode", false, false, null)
                ),
                parseResult.conditions());
    }

    @Test
    public void testFindByAddress_zipcode() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByAddress_zipcode").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "address.zipcode", false, false, null)
                ),
                parseResult.conditions());
    }

    // ----------------- Aditional Tests ---------------------
    @Test
    public void testFindByTitleOrAuthor() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByTitleOrAuthor").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "title", false, false, null),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.OR, "author", false, false, null)
                ),
                parseResult.conditions());
    }

    @Test
    public void testFindByPriceLessThanOrNameLikeAndPublishedTrue() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByPriceLessThanOrNameLikeAndPublishedTrue").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "price", false, false, "LessThan"),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.OR, "name", false, false, "Like"),
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.AND, "published", false, false, "True")
                ),
                parseResult.conditions());
    }

    @Test
    public void testFindByNameNotLike() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByNameNotLike").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, true, "Like")
                ),
                parseResult.conditions());
    }

    @Test
    public void testFindByNameNot() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByNameNot").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", false, true, null)
                ),
                parseResult.conditions());
    }

    @Test
    public void testFindByNameIgnoreCase() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByNameIgnoreCase").parse();

        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "name", true, false, null)
                ),
                parseResult.conditions());
    }

    @Test
    public void testFindByTitleIgnoreCaseContains() throws QueryMethodSyntaxException {
        QueryMethodParser.ParseResult parseResult = new QueryMethodParser("findByTitleIgnoreCaseContains").parse();
        assertEquals(
                List.of(
                        new QueryMethodParser.Condition(QueryMethodParser.LogicalOperator.NONE, "title", true, false, "Contains")
                ),
                parseResult.conditions());
    }
}