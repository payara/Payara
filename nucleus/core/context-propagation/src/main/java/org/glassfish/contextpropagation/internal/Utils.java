/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.contextpropagation.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.glassfish.contextpropagation.ContextLifecycle;
import org.glassfish.contextpropagation.ContextMap;
import org.glassfish.contextpropagation.ContextViewFactory;
import org.glassfish.contextpropagation.InsufficientCredentialException;
import org.glassfish.contextpropagation.Location;
import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.View;
import org.glassfish.contextpropagation.ViewCapable;
import org.glassfish.contextpropagation.bootstrap.ContextAccessController;
import org.glassfish.contextpropagation.bootstrap.ContextBootstrap;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.Level;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.MessageID;
import org.glassfish.contextpropagation.internal.Entry.ContextType;
import org.glassfish.contextpropagation.internal.SimpleMap.Filter;
import org.glassfish.contextpropagation.spi.ContextMapPropagator;
import org.glassfish.contextpropagation.wireadapters.WireAdapter;

public class Utils {
  static AccessControlledMapFinder mapFinder = new AccessControlledMapFinder();
  private static final boolean IS_NOT_ORIGINATOR = false;

  private static final Filter propagationModeFilter = new Filter() {
    @Override
    public boolean keep(Map.Entry<String, Entry> mapEntry,
        PropagationMode mode) {
      return mapEntry.getValue().getPropagationModes().contains(mode);
    }
  };
  private static final Filter onewayPropagationModeFilter = new Filter() {
    @Override
    public boolean keep(Map.Entry<String, Entry> mapEntry,
        PropagationMode mode) {
      EnumSet<PropagationMode> modes = mapEntry.getValue().getPropagationModes();
      return modes.contains(mode) && !modes.contains(PropagationMode.ONEWAY);
    }    
  };
  protected static final Long DUMMY_VALUE = 1L;

  private static interface OriginatorFinder {
    boolean isOriginator(String key);
  }

