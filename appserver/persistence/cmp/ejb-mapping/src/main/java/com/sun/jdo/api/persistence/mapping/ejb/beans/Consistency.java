/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdo.api.persistence.mapping.ejb.beans;

import org.w3c.dom.*;
import org.netbeans.modules.schema2beans.*;
import java.beans.*;
import java.util.*;

// BEGIN_NOI18N

public class Consistency extends org.netbeans.modules.schema2beans.BaseBean
{

	static Vector comparators = new Vector();
	private static final org.netbeans.modules.schema2beans.Version runtimeVersion = new org.netbeans.modules.schema2beans.Version(5, 0, 0);

	static public final String NONE = "None";	// NOI18N
	static public final String CHECK_MODIFIED_AT_COMMIT = "CheckModifiedAtCommit";	// NOI18N
	static public final String LOCK_WHEN_LOADED = "LockWhenLoaded";	// NOI18N
	static public final String CHECK_ALL_AT_COMMIT = "CheckAllAtCommit";	// NOI18N
	static public final String LOCK_WHEN_MODIFIED = "LockWhenModified";	// NOI18N
	static public final String CHECK_ALL_AT_COMMIT2 = "CheckAllAtCommit2";	// NOI18N
	static public final String CHECK_VERSION_OF_ACCESSED_INSTANCES = "CheckVersionOfAccessedInstances";	// NOI18N

	public Consistency() {
		this(Common.USE_DEFAULT_VALUES);
	}

	public Consistency(int options)
	{
		super(comparators, runtimeVersion);
		// Properties (see root bean comments for the bean graph)
		initPropertyTables(7);
		this.createProperty("none", 	// NOI18N
			NONE, Common.SEQUENCE_OR | 
			Common.TYPE_0_1 | Common.TYPE_BOOLEAN | Common.TYPE_KEY, 
			Boolean.class);
		this.createProperty("check-modified-at-commit", 	// NOI18N
			CHECK_MODIFIED_AT_COMMIT, Common.SEQUENCE_OR | 
			Common.TYPE_0_1 | Common.TYPE_BOOLEAN | Common.TYPE_KEY, 
			Boolean.class);
		this.createProperty("lock-when-loaded", 	// NOI18N
			LOCK_WHEN_LOADED, Common.SEQUENCE_OR | 
			Common.TYPE_0_1 | Common.TYPE_BOOLEAN | Common.TYPE_KEY, 
			Boolean.class);
		this.createProperty("check-all-at-commit", 	// NOI18N
			CHECK_ALL_AT_COMMIT, Common.SEQUENCE_OR | 
			Common.TYPE_0_1 | Common.TYPE_BOOLEAN | Common.TYPE_KEY, 
			Boolean.class);
		this.createProperty("lock-when-modified", 	// NOI18N
			LOCK_WHEN_MODIFIED, Common.SEQUENCE_OR | 
			Common.TYPE_0_1 | Common.TYPE_BOOLEAN | Common.TYPE_KEY, 
			Boolean.class);
		this.createProperty("check-all-at-commit", 	// NOI18N
			CHECK_ALL_AT_COMMIT2, Common.SEQUENCE_OR | 
			Common.TYPE_0_1 | Common.TYPE_BOOLEAN | Common.TYPE_KEY, 
			Boolean.class);
		this.createProperty("check-version-of-accessed-instances", 	// NOI18N
			CHECK_VERSION_OF_ACCESSED_INSTANCES, Common.SEQUENCE_OR | 
			Common.TYPE_1 | Common.TYPE_BEAN | Common.TYPE_KEY, 
			CheckVersionOfAccessedInstances.class);
		this.initialize(options);
	}

	// Setting the default values of the properties
	void initialize(int options) {

	}

