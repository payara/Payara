/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.util.jmx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import org.glassfish.admin.amx.util.ClassUtil;
import org.glassfish.admin.amx.util.jmx.stringifier.MBeanNotificationInfoStringifier;
import org.glassfish.admin.amx.util.stringifier.SmartStringifier;

/**
Generate an MBean ".java" file.
 */
public class MBeanInterfaceGenerator
{
    boolean mEmitComments;

    Map<String, Integer> mCounts;

    public MBeanInterfaceGenerator()
    {
        mCounts = null;
        mEmitComments = true;
    }
    //private final static String INDENT = "\t";

    private final static String INDENT = "    ";

    private final static String NEWLINE = "\n";

    private final static String PARAM_DELIM = ", ";

    public final static String FINAL_PREFIX = "final ";

    public final static int IMPORT_THRESHOLD = 1;

    private static final String BRACKETS = "[]";

    static String stripBrackets(String name)
    {
        String result = name;

        while (result.endsWith(BRACKETS))
        {
            result = result.substring(0, result.length() - BRACKETS.length());
        }

        return (result);
    }

    private static void countType(Map<String, Integer> counts, String typeIn)
    {
        final String type = stripBrackets(ClassUtil.getFriendlyClassname(typeIn));

        Integer count = counts.get(type);
        if (count == null)
        {
            count = Integer.valueOf(1);
        }
        else
        {
            count = Integer.valueOf(count.intValue() + 1);
        }

        counts.put(type, count);
    }

    /**
    Count how many times an Attribute type is used.
     */
    public static void countTypes(Map<String, Integer> counts, MBeanAttributeInfo[] infos)
    {
        for (int i = 0; i < infos.length; ++i)
        {
            countType(counts, infos[i].getType());
        }
    }

    /**
    Count how many times the return type and parameter types are used.
     */
    private static void countTypes(Map<String, Integer> counts, MBeanOperationInfo[] infos)
    {
        for (int i = 0; i < infos.length; ++i)
        {
            countType(counts, infos[i].getReturnType());

            final MBeanParameterInfo[] params = infos[i].getSignature();
            for (int p = 0; p < params.length; ++p)
            {
                countType(counts, params[p].getType());
            }
        }
    }

		String
	getCodeClassname( final String classnameIn )
	{
		final String	name	= ClassUtil.getFriendlyClassname( classnameIn );
        
        String base = name;
        String extra = "";
        
        final int idx = name.indexOf("[");
        if ( idx > 0 )
        {
            base  = name.substring(0, idx);
            extra = name.substring(idx);
        }
        
		if ( typeMayBeAbbreviated( base ) )
		{
			base	= ClassUtil.stripPackagePrefix( base );
		}
        
		return base + extra;
	}
	
    private Map<String, Integer> countAllTypes(MBeanInfo info)
    {
        final Map<String, Integer> counts = new HashMap<String, Integer>();
        final MBeanAttributeInfo[] attrInfos = info.getAttributes();
        final MBeanOperationInfo[] operationInfos = info.getOperations();
        if (attrInfos != null)
        {
            countTypes(counts, attrInfos);
        }
        if (operationInfos != null)
        {
            countTypes(counts, operationInfos);
        }

        return (counts);
    }

    private String getImportBlock(Map<String, Integer> counts)
    {
        final StringBuffer buf = new StringBuffer();

        for (final Map.Entry<String, Integer> me : counts.entrySet())
        {
            final String key = me.getKey();
            final Integer count = me.getValue();

            // if used twice or more, generate an import statement
            if (count.intValue() >= IMPORT_THRESHOLD && !isUnqualifiedType(key))
            {
                buf.append("import ").append(key).append(";" + NEWLINE);
            }
        }

        return (buf.toString());
    }

    protected boolean isUnqualifiedType(String type)
    {
        return (type.indexOf(".") < 0);
    }

    /**
    type must be the "friendly" name.
     */
    protected boolean typeMayBeAbbreviated(String type)
    {
        final Integer count = mCounts.get(type);
        if (count == null)
        {
            return (false);
        }

        return (count.intValue() >= IMPORT_THRESHOLD);
    }

