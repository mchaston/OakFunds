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
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import org.chaston.oakfunds.jdbc.FunctionDef;
import org.chaston.oakfunds.storage.RecordType;

import javax.sql.DataSource;

/**
 * TODO(mchaston): write JavaDocs
 */
public class StorageManagementModule extends AbstractModule {
  @Override
  protected void configure() {
    requireBinding(SchemaBuilder.class);
    requireBinding(DataSource.class);
    bind(SchemaValidator.class).in(Scopes.SINGLETON);
    bind(SchemaUpdater.class).in(Scopes.SINGLETON);

    // Preemptively bind multivalues so that they are never null.
    Multibinder.newSetBinder(binder(), RecordType.class);
    Multibinder.newSetBinder(binder(), FunctionDef.class);
  }
}
