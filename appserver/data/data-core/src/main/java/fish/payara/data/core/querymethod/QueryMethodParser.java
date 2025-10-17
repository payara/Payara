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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Jakarta Data Query by name parser. The specification for the parser:
 * <a href="https://jakarta.ee/specifications/data/1.0/jakarta-data-addendum-1.0">Jakarta Data: Query by Method Name
 * Extension</a>
 *
 * The grammar in BNF looks like this:
 * <pre>
 * query : find | action
 * find : "find" limit? ignoredText? restriction? order?
 * action : ("delete" | "count" | "exists") ignoredText? restriction?
 * restriction : "By" predicate
 * limit : "First" max?
 * predicate : condition (("And" | "Or") condition)*
 * condition : property "IgnoreCase"? "Not"? operator?
 * operator
 *   : "Contains"
 *   | "EndsWith"
 *   | "StartsWith"
 *   | "LessThan"
 *   | "LessThanEqual"
 *   | "GreaterThan"
 *   | "GreaterThanEqual"
 *   | "Between"
 *   | "Like"
 *   | "In"
 *   | "Null"
 *   | "True"
 *   | "False"
 * property : identifier ("_" identifier)*
 * identifier : word
 * max : digit+
 * order : "OrderBy" (property | orderItem+)
 * orderItem : property ("Asc" | "Desc")
 * </pre>
 *
 * @author Petr Aubrecht <aubrecht@asoftware.cz>
 */
public class QueryMethodParser {

    private static final String KEYWORD_FIRST = "First";
    private static final String KEYWORD_BY = "By";
    private static final String KEYWORD_ORDER = "Order";
    private static final String KEYWORD_ASC = "Asc";
    private static final String KEYWORD_DESC = "Desc";
    private static final String KEYWORD_AND = "And";
    private static final String KEYWORD_OR = "Or";
    private static final String KEYWORD_IGNORE = "Ignore";
    private static final String KEYWORD_CASE = "Case";
    private static final String KEYWORD_NOT = "Not";
    private static final String KEYWORD_UNDERSCORE = "_";

    private static final String KEYWORD_CONTAINS = "Contains";
    private static final String KEYWORD_ENDS = "Ends";
    private static final String KEYWORD_WITH = "With";
    private static final String KEYWORD_STARTS = "Starts";
    private static final String KEYWORD_LESS = "Less";
    private static final String KEYWORD_THAN = "Than";
    private static final String KEYWORD_EQUAL = "Equal";
    private static final String KEYWORD_GREATER = "Greater";
    private static final String KEYWORD_BETWEEN = "Between";
    private static final String KEYWORD_LIKE = "Like";
    private static final String KEYWORD_IN = "In";
    private static final String KEYWORD_NULL = "Null";
    private static final String KEYWORD_TRUE = "True";
    private static final String KEYWORD_FALSE = "False";

    public static enum LogicalOperator {
        NONE, AND, OR
    };

    private void grammarQuery() throws QueryMethodSyntaxException {
        switch (action) {
            case FIND ->
                grammarFind();
            case COUNT, DELETE, EXISTS ->
                grammarAction();
        }
        if (!isEof()) {
            throw new QueryMethodSyntaxException("Unexpected text", methodName, currentToken, tokens, tokensPositions);
        }
    }

    private void grammarFind() throws QueryMethodSyntaxException {
        if (follows(KEYWORD_FIRST)) {
            grammarLimit();
        }
        grammarIgnoredText(Set.of(KEYWORD_BY, KEYWORD_ORDER));
        if (follows(KEYWORD_BY)) {
            grammarRestriction();
        }
        if (follows(KEYWORD_ORDER)) {
            grammarOrder();
        }
    }

    private void grammarAction() throws QueryMethodSyntaxException {
        grammarIgnoredText(Set.of(KEYWORD_BY));
        if (follows(KEYWORD_BY)) {
            grammarRestriction();
        }
    }

    private void grammarRestriction() throws QueryMethodSyntaxException {
        next(KEYWORD_BY);
        grammarPredicate();
    }

    private void grammarLimit() throws QueryMethodSyntaxException {
        next(KEYWORD_FIRST);
        grammarMax();
    }

    private void grammarPredicate() throws QueryMethodSyntaxException {
        grammarCondition(LogicalOperator.NONE);
        while (follows(KEYWORD_AND) || follows(KEYWORD_OR)) {
            String operator = current();
            currentToken++;
            grammarCondition(LogicalOperator.valueOf(operator.toUpperCase()));
        }
    }

