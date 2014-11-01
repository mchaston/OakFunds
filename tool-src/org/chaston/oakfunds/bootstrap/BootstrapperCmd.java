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
package org.chaston.oakfunds.bootstrap;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.chaston.oakfunds.jdbc.RemoteDataStoreModule;
import org.chaston.oakfunds.storage.RecordTypeRegistryModule;
import org.chaston.oakfunds.storage.StorageModule;
import org.chaston.oakfunds.util.Flag;
import org.chaston.oakfunds.util.Flags;

import java.io.File;

/**
 * TODO(mchaston): write JavaDocs
 */
public class BootstrapperCmd {

  private static final Flag<String> BOOTSTRAP_CONFIG_FILENAME =
      Flag.builder("bootstrap_config_filename", "")
          .setShortName("f")
          .build();

  public static void main(String[] args) throws Exception {
    Injector injector = Guice.createInjector(
        new AllBootstrapTasksModule(),
        new BootstrapModule(),
        new BootstrapConfigModule(),
        new RecordTypeRegistryModule(),
        new RemoteDataStoreModule(),
        new StorageModule());

    Flags.parse(args);
    if (BOOTSTRAP_CONFIG_FILENAME.get().isEmpty()) {
      throw new IllegalArgumentException("The bootstrap_config_filename has to be specified.");
    }
    File file = new File(BOOTSTRAP_CONFIG_FILENAME.get());
    if (!file.exists()) {
      throw new IllegalArgumentException("The bootstrap config file ("
          + file.getAbsolutePath() + ") does not exist.");
    }
    if (!file.isFile()) {
      throw new IllegalArgumentException("The bootstrap config file ("
          + file.getAbsolutePath() + ") is not a file.");
    }
    if (!file.canRead()) {
      throw new IllegalArgumentException("The bootstrap config file ("
          + file.getAbsolutePath() + ") cannot be read.");
    }

    // Actually do the bootstrapping.
    injector.getInstance(BootstrapConfigReader.class).read(file);
    injector.getInstance(Bootstrapper.class).bootstrap();

    System.out.println("** Bootstrapping completed successfully. **");
  }
}
