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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO(mchaston): write JavaDocs
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AttributeMethod {
  /**
   * The name of the attribute that this method binds to.
   *
   * <p>The rules for this are:
   * <ul>
   *   <li>It must have at least one character.</li>
   *   <li>It must only use lower case characters and the '_' character.</li>
   *   <li>It cannot start with "sys_" as that is used by system columns.</li>
   *   <li>It cannot start with a '_' character as databases do not support that.</li>
   *   <li>It cannot end with a '_' character as that is considered bad form.</li>
   *   <li>It cannot contain two adjacent '_' characters as that is considered bad form due
   *       to its use for subtype namespacing by the system.</li>
   *   <li>It cannot be the same as any other attribute in the same type.</li>
   *   <li>If the type is in an inheritance hierarchy, it cannot have the same name as
   *       any attributes in its parent types (but it can be the same as its sibling types, but
   *       this should be implemented as a common interface).</li>
   * </ul>
   */
  String attribute();

  boolean required() default false;
}
