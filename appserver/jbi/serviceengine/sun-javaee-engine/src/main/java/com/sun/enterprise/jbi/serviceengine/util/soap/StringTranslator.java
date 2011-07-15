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

import java.text.MessageFormat;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;


/**
 * This is the implementation of the String Translator, which provides services
 * for internationalization of messages to all services running inside the JBI
 * environment.
 *
 * @author Sun Microsystems Inc.
 */
public class StringTranslator
{
    /**
     * Logger name
     */
    private static final String LOGGER_NAME = "com.sun.jbi.common.soap";

    /**
     * Unqualified name for resource bundles.
     */
    public static final String RESOURCE_BUNDLE_NAME = "LocalStrings";

    /**
     * Log message for creation of new instance.
     */
    private static final String LOG_NEW_INSTANCE =
        "New StringTranslator for package {0}, classLoader is {1}";

    /**
     * Log message for locale.
     */
    private static final String LOG_CURRENT_LOCALE = "Current locale is {0}";

    /**
     * Log message for failure loading resource bundle.
     */
    private static final String LOG_UNABLE_TO_LOAD_BUNDLE =
        "Unable to load resource bundle {0} for locale {1}: {2}";

    /**
     * Log message for using alternate resource bundle.
     */
    private static final String LOG_USING_BUNDLE =
        "Using resource bundle for locale {0} instead.";

    /**
     * Log message for using fallback resource bundle to look up a message.
     */
    private static final String LOG_TRANSLATION_USING_FALLBACK =
        "No translation for key={0} found in resource bundle for locale {1}, "
        + "using locale {2} instead.";

    /**
     * Log message for no translation available for a message key in any
     * resource bundle.
     */
    private static final String LOG_NO_TRANSLATION_FOR_KEY =
        "No translation for key={0} found in any resource bundle. "
        + "Insert data is [{1}].";

    /**
     * Log message for no translation available for a message key in a
     * particular resource bundle.
     */
    private static final String LOG_NO_TRANSLATION_FOR_KEY_IN_BUNDLE =
        "No translation for key={0} found in resource bundle for locale {1}. "
        + "Insert data is [{2}].";

    /**
     * Message text used when no translation is available for a message key.
     */
    private static final String MSG_NO_TRANSLATION =
        "No translation available for message with key={0} and inserts=[{1}].";

    /**
     * The default locale at the time this StringTranslator was created.
     */
    private Locale mDefaultLocale;

    /**
     * Logger for this instance
     */
    private Logger mLog;

    /**
     * Fallback ResourceBundle for a single package name. This is used only
     * when the default locale is something other than Locale.US. In that
     * case, this is the bundle for Locale.US for looking up messages that are
     * not found in the default locale.
     */
    private ResourceBundle mFallbackBundle;

    /**
     * ResourceBundle for a single package name.
     */
    private ResourceBundle mResourceBundle;

    public StringTranslator() {
        this("com.sun.enterprise.jbi.serviceengine.core", null);
    }
    
    private static StringTranslator defaultInstance;
    
    public static StringTranslator getDefaultInstance() {
        if (defaultInstance == null) {
            defaultInstance = new StringTranslator();
        }
        return defaultInstance;
    }
    
    /**
     * Constructor. This loads the Resource Bundle for the current locale, and
     * if the current locale is not Locale.US, it loads the Resource Bundle
     * for Locale.US and stores it as the backup for string lookup.
     *
     * @param packageName - the name of the package that contains the resource
     *        bundle.
     * @param classLoader - the class loader to be used for loading the
     *        resource bundle. If this parameter is null, the current class
     *        loader is used.
     */
    public StringTranslator(
        String packageName,
        ClassLoader classLoader)
    {
        mLog = Logger.getLogger(packageName);

        String bundleName = packageName + "." + RESOURCE_BUNDLE_NAME;
        mDefaultLocale = Locale.getDefault();

        // Always load the bundle for english
        mFallbackBundle = null;

        ResourceBundle englishBundle = null;

        try
        {
            if (null == classLoader)
            {
                englishBundle = ResourceBundle.getBundle(bundleName, Locale.US);
            }
            else
            {
                englishBundle =
                    ResourceBundle.getBundle(bundleName, Locale.US, classLoader);
            }
        }
        catch (java.util.MissingResourceException mrEx)
        {
            mLog.warning(MessageFormat.format(LOG_UNABLE_TO_LOAD_BUNDLE,
                    new Object [] {bundleName, Locale.US, mrEx}));
        }

        // If the default locale is English, set it as the primary bundle.
        // If the default locale is not English, attempt to load the bundle.
        // If it is found, save it as the primary and set the fallback to
        // English. If it is not found, set the primary to English.
        if (mDefaultLocale.equals(Locale.US))
        {
            mResourceBundle = englishBundle;
        }
        else
        {
            try
            {
                if (null == classLoader)
                {
                    mResourceBundle = ResourceBundle.getBundle(bundleName);
                    mFallbackBundle = englishBundle;
                }
                else
                {
                    mResourceBundle =
                        ResourceBundle.getBundle(bundleName, mDefaultLocale,
                            classLoader);
                    mFallbackBundle = englishBundle;
                }
            }
            catch (java.util.MissingResourceException mrEx)
            {
                mLog.warning(MessageFormat.format(LOG_UNABLE_TO_LOAD_BUNDLE,
                        new Object [] {bundleName, mDefaultLocale, mrEx}));
                mLog.warning(MessageFormat.format(LOG_USING_BUNDLE,
                        new Object [] {Locale.US}));
                mResourceBundle = englishBundle;
            }
        }
    }

