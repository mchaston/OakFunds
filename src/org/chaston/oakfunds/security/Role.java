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

import java.util.HashSet;
import java.util.Set;

/**
 * TODO(mchaston): write JavaDocs
 */
public class Role {
  private final String name;
  private final ImmutableSet<String> permissionNames;

  private Role(String name, ImmutableSet<String> permissionNames) {
    this.name = name;
    this.permissionNames = permissionNames;
  }

  public String getName() {
    return name;
  }

  public ImmutableSet<String> getPermissionNames() {
    return permissionNames;
  }

  public static Builder builder(String name) {
    return new Builder(name);
  }

  public static class Builder {

    private final String name;
    private final Set<String> permissionNames = new HashSet<>();

    Builder(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public Builder addPermission(String permissionName) {
      permissionNames.add(permissionName);
      return this;
    }

    public Role build() {
      return new Role(name, ImmutableSet.copyOf(permissionNames));
    }
  }
}
