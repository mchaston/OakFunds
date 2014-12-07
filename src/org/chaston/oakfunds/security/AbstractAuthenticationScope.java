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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO(mchaston): write JavaDocs
 */
abstract class AbstractAuthenticationScope implements AuthenticationScope {

  private final Map<RecordType, Map<ActionType, AtomicInteger>> accessCounterMap = new HashMap<>();
  private final SystemAuthenticationManagerImpl authenticationManager;

  protected AbstractAuthenticationScope(SystemAuthenticationManagerImpl authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  abstract boolean hasPermission(String permissionName);

  abstract Set<String> getPermissions();

  Map<ActionType, AtomicInteger> getAccessCounters(RecordType<?> recordType) {
    Map<ActionType, AtomicInteger> accessCounters = accessCounterMap.get(recordType);
    if (accessCounters == null) {
      accessCounters = new HashMap<>();
      accessCounterMap.put(recordType, accessCounters);
    }
    return accessCounters;
  }

  @Override
  public void close() {
    authenticationManager.endAuthenticationScope(this);
  }
}
