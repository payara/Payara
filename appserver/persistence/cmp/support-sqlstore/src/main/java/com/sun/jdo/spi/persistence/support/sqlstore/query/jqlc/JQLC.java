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
 * JQLC.java
 *
 * Created on March 8, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore.query.jqlc;

import java.io.Reader;
import java.io.StringReader;
import java.util.*;

import antlr.TokenBuffer;
import antlr.ANTLRException;

import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.api.persistence.support.JDOQueryException;
import com.sun.jdo.api.persistence.support.JDOUnsupportedOptionException;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.spi.persistence.utility.StringHelper;
import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceManager;
import com.sun.jdo.spi.persistence.support.sqlstore.RetrieveDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.ExtentCollection;
import com.sun.jdo.spi.persistence.utility.logging.Logger;

import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.TypeTable;

/** 
 *
 * @author  Michael Bouschen
 * @version 0.1
 * 
 * Note: this class allows to override its fields even after all the processing
 * is done via the corresponding setXXX methods. This is not expected behavior.
 * A better solution would be to change all setters to be private and have use 
 * a constructor to populate all the values. The constructor will call private
 * setters to process the arguments.
 */
public class JQLC
{
    /** */
    protected TypeTable typetab;

    /** */
    protected ErrorMsg errorMsg;    
    
    /** */
    protected Class candidateClass;
    
    /** */
    protected JQLAST filterAST = null;
    
    /** */
    protected JQLAST importsAST = null;
    
    /** */
    protected JQLAST varsAST = null;
    
    /** */
    protected JQLAST paramsAST = null;
    
    /** */
    protected JQLAST orderingAST = null;

    /** */
    protected JQLAST resultAST = null;

    /** */
    protected JQLAST queryAST = null;
    
    /** */
    private boolean prefetchEnabled;
    
    /** 
     * RD cache, key is a string build from actual param values 
     * (see ParameterTable.getKeyForRetrieveDescCache).
     * It's ok to use WeakHashMap from java.util, because the key is a string 
     * which is not referenced by the RD.
     */
    protected Map retrieveDescCache = new HashMap();

    /** I18N support */
    protected final static ResourceBundle messages = 
        I18NHelper.loadBundle(JQLC.class);

    /** The logger */
    private static Logger logger = LogHelperQueryCompilerJDO.getLogger();

    /**
     *
     */
    public JQLC()
    {
        this.errorMsg = new ErrorMsg();
    }
    
    /**
     *
     */
    public void setClass(Class candidateClass)
    {
        // check valid candidate class definition
        if (candidateClass == null)
        {
            JDOQueryException ex =  new JDOQueryException(I18NHelper.getMessage(
                messages, "jqlc.jqlc.generic.nocandidateclass")); //NOI18N
            logger.throwing("jqlc.JQLC", "setClass", ex); //NOI18N
            throw ex;
        }
        this.candidateClass = candidateClass;
    }

    /**
     *
     */
    public void declareImports(String imports)
    {
        if (imports == null)
        {
            importsAST = null;
            return;
        }

        try
        {
            JQLParser parser = createStringParser(imports);
            parser.parseImports();
            importsAST = (JQLAST)parser.getAST();        
        }
        catch (ANTLRException ex)
        {
            JQLParser.handleANTLRException(ex, errorMsg);
        }
    }
    
    /**
     *
     */
    public void declareParameters(String parameters)
    {
        if (parameters == null)
        { 
            paramsAST = null;
            return;
        }
        
        try
        {
            JQLParser parser = createStringParser(parameters);
            parser.parseParameters();
            paramsAST = (JQLAST)parser.getAST();        
        }
        catch (ANTLRException ex)
        {
            JQLParser.handleANTLRException(ex, errorMsg);
        }
    }
    
    /**
     *
     */
    public void declareVariables(String variables)
    {
        if (variables == null)
        {
            varsAST = null;
            return;
        }
        
        try
        {
            JQLParser parser = createStringParser(variables);
            parser.parseVariables();
            varsAST = (JQLAST)parser.getAST();        
        }
        catch (ANTLRException ex)
        {
            JQLParser.handleANTLRException(ex, errorMsg);
        }
    }
    
    /**
     *
     */
    public void setOrdering(String ordering)
    {
        if (ordering == null)
        {
            orderingAST = null;
            return;
        }
        
        try
        {
            JQLParser parser = createStringParser(ordering);
            parser.parseOrdering();
            orderingAST = (JQLAST)parser.getAST();        
        }
        catch (ANTLRException ex)
        {
            JQLParser.handleANTLRException(ex, errorMsg);
        }
    }
    
    /**
     *
     */
    public void setResult(String result)
    {
        if (result == null)
        {
            resultAST = null;
            return;
        }
        
        try
        {
            JQLParser parser = createStringParser(result);
            parser.parseResult();
            resultAST = (JQLAST)parser.getAST();        
        }
        catch (ANTLRException ex)
        {
            JQLParser.handleANTLRException(ex, errorMsg);
        }
    }
    
    /**
     *
     */
    public void setFilter(String filter)
    {
        if (StringHelper.isEmpty(filter))
        {
            // If there is no filter specified use "true" as filter.
            // This is the case if 
            // - setFilter is not called at all (filter == null)
            // - the filter is empty or contians whitespecace only.
            // Internally the filter has to be specified, 
            // otherwise semantic analysis has problems with empty AST.
            filter = "true"; //NOI18N
        }
        
        try
        {
            JQLParser parser = createStringParser(filter);
            parser.parseFilter();
            filterAST = (JQLAST)parser.getAST();        
        }
        catch (ANTLRException ex)
        {
            JQLParser.handleANTLRException(ex, errorMsg);
        }
     }
    
