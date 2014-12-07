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

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.chaston.oakfunds.security.Permission;
import org.chaston.oakfunds.storage.RecordType;

/**
 * TODO(mchaston): write JavaDocs
 */
public class ModelTypesModule extends AbstractModule {
  @Override
  protected void configure() {
    Multibinder<RecordType> recordTypeMultibinder
        = Multibinder.newSetBinder(binder(), RecordType.class);

    recordTypeMultibinder.addBinding().toInstance(Model.TYPE);

    recordTypeMultibinder.addBinding().toInstance(RecurringEvent.TYPE);
    recordTypeMultibinder.addBinding().toInstance(AnnualRecurringEvent.TYPE);
    recordTypeMultibinder.addBinding().toInstance(MonthlyRecurringEvent.TYPE);

    recordTypeMultibinder.addBinding().toInstance(ModelAccountTransaction.TYPE);
    recordTypeMultibinder.addBinding().toInstance(ModelDistributionTransaction.TYPE);

    Multibinder<Permission> permissionMultibinder
        = Multibinder.newSetBinder(binder(), Permission.class);

    permissionMultibinder.addBinding()
        .toInstance(ModelManagerImpl.PERMISSION_MODEL_READ);
    permissionMultibinder.addBinding()
        .toInstance(ModelManagerImpl.PERMISSION_MODEL_CREATE);
    permissionMultibinder.addBinding()
        .toInstance(ModelManagerImpl.PERMISSION_MODEL_UPDATE);
    permissionMultibinder.addBinding()
        .toInstance(ModelManagerImpl.PERMISSION_MONTHLY_RECURRING_EVENT_UPDATE);
    permissionMultibinder.addBinding()
        .toInstance(ModelManagerImpl.PERMISSION_ANNUAL_RECURRING_EVENT_UPDATE);
    permissionMultibinder.addBinding()
        .toInstance(ModelManagerImpl.PERMISSION_MODEL_ACCOUNT_TRANSACTION_CREATE);
    permissionMultibinder.addBinding()
        .toInstance(ModelManagerImpl.PERMISSION_MODEL_ACCOUNT_TRANSACTION_READ);
    permissionMultibinder.addBinding()
        .toInstance(ModelManagerImpl.PERMISSION_MODEL_ACCOUNT_TRANSACTION_UPDATE);
    permissionMultibinder.addBinding()
        .toInstance(ModelManagerImpl.PERMISSION_MODEL_ACCOUNT_TRANSACTION_DELETE);
    permissionMultibinder.addBinding()
        .toInstance(ModelManagerImpl.PERMISSION_MODEL_ACCOUNT_TRANSACTION_REPORT);
    permissionMultibinder.addBinding()
        .toInstance(ModelManagerImpl.PERMISSION_MODEL_DISTRIBUTION_TRANSACTION_READ);
    permissionMultibinder.addBinding()
        .toInstance(ModelManagerImpl.PERMISSION_MODEL_DISTRIBUTION_TRANSACTION_REPORT);
  }
}
