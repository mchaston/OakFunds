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

import javax.annotation.Nullable;

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

  public RecordType(String name, Class<T> recordTypeClass, RecordType<? super T> parentType, boolean isFinalType) {
    this.name = name;
    this.recordTypeClass = recordTypeClass;
    this.containingType = parentType.getRootType().getContainingType();
    this.temporalType = parentType.getRootType().getTemporalType();
    this.parentType = parentType;
    this.isFinalType = isFinalType;
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
  }

  public RecordTemporalType getTemporalType() {
    return temporalType;
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
}
