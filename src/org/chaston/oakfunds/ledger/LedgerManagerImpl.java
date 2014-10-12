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

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.chaston.oakfunds.account.AccountCode;
import org.chaston.oakfunds.model.ModelAccount;
import org.chaston.oakfunds.storage.ParentIdentifierSearchTerm;
import org.chaston.oakfunds.storage.Report;
import org.chaston.oakfunds.storage.ReportDateGranularity;
import org.chaston.oakfunds.storage.SearchTerm;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.util.DateUtil;
import org.joda.time.Instant;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
class LedgerManagerImpl implements LedgerManager {

  private final Store store;

  @Inject
  LedgerManagerImpl(Store store) {
    this.store = store;
    store.registerType(Account.TYPE
    );
    store.registerType(AccountTransaction.TYPE
    );
    store.registerType(BankAccount.TYPE
    );
    store.registerType(BankAccountInterest.TYPE
    );
    store.registerType(ExpenseAccount.TYPE
    );
    store.registerType(RevenueAccount.TYPE
    );
  }

  @Override
  public BankAccount createBankAccount(AccountCode accountCode, String title,
      BankAccountType bankAccountType) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(Account.ATTRIBUTE_TITLE, title);
    attributes.put(BankAccount.ATTRIBUTE_BANK_ACCOUNT_TYPE, bankAccountType);
    return store.createRecord(BankAccount.TYPE, attributes);
  }

  @Override
  public void setInterestRate(BankAccount bankAccount, BigDecimal interestRate, Instant start,
      Instant end) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(BankAccountInterest.ATTRIBUTE_INTEREST_RATE, interestRate);
    store.updateIntervalRecord(bankAccount, BankAccountInterest.TYPE, start, end,
        attributes);
  }

  @Override
  public BigDecimal getInterestRate(BankAccount bankAccount, Instant date) throws StorageException {
    BankAccountInterest interestRecord =
        store.getIntervalRecord(bankAccount, BankAccountInterest.TYPE, date);
    return interestRecord.getInterestRate();
  }

  @Override
  public BigDecimal getBalance(BankAccount bankAccount, Instant date) throws StorageException {
    Iterable<AccountTransaction> accountTransactions =
        store.findInstantRecords(bankAccount, AccountTransaction.TYPE,
            DateUtil.BEGINNING_OF_TIME, date.plus(1), ImmutableList.<SearchTerm>of());
    BigDecimal balance = BigDecimal.ZERO;
    for (AccountTransaction accountTransaction : accountTransactions) {
      balance = balance.add(accountTransaction.getAmount());
    }
    return balance;
  }

  @Override
  public BankAccount getBankAccount(int id) throws StorageException {
    return store.getRecord(BankAccount.TYPE, id);
  }

  @Override
  public ExpenseAccount createExpenseAccount(AccountCode accountCode, String title,
      BankAccount defaultSourceAccount) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(Account.ATTRIBUTE_TITLE, title);
    attributes.put(ExpenseAccount.ATTRIBUTE_DEFAULT_SOURCE_ACCOUNT_ID, defaultSourceAccount.getId());
    return store.createRecord(ExpenseAccount.TYPE, attributes);
  }

  @Override
  public RevenueAccount createRevenueAccount(AccountCode accountCode, String title,
      BankAccount defaultDepositAccount) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(Account.ATTRIBUTE_TITLE, title);
    attributes.put(RevenueAccount.ATTRIBUTE_DEFAULT_DEPOSIT_ACCOUNT_ID, defaultDepositAccount.getId());
    return store.createRecord(RevenueAccount.TYPE, attributes);
  }

  @Override
  public void recordTransaction(Account account, Instant date, BigDecimal amount)
      throws StorageException {
    recordTransaction(account, date, amount, null);
  }

  @Override
  public void recordTransaction(Account account, Instant date, BigDecimal amount, String comment)
      throws StorageException {
    recordTransaction(account, date, amount, comment, null);
  }

  @Override
  public void setRelatedModelAccount(Account account, ModelAccount modelAccount,
      PaymentIncrement paymentIncrement, boolean retroactive) throws StorageException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setRelatedModelAccount(AccountTransaction account, ModelAccount modelAccount,
      PaymentIncrement paymentIncrement) throws StorageException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public Report runReport(Account<?> account, int startYear, int endYear,
      ReportDateGranularity granularity) {
    ImmutableList<? extends SearchTerm> searchTerms =
        ImmutableList.of(ParentIdentifierSearchTerm.of(account.getRecordType(), account.getId()));
    ImmutableList<String> dimensions = ImmutableList.of();
    ImmutableList<String> measures = ImmutableList.of(AccountTransaction.ATTRIBUTE_AMOUNT);
    return store.runReport(AccountTransaction.TYPE, startYear, endYear, granularity,
        searchTerms, dimensions, measures);
  }

  private void recordTransaction(Account account, Instant date, BigDecimal amount, String comment,
      Integer sisterTransactionId) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(AccountTransaction.ATTRIBUTE_AMOUNT, amount);
    if (comment != null) {
      attributes.put(AccountTransaction.ATTRIBUTE_COMMENT, comment);
    }
    if (sisterTransactionId != null) {
      attributes.put(AccountTransaction.ATTRIBUTE_SISTER_TRANSACTION_ID, sisterTransactionId);
    }
    AccountTransaction transaction =
        store.insertInstantRecord(account, AccountTransaction.TYPE, date, attributes);
    if (account instanceof ExpenseAccount) {
      ExpenseAccount expenseAccount = (ExpenseAccount) account;
      BankAccount bankAccount = getBankAccount(expenseAccount.getDefaultSourceAccountId());
      recordTransaction(bankAccount, date, amount.negate(), comment, transaction.getId());
    }
    if (account instanceof RevenueAccount) {
      RevenueAccount revenueAccount = (RevenueAccount) account;
      BankAccount bankAccount = getBankAccount(revenueAccount.getDefaultDepositAccountId());
      recordTransaction(bankAccount, date, amount, comment, transaction.getId());
    }
  }
}
