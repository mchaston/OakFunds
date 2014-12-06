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

import org.chaston.oakfunds.ledger.BankAccountType;
import org.chaston.oakfunds.storage.Report;
import org.chaston.oakfunds.storage.ReportDateGranularity;
import org.chaston.oakfunds.storage.StorageException;
import org.joda.time.Instant;

import java.math.BigDecimal;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface ModelManager {
  Model createNewModel(String title) throws StorageException;

  Model getBaseModel() throws StorageException;

  Model getModel(int modelId) throws StorageException;

  Iterable<Model> getModels() throws StorageException;

  Model updateModel(Model model, String title) throws StorageException;

  ModelExpenseAccount createModelExpenseAccount(String title,
      BankAccountType sourceBankAccountType) throws StorageException;

  ModelRevenueAccount createModelRevenueAccount(String title,
      BankAccountType depositBankAccountType) throws StorageException;

  MonthlyRecurringEvent setMonthlyRecurringEventDetails(Model model, ModelAccount account,
      Instant start, Instant end, BigDecimal amount) throws StorageException;

  AnnualRecurringEvent setAnnualRecurringEventDetails(Model model, ModelAccount account,
      Instant start, Instant end, int paymentMonth, BigDecimal amount) throws StorageException;

  ModelAccountTransaction createAdHocEvent(Model model, ModelAccount account, Instant date,
      int distributionTime, DistributionTimeUnit distributionTimeUnit, BigDecimal amount)
      throws StorageException;

  ModelAccountTransaction updateAdHocEvent(ModelAccountTransaction modelAccountTransaction,
      Instant date, int distributionTime, DistributionTimeUnit distributionTimeUnit,
      BigDecimal amount)
      throws StorageException;

  void deleteAdHocEvent(ModelAccountTransaction modelAccountTransaction)
      throws StorageException;

  Iterable<ModelAccountTransaction> getModelTransactions(Model model, ModelAccount account,
      Instant start, Instant end) throws StorageException;

  Iterable<ModelDistributionTransaction> getModelDistributionTransactions(Model model,
      ModelAccount account, Instant start, Instant end) throws StorageException;

  Report runDistributionReport(Model model, int startYear, int endYear,
      ReportDateGranularity reportDateGranularity) throws StorageException;
}
