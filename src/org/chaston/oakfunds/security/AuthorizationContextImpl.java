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

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.chaston.oakfunds.storage.Record;
import org.chaston.oakfunds.storage.RecordType;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO(mchaston): write JavaDocs
 */
class AuthorizationContextImpl implements AuthorizationContext {

  private final Provider<Map<RecordType, Map<ActionType, AtomicInteger>>> accessCountersProvider;
  private final SinglePermissionAssertionFactory singlePermissionAssertionFactory;

  @Inject
  AuthorizationContextImpl(
      Provider<Map<RecordType, Map<ActionType, AtomicInteger>>> accessCountersProvider,
      SinglePermissionAssertionFactory singlePermissionAssertionFactory) {
    this.accessCountersProvider = accessCountersProvider;
    this.singlePermissionAssertionFactory = singlePermissionAssertionFactory;
  }

  @Override
  public <T extends Record> SinglePermissionAssertion assertPermission(String permission) {
    // TODO: check against the current user to see if they have this permission
    return singlePermissionAssertionFactory.create(permission);
  }

  @Override
  public <T extends Record> void assertAccess(RecordType<T> recordType, ActionType actionType) {
    Map<RecordType, Map<ActionType, AtomicInteger>> accessCounters = accessCountersProvider.get();
    if (accessCounters == null) {
      throw throwAuthorizationException(recordType, actionType);
    }
    Map<ActionType, AtomicInteger> actionTypeCounters = accessCounters.get(recordType);
    if (actionTypeCounters == null) {
      throw throwAuthorizationException(recordType, actionType);
    }
    // Check for direct access.
    AtomicInteger actionTypeCounter = actionTypeCounters.get(actionType);
    if (actionTypeCounter != null && actionTypeCounter.get() > 0) {
      // Found a match.
      return;
    }
    // Check for implied access.
    for (ActionType impliedActionType : actionType.getImpliedActions()) {
      actionTypeCounter = actionTypeCounters.get(impliedActionType);
      if (actionTypeCounter != null && actionTypeCounter.get() > 0) {
        // Found a match.
        return;
      }
    }
    throw throwAuthorizationException(recordType, actionType);
  }

  private <T extends Record> AuthorizationException throwAuthorizationException(
      RecordType<T> recordType, ActionType actionType) {
    throw new AuthorizationException("User has not been given access to "
        + actionType + " records of type " + recordType.getName() + ".");
  }
}
