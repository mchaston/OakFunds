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

import org.chaston.oakfunds.ledger.Account;
import org.chaston.oakfunds.storage.AttributeMethod;
import org.chaston.oakfunds.storage.IntervalRecord;
import org.chaston.oakfunds.storage.RecordType;

import java.math.BigDecimal;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface RecurringEvent<T extends RecurringEvent> extends IntervalRecord<T>, ModelBound, AccountChild {

  static final RecordType<RecurringEvent> TYPE =
      RecordType.builder("recurring_event", RecurringEvent.class)
          .containedBy(Account.TYPE)
          .build();

  String ATTRIBUTE_AMOUNT = "amount";

  @AttributeMethod(attribute = ATTRIBUTE_AMOUNT, required = true)
  BigDecimal getAmount();
}
