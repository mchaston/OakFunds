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
import org.chaston.oakfunds.ledger.BankAccount;
import org.chaston.oakfunds.ledger.LedgerManager;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.util.JSONUtils;
import org.chaston.oakfunds.util.RequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * TODO(mchaston): write JavaDocs
 */
class BankAccountListServlet extends HttpServlet {
  private final RequestHandler requestHandler;
  private final LedgerManager ledgerManager;

  @Inject
  BankAccountListServlet(RequestHandler requestHandler,
      LedgerManager ledgerManager) {
    this.requestHandler = requestHandler;
    this.ledgerManager = ledgerManager;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Iterable<BankAccount> accounts =
        requestHandler.handle(request, response,
            new RequestHandler.Action<Iterable<BankAccount>>() {
              @Override
              public Iterable<BankAccount> doAction(HttpServletRequest request)
                  throws StorageException, ServletException {
                return ledgerManager.getBankAccounts();
              }
            });

    // Write result to response.
    response.setContentType("application/json");
    JSONUtils.writeJSONString(response.getWriter(), accounts);
  }
}
