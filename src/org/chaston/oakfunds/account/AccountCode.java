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
package org.chaston.oakfunds.account;

import org.chaston.oakfunds.storage.AttributeMethod;
import org.chaston.oakfunds.storage.Record;
import org.chaston.oakfunds.storage.RecordTemporalType;
import org.chaston.oakfunds.storage.RecordType;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface AccountCode extends Record<AccountCode> {

  static final RecordType<AccountCode> TYPE =
      new RecordType<>("account_code", AccountCode.class,
          RecordTemporalType.NONE, true);

  @AttributeMethod(attribute = "title")
  String getTitle();
}