    private void grammarCondition(LogicalOperator precedingOperator) throws QueryMethodSyntaxException {
        String property = grammarProperty();
        boolean ignoreCase = false;
        if (follows(KEYWORD_IGNORE, KEYWORD_CASE)) {
            ignoreCase = true;
            next(); // Ignore
            next(); // Case
        }
        boolean not = false;
        if (follows(KEYWORD_NOT)) {
            not = true;
            next();
        }
        String operator = grammarOperator();
        conditions.add(new Condition(precedingOperator, property, ignoreCase, not, operator));
    }

    private String grammarOperator() throws QueryMethodSyntaxException {
        int operatorTokens = followsOperator();
        if (operatorTokens > 0) {
            return mergeAndSkip(operatorTokens);
        } else {
            return null; // FIXME: use null object?
//                throw new QueryMethodSyntaxException("Unexpected token, expected operator, got '" + current + "'"); // FIXME: improve the error message with position
        }
    }

    /**
     * @return number of tokens of the following operator, 0 is there is no operator
     */
    private int followsOperator() {
        if (follows(KEYWORD_LESS, KEYWORD_THAN, KEYWORD_EQUAL)
                || follows(KEYWORD_GREATER, KEYWORD_THAN, KEYWORD_EQUAL)) {
            return 3;
        }
        if (follows(KEYWORD_ENDS, KEYWORD_WITH)
                || follows(KEYWORD_STARTS, KEYWORD_WITH)
                || follows(KEYWORD_LESS, KEYWORD_THAN)
                || follows(KEYWORD_GREATER, KEYWORD_THAN)) {
            return 2;
        }
        if (follows(KEYWORD_CONTAINS)
                || follows(KEYWORD_BETWEEN)
                || follows(KEYWORD_LIKE)
                || follows(KEYWORD_IN)
                || follows(KEYWORD_NULL)
                || follows(KEYWORD_TRUE)
                || follows(KEYWORD_FALSE)) {
            return 1;
        }
        return 0;
    }

    private String grammarProperty() {
        StringBuilder propertyName = new StringBuilder();
        // Process the first part of the property (e.g., "Address")
        String firstPart = grammarIdentifier();
        propertyName.append(decapitalize(firstPart));

        // Process subsequent parts if they exist (e.g., "_City")
        while (follows(KEYWORD_UNDERSCORE)) {
            next(); // Consume the "_" token
            String subsequentPart = grammarIdentifier();
            // Add the JPQL delimiter '.' and the decapitalized subsequent part
            propertyName.append(".").append(decapitalize(subsequentPart));
        }
        return propertyName.toString();
    }

