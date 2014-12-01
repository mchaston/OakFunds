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

import com.google.common.collect.Iterables;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.chaston.oakfunds.bootstrap.BootstrapModule;
import org.chaston.oakfunds.jdbc.DatabaseTearDown;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.storage.TestStorageModule;
import org.chaston.oakfunds.storage.Transaction;
import org.chaston.oakfunds.storage.mgmt.SchemaDeploymentTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class UserManagerTest {

  @Inject
  private Store store;
  @Inject
  private UserManager userManager;
  @Inject
  private UserAuthenticationManager userAuthenticationManager;
  @Inject
  private SchemaDeploymentTask schemaDeploymentTask;
  @Inject
  private DatabaseTearDown databaseTearDown;

  private AuthenticationScope authenticationScope;

  @Before
  public void setUp() throws Exception {
    Injector injector = Guice.createInjector(
        new BootstrapModule(),
        new UserSecurityModule(),
        new TestStorageModule(),
        new TestUserAuthenticatorModule());
    injector.injectMembers(this);
    authenticationScope = userAuthenticationManager.authenticateUser();
  }

  @After
  public void teardown() throws SQLException {
    authenticationScope.close();
    databaseTearDown.teardown();
  }

  @Test
  public void createUser() throws StorageException {
    Transaction transaction = store.startTransaction();
    User newUser = userManager.createUser(
        "gmail.com:miles.chaston", "miles.chaston@gmail.com", "Miles Chaston");
    assertNotNull(newUser);
    assertEquals("gmail.com:miles.chaston", newUser.getIdentifier());
    assertEquals("miles.chaston@gmail.com", newUser.getEmail());
    assertEquals("Miles Chaston", newUser.getName());

    transaction.commit();

    User gotUser = userManager.getUser("gmail.com:miles.chaston");
    assertEquals("gmail.com:miles.chaston", gotUser.getIdentifier());
    assertEquals("miles.chaston@gmail.com", gotUser.getEmail());
    assertEquals("Miles Chaston", gotUser.getName());
  }

  @Test
  public void createMinimalUser() throws StorageException {
    Transaction transaction = store.startTransaction();
    User newUser = userManager.createUser(
        "gmail.com:miles.chaston", null , null);
    assertNotNull(newUser);
    assertEquals("gmail.com:miles.chaston", newUser.getIdentifier());
    assertNull(newUser.getEmail());
    assertNull(newUser.getName());

    transaction.commit();

    User gotUser = userManager.getUser("gmail.com:miles.chaston");
    assertEquals("gmail.com:miles.chaston", gotUser.getIdentifier());
    assertNull(gotUser.getEmail());
    assertNull(gotUser.getName());
  }

  @Test
  public void upsertUserFromNothing() throws StorageException {
    Transaction transaction = store.startTransaction();
    User newUser = userManager.upsertUser(
        "gmail.com:miles.chaston", "miles.chaston@gmail.com", "Miles Chaston");
    assertNotNull(newUser);
    assertEquals("gmail.com:miles.chaston", newUser.getIdentifier());
    assertEquals("miles.chaston@gmail.com", newUser.getEmail());
    assertEquals("Miles Chaston", newUser.getName());

    transaction.commit();

    User gotUser = userManager.getUser("gmail.com:miles.chaston");
    assertEquals("gmail.com:miles.chaston", gotUser.getIdentifier());
    assertEquals("miles.chaston@gmail.com", gotUser.getEmail());
    assertEquals("Miles Chaston", gotUser.getName());
  }

  @Test
  public void upsertUserIdentical() throws StorageException {
    Transaction transaction = store.startTransaction();
    userManager.createUser(
        "gmail.com:miles.chaston", "miles.chaston@gmail.com", "Miles Chaston");
    User newUser = userManager.upsertUser(
        "gmail.com:miles.chaston", "miles.chaston@gmail.com", "Miles Chaston");
    assertNotNull(newUser);
    assertEquals("gmail.com:miles.chaston", newUser.getIdentifier());
    assertEquals("miles.chaston@gmail.com", newUser.getEmail());
    assertEquals("Miles Chaston", newUser.getName());

    transaction.commit();

    User gotUser = userManager.getUser("gmail.com:miles.chaston");
    assertEquals("gmail.com:miles.chaston", gotUser.getIdentifier());
    assertEquals("miles.chaston@gmail.com", gotUser.getEmail());
    assertEquals("Miles Chaston", gotUser.getName());
  }

  @Test
  public void upsertUserChange() throws StorageException {
    Transaction transaction = store.startTransaction();
    User oldUser = userManager.createUser(
        "gmail.com:miles.chaston", "miles_chaston@yahoo.com", "Bob the Builder");
    User newUser = userManager.upsertUser(
        "gmail.com:miles.chaston", "miles.chaston@gmail.com", "Miles Chaston");
    assertNotNull(newUser);
    assertEquals("gmail.com:miles.chaston", newUser.getIdentifier());
    assertEquals("miles.chaston@gmail.com", newUser.getEmail());
    assertEquals("Miles Chaston", newUser.getName());

    transaction.commit();

    User gotUser = userManager.getUser("gmail.com:miles.chaston");
    assertEquals("gmail.com:miles.chaston", gotUser.getIdentifier());
    assertEquals("miles.chaston@gmail.com", gotUser.getEmail());
    assertEquals("Miles Chaston", gotUser.getName());
  }

  @Test
  public void grantRole() throws StorageException {
    Transaction transaction = store.startTransaction();
    User user = userManager.createUser(
        "gmail.com:miles.chaston", "miles.chaston@gmail.com", "Miles Chaston");
    assertNotNull(user);
    assertEquals("gmail.com:miles.chaston", user.getIdentifier());
    assertEquals("miles.chaston@gmail.com", user.getEmail());
    assertEquals("Miles Chaston", user.getName());

    userManager.grantRole(user, "admin");

    transaction.commit();

    Iterable<RoleGrant> roleGrants = userManager.getRoleGrants(user);

    assertEquals(1, Iterables.size(roleGrants));

    RoleGrant roleGrant = Iterables.get(roleGrants, 0);
    assertEquals("admin", roleGrant.getName());
  }

  @Test
  public void revokeRole() throws StorageException {
    Transaction transaction = store.startTransaction();
    User user = userManager.createUser(
        "gmail.com:miles.chaston", "miles.chaston@gmail.com", "Miles Chaston");
    assertNotNull(user);
    assertEquals("gmail.com:miles.chaston", user.getIdentifier());
    assertEquals("miles.chaston@gmail.com", user.getEmail());
    assertEquals("Miles Chaston", user.getName());

    userManager.grantRole(user, "admin");

    transaction.commit();

    Iterable<RoleGrant> roleGrants = userManager.getRoleGrants(user);
    RoleGrant roleGrant = Iterables.get(roleGrants, 0);

    transaction = store.startTransaction();
    userManager.revokeRole(roleGrant);
    transaction.commit();

    roleGrants = userManager.getRoleGrants(user);
    assertTrue(Iterables.isEmpty(roleGrants));
  }
}
