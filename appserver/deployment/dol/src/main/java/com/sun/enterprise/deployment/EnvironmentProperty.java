/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

 package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.runtime.application.wls.ApplicationParam;
import com.sun.enterprise.deployment.web.ContextParameter;
import com.sun.enterprise.deployment.web.EnvironmentEntry;
import com.sun.enterprise.deployment.web.InitializationParameter;
import com.sun.enterprise.deployment.web.WebDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.internal.api.RelativePathResolver;

import java.util.HashSet;
import java.util.Set;

    /** 
    ** The EnvironmentProperty class hold the data about a single environment entry for J2EE components.
    ** @author Danny Coward 
    */
 
public class EnvironmentProperty extends Descriptor implements InitializationParameter, ContextParameter, ApplicationParam, WebDescriptor, EnvironmentEntry, InjectionCapable {
    private String value; 
    private String type;
    private Object valueObject;
    private boolean setValueCalled = false;

    // list of injection targes
    private Set<InjectionTarget> injectionTargets;

    private static Class[] allowedTypes = {
                                        int.class,
                                        boolean.class,
                                        double.class,
                                        float.class,
                                        long.class,
                                        short.class,
                                        byte.class,
                                        char.class,
					java.lang.String.class,
					java.lang.Boolean.class,
					java.lang.Integer.class,
					java.lang.Double.class,
					java.lang.Byte.class,
					java.lang.Short.class,
					java.lang.Long.class,
					java.lang.Float.class,
                                        java.lang.Character.class,
                                        java.lang.Class.class
					    };
    static LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(EnvironmentProperty.class);

    protected String mappedName;

    protected String lookupName;

    /** 
    ** copy constructor.
    */

    public EnvironmentProperty(EnvironmentProperty other) {
	super(other);
	value = other.value;
	type = other.type;
	valueObject = other.valueObject;
    }  
				    
    /** 
    ** Construct an environment property if type String and empty string value and no description.
    */

    public EnvironmentProperty() {
    }  
    
     /** 
    ** Construct an environment property of given name value and description. 
    */
    
    public EnvironmentProperty(String name, String value, String description) {
	this(name, value, description, null);
    }  
    
    /** 
    ** Construct an environment property of given name value and description and type.
    ** Throws an IllegalArgumentException if bounds checking is true and the value cannot be
    ** reconciled with the given type. 
    */ 
    
    public EnvironmentProperty(String name, String value, String description, String type) {
	super(name, description);
	this.value = value;
	checkType(type);
	this.type = type;
    } 
    
    /** 
    ** Returns the String value of this environment property 
    */
    public String getValue() {
	if (this.value == null) {
	    this.value = "";
	}
	return value;
    }
    
    /**
     * Returns a resolved value of this environment property
     */
    public String getResolvedValue() {
    	return RelativePathResolver.resolvePath(getValue());
    }
    
    /** 
     ** Returns the typed value object of this environment property. Throws an IllegalArgumentException if bounds checking is 
     ** true and the value cannot be
     ** reconciled with the given type. 
     */
     public Object getResolvedValueObject() {
 	if (this.valueObject == null) {
 	    this.valueObject = "";
 	}
 	return getObjectFromString(this.getResolvedValue(), this.getValueType()); 
     }
    
    /** 
    ** checks the given class type. throws an IllegalArgumentException if bounds checking
    ** if the type is not allowed.
    */
    
    private void checkType(String type) {
	if (type != null) {
	    Class typeClass = null;
	    // is it loadable ?
	    try {
		typeClass = Class.forName(type, true,
                    Thread.currentThread().getContextClassLoader());
	    } catch (Throwable t) {
		if (this.isBoundsChecking()) {
		    throw new IllegalArgumentException(localStrings.getLocalString(
										   "enterprise.deployment.exceptiontypenotallowedpropertytype",
										   "{0} is not an allowed property value type", new Object[] {type}));
		} else {
		    return;
		}
	    }
	    boolean allowedType = false;
	    for (int i = 0; i < allowedTypes.length; i++) {
		if (allowedTypes[i].equals(typeClass)) {
		    allowedType = true;
		    break;
		}
	    }
            if (typeClass != null && typeClass.isEnum()) {
                allowedType = true;
            }

	    if (this.isBoundsChecking() && !allowedType) {
		throw new IllegalArgumentException(localStrings.getLocalString(
										   "enterprise.deployment.exceptiontypenotallowedprprtytype",
										   "{0} is not an allowed property value type", new Object[] {type}));
	    }
	}
    }
    
