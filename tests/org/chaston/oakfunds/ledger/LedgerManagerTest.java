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

import com.google.common.collect.Iterables;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.chaston.oakfunds.account.AccountCode;
import org.chaston.oakfunds.account.AccountCodeManager;
import org.chaston.oakfunds.account.AccountCodeModule;
import org.chaston.oakfunds.bootstrap.BootstrapModule;
import org.chaston.oakfunds.jdbc.DatabaseTearDown;
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

import static org.junit.Assert.assertEquals;

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class LedgerManagerTest {

  private static final int YEAR_2014 = Instant.parse("2014-01-01").get(DateTimeFieldType.year());
  private static final int YEAR_2015 = Instant.parse("2015-01-01").get(DateTimeFieldType.year());

  @Inject
  private AccountCodeManager accountCodeManager;
  @Inject
  private UserAuthenticationManager userAuthenticationManager;
  @Inject
  private LedgerManager ledgerManager;
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
        new LedgerModule(),
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
  public void createNewBankAccount() throws StorageException {
    Transaction transaction = store.startTransaction();
    AccountCode accountCode = accountCodeManager.createAccountCode(80000, "Operating");
    BankAccount bankAccount = ledgerManager.createBankAccount(accountCode, "Bob's bank", BankAccountType.OPERATING);
    BigDecimal interestRate = BigDecimalUtil.valueOf(0.03); // 3%
    ledgerManager.setInterestRate(bankAccount, interestRate, DateUtil.BEGINNING_OF_TIME,
        DateUtil.END_OF_TIME);
    transaction.commit();

    transaction = store.startTransaction();
    Instant date = Instant.parse("2014-09-28");
    BigDecimal amount = BigDecimalUtil.valueOf(12000);
    ledgerManager.recordTransaction(bankAccount, date, amount, "Initial value");
    transaction.commit();

    transaction = store.startTransaction();
    BigDecimal interestRate2 = BigDecimalUtil.valueOf(0.04); // 4%
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
  public void getAccounts() throws StorageException {
    Transaction transaction = store.startTransaction();
    AccountCode operatingAccountCode = accountCodeManager.createAccountCode(80000, "Operating");
    BankAccount bankAccount = ledgerManager.createBankAccount(operatingAccountCode, "SF bank", BankAccountType.OPERATING);
    transaction.commit();

    transaction = store.startTransaction();
    AccountCode electricityAccountCode = accountCodeManager.createAccountCode(50000, "Electricity");
    ExpenseAccount electricityExpenseAccount =
        ledgerManager.createExpenseAccount(electricityAccountCode, "PG&E", bankAccount);
    transaction.commit();

    Iterable<Account> accounts = ledgerManager.getAccounts();
    assertEquals(2, Iterables.size(accounts));
    assertEquals(electricityExpenseAccount.getId(), Iterables.get(accounts, 0).getId());
    assertEquals(bankAccount.getId(), Iterables.get(accounts, 1).getId());
  }

  @Test
  public void payExpense() throws StorageException {
    Transaction transaction = store.startTransaction();
    AccountCode operatingAccountCode = accountCodeManager.createAccountCode(80000, "Operating");
    BankAccount bankAccount = ledgerManager.createBankAccount(operatingAccountCode, "Bob's bank", BankAccountType.OPERATING);
    BigDecimal interestRate = BigDecimalUtil.valueOf(0.03); // 3%
    ledgerManager.setInterestRate(bankAccount, interestRate, DateUtil.BEGINNING_OF_TIME,
        DateUtil.END_OF_TIME);
    transaction.commit();

    transaction = store.startTransaction();
    Instant date = Instant.parse("2014-09-28");
    BigDecimal amount = BigDecimalUtil.valueOf(12000);
    ledgerManager.recordTransaction(bankAccount, date, amount, "Initial value");
    transaction.commit();

    transaction = store.startTransaction();
    AccountCode electricityAccountCode = accountCodeManager.createAccountCode(50000, "Electricity");
    ExpenseAccount electricityExpenseAccount =
        ledgerManager.createExpenseAccount(electricityAccountCode, "PG&E", bankAccount);
    transaction.commit();

    transaction = store.startTransaction();
    date = Instant.parse("2014-09-29");
    amount = BigDecimalUtil.valueOf(1000);
    ledgerManager.recordTransaction(electricityExpenseAccount, date, amount);
    transaction.commit();

    assertEquals(BigDecimal.ZERO,
        ledgerManager.getBalance(bankAccount, Instant.parse("2014-09-27")));
    assertEquals(BigDecimalUtil.valueOf(12000),
        ledgerManager.getBalance(bankAccount, Instant.parse("2014-09-28")));
    assertEquals(BigDecimalUtil.valueOf(11000),
        ledgerManager.getBalance(bankAccount, Instant.parse("2014-09-29")));
  }

  @Test
  public void payExpenseWithRevenue() throws StorageException {
    Transaction transaction = store.startTransaction();
    AccountCode operatingAccountCode = accountCodeManager.createAccountCode(80000, "Operating");
    BankAccount bankAccount = ledgerManager.createBankAccount(operatingAccountCode, "Bob's bank", BankAccountType.OPERATING);
    BigDecimal interestRate = BigDecimalUtil.valueOf(0.03); // 3%
    ledgerManager.setInterestRate(bankAccount, interestRate, DateUtil.BEGINNING_OF_TIME,
        DateUtil.END_OF_TIME);
    transaction.commit();

    transaction = store.startTransaction();
    Instant date = Instant.parse("2014-09-28");
    BigDecimal amount = BigDecimalUtil.valueOf(12000);
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
    amount = BigDecimalUtil.valueOf(1000);
    ledgerManager.recordTransaction(electricityExpenseAccount, date, amount);
    transaction.commit();

    transaction = store.startTransaction();
    date = Instant.parse("2014-09-28");
    amount = BigDecimalUtil.valueOf(2000);
    ledgerManager.recordTransaction(interestRevenueAccount, date, amount);
    transaction.commit();

    assertEquals(BigDecimal.ZERO,
        ledgerManager.getBalance(bankAccount, Instant.parse("2014-09-27")));
    assertEquals(BigDecimalUtil.valueOf(14000),
        ledgerManager.getBalance(bankAccount, Instant.parse("2014-09-28")));
    assertEquals(BigDecimalUtil.valueOf(13000),
        ledgerManager.getBalance(bankAccount, Instant.parse("2014-09-29")));
  }

  @Test
  public void runReportForOneAccount() throws StorageException {
    ReportingAccounts accounts = initReportingDataset();

    Report report = ledgerManager.runReport(accounts.bankAccount,
        YEAR_2014, YEAR_2015, ReportDateGranularity.MONTH);

    assertEquals(Iterables.size(report.getRows()), 1);
    ReportRow reportRow = Iterables.getOnlyElement(report.getRows());

    assertEquals(13, Iterables.size(reportRow.getEntries()));

    ReportEntry entry = Iterables.get(reportRow.getEntries(), 0);
    assertEquals(DateUtil.endOfYear(2013), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(11000),
        entry.getMeasure(AccountTransaction.ATTRIBUTE_AMOUNT));

    entry = Iterables.get(reportRow.getEntries(), 1);
    assertEquals(DateUtil.endOfMonth(2014, 1), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(12000),
        entry.getMeasure(AccountTransaction.ATTRIBUTE_AMOUNT));

    entry = Iterables.get(reportRow.getEntries(), 2);
    assertEquals(DateUtil.endOfMonth(2014, 2), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(13000),
        entry.getMeasure(AccountTransaction.ATTRIBUTE_AMOUNT));

    entry = Iterables.get(reportRow.getEntries(), 3);
    assertEquals(DateUtil.endOfMonth(2014, 3), entry.getInstant());
    assertEquals(BigDecimalUtil.valueOf(15000),
        entry.getMeasure(AccountTransaction.ATTRIBUTE_AMOUNT));

    // Same value through the end of the year.
    for (int i = 4; i <= 12; i++) {
      entry = Iterables.get(reportRow.getEntries(), i);
      assertEquals(DateUtil.endOfMonth(2014, i), entry.getInstant());
      assertEquals(BigDecimalUtil.valueOf(16000),
          entry.getMeasure(AccountTransaction.ATTRIBUTE_AMOUNT));
    }
  }

  private ReportingAccounts initReportingDataset() throws StorageException {
    ReportingAccounts reportingAccounts = new ReportingAccounts();

    Transaction transaction = store.startTransaction();
    AccountCode operatingAccountCode = accountCodeManager.createAccountCode(80000, "Operating");
    reportingAccounts.bankAccount =
        ledgerManager.createBankAccount(operatingAccountCode, "Bob's bank", BankAccountType.OPERATING);
    BigDecimal interestRate = BigDecimalUtil.valueOf(0.03); // 3%
    ledgerManager.setInterestRate(reportingAccounts.bankAccount,
        interestRate, DateUtil.BEGINNING_OF_TIME, DateUtil.END_OF_TIME);
    AccountCode interestAccountCode = accountCodeManager.createAccountCode(31000, "Operating Interest");
    reportingAccounts.interestRevenueAccount =
        ledgerManager.createRevenueAccount(interestAccountCode, "Interest from Bob's Bank",
            reportingAccounts.bankAccount);

    ledgerManager.recordTransaction(reportingAccounts.bankAccount,
        Instant.parse("2013-12-31"), BigDecimalUtil.valueOf(11000), "Initial value");
    ledgerManager.recordTransaction(reportingAccounts.interestRevenueAccount,
        Instant.parse("2014-01-04"), BigDecimalUtil.valueOf(1000));
    ledgerManager.recordTransaction(reportingAccounts.interestRevenueAccount,
        Instant.parse("2014-02-02"), BigDecimalUtil.valueOf(1000));
    ledgerManager.recordTransaction(reportingAccounts.interestRevenueAccount,
        Instant.parse("2014-03-01"), BigDecimalUtil.valueOf(1000));
    ledgerManager.recordTransaction(reportingAccounts.interestRevenueAccount,
        Instant.parse("2014-03-15"), BigDecimalUtil.valueOf(1000));
    ledgerManager.recordTransaction(reportingAccounts.interestRevenueAccount,
        Instant.parse("2014-04-15"), BigDecimalUtil.valueOf(1000));
    transaction.commit();

    return reportingAccounts;
  }

  private static class ReportingAccounts {
    public BankAccount bankAccount;
    public RevenueAccount interestRevenueAccount;
  }
}
