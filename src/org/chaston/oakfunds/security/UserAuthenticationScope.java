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

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * TODO(mchaston): write JavaDocs
 */
class UserAuthenticationScope extends AbstractAuthenticationScope {
  private final ImmutableSet<String> userPermissions;

  UserAuthenticationScope(
      AuthenticationManagerImpl authenticationManager, Set<String> userPermissions) {
    super(authenticationManager);

    this.userPermissions = ImmutableSet.copyOf(userPermissions);
  }

  @Override
  boolean hasPermission(String permissionName) {
    return userPermissions.contains(permissionName);
  }
}
