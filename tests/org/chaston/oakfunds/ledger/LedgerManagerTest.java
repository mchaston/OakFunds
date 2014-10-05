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
package org.chaston.oakfunds.ledger;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.chaston.oakfunds.account.AccountCode;
import org.chaston.oakfunds.account.AccountCodeManager;
import org.chaston.oakfunds.account.AccountCodeModule;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.storage.TestStorageModule;
import org.chaston.oakfunds.storage.Transaction;
import org.chaston.oakfunds.util.DateUtil;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class LedgerManagerTest {

  @Inject
  private AccountCodeManager accountCodeManager;
  @Inject
  private LedgerManager ledgerManager;
  @Inject
  private Store store;

  @Before
  public void setUp() {
    Injector injector = Guice.createInjector(
        new AccountCodeModule(),
        new LedgerModule(),
        new TestStorageModule());
    injector.injectMembers(this);
  }

  @Test
  public void createNewBankAccount() throws StorageException {
    Transaction transaction = store.startTransaction();
    AccountCode accountCode = accountCodeManager.createAccountCode(80000, "Operating");
    BankAccount bankAccount = ledgerManager.createBankAccount(accountCode, "Bob's bank", BankAccountType.OPERATING);
    BigDecimal interestRate = BigDecimal.valueOf(0.03); // 3%
    ledgerManager.setInterestRate(bankAccount, interestRate, DateUtil.BEGINNING_OF_TIME,
        DateUtil.END_OF_TIME);
    transaction.commit();

    transaction = store.startTransaction();
    Instant date = Instant.parse("2014-09-28");
    BigDecimal amount = BigDecimal.valueOf(12000);
    ledgerManager.recordTransaction(bankAccount, date, amount, "Initial value");
    transaction.commit();

    transaction = store.startTransaction();
    BigDecimal interestRate2 = BigDecimal.valueOf(0.04); // 4%
    ledgerManager.setInterestRate(bankAccount, interestRate2, Instant.parse("2014-01-01"),
        Instant.parse("2015-01-01"));
    transaction.commit();

    bankAccount = ledgerManager.getBankAccount(bankAccount.getId());
    assertEquals(interestRate,
        ledgerManager.getInterestRate(bankAccount, Instant.parse("2013-01-01")));
    assertEquals(interestRate2,
        ledgerManager.getInterestRate(bankAccount, Instant.parse("2014-01-01")));
    assertEquals(interestRate,
        ledgerManager.getInterestRate(bankAccount, Instant.parse("2015-01-01")));
  }

  @Test
  public void payExpense() throws StorageException {
    Transaction transaction = store.startTransaction();
    AccountCode operatingAccountCode = accountCodeManager.createAccountCode(80000, "Operating");
    BankAccount bankAccount = ledgerManager.createBankAccount(operatingAccountCode, "Bob's bank", BankAccountType.OPERATING);
    BigDecimal interestRate = BigDecimal.valueOf(0.03); // 3%
    ledgerManager.setInterestRate(bankAccount, interestRate, DateUtil.BEGINNING_OF_TIME,
        DateUtil.END_OF_TIME);
    transaction.commit();

    transaction = store.startTransaction();
    Instant date = Instant.parse("2014-09-28");
    BigDecimal amount = BigDecimal.valueOf(12000);
    ledgerManager.recordTransaction(bankAccount, date, amount, "Initial value");
    transaction.commit();

    transaction = store.startTransaction();
    AccountCode electricityAccountCode = accountCodeManager.createAccountCode(50000, "Electricity");
    ExpenseAccount electricityExpenseAccount =
        ledgerManager.createExpenseAccount(electricityAccountCode, "PG&E", bankAccount);
    transaction.commit();

    transaction = store.startTransaction();
    date = Instant.parse("2014-09-29");
    amount = BigDecimal.valueOf(1000);
    ledgerManager.recordTransaction(electricityExpenseAccount, date, amount);
    transaction.commit();

    assertEquals(BigDecimal.valueOf(0),
        ledgerManager.getBalance(bankAccount, Instant.parse("2014-09-27")));
    assertEquals(BigDecimal.valueOf(12000),
        ledgerManager.getBalance(bankAccount, Instant.parse("2014-09-28")));
    assertEquals(BigDecimal.valueOf(11000),
        ledgerManager.getBalance(bankAccount, Instant.parse("2014-09-29")));
  }

  @Test
  public void payExpenseWithRevenue() throws StorageException {
    Transaction transaction = store.startTransaction();
    AccountCode operatingAccountCode = accountCodeManager.createAccountCode(80000, "Operating");
    BankAccount bankAccount = ledgerManager.createBankAccount(operatingAccountCode, "Bob's bank", BankAccountType.OPERATING);
    BigDecimal interestRate = BigDecimal.valueOf(0.03); // 3%
    ledgerManager.setInterestRate(bankAccount, interestRate, DateUtil.BEGINNING_OF_TIME,
        DateUtil.END_OF_TIME);
    transaction.commit();

    transaction = store.startTransaction();
    Instant date = Instant.parse("2014-09-28");
    BigDecimal amount = BigDecimal.valueOf(12000);
    ledgerManager.recordTransaction(bankAccount, date, amount, "Initial value");
    transaction.commit();

    transaction = store.startTransaction();
    AccountCode electricityAccountCode = accountCodeManager.createAccountCode(50000, "Electricity");
    ExpenseAccount electricityExpenseAccount =
        ledgerManager.createExpenseAccount(electricityAccountCode, "PG&E", bankAccount);
    transaction.commit();

    transaction = store.startTransaction();
    AccountCode interestAccountCode = accountCodeManager.createAccountCode(31000, "Operating Interest");
    RevenueAccount interestRevenueAccount =
        ledgerManager.createRevenueAccount(interestAccountCode, "Interest from Bob's Bank", bankAccount);
    transaction.commit();

    transaction = store.startTransaction();
    date = Instant.parse("2014-09-29");
    amount = BigDecimal.valueOf(1000);
    ledgerManager.recordTransaction(electricityExpenseAccount, date, amount);
    transaction.commit();

    transaction = store.startTransaction();
    date = Instant.parse("2014-09-28");
    amount = BigDecimal.valueOf(2000);
    ledgerManager.recordTransaction(interestRevenueAccount, date, amount);
    transaction.commit();

    assertEquals(BigDecimal.valueOf(0),
        ledgerManager.getBalance(bankAccount, Instant.parse("2014-09-27")));
    assertEquals(BigDecimal.valueOf(14000),
        ledgerManager.getBalance(bankAccount, Instant.parse("2014-09-28")));
    assertEquals(BigDecimal.valueOf(13000),
        ledgerManager.getBalance(bankAccount, Instant.parse("2014-09-29")));
  }
}
