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
import com.google.inject.Inject;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO(mchaston): write JavaDocs
 */
class AuthenticationManagerImpl implements AuthenticationManager {

  private final ThreadLocal<AbstractAuthenticationScope> currentAuthenticationScope =
      new ThreadLocal<>();
  private final UserManager userManager;
  private final RoleRegistry roleRegistry;

  @Inject
  AuthenticationManagerImpl(UserManager userManager, RoleRegistry roleRegistry) {
    this.userManager = userManager;
    this.roleRegistry = roleRegistry;
  }

  @Override
  public AuthenticationScope authenticateUser() {
    if (currentAuthenticationScope.get() != null) {
      throw new IllegalStateException("Already withing an authentication scope.");
    }
    User user = userManager.getCurrentUser();
    if (user == null) {
      throw new IllegalStateException("No authenticated user available.");
    }
    Set<String> userPermissions = getUserPermissions(user);
    UserAuthenticationScope userAuthenticationScope =
        new UserAuthenticationScope(this, userPermissions);
    currentAuthenticationScope.set(userAuthenticationScope);
    return userAuthenticationScope;
  }

  private Set<String> getUserPermissions(User user) {
    Set<String> permissionNames = new HashSet<>();
    for (String roleName : user.getGrantedRoleNames()) {
      Role role = roleRegistry.getRole(roleName);
      permissionNames.addAll(role.getPermissionNames());
    }
    return ImmutableSet.copyOf(permissionNames);
  }

  @Override
  public AuthenticationScope authenticateSystem() {
    if (currentAuthenticationScope.get() != null) {
      throw new IllegalStateException("Already withing an authentication scope.");
    }
    SystemAuthenticationScope systemAuthenticationScope = new SystemAuthenticationScope(this);
    currentAuthenticationScope.set(systemAuthenticationScope);
    return systemAuthenticationScope;
  }

  void endAuthenticationScope(AuthenticationScope abstractAuthenticationScope) {
    if (currentAuthenticationScope.get() != abstractAuthenticationScope) {
      throw new IllegalStateException("Cannot end a scope that is currently not in use.");
    }
    currentAuthenticationScope.remove();
  }

  public AbstractAuthenticationScope getCurrentScope() {
    AbstractAuthenticationScope currentScope = currentAuthenticationScope.get();
    if (currentScope == null) {
      throw new IllegalStateException("Not within an authentication scope.");
    }
    return currentScope;
  }
}
