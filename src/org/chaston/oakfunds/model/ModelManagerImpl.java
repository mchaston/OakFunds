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
import org.chaston.oakfunds.storage.FinalInstantRecordFactory;
import org.chaston.oakfunds.storage.FinalIntervalRecordFactory;
import org.chaston.oakfunds.storage.FinalRecordFactory;
import org.chaston.oakfunds.storage.IntervalRecordFactory;
import org.chaston.oakfunds.storage.RecordFactory;
import org.chaston.oakfunds.storage.RecordType;
import org.chaston.oakfunds.storage.SearchOperator;
import org.chaston.oakfunds.storage.SearchTerm;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.storage.Transaction;
import org.chaston.oakfunds.system.SystemPropertiesManager;
import org.joda.time.Instant;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
class ModelManagerImpl implements ModelManager {

  private static final String ATTRIBUTE_BASE_MODEL = "base_model";
  private static final String ATTRIBUTE_TITLE = "title";
  private static final String ATTRIBUTE_MODEL_ID = "model_id";
  private static final String ATTRIBUTE_SOURCE_BANK_ACCOUNT_TYPE = "source_bank_account_type";
  private static final String ATTRIBUTE_DEPOSIT_BANK_ACCOUNT_TYPE = "deposit_bank_account_type";
  private static final String ATTRIBUTE_AMOUNT = "amount";
  private static final String ATTRIBUTE_PAYMENT_MONTH = "payment_month";
  private static final String ATTRIBUTE_DISTRIBUTION_TIME = "distribution_time";
  private static final String ATTRIBUTE_DISTRIBUTION_TIME_UNIT = "distribution_time_unit";
  private static final String ATTRIBUTE_DERIVED = "derived";

  private final SystemPropertiesManager systemPropertiesManager;
  private final Store store;
  private int baseModelId;