    public String generate(final MBeanInfo info, boolean emitComments)
    {
        mEmitComments = emitComments;

        final StringBuffer buf = new StringBuffer();

        if (mEmitComments)
        {
            buf.append(getHeaderComment(info)).append(NEWLINE + NEWLINE);
        }

        buf.append("package ").append(getPackageName(info)).append(";" + NEWLINE);

        mCounts = countAllTypes(info);
        buf.append(NEWLINE).append(getImportBlock(mCounts)).append(NEWLINE);

        if (mEmitComments)
        {
            buf.append(getInterfaceComment(info)).append(NEWLINE);
        }
        String interfaceName = getClassname(info);
        buf.append("public interface ").append(interfaceName).append(" \n{\n");

        final MBeanAttributeInfo[] attrInfos = info.getAttributes();
        final MBeanOperationInfo[] operationInfos = info.getOperations();
        if (attrInfos != null)
        {
            Arrays.sort(attrInfos, MBeanAttributeInfoComparator.INSTANCE);
            
            final List<MBeanAttributeInfo> readOnlyAttrInfos  = new ArrayList<MBeanAttributeInfo>();
            final List<MBeanAttributeInfo> writebleAttrInfos  = new ArrayList<MBeanAttributeInfo>();
            for(  final MBeanAttributeInfo ai : attrInfos )
            {
                if ( ai.isWritable() )
                {
                    writebleAttrInfos.add(ai);
                }
                else
                {
                    readOnlyAttrInfos.add(ai);
                }
            }
            
			buf.append( generateAttributes( readOnlyAttrInfos ) );
			buf.append( generateAttributes( writebleAttrInfos ) );
        }
        if (operationInfos != null)
        {
            if (operationInfos.length != 0)
            {
                Arrays.sort(operationInfos, MBeanOperationInfoComparator.INSTANCE);

                buf.append(NEWLINE + "// -------------------- Operations --------------------" + NEWLINE);
                buf.append(generateOperations(operationInfos));
            }
        }


        buf.append("\n}");

        return (buf.toString());
    }

    protected String indent(String contents, String prefix)
    {
        final StringBuffer buf = new StringBuffer();
        if (contents.length() != 0)
        {
            final String[] lines = contents.split(NEWLINE);

            for (int i = 0; i < lines.length; ++i)
            {
                buf.append(prefix + lines[i] + NEWLINE);
            }

            if (buf.length() != 0)
            {
                buf.setLength(buf.length() - 1);
            }
        }

        return (buf.toString());
    }

    protected String indent(String contents)
    {
        return (indent(contents, INDENT));
    }

    protected String makeJavadocComment(String contents)
    {
        if (contents == null || contents.length() == 0)
        {
            return "";
        }
        return ("/**" + NEWLINE + indent(contents) + NEWLINE + "*/");
    }

    private String padRight(final String s, final int fieldWidth)
    {
        final StringBuffer buf = new StringBuffer();
        buf.append(s);
        buf.append(" ");
        for( int i = 0; i < fieldWidth - s.length(); ++i )
        {
            buf.append(" ");
        }
        
        return buf.toString();
    }
    
    protected String formMethod(String returnType, String name, String[] params, String[] names)
    {
        final String begin = "public " + padRight( getCodeClassname(returnType), 16) + " " + name + "(";
        String paramsString = "";
        if (params != null && params.length != 0)
        {
            final StringBuffer buf = new StringBuffer();

            buf.append(" ");
            for (int i = 0; i < params.length; ++i)
            {
                //buf.append(FINAL_PREFIX);
                buf.append(getCodeClassname(params[i]));
                buf.append(" " + names[i]);
                buf.append(PARAM_DELIM);
            }

            buf.setLength(buf.length() - PARAM_DELIM.length());	// strip last ","
            buf.append(" ");
            paramsString = buf.toString();
        }


        return (begin + paramsString + ");");
    }

    /**
    Return a comment regarding the Attribute name if it was mapped to a different
    Java name.
     */
    protected String getAttributeNameComment(String attributeName, String javaName)
    {
        String comment = "";

        if (!attributeName.equals(javaName))
        {
            comment = attributeName + " => " + javaName;
        }
        return comment;
    }

    protected String attributeNameToJavaName(final String attrName)
    {
        return attrName;
    }

    protected String generateAttributes( final List<MBeanAttributeInfo> infos)
    {
        final StringBuffer buf = new StringBuffer();

        final String[] typeTemp = new String[1];
        final String[] nameTemp = new String[1];
        
        final MBeanAttributeInfo[] infosArray = new MBeanAttributeInfo[infos.size()];
        infos.toArray( infosArray );

        for ( final MBeanAttributeInfo info: infos )
        {
            final String attributeName = info.getName();
            final String type = info.getType();
            String comment = "";

            final String javaName = attributeNameToJavaName(attributeName);
            
            if ( info.isReadable() && info.isWritable() )
            {
                buf.append( NEWLINE  ); // extra blank line before read/write attribute
            }

            if (info.isReadable())
            {
                if (mEmitComments)
                {
                    comment = getGetterComment(info, javaName);
                    if (comment.length() != 0)
                    {
                        buf.append(indent(comment) + NEWLINE);
                    }
                }
                buf.append(indent(formMethod(type, "get" + javaName, null, null)));
				buf.append( NEWLINE );
            }

            if (info.isWritable())
            {
                if (mEmitComments)
                {
                    comment = getSetterComment(info, javaName);
                    if (comment.length() != 0)
                    {
                        buf.append(indent(comment) + NEWLINE);
                    }
                }

                typeTemp[ 0] = type;
                nameTemp[ 0] = "value";

                buf.append(indent(formMethod("void", "set" + javaName, typeTemp, nameTemp)));
                buf.append( NEWLINE  );
            }
        }

        return (buf.toString());
    }

