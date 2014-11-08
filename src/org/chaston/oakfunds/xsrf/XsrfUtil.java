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
package org.chaston.oakfunds.xsrf;

import com.google.appengine.api.utils.SystemProperty;
import com.google.inject.Inject;
import org.chaston.oakfunds.security.AuthenticationException;
import org.chaston.oakfunds.security.UserAuthenticator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;

/**
 * TODO(mchaston): write JavaDocs
 */
public class XsrfUtil {

  private static final Logger LOG = Logger.getLogger(XsrfUtil.class.getName());

  private final UserAuthenticator userAuthenticator;
  private final XsrfSigner xsrfSigner;

  @Inject
  XsrfUtil(UserAuthenticator userAuthenticator, XsrfSigner xsrfSigner) {
    this.userAuthenticator = userAuthenticator;
    this.xsrfSigner = xsrfSigner;
  }

  public void addXsrfToken(HttpServletResponse response)
      throws IOException, ServletException {
    PrintWriter writer = response.getWriter();
    if (userAuthenticator.isUserLoggedIn()) {
      // When logged in, add the XSRF cookie
      StringBuilder cookie = new StringBuilder();
      try {
        cookie.append("XSRF-TOKEN=").append(xsrfSigner.sign(createMaterial()));
      } catch (GeneralSecurityException e) {
        throw new ServletException("Failure to create XSRF signature.");
      }
      if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
        // Only force secure on production as development is not on HTTPS.
        cookie.append(";secure");
      }
      writer.println("document.cookie=\"" + cookie + "\";");
    }
  }

  public boolean verifyXsrfToken(HttpServletRequest request) throws ServletException {
    return verifyXsrfToken(request.getHeader("X-XSRF-TOKEN"));
  }

  public boolean verifyXsrfToken(String token) throws ServletException {
    if (token == null || token.isEmpty()) {
      return false;
    }
    try {
      return xsrfSigner.verify(createMaterial(), token);
    } catch (GeneralSecurityException e) {
      LOG.warning("Unable to verify XSRF token.");
      return false;
    }
  }

  private String createMaterial() throws ServletException {
    try {
      return userAuthenticator.getAuthenticatedUser().getIdentifier();
    } catch (AuthenticationException e) {
      throw new ServletException("Failed to authenticate user", e);
    }
  }
}