	// This attribute is mandatory
	public void setNone(boolean value) {
		this.setValue(NONE, (value ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
		if (value != false) {
			// It's a mutually exclusive property.
			setCheckModifiedAtCommit(false);
			setLockWhenLoaded(false);
			setCheckAllAtCommit(false);
			setLockWhenModified(false);
			setCheckAllAtCommit2(false);
			setCheckVersionOfAccessedInstances(null);
		}
	}

	//
	public boolean isNone() {
		Boolean ret = (Boolean)this.getValue(NONE);
		if (ret == null)
			ret = (Boolean)Common.defaultScalarValue(Common.TYPE_BOOLEAN);
		return ((java.lang.Boolean)ret).booleanValue();
	}

	// This attribute is mandatory
	public void setCheckModifiedAtCommit(boolean value) {
		this.setValue(CHECK_MODIFIED_AT_COMMIT, (value ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
		if (value != false) {
			// It's a mutually exclusive property.
			setNone(false);
			setLockWhenLoaded(false);
			setCheckAllAtCommit(false);
			setLockWhenModified(false);
			setCheckAllAtCommit2(false);
			setCheckVersionOfAccessedInstances(null);
		}
	}

	//
	public boolean isCheckModifiedAtCommit() {
		Boolean ret = (Boolean)this.getValue(CHECK_MODIFIED_AT_COMMIT);
		if (ret == null)
			ret = (Boolean)Common.defaultScalarValue(Common.TYPE_BOOLEAN);
		return ((java.lang.Boolean)ret).booleanValue();
	}

	// This attribute is mandatory
	public void setLockWhenLoaded(boolean value) {
		this.setValue(LOCK_WHEN_LOADED, (value ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
		if (value != false) {
			// It's a mutually exclusive property.
			setNone(false);
			setCheckModifiedAtCommit(false);
			setCheckAllAtCommit(false);
			setLockWhenModified(false);
			setCheckAllAtCommit2(false);
			setCheckVersionOfAccessedInstances(null);
		}
	}

	//
	public boolean isLockWhenLoaded() {
		Boolean ret = (Boolean)this.getValue(LOCK_WHEN_LOADED);
		if (ret == null)
			ret = (Boolean)Common.defaultScalarValue(Common.TYPE_BOOLEAN);
		return ((java.lang.Boolean)ret).booleanValue();
	}

	// This attribute is mandatory
	public void setCheckAllAtCommit(boolean value) {
		this.setValue(CHECK_ALL_AT_COMMIT, (value ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
		if (value != false) {
			// It's a mutually exclusive property.
			setNone(false);
			setCheckModifiedAtCommit(false);
			setLockWhenLoaded(false);
			setLockWhenModified(false);
			setCheckAllAtCommit2(false);
			setCheckVersionOfAccessedInstances(null);
		}
	}

	//
	public boolean isCheckAllAtCommit() {
		Boolean ret = (Boolean)this.getValue(CHECK_ALL_AT_COMMIT);
		if (ret == null)
			ret = (Boolean)Common.defaultScalarValue(Common.TYPE_BOOLEAN);
		return ((java.lang.Boolean)ret).booleanValue();
	}

	// This attribute is mandatory
	public void setLockWhenModified(boolean value) {
		this.setValue(LOCK_WHEN_MODIFIED, (value ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
		if (value != false) {
			// It's a mutually exclusive property.
			setNone(false);
			setCheckModifiedAtCommit(false);
			setLockWhenLoaded(false);
			setCheckAllAtCommit(false);
			setCheckVersionOfAccessedInstances(null);
		}
	}

	//
	public boolean isLockWhenModified() {
		Boolean ret = (Boolean)this.getValue(LOCK_WHEN_MODIFIED);
		if (ret == null)
			ret = (Boolean)Common.defaultScalarValue(Common.TYPE_BOOLEAN);
		return ((java.lang.Boolean)ret).booleanValue();
	}

	// This attribute is optional
	public void setCheckAllAtCommit2(boolean value) {
		this.setValue(CHECK_ALL_AT_COMMIT2, (value ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE));
		if (value != false) {
			// It's a mutually exclusive property.
			setNone(false);
			setCheckModifiedAtCommit(false);
			setLockWhenLoaded(false);
			setCheckAllAtCommit(false);
			setCheckVersionOfAccessedInstances(null);
		}
	}

	//
	public boolean isCheckAllAtCommit2() {
		Boolean ret = (Boolean)this.getValue(CHECK_ALL_AT_COMMIT2);
		if (ret == null)
			ret = (Boolean)Common.defaultScalarValue(Common.TYPE_BOOLEAN);
		return ((java.lang.Boolean)ret).booleanValue();
	}

	// This attribute is mandatory
	public void setCheckVersionOfAccessedInstances(CheckVersionOfAccessedInstances value) {
		this.setValue(CHECK_VERSION_OF_ACCESSED_INSTANCES, value);
		if (value != null) {
			// It's a mutually exclusive property.
			setNone(false);
			setCheckModifiedAtCommit(false);
			setLockWhenLoaded(false);
			setCheckAllAtCommit(false);
			setLockWhenModified(false);
			setCheckAllAtCommit2(false);
		}
	}

	//
	public CheckVersionOfAccessedInstances getCheckVersionOfAccessedInstances() {
		return (CheckVersionOfAccessedInstances)this.getValue(CHECK_VERSION_OF_ACCESSED_INSTANCES);
	}

	/**
	 * Create a new bean using it's default constructor.
	 * This does not add it to any bean graph.
	 */
	public CheckVersionOfAccessedInstances newCheckVersionOfAccessedInstances() {
		return new CheckVersionOfAccessedInstances();
	}

	//
	public static void addComparator(org.netbeans.modules.schema2beans.BeanComparator c) {
		comparators.add(c);
	}

	//
	public static void removeComparator(org.netbeans.modules.schema2beans.BeanComparator c) {
		comparators.remove(c);
	}
	public void validate() throws org.netbeans.modules.schema2beans.ValidateException {
		boolean restrictionFailure = false;
		boolean restrictionPassed = false;
		// Validating property none
		if (isNone() != false) {
			if (isCheckModifiedAtCommit() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: None and CheckModifiedAtCommit", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckModifiedAtCommit", this);	// NOI18N
			}
			if (isLockWhenLoaded() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: None and LockWhenLoaded", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "LockWhenLoaded", this);	// NOI18N
			}
			if (isCheckAllAtCommit() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: None and CheckAllAtCommit", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckAllAtCommit", this);	// NOI18N
			}
			if (isLockWhenModified() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: None and LockWhenModified", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "LockWhenModified", this);	// NOI18N
			}
			if (isCheckAllAtCommit2() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: None and CheckAllAtCommit2", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckAllAtCommit2", this);	// NOI18N
			}
			if (getCheckVersionOfAccessedInstances() != null) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: None and CheckVersionOfAccessedInstances", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckVersionOfAccessedInstances", this);	// NOI18N
			}
		}
		// Validating property checkModifiedAtCommit
		if (isCheckModifiedAtCommit() != false) {
			if (isNone() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckModifiedAtCommit and None", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "None", this);	// NOI18N
			}
			if (isLockWhenLoaded() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckModifiedAtCommit and LockWhenLoaded", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "LockWhenLoaded", this);	// NOI18N
			}
			if (isCheckAllAtCommit() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckModifiedAtCommit and CheckAllAtCommit", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckAllAtCommit", this);	// NOI18N
			}
			if (isLockWhenModified() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckModifiedAtCommit and LockWhenModified", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "LockWhenModified", this);	// NOI18N
			}
			if (isCheckAllAtCommit2() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckModifiedAtCommit and CheckAllAtCommit2", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckAllAtCommit2", this);	// NOI18N
			}
			if (getCheckVersionOfAccessedInstances() != null) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckModifiedAtCommit and CheckVersionOfAccessedInstances", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckVersionOfAccessedInstances", this);	// NOI18N
			}
		}
		// Validating property lockWhenLoaded
		if (isLockWhenLoaded() != false) {
			if (isNone() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: LockWhenLoaded and None", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "None", this);	// NOI18N
			}
			if (isCheckModifiedAtCommit() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: LockWhenLoaded and CheckModifiedAtCommit", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckModifiedAtCommit", this);	// NOI18N
			}
			if (isCheckAllAtCommit() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: LockWhenLoaded and CheckAllAtCommit", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckAllAtCommit", this);	// NOI18N
			}
			if (isLockWhenModified() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: LockWhenLoaded and LockWhenModified", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "LockWhenModified", this);	// NOI18N
			}
			if (isCheckAllAtCommit2() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: LockWhenLoaded and CheckAllAtCommit2", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckAllAtCommit2", this);	// NOI18N
			}
			if (getCheckVersionOfAccessedInstances() != null) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: LockWhenLoaded and CheckVersionOfAccessedInstances", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckVersionOfAccessedInstances", this);	// NOI18N
			}
		}
		// Validating property checkAllAtCommit
		if (isCheckAllAtCommit() != false) {
			if (isNone() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckAllAtCommit and None", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "None", this);	// NOI18N
			}
			if (isCheckModifiedAtCommit() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckAllAtCommit and CheckModifiedAtCommit", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckModifiedAtCommit", this);	// NOI18N
			}
			if (isLockWhenLoaded() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckAllAtCommit and LockWhenLoaded", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "LockWhenLoaded", this);	// NOI18N
			}
			if (isLockWhenModified() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckAllAtCommit and LockWhenModified", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "LockWhenModified", this);	// NOI18N
			}
			if (isCheckAllAtCommit2() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckAllAtCommit and CheckAllAtCommit2", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckAllAtCommit2", this);	// NOI18N
			}
			if (getCheckVersionOfAccessedInstances() != null) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckAllAtCommit and CheckVersionOfAccessedInstances", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckVersionOfAccessedInstances", this);	// NOI18N
			}
		}
		// Validating property lockWhenModified
		if (isLockWhenModified() != false) {
			if (isNone() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: LockWhenModified and None", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "None", this);	// NOI18N
			}
			if (isCheckModifiedAtCommit() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: LockWhenModified and CheckModifiedAtCommit", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckModifiedAtCommit", this);	// NOI18N
			}
			if (isLockWhenLoaded() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: LockWhenModified and LockWhenLoaded", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "LockWhenLoaded", this);	// NOI18N
			}
			if (isCheckAllAtCommit() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: LockWhenModified and CheckAllAtCommit", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckAllAtCommit", this);	// NOI18N
			}
			if (getCheckVersionOfAccessedInstances() != null) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: LockWhenModified and CheckVersionOfAccessedInstances", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckVersionOfAccessedInstances", this);	// NOI18N
			}
		}
		// Validating property checkAllAtCommit2
		if (isCheckAllAtCommit2() != false) {
			if (isNone() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckAllAtCommit2 and None", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "None", this);	// NOI18N
			}
			if (isCheckModifiedAtCommit() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckAllAtCommit2 and CheckModifiedAtCommit", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckModifiedAtCommit", this);	// NOI18N
			}
			if (isLockWhenLoaded() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckAllAtCommit2 and LockWhenLoaded", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "LockWhenLoaded", this);	// NOI18N
			}
			if (isCheckAllAtCommit() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckAllAtCommit2 and CheckAllAtCommit", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckAllAtCommit", this);	// NOI18N
			}
			if (getCheckVersionOfAccessedInstances() != null) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckAllAtCommit2 and CheckVersionOfAccessedInstances", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckVersionOfAccessedInstances", this);	// NOI18N
			}
		}
		// Validating property checkVersionOfAccessedInstances
		if (getCheckVersionOfAccessedInstances() != null) {
			getCheckVersionOfAccessedInstances().validate();
		}
		if (getCheckVersionOfAccessedInstances() != null) {
			if (isNone() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckVersionOfAccessedInstances and None", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "None", this);	// NOI18N
			}
			if (isCheckModifiedAtCommit() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckVersionOfAccessedInstances and CheckModifiedAtCommit", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckModifiedAtCommit", this);	// NOI18N
			}
			if (isLockWhenLoaded() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckVersionOfAccessedInstances and LockWhenLoaded", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "LockWhenLoaded", this);	// NOI18N
			}
			if (isCheckAllAtCommit() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckVersionOfAccessedInstances and CheckAllAtCommit", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckAllAtCommit", this);	// NOI18N
			}
			if (isLockWhenModified() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckVersionOfAccessedInstances and LockWhenModified", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "LockWhenModified", this);	// NOI18N
			}
			if (isCheckAllAtCommit2() != false) {
				throw new org.netbeans.modules.schema2beans.ValidateException("mutually exclusive properties: CheckVersionOfAccessedInstances and CheckAllAtCommit2", org.netbeans.modules.schema2beans.ValidateException.FailureType.MUTUALLY_EXCLUSIVE, "CheckAllAtCommit2", this);	// NOI18N
			}
		}
		if (getCheckVersionOfAccessedInstances() == null && isCheckAllAtCommit() == false && isCheckAllAtCommit2() == false && isCheckModifiedAtCommit() == false && isLockWhenLoaded() == false && isNone() == false) {
			throw new org.netbeans.modules.schema2beans.ValidateException("required properties: getCheckVersionOfAccessedInstances() == null && isCheckAllAtCommit() == false && isCheckAllAtCommit2() == false && isCheckModifiedAtCommit() == false && isLockWhenLoaded() == false && isNone() == false", org.netbeans.modules.schema2beans.ValidateException.FailureType.NULL_VALUE, "CheckAllAtCommit2", this);	// NOI18N
		}
		if (getCheckVersionOfAccessedInstances() == null && isCheckAllAtCommit() == false && isCheckModifiedAtCommit() == false && isLockWhenLoaded() == false && isLockWhenModified() == false && isNone() == false) {
			throw new org.netbeans.modules.schema2beans.ValidateException("required properties: getCheckVersionOfAccessedInstances() == null && isCheckAllAtCommit() == false && isCheckModifiedAtCommit() == false && isLockWhenLoaded() == false && isLockWhenModified() == false && isNone() == false", org.netbeans.modules.schema2beans.ValidateException.FailureType.NULL_VALUE, "LockWhenModified", this);	// NOI18N
		}
		if (getCheckVersionOfAccessedInstances() == null && isCheckAllAtCommit() == false && isCheckAllAtCommit2() == false && isCheckModifiedAtCommit() == false && isLockWhenLoaded() == false && isLockWhenModified() == false && isNone() == false) {
			throw new org.netbeans.modules.schema2beans.ValidateException("required properties: getCheckVersionOfAccessedInstances() == null && isCheckAllAtCommit() == false && isCheckAllAtCommit2() == false && isCheckModifiedAtCommit() == false && isLockWhenLoaded() == false && isLockWhenModified() == false && isNone() == false", org.netbeans.modules.schema2beans.ValidateException.FailureType.NULL_VALUE, "CheckVersionOfAccessedInstances", this);	// NOI18N
		}
	}

	// Dump the content of this bean returning it as a String
	public void dump(StringBuffer str, String indent){
		String s;
		Object o;
		org.netbeans.modules.schema2beans.BaseBean n;
		str.append(indent);
		str.append("None");	// NOI18N
		str.append(indent+"\t");	// NOI18N
		str.append((this.isNone()?"true":"false"));
		this.dumpAttributes(NONE, 0, str, indent);

		str.append(indent);
		str.append("CheckModifiedAtCommit");	// NOI18N
		str.append(indent+"\t");	// NOI18N
		str.append((this.isCheckModifiedAtCommit()?"true":"false"));
		this.dumpAttributes(CHECK_MODIFIED_AT_COMMIT, 0, str, indent);

		str.append(indent);
		str.append("LockWhenLoaded");	// NOI18N
		str.append(indent+"\t");	// NOI18N
		str.append((this.isLockWhenLoaded()?"true":"false"));
		this.dumpAttributes(LOCK_WHEN_LOADED, 0, str, indent);

		str.append(indent);
		str.append("CheckAllAtCommit");	// NOI18N
		str.append(indent+"\t");	// NOI18N
		str.append((this.isCheckAllAtCommit()?"true":"false"));
		this.dumpAttributes(CHECK_ALL_AT_COMMIT, 0, str, indent);

		str.append(indent);
		str.append("LockWhenModified");	// NOI18N
		str.append(indent+"\t");	// NOI18N
		str.append((this.isLockWhenModified()?"true":"false"));
		this.dumpAttributes(LOCK_WHEN_MODIFIED, 0, str, indent);

		str.append(indent);
		str.append("CheckAllAtCommit2");	// NOI18N
		str.append(indent+"\t");	// NOI18N
		str.append((this.isCheckAllAtCommit2()?"true":"false"));
		this.dumpAttributes(CHECK_ALL_AT_COMMIT2, 0, str, indent);

		str.append(indent);
		str.append("CheckVersionOfAccessedInstances");	// NOI18N
		n = (org.netbeans.modules.schema2beans.BaseBean) this.getCheckVersionOfAccessedInstances();
		if (n != null)
			n.dump(str, indent + "\t");	// NOI18N
		else
			str.append(indent+"\tnull");	// NOI18N
		this.dumpAttributes(CHECK_VERSION_OF_ACCESSED_INSTANCES, 0, str, indent);

	}
	public String dumpBeanNode(){
		StringBuffer str = new StringBuffer();
		str.append("Consistency\n");	// NOI18N
		this.dump(str, "\n  ");	// NOI18N
		return str.toString();
	}}

