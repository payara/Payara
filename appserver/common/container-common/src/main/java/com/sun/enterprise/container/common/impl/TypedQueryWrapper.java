/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.container.common.impl;


import javax.persistence.*;
import java.util.List;
import java.util.Date;
import java.util.Calendar;

/**
 * Wrapper class for javax.persistence.TypedQuery objects returned from
 * non-transactional access of a container-managed transactional
 * EntityManager.
 *
 * @see QueryWrapper for more details about why the wrapper is needed
 */
public class TypedQueryWrapper<X> extends QueryWrapper<TypedQuery <X> > implements TypedQuery<X> {


    public static <X> TypedQuery<X> createQueryWrapper(TypedQuery<X> queryDelegate, EntityManager emDelegate) {
        return new TypedQueryWrapper<X>(queryDelegate, emDelegate);
    }

    private TypedQueryWrapper(TypedQuery<X> qDelegate, EntityManager emDelegate) {
        super(qDelegate, emDelegate);
    }


    @SuppressWarnings("unchecked")
    @Override
    public List<X> getResultList() {
        // If this method is called, the current instance is guarantied to be of type TypedQuery<X>
        // It is safe to cast here.
        return (List<X>) super.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public X getSingleResult() {
        // If this method is called, the current instance is guarantied to be of type TypedQuery<X>
        // It is safe to cast here.
        return (X) super.getSingleResult();
    }

    @Override
    public TypedQuery<X> setMaxResults(int maxResults) {
        super.setMaxResults(maxResults);
        return this;
    }

    @Override
    public TypedQuery<X> setFirstResult(int startPosition) {
        super.setFirstResult(startPosition);
        return this;
    }

    @Override
    public TypedQuery<X> setHint(String hintName, Object value) {
        super.setHint(hintName, value);
        return this;
    }

    @Override
    public <T> TypedQuery<X> setParameter(Parameter<T> param, T value) {
        super.setParameter(param, value);
        return this;
    }

    @Override
    public TypedQuery<X> setParameter(Parameter<Date> param, Date value,  TemporalType temporalType) {
       super.setParameter(param, value, temporalType);
       return this;
   }

    @Override
    public TypedQuery<X> setParameter(Parameter<Calendar> param, Calendar value,  TemporalType temporalType) {
       super.setParameter(param, value, temporalType);
       return this;
   }

    @Override
    public TypedQuery<X> setParameter(String name, Object value) {
       super.setParameter(name, value);
       return this;
   }

    @Override
    public TypedQuery<X> setParameter(String name, Date value, TemporalType temporalType) {
       super.setParameter(name, value, temporalType);
       return this;
   }

    @Override
    public TypedQuery<X> setParameter(String name, Calendar value, TemporalType temporalType) {
       super.setParameter(name, value, temporalType);
       return this;
   }

    @Override
    public TypedQuery<X> setParameter(int position, Object value) {
       super.setParameter(position, value);
       return this;
   }

    @Override
    public TypedQuery<X> setParameter(int position, Date value,  TemporalType temporalType) {
       super.setParameter(position, value, temporalType);
       return this;
   }

    @Override
    public TypedQuery<X> setParameter(int position, Calendar value,  TemporalType temporalType) {
       super.setParameter(position, value, temporalType);
       return this;
   }

    @Override
    public <T> Parameter<T> getParameter(String name, Class<T> type) {
       return super.getParameter(name, type);
   }

    @Override
    public <T> Parameter<T> getParameter(int position, Class<T> type) {
       return super.getParameter(position, type);
   }

    @Override
    public TypedQuery<X> setFlushMode(FlushModeType flushMode) {
        super.setFlushMode(flushMode);
        return this;
    }

    @Override
    public TypedQuery<X> setLockMode(LockModeType lockModeType) {
        super.setLockMode(lockModeType);
        return this;
    }
}