  @Inject
  ModelManagerImpl(SystemPropertiesManager systemPropertiesManager, Store store)
      throws StorageException {
    this.systemPropertiesManager = systemPropertiesManager;
    this.store = store;

    store.registerType(Model.TYPE,
        new FinalRecordFactory<Model>(Model.TYPE) {
          @Override
          protected Model newInstance(int id) {
            return new Model(id);
          }
        });

    store.registerType(RecurringEvent.TYPE,
        new IntervalRecordFactory<RecurringEvent>() {
          @Override
          public RecurringEvent newInstance(RecordType recordType, int id, Instant start,
              Instant end) {
            if (recordType == AnnualRecurringEvent.TYPE) {
              return new AnnualRecurringEvent(id, start, end);
            }
            if (recordType == MonthlyRecurringEvent.TYPE) {
              return new MonthlyRecurringEvent(id, start, end);
            }
            throw new IllegalArgumentException(
                "RecordType " + recordType
                    + " is not supported by the recurring_event record factory.");
          }
        });
    store.registerType(AnnualRecurringEvent.TYPE,
        new FinalIntervalRecordFactory<AnnualRecurringEvent>(AnnualRecurringEvent.TYPE) {
          @Override
          protected AnnualRecurringEvent newInstance(int id, Instant start, Instant end) {
            return new AnnualRecurringEvent(id, start, end);
          }
        });
    store.registerType(MonthlyRecurringEvent.TYPE,
        new FinalIntervalRecordFactory<MonthlyRecurringEvent>(MonthlyRecurringEvent.TYPE) {
          @Override
          protected MonthlyRecurringEvent newInstance(int id, Instant start, Instant end) {
            return new MonthlyRecurringEvent(id, start, end);
          }
        });

    store.registerType(ModelAccount.TYPE,
        new RecordFactory<ModelAccount>() {
          @Override
          public ModelAccount newInstance(RecordType recordType, int id) {
            if (recordType == ModelExpenseAccount.TYPE) {
              return new ModelExpenseAccount(id);
            }
            if (recordType == ModelRevenueAccount.TYPE) {
              return new ModelRevenueAccount(id);
            }
            throw new IllegalArgumentException(
                "RecordType " + recordType
                    + " is not supported by the model_account record factory.");
          }
        });
    store.registerType(ModelExpenseAccount.TYPE,
        new FinalRecordFactory<ModelExpenseAccount>(ModelExpenseAccount.TYPE) {
          @Override
          protected ModelExpenseAccount newInstance(int id) {
            return new ModelExpenseAccount(id);
          }
        });
    store.registerType(ModelRevenueAccount.TYPE,
        new FinalRecordFactory<ModelRevenueAccount>(ModelRevenueAccount.TYPE) {
          @Override
          protected ModelRevenueAccount newInstance(int id) {
            return new ModelRevenueAccount(id);
          }
        });

    store.registerType(ModelAccountTransaction.TYPE,
        new FinalInstantRecordFactory<ModelAccountTransaction>(ModelAccountTransaction.TYPE) {
          @Override
          protected ModelAccountTransaction newInstance(int id, Instant instant) {
            return new ModelAccountTransaction(id, instant);
          }
        });
    store.registerType(ModelDistributionTransaction.TYPE,
        new FinalInstantRecordFactory<ModelDistributionTransaction>(ModelDistributionTransaction.TYPE) {
          @Override
          protected ModelDistributionTransaction newInstance(int id, Instant instant) {
            return new ModelDistributionTransaction(id, instant);
          }
        });

    List<SearchTerm> searchTerms =
        ImmutableList.of(SearchTerm.of(ATTRIBUTE_BASE_MODEL, SearchOperator.EQUALS, true));
    Iterable<Model> baseModels = store.findRecords(Model.TYPE, searchTerms);
    Model baseModel;
    if (Iterables.isEmpty(baseModels)) {
      Map<String, Object> attributes = new HashMap<>();
      attributes.put(ATTRIBUTE_TITLE, "[base]");
      attributes.put(ATTRIBUTE_BASE_MODEL, true);
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
    attributes.put(ATTRIBUTE_TITLE, title);
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
    attributes.put(ATTRIBUTE_TITLE, title);
    attributes.put(ATTRIBUTE_SOURCE_BANK_ACCOUNT_TYPE, sourceBankAccountType);
    return store.createRecord(ModelExpenseAccount.TYPE, attributes);
  }

  @Override
  public ModelRevenueAccount createModelRevenueAccount(String title,
      BankAccountType depositBankAccountType) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_TITLE, title);
    attributes.put(ATTRIBUTE_DEPOSIT_BANK_ACCOUNT_TYPE, depositBankAccountType);
    return store.createRecord(ModelRevenueAccount.TYPE, attributes);
  }

  @Override
  public MonthlyRecurringEvent setMonthlyRecurringEventDetails(Model model, ModelAccount account,
      Instant start, Instant end, BigDecimal amount) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_MODEL_ID, model.getId());
    attributes.put(ATTRIBUTE_AMOUNT, amount);
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
    attributes.put(ATTRIBUTE_MODEL_ID, model.getId());
    attributes.put(ATTRIBUTE_PAYMENT_MONTH, paymentMonth);
    attributes.put(ATTRIBUTE_AMOUNT, amount);
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
    attributes.put(ATTRIBUTE_MODEL_ID, model.getId());
    attributes.put(ATTRIBUTE_AMOUNT, amount);
    attributes.put(ATTRIBUTE_DERIVED, false);
    attributes.put(ATTRIBUTE_DISTRIBUTION_TIME, distributionTime);
    attributes.put(ATTRIBUTE_DISTRIBUTION_TIME_UNIT, distributionTimeUnit);
    ModelAccountTransaction modelAccountTransaction =
        store.insertInstantRecord(account, ModelAccountTransaction.TYPE, date, attributes);
    recalculateDistributionTransactions(modelAccountTransaction);
    return modelAccountTransaction;
  }

  @Override
  public Iterable<ModelAccountTransaction> getModelTransactions(Model model, ModelAccount account,
      Instant start, Instant end) throws StorageException {
    List<SearchTerm> searchTerms = ImmutableList.of(
        SearchTerm.of(ATTRIBUTE_MODEL_ID, SearchOperator.EQUALS, model.getId()));
    return store.findInstantRecords(account, ModelAccountTransaction.TYPE, start,
        end, searchTerms);
  }

  @Override
  public Iterable<ModelDistributionTransaction> getModelDistributionTransactions(Model model,
      ModelAccount account, Instant start, Instant end) throws StorageException {
    List<SearchTerm> searchTerms = ImmutableList.of(
        SearchTerm.of(ATTRIBUTE_MODEL_ID, SearchOperator.EQUALS, model.getId()));
    return store.findInstantRecords(account, ModelDistributionTransaction.TYPE,
        start, end, searchTerms);
  }

  private void recalculateAccountTransactions(Model model, ModelAccount account,
      Instant start, Instant end) throws StorageException {
    List<SearchTerm> searchTerms = ImmutableList.of(
        SearchTerm.of(ATTRIBUTE_MODEL_ID, SearchOperator.EQUALS, model.getId()));
    Iterable<RecurringEvent> recurringEvents =
        store.findIntervalRecords(account, RecurringEvent.TYPE, start, end, searchTerms);
  }

  private void recalculateDistributionTransactions(ModelAccountTransaction modelAccountTransaction)
      throws StorageException {

  }
}