// END_NOI18N


/*
		The following schema file has been used for generation:

<!--
  XML DTD for Sun ONE Application Server specific Object Relational Mapping 
  with Container Managed Persistence.
-->

<!--

This sun-cmp-mapping_1_2.dtd has a workaround for an unfiled schema2beans bug
which prevents us from having the DTD specify the sub-elements in column-pair as
it really should be.  This issue is fixed in schema2beans shipped with NB > 3.5,
but we are currently using schema2beans from NB 3.5, and so must use this
workaround.

Because of the workaround, the file here differs from the official one in
appserv-commons/lib/dtds (which also has previous versions of sun-cmp-mapping 
dtds) in the definition of the column pair element.  This difference is so 
that schema2beans can produce usable beans.  The official dtd has:

    <!ELEMENT column-pair (column-name, column-name) >

and the one in here has:

    <!ELEMENT column-pair (column-name+) >

-->

<!-- This file maps at least one set of beans to tables and columns in a 
     specific db schema
-->
<!ELEMENT sun-cmp-mappings ( sun-cmp-mapping+ ) >

<!-- At least one bean is mapped to database columns in the named schema -->
<!ELEMENT sun-cmp-mapping ( schema, entity-mapping+) >

<!-- A cmp bean has a name, a primary table, one or more fields, zero or 
     more relationships, and zero or more secondary tables, plus flags for 
     consistency checking.
 
     If the consistency checking flag element is not present, then none 
     is assumed 
--> 
<!ELEMENT entity-mapping (ejb-name, table-name, cmp-field-mapping+, 
        cmr-field-mapping*, secondary-table*, consistency?)>

<!ELEMENT consistency (none | check-modified-at-commit | lock-when-loaded |
        check-all-at-commit | (lock-when-modified, check-all-at-commit?) |
        check-version-of-accessed-instances) >

<!ELEMENT read-only EMPTY>

<!-- A cmp-field-mapping has a field, one or more columns that it maps to.  
     The column can be from a bean's primary table or any defined secondary 
     table.  If a field is mapped to multiple columns, the column listed first
     is used as the SOURCE for getting the value from the database.  The 
     columns are updated in their order.  A field may also be marked as 
     read-only.  It may also participate in a hierarchial or independent 
     fetch group. If the fetched-with element is not present, the value,
          <fetched-with><none/></fetched-with>
     is assumed.
-->
<!ELEMENT cmp-field-mapping (field-name, column-name+, read-only?, 
        fetched-with?) >
            
<!-- The java identifier of a field. Must match the value of the field-name 
     sub-element of the cmp-field that is being mapped. 
-->
<!ELEMENT field-name (#PCDATA) >

<!-- The java identifier of a field.  Must match the value of the 
     cmr-field-name sub-element of the cmr-field tat is being mapped. 
-->
<!ELEMENT cmr-field-name (#PCDATA) >

<!-- The ejb-name from the standard EJB-jar DTD--> 
<!ELEMENT ejb-name (#PCDATA) >

<!-- The COLUMN name of a column from the primary table, or the table 
     qualified name (TABLE.COLUMN) of a column from a secondary or related 
     table
--> 
<!ELEMENT column-name (#PCDATA) >

<!-- Holds the fetch group configuration for fields and relationships -->
<!ELEMENT fetched-with (default | level | named-group | none) >

<!-- Sub element of fetched-with. Implies that a field belongs to the default 
     hierarchical fetch group. -->
<!ELEMENT default EMPTY>

<!-- A hierarchial fetch group.  The value of this element must be an integer.
     Fields and relationships that belong to a hierachial fetch group of equal
     (or lesser) value are fetched at the same time. The value of level must
     be greater than zero.
-->
<!ELEMENT level (#PCDATA) >

<!-- The name of an independent fetch group.  All the fields and relationships 
  that are part of a named-group are fetched at the same time-->
<!ELEMENT named-group (#PCDATA) >

<!-- The name of a database table -->
<!ELEMENT table-name (#PCDATA) >

<!-- a bean's secondary tables -->
<!ELEMENT secondary-table (table-name, column-pair+) >

<!-- the pair of columns -->
<!ELEMENT column-pair (column-name+) >

<!-- cmr-field mapping.  A cmr field has a name and one or more column 
     pairs that define the relationship. The relationship can also 
     participate in a fetch group.
     
     If the fetched-with element is not present, the value,
          <fetched-with><none/></fetched-with>
     is assumed. 
-->
<!ELEMENT cmr-field-mapping (cmr-field-name, column-pair+, fetched-with? ) >

<!-- The path name to the schema file--> 
<!ELEMENT schema (#PCDATA) >

<!-- flag elements for consistency levels -->

<!-- note: none is also a sub-element of the fetched-with tag -->
<!ELEMENT none EMPTY >
<!ELEMENT check-modified-at-commit EMPTY >
<!ELEMENT check-all-at-commit EMPTY>
<!ELEMENT lock-when-modified EMPTY>
<!ELEMENT lock-when-loaded EMPTY >
<!ELEMENT check-version-of-accessed-instances (column-name+) >

*/
