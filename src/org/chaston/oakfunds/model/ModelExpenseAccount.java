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
import org.chaston.oakfunds.storage.AttributeMethod;
import org.chaston.oakfunds.storage.RecordType;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface ModelExpenseAccount extends ModelAccount<ModelExpenseAccount> {

  static final RecordType<ModelExpenseAccount> TYPE =
      RecordType.builder("model_expense_account", ModelExpenseAccount.class)
          .extensionOf(ModelAccount.TYPE)
          .build();

  String ATTRIBUTE_SOURCE_BANK_ACCOUNT_TYPE = "source_bank_account_type";

  @AttributeMethod(attribute = ATTRIBUTE_SOURCE_BANK_ACCOUNT_TYPE)
  BankAccountType getSourceBankAccountType();
}
