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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import java.util.Set;

/**
 * TODO(mchaston): write JavaDocs
 */
class PermissionRegistry {
  private final ImmutableMap<String, Permission> permissions;

  @Inject
  PermissionRegistry(Set<Permission> permissions) {
    ImmutableMap.Builder<String, Permission> permissionsBuilder = ImmutableMap.builder();
    for (Permission permission : permissions) {
      permissionsBuilder.put(permission.getName(), permission);
    }
    this.permissions = permissionsBuilder.build();
  }

  public Permission getPermission(String permissionName) {
    Permission permission = permissions.get(permissionName);
    if (permission == null) {
      throw new IllegalArgumentException("No such permission: " + permissionName);
    }
    return permission;
  }
}
