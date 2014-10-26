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

import com.google.common.collect.ImmutableList;
import org.chaston.oakfunds.storage.RecordType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO(mchaston): write JavaDocs
 */
class SinglePermissionAssertionImpl implements SinglePermissionAssertion {

  private final AccessCounterMap accessCounterMap;
  private final boolean firstAssertion;
  private final List<AtomicInteger> actionTypeCounts;

  public SinglePermissionAssertionImpl(
      AccessCounterMap accessCounterMap,
      Permission permission) {
    this.accessCounterMap = accessCounterMap;
    firstAssertion = accessCounterMap.initialize();

    ImmutableList.Builder<AtomicInteger> actionTypeCounts = ImmutableList.builder();
    for (Map.Entry<RecordType, ActionType> entry : permission.getRelatedActions().entries()) {
      Map<ActionType, AtomicInteger> actionCounters = accessCounterMap.get(entry.getKey());
      AtomicInteger actionTypeCount = actionCounters.get(entry.getValue());
      if (actionTypeCount == null) {
        actionTypeCount = new AtomicInteger();
        actionCounters.put(entry.getValue(), actionTypeCount);
      }
      actionTypeCount.incrementAndGet();
      actionTypeCounts.add(actionTypeCount);
    }
    this.actionTypeCounts = actionTypeCounts.build();
  }

  @Override
  public void close() {
    for (AtomicInteger actionTypeCount : actionTypeCounts) {
      actionTypeCount.decrementAndGet();
    }
    if (firstAssertion) {
      accessCounterMap.clear();
    }
  }
}
