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
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.chaston.oakfunds.ledger.BankAccountType;
import org.chaston.oakfunds.storage.AttributeSearchTerm;
import org.chaston.oakfunds.storage.IdentifierSearchTerm;
import org.chaston.oakfunds.storage.SearchOperator;
import org.chaston.oakfunds.storage.SearchTerm;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.storage.Transaction;
import org.chaston.oakfunds.system.SystemPropertiesManager;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DurationFieldType;
import org.joda.time.Instant;
import org.joda.time.MutableDateTime;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
class ModelManagerImpl implements ModelManager {

  private final SystemPropertiesManager systemPropertiesManager;
  private final Store store;
  private int baseModelId;

  @Inject
  ModelManagerImpl(SystemPropertiesManager systemPropertiesManager, Store store)
      throws StorageException {
    this.systemPropertiesManager = systemPropertiesManager;
    this.store = store;

    store.registerType(Model.TYPE);

    store.registerType(RecurringEvent.TYPE);
    store.registerType(AnnualRecurringEvent.TYPE);
    store.registerType(MonthlyRecurringEvent.TYPE);

    store.registerType(ModelAccount.TYPE);
    store.registerType(ModelExpenseAccount.TYPE);
    store.registerType(ModelRevenueAccount.TYPE);

    store.registerType(ModelAccountTransaction.TYPE);
    store.registerType(ModelDistributionTransaction.TYPE);

    List<? extends SearchTerm> searchTerms =
        ImmutableList.of(AttributeSearchTerm.of(Model.ATTRIBUTE_BASE_MODEL, SearchOperator.EQUALS, true));
    Iterable<Model> baseModels = store.findRecords(Model.TYPE, searchTerms);
    Model baseModel;
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
    baseModelId = baseModel.getId();
  }