    /** 
    ** Returns the typed value object of this environment property. Throws an IllegalArgumentException if bounds checking is 
    ** true and the value cannot be
    ** reconciled with the given type. 
    */
    public Object getValueObject() {
	if (this.valueObject == null) {
	    this.valueObject = "";
	}
	return getObjectFromString(this.getValue(), this.getValueType()); 
    }
    
    /** 
    ** Returns value type of this environment property. 
    */
    
    public Class getValueType() {
	if (this.type == null) {
	    return String.class;
	} else {
	    try {
		return Class.forName(this.type, true,
                    Thread.currentThread().getContextClassLoader());
	    } catch (Throwable t) {
		return null;
	    }
	}
    }
    
     /** 
    ** Returns value type of this environment property. Throws Illegal argument exception if this is not an
    ** allowed type and bounds checking.
    */
    
    public void setType(String type) {
        checkType(type);
        this.type = type;
    }

    private String convertPrimitiveTypes(String type) {
        if (type == null) {
            return type;
        }

        if (type.equals("int")) {
            return "java.lang.Integer";
        } else if (type.equals("boolean")) {
            return "java.lang.Boolean";
        } else if (type.equals("double")) {
            return "java.lang.Double";
        } else if (type.equals("float")) {
            return "java.lang.Float";
        } else if (type.equals("long")) {
            return "java.lang.Long";
        } else if (type.equals("short")) {
            return "java.lang.Short";
        } else if (type.equals("byte")) {
            return "java.lang.Byte";
        } else if (type.equals("char")) {
            return "java.lang.Character";
        }
        return type;
    }

    
     /** 
    ** Returns value type of this environment property as a classname. 
    */
    
    public String getType() {
        if (type == null && this.isBoundsChecking()) {
            return String.class.getName();
        } else {
            type = convertPrimitiveTypes(type);
            return type;
        }
    }

    public void setMappedName(String mName) {
        mappedName = mName;
    }

    public String getMappedName() {
        return (mappedName != null)? mappedName : "";
    }

   public void setLookupName(String lName) {
        lookupName = lName;
    }

    public String getLookupName() {
        return (lookupName != null)? lookupName : "";
    }

    public boolean hasLookupName() {
        return (lookupName != null && lookupName.length() > 0);
    }
    
     /** 
    ** Sets the value of the environment property to the given string.
    */
    
    public void setValue(String value) {
	this.value = value;
        this.setValueCalled = true;

    }

    public boolean isSetValueCalled() {
        return setValueCalled;
    }

    public boolean hasAValue() {
        return ( setValueCalled || hasLookupName() || getMappedName().length() > 0);
    }

     /** 
    ** Returns true if the argument is an environment property of the same name, false else.
    */
    
    public boolean equals(Object other) {
	if (other instanceof EnvironmentProperty &&
	    this.getName().equals( ((EnvironmentProperty) other).getName() )) {
		return true;
	}
	return false;
    }
    
    /** 
    ** The hashCode of an environment property is the same as that of the name String.
    */
    public int hashCode() {
	return this.getName().hashCode();
    }
    
    /** 
    ** Returns a String representation of this environment property.
    */
    public void print(StringBuffer toStringBuffer) {
	toStringBuffer.append("Env-Prop: ").append(super.getName()).append("@");
        printInjectableResourceInfo(toStringBuffer);
        toStringBuffer.append("@").append(this.getType()).append("@").append(this.getValue()).append("@").append("@").append(super.getDescription());
    }
    
