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

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.chaston.oakfunds.storage.RecordType;

/**
 * TODO(mchaston): write JavaDocs
 */
public class Permission {
  private final String name;
  private final ImmutableMultimap<RecordType, ActionType> relatedActions;

  Permission(String name, ImmutableMultimap<RecordType, ActionType> relatedActions) {
    this.name = name;
    this.relatedActions = relatedActions;
  }

  public String getName() {
    return name;
  }

  public ImmutableMultimap<RecordType, ActionType> getRelatedActions() {
    return relatedActions;
  }

  public static Builder builder(String name) {
    return new Builder(name);
  }

  public static class Builder {
    private final String name;
    private Multimap<RecordType, ActionType> relatedActions =
        MultimapBuilder.hashKeys().arrayListValues().build();

    Builder(String name) {
      this.name = name;
    }

    public Builder addRelatedAction(RecordType recordType, ActionType actionType) {
      relatedActions.put(recordType, actionType);
      return this;
    }

    public Permission build() {
      return new Permission(name, ImmutableMultimap.copyOf(relatedActions));
    }
  }
}