  /**
   * 
   * @return The in-scope instance of ContextMapPropagator so that 
   * communication protocols can ask the ContextMapPropagator to handle
   * the context propagation bytes on the wire.
   */
  public static ContextMapPropagator getScopeAwarePropagator() {
    return new ContextMapPropagator() {

      private WireAdapter wireAdapter = ContextBootstrap.getWireAdapter();

      private SimpleMap getMapIfItExists() {
        AccessControlledMap map = mapFinder.getMapIfItExists();
        return map == null ? null : map.simpleMap;
      }

      private SimpleMap getMapAndCreateIfNeeded() {
        return mapFinder.getMapAndCreateIfNeeded().simpleMap;
      }

      @Override
      public void sendRequest(OutputStream out, PropagationMode propagationMode)
          throws IOException {
        sendItems(propagationModeFilter, out, propagationMode, true);
      }

      @Override
      public void sendResponse(OutputStream out, PropagationMode propagationMode)
          throws IOException {
        sendItems(onewayPropagationModeFilter, out, propagationMode, false);
      }

      private void sendItems(Filter filter, OutputStream out, PropagationMode propagationMode, boolean sendLocation)
          throws IOException {
        ContextBootstrap.debug(MessageID.PROPAGATION_STARTED, "Outgoing");
        SimpleMap map = getMapIfItExists();
        if (map != null) {
          ContextBootstrap.debug(MessageID.USING_WIRE_ADAPTER, "Writing to", wireAdapter);
          wireAdapter.prepareToWriteTo(out);
          Iterator<Map.Entry<String, Entry>> items = map.iterator(filter, propagationMode);
          while (items.hasNext()) {
            Map.Entry<String, Entry> mapEntry = items.next();
            Entry entry = mapEntry.getValue();
            Object value = entry.getValue();
            if (value instanceof ContextLifecycle) {
              ((ContextLifecycle) value).contextToPropagate();
            }
          }
          items = map.iterator(filter, propagationMode);
          while (items.hasNext()) {
            Map.Entry<String, Entry> mapEntry = items.next();
            wireAdapter.write(mapEntry.getKey(), mapEntry.getValue());
          }
          wireAdapter.flush();
        }
        ContextBootstrap.debug(MessageID.PROPAGATION_COMPLETED, "Outgoing");
      }

      @Override
      public void receiveRequest(InputStream in) throws IOException {
        receive(in, new OriginatorFinder() {
          public boolean isOriginator(String key) { return IS_NOT_ORIGINATOR; }
        });
      }

      private void receive(InputStream in, OriginatorFinder origFinder) throws IOException {
        ContextBootstrap.debug(MessageID.PROPAGATION_STARTED, "Ingoing");
        ContextAccessController accessController = ContextBootstrap.getContextAccessController();
        wireAdapter.prepareToReadFrom(in);
        SimpleMap map = getMapAndCreateIfNeeded();
        map.prepareToPropagate();
        for (String key = wireAdapter.readKey(); key != null; key = wireAdapter.readKey()) {
          try {
            Entry entry = wireAdapter.readEntry();
            if (entry == null) {
              break;
            } else {
              entry.init(origFinder.isOriginator(key), accessController.isEveryoneAllowedToRead(key));
              map.put(key, entry);
            }
          } catch (ClassNotFoundException e) {
            ContextBootstrap.getLoggerAdapter().log(Level.ERROR, e,
                MessageID.ERROR_UNABLE_TO_INSTANTIATE_CONTEXT_FROM_THE_WIRE);
          }
        }
        for (ContextLifecycle context : map.getAddedContextLifecycles()) {
          context.contextAdded();
        }
        ContextBootstrap.debug(MessageID.PROPAGATION_COMPLETED, "Ingoing");
      }

      @Override
      public void receiveResponse(InputStream in, PropagationMode mode) throws IOException {
        SimpleMap map = getMapAndCreateIfNeeded();
        final Set<String> keySet = clearPropagatedEntries(mode, map);
        ContextBootstrap.debug(MessageID.CLEARED_ENTRIES, keySet);
        receive(in, new OriginatorFinder() {
          @Override public boolean isOriginator(String key) { 
            return keySet.contains(key);
          }
        });
      }

      private Set<String> clearPropagatedEntries(PropagationMode mode, SimpleMap map) {
        Set<String> keySet = new HashSet<String>();
        Iterator<Map.Entry<String, Entry>> iterator = map.iterator(
            new Filter() {
              @Override
              public boolean keep(Map.Entry<String, Entry> mapEntry,
                  PropagationMode mode) {
                EnumSet<PropagationMode> modes = mapEntry.getValue().propagationModes;
                return modes.contains(mode);
              }
            }, mode);
        while(iterator.hasNext()) {
          keySet.add(iterator.next().getKey());
          iterator.remove();
        }
        return keySet;
      }

      /**
       * Replaces the in-scope ContextMap entries with those in the srcContexts
       * that have the THREAD propagation mode.
       */
      @Override
      public void restoreThreadContexts(final AccessControlledMap srcContexts) {
        if (ContextBootstrap.IS_DEBUG) {
          ContextBootstrap.debug(MessageID.RESTORING_CONTEXTS, asList(srcContexts.entryIterator()));
        }
        if (srcContexts == null) {
          throw new IllegalArgumentException("You must specify a ContextMap.");
        } 
        SimpleMap srcSimpleMap = srcContexts.simpleMap;
        if (!srcSimpleMap.map.isEmpty()) {
          SimpleMap destSimpleMap = mapFinder.getMapAndCreateIfNeeded().simpleMap;
          destSimpleMap.prepareToPropagate();
          if (destSimpleMap == srcSimpleMap) {
            throw new IllegalArgumentException("Cannot restore a ContextMap on itself. The source and destination maps must not be the same.");
          }
          Iterator<Map.Entry<String, Entry>> iterator = 
              srcSimpleMap.iterator(propagationModeFilter, PropagationMode.THREAD);
          while (iterator.hasNext()) {
            Map.Entry<String, Entry> mapEntry = iterator.next();
            destSimpleMap.put(mapEntry.getKey(), mapEntry.getValue());
          }
          for (ContextLifecycle context : destSimpleMap.getAddedContextLifecycles()) {
            context.contextAdded();
          }
          if (ContextBootstrap.IS_DEBUG) {
            ContextBootstrap.debug(MessageID.RESTORING_CONTEXTS, 
                asList(mapFinder.getMapIfItExists().entryIterator()));
          }
        }
      }

      private LinkedList<String> asList(final Iterator<Map.Entry<String, Entry>> mapEntries) {
        LinkedList<String> list = new LinkedList<String>();
        while (mapEntries.hasNext()) {
          Map.Entry<String, Entry> mapEntry = mapEntries.next();
          list.add(mapEntry.getKey() + ": " + mapEntry.getValue());
        }
        return list;
      }

      @Override
      public void useWireAdapter(WireAdapter aWireAdapter) {
        wireAdapter = aWireAdapter;        
      }

    }; 
  }