    private String decapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        if (str.length() > 1 && Character.isUpperCase(str.charAt(1)) && Character.isUpperCase(str.charAt(0))) {
            return str; // Avoids changing "URL" to "uRL", assumes acronym
        }
        char c[] = str.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        return new String(c);
    }

    private String grammarIdentifier() {
        // after identifier, there can be: "_", IgnoreCare, Not, operator, And, Or, Desc, Asc, OrderBy, EOF
        StringBuilder identifier = new StringBuilder();
        identifier.append(next());
        while (!(isEof()
                || follows(KEYWORD_UNDERSCORE)
                || follows(KEYWORD_IGNORE, KEYWORD_CASE)
                || follows(KEYWORD_NOT)
                || followsOperator() > 0
                || follows(KEYWORD_AND)
                || follows(KEYWORD_OR)
                || follows(KEYWORD_DESC)
                || follows(KEYWORD_ASC)
                || follows(KEYWORD_ORDER, KEYWORD_BY))) {
            identifier.append(next());
        }
        return identifier.toString();
    }

    private void grammarMax() throws QueryMethodSyntaxException {
        try {
            if (!follows(KEYWORD_BY)) {
                limit = Integer.valueOf(current());
                next();
            } else if (follows(KEYWORD_BY)) {
                grammarRestriction();
            }
        } catch (NumberFormatException ex) {
            throw new QueryMethodSyntaxException("Expected number", methodName, currentToken, tokens, tokensPositions);
            //throw new QueryMethodSyntaxException("Error in method name '" + methodName + "', position " + tokensPositions.get(currentToken) + " should be a number, is '" + current() + "'");
        }
    }

    /**
     * Slightly rewritten to be decidable by the next symbol (order is the last in the method name, it is followed only
     * by EOF):
     * <pre>
     * order : "OrderBy" orderItem
     * orderItem: (property ("Asc" | "Desc")? )*
     * </pre>
     *
     * @throws QueryMethodSyntaxException
     */
    private void grammarOrder() throws QueryMethodSyntaxException {
        next(KEYWORD_ORDER);
        next(KEYWORD_BY);
        orderItem();
    }

    private void orderItem() {
        while (!isEof()) {
            String property = grammarProperty();
            if (follows(KEYWORD_ASC) || follows(KEYWORD_DESC)) {
                String ascDesc = next();
                orderBy.add(new OrderBy(property, ascDesc));
            } else {
                orderBy.add(new OrderBy(property, null));
                break;
            }
        }
    }

    private void grammarIgnoredText(Set<String> ignoreUntil) {
        while (currentToken < tokens.size() && !ignoreUntil.contains(current())) {
            next();
        }
    }

    private String current() {
        return tokens.get(currentToken);
    }

    private boolean follows(String... testedTokens) {
        for (int i = 0; i < testedTokens.length; i++) {
            if (currentToken + i >= tokens.size() || !testedTokens[i].equals(tokens.get(currentToken + i))) {
                return false;
            }
        }
        return true;
    }

    private String next() {
        String current = current();
        currentToken++;
        return current;
    }

    private String next(String expectedToken) throws QueryMethodSyntaxException {
        if (!follows(expectedToken)) {
            throw new QueryMethodSyntaxException("Error in method name '" + methodName + "', position " + tokensPositions.get(currentToken) + " should be " + expectedToken);
        }
        return next();
    }

    private boolean isEof() {
        return currentToken >= tokens.size();
    }

    private String mergeAndSkip(int tokensCount) {
        StringBuilder sb = new StringBuilder();
        for (int i = currentToken; i < currentToken + tokensCount && i < tokens.size(); i++) {
            sb.append(tokens.get(i));
        }
        currentToken += tokensCount;
        return sb.toString();
    }

    public enum Action {
        FIND, DELETE, COUNT, FIND_FOR_DELETE, EXISTS
    }

    private final String methodName;
    private List<String> tokens = new ArrayList<>();
    private List<Integer> tokensPositions = new ArrayList<>();
    private int currentToken = 0; // current position during parsing
    private Action action = null;
    private Integer limit = null;
    private List<OrderBy> orderBy = new ArrayList<>();
    private List<Condition> conditions = new ArrayList<>();

    public QueryMethodParser(String methodName) {
        this.methodName = methodName;
    }

    public QueryMethodParser parse() throws QueryMethodSyntaxException {
        splitTokens();
        grammarQuery();
        return this; // for flow usage
    }

    private void splitTokens() throws QueryMethodSyntaxException {
        tokens = new ArrayList<>();
        int i = 0;
        int len = methodName.length();

        // find the name of the action
        for (Action a : Action.values()) {
            if (methodName.startsWith(a.name().toLowerCase())) {
                action = a;
                i += a.name().length();
                break;
            }
        }
        if (action == null) {
            throw new QueryMethodSyntaxException("Method name must start with find, delete, count or exists. Name: '" + methodName + "'");
        }

        boolean justAfterUnderscore = false; // the grammar allows lowercase right after underscore
        while (i < len) {
            char c = methodName.charAt(i);

            if (Character.isUpperCase(c) || justAfterUnderscore) {
                int start = i;
                i++;
                while (i < len && (Character.isLowerCase(methodName.charAt(i)))) {
                    i++;
                }
                tokens.add(methodName.substring(start, i));
                tokensPositions.add(start);
                justAfterUnderscore = false;
            } else if (Character.isDigit(c)) {
                int start = i++;
                while (i < len && Character.isDigit(methodName.charAt(i))) {
                    i++;
                }
                tokens.add(methodName.substring(start, i));
                tokensPositions.add(start);
                justAfterUnderscore = false;
            } else if (c == '_') {
                tokens.add(KEYWORD_UNDERSCORE);
                tokensPositions.add(i);
                i++;
                justAfterUnderscore = true;
            } else {
                throw new QueryMethodSyntaxException("Invalid character in method name '" + methodName + "', position "
                        + i + ", character '" + c + "' must be uppercase or number. " + methodName.substring(0, i)
                        + "[" + c + "]" + methodName.substring(i + 1));
            }
        }
    }

    public Action getAction() {
        return action;
    }

    public Integer getLimit() {
        return limit;
    }

    /**
     * @return list of tokens, only for testing
     */
    protected List<String> getTokens() {
        return tokens;
    }

    public List<OrderBy> getOrderBy() {
        return orderBy;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    /**
     * Order by the property. The ascDesc is either "Asc", "Desc" or null if not specified.
     */
    public record OrderBy(String property, String ascDesc) {

    }

    public record Condition(LogicalOperator precedingOperator, String property, boolean ignoreCase, boolean not, String operator) {
    }
}
