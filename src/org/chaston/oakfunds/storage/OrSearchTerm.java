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
package org.chaston.oakfunds.storage;

import com.google.common.collect.ImmutableList;

import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public class OrSearchTerm extends SearchTerm {
  private final ImmutableList<SearchTerm> searchTerms;

  OrSearchTerm(ImmutableList<SearchTerm> searchTerms) {
    this.searchTerms = searchTerms;
  }

  public static OrSearchTerm of(SearchTerm... searchTerms) {
    return new OrSearchTerm(ImmutableList.copyOf(searchTerms));
  }

  @Override
  boolean matches(Integer parentId, int id, Map<String, Object> attributes) {
    for (SearchTerm searchTerm : searchTerms) {
      if (searchTerm.matches(parentId, id, attributes)) {
        return true;
      }
    }
    return false;
  }
}