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
import org.chaston.oakfunds.storage.Record;
import org.chaston.oakfunds.storage.RecordType;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO(mchaston): write JavaDocs
 */
class AuthorizationContextImpl implements AuthorizationContext {

  private final PermissionRegistry permissionRegistry;
  private final AuthenticationManagerImpl authenticationManager;

  @Inject
  AuthorizationContextImpl(
      PermissionRegistry permissionRegistry,
      AuthenticationManagerImpl authenticationManager) {
    this.permissionRegistry = permissionRegistry;
    this.authenticationManager = authenticationManager;
  }

  @Override
  public SinglePermissionAssertion assertPermission(String permissionName) {
    if (!currentUserHasPermission(permissionName)) {
      throw throwAuthorizationException(permissionName);
    }
    Permission permission = permissionRegistry.getPermission(permissionName);
    if (permission == null) {
      throw new IllegalArgumentException("Permission " + permissionName + " does not exist.");
    }
    return new SinglePermissionAssertionImpl(authenticationManager.getCurrentScope(), permission);
  }

  private boolean currentUserHasPermission(String permission) {
    return authenticationManager.getCurrentScope().hasPermission(permission);
  }

  @Override
  public <T extends Record> void assertAccess(RecordType<T> recordType, ActionType actionType) {
    AbstractAuthenticationScope authenticationScope = authenticationManager.getCurrentScope();
    Map<ActionType, AtomicInteger> actionTypeCounters =
        authenticationScope.getAccessCounters(recordType);
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

  private AuthorizationException throwAuthorizationException(String permission) {
    throw new AuthorizationException("User does not have permission " + permission + ".");
  }
}
