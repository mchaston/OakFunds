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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.chaston.oakfunds.storage.AttributeSearchTerm;
import org.chaston.oakfunds.storage.SearchOperator;
import org.chaston.oakfunds.storage.SearchTerm;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;

import java.util.List;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public class UserManagerImpl implements UserManager {

  static final Permission PERMISSION_USER_READ = Permission.builder("user.read")
      .addRelatedAction(User.TYPE, ActionType.READ)
      .build();
  static final Permission PERMISSION_USER_CREATE = Permission.builder("user.create")
      .addRelatedAction(User.TYPE, ActionType.CREATE)
      .build();

  static final Permission PERMISSION_ROLE_GRANT_READ = Permission.builder("role_grant.read")
      .addRelatedAction(RoleGrant.TYPE, ActionType.READ)
      .build();
  static final Permission PERMISSION_ROLE_GRANT_CREATE = Permission.builder("role_grant.create")
      .addRelatedAction(RoleGrant.TYPE, ActionType.CREATE)
      .build();

  private final Store store;

  @Inject
  UserManagerImpl(Store store) {
    this.store = store;
  }

  @Override
  @PermissionAssertion("user.read")
  public User getUser(String identifier) throws StorageException {
    List<? extends SearchTerm> searchTerms = ImmutableList.of(
        AttributeSearchTerm.of(User.ATTRIBUTE_IDENTIFIER, SearchOperator.EQUALS, identifier));
    Iterable<User> users = store.findRecords(User.TYPE, searchTerms);
    return Iterables.getOnlyElement(users, null);
  }

  @Override
  @PermissionAssertion("user.create")
  public User createUser(String identifier, String name) throws StorageException {
    Map<String, Object> attributes = ImmutableMap.<String, Object>of(
        User.ATTRIBUTE_IDENTIFIER, identifier,
        User.ATTRIBUTE_NAME, name);
    return store.createRecord(User.TYPE, attributes);
  }

  @Override
  @PermissionAssertion("role_grant.read")
  public Iterable<RoleGrant> getRoleGrants(User user) throws StorageException {
    List<? extends SearchTerm> searchTerms = ImmutableList.of(
        AttributeSearchTerm.of(RoleGrant.ATTRIBUTE_USER_ID, SearchOperator.EQUALS, user.getId()));
    return store.findRecords(RoleGrant.TYPE, searchTerms);
  }

  @Override
  @PermissionAssertion("role_grant.create")
  public void grantRole(User user, String roleName) throws StorageException {
    Map<String, Object> attributes = ImmutableMap.<String, Object>of(
        RoleGrant.ATTRIBUTE_USER_ID, user.getId(),
        RoleGrant.ATTRIBUTE_NAME, roleName);
    store.createRecord(RoleGrant.TYPE, attributes);
  }
}
