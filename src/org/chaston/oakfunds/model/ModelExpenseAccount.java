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
import org.chaston.oakfunds.storage.Attribute;
import org.chaston.oakfunds.storage.RecordType;

/**
 * TODO(mchaston): write JavaDocs
 */
public class ModelExpenseAccount extends ModelAccount<ModelExpenseAccount> {

  static final RecordType<ModelExpenseAccount> TYPE =
      new RecordType<>("model_expense_account", ModelExpenseAccount.class,
          ModelAccount.TYPE, true);

  @Attribute(name = "source_bank_account_type", propertyName = "sourceBankAccountType")
  private BankAccountType sourceBankAccountType;

  ModelExpenseAccount(int id) {
    super(TYPE, id);
  }

  public BankAccountType getSourceBankAccountType() {
    return sourceBankAccountType;
  }

  public void setSourceBankAccountType(BankAccountType sourceBankAccountType) {
    this.sourceBankAccountType = sourceBankAccountType;
  }
}