    private Object getObjectFromString(String string, Class type) {
        if (type == null && !this.isBoundsChecking()) {
            Object obj = getValueObjectUsingAllowedTypes(string);
            if (obj != null) return obj;
        }
	if (string == null || ("".equals(string) && !type.equals(String.class))) {
	    return null;
	}
	try {
            if (String.class.equals(type)) {
		return string;
            } else if (Boolean.class.equals(type)) {
		return Boolean.valueOf(string);
	    } else if (Integer.class.equals(type)) {
		return Integer.valueOf(string);
	    } else if (Double.class.equals(type)) {
		return new Double(string);
	    } else if (Float.class.equals(type)) {
		return new Float(string);
	    } else if (Short.class.equals(type)) {
		return Short.valueOf(string);
	    } else if (Byte.class.equals(type)) {
		return Byte.valueOf(string);
	    } else if (Long.class.equals(type)) {
		return Long.valueOf(string);
	    } else if (Character.class.equals(type)) {
                if (string.length() != 1) {
                    throw new IllegalArgumentException();
                } else {
                    return Character.valueOf(string.charAt(0));
                }
            } else if (Class.class.equals(type)) {
                return Class.forName(string, true,
                    Thread.currentThread().getContextClassLoader());
            } else if (type != null && type.isEnum()) {
                return Enum.valueOf(type, string);
            }
	} catch (Throwable t) {
	    throw new IllegalArgumentException(localStrings.getLocalString(
									   "enterprise.deployment.exceptioncouldnotcreateinstancetype",
									   "Could not create instance of {0} from {1}\n reason: {2}" + t, new Object[] {type, string, t}));
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
								       "enterprise.deployment.exceptionillegaltypeenvproperty",
								       "Illegal type for environment properties: {0}", new Object[] {type}));
    }
	

    private Object getValueObjectUsingAllowedTypes(String string) 
                                  throws IllegalArgumentException {
        if (this.type.equals(int.class.getName())) {
            return Integer.valueOf(string);
        } else if (this.type.equals(long.class.getName())) {
            return Long.valueOf(string);
        } else if (this.type.equals(short.class.getName())) {
            return Short.valueOf(string);
        } else if (this.type.equals(boolean.class.getName())) {
            return Boolean.valueOf(string);
        } else if (this.type.equals(float.class.getName())) {
            return new Float(string);
        } else if (this.type.equals(double.class.getName())) {
            return new Double(string);
        } else if (this.type.equals(byte.class.getName())) {
            return Byte.valueOf(string);
        } else if (this.type.equals(char.class.getName())) {
            if (string.length() != 1) {
                throw new IllegalArgumentException();
            } else {
                return Character.valueOf(string.charAt(0));
            }
        } 
        return null;
    }

    public boolean isConflict(EnvironmentProperty other) {
        return (getName().equals(other.getName())) &&
            (!(
                DOLUtils.equals(getType(), other.getType()) &&
                getValue().equals(other.getValue())
                ) ||
            isConflictResourceGroup(other));
    }

    protected boolean isConflictResourceGroup(EnvironmentProperty other) {
        return !(getLookupName().equals(other.getLookupName()) &&
                getMappedName().equals(other.getMappedName()));
    }

    //
    // InjectableResource implementation
    //
    public void addInjectionTarget(InjectionTarget target) {
        if (injectionTargets==null) {
            injectionTargets = new HashSet<InjectionTarget>();
        }
        boolean found = false;
        for (InjectionTarget injTarget : injectionTargets) {
            if (injTarget.equals(target)) {
                found = true;
                break;
            }
        }
        if (!found) {
            injectionTargets.add(target);
        }
    }
    
    public Set<InjectionTarget> getInjectionTargets() {
        return (injectionTargets != null) ? injectionTargets : new HashSet<InjectionTarget>();
    }

    public boolean isInjectable() {
        return (injectionTargets!=null && injectionTargets.size()>0);
        //return (getInjectTargetName() != null);
    }

    public boolean hasInjectionTargetFromXml() {
        boolean fromXml = false;
        if (injectionTargets != null) {
            for (InjectionTarget injTarget: injectionTargets) {
                fromXml = (MetadataSource.XML == injTarget.getMetadataSource());
                if (fromXml) {
                    break;
                }
            }
        }
        return fromXml;
    }

    public String getComponentEnvName() {
        return getName();
    }

    public String getInjectResourceType() {
        return type;
    }

    public void setInjectResourceType(String resourceType) {
        type = convertPrimitiveTypes(resourceType);
    }
    

    public StringBuffer printInjectableResourceInfo
        (StringBuffer toStringBuffer) {
        
        if( isInjectable() ) {
            for (InjectionTarget target : getInjectionTargets()) {
                if( target.isFieldInjectable() ) {
                    toStringBuffer.append("Field-Injectable Resource. Class name = ").
                            append(target.getClassName()).append(" Field name=").
                            append(target.getFieldName());
                } else {
                    toStringBuffer.append("Method-Injectable Resource. Class name =").
                            append(target.getClassName()).append(" Method =").                            
                            append(target.getMethodName());
                }
            }
        } else {
            toStringBuffer.append("Non-Injectable Resource");
        }

        return toStringBuffer;
    }
    
    // 
    // End InjectableResource implementation
    //
    
    
}
