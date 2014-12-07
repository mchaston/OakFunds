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

import org.chaston.oakfunds.account.AccountCode;
import org.chaston.oakfunds.storage.Report;
import org.chaston.oakfunds.storage.ReportDateGranularity;
import org.chaston.oakfunds.storage.StorageException;
import org.joda.time.Instant;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface LedgerManager {
  BankAccount getBankAccount(int id) throws StorageException;

  BankAccount createBankAccount(AccountCode accountCode, String title,
      BankAccountType bankAccountType) throws StorageException;

  BankAccount updateBankAccount(BankAccount bankAccount,
      AccountCode accountCode, String title, BankAccountType bankAccountType)
      throws StorageException;

  void setInterestRate(BankAccount bankAccount, BigDecimal interestRate, Instant start, Instant end)
      throws StorageException;

  BigDecimal getInterestRate(BankAccount bankAccount, Instant date) throws StorageException;

  BigDecimal getBalance(BankAccount bankAccount, Instant date) throws StorageException;

  ExpenseAccount getExpenseAccount(int id) throws StorageException;

  ExpenseAccount createExpenseAccount(AccountCode accountCode, String title,
      @Nullable BankAccount defaultSourceAccount) throws StorageException;

  ExpenseAccount updateExpenseAccount(ExpenseAccount expenseAccount,
      AccountCode accountCode, String title, @Nullable BankAccount defaultSourceAccount)
      throws StorageException;

  RevenueAccount getRevenueAccount(int id) throws StorageException;

  RevenueAccount createRevenueAccount(AccountCode accountCode, String title,
      @Nullable BankAccount defaultDepositAccount) throws StorageException;

  RevenueAccount updateRevenueAccount(RevenueAccount expenseAccount,
      AccountCode accountCode, String title, @Nullable BankAccount defaultDepositAccount)
      throws StorageException;

  Iterable<Account> getAccounts() throws StorageException;

  Account getAccount(int id) throws StorageException;

  Iterable<BankAccount> getBankAccounts() throws StorageException;

  AccountTransaction recordTransaction(Account account, Instant date, BigDecimal amount)
      throws StorageException;

  AccountTransaction recordTransaction(Account account, Instant date, BigDecimal amount,
      String comment) throws StorageException;

  Iterable<AccountTransaction> getAccountTransactions(Account account) throws StorageException;

  Report runReport(Account<?> account, int startYear, int endYear,
      ReportDateGranularity granularity) throws StorageException;
}
