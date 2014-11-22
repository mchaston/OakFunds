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
import org.chaston.oakfunds.security.ActionType;
import org.chaston.oakfunds.security.Permission;
import org.chaston.oakfunds.security.PermissionAssertion;
import org.chaston.oakfunds.storage.AttributeOrderingTerm;
import org.chaston.oakfunds.storage.ContainerIdentifierSearchTerm;
import org.chaston.oakfunds.storage.OrderingTerm;
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

  static final Permission PERMISSION_ACCOUNT_READ =
      Permission.builder("account.read")
          .addRelatedAction(Account.TYPE, ActionType.READ)
          .addRelatedAction(BankAccount.TYPE, ActionType.READ)
          .addRelatedAction(RevenueAccount.TYPE, ActionType.READ)
          .addRelatedAction(ExpenseAccount.TYPE, ActionType.READ)
          .build();

  static final Permission PERMISSION_BANK_ACCOUNT_READ =
      Permission.builder("bank_account.read")
          .addRelatedAction(BankAccount.TYPE, ActionType.READ).build();
  static final Permission PERMISSION_BANK_ACCOUNT_CREATE =
      Permission.builder("bank_account.create")
          .addRelatedAction(BankAccount.TYPE, ActionType.CREATE).build();
  static final Permission PERMISSION_BANK_ACCOUNT_UPDATE =
      Permission.builder("bank_account.update")
          .addRelatedAction(BankAccount.TYPE, ActionType.UPDATE).build();

  static final Permission PERMISSION_EXPENSE_ACCOUNT_READ =
      Permission.builder("expense_account.read")
          .addRelatedAction(ExpenseAccount.TYPE, ActionType.READ).build();
  static final Permission PERMISSION_EXPENSE_ACCOUNT_CREATE =
      Permission.builder("expense_account.create")
          .addRelatedAction(ExpenseAccount.TYPE, ActionType.CREATE).build();
  static final Permission PERMISSION_EXPENSE_ACCOUNT_UPDATE =
      Permission.builder("expense_account.update")
          .addRelatedAction(ExpenseAccount.TYPE, ActionType.UPDATE).build();

  static final Permission PERMISSION_REVENUE_ACCOUNT_READ =
      Permission.builder("revenue_account.read")
          .addRelatedAction(RevenueAccount.TYPE, ActionType.READ).build();
  static final Permission PERMISSION_REVENUE_ACCOUNT_CREATE =
      Permission.builder("revenue_account.create")
          .addRelatedAction(RevenueAccount.TYPE, ActionType.CREATE).build();
  static final Permission PERMISSION_REVENUE_ACCOUNT_UPDATE =
      Permission.builder("revenue_account.update")
          .addRelatedAction(RevenueAccount.TYPE, ActionType.UPDATE).build();

  static final Permission PERMISSION_BANK_ACCOUNT_INTEREST_READ =
      Permission.builder("bank_account_interest.read")
          .addRelatedAction(BankAccountInterest.TYPE, ActionType.READ).build();
  static final Permission PERMISSION_BANK_ACCOUNT_INTEREST_UPDATE =
      Permission.builder("bank_account_interest.update")
          .addRelatedAction(BankAccountInterest.TYPE, ActionType.UPDATE).build();

  static final Permission PERMISSION_ACCOUNT_TRANSACTION_READ =
      Permission.builder("account_transaction.read")
          .addRelatedAction(AccountTransaction.TYPE, ActionType.READ).build();
  static final Permission PERMISSION_ACCOUNT_TRANSACTION_CREATE =
      Permission.builder("account_transaction.create")
          .addRelatedAction(AccountTransaction.TYPE, ActionType.CREATE).build();
  static final Permission PERMISSION_ACCOUNT_TRANSACTION_REPORT =
      Permission.builder("account_transaction.report")
          .addRelatedAction(AccountTransaction.TYPE, ActionType.REPORT).build();

  private final Store store;

  @Inject
  LedgerManagerImpl(Store store) {
    this.store = store;
  }

  @Override
  @PermissionAssertion("bank_account.create")
  public BankAccount createBankAccount(AccountCode accountCode, String title,
      BankAccountType bankAccountType) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(Account.ATTRIBUTE_TITLE, title);
    attributes.put(Account.ATTRIBUTE_ACCOUNT_CODE_ID, accountCode.getId());
    attributes.put(BankAccount.ATTRIBUTE_BANK_ACCOUNT_TYPE, bankAccountType);
    return store.createRecord(BankAccount.TYPE, attributes);
  }

  @Override
  @PermissionAssertion("bank_account.update")
  public BankAccount updateBankAccount(BankAccount bankAccount, AccountCode accountCode,
      String title, BankAccountType bankAccountType) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(Account.ATTRIBUTE_TITLE, title);
    attributes.put(Account.ATTRIBUTE_ACCOUNT_CODE_ID, accountCode.getId());
    attributes.put(BankAccount.ATTRIBUTE_BANK_ACCOUNT_TYPE, bankAccountType);
    return store.updateRecord(bankAccount, attributes);
  }

  @Override
  @PermissionAssertion("bank_account_interest.update")
  public void setInterestRate(BankAccount bankAccount, BigDecimal interestRate, Instant start,
      Instant end) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(BankAccountInterest.ATTRIBUTE_INTEREST_RATE, interestRate);
    store.updateIntervalRecord(bankAccount, BankAccountInterest.TYPE, start, end,
        attributes);
  }

  @Override
  @PermissionAssertion("bank_account_interest.read")
  public BigDecimal getInterestRate(BankAccount bankAccount, Instant date) throws StorageException {
    BankAccountInterest interestRecord =
        store.getIntervalRecord(bankAccount, BankAccountInterest.TYPE, date);
    return interestRecord.getInterestRate();
  }

  @Override
  @PermissionAssertion("account_transaction.read")
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
  @PermissionAssertion("bank_account.read")
  public BankAccount getBankAccount(int id) throws StorageException {
    return store.getRecord(BankAccount.TYPE, id);
  }

  @Override
  @PermissionAssertion("expense_account.read")
  public ExpenseAccount getExpenseAccount(int id) throws StorageException {
    return store.getRecord(ExpenseAccount.TYPE, id);
  }

  @Override
  @PermissionAssertion("expense_account.create")
  public ExpenseAccount createExpenseAccount(AccountCode accountCode, String title,
      BankAccount defaultSourceAccount) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(Account.ATTRIBUTE_TITLE, title);
    attributes.put(Account.ATTRIBUTE_ACCOUNT_CODE_ID, accountCode.getId());
    Integer defaultSourceAccountId = null;
    if (defaultSourceAccount != null) {
      defaultSourceAccountId = defaultSourceAccount.getId();
    }
    attributes.put(ExpenseAccount.ATTRIBUTE_DEFAULT_SOURCE_ACCOUNT_ID, defaultSourceAccountId);
    return store.createRecord(ExpenseAccount.TYPE, attributes);
  }

  @Override
  @PermissionAssertion("expense_account.update")
  public ExpenseAccount updateExpenseAccount(ExpenseAccount expenseAccount, AccountCode accountCode,
      String title, BankAccount defaultSourceAccount) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(Account.ATTRIBUTE_TITLE, title);
    attributes.put(Account.ATTRIBUTE_ACCOUNT_CODE_ID, accountCode.getId());
    Integer defaultSourceAccountId = null;
    if (defaultSourceAccount != null) {
      defaultSourceAccountId = defaultSourceAccount.getId();
    }
    attributes.put(ExpenseAccount.ATTRIBUTE_DEFAULT_SOURCE_ACCOUNT_ID, defaultSourceAccountId);
    return store.updateRecord(expenseAccount, attributes);
  }

  @Override
  @PermissionAssertion("revenue_account.read")
  public RevenueAccount getRevenueAccount(int id) throws StorageException {
    return store.getRecord(RevenueAccount.TYPE, id);
  }

  @Override
  @PermissionAssertion("revenue_account.create")
  public RevenueAccount createRevenueAccount(AccountCode accountCode, String title,
      BankAccount defaultDepositAccount) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(Account.ATTRIBUTE_TITLE, title);
    attributes.put(Account.ATTRIBUTE_ACCOUNT_CODE_ID, accountCode.getId());
    Integer defaultDepositAccountId = null;
    if (defaultDepositAccount != null) {
      defaultDepositAccountId = defaultDepositAccount.getId();
    }
    attributes.put(RevenueAccount.ATTRIBUTE_DEFAULT_DEPOSIT_ACCOUNT_ID, defaultDepositAccountId);
    return store.createRecord(RevenueAccount.TYPE, attributes);
  }

  @Override
  @PermissionAssertion("revenue_account.update")
  public RevenueAccount updateRevenueAccount(RevenueAccount expenseAccount, AccountCode accountCode,
      String title, BankAccount defaultDepositAccount) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(Account.ATTRIBUTE_TITLE, title);
    attributes.put(Account.ATTRIBUTE_ACCOUNT_CODE_ID, accountCode.getId());
    Integer defaultDepositAccountId = null;
    if (defaultDepositAccount != null) {
      defaultDepositAccountId = defaultDepositAccount.getId();
    }
    attributes.put(RevenueAccount.ATTRIBUTE_DEFAULT_DEPOSIT_ACCOUNT_ID, defaultDepositAccountId);
    return store.updateRecord(expenseAccount, attributes);
  }

  @Override
  @PermissionAssertion("account.read")
  public Iterable<Account> getAccounts() throws StorageException {
    return store.findRecords(Account.TYPE, ImmutableList.<SearchTerm>of(),
        ImmutableList.of(
            AttributeOrderingTerm.of(Account.ATTRIBUTE_TITLE, OrderingTerm.Order.ASC)));
  }

  @Override
  @PermissionAssertion("bank_account.read")
  public Iterable<BankAccount> getBankAccounts() throws StorageException {
    return store.findRecords(BankAccount.TYPE, ImmutableList.<SearchTerm>of(),
        ImmutableList.of(
            AttributeOrderingTerm.of(Account.ATTRIBUTE_TITLE, OrderingTerm.Order.ASC)));
  }

  @Override
  @PermissionAssertion("account_transaction.create")
  public void recordTransaction(Account account, Instant date, BigDecimal amount)
      throws StorageException {
    recordTransaction(account, date, amount, null);
  }

  @Override
  @PermissionAssertion("account_transaction.create")
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
  @PermissionAssertion("account_transaction.report")
  public Report runReport(Account<?> account, int startYear, int endYear,
      ReportDateGranularity granularity) throws StorageException {
    ImmutableList<? extends SearchTerm> searchTerms =
        ImmutableList.of(ContainerIdentifierSearchTerm.of(
            account.getRecordType(), account.getId()));
    ImmutableList<String> dimensions = ImmutableList.of();
    ImmutableList<String> measures = ImmutableList.of(AccountTransaction.ATTRIBUTE_AMOUNT);
    return store.runReport(AccountTransaction.TYPE, startYear, endYear, granularity,
        searchTerms, "account_id", dimensions, measures);
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
