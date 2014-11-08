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

import com.google.identitytoolkit.GitkitClient;
import com.google.identitytoolkit.GitkitClientException;
import com.google.identitytoolkit.GitkitUser;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.chaston.oakfunds.security.AuthenticatedUser;
import org.chaston.oakfunds.security.AuthenticationException;
import org.chaston.oakfunds.security.AuthenticationScope;
import org.chaston.oakfunds.security.NotAuthenticatedException;
import org.chaston.oakfunds.security.SystemAuthenticationManager;
import org.chaston.oakfunds.security.User;
import org.chaston.oakfunds.security.UserAuthenticator;
import org.chaston.oakfunds.security.UserManager;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.storage.Transaction;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.InputStream;

/**
 * TODO(mchaston): write JavaDocs
 */
class GitKitUserAuthenticator implements UserAuthenticator {

  private static final String TOKEN_COOKIE_NAME = "gtoken";

  private final Store store;
  private final SystemAuthenticationManager systemAuthenticationManager;
  private final UserManager userManager;
  private final Provider<AuthenticationState> authenticationStateProvider;

  @Inject
  GitKitUserAuthenticator(
      Store store,
      SystemAuthenticationManager systemAuthenticationManager,
      UserManager userManager,
      Provider<AuthenticationState> authenticationStateProvider) {
    this.store = store;
    this.systemAuthenticationManager = systemAuthenticationManager;
    this.userManager = userManager;
    this.authenticationStateProvider = authenticationStateProvider;
  }

  @Override
  public AuthenticatedUser getAuthenticatedUser() throws AuthenticationException {
    AuthenticationState authenticationState = authenticationStateProvider.get();
    if (!authenticationState.isAuthenticated()) {
      throw new NotAuthenticatedException("User is not authenticated.");
    }
    return new AuthenticatedUser(authenticationState.getIdentifier());
  }

  @Override
  public boolean isUserLoggedIn() {
    AuthenticationState authenticationState = authenticationStateProvider.get();
    return authenticationState.isAuthenticated();
  }

  boolean isAuthenticated(HttpServletRequest servletRequest) throws ServletException {
    AuthenticationState authenticationState = authenticationStateProvider.get();
    if (authenticationState.isAuthenticated()) {
      return true;
    }
    GitkitClient gitkitClient = GitkitClient.newBuilder()
        .setCookieName(TOKEN_COOKIE_NAME)
        .setGoogleClientId(
            "885468929755-ld03mp2llinf9osesobrhmggh1oqjve3.apps.googleusercontent.com")
        .setServiceAccountEmail(
            "885468929755-6s74fog0a1ladlmce1u3o2q119ub73n5@developer.gserviceaccount.com")
        .setWidgetUrl("https://mchaston-oakfunds.appspot.com/index.html")
        .setKeyStream(getKeyStream())
        .build();
    GitkitUser gitKitUser;
    try {
      gitKitUser = gitkitClient.validateTokenInRequest(servletRequest);
    } catch (GitkitClientException e) {
      throw new ServletException("Failure to validate authentication token.", e);
    }
    if (gitKitUser != null) {
      User user;
      try {
        // Ensure that the user exists.
        Transaction transaction = store.startTransaction();
        boolean success = false;
        try (AuthenticationScope authenticationScope =
                 systemAuthenticationManager.authenticateSystem()) {
          user = userManager.upsertUser(gitKitUser.getLocalId(),
              gitKitUser.getEmail(), gitKitUser.getName());
          success = true;
        } finally {
          if (success) {
            transaction.commit();
          } else {
            transaction.rollback();
          }
        }
      } catch (StorageException e) {
        throw new ServletException("Failed to load user data.", e);
      }
      // bind user to session
      authenticationState.bindToken(user.getId(), gitKitUser);
      return true;
    } else {
      return false;
    }
  }

  private InputStream getKeyStream() {
    return getClass().getClassLoader()
        .getResourceAsStream("META-INF/secrets/ServiceAccountPrivateKey.p12");
  }

  void signout(HttpServletRequest servletRequest) {
    AuthenticationState authenticationState = authenticationStateProvider.get();
    authenticationState.signout();
    HttpSession session = servletRequest.getSession();
    if (session != null) {
      session.invalidate();
    }
  }
}
