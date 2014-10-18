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
  @Nullable
  private final RecordType<?> containingType;
  private final RecordTemporalType temporalType;
  private final RecordType parentType;
  private final boolean isFinalType;
  private final ImmutableMap<String, AttributeType> attributes;

  public RecordType(String name, Class<T> recordTypeClass, RecordType<? super T> parentType, boolean isFinalType) {
    this.name = name;
    this.recordTypeClass = recordTypeClass;
    this.containingType = parentType.getRootType().getContainingType();
    this.temporalType = parentType.getRootType().getTemporalType();
    this.parentType = parentType;
    this.isFinalType = isFinalType;
    this.attributes = buildAttributes(recordTypeClass, parentType.getRecordTypeClass());
  }

  public RecordType(String name, Class<T> recordTypeClass,
      @Nullable RecordType<?> containingType, RecordTemporalType temporalType, boolean isFinalType) {
    this.name = name;
    this.recordTypeClass = recordTypeClass;
    this.containingType = containingType;
    this.temporalType = temporalType;
    if (temporalType == RecordTemporalType.INSTANT) {
      Preconditions.checkArgument(containingType != null, "Instant types must have a containing type.");
    }
    if (temporalType == RecordTemporalType.INTERVAL) {
      Preconditions.checkArgument(containingType != null, "Interval types must have a containing type.");
    }
    this.parentType = null;
    this.isFinalType = isFinalType;
    this.attributes = buildAttributes(recordTypeClass, null);
  }

  private static ImmutableMap<String, AttributeType> buildAttributes(Class<?> recordTypeClass,
      @Nullable Class<?> parentClass) {
    Map<String, AttributeType> attributes = new HashMap<>();
    extractAttributesFromMethods(attributes, recordTypeClass);
    for (Class<?> interfaceClass : recordTypeClass.getInterfaces()) {
      if (!interfaceClass.equals(parentClass)) {
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

  public boolean isFinalType() {
    return isFinalType;
  }

  public Class<T> getRecordTypeClass() {
    return recordTypeClass;
  }

  public String getName() {
    return name;
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
}
