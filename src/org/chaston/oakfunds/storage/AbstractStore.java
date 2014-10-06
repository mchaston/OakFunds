/*
 * Copyright 2014 Miles Chaston
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.chaston.oakfunds.storage;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
abstract class AbstractStore implements Store {

  private final Map<String, RecordValidator> recordValidators = new HashMap<>();

  @Override
  public void registerType(RecordType<?> recordType) {
    if (recordValidators.containsKey(recordType.getName())) {
      throw new IllegalStateException(
          "RecordType " + recordType.getName() + " has already been registered.");
    }
    recordValidators.put(recordType.getName(), new RecordValidator(recordType));
  }

  protected void validateRecordAttributes(RecordType<?> recordType, Map<String, Object> attributes)
      throws StorageException {
    RecordValidator recordValidator = recordValidators.get(recordType.getName());
    if (recordValidator == null) {
      throw new IllegalStateException(
          "RecordType " + recordType.getName() + " has not ben registered.");
    }
    recordValidator.validateAttributes(attributes);
  }

  private static class RecordValidator {

    private static final ImmutableMap<Class<?>, Class<?>> NON_PRIMITIVES =
        ImmutableMap.<Class<?>, Class<?>>builder()
            .put(Boolean.TYPE, Boolean.class)
            .put(Character.TYPE, Character.class)
            .put(Byte.TYPE, Byte.class)
            .put(Short.TYPE, Short.class)
            .put(Integer.TYPE, Integer.class)
            .put(Long.TYPE, Long.class)
            .put(Float.TYPE, Float.class)
            .put(Double.TYPE, Double.class)
            .build();

    private final RecordType recordType;
    private final Map<String, AttributeValidator> attributeValidators = new HashMap<>();

    public RecordValidator(RecordType<?> recordType) {
      this.recordType = recordType;
      extractAttributes(recordType.getRecordTypeClass());
    }

    private void extractAttributes(Class<?> clazz) {
      for (Method method : clazz.getMethods()) {
        AttributeMethod attributeMethod = method.getAnnotation(AttributeMethod.class);
        if (attributeMethod != null) {
          attributeValidators.put(attributeMethod.attribute(),
              new AttributeValidator(attributeMethod.attribute(), method.getReturnType(),
                  attributeMethod.required()));
        }
      }
      for (Class<?> superInterface : clazz.getInterfaces()) {
        extractAttributes(superInterface);
      }
    }

    public void validateAttributes(Map<String, Object> attributes) throws StorageException {
      for (Map.Entry<String, Object> entry : attributes.entrySet()) {
        AttributeValidator attributeValidator = attributeValidators.get(entry.getKey());
        if (attributeValidator == null) {
          throw new StorageException(
              "Attribute " + entry.getKey()
                  + " is not a known attribute for RecordType " + recordType + ".");
        }
        attributeValidator.validate(entry.getValue());
      }
      for (Map.Entry<String, AttributeValidator> entry : attributeValidators.entrySet()) {
        if (entry.getValue().required) {
          if (!attributes.containsKey(entry.getKey())) {
            throw new StorageException(
                "Attribute " + entry.getKey() + " on RecordType " + recordType + " is required.");
          }
        }
      }
    }

    private class AttributeValidator {

      private final String attribute;
      private final Class<?> type;
      private final boolean required;

      private AttributeValidator(String attribute, Class<?> type, boolean required) {
        this.attribute = attribute;
        this.type = type.isPrimitive() ? NON_PRIMITIVES.get(type) : type;
        this.required = required;
      }

      public void validate(Object value) throws StorageException {
        if (value == null) {
          if (required) {
            throw new StorageException(
                "Attribute " + attribute + " on RecordType " + recordType + " is required.");
          }
        } else {
          if (!type.equals(value.getClass())) {
            throw new StorageException(
                "Attribute " + attribute + " on RecordType " + recordType + " is of type "
                    + type + ", but an object of type " + value.getClass() + " was provided.");
          }
        }
      }
    }
  }
}
