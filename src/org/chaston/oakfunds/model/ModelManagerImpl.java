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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.chaston.oakfunds.ledger.Account;
import org.chaston.oakfunds.ledger.AccountTransaction;
import org.chaston.oakfunds.ledger.BankAccount;
import org.chaston.oakfunds.ledger.ExpenseAccount;
import org.chaston.oakfunds.ledger.LedgerManager;
import org.chaston.oakfunds.ledger.RevenueAccount;
import org.chaston.oakfunds.security.ActionType;
import org.chaston.oakfunds.security.AuthenticationScope;
import org.chaston.oakfunds.security.AuthorizationContext;
import org.chaston.oakfunds.security.Permission;
import org.chaston.oakfunds.security.PermissionAssertion;
import org.chaston.oakfunds.security.SinglePermissionAssertion;
import org.chaston.oakfunds.security.SystemAuthenticationManager;
import org.chaston.oakfunds.storage.AttributeOrderingTerm;
import org.chaston.oakfunds.storage.AttributeSearchTerm;
import org.chaston.oakfunds.storage.IdentifierSearchTerm;
import org.chaston.oakfunds.storage.OrSearchTerm;
import org.chaston.oakfunds.storage.OrderingTerm;
import org.chaston.oakfunds.storage.Report;
import org.chaston.oakfunds.storage.ReportDateGranularity;
import org.chaston.oakfunds.storage.SearchOperator;
import org.chaston.oakfunds.storage.SearchTerm;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.storage.Transaction;
import org.chaston.oakfunds.system.SystemPropertiesManager;
import org.chaston.oakfunds.util.DateUtil;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DurationFieldType;
import org.joda.time.Instant;
import org.joda.time.MutableDateTime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
class ModelManagerImpl implements ModelManager {

