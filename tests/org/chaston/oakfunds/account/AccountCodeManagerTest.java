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
package org.chaston.oakfunds.account;

import com.google.common.collect.Iterables;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.chaston.oakfunds.bootstrap.BootstrapModule;
import org.chaston.oakfunds.jdbc.DatabaseTearDown;
import org.chaston.oakfunds.security.AuthenticationScope;
import org.chaston.oakfunds.security.TestUserAuthenticatorModule;
import org.chaston.oakfunds.security.UserAuthenticationManager;
import org.chaston.oakfunds.security.UserSecurityModule;
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

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class AccountCodeManagerTest {

  @Inject
  private AccountCodeManager accountCodeManager;
  @Inject
  private UserAuthenticationManager userAuthenticationManager;
  @Inject
  private Store store;
  @Inject
  private SchemaDeploymentTask schemaDeploymentTask;
  @Inject
  private DatabaseTearDown databaseTearDown;

  private AuthenticationScope authenticationScope;

  @Before
  public void setUp() throws Exception {
    Injector injector = Guice.createInjector(
        new AccountCodeModule(),
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
  public void createNewAccountCode() throws StorageException {
    Transaction transaction = store.startTransaction();
    AccountCode accountCode = accountCodeManager.createAccountCode(80000, "Operating");
    transaction.commit();

    assertNotNull(accountCode);
    assertEquals(80000, accountCode.getId());
    assertEquals("Operating", accountCode.getTitle());

    accountCode = accountCodeManager.getAccountCode(80000);

    assertNotNull(accountCode);
    assertEquals(80000, accountCode.getId());
    assertEquals("Operating", accountCode.getTitle());
  }

  @Test
  public void getAccountCodes() throws StorageException {
    Transaction transaction = store.startTransaction();
    accountCodeManager.createAccountCode(80000, "Operating");
    accountCodeManager.createAccountCode(50000, "Electricity");
    transaction.commit();

    Iterable<AccountCode> accountCodes = accountCodeManager.getAccountCodes();
    assertEquals(2, Iterables.size(accountCodes));
    assertEquals(50000, Iterables.get(accountCodes, 0).getId());
    assertEquals(80000, Iterables.get(accountCodes, 1).getId());
  }
}
