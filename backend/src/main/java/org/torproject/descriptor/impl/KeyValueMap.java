/* Copyright 2017--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.DescriptorParseException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.TreeMap;

public class KeyValueMap<T> extends TreeMap<String, T> {

  private static final long serialVersionUID = 1124527355143605927L;

  private Class<T> clazz;

  public KeyValueMap(Class<T> clazz) {
    super();
    this.clazz = clazz;
  }

  private void putPair(String key, T value, String line, String listElement,
      int keyLength) throws DescriptorParseException {
    if (this.containsKey(key)) {
      throw new DescriptorParseException("Line '" + line + "' contains "
          + "duplicate key '" + key + "'.");
    }
    if (key.isEmpty() || (keyLength > 0 && key.length() != keyLength)) {
      throw new DescriptorParseException("Line '" + line + "' contains an "
          + "illegal key in list element '" + listElement + "'.");
    }
    if (null == value) {
      throw new DescriptorParseException("Line '" + line + "' contains an "
          + "illegal value in list element '" + listElement + "'.");
    }
    this.put(key, value);
  }

  /** Extract key value maps of numbers and verify the key-value pairs. */
  public KeyValueMap<T> parseKeyValueList(String line, String[] partsNoOpt,
      int startIndex, int keyLength, String separatorPattern)
      throws DescriptorParseException {
    if (startIndex >= partsNoOpt.length) {
      return this;
    }
    boolean usingSpacePattern = " ".equals(separatorPattern);
    String[] keysAndValues = usingSpacePattern ? partsNoOpt
        : partsNoOpt[startIndex].split(separatorPattern, -1);
    for (int i = usingSpacePattern ? startIndex : 0; i < keysAndValues.length;
        i++) {
      String listElement = keysAndValues[i];
      String[] keyAndValue = listElement.split("=");
      String key = keyAndValue[0];
      T value = null;
      if (keyAndValue.length == 2) {
        try {
          Method method = clazz.getMethod("valueOf", String.class);
          value = (T) method.invoke(clazz, keyAndValue[1]);
        } catch (IllegalAccessException | SecurityException e) {
          throw new RuntimeException("This shouldn't happen.", e);
        } catch (IllegalArgumentException | InvocationTargetException e) {
          value = null;
        } catch (NoSuchMethodException e) { // use the String value
          value = (T) keyAndValue[1];
        }
      }
      this.putPair(key, value, line, listElement, keyLength);
    }
    return this;
  }

}
