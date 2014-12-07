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
package org.chaston.oakfunds.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.chaston.oakfunds.account.AccountCode;
import org.chaston.oakfunds.account.AccountCodeManager;
import org.chaston.oakfunds.account.AccountCodeModule;
import org.chaston.oakfunds.bootstrap.BootstrapModule;
import org.chaston.oakfunds.jdbc.DatabaseTearDown;
import org.chaston.oakfunds.ledger.BankAccount;
import org.chaston.oakfunds.ledger.BankAccountType;
import org.chaston.oakfunds.ledger.ExpenseAccount;
import org.chaston.oakfunds.ledger.LedgerManager;
import org.chaston.oakfunds.ledger.LedgerModule;
import org.chaston.oakfunds.security.AuthenticationScope;
import org.chaston.oakfunds.security.TestUserAuthenticatorModule;
import org.chaston.oakfunds.security.UserAuthenticationManager;
import org.chaston.oakfunds.security.UserSecurityModule;
import org.chaston.oakfunds.storage.Report;
import org.chaston.oakfunds.storage.ReportDateGranularity;
import org.chaston.oakfunds.storage.ReportEntry;
import org.chaston.oakfunds.storage.ReportRow;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.storage.TestStorageModule;
import org.chaston.oakfunds.storage.Transaction;
import org.chaston.oakfunds.storage.mgmt.SchemaDeploymentTask;
import org.chaston.oakfunds.system.SystemModule;
import org.chaston.oakfunds.system.TestSystemBootstrapModuleBuilder;
import org.chaston.oakfunds.util.BigDecimalUtil;
import org.chaston.oakfunds.util.DateUtil;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class AccountModellingTest {

  private static final int YEAR_2014 = Instant.parse("2014-01-01").get(DateTimeFieldType.year());
  private static final int YEAR_2015 = Instant.parse("2015-01-01").get(DateTimeFieldType.year());

  @Inject
  private AccountCodeManager accountCodeManager;
  @Inject
  private UserAuthenticationManager userAuthenticationManager;
  @Inject
  private LedgerManager ledgerManager;
  @Inject
  private ModelManager modelManager;
  @Inject
  private Store store;
  @Inject
  private SchemaDeploymentTask schemaDeploymentTask;
  @Inject
  private DatabaseTearDown databaseTearDown;

  private AuthenticationScope authenticationScope;
  private BankAccount bankAccount1;
  private ExpenseAccount expenseAccount1;

  @Before
  public void setUp() throws Exception {
    Injector injector = Guice.createInjector(
        new AccountCodeModule(),
        new LedgerModule(),
        new BootstrapModule(),
        new ModelModule(),
        new UserSecurityModule(),
        new SystemModule(),
        new TestSystemBootstrapModuleBuilder()
            .setCurrentYear(Instant.parse("2014-01-01").get(DateTimeFieldType.year()))
            .setTimeHorizon(10)
            .build(),
        new TestStorageModule(),
        new TestUserAuthenticatorModule());
    injector.injectMembers(this);
    authenticationScope = userAuthenticationManager.authenticateUser();

    runInTransaction(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        AccountCode accountCode1 = accountCodeManager.createAccountCode(1, "Account Code 1");
        bankAccount1 = ledgerManager.createBankAccount(
            accountCode1, "Bank Account 1", BankAccountType.OPERATING);
        AccountCode accountCode2 = accountCodeManager.createAccountCode(2, "Account Code 2");
        expenseAccount1 = ledgerManager.createExpenseAccount(
            accountCode2, "Expense Account 1", bankAccount1);
        return null;
      }
    });
  }

  @After
  public void teardown() throws SQLException {
    authenticationScope.close();
    databaseTearDown.teardown();
  }

  @Test
  public void runReportWithNoModelTransactions() throws Exception {
    final Model baseModel = modelManager.getBaseModel();
    runInTransaction(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        ledgerManager.recordTransaction(expenseAccount1, DateUtil.endOfMonth(2014, 1),
            BigDecimalUtil.valueOf(1000));
        return null;
      }
    });

    Report report = modelManager.runTransactionReport(baseModel, YEAR_2014, YEAR_2015,
        ReportDateGranularity.MONTH);
    Iterable<ReportRow> rows = report.getRows();
    assertEquals(2, Iterables.size(rows));
    ReportRow row = report.getRow(
        ImmutableMap.<String, Object>of(
            ModelManager.DIMENSION_ACCOUNT_ID, expenseAccount1.getId()));

    ReportEntry entry = Iterables.get(row.getEntries(), 0);
    assertEquals(DateUtil.endOfYear(2013), entry.getInstant());
    assertEquals(BigDecimal.ZERO, entry.getMeasure(ModelManager.MEASURE_AMOUNT));

    entry = Iterables.get(row.getEntries(), 1);
    assertEquals(DateUtil.endOfMonth(2014, 1), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(1000), entry.getMeasure(ModelManager.MEASURE_AMOUNT));

    entry = Iterables.get(row.getEntries(), 2);
    assertEquals(DateUtil.endOfMonth(2014, 2), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(1000), entry.getMeasure(ModelManager.MEASURE_AMOUNT));
  }

  @Test
  public void runReportWithFollowingModelTransactions() throws Exception {
    final Model baseModel = modelManager.getBaseModel();
    runInTransaction(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        ledgerManager.recordTransaction(bankAccount1, DateUtil.endOfMonth(2013, 12),
            BigDecimalUtil.valueOf(100000));
        ledgerManager.recordTransaction(expenseAccount1, DateUtil.endOfMonth(2014, 1),
            BigDecimalUtil.valueOf(1000));
        modelManager.setMonthlyRecurringEventDetails(baseModel, expenseAccount1,
            Instant.parse("2014-02-01"), Instant.parse("2014-05-01"),
            BigDecimal.valueOf(1000));
        return null;
      }
    });

    Report report = modelManager.runTransactionReport(baseModel, YEAR_2014, YEAR_2015,
        ReportDateGranularity.MONTH);

    // Look at expense account.
    ReportRow row = report.getRow(
        ImmutableMap.<String, Object>of(
            ModelManager.DIMENSION_ACCOUNT_ID, expenseAccount1.getId()));

    ReportEntry entry = Iterables.get(row.getEntries(), 0);
    assertEquals(DateUtil.endOfYear(2013), entry.getInstant());
    assertEquals(BigDecimal.ZERO, entry.getMeasure(ModelManager.MEASURE_AMOUNT));

    entry = Iterables.get(row.getEntries(), 1);
    assertEquals(DateUtil.endOfMonth(2014, 1), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(1000), entry.getMeasure(ModelManager.MEASURE_AMOUNT));

    entry = Iterables.get(row.getEntries(), 2);
    assertEquals(DateUtil.endOfMonth(2014, 2), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(2000), entry.getMeasure(ModelManager.MEASURE_AMOUNT));

    entry = Iterables.get(row.getEntries(), 4);
    assertEquals(DateUtil.endOfMonth(2014, 4), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(4000), entry.getMeasure(ModelManager.MEASURE_AMOUNT));

    // Look at bank account.
    row = report.getRow(
        ImmutableMap.<String, Object>of(
            ModelManager.DIMENSION_ACCOUNT_ID, bankAccount1.getId()));

    entry = Iterables.get(row.getEntries(), 0);
    assertEquals(DateUtil.endOfYear(2013), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(100000), entry.getMeasure(ModelManager.MEASURE_AMOUNT));

    entry = Iterables.get(row.getEntries(), 1);
    assertEquals(DateUtil.endOfMonth(2014, 1), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(99000), entry.getMeasure(ModelManager.MEASURE_AMOUNT));

    entry = Iterables.get(row.getEntries(), 2);
    assertEquals(DateUtil.endOfMonth(2014, 2), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(98000), entry.getMeasure(ModelManager.MEASURE_AMOUNT));

    entry = Iterables.get(row.getEntries(), 4);
    assertEquals(DateUtil.endOfMonth(2014, 4), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(96000), entry.getMeasure(ModelManager.MEASURE_AMOUNT));
  }

  @Test
  public void runReportWithCompensatedModelTransactions() {

  }

  private void runInTransaction(Callable<Void> callable) throws Exception {
    Transaction transaction = store.startTransaction();
    callable.call();
    transaction.commit();
  }
}
