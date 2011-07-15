/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.jbi.serviceengine.util.soap;

/**
 * This object represents the operation supported by a JBI service engine. This
 * information is obtained when a service has been deployed on the SOAP Binding
 * component.
 *
 * @author Sun Microsystems, Inc.
 */
public class Operation
{
    /**
     * Operation name.
     */
    private String mOperationName;

    /**
     * Pattern used by the operation.
     */
    private String mPattern;

    /**
     * Operation's SOAP action URI.
     */
    private String mSoapAction;

    /**
     * Style used to physically represent the operation message style. This is either
     * "rpc" style, "uri" style or "multipart" style. These styles are based on WSDL 2.0.
     */
    private String mOperationStyle;

    /**
     * Style used to physically represent the operation message style. This is either
     * "rpc" style, "uri" style or "multipart" style. These styles are based on WSDL 2.0.
     */
    private String mInterfaceStyle;

    /**
     * Input operation namespace.
     */
    private String mInputNamespace;

    /**
     * Output operation namespace.
     */
    private String mOutputNamespace;

    /**
     * Creates a new instance of Operation.
     *
     * @param operationName operation name
     * @param pattern message exchange pattern name.
     */
    public Operation(String operationName, String pattern)
    {
        mPattern = pattern;
        mOperationName = operationName;
        mOperationStyle = null;
        mSoapAction = "\"\"";
    }

    /**
     * Gets the operation name.
     *
     * @return the operation name.
     */
    public String getName()
    {
        return mOperationName;
    }

    /**
     * Gets the message exchange pattern name.
     *
     * @return message exchange pattern name.
     */
    public String getPattern()
    {
        return mPattern;
    }

    /**
     * Sets the soap Action URI.
     *
     * @param soapAction soapAction associated with this operation.
     */
    public void setSoapAction(String soapAction)
    {
        if ( ( soapAction != null) && ( !soapAction.equals("")) )
        {
            mSoapAction = soapAction;
        }
    }

    /**
     * Gets the soap Action URI.
     *
     * @return soap action associated with this operation.
     */
    public String getSoapAction()
    {
        return mSoapAction;
    }

    /**
     * Sets the operation style.
     *
     * @param operationStyle operation style.
     */
    public void setStyle(String operationStyle)
    {
        mOperationStyle = operationStyle;
    }

    /**
     * Gets the operation style.
     *
     * @return operation style.
     */
    public String getStyle()
    {
        if (mOperationStyle != null)
        {
            return mOperationStyle;
        }
        else
        {
            return mInterfaceStyle;
        }
    }

    /**
     * Indicates whether the operation input is encoded or not.
     *
     * @return false if it is not encoded;true otherwise.
     */
    public boolean isInputEncoded()
    {
        return false;
    }

    /**
     * Sets the operation input namespace.
     *
     * @param namespace operation input namespace
     */
    public void setInputNamespace(String namespace)
    {
        mInputNamespace = namespace;
    }

    /**
     * Gets the operation input namespace.
     *
     * @return operation input namespace;
     */
    public String getInputNamespace()
    {
        return mInputNamespace;
    }

    /**
     * Indicates whether the operation output is encoded or not.
     *
     * @return false if it is not encoded;true otherwise.
     */
    public boolean isOutputEncoded()
    {
        return false;
    }

    /**
     * Sets the operation output namespace.
     *
     * @param namespace operation output namespace
     */
    public void setOutputNamespace(String namespace)
    {
        mOutputNamespace = namespace;
    }

    /**
     * Gets the operation output namespace.
     *
     * @return operation output namespace;
     */
    public String getOutputNamespace()
    {
        return mOutputNamespace;
    }

    /**
     * Sets the interface style.
     *
     * @param style interface style
     */
    public void setInterfaceStyle(String style)
    {
        mInterfaceStyle = style;
    }

    /**
     * Returns the operation as a string.
     *
     * @return operation represented as a string.
     */
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{ name = " + mOperationName);
        buffer.append(", pattern = " + mPattern);
        buffer.append(", style = " +  getStyle());
        buffer.append(", soap action = " + mSoapAction);
        buffer.append(", input namespace = " + mInputNamespace);
        buffer.append(", output namespace = " + mOutputNamespace);
        buffer.append("}");
        return buffer.toString();
    }
}
