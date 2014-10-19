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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
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

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class AccountCodeManagerTest {

  @Inject
  private AccountCodeManager accountCodeManager;
  @Inject
  private Store store;
  @Inject
  private SchemaDeploymentTask schemaDeploymentTask;
  @Inject
  private DatabaseTearDown databaseTearDown;

  @Before
  public void setUp() throws SQLException {
    Injector injector = Guice.createInjector(
        new AccountCodeModule(),
        new TestStorageModule());
    injector.injectMembers(this);
  }

  @After
  public void teardown() throws SQLException {
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
}