    /**
     * Get a localized string using the specified resource key.
     *
     * @param key - the key to the localized string in the resource bundle.
     *
     * @return the localized string.
     */
    public String getString(String key)
    {
        Object [] inserts = new Object[0];

        return getString(key, inserts);
    }

    /**
     * Get a localized string using the specified resource key. Handle one
     * message insert.
     *
     * @param key - the key to the localized string in the resource bundle.
     * @param insert1 - the message insert.
     *
     * @return the localized string formatted with the message insert.
     */
    public String getString(
        String key,
        Object insert1)
    {
        Object [] inserts = {insert1};

        return getString(key, inserts);
    }

    /**
     * Get a localized string using the specified resource key. Handle two
     * message inserts.
     *
     * @param key - the key to the localized string in the resource bundle.
     * @param insert1 - the first message insert.
     * @param insert2 - the second message insert.
     *
     * @return the localized string formatted with the message inserts.
     */
    public String getString(
        String key,
        Object insert1,
        Object insert2)
    {
        Object [] inserts = {insert1, insert2};

        return getString(key, inserts);
    }

    /**
     * Get a localized string using the specified resource key. Handle three
     * message inserts.
     *
     * @param key - the key to the localized string in the resource bundle.
     * @param insert1 - the first message insert.
     * @param insert2 - the second message insert.
     * @param insert3 - the third message insert.
     *
     * @return the localized string formatted with the message inserts.
     */
    public String getString(
        String key,
        Object insert1,
        Object insert2,
        Object insert3)
    {
        Object [] inserts = {insert1, insert2, insert3};

        return getString(key, inserts);
    }

    /**
     * Get a localized string using the specified resource key. Handle four
     * message inserts.
     *
     * @param key - the key to the localized string in the resource bundle.
     * @param insert1 - the first message insert.
     * @param insert2 - the second message insert.
     * @param insert3 - the third message insert.
     * @param insert4 - the fourth message insert.
     *
     * @return the localized string formatted with the message inserts.
     */
    public String getString(
        String key,
        Object insert1,
        Object insert2,
        Object insert3,
        Object insert4)
    {
        Object [] inserts = {insert1, insert2, insert3, insert4};

        return getString(key, inserts);
    }

    /**
     * Get a localized string using the specified resource key. Handle any
     * number of message inserts. This method is called by all the other
     * getString() methods in the class. The procedure for string lookup is to
     * first look in the primary resource bundle, then in the fallback
     * resource bundle. If the string is found in the primary, return the
     * translated string quietly. If the string is not found in the primary
     * but in the fallback, log a warning and return the translated string. If
     * the string is not found in either bundle, log an error and return a
     * message formatted with the key and insert values provided by the
     * caller. If there is no resource bundle available, just return a message
     * formatted with the key and insert values provided by the caller.
     *
     * @param key - the key to the localized string in the resource bundle.
     * @param inserts - the array of message inserts.
     *
     * @return the localized string formatted with the message inserts.
     */
    public String getString(
        String key,
        Object [] inserts)
    {
        String translated = null;

        if (null != mResourceBundle)
        {
            try
            {
                translated = mResourceBundle.getString(key);
                translated = MessageFormat.format(translated, inserts);
            }
            catch (java.util.MissingResourceException mrEx)
            {
                if (null != mFallbackBundle)
                {
                    try
                    {
                        translated = mFallbackBundle.getString(key);
                        translated = MessageFormat.format(translated, inserts);
                        mLog.fine(MessageFormat.format(
                                LOG_TRANSLATION_USING_FALLBACK,
                                new Object [] {key, mDefaultLocale, Locale.US}));
                    }
                    catch (java.util.MissingResourceException mreEx)
                    {
                        String fi = formatInserts(inserts);
                        translated =
                            MessageFormat.format(MSG_NO_TRANSLATION,
                                new Object [] {key, fi});
                        mLog.warning(MessageFormat.format(
                                LOG_NO_TRANSLATION_FOR_KEY,
                                new Object [] {key, fi}));
                    }
                }
                else
                {
                    String fi = formatInserts(inserts);
                    translated =
                        MessageFormat.format(MSG_NO_TRANSLATION,
                            new Object [] {key, fi});
                    mLog.warning(MessageFormat.format(
                            LOG_NO_TRANSLATION_FOR_KEY_IN_BUNDLE,
                            new Object [] {key, mDefaultLocale, fi}));
                }
            }
        }
        else
        {
            translated =
                MessageFormat.format(MSG_NO_TRANSLATION,
                    new Object [] {key, formatInserts(inserts)});
        }

        return translated;
    }

    /**
     * Format an array of message inserts into a string. The ouptut string is
     * in the format "insert1,insert2,....,insertn".
     *
     * @param inserts - the array of message inserts.
     *
     * @return the string formatted with the message inserts.
     */
    private String formatInserts(Object [] inserts)
    {
        StringBuffer formatted = new StringBuffer("");

        for (int i = 0; i < inserts.length; i++)
        {
            if (i > 0)
            {
                formatted.append(",");
            }

            formatted.append(inserts[i].toString());
        }

        return formatted.toString();
    }
}
