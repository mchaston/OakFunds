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
package org.chaston.oakfunds.util;

import com.google.inject.Inject;
import org.chaston.oakfunds.security.AuthenticationException;
import org.chaston.oakfunds.security.AuthenticationScope;
import org.chaston.oakfunds.security.AuthorizationException;
import org.chaston.oakfunds.security.UserAuthenticationManager;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.storage.Transaction;
import org.chaston.oakfunds.xsrf.XsrfUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * TODO(mchaston): write JavaDocs
 */
public class RequestHandler {
  private final Store store;
  private final UserAuthenticationManager userAuthenticationManager;
  private final XsrfUtil xsrfUtil;

  @Inject
  RequestHandler(
      Store store,
      UserAuthenticationManager userAuthenticationManager,
      XsrfUtil xsrfUtil) {
    this.store = store;
    this.userAuthenticationManager = userAuthenticationManager;
    this.xsrfUtil = xsrfUtil;
  }

  public <V> V handle(HttpServletRequest request, HttpServletResponse response, Action<V> action)
      throws ServletException, IOException {
    if (!xsrfUtil.verifyXsrfToken(request)) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return null;
    }
    try {
      try (AuthenticationScope authenticationScope = userAuthenticationManager.authenticateUser()) {
        return action.doAction(request);
      }
    } catch (StorageException e) {
      throw new ServletException("Failed to handle request due to a storage problem.", e);
    } catch (AuthenticationException e) {
      throw new ServletException("Failed to handle request due to an authentication problem.", e);
    } catch (AuthorizationException e) {
      throw new ServletException("Failed to handle request due to an authorization problem.", e);
    }
  }

  public <V> V handleTransaction(HttpServletRequest request, HttpServletResponse response,
      Action<V> action) throws ServletException, IOException {
    if (!xsrfUtil.verifyXsrfToken(request)) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return null;
    }
    try {
      try (AuthenticationScope authenticationScope = userAuthenticationManager.authenticateUser()) {
        Transaction transaction = store.startTransaction();
        boolean succeeded = false;
        try {
          V value = action.doAction(request);
          succeeded = true;
          return value;
        } finally {
          if (succeeded) {
            transaction.commit();
          } else {
            transaction.rollback();
          }
        }
      }
    } catch (StorageException e) {
      throw new ServletException("Failed to handle request due to a storage problem.", e);
    } catch (AuthenticationException e) {
      throw new ServletException("Failed to handle request due to an authentication problem.", e);
    } catch (AuthorizationException e) {
      throw new ServletException("Failed to handle request due to an authorization problem.", e);
    }
  }

  public interface Action<V> {
    V doAction(HttpServletRequest request) throws StorageException, ServletException;
  }
}