  static final Permission PERMISSION_MODEL_READ = Permission.builder("model.read")
      .addRelatedAction(Model.TYPE, ActionType.READ)
      .build();
  static final Permission PERMISSION_MODEL_CREATE = Permission.builder("model.create")
      .addRelatedAction(Model.TYPE, ActionType.CREATE)
      .build();
  static final Permission PERMISSION_MODEL_UPDATE = Permission.builder("model.update")
      .addRelatedAction(Model.TYPE, ActionType.UPDATE)
      .build();
  static final Permission PERMISSION_MONTHLY_RECURRING_EVENT_UPDATE =
      Permission.builder("monthly_recurring_event.update")
          .addRelatedAction(RecurringEvent.TYPE, ActionType.READ)
          .addRelatedAction(MonthlyRecurringEvent.TYPE, ActionType.UPDATE)
          .addRelatedAction(ModelAccountTransaction.TYPE, ActionType.CREATE)
          .addRelatedAction(ModelAccountTransaction.TYPE, ActionType.UPDATE)
          .addRelatedAction(ModelAccountTransaction.TYPE, ActionType.DELETE)
          .build();
  static final Permission PERMISSION_ANNUAL_RECURRING_EVENT_UPDATE =
      Permission.builder("annual_recurring_event.update")
          .addRelatedAction(RecurringEvent.TYPE, ActionType.READ)
          .addRelatedAction(AnnualRecurringEvent.TYPE, ActionType.UPDATE)
          .addRelatedAction(ModelAccountTransaction.TYPE, ActionType.CREATE)
          .addRelatedAction(ModelAccountTransaction.TYPE, ActionType.UPDATE)
          .addRelatedAction(ModelAccountTransaction.TYPE, ActionType.DELETE)
          .addRelatedAction(ModelDistributionTransaction.TYPE, ActionType.CREATE)
          .addRelatedAction(ModelDistributionTransaction.TYPE, ActionType.UPDATE)
          .addRelatedAction(ModelDistributionTransaction.TYPE, ActionType.DELETE)
          .build();
  static final Permission PERMISSION_MODEL_ACCOUNT_TRANSACTION_READ =
      Permission.builder("model_account_transaction.read")
          .addRelatedAction(ModelAccountTransaction.TYPE, ActionType.READ)
          .build();
  static final Permission PERMISSION_MODEL_ACCOUNT_TRANSACTION_CREATE =
      Permission.builder("model_account_transaction.create")
          .addRelatedAction(ModelAccountTransaction.TYPE, ActionType.CREATE)
          .addRelatedAction(ModelDistributionTransaction.TYPE, ActionType.CREATE)
          .addRelatedAction(ModelDistributionTransaction.TYPE, ActionType.UPDATE)
          .addRelatedAction(ModelDistributionTransaction.TYPE, ActionType.DELETE)
          .build();
  static final Permission PERMISSION_MODEL_ACCOUNT_TRANSACTION_UPDATE =
      Permission.builder("model_account_transaction.update")
          .addRelatedAction(Model.TYPE, ActionType.READ)
          .addRelatedAction(Account.TYPE, ActionType.READ)
          .addRelatedAction(ModelAccountTransaction.TYPE, ActionType.UPDATE)
          .addRelatedAction(ModelDistributionTransaction.TYPE, ActionType.CREATE)
          .addRelatedAction(ModelDistributionTransaction.TYPE, ActionType.UPDATE)
          .addRelatedAction(ModelDistributionTransaction.TYPE, ActionType.DELETE)
          .build();
  static final Permission PERMISSION_MODEL_ACCOUNT_TRANSACTION_DELETE =
      Permission.builder("model_account_transaction.delete")
          .addRelatedAction(Account.TYPE, ActionType.READ)
          .addRelatedAction(ModelAccountTransaction.TYPE, ActionType.DELETE)
          .addRelatedAction(ModelDistributionTransaction.TYPE, ActionType.DELETE)
          .build();
  static final Permission PERMISSION_MODEL_ACCOUNT_TRANSACTION_REPORT =
      Permission.builder("model_account_transaction.report")
          .addRelatedAction(AccountTransaction.TYPE, ActionType.REPORT)
          .addRelatedAction(ModelAccountTransaction.TYPE, ActionType.REPORT)
          .build();
  static final Permission PERMISSION_MODEL_DISTRIBUTION_TRANSACTION_READ =
      Permission.builder("model_distribution_transaction.read")
          .addRelatedAction(ModelDistributionTransaction.TYPE, ActionType.READ)
          .build();
  static final Permission PERMISSION_MODEL_DISTRIBUTION_TRANSACTION_REPORT =
      Permission.builder("model_distribution_transaction.report")
          .addRelatedAction(ModelDistributionTransaction.TYPE, ActionType.REPORT)
          .build();

  private final SystemPropertiesManager systemPropertiesManager;
  private LedgerManager ledgerManager;
  private final Store store;
  private int baseModelId;

  @Inject
  ModelManagerImpl(
      SystemPropertiesManager systemPropertiesManager,
      LedgerManager ledgerManager,
      Store store,
      AuthorizationContext authorizationContext,
      SystemAuthenticationManager authenticationManager) throws StorageException {
    this.systemPropertiesManager = systemPropertiesManager;
    this.ledgerManager = ledgerManager;
    this.store = store;

    List<? extends SearchTerm> searchTerms =
        ImmutableList.of(AttributeSearchTerm.of(Model.ATTRIBUTE_BASE_MODEL,
            SearchOperator.EQUALS, true));
    Model baseModel;
    try (AuthenticationScope authenticationScope = authenticationManager.authenticateSystem()) {
      try (SinglePermissionAssertion singlePermissionAssertion =
               authorizationContext.assertPermission("model.create")) {
        Iterable<Model> baseModels = store.findRecords(Model.TYPE, searchTerms,
            ImmutableList.<OrderingTerm>of());
        if (Iterables.isEmpty(baseModels)) {
          Map<String, Object> attributes = new HashMap<>();
          attributes.put(Model.ATTRIBUTE_TITLE, "[base]");
          attributes.put(Model.ATTRIBUTE_BASE_MODEL, true);
          Transaction transaction = store.startTransaction();
          boolean success = false;
          try {
            baseModel = store.createRecord(Model.TYPE, attributes);
            success = true;
          } finally {
            if (success) {
              transaction.commit();
            } else {
              transaction.rollback();
            }
          }
        } else {
          baseModel = Iterables.getOnlyElement(baseModels);
        }
      }
    }
    baseModelId = baseModel.getId();
  }