  protected static class AccessControlledMapFinder {

    protected AccessControlledMap getMapIfItExists() {
      return ContextBootstrap.getThreadLocalAccessor().get();
    }

    protected final AccessControlledMap getMapAndCreateIfNeeded() {
      AccessControlledMap map = getMapIfItExists();
      return map == null ? createMap() : map;
    }

    private synchronized AccessControlledMap createMap() {
      AccessControlledMap map = ContextBootstrap.getThreadLocalAccessor().get();
      if (map == null) {
        map = new AccessControlledMap();
        ContextBootstrap.debug(MessageID.CREATING_NEW_CONTEXT_MAP, map);
        ContextBootstrap.getThreadLocalAccessor().set(map);
      }
      return map;
    }
  }

  private static class ContextMapImpl implements ContextMap, ContextMapAdditionalAccessors, PrivilegedWireAdapterAccessor {

    @SuppressWarnings("unchecked")
    public <T> T get(String key) throws InsufficientCredentialException {
      return (T) (mapFinder.getMapIfItExists() == null ? 
          null : mapFinder.getMapIfItExists().get(key));
    }

    public <T> T put(String key, String value,
        EnumSet<PropagationMode> propagationModes) throws InsufficientCredentialException {
      return (T) mapFinder.getMapAndCreateIfNeeded().put(
          key,  new Entry(value , propagationModes, 
              isAsciiString(value) ?Entry.ContextType.ASCII_STRING : Entry.ContextType.STRING));
    }

    public <T> T putNotAscii(String key, String value,
        EnumSet<PropagationMode> propagationModes) throws InsufficientCredentialException {
      return (T) mapFinder.getMapAndCreateIfNeeded().put(
          key, new Entry(value , propagationModes, Entry.ContextType.STRING));
    }

    public <T> T putAscii(String key, String value,
        EnumSet<PropagationMode> propagationModes) throws InsufficientCredentialException {
      if (isAsciiString(value)) {
        return (T) mapFinder.getMapAndCreateIfNeeded().put(
            key, new Entry(value , propagationModes, Entry.ContextType.ASCII_STRING));
      } else {
        throw new IllegalArgumentException("The specified string is not an ascii string: " + value);
      }
    }

    public <T, U extends Number> T put(String key, U value,
        EnumSet<PropagationMode> propagationModes) throws InsufficientCredentialException {
      return (T) mapFinder.getMapAndCreateIfNeeded().put(
          key,  new Entry(value , propagationModes, Entry.ContextType.fromNumberClass(value.getClass())));
    }

    public <T> T put(String key, Boolean value,
        EnumSet<PropagationMode> propagationModes) throws InsufficientCredentialException {
      return (T) mapFinder.getMapAndCreateIfNeeded().put(
          key,  new Entry(value , propagationModes, Entry.ContextType.BOOLEAN));
    }

