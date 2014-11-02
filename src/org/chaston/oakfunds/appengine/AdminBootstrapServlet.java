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
package org.chaston.oakfunds.appengine;

import com.google.appengine.api.users.UserService;
import com.google.inject.Inject;
import org.chaston.oakfunds.security.AuthenticatedUser;
import org.chaston.oakfunds.security.AuthenticationException;
import org.chaston.oakfunds.security.AuthenticationScope;
import org.chaston.oakfunds.security.RoleGrant;
import org.chaston.oakfunds.security.SystemAuthenticationManager;
import org.chaston.oakfunds.security.User;
import org.chaston.oakfunds.security.UserAuthenticator;
import org.chaston.oakfunds.security.UserManager;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.storage.Transaction;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * TODO(mchaston): write JavaDocs
 */
class AdminBootstrapServlet extends HttpServlet {

  private static final String ADMIN_ROLE_NAME = "admin";

  private final SystemAuthenticationManager systemAuthenticationManager;
  private final Store store;
  private final UserAuthenticator userAuthenticator;
  private final UserManager userManager;
  private final UserService appEngineUserService;

  @Inject
  AdminBootstrapServlet(SystemAuthenticationManager systemAuthenticationManager, Store store,
      UserAuthenticator userAuthenticator, UserManager userManager,
      UserService appEngineUserService) {
    this.systemAuthenticationManager = systemAuthenticationManager;
    this.store = store;
    this.userAuthenticator = userAuthenticator;
    this.userManager = userManager;
    this.appEngineUserService = appEngineUserService;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    if (!appEngineUserService.isUserLoggedIn()) {
      reportMessage(resp, "User is not logged in to appengine.  Will not bootstrap.");
      return;
    }
    if (!appEngineUserService.isUserAdmin()) {
      reportMessage(resp, "User is not an appengine admin.  Will not bootstrap.");
      return;
    }
    try (AuthenticationScope authenticationScope =
             systemAuthenticationManager.authenticateSystem()) {
      AuthenticatedUser authenticatedUser = userAuthenticator.getAuthenticatedUser();
      User user = userManager.getUser(authenticatedUser.getIdentifier());
      Iterable<RoleGrant> grants = userManager.getRoleGrants(user);
      boolean alreadyAdmin = false;
      for (RoleGrant grant : grants) {
        if (ADMIN_ROLE_NAME.equals(grant.getName())) {
          alreadyAdmin = true;
        }
      }
      if (alreadyAdmin) {
        reportMessage(resp, "User already has the " + ADMIN_ROLE_NAME + " role.");
        return;
      }
      Transaction transaction = store.startTransaction();
      boolean successful = false;
      try {
        userManager.grantRole(user, ADMIN_ROLE_NAME);
        successful = true;
      } finally {
        if (successful) {
          transaction.commit();
        } else {
          transaction.rollback();
        }
      }
      reportMessage(resp, "User has been granted the " + ADMIN_ROLE_NAME + " role.");
    } catch (AuthenticationException e) {
      throw new ServletException("Failed to authenticate the user.", e);
    } catch (StorageException e) {
      throw new ServletException("Failed to boostrap the admin user.", e);
    }
  }

  private void reportMessage(HttpServletResponse resp, String message) throws IOException {
    PrintWriter writer = resp.getWriter();
    writer.append("<html>");
    writer.append("<body>");
    writer.append("<p>");
    writer.append(message);
    writer.append("</p>");
    writer.append("</body>");
    writer.append("</html>");
  }
}
