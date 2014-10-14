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

import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public class AttributeSearchTerm extends SearchTerm {
  private final String attribute;
  private final SearchOperator operator;
  private final Object value;

  private AttributeSearchTerm(String attribute, SearchOperator operator, Object value) {
    this.attribute = attribute;
    this.operator = operator;
    this.value = value;
  }

  public static AttributeSearchTerm of(String attribute, SearchOperator operator, Object value) {
    return new AttributeSearchTerm(attribute, operator, value);
  }

  public String getAttribute() {
    return attribute;
  }

  public SearchOperator getOperator() {
    return operator;
  }

  public Object getValue() {
    return value;
  }

  @Override
  boolean matches(Integer parentId, int id, Map<String, Object> attributes) {
    return operator.matches(value, attributes.get(attribute));
  }
}
