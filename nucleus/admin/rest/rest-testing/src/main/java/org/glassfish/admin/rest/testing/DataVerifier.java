/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.testing;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataVerifier {
    private Environment env;
    private ObjectValue objectWant;
    private JSONObject objectHave;
    private IndentingStringBuffer sb = new IndentingStringBuffer();

    public DataVerifier(Environment env, ObjectValue objectWant, JSONObject objectHave) {
        this.env = env;
        this.objectWant = objectWant;
        this.objectHave = objectHave;
    }

    private void trace(String msg) {
        this.sb.println(msg);
    }

    private void indent() {
        this.sb.indent();
    }

    private void undent() {
        this.sb.undent();
    }

    private Environment getEnvironment() {
        return this.env;
    }

    public static void verify(Environment env, ObjectValue objectWant, JSONObject objectHave) throws Exception {
        IndentingStringBuffer sb = new IndentingStringBuffer();
        objectWant.print(sb);
        env.debug("Body want : " + sb.toString());
        env.debug("Body have : " + objectHave.toString(2));
        (new DataVerifier(env, objectWant, objectHave)).verify();
    }

    private void verify() throws Exception {
        try {
            if (!sameProperties(this.objectWant.getProperties(), this.objectHave, this.objectWant.isIgnoreExtra())) {
                trace("Response object want=");
                this.objectWant.print(sb);
                trace("Response object have=");
                indent();
                try {
                    trace(this.objectHave.toString(2));
                } finally {
                    undent();
                }
                throw new IllegalArgumentException("Response object does not match expected value");
            } else {
                trace("same response bodies");
                getEnvironment().debug(this.sb.toString());
            }
        } catch (Exception e) {
            throw new Exception("Exception verifying resource response object\n" + this.sb.toString(), e);
        }
    }

    private boolean sameProperties(Map<String, Value> want, JSONObject have, boolean ignoreExtra) throws Exception {
        trace("comparing properties, ignoreExtra=" + ignoreExtra);
        indent();

        try {
            if (want.size() > have.length()) {
                trace("different since object has too few properties");
                return false;
            }
            if (!(ignoreExtra) && want.size() < have.length()) {
                trace("different since object has too many properties want=" + want.size() + " have=" + have.length());
                return false;
            }
            for (Map.Entry<String, Value> p : want.entrySet()) {
                if (!sameProperty(p.getKey(), p.getValue(), have)) {
                    trace("different since object didn't have matching " + p.getKey() + " property");
                    return false;
                }
            }
            trace("same properties");
            return true;
        } finally {
            undent();
        }
    }

    private boolean sameArray(ArrayValue want, JSONArray have) throws Exception {
        trace("comparing arrays, ignoreExtra=" + want.isIgnoreExtra() + ", ordered=" + want.isOrdered());
        indent();
        try {
            boolean ignoreExtra = want.isIgnoreExtra();
            boolean ordered = want.isOrdered();
            List<Value> wantVals = want.getValues();

            if (ordered && ignoreExtra) {
                throw new AssertionError("ignore-extra must be false if ordered is true");
            }

            if (wantVals.size() > have.length()) {
                trace("different since array has too few elements");
                return false;
            }
            if (!ignoreExtra && wantVals.size() < have.length()) {
                trace("diffent since array has too many elements");
                return false;
            }
            if (ordered) {
                for (int i = 0; i < wantVals.size(); i++) {
                    if (!sameValue(wantVals.get(i), have, i)) {
                        trace("different since array element " + i + " didn't match");
                        return false;
                    }
                }
                trace("same ordered elements");
                return true;
            } else {
                if ((new UnorderedArrayMatcher(want, have)).matches()) {
                    trace("same unordered elements");
                    return true;
                } else {
                    trace("different unorder elements");
                    return false;
                }
            }
        } finally {
            undent();
        }
    }

    private boolean sameProperty(String nameWant, Value valueWant, JSONObject have) throws Exception {
        trace("comparing property " + nameWant);
        indent();
        try {
            if (!have.has(nameWant)) {
                trace("missing property " + nameWant);
                return false;
            }
            if (!sameValue(valueWant, have, nameWant)) {
                trace("different value for property " + nameWant);
                return false;
            }
            trace("same property " + nameWant);
            return true;
        } finally {
            undent();
        }
    }

    private boolean sameValue(Value want, JSONObject parent, String name) throws Exception {
        if (want instanceof ObjectValue) {
            return sameObject((ObjectValue) want, parent, name);
        }
        if (want instanceof ArrayValue) {
            return sameArray((ArrayValue) want, parent, name);
        }
        if (want instanceof ScalarValue) {
            return sameScalar((ScalarValue) want, parent, name);
        }
        if (want instanceof NilValue) {
            return sameNil(parent, name);
        }
        throw new AssertionError("Unknown value " + want);
    }

    private boolean sameValue(Value want, JSONArray parent, int index) throws Exception {
        if (want instanceof ObjectValue) {
            return sameObject((ObjectValue) want, parent, index);
        }
        if (want instanceof ArrayValue) {
            return sameArray((ArrayValue) want, parent, index);
        }
        if (want instanceof ScalarValue) {
            return sameScalar((ScalarValue) want, parent, index);
        }
        if (want instanceof NilValue) {
            return sameNil(parent, index);
        }
        throw new AssertionError("Unknown value " + want);
    }

    private boolean sameObject(ObjectValue want, JSONObject parent, String name) throws Exception {
        trace("comparing object object property " + name);
        indent();
        try {
            try {
                JSONObject have = parent.getJSONObject(name);
                if (sameProperties(want.getProperties(), have, want.isIgnoreExtra())) {
                    trace("same object");
                    return true;
                } else {
                    trace("different object");
                    return false;
                }
            } catch (JSONException e) {
                trace("different since property was not an object");
                return false;
            }
        } finally {
            undent();
        }
    }

    private boolean sameObject(ObjectValue want, JSONArray parent, int index) throws Exception {
        trace("comparing array object element " + index);
        indent();
        try {
            try {
                JSONObject have = parent.getJSONObject(index);
                if (sameProperties(want.getProperties(), have, want.isIgnoreExtra())) {
                    trace("same object");
                    return true;
                } else {
                    trace("different object");
                    return false;
                }
            } catch (JSONException e) {
                trace("different since property was not an object");
                return false;
            }
        } finally {
            undent();
        }
    }

    private boolean sameArray(ArrayValue want, JSONObject parent, String name) throws Exception {
        trace("comparing object array property " + name);
        indent();
        try {
            try {
                if (sameArray(want, parent.getJSONArray(name))) {
                    trace("same array");
                    return true;
                } else {
                    trace("different array");
                    return false;
                }
            } catch (JSONException e) {
                trace("different since property was not an array");
                return false;
            }
        } finally {
            undent();
        }
    }

    private boolean sameArray(ArrayValue want, JSONArray parent, int index) throws Exception {
        trace("comparing array array element " + index);
        indent();
        try {
            try {
                if (sameArray(want, parent.getJSONArray(index))) {
                    trace("same array");
                    return true;
                } else {
                    trace("different array");
                    return false;
                }
            } catch (JSONException e) {
                trace("different since property was not an array");
                return false;
            }
        } finally {
            undent();
        }
    }

    private boolean sameScalar(ScalarValue want, JSONObject parent, String name) throws Exception {
        trace("comparing object scalar property " + name);
        String regexp = want.getRegexp();
        if (want instanceof StringValue) {
            return sameString(((StringValue) want).getValue(), regexp, parent.getString(name));
        }
        if (want instanceof LongValue) {
            return sameString(longToString(((LongValue) want).getValue()), regexp, longToString(parent.getLong(name)));
        }
        if (want instanceof IntValue) {
            return sameString(intToString(((IntValue) want).getValue()), regexp, intToString(parent.getInt(name)));
        }
        if (want instanceof DoubleValue) {
            return sameString(doubleToString(((DoubleValue) want).getValue()), regexp, doubleToString(parent.getDouble(name)));
        }
        if (want instanceof BooleanValue) {
            return sameString(booleanToString(((BooleanValue) want).getValue()), regexp, booleanToString(parent.getBoolean(name)));
        }
        throw new AssertionError(want + " is not a valid scalar type.  Valid types are string, long, int, double, boolean");
    }

    private boolean sameScalar(ScalarValue want, JSONArray parent, int index) throws Exception {
        trace("comparing array scalar element " + index);
        String regexp = want.getRegexp();
        if (want instanceof StringValue) {
            return sameString(((StringValue) want).getValue(), regexp, parent.getString(index));
        }
        if (want instanceof LongValue) {
            return sameString(longToString(((LongValue) want).getValue()), regexp, longToString(parent.getLong(index)));
        }
        if (want instanceof IntValue) {
            return sameString(intToString(((IntValue) want).getValue()), regexp, intToString(parent.getInt(index)));
        }
        if (want instanceof DoubleValue) {
            return sameString(doubleToString(((DoubleValue) want).getValue()), regexp, doubleToString(parent.getDouble(index)));
        }
        if (want instanceof BooleanValue) {
            return sameString(booleanToString(((BooleanValue) want).getValue()), regexp, booleanToString(parent.getBoolean(index)));
        }
        throw new AssertionError(want + " is not a valid scalar type.  Valid types are string, long, int, double, boolean");
    }

    private String longToString(long val) {
        return Long.toString(val);
    }

    private String intToString(int val) {
        return Integer.toString(val);
    }

    private String doubleToString(double val) {
        return Double.toHexString(val);
    }

    private String booleanToString(boolean val) {
        return Boolean.toString(val);
    }

    private boolean sameString(String want, String regexp, String have) throws Exception {
        if (Common.haveValue(regexp)) {
            return sameRegexpString(regexp, have);
        } else {
            return sameLiteralString(want, have);
        }
    }

    private boolean sameRegexpString(String regexp, String have) throws Exception {
        trace("comparing string against regular expression regexp='" + regexp + "', have='" + have + "'");
        indent();
        try {
            Pattern pattern = Pattern.compile(regexp);
            Matcher matcher = pattern.matcher(have);
            if (matcher.matches()) {
                trace("same since matches regular expression");
                return true;
            } else {
                trace("different since does not match regular expression");
                return false;
            }
        } finally {
            undent();
        }
    }

    private boolean sameLiteralString(String want, String have) throws Exception {
        trace("comparing strings want='" + want + "', have='" + have + "'");
        indent();
        try {
            if (Common.haveValue(want) != Common.haveValue(have)) {
                trace("different strings - one is null and the other is not");
                return false;
            }
            if (!Common.haveValue(want)) {
                trace("same empty strings");
                return true;
            }
            if (want.equals(have)) {
                trace("same literal value");
                return true;
            } else {
                trace("different literal value");
                return false;
            }
        } finally {
            undent();
        }
    }

    private boolean sameNil(JSONObject parent, String name) {
        trace("comparing object nil property " + name);
        indent();
        try {
            if (parent.isNull(name)) {
                trace("same nil");
                return true;
            } else {
                trace("different nil");
                return false;
            }
        } finally {
            undent();
        }
    }

    private boolean sameNil(JSONArray parent, int index) {
        trace("comparing array nil element " + index);
        indent();
        try {
            if (parent.isNull(index)) {
                trace("same nil");
                return true;
            } else {
                trace("different nil");
                return false;
            }
        } finally {
            undent();
        }
    }

    // tbd - speed this up by finding out if the value is exact or flexible (ie. regexp or ignoreExtra) - if exact, then grab it as soon as we have a match
    private class UnorderedArrayMatcher {

        private int matchCount = 0;
        private ArrayValue want;
        private List<Value> wantValues;
        private JSONArray have;
        private int[] wantMatches; // an entry for each want element. -1 means it hasn't been matched yet. otherwise, the index of the have element we're matched with
        private int[] haveMatches; // an entry for each have element. -1 means it hasn't been matched yet. otherwise, the index of the want element we're matched with
        private boolean[][] potentialMatches; // wantPotentialMatches[wantIndex][haveIndex] indicates if want matches have

        private UnorderedArrayMatcher(ArrayValue want, JSONArray have) {
            this.want = want;
            this.wantValues = this.want.getValues();
            this.have = have;
            this.wantMatches = new int[this.wantValues.size()];
            this.haveMatches = new int[this.have.length()];
            for (int i = 0; i < wantCount(); i++) {
                setWantMatch(i, -1);
            }
            for (int i = 0; i < haveCount(); i++) {
                setHaveMatch(i, -1);
            }

            this.potentialMatches = new boolean[wantCount()][];
            for (int i = 0; i < this.wantMatches.length; i++) {
                this.potentialMatches[i] = new boolean[haveCount()];
                for (int j = 0; j < haveCount(); j++) {
                    setPotentialMatch(i, j, false);
                }
            }
        }

        private boolean matches() throws Exception {
            findExactMatches();
            if (!done()) {
                findPotentialMatches();
                while (!done()) {
                    findMatches();
                    if (!done()) {
                        selectAnyMatch();
                    }
                }
            }
            return result();
        }

        private void findExactMatches() throws Exception {
            for (int i = 0; i < wantCount(); i++) {
                Value v = getWantValue(i);
                if (requiresExactMatch(v)) {
                    for (int j = 0; j < haveCount(); j++) {
                        if (!haveHaveMatch(j) && sameValue(v, this.have, j)) {
                            matched(i, j);
                            break;
                        }
                    }
                    if (!haveWantMatch(i)) {
                        notMatched(i);
                        return;
                    }
                }
            }
        }

        private void findPotentialMatches() throws Exception {
            for (int i = 0; i < wantCount(); i++) {
                if (!haveWantMatch(i)) {
                    for (int j = 0; j < haveCount(); j++) {
                        if (!haveHaveMatch(j) && sameValue(getWantValue(i), this.have, j)) {
                            setPotentialMatch(i, j, true);
                        }
                    }
                    int c = potentialMatchCount(i);
                    if (c == 0) {
                        notMatched(i);
                        return;
                    }
                    if (c == 1) {
                        for (int j = 0; j < haveCount(); j++) {
                            if (isPotentialMatch(i, j)) {
                                matched(i, j);
                            }
                        }
                    }
                }
            }
        }

        // keep going until we run out of matches
        private void findMatches() throws Exception {
            boolean modified = true;
            while (modified) {
                modified = false;

                for (int i = 0; i < wantCount(); i++) {
                    if (!haveWantMatch(i)) {
                        int c = potentialMatchCount(i);

                        // we're all done if I don't match anyone
                        if (c == 0) {
                            notMatched(i);
                            return;
                        }

                        // if I only have one match, grab it
                        if (c == 1) {
                            for (int j = 0; j < haveCount(); j++) {
                                if (isPotentialMatch(i, j)) {
                                    matched(i, j);
                                    modified = true;
                                }
                            }
                        }

                        // if I have a potential match that no one else has, grab it
                        if (c > 1) {
                            for (int j = 0; j < haveCount(); j++) {
                                if (isPotentialMatch(i, j) && !matchedBySomeoneElse(i, j)) {
                                    matched(i, j); // this empties out all my other potential matches, which will terminate the 'have' loop
                                    modified = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        // we've found all the unambiguous matches we can
        // select a match to break the deadlock.
        private void selectAnyMatch() throws Exception {
            for (int i = 0; i < wantCount(); i++) {
                if (!haveWantMatch(i)) {
                    for (int j = 0; j < haveCount(); j++) {
                        if (!haveHaveMatch(j)) {
                            matched(i, j);
                            return;
                        }
                    }
                }
            }
        }

        private boolean matchedBySomeoneElse(int wantIndex, int haveIndex) {
            for (int i = 0; i < wantCount(); i++) {
                if (i != wantIndex) { // ignore me
                    if (isPotentialMatch(i, haveIndex)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void notMatched(int wantIndex) {
            this.matchCount = -1; // there is no match
            trace("Different since no match for array element " + wantIndex);
        }

        private void matched(int wantIndex, int haveIndex) {
            trace("matched array element want=" + wantIndex + " have=" + haveIndex);

            // record the match
            this.matchCount++;
            setWantMatch(wantIndex, haveIndex);
            setHaveMatch(haveIndex, wantIndex);
            setPotentialMatch(wantIndex, haveIndex, true);

            // i can't match anyone else
            for (int i = 0; i < haveCount(); i++) {
                if (i != haveIndex) {
                    setPotentialMatch(wantIndex, i, false);
                }
            }

            // no one else can have this match
            for (int i = 0; i < wantCount(); i++) {
                if (i != wantIndex) {
                    setPotentialMatch(i, haveIndex, false);
                }
            }
        }

        private boolean done() {
            if (this.matchCount == -1) {
                return true;
            }
            if (this.matchCount == this.wantMatches.length) {
                return true;
            }
            return false;
        }

        private boolean result() {
            if (!done()) {
                throw new AssertionError("Asking for result before we're done finding matches.");
            }
            return (this.matchCount == -1) ? false : true;
        }

        private boolean isPotentialMatch(int wantIndex, int haveIndex) {
            return this.potentialMatches[wantIndex][haveIndex];
        }

        private void setPotentialMatch(int wantIndex, int haveIndex, boolean matches) {
            this.potentialMatches[wantIndex][haveIndex] = matches;
        }

        private int wantCount() {
            return this.wantMatches.length;
        }

        private int haveCount() {
            return this.haveMatches.length;
        }

        private boolean haveWantMatch(int wantIndex) {
            return (getWantMatch(wantIndex) != -1);
        }

        private boolean haveHaveMatch(int haveIndex) {
            return (getHaveMatch(haveIndex) != -1);
        }

        private int getWantMatch(int wantIndex) {
            return this.wantMatches[wantIndex];
        }

        private int getHaveMatch(int haveIndex) {
            return this.haveMatches[haveIndex];
        }

        private void setWantMatch(int wantIndex, int haveIndex) {
            this.wantMatches[wantIndex] = haveIndex;
        }

        private void setHaveMatch(int haveIndex, int wantIndex) {
            this.haveMatches[haveIndex] = wantIndex;
        }

        private int potentialMatchCount(int wantIndex) {
            int count = 0;
            for (int i = 0; i < haveCount(); i++) {
                if (isPotentialMatch(wantIndex, i)) {
                    count++;
                }
            }
            return count;
        }

        private Value getWantValue(int index) {
            return this.wantValues.get(index);
        }

        private boolean requiresExactMatch(Value val) {
            if (val instanceof ObjectValue) {
                ObjectValue v = (ObjectValue) val;

                if (v.isIgnoreExtra()) {
                    return false;
                }
                for (Map.Entry<String, Value> p : v.getProperties().entrySet()) {
                    if (!requiresExactMatch(p.getValue())) {
                        return false;
                    }
                }
                return true;
            }
            if (val instanceof ArrayValue) {
                ArrayValue v = (ArrayValue) val;
                if (v.isIgnoreExtra() || !(v.isOrdered())) {
                    return false;
                }
                for (Value e : v.getValues()) {
                    if (!requiresExactMatch(e)) {
                        return false;
                    }
                }
                return true;
            }
            if (val instanceof ScalarValue) {
                ScalarValue v = (ScalarValue) val;
                if (Common.haveValue(v.getRegexp())) {
                    return false;
                }
                return true;
            }
            if (val instanceof NilValue) {
                return true;
            }
            throw new AssertionError("Unknown value " + want);
        }
    }
}
