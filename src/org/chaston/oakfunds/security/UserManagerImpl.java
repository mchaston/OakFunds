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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.chaston.oakfunds.storage.AttributeOrderingTerm;
import org.chaston.oakfunds.storage.AttributeSearchTerm;
import org.chaston.oakfunds.storage.OrderingTerm;
import org.chaston.oakfunds.storage.SearchOperator;
import org.chaston.oakfunds.storage.SearchTerm;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
  static final Permission PERMISSION_USER_UPSERT = Permission.builder("user.upsert")
      .addRelatedAction(User.TYPE, ActionType.CREATE)
      .addRelatedAction(User.TYPE, ActionType.UPDATE)
      .build();
  static final Permission PERMISSION_USER_UPDATE = Permission.builder("user.update")
      .addRelatedAction(User.TYPE, ActionType.UPDATE)
      .build();

  static final Permission PERMISSION_ROLE_READ = Permission.builder("role.read")
      .build();

  static final Permission PERMISSION_ROLE_GRANT_READ = Permission.builder("role_grant.read")
      .addRelatedAction(RoleGrant.TYPE, ActionType.READ)
      .build();
  static final Permission PERMISSION_ROLE_GRANT_CREATE = Permission.builder("role_grant.create")
      .addRelatedAction(RoleGrant.TYPE, ActionType.CREATE)
      .build();
  static final Permission PERMISSION_ROLE_GRANT_DELETE = Permission.builder("role_grant.delete")
      .addRelatedAction(RoleGrant.TYPE, ActionType.DELETE)
      .build();

  private final Store store;
  private final RoleRegistry roleRegistry;

  @Inject
  UserManagerImpl(Store store, RoleRegistry roleRegistry) {
    this.store = store;
    this.roleRegistry = roleRegistry;
  }

  @Override
  @PermissionAssertion("user.read")
  public User getUser(String identifier) throws StorageException {
    Preconditions.checkNotNull(identifier, "identifier");
    List<? extends SearchTerm> searchTerms = ImmutableList.of(
        AttributeSearchTerm.of(User.ATTRIBUTE_IDENTIFIER, SearchOperator.EQUALS, identifier));
    Iterable<User> users = store.findRecords(User.TYPE, searchTerms,
        ImmutableList.<OrderingTerm>of());
    return Iterables.getOnlyElement(users, null);
  }

  @Override
  @PermissionAssertion("user.read")
  public User getUser(int id) throws StorageException {
    return store.getRecord(User.TYPE, id);
  }

  @Override
  @PermissionAssertion("user.read")
  public Iterable<User> getUsers() throws StorageException {
    return store.findRecords(User.TYPE, ImmutableList.<SearchTerm>of(),
        ImmutableList.<OrderingTerm>of());
  }

  @Override
  @PermissionAssertion("user.create")
  public User createUser(String identifier, String email, String name) throws StorageException {
    Preconditions.checkNotNull(identifier, "identifier");
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(User.ATTRIBUTE_IDENTIFIER, identifier);
    attributes.put(User.ATTRIBUTE_EMAIL, email);
    attributes.put(User.ATTRIBUTE_NAME, name);
    return store.createRecord(User.TYPE, attributes);
  }

  @Override
  @PermissionAssertion("user.upsert")
  public User upsertUser(String identifier, String email, String name) throws StorageException {
    Preconditions.checkNotNull(identifier, "identifier");
    User oldUser = getUser(identifier);
    if (oldUser == null) {
      return createUser(identifier, email, name);
    }
    if (!Objects.equals(email, oldUser.getEmail())
        || !Objects.equals(name, oldUser.getName())) {
      Map<String, Object> attributes = new HashMap<>();
      attributes.put(User.ATTRIBUTE_IDENTIFIER, identifier);
      attributes.put(User.ATTRIBUTE_EMAIL, email);
      attributes.put(User.ATTRIBUTE_NAME, name);
      return store.updateRecord(oldUser, attributes);
    } else {
      return oldUser;
    }
  }

  @Override
  @PermissionAssertion("user.update")
  public User updateUser(User user, String email, String name) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(User.ATTRIBUTE_EMAIL, email);
    attributes.put(User.ATTRIBUTE_NAME, name);
    return store.updateRecord(user, attributes);
  }

  @Override
  @PermissionAssertion("role_grant.read")
  public Iterable<RoleGrant> getRoleGrants(User user) throws StorageException {
    Preconditions.checkNotNull(user, "user");
    List<? extends SearchTerm> searchTerms = ImmutableList.of(
        AttributeSearchTerm.of(RoleGrant.ATTRIBUTE_USER_ID, SearchOperator.EQUALS, user.getId()));
    return store.findRecords(RoleGrant.TYPE, searchTerms,
        ImmutableList.of(
            AttributeOrderingTerm.of(RoleGrant.ATTRIBUTE_NAME, OrderingTerm.Order.ASC)));
  }

  @Override
  @PermissionAssertion("role_grant.create")
  public void grantRole(User user, String roleName) throws StorageException {
    Preconditions.checkNotNull(user, "user");
    Preconditions.checkNotNull(roleName, "roleName");
    Role role = roleRegistry.getRole(roleName);
    if (role == null) {
      throw new StorageException("No such role: " + roleName);
    }
    Map<String, Object> attributes = ImmutableMap.<String, Object>of(
        RoleGrant.ATTRIBUTE_USER_ID, user.getId(),
        RoleGrant.ATTRIBUTE_NAME, roleName);
    store.createRecord(RoleGrant.TYPE, attributes);
  }

  @Override
  @PermissionAssertion("role_grant.delete")
  public void revokeRole(RoleGrant roleGrant) throws StorageException {
    Preconditions.checkNotNull(roleGrant, "roleGrant");
    store.deleteRecord(roleGrant);
  }

  @Override
  @PermissionAssertion("role.read")
  public Iterable<String> getRoleNames() throws StorageException {
    return Iterables.transform(roleRegistry.getRoles(), new Function<Role, String>() {
      @Override
      public String apply(Role role) {
        return role.getName();
      }
    });
  }
}
