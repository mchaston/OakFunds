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

import org.chaston.oakfunds.storage.AttributeMethod;
import org.chaston.oakfunds.storage.InstantRecord;
import org.chaston.oakfunds.storage.ParentIdMethod;
import org.chaston.oakfunds.storage.RecordTemporalType;
import org.chaston.oakfunds.storage.RecordType;

import java.math.BigDecimal;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface ModelAccountTransaction extends InstantRecord<ModelAccountTransaction> {

  public static final RecordType<ModelAccountTransaction> TYPE =
      new RecordType<>("model_account_transaction", ModelAccountTransaction.class,
          RecordTemporalType.INSTANT, true);

  @AttributeMethod(attribute = "model_id")
  int getModelId();

  @ParentIdMethod
  int getAccountId();

  @AttributeMethod(attribute = "amount")
  BigDecimal getAmount();

  @AttributeMethod(attribute = "distribution_time")
  int getDistributionTime();

  @AttributeMethod(attribute = "distribution_time_unit")
  DistributionTimeUnit getDistributionTimeUnit();

  @AttributeMethod(attribute = "derived")
  boolean isDerived();
}
