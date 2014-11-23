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
package org.chaston.oakfunds.ledger.ui;

import com.google.inject.Inject;
import org.chaston.oakfunds.ledger.Account;
import org.chaston.oakfunds.ledger.LedgerManager;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.util.JSONUtils;
import org.chaston.oakfunds.util.RequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO(mchaston): write JavaDocs
 */
class AccountReadServlet extends HttpServlet {

  static final String URI_REGEX = "/ledger/account/([0-9]+)";
  private static final Pattern URI_PATTERN = Pattern.compile(URI_REGEX);

  private final RequestHandler requestHandler;
  private final LedgerManager ledgerManager;

  @Inject
  AccountReadServlet(RequestHandler requestHandler,
      LedgerManager ledgerManager) {
    this.requestHandler = requestHandler;
    this.ledgerManager = ledgerManager;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // Read the requestURI to determine the account to get transactions for.
    Matcher matcher = URI_PATTERN.matcher(request.getRequestURI());
    if (!matcher.matches()) {
      // This should never happen as the servlet is only invoked on a match of the same pattern.
      String errorMessage = "Unexpected match failure on: " + request.getRequestURI();
      log(errorMessage);
      response.sendError(HttpServletResponse.SC_NOT_FOUND, errorMessage);
      return;
    }

    final int id = Integer.parseInt(matcher.group(1));
    Account account =
        requestHandler.handle(request, response,
            new RequestHandler.Action<Account>() {
              @Override
              public Account doAction(HttpServletRequest request)
                  throws StorageException, ServletException {
                return ledgerManager.getAccount(id);
              }
            });

    // Write result to response.
    response.setContentType("application/json");
    JSONUtils.writeJSONString(response.getWriter(), account);
  }
}
