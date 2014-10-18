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
package org.chaston.oakfunds.storage.mgmt;

import com.google.inject.AbstractModule;
import org.chaston.oakfunds.account.AccountTypesModule;
import org.chaston.oakfunds.ledger.LedgerTypesModule;
import org.chaston.oakfunds.model.ModelTypesModule;
import org.chaston.oakfunds.system.SystemTypesModule;

/**
 * TODO(mchaston): write JavaDocs
 */
public class AllTypesModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new AccountTypesModule());
    install(new LedgerTypesModule());
    install(new ModelTypesModule());
    install(new SystemTypesModule());
  }
}
