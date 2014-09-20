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

/*
 * QueryImpl.java
 *
 * Created on March 8, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;

import com.sun.jdo.api.persistence.support.Query;
import com.sun.jdo.api.persistence.support.Transaction;
import com.sun.jdo.api.persistence.support.JDOException;
import com.sun.jdo.api.persistence.support.JDOQueryException;
import com.sun.jdo.api.persistence.support.JDOUnsupportedOptionException;
import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceManager;
import com.sun.jdo.spi.persistence.support.sqlstore.RetrieveDesc;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import com.sun.jdo.spi.persistence.support.sqlstore.query.jqlc.JQLC;
import com.sun.jdo.spi.persistence.support.sqlstore.query.jqlc.ParameterTable;
import com.sun.jdo.spi.persistence.support.sqlstore.ValueFetcher;
import org.glassfish.persistence.common.I18NHelper;

/**
 *
 * @author  Michael Bouschen
 * @version 0.1
 */
public class QueryImpl
    implements Query
{
    /**
     *
     */
    private Class candidateClass;

    /**
     *
     */
    private String filterExpression;

    /**
     *
     */
    private String importDeclarations;

    /**
     *
     */
    private String parameterDeclarations;

    /**
     *
     */
    private String variableDeclarations;

    /**
     *
     */
    private String orderingSpecification;

    /**
     *
     */
    private String resultExpression;

    /**
     *
     */
    private boolean compiled = false;

    /**
     *
     */
    private transient PersistenceManager pm;

    /**
     *
     */
    private transient Collection candidateCollection;

    /**
     *
     */
    private transient boolean ignoreCache;

    /**
     * Enable relationship fields prefetch for this query.
     */
    private transient boolean prefetchEnabled = true;

    /**
     *
     */
    private transient JQLC jqlc;

    /**
     *
     */
    private transient ParameterTable paramtab;

    /**
     * Flag indicating whtehr this instance was created by serialization.
     */
    private transient boolean createdBySerialization = false;

    /**
     * I18N support
     */
    protected final static ResourceBundle messages =
        I18NHelper.loadBundle(QueryImpl.class);

    /** The logger */
    private static Logger logger = LogHelperQueryExecute.getLogger();

    /**
     * Create an empty query instance with no elements.
     */
    public QueryImpl(PersistenceManager pm)
    {
        if (logger.isLoggable(Logger.FINER))
            logger.finer("LOG_CreateNewQuery", identity()); //NOI18N
        this.pm = pm;
        this.paramtab = new ParameterTable();
        this.ignoreCache = pm.getPersistenceManagerFactory().getIgnoreCache();
    }

    /**
     * Create a new Query using elements from another Query.  The other Query
     * must have been created by the same JDO implementation.  It might be active
     * in a different PersistenceManager or might have been serialized and restored.
     * @param compiled another Query from the same JDO implementation
     */
    public QueryImpl (PersistenceManager pm, Object compiled)
    {
        if (logger.isLoggable(Logger.FINER))
            logger.finer("LOG_CreateNewQueryFromCompiled", identity(), compiled); //NOI18N
        this.pm = pm;
        if (compiled == null)
        {
            JDOException ex = new JDOQueryException(I18NHelper.getMessage(
                messages, "query.queryimpl.init.compiledquery.isnull")); //NOI18N
            logger.throwing("query.QueryImpl", "<init>", ex); //NOI18N
            throw ex;
        }

        if (!(compiled instanceof QueryImpl))
        {
            JDOException ex = new JDOQueryException(I18NHelper.getMessage(
                messages, "query.queryimpl.init.compiledquery.invalidtype", //NOI18N
                compiled.getClass().getName()));
            logger.throwing("query.QueryImpl", "<init>", ex); //NOI18N
            throw ex;
        }

        QueryImpl other = (QueryImpl)compiled;
        this.candidateClass = other.candidateClass;
        this.filterExpression = other.filterExpression;
        this.importDeclarations = other.importDeclarations;
        this.parameterDeclarations = other.parameterDeclarations;
        this.variableDeclarations = other.variableDeclarations;
        this.orderingSpecification = other.orderingSpecification;
        this.resultExpression = other.resultExpression;
        this.ignoreCache = other.ignoreCache;
        this.prefetchEnabled = other.prefetchEnabled;
        this.candidateCollection = null;

        // initialize paramtab, jqlc and compiled
        if (other.paramtab != null)
        {
            this.jqlc = other.jqlc;
            this.paramtab = new ParameterTable(other.paramtab);
            this.compiled = other.compiled;
        } 
        else
        {
            // other.paramtab == null means deserialized query =>
            // - parameter table
            // - set compiled = false
            this.jqlc = null;
            this.paramtab = new ParameterTable();
            this.compiled = false;
        }
    }

    /**
     * Create a query instance with the candidate class specified.
     * @param candidateClass the Class of the candidate instances.
     */
    public QueryImpl(PersistenceManager pm, Class candidateClass)
    {
        this(pm);
        setClass(candidateClass);
    }

    /**
     * Create a query instance with the candidate class and
     * candidate collection specified.
     * @param candidateClass the Class of the candidate instances.
     * @param candidateCollection the Collection of candidate instances.
     */
    public QueryImpl(PersistenceManager pm, Class candidateClass, Collection candidateCollection)
    {
        this(pm);
        setClass(candidateClass);
        setCandidates(candidateCollection);
    }

    /**
     * Create a query instance with the candidate class and
     * filter specified.
     * @param candidateClass the Class of the candidate instances.
     * @param filter the Filter for candidate instances.
     */
    public QueryImpl(PersistenceManager pm, Class candidateClass, String filter)
    {
        this(pm);
        setClass(candidateClass);
        setFilter(filter);
    }

    /**
     * Create a query instance with the candidate class,
     * the candidate collection, and filter specified.
     * @param candidateClass the Class of the candidate instances.
     * @param candidateCollection the Collection of candidate instances.
     * @param filter the Filter for candidate instances
     */
    public QueryImpl(PersistenceManager pm, Class candidateClass, Collection candidateCollection, String filter)
    {
        this(pm);
        setClass(candidateClass);
        setCandidates(candidateCollection);
        setFilter(filter);
    }

    /**
     * Bind the candidate class to the query instance.
     *
     * The class is used to scope the names in the query filter.
     * All of the candidate instances will be of this class or subclass.
     *
     * @param candidateClass the Class of the candidate instances.
     */
    public void setClass(Class candidateClass)
    {
        synchronized (this.paramtab)
        {
            this.candidateClass = candidateClass;
            this.compiled = false;
        }
    }

    /**
     * Bind the candidate Collection to the query instance.
     *
     * @param candidateCollection the Candidate collection.
     */
    public void setCandidates(Collection candidateCollection)
    {
        synchronized (this.paramtab)
        {
            this.candidateCollection = candidateCollection;
            // candidateCollection is not part of query compilation =>
            // do not change compiled flag
        }
    }

    /**
     * Bind the query filter to the query instance.
     *
     * The query filter is a Java boolean expression, which tells whether
     * instances in the candidate collection are to be returned in the result.
     *
     * @param filter the query filter.
     */
    public void setFilter(String filter)
    {
        synchronized (this.paramtab)
        {
            this.filterExpression = filter;
            this.compiled = false;
        }
    }

    /**
     * Bind the import statements to the query instance.
     * All imports must be declared in the same method call,
     * and the imports must be separated by semicolons.
     * The syntax is the same as in the Java language import statement.
     *
     * Parameters and unbound variables might come from a different class
     * from the candidate class, and the names might need to be declared in an
     * import statement to eliminate ambiguity.
     *
     * @param imports import statements separated by semicolons.
     */
    public void declareImports(String imports)
    {
        synchronized (this.paramtab)
        {
            this.importDeclarations = imports;
            this.compiled = false;
        }
    }

    /**
     * Bind the parameter statements to the query instance.
     * This method defines the parameter types and names
     * which will be used by a subsequent execute method.
     *
     * The parameter declaration is a String containing one or
     * more query parameter declarations separated with commas.
     * It follows the syntax for formal parameters in the Java language.
     * Each parameter named in the parameter declaration must be bound
     * to a value when the query is executed.
     *
     * @param parameters the list of parameters separated by commas.
     */
    public void declareParameters(String parameters)
    {
        synchronized (this.paramtab)
        {
            this.parameterDeclarations = parameters;
            this.compiled = false;
        }
    }

    /**
     * Bind the unbound variable statements to the query instance.
     * This method defines the types and names of variables that will be used
     * in the filter but not provided as values by the execute method.
     *
     * Variables might be used in the filter, and these variables must be
     * declared with their type. The unbound variable declaration is a
     * String containing one or more unbound variable declarations separated with
     * semicolons. It follows the syntax for local variables in the Java language.
     *
     * @param variables the variables separated by semicolons.
     */
    public void declareVariables(String variables)
    {
        synchronized (this.paramtab)
        {
            this.variableDeclarations = variables;
            this.compiled = false;
        }
    }

    /**
     * Bind the ordering statements to the query instance.
     *
     * The ordering specification includes a list of expressions
     * with the ascending/descending indicator.
     */
    public void setOrdering(String ordering)
    {
        synchronized (this.paramtab)
        {
            this.orderingSpecification = ordering;
            this.compiled = false;
        }
    }

    /**
     * Set the result of the query.
     * <p>
     * The query result is an optional keyword distinct followed by a Java
     * expression, which tells what values are to be returned by the JDO query.
     * If the result is not specified, then it defaults to "distinct this",
     * which has the effect of returning the elements of the candidates
     * that match the filter.
     */
    public void setResult(String result)
    {
        synchronized (this.paramtab)
        {
            this.resultExpression = result;
            this.compiled = false;
        }
    }

    /**
     * Set the ignoreCache option.
     *
     * The ignoreCache option setting specifies whether the query should execute
     * entirely in the back end, instead of in the cache.
     * @param ignoreCache the setting of the ignoreCache option.
     */
    public void setIgnoreCache(boolean ignoreCache)
    {
        synchronized (this.paramtab)
        {
            this.ignoreCache = ignoreCache;
        }
    }

    /**
     * Get the ignoreCache option setting.
     * @return the ignoreCache option setting.
     * @see #setIgnoreCache
     */
    public boolean getIgnoreCache()
    {
        return ignoreCache;
    }

    /** Sets the prefetchEnabled option.
     *   
     * The prefetchEnabled option specifies whether prefetch of relationship
     * fields should be enabled for this query. The prefetch is enabled by
     * default if such fields are part of DFG. A user needs to explicitely
     * disable prefetch for any particular query if the related instances
     * will not be used in this transaction.
     *
     * @param prefetchEnabled the setting of the prefetchEnabled option.
     */  
    public void setPrefetchEnabled(boolean prefetchEnabled) 
    {
        synchronized (this.paramtab)
        {
            this.prefetchEnabled = prefetchEnabled;
            this.compiled = false;
        }
    }

    /**
     * Verify the elements of the query and provide a hint to the query to
     * prepare and optimize an execution plan.
     */
    public void compile()
    {
        synchronized (this.paramtab)
        {
            if (!this.compiled)
            {
                if (logger.isLoggable(Logger.FINER))
                    logger.finer("LOG_CompileQuery", this); //NOI18N
                // create new query compiler instance
                jqlc = new JQLC();
                // define the query parts including syntax checks
                jqlc.setClass(candidateClass);
                jqlc.declareImports(importDeclarations);
                jqlc.declareParameters(parameterDeclarations);
                jqlc.declareVariables(variableDeclarations);
                jqlc.setOrdering(orderingSpecification);
                jqlc.setResult(resultExpression);
                jqlc.setFilter(filterExpression);
                jqlc.setPrefetchEnabled(prefetchEnabled);

                // semantic analysis
                jqlc.semanticCheck(paramtab);
                this.compiled = true;
            }
        }
    }

    /**
     * Execute the query and return the filtered Collection.
     * @return the filtered Collection.
     * @see #executeWithArray (Object[] parameters)
     */
    public Object execute()
    {
        synchronized (this.paramtab)
        {
            compile();
            ParameterTable params = new ParameterTable(paramtab);
            params.initValueHandling();
            params.checkUnboundParams();
            return doExecute(params);
        }
    }

    /**
     *  Execute the query and return the filtered Collection.
     * @return the filtered Collection.
     * @see #executeWithArray (Object[] parameters)
     * @param p1 the value of the first parameter declared.
     */
    public Object execute(Object p1)
    {
        Object [] params = new Object[1];
        params[0] = p1;
        return executeWithArray(params);
    }

    /**
     * Execute the query and return the filtered Collection.
     * @return the filtered Collection.
     * @see #executeWithArray (Object[] parameters)
     * @param p1 the value of the first parameter declared.
     * @param p2 the value of the second parameter declared.
     */
    public Object execute(Object p1, Object p2)
    {
        Object [] params = new Object[2];
        params[0] = p1;
        params[1] = p2;
        return executeWithArray(params);
    }

    /**
     * Execute the query and return the filtered Collection.
     * @return the filtered Collection.
     * @see #executeWithArray (Object[] parameters)
     * @param p1 the value of the first parameter declared.
     * @param p2 the value of the second parameter declared.
     * @param p3 the value of the third parameter declared.
     */
    public Object execute(Object p1, Object p2, Object p3)
    {
        Object [] params = new Object[3];
        params[0] = p1;
        params[1] = p2;
        params[2] = p3;
        return executeWithArray(params);
    }

    /**
     * Execute the query and return the filtered Collection.
     * @return the filtered Collection.
     * @see #executeWithArray (Object[] parameters)
     * @param parameters the Map containing all of the parameters.
     */
    public Object executeWithMap (Map parameters)
    {
        synchronized (this.paramtab)
        {
            compile();
            ParameterTable params = new ParameterTable(paramtab);
            params.initValueHandling();
            params.setValues(parameters);
            params.checkUnboundParams();
            return doExecute(params);
        }
    }

    /**
     * Execute the query and return the filtered Collection.
     *
     * <P>The execution of the query obtains the values of the parameters and
     * matches them against the declared parameters in order.  The type of
     * the declared parameters must match the type of the passed parameters,
     * except that the passed parameters might need to be unwrapped to get
     * their primitive values.
     *
     * <P>The filter, import, declared parameters, declared variables, and
     * ordering statements are verified for consistency.
     *
     * <P>Each element in the candidate Collection is examined to see that it
     * is assignment compatible to the Class of the query.  It is then evaluated
     * by the boolean expression of the filter.  The element passes the filter
     * if there exist unique values for all variables for which the filter
     * expression evaluates to true.
     * @return the filtered Collection.
     * @param parameters the Object array with all of the parameters.
     */
    public Object executeWithArray (Object[] parameters)
    {
        synchronized (this.paramtab)
        {
            compile();
            ParameterTable params = new ParameterTable(paramtab);
            params.initValueHandling();
            params.setValues(parameters);
            params.checkUnboundParams();
            return doExecute(params);
        }

    }

    /**
     * Get the PersistenceManager associated with this Query.
     *
     * <P>If this Query has no PersistenceManager return null.
     * @return the PersistenceManager associated with this Query.
     */
    public com.sun.jdo.api.persistence.support.PersistenceManager getPersistenceManager()
    {
        return (pm == null)? null : pm.getCurrentWrapper();
    }
    
    /**
     * This method clears the PersistenceManager and the candidateCollection fields. 
     * Then this query instance cannot be executed anymore, but it might be used to 
     * create a new equivalent query instance by passing this query instance to 
     * PersistenceManager newQuery method taking a compiled query.  
     * <p>
     * This method effectively disconnects the PersistenceManager allowing it to be 
     * garbage collected.
     */
    public void clearPersistenceManager()
    {
        this.pm = null;
        this.candidateCollection = null;
    }
    
    /**
     * Internal method called by execute, executeWithArray, executeWithMap.
     * - calls the code generation of the query compiler
     * - flushes updates
     * - executes the RetrieveDesc returned by the code generation
     * - resets the compiler
     */
    private Object doExecute(ParameterTable params)
    {
        Object result = null;
        RetrieveDesc rd = null;

        try
        {
            // We need to make sure that no parallel thread closes the pm =>
            // try to get a shared lock for the pm. Today, the pm impl does
            // not allow to promote a shared lock into a exclusive lock. Thus
            // we need to get an exclusive lock here. Otherwise pm.internalFlush
            // runs into a deadlock, because it tries to get a exclusive lock.
            // This code need to be changed to get a ahared lock as soon as 

            // The next line might result in a NPE, if pm is closed or if the
            // query instance was deserialized. Please note, I cannot check the
            // pm and then get the lock, because the pm might be closed in 
            // parallel. Then subsequent uses of pm in doexecute would fail.
            pm.acquireExclusiveLock();
        }
        catch (NullPointerException npe)
        {
            // NPE means pm is closed or query instance was serialized.
            String key = (createdBySerialization ? 
                          "query.queryimpl.doexecute.notboundtopm" : //NOI18N
                          "query.queryimpl.doexecute.pmclosed"); //NOI18N
            JDOException ex = new JDOQueryException(
                I18NHelper.getMessage(messages, key));
            logger.throwing("query.QueryImpl", "compile", ex); //NOI18N
            throw ex;
        }

        try
        {
            checkCandidates();
            // call the code generation
            rd = jqlc.codeGen(pm, params);
            // flush changes (inserts, updates, deletes) to the datastore
            flush();
            if (logger.isLoggable(Logger.FINER))
                logger.finer("LOG_ExecuteQuery", this, params.getValues()); //NOI18N
            // Note, the RetrieveDesc returned by the code generation is null
            // in the case of a query having a false filter =>
            // do not go to the datastore, but return an emtpy collection
            result = (rd != null) ?  pm.retrieve(rd, params.getValueFetcher()) : new ArrayList();
        }
        finally
        {
            // Note, the following stmt needs to be replaced by 
            // pm.releaseSharedLock, as soon as the pm supports promoting a 
            // shared lock into an exclusive lock.
            pm.releaseExclusiveLock();
        }

        return result;
    }

    /**
     * This method checks a valid candidates setting for this query. 
     */
    private void checkCandidates()
    {
        if ((candidateCollection == null) && (candidateClass != null)) 
        {
            // Set candidateCollection to the extent of the candidate class, if
            // candidateCollection is not specified. Note, the JDO spec defines 
            // subclasses=true as the default, but since this is not supported 
            // right now, I set it to subclasses=false.
            candidateCollection = pm.getExtent(candidateClass, false);
        }
        else {
            jqlc.checkCandidates(candidateClass, candidateCollection);
        }
    }

    /**
     *
     */
    private void flush()
    {
        Transaction tx = pm.currentTransaction();
        // flush updates to the database,
        // - if the is a transaction active and
        // - if transaction is not optimistic
        // - if ignoreCache is false
        if ((tx != null) && tx.isActive() && 
            !tx.getOptimistic() && !this.ignoreCache)
        {
            pm.internalFlush();
        }
    }

    /**  Returns a string representation of the object. */
    public String toString()
    {
        StringBuffer repr = new StringBuffer();
        repr.append("QueryImpl("); //NOI18N
        repr.append("candidateClass: "); //NOI18N
        repr.append(candidateClass);
        if (importDeclarations != null) {
            repr.append(", imports: "); //NOI18N
            repr.append(importDeclarations);
        } 
        if (parameterDeclarations != null) {
            repr.append(", parameters: "); //NOI18N
            repr.append(parameterDeclarations);
        }
        if (variableDeclarations != null) {
            repr.append(", variables: "); //NOI18N
            repr.append(variableDeclarations);
        }
        if (filterExpression != null) {
            repr.append(", filter: "); //NOI18N
            repr.append(filterExpression);
        }
        if (orderingSpecification != null) {
            repr.append(", ordering: "); //NOI18N
            repr.append(orderingSpecification);
        }
        if (resultExpression != null) {
            repr.append(", result: "); //NOI18N
            repr.append(resultExpression);
        }
        repr.append(", prefetchEnabled: "); //NOI18N
        repr.append(prefetchEnabled);
        repr.append(", identity: "); //NOI18N
        repr.append(identity());
        repr.append(")"); //NOI18N
        return repr.toString();
    }

    /** */
    private String identity()
    {
        return "QueryImpl@" + System.identityHashCode(this); //NOI18N
    }
    

    /**
     * Define readObject to initialize the transient field paramtab after deserialization.
     * This object is used for synchronization, thus it cannot be null.
     */
    private void readObject(java.io.ObjectInputStream in)
        throws java.io.IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        this.paramtab = new ParameterTable();
        this.createdBySerialization = true;
    }

}
