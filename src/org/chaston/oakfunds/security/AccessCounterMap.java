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
package org.chaston.oakfunds.security;

import org.chaston.oakfunds.storage.RecordType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO(mchaston): write JavaDocs
 */
class AccessCounterMap {

  private final ThreadLocal<Map<RecordType, Map<ActionType, AtomicInteger>>>
      accessCounterMapThreadLocal = new ThreadLocal<>();

  public Map<ActionType, AtomicInteger> get(RecordType<?> recordType) {
    Map<RecordType, Map<ActionType, AtomicInteger>> accessCounterMap =
        accessCounterMapThreadLocal.get();
    if (accessCounterMap == null) {
      throw new IllegalStateException("Not initialized.");
    }
    Map<ActionType, AtomicInteger> accessCounters = accessCounterMap.get(recordType);
    if (accessCounters == null) {
      accessCounters = new HashMap<>();
      accessCounterMap.put(recordType, accessCounters);
    }
    return accessCounters;
  }

  public boolean initialize() {
    Map<RecordType, Map<ActionType, AtomicInteger>> accessCounterMap =
        accessCounterMapThreadLocal.get();
    if (accessCounterMap == null) {
      accessCounterMapThreadLocal.set(new HashMap<RecordType, Map<ActionType, AtomicInteger>>());
      return true;
    } else {
      return false;
    }
  }

  public void clear() {
    accessCounterMapThreadLocal.remove();
  }
}
