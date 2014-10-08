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

import com.google.common.collect.Iterables;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.chaston.oakfunds.account.AccountCodeManager;
import org.chaston.oakfunds.account.AccountCodeModule;
import org.chaston.oakfunds.ledger.BankAccountType;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.storage.TestStorageModule;
import org.chaston.oakfunds.storage.Transaction;
import org.chaston.oakfunds.system.TestSystemModuleBuilder;
import org.chaston.oakfunds.util.DateUtil;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class ModelManagerTest {

  @Inject
  private AccountCodeManager accountCodeManager;
  @Inject
  private ModelManager modelManager;
  @Inject
  private Store store;

  @Before
  public void setUp() {
    Injector injector = Guice.createInjector(
        new AccountCodeModule(),
        new ModelModule(),
        new TestSystemModuleBuilder()
            .setCurrentYear(Instant.parse("2014-01-01").get(DateTimeFieldType.year()))
            .setTimeHorizon(10)
            .build(),
        new TestStorageModule());
    injector.injectMembers(this);
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
  public void createModelExpenseAccount() throws StorageException {
    Transaction transaction = store.startTransaction();
    ModelExpenseAccount expenseAccount =
        modelManager.createModelExpenseAccount("Electricity / Water", BankAccountType.OPERATING);
    transaction.commit();

    assertNotNull(expenseAccount);
  }

  @Test
  public void createModelRevenueAccount() throws StorageException {
    Transaction transaction = store.startTransaction();
    ModelRevenueAccount revenueAccount =
        modelManager.createModelRevenueAccount("Dues", BankAccountType.OPERATING);
    transaction.commit();

    assertNotNull(revenueAccount);
  }

  @Test
  public void setMonthlyRecurringEventDetails() throws StorageException {
    Transaction transaction = store.startTransaction();
    ModelExpenseAccount expenseAccount =
        modelManager.createModelExpenseAccount("Electricity / Water", BankAccountType.OPERATING);
    MonthlyRecurringEvent monthlyRecurringEvent =
        modelManager.setMonthlyRecurringEventDetails(modelManager.getBaseModel(), expenseAccount,
            DateUtil.BEGINNING_OF_TIME, DateUtil.END_OF_TIME,
            BigDecimal.valueOf(3000.0));
    transaction.commit();

    assertEquals(BigDecimal.valueOf(3000.0), monthlyRecurringEvent.getAmount());

    Iterable<ModelAccountTransaction> modelTransactions =
        modelManager.getModelTransactions(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2014-01-01"), Instant.parse("2015-01-01"));

    assertEquals(12, Iterables.size(modelTransactions));
    ModelAccountTransaction firstModelTransaction = Iterables.getFirst(modelTransactions, null);
    assertNotNull(firstModelTransaction);
    assertEquals(modelManager.getBaseModel().getId(), firstModelTransaction.getModelId());
    assertEquals(expenseAccount.getId(), firstModelTransaction.getAccountId());
    assertEquals(BigDecimal.valueOf(3000.0), firstModelTransaction.getAmount());
    assertEquals(Instant.parse("2014-01-01"), firstModelTransaction.getInstant());

    Iterable<ModelDistributionTransaction> modelDistributionTransactions =
        modelManager.getModelDistributionTransactions(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2014-01-01"), Instant.parse("2015-01-01"));

    assertEquals(0, Iterables.size(modelDistributionTransactions));
  }

  @Test
  public void setAnnualRecurringEventDetails() throws StorageException {
    Transaction transaction = store.startTransaction();
    ModelExpenseAccount expenseAccount =
        modelManager.createModelExpenseAccount("Insurance", BankAccountType.OPERATING);
    AnnualRecurringEvent annualRecurringEvent =
        modelManager.setAnnualRecurringEventDetails(modelManager.getBaseModel(), expenseAccount,
            DateUtil.BEGINNING_OF_TIME, DateUtil.END_OF_TIME,
            3, BigDecimal.valueOf(12000.0));
    transaction.commit();

    assertEquals(BigDecimal.valueOf(12000.0), annualRecurringEvent.getAmount());
    assertEquals(3, annualRecurringEvent.getPaymentMonth());

    Iterable<ModelAccountTransaction> modelTransactions =
        modelManager.getModelTransactions(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2014-01-01"), Instant.parse("2015-01-01"));

    assertEquals(1, Iterables.size(modelTransactions));
    ModelAccountTransaction modelTransaction = Iterables.getOnlyElement(modelTransactions);
    assertEquals(modelManager.getBaseModel().getId(), modelTransaction.getModelId());
    assertEquals(expenseAccount.getId(), modelTransaction.getAccountId());
    assertEquals(BigDecimal.valueOf(12000.0), modelTransaction.getAmount());
    assertEquals(Instant.parse("2014-03-01"), modelTransaction.getInstant());

    Iterable<ModelDistributionTransaction> modelDistributionTransactions =
        modelManager.getModelDistributionTransactions(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2014-01-01"), Instant.parse("2015-01-01"));

    assertEquals(12, Iterables.size(modelDistributionTransactions));

    ModelDistributionTransaction firstModelDistributionTransaction =
        Iterables.getFirst(modelDistributionTransactions, null);
    assertNotNull(firstModelDistributionTransaction);
    assertEquals(modelManager.getBaseModel().getId(), firstModelDistributionTransaction.getModelId());
    assertEquals(expenseAccount.getId(), firstModelDistributionTransaction.getAccountId());
    // This is the sum of distributions until the first month of the current year.
    assertEquals(BigDecimal.valueOf(10000.0), firstModelDistributionTransaction.getAmount());
    assertEquals(Instant.parse("2014-01-01"), firstModelDistributionTransaction.getInstant());

    // This is the distribution that cancels out previous distributions.
    ModelDistributionTransaction thirdModelDistributionTransaction =
        Iterables.get(modelDistributionTransactions, 2, null);
    assertNotNull(thirdModelDistributionTransaction);
    assertEquals(modelManager.getBaseModel().getId(), thirdModelDistributionTransaction.getModelId());
    assertEquals(expenseAccount.getId(), thirdModelDistributionTransaction.getAccountId());
    assertEquals(BigDecimal.valueOf(-11000.0), thirdModelDistributionTransaction.getAmount());
    assertEquals(Instant.parse("2014-03-01"), thirdModelDistributionTransaction.getInstant());
    assertEquals(firstModelDistributionTransaction.getModelAccountTransactionId(),
        thirdModelDistributionTransaction.getModelAccountTransactionId());

    ModelDistributionTransaction fourthModelDistributionTransaction =
        Iterables.get(modelDistributionTransactions, 3, null);
    assertNotNull(fourthModelDistributionTransaction);
    assertEquals(modelManager.getBaseModel().getId(),
        fourthModelDistributionTransaction.getModelId());
    assertEquals(expenseAccount.getId(), fourthModelDistributionTransaction.getAccountId());
    assertEquals(BigDecimal.valueOf(1000.0), fourthModelDistributionTransaction.getAmount());
    assertEquals(Instant.parse("2014-04-01"), fourthModelDistributionTransaction.getInstant());
    // Different transaction from the previous ones.
    assertNotEquals(firstModelDistributionTransaction.getModelAccountTransactionId(),
        fourthModelDistributionTransaction.getModelAccountTransactionId());
  }

  @Test
  public void createAdHocEvent() throws StorageException {
    Transaction transaction = store.startTransaction();
    ModelExpenseAccount expenseAccount =
        modelManager.createModelExpenseAccount("House Painting", BankAccountType.OPERATING);
    ModelAccountTransaction modelAccountTransaction =
        modelManager.createAdHocEvent(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2017-01-01"),
            5, DistributionTimeUnit.YEARS, BigDecimal.valueOf(60000.0));
    transaction.commit();

    assertNotNull(modelAccountTransaction);

    Iterable<ModelDistributionTransaction> modelDistributionTransactions =
        modelManager.getModelDistributionTransactions(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2014-01-01"), Instant.parse("2015-01-01"));

    assertEquals(12, Iterables.size(modelDistributionTransactions));
    ModelDistributionTransaction firstModelDistributionTransaction = Iterables.getFirst(
        modelDistributionTransactions, null);
    assertNotNull(firstModelDistributionTransaction);
    assertEquals(modelManager.getBaseModel().getId(), firstModelDistributionTransaction.getModelId());
    assertEquals(expenseAccount.getId(), firstModelDistributionTransaction.getAccountId());
    // First one is a roll up of the previous distributions.
    assertEquals(BigDecimal.valueOf(24000.0), firstModelDistributionTransaction.getAmount());
    assertEquals(Instant.parse("2014-01-01"), firstModelDistributionTransaction.getInstant());

    // Regular ones are a normal size.
    ModelDistributionTransaction secondModelDistributionTransaction =
        Iterables.get(modelDistributionTransactions, 1, null);
    assertNotNull(secondModelDistributionTransaction);
    assertEquals(modelManager.getBaseModel().getId(), secondModelDistributionTransaction.getModelId());
    assertEquals(expenseAccount.getId(), secondModelDistributionTransaction.getAccountId());
    assertEquals(BigDecimal.valueOf(1000.0), secondModelDistributionTransaction.getAmount());
    assertEquals(Instant.parse("2014-02-01"), secondModelDistributionTransaction.getInstant());
  }

  @Test
  public void createAdHocEventBeyondTimeHorizon() throws StorageException {
    Transaction transaction = store.startTransaction();
    ModelExpenseAccount expenseAccount =
        modelManager.createModelExpenseAccount("House Painting", BankAccountType.OPERATING);
    ModelAccountTransaction modelAccountTransaction =
        modelManager.createAdHocEvent(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2027-01-01"),
            50, DistributionTimeUnit.YEARS, BigDecimal.valueOf(60000.0));
    transaction.commit();

    assertNotNull(modelAccountTransaction);

    Iterable<ModelDistributionTransaction> modelDistributionTransactions =
        modelManager.getModelDistributionTransactions(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2014-01-01"), Instant.parse("2015-01-01"));

    assertEquals(12, Iterables.size(modelDistributionTransactions));
    ModelDistributionTransaction firstModelDistributionTransaction = Iterables.getFirst(
        modelDistributionTransactions, null);
    assertNotNull(firstModelDistributionTransaction);
    assertEquals(modelManager.getBaseModel().getId(), firstModelDistributionTransaction.getModelId());
    assertEquals(expenseAccount.getId(), firstModelDistributionTransaction.getAccountId());
    // First one is a roll up of the previous distributions.
    assertEquals(BigDecimal.valueOf(44400.0), firstModelDistributionTransaction.getAmount());
    assertEquals(Instant.parse("2014-01-01"), firstModelDistributionTransaction.getInstant());

    // Regular ones are a normal size.
    ModelDistributionTransaction secondModelDistributionTransaction =
        Iterables.get(modelDistributionTransactions, 1, null);
    assertNotNull(secondModelDistributionTransaction);
    assertEquals(modelManager.getBaseModel().getId(), secondModelDistributionTransaction.getModelId());
    assertEquals(expenseAccount.getId(), secondModelDistributionTransaction.getAccountId());
    assertEquals(BigDecimal.valueOf(100.0), secondModelDistributionTransaction.getAmount());
    assertEquals(Instant.parse("2014-02-01"), secondModelDistributionTransaction.getInstant());
  }

  @Test
  public void updateAdHocEvent() throws StorageException {
    // Create the initial event.
    Transaction transaction = store.startTransaction();
    ModelExpenseAccount expenseAccount =
        modelManager.createModelExpenseAccount("House Painting", BankAccountType.OPERATING);
    modelManager.createAdHocEvent(modelManager.getBaseModel(), expenseAccount,
        Instant.parse("2017-01-01"),
        5, DistributionTimeUnit.YEARS, BigDecimal.valueOf(60000.0));
    transaction.commit();

    Iterable<ModelDistributionTransaction> oldModelDistributionTransactions =
        modelManager.getModelDistributionTransactions(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2014-01-01"), Instant.parse("2015-01-01"));

    // Get the event back.
    ModelAccountTransaction modelAccountTransaction = Iterables.getOnlyElement(
        modelManager.getModelTransactions(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2016-01-01"), Instant.parse("2017-02-01")));

    transaction = store.startTransaction();
    modelManager.updateAdHocEvent(modelAccountTransaction,
        Instant.parse("2017-06-01"),
        5, DistributionTimeUnit.YEARS, BigDecimal.valueOf(60000.0));
    transaction.commit();

    Iterable<ModelDistributionTransaction> newModelDistributionTransactions =
        modelManager.getModelDistributionTransactions(modelManager.getBaseModel(), expenseAccount,
            Instant.parse("2014-01-01"), Instant.parse("2015-01-01"));

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
    ModelExpenseAccount expenseAccount =
        modelManager.createModelExpenseAccount("House Painting", BankAccountType.OPERATING);
    modelManager.createAdHocEvent(modelManager.getBaseModel(), expenseAccount,
        Instant.parse("2017-01-01"),
        5, DistributionTimeUnit.YEARS, BigDecimal.valueOf(60000.0));
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
            Instant.parse("2014-01-01"), Instant.parse("2015-01-01"));
    assertTrue(Iterables.isEmpty(newModelDistributionTransactions));
  }
}
