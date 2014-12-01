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

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.chaston.oakfunds.security.RoleGrant;
import org.chaston.oakfunds.security.User;
import org.chaston.oakfunds.security.UserManager;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.util.JSONRepresentable;
import org.chaston.oakfunds.util.JSONUtils;
import org.chaston.oakfunds.util.RequestHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

/**
 * TODO(mchaston): write JavaDocs
 */
class UserListServlet extends HttpServlet {
  private final RequestHandler requestHandler;
  private final UserManager userManager;

  @Inject
  UserListServlet(RequestHandler requestHandler,
      UserManager userManager) {
    this.requestHandler = requestHandler;
    this.userManager = userManager;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Iterable<UserWithRoleNames> usersWithRoleNames =
        requestHandler.handle(request, response,
            new RequestHandler.Action<Iterable<UserWithRoleNames>>() {
              @Override
              public Iterable<UserWithRoleNames> doAction(HttpServletRequest request)
                  throws StorageException, ServletException {
                Iterable<User> users = userManager.getUsers();
                ImmutableList.Builder<UserWithRoleNames> results = ImmutableList.builder();
                for (User user : users) {
                  results.add(new UserWithRoleNames(user, userManager.getRoleGrants(user)));
                }
                return results.build();
              }
            });

    // Write result to response.
    response.setContentType("application/json");
    JSONUtils.writeJSONString(response.getWriter(), usersWithRoleNames);
  }

  private static class UserWithRoleNames implements JSONRepresentable {
    private final User user;
    private final Set<String> roleNames = new TreeSet<>();

    private UserWithRoleNames(User user, Iterable<RoleGrant> roleGrants) {
      this.user = user;
      for (RoleGrant roleGrant : roleGrants) {
        roleNames.add(roleGrant.getName());
      }
    }

    @Override
    public JSONObject toJSONObject() {
      JSONObject jsonObject = user.toJSONObject();
      JSONArray jsonArray = new JSONArray();
      jsonArray.addAll(roleNames);
      jsonObject.put("role_names", jsonArray);
      return jsonObject;
    }
  }
}
