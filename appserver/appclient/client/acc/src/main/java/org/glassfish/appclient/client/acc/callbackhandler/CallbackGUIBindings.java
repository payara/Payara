/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.client.acc.callbackhandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import org.glassfish.appclient.client.acc.callbackhandler.CallbackBinding.GUI;

/**
 *
 * @author tjquinn
 */
public class CallbackGUIBindings {

    private static final Map<Class,Class> callbackToBinding = initCallbackToBindingMap();

    private static Map<Class,Class> initCallbackToBindingMap() {
        Map<Class,Class> result = new HashMap<Class,Class>();
        result.put(ChoiceCallback.class, Choice.class);
        result.put(ConfirmationCallback.class, Confirmation.class);
        result.put(LanguageCallback.class, Language.class);
        result.put(NameCallback.class, Name.class);
        result.put(PasswordCallback.class, Password.class);
        result.put(TextInputCallback.class, TextInput.class);
        result.put(TextOutputCallback.class, TextOutput.class);
        return result;
    }
    
    /**
     * Factory method for creating the appropriate callback-to-U/I binding
     * object given a callback.
     * @param callback the Callback for which to create the binding
     * @return the CallbackUIBinding suitable for the type of callback provided
     * @throws javax.security.auth.callback.UnsupportedCallbackException if the 
     * type of callback is not recognized
     */
    public Binding createCallbackGUIBinding(Callback callback) 
            throws UnsupportedCallbackException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        
        Class bindingClass = callbackToBinding.get(callback.getClass());
        if (bindingClass != null) {
            Constructor<Binding> constructor = 
                    bindingClass.getConstructor(
                        new Class[] { CallbackGUIBindings.class });
            Binding binding = constructor.newInstance(new Object[] { this });
            binding.setCallback(callback);
            return binding;
        }
        throw new UnsupportedCallbackException(callback);
    }    

    /**
     * Creates a default ConfirmationCallback binding (because the caller did
     * not provide one).
     * @return the default ConfirmationCallbackUIBinding
     */
    public CallbackGUIBindings.Confirmation getDefaultConfirmationCallbackUIBinding() {
        ConfirmationCallback defaultCallback = new ConfirmationCallback
                (ConfirmationCallback.INFORMATION,
                 ConfirmationCallback.OK_CANCEL_OPTION,
                 ConfirmationCallback.OK);

        CallbackGUIBindings.Confirmation binding = 
                new CallbackGUIBindings.Confirmation();
        binding.setCallback(defaultCallback);
        return binding;
    }
    
    /** number of rows to be visible in JLists used to display information*/
    protected static final int LIST_ROWS = 4;
    
    /** number of rows of text to be visible in text areas */
    protected static final int TEXT_ROWS = 4;
    
    /** number of columns for text areas */
    protected static final int TEXT_COLUMNS = 20;

    public abstract class Binding<C extends Callback> implements GUI<C> {
        private JComponent component = null;

        protected C callback;

        protected JComponent createPromptedInputBox(String prompt, JComponent input) {
            Box box = new Box(BoxLayout.X_AXIS);
            JLabel promptLabel = new JLabel(prompt);
            box.add(promptLabel);
            box.add(input);
            return box;
        }

        protected MessageType getMessageType() {
            return null;
        }

        /**
         * Creates a JScrollPane containing the specified component with no
         * column header.
         * @param body the JComponent to enclose in the scroll pane
         * @return the JScrollPane holding the component
         */
        protected JScrollPane prepareScrollPane(JComponent body) {
            return new JScrollPane(body);
        }

        /**
         * Creates a JScrollPane containing a column header and the specified component.
         * @param columnHeader String containing the header text
         * @param body the JComponent to enclose in the scroll pane
         * @return the JScrollPane with the specified header and component 
         */
        protected JScrollPane prepareScrollPane(String columnHeader, JComponent body) {
            JScrollPane scrollPane = new JScrollPane(body);
            JLabel headerLabel = new JLabel(columnHeader);
            headerLabel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
            scrollPane.setColumnHeaderView(headerLabel);
            return scrollPane;
        }

        protected abstract JComponent createComponent();

        public JComponent getComponent() {
            if (component == null) {
                component = createComponent();
            }
            return component;
        }

        @Override
        public void setCallback(C callback) {
            this.callback = callback;
        }
    }
    
    public class Choice extends Binding<ChoiceCallback> {
        /**
         * The binding for ChoiceCallbacks.
         */
        private JList jList;

        @Override
        protected JComponent createComponent() {
            jList = prepareList(
                        callback.getChoices(), 
                        callback.getSelectedIndexes());
            return prepareScrollPane(callback.getPrompt(), jList);
        }

        private JList prepareList(String[] choices, int[] selectedIndexes) {
            JList result = new JList();
            result.setVisibleRowCount(LIST_ROWS);
            result.setListData(choices);
            if (selectedIndexes != null) {
                result.setSelectedIndices(selectedIndexes);
            }
            result.setSelectionMode(this.callback.allowMultipleSelections() ? 
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION :
                ListSelectionModel.SINGLE_SELECTION);

            return result;
        }

        @Override
        public void finish() {
            if (callback.allowMultipleSelections()) {
                callback.setSelectedIndexes(jList.getSelectedIndices());
            } else {
                callback.setSelectedIndex(jList.getSelectedIndex());
            }
        }
    }

    public class Confirmation extends Binding<ConfirmationCallback> {

        @Override
        protected JComponent createComponent() {
            return null;
        }

        @Override
        protected MessageType getMessageType() {
            return MessageType.severityForConfirmation(callback.getMessageType());
        }

        @Override
        public void finish() {
        }

        public int getOptionPaneOptionType() {
            if (callback.getOptionType() == ConfirmationCallback.OK_CANCEL_OPTION) {
                return JOptionPane.OK_CANCEL_OPTION;
            }
            if (callback.getOptionType() == ConfirmationCallback.YES_NO_CANCEL_OPTION) {
                return JOptionPane.YES_NO_CANCEL_OPTION;
            }
            if (callback.getOptionType() == ConfirmationCallback.YES_NO_OPTION) {
                return JOptionPane.YES_NO_OPTION;
            }
            return JOptionPane.OK_CANCEL_OPTION;
        }

        public String[] getOptions() {
            return callback.getOptions();
        }

        public int getDefaultOption() {
            return callback.getDefaultOption();
        }

        public void setResult(int result) {
            int callbackResult = -1;
            if (callback.getOptionType() == ConfirmationCallback.UNSPECIFIED_OPTION) {
                callbackResult = result;
            } else {
                if (callback.getOptionType() == ConfirmationCallback.OK_CANCEL_OPTION) {
                    if (result == JOptionPane.OK_OPTION) {
                        callbackResult = ConfirmationCallback.OK;
                    } else if (result == JOptionPane.CANCEL_OPTION) {
                        callbackResult = ConfirmationCallback.CANCEL;
                    }
                } else if (callback.getOptionType() == ConfirmationCallback.YES_NO_CANCEL_OPTION) {
                    if (result == JOptionPane.YES_OPTION) {
                        callbackResult = ConfirmationCallback.YES;
                    } else if (result == JOptionPane.NO_OPTION) {
                        callbackResult = ConfirmationCallback.NO;
                    } else if (result == JOptionPane.CANCEL_OPTION) {
                        callbackResult = ConfirmationCallback.CANCEL;
                    }
                } else if (callback.getOptionType() == ConfirmationCallback.YES_NO_OPTION) {
                    if (result == JOptionPane.YES_OPTION) {
                        callbackResult = ConfirmationCallback.YES;
                    } else if (result == JOptionPane.NO_OPTION) {
                        callbackResult = ConfirmationCallback.NO;
                    }                    
                }
            }
            callback.setSelectedIndex(callbackResult);
        }
    }

    public class Name extends Binding<NameCallback> {

        private JTextField nameField;

        @Override
        protected JComponent createComponent() {
            JComponent result = createPromptedInputBox(
                        callback.getPrompt(),
                        nameField = new JTextField(callback.getDefaultName()));
            nameField.setColumns(20);
            return result;
        }

        @Override
        public void finish() {
             callback.setName(nameField.getText());
        }

    }

    public class Password extends Binding<PasswordCallback> {
        private JPasswordField passwordField;

        @Override
        public JComponent createComponent() {
            JComponent result = createPromptedInputBox(
                    callback.getPrompt(),
                    passwordField = new JPasswordField());
            passwordField.setColumns(20);
            return result;
        }

        @Override
        public void finish() {
            callback.setPassword(passwordField.getPassword());
            passwordField.setText("");
        }
    }

    public class TextInput extends Binding<TextInputCallback> {

        private JTextArea textArea;

        @Override
        public JComponent createComponent() {
            textArea = new JTextArea(
                    callback.getDefaultText(), 
                    TEXT_ROWS, 
                    TEXT_COLUMNS);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            return prepareScrollPane(callback.getPrompt(), textArea);
        }

        @Override
        public void finish() {
            callback.setText(textArea.getText());
        }

    }

    public class TextOutput extends Binding<TextOutputCallback> {
        private JTextArea textArea;

        @Override
        public JComponent createComponent() {
            textArea = new JTextArea(this.callback.getMessage());
            textArea.setEditable(false);
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            textArea.setBackground(new JLabel().getBackground());
            return textArea;
        }

        @Override
        public void finish() {
        }

        @Override
        public MessageType getMessageType() {
            return MessageType.severityForTextOutput(callback.getMessageType());
        }
    }

    public class Language extends Binding<LanguageCallback> {
        private JList languageList;

        @Override
        public JComponent createComponent() {

            Vector<LocaleEntry> entries = new Vector<LocaleEntry>();
            Locale defaultLocale = Locale.getDefault();
            LocaleEntry defaultEntry = null;
            Locale [] sortedLocales = Locale.getAvailableLocales();
            Arrays.sort(sortedLocales, 
                    new Comparator<Locale>() {
                        @Override
                        public int compare(Locale l1, Locale l2) {
                            return l1.getDisplayName().compareTo(l2.getDisplayName());
                        }
                });
            for (Locale locale : sortedLocales) {
                LocaleEntry newEntry = new LocaleEntry(locale);
                entries.add(newEntry);
                if (locale.equals(defaultLocale)) {
                    defaultEntry = newEntry;
                }
            }

            languageList = new JList(entries);
            languageList.setVisibleRowCount(LIST_ROWS);
            languageList.setSelectedValue(defaultEntry, true);
            return prepareScrollPane(languageList);
        }

        @Override
        public void finish() {
            callback.setLocale(((LocaleEntry) languageList.getSelectedValue()).locale);
        }

        private class LocaleEntry {
            private Locale locale;

            private LocaleEntry(Locale locale) {
                this.locale = locale;
            }

            @Override
            public String toString() {
                return locale.getDisplayName();
            }


        }        
    }

    /**
     * Simplifies converting between option pane message types and callback
     * message types.
     */
    protected enum MessageType {
        /*
         * The message types must be defined in order of increasing severity.
         */
        PLAIN(JOptionPane.PLAIN_MESSAGE),
        INFORMATION(
                JOptionPane.INFORMATION_MESSAGE, 
                ConfirmationCallback.INFORMATION,
                TextOutputCallback.INFORMATION),
        QUESTION(JOptionPane.QUESTION_MESSAGE),
        WARNING(
                JOptionPane.WARNING_MESSAGE,
                ConfirmationCallback.WARNING,
                TextOutputCallback.WARNING),
        ERROR(
                JOptionPane.ERROR_MESSAGE,
                ConfirmationCallback.ERROR,
                TextOutputCallback.ERROR);
        
        private int optionPaneMessageType;
        private int confirmationCallbackMessageType;
        private int textOutputCallbackMessageType;
        private boolean mapsToCallback;
        
        private MessageType(int optionPaneMessageType) {
            this.optionPaneMessageType = optionPaneMessageType;
            mapsToCallback = false;
        }
        
        private MessageType(
                int optionPaneMessageType,
                int confirmationCallbackMessageType,
                int textOutputCallbackMessageType) {
            mapsToCallback = true;
            this.optionPaneMessageType = optionPaneMessageType;
            this.confirmationCallbackMessageType = confirmationCallbackMessageType;
            this.textOutputCallbackMessageType = textOutputCallbackMessageType;
        }
        
        public int getOptionPaneMessageType() {
            return optionPaneMessageType;
        }
        
        public boolean exceeds(MessageType ms) {
            return (ms == null) || ordinal() > ms.ordinal();
        }
        
        public static MessageType severityForConfirmation(
                int confirmationCallbackMessageType) {
            for (MessageType ms : values()) {
                if (ms.mapsToCallback && ms.confirmationCallbackMessageType == confirmationCallbackMessageType) {
                    return ms;
                }
            }
            throw new IllegalArgumentException(Integer.toString(confirmationCallbackMessageType));
        }
        
        public static MessageType severityForTextOutput(
                int textOutputCallbackMessageType) {
            for (MessageType ms : values()) {
                if (ms.mapsToCallback && ms.textOutputCallbackMessageType == textOutputCallbackMessageType) {
                    return ms;
                }
            }
            throw new IllegalArgumentException(Integer.toString(textOutputCallbackMessageType));
        }
    }
}
