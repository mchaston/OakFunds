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

import org.chaston.oakfunds.storage.AttributeMethod;
import org.chaston.oakfunds.storage.Record;
import org.chaston.oakfunds.storage.RecordType;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface User extends Record<User> {
  RecordType<User> TYPE = RecordType.builder("user", User.class)
      .build();

  String ATTRIBUTE_IDENTIFIER = "identifier";
  String ATTRIBUTE_EMAIL = "email";
  String ATTRIBUTE_NAME = "name";

  @AttributeMethod(attribute = ATTRIBUTE_IDENTIFIER, required = true)
  String getIdentifier();

  @AttributeMethod(attribute = ATTRIBUTE_EMAIL)
  String getEmail();

  @AttributeMethod(attribute = ATTRIBUTE_NAME)
  String getName();
}
