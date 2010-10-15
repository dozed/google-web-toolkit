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

package com.google.gwt.requestfactory.shared;

import com.google.gwt.requestfactory.server.UserInformation;

/**
 * "API Generated" DTO interface based on {@link UserInformation}.
 */
@ProxyFor(UserInformation.class)
public interface UserInformationProxy extends EntityProxy  {
  /**
   * Returns the user's email address.
   *
   * @return the user's email address as a String
   */
  String getEmail();
  
  /**
   * Returns the user's login url.
   *
   * @return the user's login url as a String
   */
  String getLoginUrl();
  
  /**
   * Returns the user's logout url.
   *
   * @return the user's logout url as a String
   */
  String getLogoutUrl();
  
  /**
   * Returns the user's name.
   *
   * @return the user's name as a String
   */
  String getName();
}
