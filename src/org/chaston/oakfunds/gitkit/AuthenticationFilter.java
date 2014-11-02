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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * TODO(mchaston): write JavaDocs
 */
class AuthenticationFilter implements Filter {

  private static final ImmutableSet<String> KNOWN_PASS_THROUGH_PATHS =
      ImmutableSet.of(
          "/login.html",
          "/gitkit_email",
          "/signout");

  private final GitKitUserAuthenticator userAuthenticator;
  private ServletContext servletContext;

  @Inject
  AuthenticationFilter(GitKitUserAuthenticator userAuthenticator) {
    this.userAuthenticator = userAuthenticator;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Do nothing.
    servletContext = filterConfig.getServletContext();
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) servletRequest;
    // If authenticated, let the call through.
    if (userAuthenticator.isAuthenticated(req)) {
      servletContext.log("Authenticated user can access any content.");
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }
    // Allow pass through to known paths.
    for (String knownPassThroughPath : KNOWN_PASS_THROUGH_PATHS) {
      if (req.getRequestURI().endsWith(knownPassThroughPath)) {
        servletContext.log("Unauthenticated user can access login page.");
        filterChain.doFilter(servletRequest, servletResponse);
        return;
      }
    }
    // If not authenticated, redirect to the login screen.
    servletContext.log("Unauthenticated user being redirected to login page.");
    ((HttpServletResponse) servletResponse).sendRedirect("/login.html?mode=select");
  }

  @Override
  public void destroy() {
    // Do nothing.
  }
}