    /**
     *
     */
    public void setPrefetchEnabled(boolean prefetchEnabled)
    {
        this.prefetchEnabled = prefetchEnabled;
    }

    /**
     *
     */
    public void semanticCheck(ParameterTable paramtab)
    {
        boolean finer = logger.isLoggable(Logger.FINER);
        boolean finest = logger.isLoggable(Logger.FINEST);
        this.typetab = TypeTable.getInstance(candidateClass.getClassLoader());
        paramtab.init();
        Semantic semantic = new Semantic();
        semantic.init(typetab, paramtab, errorMsg);
        semantic.setASTFactory(JQLAST.Factory.getInstance());
        
        // create complete tree representation
        JQLAST classAST = semantic.checkCandidateClass(candidateClass);
        queryAST = semantic.createQueryAST(classAST, importsAST, paramsAST, varsAST, 
                                           orderingAST, resultAST, filterAST);
        
        if (finest) logger.finest("LOG_JQLCDumpTree", queryAST.getTreeRepr("(AST)")); //NOI18N

        // start semantic check
        try
        {
            if (finer) logger.finer("LOG_JQLCStartPass", "semantic analysis"); //NOI18N
            semantic.query(queryAST);
            queryAST = (JQLAST)semantic.getAST();
            if (finest) logger.finest("LOG_JQLCDumpTree", queryAST.getTreeRepr("(typed AST)")); //NOI18N
        }
        catch (ANTLRException ex)
        {
            errorMsg.fatal("JQLC.semanticCheck unexpected exception", ex); //NOI18N
        }
    }

    /**
     *
     */
    public RetrieveDesc codeGen(PersistenceManager pm, ParameterTable paramtab)
    {
        boolean finer = logger.isLoggable(Logger.FINER);
        boolean finest = logger.isLoggable(Logger.FINEST);
        RetrieveDesc rd = null;
        
        // check if a RetrieveDescriptor for the actual parameter constellation
        // is already available in the cache
        String key = paramtab.getKeyForRetrieveDescCache();

        synchronized(retrieveDescCache)
        {
            if (key != null)
                rd = (RetrieveDesc)retrieveDescCache.get(key);
            
            if (rd == null) {
                Optimizer optimizer = new Optimizer();
                optimizer.init(typetab, paramtab, errorMsg);
                optimizer.setASTFactory(JQLAST.Factory.getInstance());
                
                CodeGeneration codeGen = new CodeGeneration();
                codeGen.init(pm, typetab, paramtab, errorMsg, prefetchEnabled);
                codeGen.setASTFactory(JQLAST.Factory.getInstance());
                
                try
                {
                    JQLAST ast = queryAST;
                    
                    // The optimizer should treat query parameters as constant values,
                    // so I cannot call the optimzer before the query parameter values 
                    // are known. That's why optimization is part of codeGen which is 
                    // called by Query.execute and not called by Query.compile.
                    if (finer) logger.finer("LOG_JQLCStartPass", "optimizer"); //NOI18N
                    optimizer.query(ast);
                    // Do not store the optimizer result in the instance variable queryAST, 
                    // it cannot be reused by the next execution of this query. The next execute 
                    // might have different query parameter values, so the optimized AST is different. 
                    ast = (JQLAST)optimizer.getAST();
                    if (finest) logger.finest("LOG_JQLCDumpTree", ast.getTreeRepr("(optimized AST)")); //NOI18N
                    
                    if (finer) logger.finer("LOG_JQLCStartPass", "code generation"); //NOI18N
                    codeGen.query(ast);
                    rd = codeGen.getRetrieveDesc();
                    // add the current RetrieveDescriptor to the cache, 
                    // if the key is not null
                    if (key != null)
                        retrieveDescCache.put(key, rd);
                }
                catch (ANTLRException ex)
                {
                    errorMsg.fatal("JQLC.codeGen unexpected exception", ex); //NOI18N
                }
            }
            else {
                if (finer) logger.finest("LOG_JQLCReuseRetrieveDesc"); //NOI18N
            }
        }
        
        return rd;
    }

    /**
     *
     */
    public void checkCandidates(Class candidateClass, Collection candidateCollection)
    {
        if (candidateClass == null)
            throw new JDOQueryException(
                I18NHelper.getMessage(messages, "jqlc.jqlc.generic.nocandidateclass")); //NOI18N
        if (!(candidateCollection instanceof ExtentCollection))
            throw new JDOUnsupportedOptionException(
                I18NHelper.getMessage(messages, "jqlc.jqlc.checkcandidates.memorycollection")); //NOI18N
        
        Class candidatePCClass = ((ExtentCollection)candidateCollection).getPersistenceCapableClass();
        if (candidatePCClass == null)
            throw new JDOFatalInternalException(
                I18NHelper.getMessage(messages, "jqlc.jqlc.checkcandidates.nullpc")); //NOI18N
        
        if (!candidateClass.getName().equals(candidatePCClass.getName()))
            throw new JDOQueryException(
                I18NHelper.getMessage(messages, "jqlc.jqlc.checkcandidates.mismatch", candidateClass.getName())); //NOI18N
    }

    /** */
    private JQLParser createStringParser(String text)
    {
        return createStringParser(text, errorMsg);
    }

    /**
     * Returns a JQLParser instance parsing the specified text.
     */
    public static JQLParser createStringParser(String text, ErrorMsg errorMsg)
    {
        Reader in = new StringReader(text);
        JQLLexer lexer = new JQLLexer(in);
        lexer.init(errorMsg);
        TokenBuffer buffer = new TokenBuffer(lexer);
        JQLParser parser = new JQLParser(buffer);
        parser.init(errorMsg);
        parser.setASTFactory(JQLAST.Factory.getInstance());
        return parser;
    }
    
}
