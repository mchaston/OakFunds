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
import org.chaston.oakfunds.storage.IntervalRecord;
import org.chaston.oakfunds.storage.RecordTemporalType;
import org.chaston.oakfunds.storage.RecordType;
import org.joda.time.Instant;

import java.math.BigDecimal;

/**
 * TODO(mchaston): write JavaDocs
 */
public abstract class RecurringEvent<T extends RecurringEvent<T>> extends IntervalRecord<T> {

  static final RecordType<RecurringEvent> TYPE =
      new RecordType<>("record_type", RecurringEvent.class,
          RecordTemporalType.INTERVAL, false);

  @Attribute(name = "model_id", propertyName = "modelId")
  private int modelId;

  @Attribute(name = "account_id", propertyName = "accountId")
  private int accountId;

  @Attribute(name = "amount")
  private BigDecimal amount;

  RecurringEvent(RecordType<T> recordType, int id, Instant start, Instant end) {
    super(recordType, id, start, end);
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
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
}
