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

import jakarta.data.Sort;

import java.util.Collection;

/**
 * Parse the beginning of the SELECT command, fill the optional parts and return
 * complete query.
 *
 * @author Petr Aubrecht
 */
public class OptionalPartsParser {

    private static final String SELECT = "SELECT";
    private static final String COUNT = "COUNT";
    private static final String THIS = "THIS";
    private static final String DOT = ".";
    private static final String FROM = "FROM";
    private static final String OPEN_PARENTHESIS = "(";
    private static final String CLOSE_PARENTHESIS = ")";

    private String queryString;
    private String entity;
    Collection<Sort<?>> sorts;
    private int position = 0;
    private StringBuilder updatedQueryString = new StringBuilder();

    public OptionalPartsParser(String queryString, String entity) {
        this(queryString, entity, null);
    }

    public OptionalPartsParser(String queryString, String entity, Collection<Sort<?>> sorts) {
        this.queryString = queryString;
        this.entity = entity;
        this.sorts = sorts;
        selectStatement();
    }

    public String getCompleteSelect() {
        return updatedQueryString.toString().stripTrailing();
    }

    /**
     * select_statement : select_clause? from_clause? where_clause?
     * orderby_clause?;
     */
    private void selectStatement() {
        skipOpionalSpace();
        if (startsWith(SELECT)) {
            selectClause();
        } else {
            updatedQueryString.append(SELECT + " this");
        }
        skipSpace();
        if (startsWith(FROM)) {
            fromClause();
        } else {
            updatedQueryString.append(FROM + " " + entity);
        }
        skipSpace();
        // do not care about the rest
        copyRest();
        appendSorts();
    }

    /**
     * select_clause : 'SELECT' select_list;
     */
    private void selectClause() {
        skip(SELECT);
        selectList();
    }

    /**
     * from_clause : 'FROM' entity_name;
     */
    private void fromClause() {
        skip(FROM);
        skipSpace();
        identifier(); // entity_name : identifier
    }

    /**
     * Copy the remaining part of the SELECT command.
     */
    private void copyRest() {
        updatedQueryString.append(queryString.substring(position));
    }

    /**
     * Add ORDER BY with sorts. The original query shouldn't have ORDER BY if there are sorts,
     * otherwise we'll get a corrupted query, with 2 ORDER BY clauses
     */
    private void appendSorts() {
        if (sorts != null && !sorts.isEmpty()) {
            updatedQueryString.append("ORDER BY ");
            boolean firstItem = true;
            for (Sort<?> sort : sorts) {
                if (!firstItem) {
                    updatedQueryString.append(",");
                }
                if (sort.ignoreCase()) {
                    updatedQueryString.append("UPPER(");
                }
                updatedQueryString.append(sort.property());
                if (sort.ignoreCase()) {
                    updatedQueryString.append(")");
                }
                if (sort.isAscending()) {
                    updatedQueryString.append(" ASC");
                }
                if (sort.isDescending()) {
                    updatedQueryString.append(" DESC");
                }
            }
        }
    }

    /**
     * select_list : state_field_path_expression | aggregate_expression;
     */
    private void selectList() {
        skipSpace();
        if (startsWith(COUNT)) {
            aggregateExpression();
        } else {
            stateFieldPathExpression();
        }
    }

    /**
     * aggregate_expression : 'COUNT' '(' 'THIS' ')';
     */
    private void aggregateExpression() {
        skip(COUNT);
        skipOpionalSpace();
        testAndskip(OPEN_PARENTHESIS);
        skipOpionalSpace();
        testAndskip(THIS);
        skipOpionalSpace();
        testAndskip(CLOSE_PARENTHESIS);
    }

    /**
     * state_field_path_expression : IDENTIFIER ('.' IDENTIFIER)*;
     */
    private void stateFieldPathExpression() {
        identifier();
        skipOpionalSpace();
        while (startsWith(DOT)) {
            skip(DOT);
            skipOpionalSpace();
            identifier();
            skipOpionalSpace();
        }
    }

    /**
     * Identifier is any Java identifier
     */
    private void identifier() {
//        skipOpionalSpace();
        if (position >= queryString.length() || !Character.isJavaIdentifierStart(queryString.charAt(position))) {
            throw new IllegalArgumentException("Expected identifier at position " + position);
        }
        int start = position;
        position++;
        while (position < queryString.length() && Character.isJavaIdentifierPart(queryString.charAt(position))) {
            position++;
        }

        updatedQueryString.append(queryString, start, position);
    }

    private boolean startsWith(String terminal) {
        return position + terminal.length() <= queryString.length()
                && queryString.regionMatches(true, position, terminal, 0, terminal.length());
    }

    private void skip(String terminal) {
        updatedQueryString.append(terminal);
        position += terminal.length();
    }

    private void testAndskip(String terminal) {
        if (!startsWith(terminal)) {
            throw new IllegalArgumentException("Expected " + terminal + " at position " + position);
        }
        skip(terminal);
    }

    private void skipOpionalSpace() {
        while (position < queryString.length() && Character.isWhitespace(queryString.charAt(position))) {
            position++;
        }
    }

    private void skipSpace() {
        skipOpionalSpace();
        // replace all spaces with one space
        updatedQueryString.append(" ");
    }

}