    public <T extends ViewCapable> T createViewCapable(String prefix) throws InsufficientCredentialException {
      return (T) createViewCapable(prefix, true);
    }

    public <T extends ViewCapable> T createViewCapable(String prefix, boolean isOriginator) throws InsufficientCredentialException {
      ContextViewFactory factory = (ContextViewFactory) getFactory(prefix);
      if (factory == null) {
        throw new IllegalStateException("Unable to create ViewCapable object for prefix, " + prefix);
      } else {
        ViewImpl view = new ViewImpl(prefix); 
        Entry entry = Entry.createViewEntryInstance(DUMMY_VALUE , EnumSet.of(PropagationMode.LOCAL), 
            view).init(isOriginator, ContextBootstrap.getContextAccessController().isEveryoneAllowedToRead(prefix));
        mapFinder.getMapAndCreateIfNeeded().put(prefix,  entry);
        entry.value = factory.createInstance(view);
        entry.propagationModes = factory.getPropagationModes();
        return (T) entry.getValue();
      }
    }

    public <T> T putSerializable(String key, Serializable value,
        EnumSet<PropagationMode> propagationModes) throws InsufficientCredentialException {
      return (T) mapFinder.getMapAndCreateIfNeeded().put(
          key,  new Entry(value , propagationModes, Entry.ContextType.SERIALIZABLE));
    }

    public EnumSet<PropagationMode> getPropagationModes(String key) throws InsufficientCredentialException {
      return mapFinder.getMapIfItExists() == null ? 
          null : mapFinder.getMapIfItExists().getPropagationModes(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T remove(String key) throws InsufficientCredentialException {
      return (T) (mapFinder.getMapIfItExists() == null ? 
          null : mapFinder.getMapIfItExists().remove(key));
    }

    @Override
    public <T> T put(String name, Character context,
        EnumSet<PropagationMode> propagationModes)
            throws InsufficientCredentialException {
      return (T) mapFinder.getMapAndCreateIfNeeded().put(
          name, new Entry(context , propagationModes, Entry.ContextType.BYTE));
    }

    @Override
    public Location getLocation() {
      try {
        Location location =  get(Location.KEY);
        if (location == null) {
          //throw new AssertionError("No location set");
          location = new Location(new ViewImpl(Location.KEY)) {};
          final Entry locationEntry = new Entry(location, 
              Location.PROP_MODES, ContextType.VIEW_CAPABLE).init(true, false);
          mapFinder.getMapAndCreateIfNeeded().put(Location.KEY, locationEntry);
        }
        return location;
      } catch (InsufficientCredentialException IgnoreSecurityBypassed) {
        throw new AssertionError(Location.KEY + " should have read access for all.");
      }
    }

    @Override
    public boolean isEmpty() {
      AccessControlledMap acMap = mapFinder.getMapIfItExists();
      return acMap == null ? true : acMap.simpleMap.map.isEmpty();
    }

    @Override
    public Iterator<String> names() {
      AccessControlledMap acMap = mapFinder.getMapIfItExists();
      return acMap == null ? null : acMap.names();
    }
    
    public interface StringFilter {
      public boolean accept(String s);
    }
    
    public Iterator<String>names(final StringFilter stringFilter) {
      return new Iterator<String>() {
        Iterator<String> it = names();
        String next;
        
        @Override public boolean hasNext() {
          if (next == null && it.hasNext()) {
            while (it.hasNext()) {
              String name = it.next(); 
              if (stringFilter.accept(name)) {
                next = name;
                break;
              }
            }
          }
          return next != null;
        }

        @Override public String next() {
          if (next == null) throw new NoSuchElementException();
          String name = next;
          next = null;
          return name;
        }

        @Override public void remove() {
          throw new UnsupportedOperationException();
        }
        
      };
    }

    public AccessControlledMap getAccessControlledMap(boolean create) {
      return create ? mapFinder.getMapAndCreateIfNeeded() : mapFinder.getMapIfItExists();     
    }

    @Override
    public AccessControlledMap getAccessControlledMap() {
      return mapFinder.getMapIfItExists();
    }
  }

  public static ContextMap getScopeAwareContextMap() {
    return new ContextMapImpl();
  }

  static Map<String, ContextViewFactory> viewFactoriesByPrefix = new HashMap<String, ContextViewFactory>();

  /**
   * ViewCapable objects are created by the context propagation framework 
   * when needed using the ContextViewFactory registered against the
   * specified context name
   * @param prefixName This is the name of the context that should be instantiated
   * with the corresponding factory. 
   * @param factory A ContextViewFactory.
   */
  public static void registerContextFactoryForPrefixNamed(String prefixName, ContextViewFactory factory) {
    Utils.validateFactoryRegistrationArgs("prefixName", 
        MessageID.WARN_FACTORY_ALREADY_REGISTERED_FOR_PREFIX, prefixName, 
        factory, viewFactoriesByPrefix);
    viewFactoriesByPrefix.put(prefixName, factory);
  }

  static ContextViewFactory getFactory(String prefix) {
    return viewFactoriesByPrefix.get(prefix);
  }

  /*
   * Utility method to validate a work context factory.
   */
  public static void validateFactoryRegistrationArgs(String key, MessageID messageID, String contextClassName,
      Object factory, Map<String, ?> factoriesByKey) {
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("Invalid key: " + key + ".");
    }
    if (contextClassName == null || contextClassName.isEmpty()) {
      throw new IllegalArgumentException("Invalid contextClassName: " + contextClassName + ".");
    }
    if (messageID == null) {
      throw new IllegalArgumentException("Must specify a messageID");
    }
    if (factory == null) {
      throw new IllegalArgumentException("You must specify a valid ContextFactory.");
    }
    if (factoriesByKey.containsKey(contextClassName)) {
      ContextBootstrap.getLoggerAdapter().log(
          Level.WARN, messageID, contextClassName, factoriesByKey.get(contextClassName), factory);
    } 
  }

