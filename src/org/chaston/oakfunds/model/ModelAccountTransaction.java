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

import org.chaston.oakfunds.storage.Attribute;
import org.chaston.oakfunds.storage.InstantRecord;
import org.chaston.oakfunds.storage.RecordTemporalType;
import org.chaston.oakfunds.storage.RecordType;
import org.joda.time.Instant;

import java.math.BigDecimal;

/**
 * TODO(mchaston): write JavaDocs
 */
public class ModelAccountTransaction extends InstantRecord {

  public static final RecordType<ModelAccountTransaction> TYPE =
      new RecordType<>("model_account_transaction", ModelAccountTransaction.class,
          RecordTemporalType.INSTANT, true);

  @Attribute(name = "model_id", propertyName = "modelId")
  private int modelId;

  @Attribute(name = "account_id", propertyName = "accountId")
  private int accountId;

  @Attribute(name = "amount")
  private BigDecimal amount;

  @Attribute(name = "distribution_time", propertyName = "distributionTime")
  private int distributionTime;

  @Attribute(name = "distribution_time_unit", propertyName = "distributionTimeUnit")
  private DistributionTimeUnit distributionTimeUnit;

  @Attribute(name = "derived")
  private boolean derived;

  ModelAccountTransaction(int id, Instant instant) {
    super(TYPE, id, instant);
  }

  public int getModelId() {
    return modelId;
  }

  public void setModelId(int modelId) {
    this.modelId = modelId;
  }

  public int getAccountId() {
    return accountId;
  }

  public void setAccountId(int accountId) {
    this.accountId = accountId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public int getDistributionTime() {
    return distributionTime;
  }

  public void setDistributionTime(int distributionTime) {
    this.distributionTime = distributionTime;
  }

  public DistributionTimeUnit getDistributionTimeUnit() {
    return distributionTimeUnit;
  }

  public void setDistributionTimeUnit(DistributionTimeUnit distributionTimeUnit) {
    this.distributionTimeUnit = distributionTimeUnit;
  }

  public boolean isDerived() {
    return derived;
  }

  public void setDerived(boolean derived) {
    this.derived = derived;
  }
}
