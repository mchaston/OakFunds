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
package org.chaston.oakfunds.security;

import com.google.common.collect.ImmutableSet;

/**
 * TODO(mchaston): write JavaDocs
 */
public enum ActionType {
  CREATE(),
  REPORT(),
  DELETE(),
  UPDATE(DELETE),
  READ(CREATE, REPORT, UPDATE, DELETE);

  private final ImmutableSet<ActionType> impliedActions;

  ActionType(ActionType... impliedActions) {
    this.impliedActions = ImmutableSet.copyOf(impliedActions);
  }

  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }

  public ImmutableSet<ActionType> getImpliedActions() {
    return impliedActions;
  }
}
