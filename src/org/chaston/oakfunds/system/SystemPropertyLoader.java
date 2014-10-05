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
package org.chaston.oakfunds.system;

import com.google.common.collect.ImmutableMap;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
abstract class SystemPropertyLoader {

  private final String name;

  SystemPropertyLoader(String name) {
    this.name = name;
  }

  void load(Store store) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(SystemPropertiesManagerImpl.ATTRIBUTE_NAME, name);
    attributes.putAll(getOtherAttributes());
    store.createRecord(SystemProperty.TYPE, attributes);
  }

  abstract Map<? extends String,?> getOtherAttributes();

  static SystemPropertyLoader createIntegerProperty(String name, final int value) {
    return new SystemPropertyLoader(name) {
      @Override
      Map<String, Integer> getOtherAttributes() {
        return ImmutableMap.of(SystemPropertiesManagerImpl.ATTRIBUTE_INTEGER_VALUE, value);
      }
    };
  }
}