  static { // Register Default Factories
    registerContextFactoryForPrefixNamed(Location.KEY, new ContextViewFactory() {
      @Override
      public ViewCapable createInstance(final View aView) {
        return new Location(aView) {};
      }
      @Override
      public EnumSet<PropagationMode> getPropagationModes() {
        return Location.PROP_MODES;
      }
    });
  }

  public interface ContextMapAdditionalAccessors {
    /**
     * Store the specified work context under the specified name into the in-scope ContextMap.
     * @param name The name to associate to the specified work context
     * @param context an ascii String work context.
     * @param propagationModes A set of propagation modes that control over 
     * which protocol this work context will be propagated.
     * @return The work context being replaced.
     * @throws InsufficientCredentialException If the user has insufficient 
     * privileges to access that work context.
     */  
    <T> T putAscii(String name, String context, EnumSet<PropagationMode> propagationModes) throws InsufficientCredentialException;

    public <T> T putNotAscii(String key, String value,
        EnumSet<PropagationMode> propagationModes) throws InsufficientCredentialException;

    public <T> T putSerializable(String key, Serializable value,
        EnumSet<PropagationMode> propagationModes) throws InsufficientCredentialException;
  }

  public interface PrivilegedWireAdapterAccessor {
    public <T extends ViewCapable> T createViewCapable(String prefix, boolean isOriginator) throws InsufficientCredentialException;
    public AccessControlledMap getAccessControlledMap(boolean create);
  }

  public static boolean isAsciiString(String s) {
    final int length = s.length();
    for (int offset = 0; offset < length; ) {
      final int codepoint = s.codePointAt(offset);

      if (codepoint >= 128) return false;

      offset += Character.charCount(codepoint);
    }
    return true;
  }

  public static String toString(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      if (b >= 32 && b < 127) {
        sb.append((char) b);
      } else {
        sb.append('~');
      }
    }
    return sb.toString();
  }

}
