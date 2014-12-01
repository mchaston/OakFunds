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

import org.chaston.oakfunds.storage.StorageException;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface UserManager {
  User getUser(String identifier) throws StorageException;

  User getUser(int id) throws StorageException;

  Iterable<User> getUsers() throws StorageException;

  Iterable<RoleGrant> getRoleGrants(User user) throws StorageException;

  User createUser(String identifier, String email, String name) throws StorageException;

  User upsertUser(String identifier, String email, String name) throws StorageException;

  User updateUser(User user, String email, String name) throws StorageException;

  void grantRole(User user, String roleName) throws StorageException;

  void revokeRole(RoleGrant roleGrant) throws StorageException;

  Iterable<String> getRoleNames() throws StorageException;
}
