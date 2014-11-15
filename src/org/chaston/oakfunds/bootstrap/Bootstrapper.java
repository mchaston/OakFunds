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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * TODO(mchaston): write JavaDocs
 */
class Bootstrapper {

  private static final Logger logger = Logger.getLogger(Bootstrapper.class.getName());

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
    List<BootstrapTask> bootstrapTaskList = new LinkedList<>(bootstrapTasks);
    Collections.sort(bootstrapTaskList, new BootstrapTaskOrderingComparator());
    try (AuthenticationScope authenticationScope =
             authenticationManager.authenticateSystem()) {
      for (BootstrapTask bootstrapTask : bootstrapTaskList) {
        logger.info("Bootstrapping " + bootstrapTask.getName() + "...");
        bootstrapTask.bootstrap();
      }
      logger.info("Bootstrapping complete.");
    }
  }

  private class BootstrapTaskOrderingComparator
      implements java.util.Comparator<BootstrapTask> {
    @Override
    public int compare(BootstrapTask bootstrapTask1, BootstrapTask bootstrapTask2) {
      int order = bootstrapTask1.getPriority() - bootstrapTask2.getPriority();
      if (order == 0) {
        // Arbitrary but deterministic tie breaker.
        order = bootstrapTask1.hashCode() - bootstrapTask2.hashCode();
      }
      return order;
    }
  }
}
