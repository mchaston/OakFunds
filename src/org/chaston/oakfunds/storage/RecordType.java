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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public class RecordType<T extends Record> {

  private final String name;
  private final Class<T> recordTypeClass;
  private final boolean autoIncrementId;
  @Nullable
  private final RecordType<?> containingType;
  private final RecordTemporalType temporalType;
  private final RecordType parentType;
  private final ImmutableMap<String, AttributeType> attributes;

  private RecordType(String name, Class<T> recordTypeClass,
      boolean autoIncrementId,
      @Nullable RecordType<? super T> parentType,
      @Nullable RecordType<?> containingType) {
    this.name = name;
    this.recordTypeClass = recordTypeClass;
    this.autoIncrementId = autoIncrementId;
    if (parentType != null && containingType != null) {
      throw new IllegalStateException(
          "A type cannot have a parent type and an explicitly declared containing type.");
    }
    if (parentType != null) {
      Preconditions.checkArgument(
          interfacesContain(recordTypeClass, parentType.getRecordTypeClass()),
          "Type " + recordTypeClass.getName() + " does not directly extend from "
              + parentType.getRecordTypeClass().getName() + ".");
      this.parentType = parentType;
      this.containingType = parentType.getContainingType();
      this.temporalType = parentType.getTemporalType();
    } else {
      this.parentType = null;
      this.containingType = containingType;
      if (interfacesContain(recordTypeClass, InstantRecord.class)) {
        this.temporalType = RecordTemporalType.INSTANT;
        Preconditions.checkArgument(containingType != null,
            "Instant types must have a containing type.");
      } else if (interfacesContain(recordTypeClass, IntervalRecord.class)) {
        this.temporalType = RecordTemporalType.INTERVAL;
        Preconditions.checkArgument(containingType != null,
            "Interval types must have a containing type.");
      } else if (interfacesContain(recordTypeClass, Record.class)) {
        this.temporalType = RecordTemporalType.NONE;
      } else {
        throw new IllegalArgumentException(
            "Type " + recordTypeClass.getName()
                + " must extend from one of the base record types.");
      }
    }
    this.attributes = buildAttributes(recordTypeClass,
        parentType == null ? null : parentType.getRecordTypeClass());
  }

  private static boolean interfacesContain(Class<?> superInterface,
      Class<?> expectedSubInterface) {
    for (Class<?> interfaceClass : superInterface.getInterfaces()) {
      if (interfaceClass.equals(expectedSubInterface)) {
        return true;
      }
    }
    return false;
  }

  private static ImmutableMap<String, AttributeType> buildAttributes(Class<?> recordTypeClass,
      @Nullable Class<?> parentClass) {
    Map<String, AttributeType> attributes = new HashMap<>();
    extractAttributesFromMethods(attributes, recordTypeClass);
    boolean seenParentClass = false;
    for (Class<?> interfaceClass : recordTypeClass.getInterfaces()) {
      if (interfaceClass.equals(parentClass)) {
        seenParentClass = true;
        continue;
      }
      ImmutableMap<String, AttributeType> superInterfaceAttributes =
          buildAttributes(interfaceClass, null);
      for (String otherAttribute : superInterfaceAttributes.keySet()) {
        if (attributes.containsKey(otherAttribute)) {
          throw new IllegalStateException("Attribute " + otherAttribute
              + " is declared more than once for type " + recordTypeClass.getName()
              + " via super types.");
        }
      }
      attributes.putAll(superInterfaceAttributes);
    }
    if (parentClass != null && !seenParentClass) {
      throw new IllegalStateException("Parent class " + parentClass.getName()
          + " was not seen while extracting attributes for " + recordTypeClass.getName()
          + ".  Was the parent type set correctly?");
    }
    return ImmutableMap.copyOf(attributes);
  }

  private static void extractAttributesFromMethods(Map<String, AttributeType> attributes,
      Class<?> clazz) {
    for (Method method : clazz.getDeclaredMethods()) {
      AttributeMethod attributeMethod = method.getAnnotation(AttributeMethod.class);
      if (attributeMethod != null) {
        if (attributes.containsKey(attributeMethod.attribute())) {
          throw new IllegalStateException("Attribute " + attributeMethod.attribute()
              + " is declared more than once for type " + clazz.getName() + ".");
        }
        attributes.put(attributeMethod.attribute(),
            new AttributeType(attributeMethod.attribute(),
                method.getReturnType(), attributeMethod.required()));
      }
    }
  }

  public RecordTemporalType getTemporalType() {
    return temporalType;
  }

  public RecordType getParentType() {
    return parentType;
  }

  public RecordType getRootType() {
    if (parentType != null) {
      return parentType.getRootType();
    }
    return this;
  }

  public Class<T> getRecordTypeClass() {
    return recordTypeClass;
  }

  public String getName() {
    return name;
  }

  public boolean isAutoIncrementId() {
    return autoIncrementId;
  }

  @Override
  public String toString() {
    return name;
  }

  public <T extends Record> boolean isTypeOf(RecordType<T> recordType) {
    return recordType.getRecordTypeClass().isAssignableFrom(recordTypeClass);
  }

  @Nullable
  public RecordType<?> getContainingType() {
    return containingType;
  }

  public ImmutableMap<String, AttributeType> getAttributes() {
    return attributes;
  }

  public static <T extends Record<T>> RecordTypeBuilder<T> builder(
      String name, Class<T> recordTypeClass) {
    return new RecordTypeBuilder<>(name, recordTypeClass);
  }

  public static class RecordTypeBuilder<T extends Record<T>> {
    private final String name;
    private final Class<T> recordTypeClass;
    private boolean autoIncrementId = true;
    private RecordType<?> containingType;
    private RecordType<? super T> parentType;

    private RecordTypeBuilder(String name, Class<T> recordTypeClass) {
      this.name = name;
      this.recordTypeClass = recordTypeClass;
    }

    public RecordTypeBuilder<T> withManualNumbering() {
      this.autoIncrementId = false;
      return this;
    }

    public RecordTypeBuilder<T> containedBy(RecordType<?> containingType) {
      this.containingType = containingType;
      return this;
    }

    public RecordTypeBuilder<T> extensionOf(RecordType<? super T> parentType) {
      this.parentType = parentType;
      return this;
    }

    public RecordType<T> build() {
      return new RecordType<T>(name, recordTypeClass, autoIncrementId, parentType, containingType);
    }
  }
}
