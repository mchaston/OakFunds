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

/**
 * TODO(mchaston): write JavaDocs
 */
public class SystemAuthenticationManagerImpl implements SystemAuthenticationManager {

  private final ThreadLocal<AbstractAuthenticationScope> currentAuthenticationScope =
      new ThreadLocal<>();

  private PermissionRegistry permissionRegistry;

  @Inject
  SystemAuthenticationManagerImpl(PermissionRegistry permissionRegistry) {
    this.permissionRegistry = permissionRegistry;
  }

  @Override
  public AuthenticationScope authenticateSystem() {
    if (currentAuthenticationScope.get() != null) {
      throw new IllegalStateException("Already withing an authentication scope.");
    }
    SystemAuthenticationScope systemAuthenticationScope =
        new SystemAuthenticationScope(this, permissionRegistry);
    setCurrentScope(systemAuthenticationScope);
    return systemAuthenticationScope;
  }

  void endAuthenticationScope(AuthenticationScope abstractAuthenticationScope) {
    if (currentAuthenticationScope.get() != abstractAuthenticationScope) {
      throw new IllegalStateException("Cannot end a scope that is currently not in use.");
    }
    currentAuthenticationScope.remove();
  }

  AbstractAuthenticationScope getCurrentScope() {
    return currentAuthenticationScope.get();
  }

  void setCurrentScope(AbstractAuthenticationScope authenticationScope) {
    currentAuthenticationScope.set(authenticationScope);
  }
}
