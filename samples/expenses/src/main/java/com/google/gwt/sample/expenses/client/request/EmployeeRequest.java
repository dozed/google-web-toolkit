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
package com.google.gwt.sample.expenses.client.request;

import com.google.gwt.requestfactory.shared.Instance;

import com.google.gwt.requestfactory.shared.ProxyListRequest;
import com.google.gwt.requestfactory.shared.ProxyRequest;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.requestfactory.shared.Service;
import com.google.gwt.sample.expenses.server.domain.Employee;

/**
 * "API Generated" request selector interface implemented by objects that give
 * client access to the methods of
 * {@link com.google.gwt.sample.expenses.server.domain.Employee}.
 * <p>
 * IRL this class will be generated by a JPA-savvy tool run before compilation.
 */
@Service(Employee.class)
public interface EmployeeRequest {

  /**
   * @return a request object
   */
  RequestObject<Long> countEmployees();

  /**
   * @return a request object
   */
  RequestObject<Long> countEmployeesByDepartment(
      String department);

  /**
   * @return a request object
   */
  ProxyListRequest<EmployeeProxy> findAllEmployees();

  /**
   * @return a request object
   */
  ProxyRequest<EmployeeProxy> findEmployee(Long id);

  /**
   * @return a request object
   */
  ProxyListRequest<EmployeeProxy> findEmployeeEntries(int firstResult,
      int maxResults);

  /**
   * @return a request object
   */
  ProxyListRequest<EmployeeProxy> findEmployeeEntriesByDepartment(
      String department, int firstResult, int maxResults);

  /**
   * @return a request object
   */
  @Instance
  RequestObject<Void> persist(EmployeeProxy record);

 /**
  * @return a request object
  */
  @Instance
  RequestObject<Void> remove(EmployeeProxy record);
}
