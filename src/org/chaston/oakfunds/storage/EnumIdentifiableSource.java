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
  private final ImmutableMap<Byte, E> values;

  public EnumIdentifiableSource(Class<E> enumClass) {
    this.enumClass = enumClass;
    ImmutableMap.Builder<Byte, E> valuesBuilders = ImmutableMap.builder();
    EnumSet<? extends E> enumSet = EnumSet.allOf(enumClass);
    for (E enumValue : enumSet) {
      valuesBuilders.put(enumValue.identifier(), enumValue);
    }
    values = valuesBuilders.build();
  }

  @Override
  public Identifiable lookup(byte identifier) {
    Identifiable identifiable = values.get(identifier);
    if (identifiable != null) {
      return identifiable;
    }
    throw new IllegalArgumentException(
        "No such " + enumClass.getSimpleName() + " identifier: " + identifier);
  }
}
