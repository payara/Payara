/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.monitoring.rest.app.processor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonValue;
import javax.management.ObjectName;
import javax.management.openmbean.SimpleType;

/**
 *
 * @author Fraser Savage
 */
// @PROPOSED - FANG-2: Flesh out flexible converter system.
public class SimpleTypeProcessor implements TypeProcessor<SimpleType> {

    @Override
    public JsonValue processObject(Object object) {
        if (SimpleType.BIGDECIMAL.isValue(object)) {
            return Json.createValue((BigDecimal) object);
        } else if (SimpleType.BIGINTEGER.isValue(object)) {
            return Json.createValue((BigInteger) object);
        } else if (SimpleType.BOOLEAN.isValue(object)) {
            Boolean value = (Boolean) object;
            if (value) {
                return JsonValue.TRUE;
            } else {
                return JsonValue.FALSE;
            }
        } else if (SimpleType.DOUBLE.isValue(object)) {
            return Json.createValue((Double) object);
        } else if (SimpleType.INTEGER.isValue(object)) {
            return Json.createValue((Integer) object);
        } else if (SimpleType.LONG.isValue(object)) {
            return Json.createValue((Long) object);
        } else if (SimpleType.STRING.isValue(object)) {
            return Json.createValue((String) object);
        } else if (object == null || SimpleType.VOID.isValue(object)){
            return JsonValue.NULL;         
        } else if (SimpleType.BYTE.isValue(object)) {
            return Json.createValue(((Byte) object).intValue());
        } else if (SimpleType.CHARACTER.isValue(object)) {
            return Json.createValue(((Character) object).toString());
        } else if (SimpleType.DATE.isValue(object)) {
            return Json.createValue(((Date) object).toString());
        } else if (SimpleType.FLOAT.isValue(object)) {
            return Json.createValue(BigDecimal.valueOf((Long) object));
        } else if (SimpleType.OBJECTNAME.isValue(object)) {
            return Json.createValue(((ObjectName) object).toString());
        } else if (SimpleType.SHORT.isValue(object)) {
            return Json.createValue(((Short) object).intValue());
        } else {
            throw new JsonException("Unable to process object: " + object);
        }
    }

}