  @Override
  public Model createNewModel(String title) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(Model.ATTRIBUTE_TITLE, title);
    attributes.put(Model.ATTRIBUTE_BASE_MODEL, false);
    return store.createRecord(Model.TYPE, attributes);
  }

  @Override
  public Model getBaseModel() throws StorageException {
    return getModel(baseModelId);
  }

  @Override
  public Model getModel(int modelId) throws StorageException {
    return store.getRecord(Model.TYPE, modelId);
  }

  @Override
  public ModelExpenseAccount createModelExpenseAccount(String title,
      BankAccountType sourceBankAccountType) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ModelAccount.ATTRIBUTE_TITLE, title);
    attributes.put(ModelExpenseAccount.ATTRIBUTE_SOURCE_BANK_ACCOUNT_TYPE, sourceBankAccountType);
    return store.createRecord(ModelExpenseAccount.TYPE, attributes);
  }

  @Override
  public ModelRevenueAccount createModelRevenueAccount(String title,
      BankAccountType depositBankAccountType) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ModelAccount.ATTRIBUTE_TITLE, title);
    attributes.put(ModelRevenueAccount.ATTRIBUTE_DEPOSIT_BANK_ACCOUNT_TYPE, depositBankAccountType);
    return store.createRecord(ModelRevenueAccount.TYPE, attributes);
  }

  @Override
  public MonthlyRecurringEvent setMonthlyRecurringEventDetails(Model model, ModelAccount account,
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
  public AnnualRecurringEvent setAnnualRecurringEventDetails(Model model, ModelAccount account,
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
  public ModelAccountTransaction createAdHocEvent(Model model, ModelAccount account, Instant date,
      int distributionTime, DistributionTimeUnit distributionTimeUnit, BigDecimal amount)
      throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ModelBound.ATTRIBUTE_MODEL_ID, model.getId());
    attributes.put(ModelAccountTransaction.ATTRIBUTE_AMOUNT, amount);
    attributes.put(ModelAccountTransaction.ATTRIBUTE_DERIVED, false);
    attributes.put(ModelAccountTransaction.ATTRIBUTE_DISTRIBUTION_TIME, distributionTime);
    attributes.put(ModelAccountTransaction.ATTRIBUTE_DISTRIBUTION_TIME_UNIT, distributionTimeUnit);
    ModelAccountTransaction modelAccountTransaction =
        store.insertInstantRecord(account, ModelAccountTransaction.TYPE, date, attributes);
    recalculateDistributionTransactions(model, account, modelAccountTransaction);
    return modelAccountTransaction;
  }

  @Override
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
    ModelAccount account =
        store.getRecord(ModelAccount.TYPE, modelAccountTransaction.getAccountId());
    ModelAccountTransaction updatedModelAccountTransaction =
        store.updateInstantRecord(account,
            ModelAccountTransaction.TYPE, modelAccountTransaction.getId(), date, attributes);
    recalculateDistributionTransactions(model, account, updatedModelAccountTransaction);
    return updatedModelAccountTransaction;
  }

  @Override
  public void deleteAdHocEvent(ModelAccountTransaction modelAccountTransaction)
      throws StorageException {
    ModelAccount account =
        store.getRecord(ModelAccount.TYPE, modelAccountTransaction.getAccountId());
    store.deleteInstantRecords(account,
        ModelAccountTransaction.TYPE, ImmutableList.of(
            IdentifierSearchTerm.of(modelAccountTransaction.getId())));
    deleteDistributionTransactions(account, modelAccountTransaction);
  }

  @Override
  public Iterable<ModelAccountTransaction> getModelTransactions(Model model, ModelAccount account,
      Instant start, Instant end) throws StorageException {
    List<? extends SearchTerm> searchTerms = ImmutableList.of(
        AttributeSearchTerm.of(ModelBound.ATTRIBUTE_MODEL_ID, SearchOperator.EQUALS, model.getId()));
    return store.findInstantRecords(account, ModelAccountTransaction.TYPE, start,
        end, searchTerms);
  }

  @Override
  public Iterable<ModelDistributionTransaction> getModelDistributionTransactions(Model model,
      ModelAccount account, Instant start, Instant end) throws StorageException {
    List<? extends SearchTerm> searchTerms = ImmutableList.of(
        AttributeSearchTerm.of(ModelBound.ATTRIBUTE_MODEL_ID, SearchOperator.EQUALS, model.getId()));
    return store.findInstantRecords(account, ModelDistributionTransaction.TYPE,
        start, end, searchTerms);
  }

  private void recalculateAccountTransactions(Model model, ModelAccount account,
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
          store.insertInstantRecord(account, ModelAccountTransaction.TYPE, instant, attributes);
        }
      }
      if (recurringEvent instanceof AnnualRecurringEvent) {
        AnnualRecurringEvent annualRecurringEvent = (AnnualRecurringEvent) recurringEvent;
        Map<String, Object> accountAttributes = new HashMap<>();
        accountAttributes.put(ModelBound.ATTRIBUTE_MODEL_ID, model.getId());
        accountAttributes.put(ModelAccountTransaction.ATTRIBUTE_AMOUNT, recurringEvent.getAmount());
        accountAttributes.put(ModelAccountTransaction.ATTRIBUTE_DERIVED, true);
        accountAttributes.put(ModelAccountTransaction.ATTRIBUTE_DISTRIBUTION_TIME, 1);
        accountAttributes.put(ModelAccountTransaction.ATTRIBUTE_DISTRIBUTION_TIME_UNIT, DistributionTimeUnit.YEARS);

        for (Instant instant
            : getAllInstantsInRange(recurringEvent.getStart(), recurringEvent.getEnd())) {
          if (instant.get(DateTimeFieldType.monthOfYear()) == annualRecurringEvent.getPaymentMonth()) {
            ModelAccountTransaction accountTransaction =
                store.insertInstantRecord(account, ModelAccountTransaction.TYPE, instant,
                    accountAttributes);
            recalculateDistributionTransactions(model, account, accountTransaction);
          }
        }
      }
    }
  }

  private void recalculateDistributionTransactions(Model model, ModelAccount account, ModelAccountTransaction modelAccountTransaction)
      throws StorageException {
    int distributionMonths = modelAccountTransaction.getDistributionTimeUnit() == DistributionTimeUnit.MONTHS
        ? modelAccountTransaction.getDistributionTime()
        : modelAccountTransaction.getDistributionTime() * 12;
    Instant end = modelAccountTransaction.getInstant();
    MutableDateTime mutableDateTime = modelAccountTransaction.getInstant().toMutableDateTime();
    mutableDateTime.add(DurationFieldType.months(), 1 - distributionMonths);
    Instant firstDistribution =
        new MutableDateTime(systemPropertiesManager.getCurrentYear(), 1, 1, 0, 0, 0, 0).toInstant();
    BigDecimal amountPerDistribution =
        modelAccountTransaction.getAmount().divide(BigDecimal.valueOf(distributionMonths));
    BigDecimal firstDistributionAmount = BigDecimal.ZERO;

    // Delete previous distributions.
    store.deleteInstantRecords(account, ModelDistributionTransaction.TYPE,
        ImmutableList.of(AttributeSearchTerm.of(
            ModelDistributionTransaction.ATTRIBUTE_ACCOUNT_TRANSACTION_ID, SearchOperator.EQUALS,
            modelAccountTransaction.getId())));

    while (mutableDateTime.isBefore(end)) {
      if (mutableDateTime.isBefore(firstDistribution)) {
        firstDistributionAmount = firstDistributionAmount.add(amountPerDistribution);
      } else {
        Map<String, Object> distributionAttributes = new HashMap<>();
        distributionAttributes.put(ModelBound.ATTRIBUTE_MODEL_ID, model.getId());
        distributionAttributes.put(ModelDistributionTransaction.ATTRIBUTE_ACCOUNT_TRANSACTION_ID, modelAccountTransaction.getId());
        if (mutableDateTime.isEqual(firstDistribution)) {
          distributionAttributes.put(ModelDistributionTransaction.ATTRIBUTE_AMOUNT, firstDistributionAmount.add(
              amountPerDistribution));
        } else {
          distributionAttributes.put(ModelDistributionTransaction.ATTRIBUTE_AMOUNT, amountPerDistribution);
        }
        store.insertInstantRecord(account, ModelDistributionTransaction.TYPE,
            mutableDateTime.toInstant(), distributionAttributes);
      }
      mutableDateTime.add(DurationFieldType.months(), 1);
    }
    // Add the anti-distribution that cancels out the others when the transaction is executed.
    Map<String, Object> distributionAttributes = new HashMap<>();
    distributionAttributes.put(ModelBound.ATTRIBUTE_MODEL_ID, model.getId());
    distributionAttributes.put(ModelDistributionTransaction.ATTRIBUTE_ACCOUNT_TRANSACTION_ID, modelAccountTransaction.getId());
    distributionAttributes.put(ModelDistributionTransaction.ATTRIBUTE_AMOUNT,
        amountPerDistribution.negate().multiply(BigDecimal.valueOf(distributionMonths - 1)));
    store.insertInstantRecord(account, ModelDistributionTransaction.TYPE,
        mutableDateTime.toInstant(), distributionAttributes);
  }

  private void deleteDistributionTransactions(ModelAccount account, ModelAccountTransaction modelAccountTransaction)
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
