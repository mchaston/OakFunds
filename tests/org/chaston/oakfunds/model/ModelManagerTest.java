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
import org.chaston.oakfunds.ledger.ExpenseAccount;
import org.chaston.oakfunds.ledger.LedgerManager;
import org.chaston.oakfunds.ledger.LedgerModule;
import org.chaston.oakfunds.ledger.RevenueAccount;
import org.chaston.oakfunds.security.AuthenticationScope;
import org.chaston.oakfunds.security.TestUserAuthenticatorModule;
import org.chaston.oakfunds.security.UserAuthenticationManager;
import org.chaston.oakfunds.security.UserSecurityModule;
import org.chaston.oakfunds.storage.Report;
import org.chaston.oakfunds.storage.ReportDateGranularity;
import org.chaston.oakfunds.storage.ReportEntry;
import org.chaston.oakfunds.storage.ReportRow;
import org.chaston.oakfunds.storage.StorageException;
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

import java.sql.SQLException;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class ModelManagerTest {

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
  }

  @After
  public void teardown() throws SQLException {
    authenticationScope.close();
    databaseTearDown.teardown();
  }

  @Test
  public void createModel() throws StorageException {
    Transaction transaction = store.startTransaction();
    Model model = modelManager.createNewModel("New Model");
    assertEquals("New Model", model.getTitle());
    transaction.commit();

    assertEquals("New Model", modelManager.getModel(model.getId()).getTitle());
  }

  @Test
  public void getBaseModel() throws StorageException {
    assertNotNull(modelManager.getBaseModel());
  }

  @Test
  public void updateModel() throws StorageException {
    Transaction transaction = store.startTransaction();
    Model model = modelManager.createNewModel("New Model");
    assertEquals("New Model", model.getTitle());
    transaction.commit();

    transaction = store.startTransaction();
    model = modelManager.updateModel(model, "Old Model");
    assertEquals("Old Model", model.getTitle());
    assertFalse(model.isBaseModel());
    transaction.commit();

    assertEquals("Old Model", modelManager.getModel(model.getId()).getTitle());

    transaction = store.startTransaction();
    model = modelManager.updateModel(modelManager.getBaseModel(), "Base Model");
    assertEquals("Base Model", model.getTitle());
    assertTrue(model.isBaseModel());
    transaction.commit();

    assertEquals("Base Model", modelManager.getBaseModel().getTitle());
    assertTrue(model.isBaseModel());
  }

  @Test
  public void setMonthlyRecurringEventDetails() throws StorageException {
    Transaction transaction = store.startTransaction();
    AccountCode accountCode = accountCodeManager.createAccountCode(5000, "Utilities");
    ExpenseAccount expenseAccount =
        ledgerManager.createExpenseAccount(accountCode, "Electricity / Water", null);
    MonthlyRecurringEvent monthlyRecurringEvent =
        modelManager.setMonthlyRecurringEventDetails(modelManager.getBaseModel(), expenseAccount,
            DateUtil.BEGINNING_OF_TIME, DateUtil.END_OF_TIME,
            BigDecimalUtil.valueOf(3000));
    transaction.commit();

    assertEquals(BigDecimalUtil.valueOf(3000), monthlyRecurringEvent.getAmount());

    Iterable<ModelAccountTransaction> modelTransactions =
        modelManager.getModelTransactions(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2014-01-01"), Instant.parse("2015-01-01"));

    assertEquals(12, Iterables.size(modelTransactions));
    ModelAccountTransaction firstModelTransaction = Iterables.getFirst(modelTransactions, null);
    assertNotNull(firstModelTransaction);
    assertEquals(modelManager.getBaseModel().getId(), firstModelTransaction.getModelId());
    assertEquals(expenseAccount.getId(), firstModelTransaction.getAccountId());
    assertEquals(BigDecimalUtil.valueOf(3000), firstModelTransaction.getAmount());
    assertEquals(Instant.parse("2014-01-01"), firstModelTransaction.getInstant());

    Iterable<ModelDistributionTransaction> modelDistributionTransactions =
        modelManager.getModelDistributionTransactions(modelManager.getBaseModel(), expenseAccount,
            DateUtil.endOfYear(2013), Instant.parse("2015-01-01"));

    assertEquals(0, Iterables.size(modelDistributionTransactions));
  }

  @Test
  public void setAnnualRecurringEventDetails() throws StorageException {
    Transaction transaction = store.startTransaction();
    AccountCode accountCode = accountCodeManager.createAccountCode(6000, "Insurance");
    ExpenseAccount expenseAccount =
        ledgerManager.createExpenseAccount(accountCode, "Insurance", null);
    AnnualRecurringEvent annualRecurringEvent =
        modelManager.setAnnualRecurringEventDetails(modelManager.getBaseModel(), expenseAccount,
            DateUtil.BEGINNING_OF_TIME, DateUtil.END_OF_TIME,
            3, BigDecimalUtil.valueOf(12000));
    transaction.commit();

    assertEquals(BigDecimalUtil.valueOf(12000), annualRecurringEvent.getAmount());
    assertEquals(3, annualRecurringEvent.getPaymentMonth());

    Iterable<ModelAccountTransaction> modelTransactions =
        modelManager.getModelTransactions(modelManager.getBaseModel(), expenseAccount,
            DateUtil.endOfYear(2013), Instant.parse("2015-01-01"));

    assertEquals(1, Iterables.size(modelTransactions));
    ModelAccountTransaction modelTransaction = Iterables.getOnlyElement(modelTransactions);
    assertEquals(modelManager.getBaseModel().getId(), modelTransaction.getModelId());
    assertEquals(expenseAccount.getId(), modelTransaction.getAccountId());
    assertEquals(BigDecimalUtil.valueOf(12000), modelTransaction.getAmount());
    assertEquals(Instant.parse("2014-03-01"), modelTransaction.getInstant());

    Iterable<ModelDistributionTransaction> modelDistributionTransactions =
        modelManager.getModelDistributionTransactions(modelManager.getBaseModel(), expenseAccount,
            DateUtil.endOfYear(2013), Instant.parse("2015-01-01"));

    assertEquals(13, Iterables.size(modelDistributionTransactions));

    ModelDistributionTransaction firstModelDistributionTransaction =
        Iterables.getFirst(modelDistributionTransactions, null);
    assertNotNull(firstModelDistributionTransaction);
    assertEquals(modelManager.getBaseModel().getId(), firstModelDistributionTransaction.getModelId());
    assertEquals(expenseAccount.getId(), firstModelDistributionTransaction.getAccountId());
    // This is the sum of distributions until the end of the previous year.
    assertEquals(BigDecimalUtil.valueOf(9000), firstModelDistributionTransaction.getAmount());
    assertEquals(DateUtil.endOfYear(2013), firstModelDistributionTransaction.getInstant());

    // This is the distribution that cancels out previous distributions.
    ModelDistributionTransaction thirdModelDistributionTransaction =
        Iterables.get(modelDistributionTransactions, 3, null);
    assertNotNull(thirdModelDistributionTransaction);
    assertEquals(modelManager.getBaseModel().getId(), thirdModelDistributionTransaction.getModelId());
    assertEquals(expenseAccount.getId(), thirdModelDistributionTransaction.getAccountId());
    assertEquals(BigDecimalUtil.valueOf(-11000), thirdModelDistributionTransaction.getAmount());
    assertEquals(Instant.parse("2014-03-01"), thirdModelDistributionTransaction.getInstant());
    assertEquals(firstModelDistributionTransaction.getModelAccountTransactionId(),
        thirdModelDistributionTransaction.getModelAccountTransactionId());

    ModelDistributionTransaction fourthModelDistributionTransaction =
        Iterables.get(modelDistributionTransactions, 4, null);
    assertNotNull(fourthModelDistributionTransaction);
    assertEquals(modelManager.getBaseModel().getId(),
        fourthModelDistributionTransaction.getModelId());
    assertEquals(expenseAccount.getId(), fourthModelDistributionTransaction.getAccountId());
    assertEquals(BigDecimalUtil.valueOf(1000), fourthModelDistributionTransaction.getAmount());
    assertEquals(Instant.parse("2014-04-01"), fourthModelDistributionTransaction.getInstant());
    // Different transaction from the previous ones.
    assertNotEquals(firstModelDistributionTransaction.getModelAccountTransactionId(),
        fourthModelDistributionTransaction.getModelAccountTransactionId());
  }

  @Test
  public void createAdHocEvent() throws StorageException {
    Transaction transaction = store.startTransaction();
    AccountCode accountCode = accountCodeManager.createAccountCode(7000, "Maintenance");
    ExpenseAccount expenseAccount =
        ledgerManager.createExpenseAccount(accountCode, "House Painting", null);
    ModelAccountTransaction modelAccountTransaction =
        modelManager.createAdHocEvent(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2017-01-01"),
            5, DistributionTimeUnit.YEARS, BigDecimalUtil.valueOf(60000));
    transaction.commit();

    assertNotNull(modelAccountTransaction);

    Iterable<ModelDistributionTransaction> modelDistributionTransactions =
        modelManager.getModelDistributionTransactions(modelManager.getBaseModel(), expenseAccount,
            DateUtil.endOfYear(2013), Instant.parse("2015-01-01"));

    assertEquals(13, Iterables.size(modelDistributionTransactions));
    ModelDistributionTransaction firstModelDistributionTransaction = Iterables.getFirst(
        modelDistributionTransactions, null);
    assertNotNull(firstModelDistributionTransaction);
    assertEquals(modelManager.getBaseModel().getId(), firstModelDistributionTransaction.getModelId());
    assertEquals(expenseAccount.getId(), firstModelDistributionTransaction.getAccountId());
    // First one is a roll up of the previous distributions.
    assertEquals(BigDecimalUtil.valueOf(23000), firstModelDistributionTransaction.getAmount());
    assertEquals(DateUtil.endOfYear(2013), firstModelDistributionTransaction.getInstant());

    // Regular ones are a normal size.
    ModelDistributionTransaction secondModelDistributionTransaction =
        Iterables.get(modelDistributionTransactions, 2, null);
    assertNotNull(secondModelDistributionTransaction);
    assertEquals(modelManager.getBaseModel().getId(), secondModelDistributionTransaction.getModelId());
    assertEquals(expenseAccount.getId(), secondModelDistributionTransaction.getAccountId());
    assertEquals(BigDecimalUtil.valueOf(1000), secondModelDistributionTransaction.getAmount());
    assertEquals(Instant.parse("2014-02-01"), secondModelDistributionTransaction.getInstant());
  }

  @Test
  public void createAdHocEventBeyondTimeHorizon() throws StorageException {
    Transaction transaction = store.startTransaction();
    AccountCode accountCode = accountCodeManager.createAccountCode(7000, "Maintenance");
    ExpenseAccount expenseAccount =
        ledgerManager.createExpenseAccount(accountCode, "House Painting", null);
    ModelAccountTransaction modelAccountTransaction =
        modelManager.createAdHocEvent(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2027-01-01"),
            50, DistributionTimeUnit.YEARS, BigDecimalUtil.valueOf(60000));
    transaction.commit();

    assertNotNull(modelAccountTransaction);

    Iterable<ModelDistributionTransaction> modelDistributionTransactions =
        modelManager.getModelDistributionTransactions(modelManager.getBaseModel(), expenseAccount,
            DateUtil.endOfYear(2013), Instant.parse("2015-01-01"));

    assertEquals(13, Iterables.size(modelDistributionTransactions));
    ModelDistributionTransaction firstModelDistributionTransaction = Iterables.getFirst(
        modelDistributionTransactions, null);
    assertNotNull(firstModelDistributionTransaction);
    assertEquals(modelManager.getBaseModel().getId(), firstModelDistributionTransaction.getModelId());
    assertEquals(expenseAccount.getId(), firstModelDistributionTransaction.getAccountId());
    // First one is a roll up of the previous distributions.
    assertEquals(BigDecimalUtil.valueOf(44300), firstModelDistributionTransaction.getAmount());
    assertEquals(DateUtil.endOfYear(2013), firstModelDistributionTransaction.getInstant());

    // Regular ones are a normal size.
    ModelDistributionTransaction secondModelDistributionTransaction =
        Iterables.get(modelDistributionTransactions, 2, null);
    assertNotNull(secondModelDistributionTransaction);
    assertEquals(modelManager.getBaseModel().getId(), secondModelDistributionTransaction.getModelId());
    assertEquals(expenseAccount.getId(), secondModelDistributionTransaction.getAccountId());
    assertEquals(BigDecimalUtil.valueOf(100), secondModelDistributionTransaction.getAmount());
    assertEquals(Instant.parse("2014-02-01"), secondModelDistributionTransaction.getInstant());
  }

  @Test
  public void updateAdHocEvent() throws StorageException {
    // Create the initial event.
    Transaction transaction = store.startTransaction();
    AccountCode accountCode = accountCodeManager.createAccountCode(7000, "Maintenance");
    ExpenseAccount expenseAccount =
        ledgerManager.createExpenseAccount(accountCode, "House Painting", null);
    modelManager.createAdHocEvent(modelManager.getBaseModel(), expenseAccount,
        Instant.parse("2017-01-01"),
        5, DistributionTimeUnit.YEARS, BigDecimalUtil.valueOf(60000));
    transaction.commit();

    Iterable<ModelDistributionTransaction> oldModelDistributionTransactions =
        modelManager.getModelDistributionTransactions(modelManager.getBaseModel(), expenseAccount,
            DateUtil.endOfYear(2013), Instant.parse("2015-01-01"));

    // Get the event back.
    ModelAccountTransaction modelAccountTransaction = Iterables.getOnlyElement(
        modelManager.getModelTransactions(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2016-01-01"), Instant.parse("2017-02-01")));

    transaction = store.startTransaction();
    modelManager.updateAdHocEvent(modelAccountTransaction,
        Instant.parse("2017-06-01"),
        5, DistributionTimeUnit.YEARS, BigDecimalUtil.valueOf(60000));
    transaction.commit();

    Iterable<ModelDistributionTransaction> newModelDistributionTransactions =
        modelManager.getModelDistributionTransactions(modelManager.getBaseModel(), expenseAccount,
            DateUtil.endOfYear(2013), Instant.parse("2015-01-01"));

    Iterator<ModelDistributionTransaction> oldTransactionsIter =
        oldModelDistributionTransactions.iterator();
    Iterator<ModelDistributionTransaction> newTransactionsIter =
        newModelDistributionTransactions.iterator();
    for (int i = 0; i < 12; i++) {
      ModelDistributionTransaction oldTransaction = oldTransactionsIter.next();
      ModelDistributionTransaction newTransaction = newTransactionsIter.next();
      assertEquals(oldTransaction.getInstant(), newTransaction.getInstant());
      if (i == 0) {
        assertNotEquals(oldTransaction.getAmount(), newTransaction.getAmount());
      } else {
        assertEquals(oldTransaction.getAmount(), newTransaction.getAmount());
      }
    }
  }

  @Test
  public void deleteAdHocEvent() throws StorageException {
    // Create the initial event.
    Transaction transaction = store.startTransaction();
    AccountCode accountCode = accountCodeManager.createAccountCode(7000, "Maintenance");
    ExpenseAccount expenseAccount =
        ledgerManager.createExpenseAccount(accountCode, "House Painting", null);
    modelManager.createAdHocEvent(modelManager.getBaseModel(), expenseAccount,
        Instant.parse("2017-01-01"),
        5, DistributionTimeUnit.YEARS, BigDecimalUtil.valueOf(60000));
    transaction.commit();

    // Get the event back.
    ModelAccountTransaction modelAccountTransaction = Iterables.getOnlyElement(
        modelManager.getModelTransactions(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2016-01-01"), Instant.parse("2017-02-01")));

    transaction = store.startTransaction();
    modelManager.deleteAdHocEvent(modelAccountTransaction);
    transaction.commit();

    Iterable<ModelDistributionTransaction> newModelDistributionTransactions =
        modelManager.getModelDistributionTransactions(modelManager.getBaseModel(), expenseAccount,
            DateUtil.endOfYear(2013), Instant.parse("2015-01-01"));
    assertTrue(Iterables.isEmpty(newModelDistributionTransactions));
  }

  @Test
  public void runDistributionReport() throws StorageException {
    ReportingAccounts accounts = initReportingDataset();

    Report report = modelManager.runDistributionReport(accounts.model1,
        YEAR_2014, YEAR_2015, ReportDateGranularity.MONTH);

    assertEquals(Iterables.size(report.getRows()), 2);
    ReportRow reportRow = report.getRow(
        ImmutableMap.of("model_account_id", (Object) accounts.longTermExpenseAccount.getId()));

    assertEquals(13, Iterables.size(reportRow.getEntries()));

    ReportEntry entry = Iterables.get(reportRow.getEntries(), 0);
    assertEquals(DateUtil.endOfYear(2013), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(11500),
        entry.getMeasure(ModelDistributionTransaction.ATTRIBUTE_AMOUNT));

    for (int i = 1; i <=12; i++) {
      entry = Iterables.get(reportRow.getEntries(), i);
      assertEquals(DateUtil.endOfMonth(2014, i), entry.getInstant());
      assertEquals(BigDecimalUtil.valueOf(11500 + (i * 250)),
          entry.getMeasure(ModelDistributionTransaction.ATTRIBUTE_AMOUNT));
    }

    reportRow = report.getRow(
        ImmutableMap.of("model_account_id", (Object) accounts.annualExpenseAccount.getId()));

    assertEquals(13, Iterables.size(reportRow.getEntries()));

    // Starts off with the amount owed from the beginning of the year.
    entry = Iterables.get(reportRow.getEntries(), 0);
    assertEquals(DateUtil.endOfYear(2013), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(5249.99997),
        entry.getMeasure(ModelDistributionTransaction.ATTRIBUTE_AMOUNT));

    entry = Iterables.get(reportRow.getEntries(), 1);
    assertEquals(DateUtil.endOfMonth(2014, 1), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(5833.33330),
        entry.getMeasure(ModelDistributionTransaction.ATTRIBUTE_AMOUNT));

    entry = Iterables.get(reportRow.getEntries(), 2);
    assertEquals(DateUtil.endOfMonth(2014, 2), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(6416.66663),
        entry.getMeasure(ModelDistributionTransaction.ATTRIBUTE_AMOUNT));

    // The March distribution resets the sum to zero (as the event would happen).
    for (int i = 3; i <= 12; i++) {
      entry = Iterables.get(reportRow.getEntries(), i);
      assertEquals(DateUtil.endOfMonth(2014, i), entry.getInstant());
      assertEquals(BigDecimalUtil.valueOf(0),
          entry.getMeasure(ModelDistributionTransaction.ATTRIBUTE_AMOUNT));
    }
  }

  private ReportingAccounts initReportingDataset() throws StorageException {
    ReportingAccounts reportingAccounts = new ReportingAccounts();

    Transaction transaction = store.startTransaction();
    reportingAccounts.model1 = modelManager.createNewModel("Alternative Model 1");
    reportingAccounts.model2 = modelManager.createNewModel("Alternative Model 2");

    AccountCode accountCode = accountCodeManager.createAccountCode(1000, "Unimportant");
    reportingAccounts.revenueAccount =
        ledgerManager.createRevenueAccount(accountCode, "Revenue", null);
    reportingAccounts.monthlyExpenseAccount =
        ledgerManager.createExpenseAccount(accountCode, "Monthly Expense", null);
    reportingAccounts.annualExpenseAccount =
        ledgerManager.createExpenseAccount(accountCode, "Annual Expense", null);
    reportingAccounts.longTermExpenseAccount =
        ledgerManager.createExpenseAccount(accountCode, "Long Term Expense", null);

    // Revenue with model 1.
    modelManager.setMonthlyRecurringEventDetails(reportingAccounts.model1,
        reportingAccounts.revenueAccount,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), BigDecimalUtil.valueOf(1000));
    // Revenue with model 2.
    modelManager.setMonthlyRecurringEventDetails(reportingAccounts.model2,
        reportingAccounts.revenueAccount,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), BigDecimalUtil.valueOf(1100));

    modelManager.setMonthlyRecurringEventDetails(modelManager.getBaseModel(),
        reportingAccounts.monthlyExpenseAccount,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), BigDecimalUtil.valueOf(200));

    modelManager.setAnnualRecurringEventDetails(modelManager.getBaseModel(),
        reportingAccounts.annualExpenseAccount,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), 3, BigDecimalUtil.valueOf(7000));

    // Long term expense in model 1 payed in 2020.
    modelManager.createAdHocEvent(reportingAccounts.model1,
        reportingAccounts.longTermExpenseAccount, Instant.parse("2020-02-01"),
        10, DistributionTimeUnit.YEARS, BigDecimalUtil.valueOf(30000));

    // Long term expense in model 2 payed in 2021.
    modelManager.createAdHocEvent(reportingAccounts.model2,
        reportingAccounts.longTermExpenseAccount, Instant.parse("2021-02-01"),
        10, DistributionTimeUnit.YEARS, BigDecimalUtil.valueOf(30000));

    transaction.commit();

    return reportingAccounts;
  }

  private static class ReportingAccounts {
    public Model model1;
    public Model model2;
    public RevenueAccount revenueAccount;
    public ExpenseAccount monthlyExpenseAccount;
    public ExpenseAccount annualExpenseAccount;
    public ExpenseAccount longTermExpenseAccount;
  }
}
