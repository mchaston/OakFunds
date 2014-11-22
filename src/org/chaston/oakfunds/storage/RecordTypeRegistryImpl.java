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
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * TODO(mchaston): write JavaDocs
 */
public class RecordTypeRegistryImpl implements RecordTypeRegistry {

  private final ImmutableMap<String, RecordType> recordTypes;
  private final ImmutableMap<String, RecordValidator> recordValidators;
  private final ImmutableMultimap<String, RecordType> assignableRecordTypes;

  @Inject
  RecordTypeRegistryImpl(Set<RecordType> recordTypes) {
    Map<String, RecordType> recordTypesBuilder = new HashMap<>();
    Map<String, RecordValidator> recordValidatorsBuilder = new HashMap<>();
    Multimap<String, RecordType> assignableRecordTypesBuilder =
        MultimapBuilder.hashKeys().hashSetValues().build();
    for (RecordType recordType : recordTypes) {
      if (recordTypesBuilder.containsKey(recordType.getName())) {
        throw new IllegalStateException(
            "RecordType " + recordType.getName() + " was bound more than once.");
      }
      recordTypesBuilder.put(recordType.getName(), recordType);
      recordValidatorsBuilder.put(recordType.getName(), new RecordValidator(recordType));
      assignableRecordTypesBuilder.put(recordType.getName(), recordType);
      RecordType parentType = recordType.getParentType();
      while (parentType != null) {
        assignableRecordTypesBuilder.put(parentType.getName(), recordType);
        parentType = parentType.getParentType();
      }
    }
    this.recordTypes = ImmutableMap.copyOf(recordTypesBuilder);
    this.recordValidators = ImmutableMap.copyOf(recordValidatorsBuilder);
    this.assignableRecordTypes = ImmutableMultimap.copyOf(assignableRecordTypesBuilder);
  }

  @Override
  public void validateRecordAttributes(RecordType<?> recordType, Map<String, Object> attributes)
      throws StorageException {
    RecordValidator recordValidator = recordValidators.get(recordType.getName());
    if (recordValidator == null) {
      throw new IllegalStateException(
          "RecordType " + recordType.getName() + " was not bound.");
    }
    recordValidator.validateAttributes(attributes);
  }

  @Override
  public <T extends Record> RecordType<T> getType(String name, RecordType<T> parentRecordType) {
    RecordType<?> recordType = recordTypes.get(name);
    if (recordType.isTypeOf(parentRecordType)) {
      return (RecordType<T>) recordType;
    }
    throw new IllegalArgumentException("RecordType " + name
        + " is not a subtype of " + parentRecordType.getName() + ".");
  }

  @Override
  public Iterable<RecordType> getAssignableTypes(RecordType recordType) {
    return assignableRecordTypes.get(recordType.getName());
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
      extractAttributes(recordType);
    }

    private void extractAttributes(RecordType<?> recordType) {
      for (AttributeType attributeType : recordType.getAttributes().values()) {
        attributeValidators.put(attributeType.getName(),
            new AttributeValidator(attributeType.getName(), attributeType.getType(),
                attributeType.isRequired()));
      }
      if (recordType.getParentType() != null) {
        extractAttributes(recordType.getParentType());
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
          if (!type.isAssignableFrom(value.getClass())) {
            throw new StorageException(
                "Attribute " + attribute + " on RecordType " + recordType + " is of type "
                    + type + ", but an object of type " + value.getClass() + " was provided.");
          }
        }
      }
    }
  }
}
