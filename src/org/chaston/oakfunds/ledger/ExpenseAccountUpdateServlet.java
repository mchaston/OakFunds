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
package org.chaston.oakfunds.ledger;

import com.google.inject.Inject;
import org.chaston.oakfunds.account.AccountCode;
import org.chaston.oakfunds.account.AccountCodeManager;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.util.JSONUtils;
import org.chaston.oakfunds.util.ParameterHandler;
import org.chaston.oakfunds.util.RequestHandler;
import org.json.simple.JSONObject;

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
class ExpenseAccountUpdateServlet extends HttpServlet {

  static final String URI_REGEX = "/ledger/expense_account/([0-9]+)/update";
  private static final Pattern URI_PATTERN = Pattern.compile(URI_REGEX);

  private static final ParameterHandler<String> PARAMETER_TITLE =
      ParameterHandler.stringParameter("title")
          .required("A title must be supplied.")
          .build();

  private static final ParameterHandler<Integer> PARAMETER_ACCOUNT_CODE_ID =
      ParameterHandler.intParameter("account_code_id")
          .required("An account code must be supplied.")
          .build();

  private static final ParameterHandler<Integer> PARAMETER_DEFAULT_SOURCE_ACCOUNT_ID =
      ParameterHandler.intParameter("default_source_account_id")
          .build();

  private final RequestHandler requestHandler;
  private final AccountCodeManager accountCodeManager;
  private final LedgerManager ledgerManager;

  @Inject
  ExpenseAccountUpdateServlet(RequestHandler requestHandler,
      AccountCodeManager accountCodeManager, LedgerManager ledgerManager) {
    this.requestHandler = requestHandler;
    this.accountCodeManager = accountCodeManager;
    this.ledgerManager = ledgerManager;
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // Read the requestURI to determine the expense account to update.
    Matcher matcher = URI_PATTERN.matcher(request.getRequestURI());
    if (!matcher.matches()) {
      // This should never happen as the servlet is only invoked on a match of the same pattern.
      String errorMessage = "Unexpected match failure on: " + request.getRequestURI();
      log(errorMessage);
      response.sendError(HttpServletResponse.SC_NOT_FOUND, errorMessage);
      return;
    }

    final int id = Integer.parseInt(matcher.group(1));
    final JSONObject jsonRequest = JSONUtils.readRequest(request, "update expense account");
    final ExpenseAccount expenseAccount =
        requestHandler.handleTransaction(request, response,
            new RequestHandler.Action<ExpenseAccount>() {
              @Override
              public ExpenseAccount doAction(HttpServletRequest request)
                  throws StorageException, ServletException {
                AccountCode accountCode =
                    accountCodeManager.getAccountCode(
                        PARAMETER_ACCOUNT_CODE_ID.parse(jsonRequest));
                Integer defaultSourceAccountId =
                    PARAMETER_DEFAULT_SOURCE_ACCOUNT_ID.parse(jsonRequest);
                BankAccount defaultSourceAccount = null;
                if (defaultSourceAccountId != null) {
                  defaultSourceAccount = ledgerManager.getBankAccount(defaultSourceAccountId);
                }
                ExpenseAccount expenseAccount = ledgerManager.getExpenseAccount(id);
                return ledgerManager.updateExpenseAccount(expenseAccount, accountCode,
                    PARAMETER_TITLE.parse(jsonRequest),
                    defaultSourceAccount);
              }
            });

    // Write result to response.
    response.setContentType("application/json");
    JSONUtils.writeJSONString(response.getWriter(), expenseAccount);
  }
}
