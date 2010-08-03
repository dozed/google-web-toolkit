/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.requestfactory.server;

import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.user.server.rpc.RPCServletUtils;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Handles GWT RequestFactory JSON requests. Does user authentication on every
 * request, returning SC_UNAUTHORIZED if authentication fails, as well as a
 * header named "login" which contains the URL the user should be sent in to
 * login. If authentication fails, a header named "userId" is returned, which
 * will be unique to the user (so the app can react if the signed in user has
 * changed).
 * 
 * Configured via servlet init params.
 * <p>
 * e.g. - in order to use GAE authentication:
 * 
 * <pre>  &lt;init-param>
    &lt;param-name>userInfoClass&lt;/param-name>
    &lt;param-value>com.google.gwt.sample.expenses.server.domain.GaeUserInformation&lt;/param-value>
  &lt;/init-param>

 * </pre>
 */
@SuppressWarnings("serial")
public class RequestFactoryServlet extends HttpServlet {
  private static final String JSON_CHARSET = "UTF-8";
  private static final String JSON_CONTENT_TYPE = "application/json";
  
  @SuppressWarnings("unchecked")
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    ensureConfig();
    String jsonRequestString = RPCServletUtils.readContent(
        request, JSON_CONTENT_TYPE, JSON_CHARSET);
    response.setStatus(HttpServletResponse.SC_OK);
    PrintWriter writer = response.getWriter();

    try {
      // Check that user is logged in before proceeding
      UserInformation userInfo = UserInformation.getCurrentUserInformation();
      if (!userInfo.isUserLoggedIn()) {
        response.setHeader("login", userInfo.getLoginUrl());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      } else {
        response.setHeader("userId", String.format("%d", userInfo.getId()));
        response.setStatus(HttpServletResponse.SC_OK);
        JsonRequestProcessor requestProcessor = new JsonRequestProcessor();
        requestProcessor.setOperationRegistry(new ReflectionBasedOperationRegistry(
            new DefaultSecurityProvider()));
        response.setHeader(
            "Content-Type", RequestFactory.JSON_CONTENT_TYPE_UTF8);
        writer.print(requestProcessor.decodeAndInvokeRequest(jsonRequestString));
        writer.flush();
      }
      // TODO: clean exception handling code below.
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void ensureConfig() {
    // Instantiate a class for authentication, using either the default
    // UserInfo class, or a subclass if the web.xml specifies one. This allows
    // clients to use a Google App Engine based authentication class without
    // adding GAE dependencies to GWT.
    String userInfoClass = getServletConfig().getInitParameter("userInfoClass");
    if (userInfoClass != null) {
      UserInformation.setUserInformationImplClass(userInfoClass);
    }
  }
}
