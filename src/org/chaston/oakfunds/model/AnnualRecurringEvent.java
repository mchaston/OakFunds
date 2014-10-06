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
import org.chaston.oakfunds.storage.RecordType;
import org.joda.time.Instant;

/**
 * TODO(mchaston): write JavaDocs
 */
public class AnnualRecurringEvent extends RecurringEvent<AnnualRecurringEvent> {

  static final RecordType<AnnualRecurringEvent> TYPE =
      new RecordType<>("annual_recurring_event",
          AnnualRecurringEvent.class, RecurringEvent.TYPE, true);

  @Attribute(name = "payment_month", propertyName = "paymentMonth")
  private int paymentMonth;

  AnnualRecurringEvent(int id, Instant start, Instant end) {
    super(TYPE, id, start, end);
  }

  public int getPaymentMonth() {
    return paymentMonth;
  }

  public void setPaymentMonth(int paymentMonth) {
    this.paymentMonth = paymentMonth;
  }
}
