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
package org.chaston.oakfunds.security.ui;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.chaston.oakfunds.security.AuthorizationContext;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.util.RequestHandler;
import org.json.simple.JSONArray;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * TODO(mchaston): write JavaDocs
 */
class UserPermissionsServlet extends HttpServlet {

  private final RequestHandler requestHandler;
  private final Provider<AuthorizationContext> authorizationContextProvider;

  @Inject
  UserPermissionsServlet(RequestHandler requestHandler,
      Provider<AuthorizationContext> authorizationContextProvider) {
    this.requestHandler = requestHandler;
    this.authorizationContextProvider = authorizationContextProvider;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Set<String> permissions = requestHandler.handle(request, response,
        new RequestHandler.Action<Set<String>>() {
          @Override
          public Set<String> doAction(HttpServletRequest request)
              throws StorageException, ServletException {
            return authorizationContextProvider.get().getAllPermissions();
          }
        });

    // Write result to response.
    response.setContentType("application/json");
    JSONArray jsonArray = new JSONArray();
    jsonArray.addAll(permissions);
    jsonArray.writeJSONString(response.getWriter());
  }
}
