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

import com.google.inject.Inject;
import org.chaston.oakfunds.security.AuthenticationScope;
import org.chaston.oakfunds.security.SystemAuthenticationManager;

import java.util.Set;

/**
 * TODO(mchaston): write JavaDocs
 */
class Bootstrapper {

  private final SystemAuthenticationManager authenticationManager;
  private final Set<BootstrapTask> bootstrapTasks;

  @Inject
  Bootstrapper(
      SystemAuthenticationManager authenticationManager,
      Set<BootstrapTask> bootstrapTasks) {
    this.authenticationManager = authenticationManager;
    this.bootstrapTasks = bootstrapTasks;
  }

  void bootstrap() throws Exception {
    try (AuthenticationScope authenticationScope =
             authenticationManager.authenticateSystem()) {
      for (BootstrapTask bootstrapTask : bootstrapTasks) {
        bootstrapTask.bootstrap();
      }
    }
  }
}
