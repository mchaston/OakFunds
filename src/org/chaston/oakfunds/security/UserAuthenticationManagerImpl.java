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
import org.chaston.oakfunds.bootstrap.BootstrappingDependency;
import org.chaston.oakfunds.storage.StorageException;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * TODO(mchaston): write JavaDocs
 */
class UserAuthenticationManagerImpl implements UserAuthenticationManager {

  private static final Logger logger = Logger.getLogger(UserAuthenticationManagerImpl.class.getName());

  private final SystemAuthenticationManagerImpl systemAuthenticationManager;
  private final UserAuthenticator userAuthenticator;
  private final UserManager userManager;
  private final RoleRegistry roleRegistry;

  @Inject
  UserAuthenticationManagerImpl(
      BootstrappingDependency bootstrappingDependency, // Here for dependency enforcement.
      SystemAuthenticationManagerImpl systemAuthenticationManager,
      UserAuthenticator userAuthenticator,
      UserManager userManager,
      RoleRegistry roleRegistry) {
    this.systemAuthenticationManager = systemAuthenticationManager;
    this.userAuthenticator = userAuthenticator;
    this.userManager = userManager;
    this.roleRegistry = roleRegistry;
  }

  @Override
  public AuthenticationScope authenticateUser() throws StorageException, AuthenticationException {
    if (systemAuthenticationManager.getCurrentScope() != null) {
      throw new IllegalStateException("Already withing an authentication scope.");
    }
    AuthenticatedUser authenticatedUser = userAuthenticator.getAuthenticatedUser();
    Set<String> userPermissions;
    try (AuthenticationScope authenticationScope =
             systemAuthenticationManager.authenticateSystem()) {
      User user = userManager.getUser(authenticatedUser.getIdentifier());
      if (user == null) {
        throw new IllegalStateException("No authenticated user available.");
      }
      userPermissions = getUserPermissions(user);
    }
    UserAuthenticationScope userAuthenticationScope =
        new UserAuthenticationScope(systemAuthenticationManager, userPermissions);
    systemAuthenticationManager.setCurrentScope(userAuthenticationScope);
    return userAuthenticationScope;
  }

  private Set<String> getUserPermissions(User user) throws StorageException {
    Set<String> permissionNames = new HashSet<>();
    for (RoleGrant roleGrant : userManager.getRoleGrants(user)) {
      Role role = roleRegistry.getRole(roleGrant.getName());
      if (role != null) {
        permissionNames.addAll(role.getPermissionNames());
      } else {
        logger.warning("User " + user.getIdentifier()
            + " was granted non existent role: " + roleGrant.getName());
      }
    }
    return ImmutableSet.copyOf(permissionNames);
  }
}
