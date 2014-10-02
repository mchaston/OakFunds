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

import com.google.inject.Inject;
import org.chaston.oakfunds.account.AccountCode;
import org.chaston.oakfunds.storage.InstantRecordFactory;
import org.chaston.oakfunds.storage.IntervalRecordFactory;
import org.chaston.oakfunds.storage.RecordFactory;
import org.chaston.oakfunds.storage.RecordType;
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

  private static final RecordFactory<BankAccount> BANK_ACCOUNT_RECORD_FACTORY =
      new RecordFactory<BankAccount>() {
        @Override
        public BankAccount newInstance(int id) {
          return new BankAccount(id);
        }

        @Override
        public RecordType getRecordType() {
          return RecordType.BANK_ACCOUNT;
        }
      };

  private static final IntervalRecordFactory<BankAccountInterest> BANK_ACCOUNT_INTEREST_RECORD_FACTORY =
      new IntervalRecordFactory<BankAccountInterest>() {
        @Override
        public BankAccountInterest newInstance(int id, Instant start, Instant end) {
          return new BankAccountInterest(id, start, end);
        }

        @Override
        public RecordType getRecordType() {
          return RecordType.BANK_ACCOUNT_INTEREST;
        }
      };

  private static final InstantRecordFactory<AccountTransaction> ACCOUNT_TRANSACTION_RECORD_FACTORY =
      new InstantRecordFactory<AccountTransaction>() {
        @Override
        public AccountTransaction newInstance(int id, Instant instant) {
          return new AccountTransaction(id, instant);
        }

        @Override
        public RecordType getRecordType() {
          return RecordType.ACCOUNT_TRANSACTION;
        }
      };

  private static final RecordFactory<ExpenseAccount> EXPENSE_ACCOUNT_RECORD_FACTORY = new RecordFactory<ExpenseAccount>() {
    @Override
    public ExpenseAccount newInstance(int id) {
      return new ExpenseAccount(id);
    }

    @Override
    public RecordType getRecordType() {
      return RecordType.EXPENSE_ACCOUNT;
    }
  };

  private static final RecordFactory<RevenueAccount> REVENUE_ACCOUNT_RECORD_FACTORY = new RecordFactory<RevenueAccount>() {
    @Override
    public RevenueAccount newInstance(int id) {
      return new RevenueAccount(id);
    }

    @Override
    public RecordType getRecordType() {
      return RecordType.REVENUE_ACCOUNT;
    }
  };

  private static final String ATTRIBUTE_AMOUNT = "amount";
  private static final String ATTRIBUTE_COMMENT = "comment";
  private static final String ATTRIBUTE_DEFAULT_DEPOSIT_ACCOUNT_ID = "default_deposit_account_id";
  private static final String ATTRIBUTE_DEFAULT_SOURCE_ACCOUNT_ID = "default_source_account_id";
  private static final String ATTRIBUTE_INTEREST_RATE = "interest_rate";
  private static final String ATTRIBUTE_SISTER_TRANSACTION_ID = "sister_transaction_id";
  private static final String ATTRIBUTE_TITLE = "title";

  private final Store store;

  @Inject
  LedgerManagerImpl(Store store) {
    this.store = store;
  }

  @Override
  public BankAccount createBankAccount(AccountCode accountCode, String title)
      throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_TITLE, title);
    return store.createRecord(BANK_ACCOUNT_RECORD_FACTORY, attributes);
  }

  @Override
  public void setInterestRate(BankAccount bankAccount, BigDecimal interestRate, Instant start,
      Instant end) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_INTEREST_RATE, interestRate);
    store.updateIntervalRecord(bankAccount, BANK_ACCOUNT_INTEREST_RECORD_FACTORY, start, end,
        attributes);
  }

  @Override
  public BigDecimal getInterestRate(BankAccount bankAccount, Instant date) throws StorageException {
    BankAccountInterest interestRecord =
        store.getIntervalRecord(bankAccount, BANK_ACCOUNT_INTEREST_RECORD_FACTORY, date);
    return interestRecord.getInterestRate();
  }

  @Override
  public BigDecimal getBalance(BankAccount bankAccount, Instant date) throws StorageException {
    Iterable<AccountTransaction> accountTransactions =
        store.getInstantRecords(bankAccount, ACCOUNT_TRANSACTION_RECORD_FACTORY,
            DateUtil.BEGINNING_OF_TIME, date);
    BigDecimal balance = BigDecimal.ZERO;
    for (AccountTransaction accountTransaction : accountTransactions) {
      balance = balance.add(accountTransaction.getAmount());
    }
    return balance;
  }

  @Override
  public BankAccount getBankAccount(int id) throws StorageException {
    return store.getRecord(BANK_ACCOUNT_RECORD_FACTORY, id);
  }

  @Override
  public ExpenseAccount createExpenseAccount(AccountCode accountCode, String title,
      BankAccount defaultSourceAccount) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_TITLE, title);
    attributes.put(ATTRIBUTE_DEFAULT_SOURCE_ACCOUNT_ID, defaultSourceAccount.getId());
    return store.createRecord(EXPENSE_ACCOUNT_RECORD_FACTORY, attributes);
  }

  @Override
  public RevenueAccount createRevenueAccount(AccountCode accountCode, String title,
      BankAccount defaultDepositAccount) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_TITLE, title);
    attributes.put(ATTRIBUTE_DEFAULT_DEPOSIT_ACCOUNT_ID, defaultDepositAccount.getId());
    return store.createRecord(REVENUE_ACCOUNT_RECORD_FACTORY, attributes);
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

  private void recordTransaction(Account account, Instant date, BigDecimal amount, String comment,
      Integer sisterTransactionId) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_AMOUNT, amount);
    if (comment != null) {
      attributes.put(ATTRIBUTE_COMMENT, comment);
    }
    if (sisterTransactionId != null) {
      attributes.put(ATTRIBUTE_SISTER_TRANSACTION_ID, sisterTransactionId);
    }
    int recordId = store
        .insertInstantRecord(account, ACCOUNT_TRANSACTION_RECORD_FACTORY, date, attributes);
    if (account instanceof ExpenseAccount) {
      ExpenseAccount expenseAccount = (ExpenseAccount) account;
      BankAccount bankAccount = getBankAccount(expenseAccount.getDefaultSourceAccountId());
      recordTransaction(bankAccount, date, amount.negate(), comment, recordId);
    }
    if (account instanceof RevenueAccount) {
      RevenueAccount revenueAccount = (RevenueAccount) account;
      BankAccount bankAccount = getBankAccount(revenueAccount.getDefaultDepositAccountId());
      recordTransaction(bankAccount, date, amount, comment, recordId);
    }
  }
}
