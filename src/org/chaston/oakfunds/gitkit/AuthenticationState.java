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
package org.chaston.oakfunds.gitkit;

import com.google.identitytoolkit.GitkitUser;

import java.io.Serializable;

/**
 * TODO(mchaston): write JavaDocs
 */
class AuthenticationState implements Serializable {
  private Long userId;
  private String localId;
  private String email;
  private String name;

  void bindToken(long userId, GitkitUser gitkitUser) {
    this.userId = userId;
    localId = gitkitUser.getLocalId();
    email = gitkitUser.getEmail();
    name = gitkitUser.getName();
  }

  boolean isAuthenticated() {
    return localId != null;
  }

  void signout() {
    userId = null;
    localId = null;
    email = null;
    name = null;
  }
}