  @Override
  @PermissionAssertion("model.create")
  public Model createNewModel(String title) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(Model.ATTRIBUTE_TITLE, title);
    attributes.put(Model.ATTRIBUTE_BASE_MODEL, false);
    return store.createRecord(Model.TYPE, attributes);
  }

  @Override
  @PermissionAssertion("model.read")
  public Model getBaseModel() throws StorageException {
    return getModel(baseModelId);
  }

  @Override
  @PermissionAssertion("model.read")
  public Model getModel(int modelId) throws StorageException {
    return store.getRecord(Model.TYPE, modelId);
  }

  @Override
  @PermissionAssertion("model.read")
  public Iterable<Model> getModels() throws StorageException {
    return store.findRecords(Model.TYPE, ImmutableList.<SearchTerm>of(),
        ImmutableList.of(AttributeOrderingTerm.of(Model.ATTRIBUTE_TITLE, OrderingTerm.Order.ASC)));
  }

  @Override
  @PermissionAssertion("model.update")
  public Model updateModel(Model model, String title) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(Model.ATTRIBUTE_TITLE, title);
    return store.updateRecord(model, attributes);
  }

  @Override
  @PermissionAssertion("monthly_recurring_event.update")
  public MonthlyRecurringEvent setMonthlyRecurringEventDetails(Model model, Account account,
      Instant start, Instant end, BigDecimal amount) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ModelBound.ATTRIBUTE_MODEL_ID, model.getId());
    attributes.put(RecurringEvent.ATTRIBUTE_AMOUNT, amount);
    MonthlyRecurringEvent monthlyRecurringEvent =
        store.updateIntervalRecord(account, MonthlyRecurringEvent.TYPE,
            start, end, attributes);
    recalculateAccountTransactions(model, account, start, end);
    return monthlyRecurringEvent;
  }

  @Override
  @PermissionAssertion("annual_recurring_event.update")
  public AnnualRecurringEvent setAnnualRecurringEventDetails(Model model, Account account,
      Instant start, Instant end, int paymentMonth, BigDecimal amount) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ModelBound.ATTRIBUTE_MODEL_ID, model.getId());
    attributes.put(AnnualRecurringEvent.ATTRIBUTE_PAYMENT_MONTH, paymentMonth);
    attributes.put(RecurringEvent.ATTRIBUTE_AMOUNT, amount);
    AnnualRecurringEvent annualRecurringEvent =
        store.updateIntervalRecord(account, AnnualRecurringEvent.TYPE,
            start, end, attributes);
    recalculateAccountTransactions(model, account, start, end);
    return annualRecurringEvent;
  }

  @Override
  @PermissionAssertion("model_account_transaction.create")
  public ModelAccountTransaction createAdHocEvent(Model model, Account account, Instant date,
      int distributionTime, DistributionTimeUnit distributionTimeUnit, BigDecimal amount)
      throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ModelBound.ATTRIBUTE_MODEL_ID, model.getId());
    attributes.put(ModelAccountTransaction.ATTRIBUTE_AMOUNT, amount);
    attributes.put(ModelAccountTransaction.ATTRIBUTE_DERIVED, false);
    attributes.put(ModelAccountTransaction.ATTRIBUTE_DISTRIBUTION_TIME, distributionTime);
    attributes.put(ModelAccountTransaction.ATTRIBUTE_DISTRIBUTION_TIME_UNIT, distributionTimeUnit);
    ModelAccountTransaction accountTransaction =
        store.insertInstantRecord(account, ModelAccountTransaction.TYPE, date, attributes);
    recalculateDistributionTransactions(model, account, accountTransaction);
    ModelAccountTransaction compensatingAccountTransaction =
        createCompensatingAccountTransaction(account, accountTransaction, attributes);
    if (compensatingAccountTransaction != null) {
      recalculateDistributionTransactions(model, account, compensatingAccountTransaction);
    }
    return accountTransaction;
  }

  @Override
  @PermissionAssertion("model_account_transaction.update")
  public ModelAccountTransaction updateAdHocEvent(ModelAccountTransaction modelAccountTransaction,
      Instant date, int distributionTime, DistributionTimeUnit distributionTimeUnit,
      BigDecimal amount) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ModelBound.ATTRIBUTE_MODEL_ID, modelAccountTransaction.getModelId());
    attributes.put(ModelAccountTransaction.ATTRIBUTE_AMOUNT, amount);
    attributes.put(ModelAccountTransaction.ATTRIBUTE_DERIVED, false);
    attributes.put(ModelAccountTransaction.ATTRIBUTE_DISTRIBUTION_TIME, distributionTime);
    attributes.put(ModelAccountTransaction.ATTRIBUTE_DISTRIBUTION_TIME_UNIT, distributionTimeUnit);
    Model model =
        store.getRecord(Model.TYPE, modelAccountTransaction.getModelId());
    Account account =
        store.getRecord(Account.TYPE, modelAccountTransaction.getAccountId());
    ModelAccountTransaction updatedModelAccountTransaction =
        store.updateInstantRecord(account,
            ModelAccountTransaction.TYPE, modelAccountTransaction.getId(), date, attributes);
    recalculateDistributionTransactions(model, account, updatedModelAccountTransaction);
    return updatedModelAccountTransaction;
  }

  @Override
  @PermissionAssertion("model_account_transaction.delete")
  public void deleteAdHocEvent(ModelAccountTransaction modelAccountTransaction)
      throws StorageException {
    Account account =
        store.getRecord(Account.TYPE, modelAccountTransaction.getAccountId());
    store.deleteInstantRecords(account,
        ModelAccountTransaction.TYPE, ImmutableList.of(
            IdentifierSearchTerm.of(modelAccountTransaction.getId())));
    deleteDistributionTransactions(account, modelAccountTransaction);
  }

  @Override
  @PermissionAssertion("model_account_transaction.read")
  public Iterable<ModelAccountTransaction> getModelTransactions(Model model, Account account,
      Instant start, Instant end) throws StorageException {
    List<? extends SearchTerm> searchTerms = ImmutableList.of(
        AttributeSearchTerm.of(ModelBound.ATTRIBUTE_MODEL_ID, SearchOperator.EQUALS, model.getId()));
    return store.findInstantRecords(account, ModelAccountTransaction.TYPE, start,
        end, searchTerms);
  }

  @Override
  @PermissionAssertion("model_distribution_transaction.read")
  public Iterable<ModelDistributionTransaction> getModelDistributionTransactions(Model model,
      Account account, Instant start, Instant end) throws StorageException {
    List<? extends SearchTerm> searchTerms = ImmutableList.of(
        AttributeSearchTerm.of(ModelBound.ATTRIBUTE_MODEL_ID, SearchOperator.EQUALS, model.getId()));
    return store.findInstantRecords(account, ModelDistributionTransaction.TYPE,
        start, end, searchTerms);
  }

  @Override
  @PermissionAssertion("model_account_transaction.report")
  public Report runTransactionReport(Model model, int startYear, int endYear,
      ReportDateGranularity reportDateGranularity) throws StorageException {
    ImmutableList<SearchTerm> modelBoundSearhTerms = ImmutableList.<SearchTerm>of(
        OrSearchTerm.of(
            AttributeSearchTerm.of(ModelBound.ATTRIBUTE_MODEL_ID,
                SearchOperator.EQUALS, baseModelId),
            AttributeSearchTerm.of(ModelBound.ATTRIBUTE_MODEL_ID,
                SearchOperator.EQUALS, model.getId())));

    return store.newReportBuilder(startYear, endYear,
        reportDateGranularity, DIMENSION_ACCOUNT_ID)
        .addRecordSource(AccountTransaction.TYPE,
            ImmutableList.<SearchTerm>of(),
            ImmutableMap.<String, String>of(),
            ImmutableMap.of(AccountTransaction.ATTRIBUTE_AMOUNT, MEASURE_AMOUNT))
        .addRecordSource(ModelAccountTransaction.TYPE,
            modelBoundSearhTerms,
            ImmutableMap.<String, String>of(),
            ImmutableMap.of(ModelAccountTransaction.ATTRIBUTE_AMOUNT, MEASURE_AMOUNT))
        .build();
  }

  @Override
  @PermissionAssertion("model_distribution_transaction.report")
  public Report runDistributionReport(Model model, int startYear, int endYear,
      ReportDateGranularity reportDateGranularity) throws StorageException {
    ImmutableList<SearchTerm> modelBoundSearhTerms = ImmutableList.<SearchTerm>of(
        OrSearchTerm.of(
            AttributeSearchTerm.of(ModelBound.ATTRIBUTE_MODEL_ID,
                SearchOperator.EQUALS, baseModelId),
            AttributeSearchTerm.of(ModelBound.ATTRIBUTE_MODEL_ID,
                SearchOperator.EQUALS, model.getId())));

    return store.newReportBuilder(startYear, endYear,
        reportDateGranularity, DIMENSION_ACCOUNT_ID)
        .addRecordSource(ModelDistributionTransaction.TYPE,
            modelBoundSearhTerms,
            ImmutableMap.<String, String>of(),
            ImmutableMap.of(ModelDistributionTransaction.ATTRIBUTE_AMOUNT, MEASURE_AMOUNT))
        .build();
  }

  private void recalculateAccountTransactions(Model model, Account account,
      Instant start, Instant end) throws StorageException {
    List<? extends SearchTerm> searchTerms = ImmutableList.of(
        AttributeSearchTerm.of(ModelBound.ATTRIBUTE_MODEL_ID, SearchOperator.EQUALS, model.getId()));
    Iterable<RecurringEvent> recurringEvents =
        store.findIntervalRecords(account, RecurringEvent.TYPE, start, end, searchTerms);
    for (RecurringEvent recurringEvent : recurringEvents) {
      if (recurringEvent instanceof MonthlyRecurringEvent) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ModelBound.ATTRIBUTE_MODEL_ID, model.getId());
        attributes.put(ModelAccountTransaction.ATTRIBUTE_AMOUNT, recurringEvent.getAmount());
        attributes.put(ModelAccountTransaction.ATTRIBUTE_DERIVED, true);
        for (Instant instant
            : getAllInstantsInRange(recurringEvent.getStart(), recurringEvent.getEnd())) {
          ModelAccountTransaction accountTransaction = store.insertInstantRecord(
              account, ModelAccountTransaction.TYPE, instant, attributes);
          createCompensatingAccountTransaction(account, accountTransaction, attributes);
        }
      }
      if (recurringEvent instanceof AnnualRecurringEvent) {
        AnnualRecurringEvent annualRecurringEvent = (AnnualRecurringEvent) recurringEvent;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ModelBound.ATTRIBUTE_MODEL_ID, model.getId());
        attributes.put(ModelAccountTransaction.ATTRIBUTE_AMOUNT, recurringEvent.getAmount());
        attributes.put(ModelAccountTransaction.ATTRIBUTE_DERIVED, true);
        attributes.put(ModelAccountTransaction.ATTRIBUTE_DISTRIBUTION_TIME, 1);
        attributes.put(ModelAccountTransaction.ATTRIBUTE_DISTRIBUTION_TIME_UNIT,
            DistributionTimeUnit.YEARS);

        for (Instant instant
            : getAllInstantsInRange(recurringEvent.getStart(), recurringEvent.getEnd())) {
          if (instant.get(DateTimeFieldType.monthOfYear()) == annualRecurringEvent.getPaymentMonth()) {
            ModelAccountTransaction accountTransaction =
                store.insertInstantRecord(account, ModelAccountTransaction.TYPE, instant,
                    attributes);
            recalculateDistributionTransactions(model, account, accountTransaction);
            ModelAccountTransaction compensatingAccountTransaction =
                createCompensatingAccountTransaction(account, accountTransaction, attributes);
            if (compensatingAccountTransaction != null) {
              recalculateDistributionTransactions(model, account, compensatingAccountTransaction);
            }
          }
        }
      }
    }
  }

  private ModelAccountTransaction createCompensatingAccountTransaction(Account account,
      ModelAccountTransaction accountTransaction, Map<String, Object> attributes) throws StorageException {
    Map<String, Object> alternateAttributes = new HashMap<>(attributes);
    alternateAttributes.put(
        ModelAccountTransaction.ATTRIBUTE_SISTER_TRANSACTION_ID, accountTransaction.getId());
    if (account instanceof ExpenseAccount) {
      ExpenseAccount revenueAccount = (ExpenseAccount) account;
      if (revenueAccount.getDefaultSourceAccountId() != null) {
        BankAccount bankAccount =
            ledgerManager.getBankAccount(revenueAccount.getDefaultSourceAccountId());
        BigDecimal amount = (BigDecimal) attributes.get(ModelAccountTransaction.ATTRIBUTE_AMOUNT);
        alternateAttributes.put(ModelAccountTransaction.ATTRIBUTE_AMOUNT, amount.negate());
        return store.insertInstantRecord(bankAccount,
            ModelAccountTransaction.TYPE, accountTransaction.getInstant(), alternateAttributes);
      }
    }
    if (account instanceof RevenueAccount) {
      RevenueAccount revenueAccount = (RevenueAccount) account;
      if (revenueAccount.getDefaultDepositAccountId() != null) {
        BankAccount bankAccount =
            ledgerManager.getBankAccount(revenueAccount.getDefaultDepositAccountId());
        return store.insertInstantRecord(bankAccount,
            ModelAccountTransaction.TYPE, accountTransaction.getInstant(), alternateAttributes);
      }
    }
    return null;
  }

  private void recalculateDistributionTransactions(Model model, Account account, ModelAccountTransaction modelAccountTransaction)
      throws StorageException {
    int distributionMonths = modelAccountTransaction.getDistributionTimeUnit() == DistributionTimeUnit.MONTHS
        ? modelAccountTransaction.getDistributionTime()
        : modelAccountTransaction.getDistributionTime() * 12;
    Instant end = modelAccountTransaction.getInstant();
    MutableDateTime mutableDateTime = modelAccountTransaction.getInstant().toMutableDateTime();
    mutableDateTime.add(DurationFieldType.months(), 1 - distributionMonths);
    Instant firstDistributionInstant = DateUtil.endOfYear(systemPropertiesManager.getCurrentYear() - 1);
    BigDecimal amountPerDistribution =
        modelAccountTransaction.getAmount().divide(
            BigDecimal.valueOf(distributionMonths), 5, RoundingMode.HALF_UP);
    BigDecimal firstDistributionAmount = BigDecimal.ZERO;

    // Delete previous distributions.
    store.deleteInstantRecords(account, ModelDistributionTransaction.TYPE,
        ImmutableList.of(AttributeSearchTerm.of(
            ModelDistributionTransaction.ATTRIBUTE_ACCOUNT_TRANSACTION_ID, SearchOperator.EQUALS,
            modelAccountTransaction.getId())));

    while (mutableDateTime.isBefore(end)) {
      if (mutableDateTime.isBefore(firstDistributionInstant)) {
        firstDistributionAmount = firstDistributionAmount.add(amountPerDistribution);
      } else {
        Map<String, Object> distributionAttributes = new HashMap<>();
        distributionAttributes.put(ModelBound.ATTRIBUTE_MODEL_ID, model.getId());
        distributionAttributes.put(
            ModelDistributionTransaction.ATTRIBUTE_ACCOUNT_TRANSACTION_ID,
            modelAccountTransaction.getId());
        distributionAttributes.put(ModelDistributionTransaction.ATTRIBUTE_AMOUNT,
            amountPerDistribution);
        store.insertInstantRecord(account, ModelDistributionTransaction.TYPE,
            mutableDateTime.toInstant(), distributionAttributes);
      }
      mutableDateTime.add(DurationFieldType.months(), 1);
    }
    // Add the first distribution amount.
    if (!firstDistributionAmount.equals(BigDecimal.ZERO)) {
      Map<String, Object> firstDistributionAttributes = new HashMap<>();
      firstDistributionAttributes.put(ModelBound.ATTRIBUTE_MODEL_ID, model.getId());
      firstDistributionAttributes.put(
          ModelDistributionTransaction.ATTRIBUTE_ACCOUNT_TRANSACTION_ID,
          modelAccountTransaction.getId());
      firstDistributionAttributes.put(ModelDistributionTransaction.ATTRIBUTE_AMOUNT,
          firstDistributionAmount);
      store.insertInstantRecord(account, ModelDistributionTransaction.TYPE,
          firstDistributionInstant, firstDistributionAttributes);
    }

    // Add the anti-distribution that cancels out the others when the transaction is executed.
    Map<String, Object> antiDistributionAttributes = new HashMap<>();
    antiDistributionAttributes.put(ModelBound.ATTRIBUTE_MODEL_ID, model.getId());
    antiDistributionAttributes.put(
        ModelDistributionTransaction.ATTRIBUTE_ACCOUNT_TRANSACTION_ID,
        modelAccountTransaction.getId());
    antiDistributionAttributes.put(ModelDistributionTransaction.ATTRIBUTE_AMOUNT,
        amountPerDistribution.negate().multiply(BigDecimal.valueOf(distributionMonths - 1)));
    store.insertInstantRecord(account, ModelDistributionTransaction.TYPE,
        mutableDateTime.toInstant(), antiDistributionAttributes);
  }

  private void deleteDistributionTransactions(Account account, ModelAccountTransaction modelAccountTransaction)
      throws StorageException {
    // Delete previous distributions.
    store.deleteInstantRecords(account, ModelDistributionTransaction.TYPE,
        ImmutableList.of(AttributeSearchTerm.of(
            ModelDistributionTransaction.ATTRIBUTE_ACCOUNT_TRANSACTION_ID, SearchOperator.EQUALS,
            modelAccountTransaction.getId())));
  }

  private Iterable<Instant> getAllInstantsInRange(Instant start, Instant end) {
    MutableDateTime mutableDateTime = new MutableDateTime(
        systemPropertiesManager.getCurrentYear(), 1, 1, 0, 0, 0, 0);
    long maxYear =
        systemPropertiesManager.getCurrentYear() + systemPropertiesManager.getTimeHorizon();
    if (mutableDateTime.isBefore(start)) {
      mutableDateTime = start.toMutableDateTime();
    }
    ImmutableList.Builder<Instant> instants = ImmutableList.builder();
    while (mutableDateTime.isBefore(end)
        && mutableDateTime.get(DateTimeFieldType.year()) <= maxYear) {
      instants.add(mutableDateTime.toInstant());
      mutableDateTime.add(DurationFieldType.months(), 1);
    }
    return instants.build();
  }
}
