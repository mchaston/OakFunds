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
import org.chaston.oakfunds.ledger.AccountTransaction;
import org.chaston.oakfunds.ledger.LedgerManager;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.util.JSONUtils;
import org.chaston.oakfunds.util.ParameterHandler;
import org.chaston.oakfunds.util.RequestHandler;
import org.joda.time.Instant;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO(mchaston): write JavaDocs
 */
class AccountTransactionCreateServlet extends HttpServlet {

  static final String URI_REGEX = "/ledger/account/([0-9]+)/create_transaction";
  private static final Pattern URI_PATTERN = Pattern.compile(URI_REGEX);

  private static final ParameterHandler<Instant> PARAMETER_DATE =
      ParameterHandler.instantParameter("date")
          .required("A date must be supplied.")
          .build();

  private static final ParameterHandler<BigDecimal> PARAMETER_AMOUNT =
      ParameterHandler.bigDecimalParameter("amount")
          .required("An amount must be supplied.")
          .build();

  private static final ParameterHandler<String> PARAMETER_COMMENT =
      ParameterHandler.stringParameter("comment")
          .build();

  private final RequestHandler requestHandler;
  private final LedgerManager ledgerManager;

  @Inject
  AccountTransactionCreateServlet(RequestHandler requestHandler,
      LedgerManager ledgerManager) {
    this.requestHandler = requestHandler;
    this.ledgerManager = ledgerManager;
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // Read the requestURI to determine the account to add a transaction to.
    Matcher matcher = URI_PATTERN.matcher(request.getRequestURI());
    if (!matcher.matches()) {
      // This should never happen as the servlet is only invoked on a match of the same pattern.
      String errorMessage = "Unexpected match failure on: " + request.getRequestURI();
      log(errorMessage);
      response.sendError(HttpServletResponse.SC_NOT_FOUND, errorMessage);
      return;
    }

    final int accountId = Integer.parseInt(matcher.group(1));
    final JSONObject jsonRequest = JSONUtils.readRequest(request, "create account transaction");
    AccountTransaction bankAccount =
        requestHandler.handleTransaction(request, response,
            new RequestHandler.Action<AccountTransaction>() {
              @Override
              public AccountTransaction doAction(HttpServletRequest request)
                  throws StorageException, ServletException {
                Account account =
                    ledgerManager.getAccount(accountId);
                return ledgerManager.recordTransaction(account,
                    PARAMETER_DATE.parse(jsonRequest),
                    PARAMETER_AMOUNT.parse(jsonRequest),
                    PARAMETER_COMMENT.parse(jsonRequest));
              }
            });

    // Write result to response.
    response.setContentType("application/json");
    JSONUtils.writeJSONString(response.getWriter(), bankAccount);
  }
}