    protected String generateOperations(MBeanOperationInfo[] infos)
    {
        final StringBuffer buf = new StringBuffer();

        for (int i = 0; i < infos.length; ++i)
        {
            final MBeanOperationInfo info = infos[i];
            final String name = info.getName();
            final String returnType = info.getReturnType();
            final MBeanParameterInfo[] paramInfos = info.getSignature();

            final String[] paramTypes = new String[paramInfos.length];
            for (int p = 0; p < paramInfos.length; ++p)
            {
                paramTypes[p] = paramInfos[p].getType();
            }

            final String[] paramNames = getParamNames(info);

            if (mEmitComments)
            {
                final String comment = getOperationComment(info, paramNames);
                if (comment.length() != 0)
                {
                    buf.append(NEWLINE + indent(comment) + NEWLINE);
                }
            }

            final String method = formMethod(returnType, name, paramTypes, paramNames);
            buf.append(indent(method) + NEWLINE);
        }

        return (buf.toString());
    }

    protected boolean isBoilerplateDescription(final String description)
    {
        if (description == null)
        {
            return true;
        }

        final String trimmed = description.trim();
        return trimmed.length() == 0 ||
               trimmed.indexOf("Attribute exposed for management") >= 0 ||
               trimmed.indexOf("Operation exposed for management") >= 0 ||
               trimmed.indexOf("No Description was available") >= 0 ||
               trimmed.equals("n/a");
    }

    public String[] getParamNames(MBeanOperationInfo info)
    {
        final MBeanParameterInfo[] params = info.getSignature();

        final String[] names = new String[params.length];

        for (int i = 0; i < params.length; ++i)
        {
            names[i] = params[i].getName();
        }

        return (names);
    }

    public String getGetterComment(MBeanAttributeInfo info, String actualName)
    {
        String description = info.getDescription();
        if (isBoilerplateDescription(description))
        {
            description = "";
        }

        final String nameComment = getAttributeNameComment(info.getName(), actualName);

        String result = description;
        if ( description.length() != 0 )
        {
            result = result + NEWLINE;
        }
        result = result + toString(info.getDescriptor());

        if (nameComment.length() != 0)
        {
            result = result + nameComment;
        }

        result = makeJavadocComment(result);

        return (result);
    }

    public String getSetterComment(MBeanAttributeInfo info, String actualName)
    {
        return "";
    }

    private static String nvp(final String name, final Object value)
    {
        return name + " = " + SmartStringifier.DEFAULT.stringify(value);
    }

    public static String toString(final Descriptor d)
    {
        final String NL = NEWLINE;
        final StringBuffer buf = new StringBuffer();
        if (d != null && d.getFieldNames().length != 0)
        {
            //buf.append( idt(indent) + "Descriptor  = " + NL );
            for (final String fieldName : d.getFieldNames())
            {
                buf.append(nvp(fieldName, d.getFieldValue(fieldName)) + NL);
            }
        }
        else
        {
            //buf.append( idt(indent) + "Descriptor = n/a" + NL );
        }

        return buf.toString();
    }

    public String getOperationComment(MBeanOperationInfo info, final String[] paramNames)
    {
        final String description = info.getDescription();

        final StringBuffer buf = new StringBuffer();

        if (description != null && description.length() != 0 )
        {
            buf.append(description + NEWLINE);
        }

        final Descriptor desc = info.getDescriptor();
        buf.append(toString(desc));

        final MBeanParameterInfo[] signature = info.getSignature();
        for (int i = 0; i < paramNames.length; ++i)
        {
            final String paramDescription = signature[i].getDescription();

            buf.append("@param " + paramNames[i] + " " + paramDescription + NEWLINE);
        }

        final String returnType = getCodeClassname(info.getReturnType());
        if (!returnType.equals("void"))
        {
            buf.append("@return " + returnType + NEWLINE);
        }

        return (makeJavadocComment(buf.toString()));
    }

    public String getHeaderComment(final MBeanInfo info)
    {
        return makeJavadocComment("");
    }

    public String getInterfaceComment(final MBeanInfo info)
    {
        final StringBuilder buf = new StringBuilder();
        
        buf.append( "Implementing class: " + info.getClassName() + NEWLINE);
        
        buf.append( NEWLINE + "Descriptor: " + NEWLINE);
        buf.append( toString(info.getDescriptor()) );
        
        final MBeanNotificationInfo[] notifs = info.getNotifications();
        if ( notifs != null && notifs.length != 0 )
        {
            buf.append( NEWLINE + "MBeanNotificationInfo: " + NEWLINE);
            for( final MBeanNotificationInfo notifInfo : notifs )
            {
                buf.append( MBeanNotificationInfoStringifier.toString(notifInfo) + NEWLINE );
            }
        }

        final String comment = buf.toString();
        
        return makeJavadocComment(comment);
    }

    public String getPackageName(final MBeanInfo info)
    {
        return ("mbeans.generated");
    }

    public String getClassname(final MBeanInfo info)
    {
        String name = info.getClassName();
        name = name.replace(".", "_");
        name = name.replace("$", "_");

        return name + "_Proxy";
    }

    public String getExceptions(final MBeanOperationInfo info)
    {
        return ("");
    }

}






