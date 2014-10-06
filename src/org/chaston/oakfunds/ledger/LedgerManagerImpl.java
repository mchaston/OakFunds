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
import org.chaston.oakfunds.storage.FinalInstantRecordFactory;
import org.chaston.oakfunds.storage.FinalIntervalRecordFactory;
import org.chaston.oakfunds.storage.FinalRecordFactory;
import org.chaston.oakfunds.storage.RecordFactory;
import org.chaston.oakfunds.storage.RecordType;
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

  private static final String ATTRIBUTE_AMOUNT = "amount";
  private static final String ATTRIBUTE_BANK_ACCOUNT_TYPE = "bank_account_type";
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
    store.registerType(Account.TYPE,
        new RecordFactory<Account>() {
          @Override
          public Account newInstance(RecordType recordType, int id) {
            if (recordType == BankAccount.TYPE) {
              return new BankAccount(id);
            }
            if (recordType == ExpenseAccount.TYPE) {
              return new ExpenseAccount(id);
            }
            if (recordType == RevenueAccount.TYPE) {
              return new RevenueAccount(id);
            }
            throw new IllegalArgumentException(
                "RecordType " + recordType + " is not supported by the account record factory.");
          }
        });
    store.registerType(AccountTransaction.TYPE,
        new FinalInstantRecordFactory<AccountTransaction>(AccountTransaction.TYPE) {
          @Override
          protected AccountTransaction newInstance(int id, Instant instant) {
            return new AccountTransaction(id, instant);
          }
        });
    store.registerType(BankAccount.TYPE,
        new FinalRecordFactory<BankAccount>(BankAccount.TYPE) {
          @Override
          protected BankAccount newInstance(int id) {
            return new BankAccount(id);
          }
        });
    store.registerType(BankAccountInterest.TYPE,
        new FinalIntervalRecordFactory<BankAccountInterest>(BankAccountInterest.TYPE) {
          @Override
          protected BankAccountInterest newInstance(int id, Instant start, Instant end) {
            return new BankAccountInterest(id, start, end);
          }
        });
    store.registerType(ExpenseAccount.TYPE,
        new FinalRecordFactory<ExpenseAccount>(ExpenseAccount.TYPE) {
          @Override
          protected ExpenseAccount newInstance(int id) {
            return new ExpenseAccount(id);
          }
        });
    store.registerType(RevenueAccount.TYPE,
        new FinalRecordFactory<RevenueAccount>(RevenueAccount.TYPE) {
          @Override
          protected RevenueAccount newInstance(int id) {
            return new RevenueAccount(id);
          }
        });
  }

  @Override
  public BankAccount createBankAccount(AccountCode accountCode, String title,
      BankAccountType bankAccountType) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_TITLE, title);
    attributes.put(ATTRIBUTE_BANK_ACCOUNT_TYPE, bankAccountType);
    return store.createRecord(BankAccount.TYPE, attributes);
  }

  @Override
  public void setInterestRate(BankAccount bankAccount, BigDecimal interestRate, Instant start,
      Instant end) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_INTEREST_RATE, interestRate);
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
    attributes.put(ATTRIBUTE_TITLE, title);
    attributes.put(ATTRIBUTE_DEFAULT_SOURCE_ACCOUNT_ID, defaultSourceAccount.getId());
    return store.createRecord(ExpenseAccount.TYPE, attributes);
  }

  @Override
  public RevenueAccount createRevenueAccount(AccountCode accountCode, String title,
      BankAccount defaultDepositAccount) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_TITLE, title);
    attributes.put(ATTRIBUTE_DEFAULT_DEPOSIT_ACCOUNT_ID, defaultDepositAccount.getId());
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
