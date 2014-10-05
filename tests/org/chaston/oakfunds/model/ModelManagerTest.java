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
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
            Calendar.MARCH, BigDecimal.valueOf(12000.0));
    transaction.commit();

    assertEquals(BigDecimal.valueOf(12000.0), annualRecurringEvent.getAmount());
    assertEquals(Calendar.MARCH, annualRecurringEvent.getPaymentMonth());

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
    ModelDistributionTransaction firstModelDistributionTransaction = Iterables.getFirst(
        modelDistributionTransactions, null);
    assertNotNull(firstModelDistributionTransaction);
    assertEquals(modelManager.getBaseModel().getId(), firstModelDistributionTransaction.getModelId());
    assertEquals(expenseAccount.getId(), firstModelDistributionTransaction.getAccountId());
    assertEquals(BigDecimal.valueOf(1000.0), firstModelDistributionTransaction.getAmount());
    assertEquals(Instant.parse("2014-01-01"), firstModelDistributionTransaction.getInstant());
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
    assertEquals(BigDecimal.valueOf(1000.0), firstModelDistributionTransaction.getAmount());
    assertEquals(Instant.parse("2014-01-01"), firstModelDistributionTransaction.getInstant());
  }
}
