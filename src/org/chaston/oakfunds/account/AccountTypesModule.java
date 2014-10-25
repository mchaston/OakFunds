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

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.chaston.oakfunds.security.Permission;
import org.chaston.oakfunds.storage.RecordType;

/**
 * TODO(mchaston): write JavaDocs
 */
public class AccountTypesModule extends AbstractModule {
  @Override
  protected void configure() {
    Multibinder<RecordType> recordTypeMultibinder
        = Multibinder.newSetBinder(binder(), RecordType.class);

    recordTypeMultibinder.addBinding().toInstance(AccountCode.TYPE);

    Multibinder<Permission> permissionMultibinder
        = Multibinder.newSetBinder(binder(), Permission.class);

    permissionMultibinder.addBinding()
        .toInstance(AccountCodeManagerImpl.PERMISSION_ACCOUNT_CODE_CREATE);
    permissionMultibinder.addBinding()
        .toInstance(AccountCodeManagerImpl.PERMISSION_ACCOUNT_CODE_READ);
  }
}
