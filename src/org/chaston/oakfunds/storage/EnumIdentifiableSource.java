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

import java.util.EnumSet;

/**
 * TODO(mchaston): write JavaDocs
 */
public class EnumIdentifiableSource<E extends Enum & Identifiable> implements IdentifiableSource {

  private final Class<E> enumClass;
  private final ImmutableMap<Byte, E> valuesByIdentifier;
  private final ImmutableMap<String, E> valuesByJson;

  public EnumIdentifiableSource(Class<E> enumClass) {
    this.enumClass = enumClass;
    ImmutableMap.Builder<Byte, E> valuesByIdentifierBuilder = ImmutableMap.builder();
    ImmutableMap.Builder<String, E> valuesByJsonBuilder = ImmutableMap.builder();
    EnumSet<? extends E> enumSet = EnumSet.allOf(enumClass);
    for (E enumValue : enumSet) {
      valuesByIdentifierBuilder.put(enumValue.identifier(), enumValue);
      valuesByJsonBuilder.put(enumValue.toJson(), enumValue);
    }
    valuesByIdentifier = valuesByIdentifierBuilder.build();
    valuesByJson = valuesByJsonBuilder.build();
  }

  @Override
  public Identifiable lookup(byte identifier) {
    Identifiable identifiable = valuesByIdentifier.get(identifier);
    if (identifiable != null) {
      return identifiable;
    }
    throw new IllegalArgumentException(
        "No such " + enumClass.getSimpleName() + " identifier: " + identifier);
  }

  @Override
  public Identifiable fromJson(String json) {
    Identifiable identifiable = valuesByJson.get(json);
    if (identifiable != null) {
      return identifiable;
    }
    throw new IllegalArgumentException(
        "No such " + enumClass.getSimpleName() + " json value: " + json);
  }
}
