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
import org.chaston.oakfunds.storage.StorageException;
import org.joda.time.Instant;

import java.math.BigDecimal;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface LedgerManager {
  BankAccount getBankAccount(int id) throws StorageException;

  BankAccount createBankAccount(AccountCode accountCode, String title) throws StorageException;

  void setInterestRate(BankAccount bankAccount, BigDecimal interestRate, Instant start, Instant end)
      throws StorageException;

  BigDecimal getInterestRate(BankAccount bankAccount, Instant date) throws StorageException;

  BigDecimal getBalance(BankAccount bankAccount, Instant date) throws StorageException;

  ExpenseAccount createExpenseAccount(AccountCode accountCode, String title,
      BankAccount defaultSourceAccount) throws StorageException;

  RevenueAccount createRevenueAccount(AccountCode accountCode, String title,
      BankAccount defaultDepositAccount) throws StorageException;

  void recordTransaction(Account account, Instant date, BigDecimal amount) throws StorageException;

  void recordTransaction(Account account, Instant date, BigDecimal amount, String comment)
      throws StorageException;

}
