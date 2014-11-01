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

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.chaston.oakfunds.jdbc.RemoteDataStoreModule;
import org.chaston.oakfunds.storage.RecordTypeRegistryModule;
import org.chaston.oakfunds.util.Flags;

import java.sql.SQLException;

/**
 * TODO(mchaston): write JavaDocs
 */
public class SchemaValidatorCmd {
  public static void main(String[] args) throws SQLException {
    Flags.parse(args);
    Injector injector = Guice.createInjector(
        new RecordTypeRegistryModule(),
        new AllTypesModule(),
        new StorageManagementModule(),
        new RemoteDataStoreModule());
    injector.getInstance(SchemaValidator.class).validateSchema();

    System.out.println("** Schema validated successfully. **");
  }
}
