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
package org.chaston.oakfunds.account;

import com.google.inject.Inject;
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

/**
 * TODO(mchaston): write JavaDocs
 */
public class AccountCodeCreateServlet extends HttpServlet {

  private static final ParameterHandler<Integer> PARAMETER_NUMBER =
      ParameterHandler.intParameter("number")
          .required("An account code must be supplied.")
          .build();

  private static final ParameterHandler<String> PARAMETER_TITLE =
      ParameterHandler.stringParameter("title")
          .required("A title must be supplied.")
          .build();

  private final RequestHandler requestHandler;
  private final AccountCodeManager accountCodeManager;

  @Inject
  AccountCodeCreateServlet(RequestHandler requestHandler,
      AccountCodeManager accountCodeManager) {
    this.requestHandler = requestHandler;
    this.accountCodeManager = accountCodeManager;
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    final JSONObject jsonRequest = JSONUtils.readRequest(request, "create account code");
    AccountCode accountCode =
        requestHandler.handleTransaction(request, response,
            new RequestHandler.Action<AccountCode>() {
              @Override
              public AccountCode doAction(HttpServletRequest request)
                  throws StorageException, ServletException {
                return accountCodeManager.createAccountCode(
                    PARAMETER_NUMBER.parse(jsonRequest), PARAMETER_TITLE.parse(jsonRequest));
              }
            });

    // Write result to response.
    response.setContentType("application/json");
    JSONUtils.writeJSONString(response.getWriter(), accountCode);
  }
}
