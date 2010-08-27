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

import com.google.gwt.requestfactory.shared.DataTransferObject;
import com.google.gwt.requestfactory.shared.Property;
import com.google.gwt.requestfactory.shared.Record;

import java.util.Date;

/**
 * "API Generated" DTO interface based on
 * {@link com.google.gwt.sample.expenses.server.domain.Expense}.
 * <p>
 * IRL this class will be generated by a JPA-savvy tool run before compilation.
 */
@DataTransferObject(com.google.gwt.sample.expenses.server.domain.Expense.class)
public interface ExpenseRecord extends Record {

  Property<Double> amount = new Property<Double>("amount", "Amount", Double.class);
  Property<String> approval = new Property<String>("approval", "Approval", String.class);
  Property<String> category = new Property<String>("category", "Category", String.class);
  Property<Date> created = new Property<Date>("created", "Created", Date.class);
  Property<String> description = new Property<String>("description", "Description", String.class);
  Property<String> reasonDenied = new Property<String>("reasonDenied", "Reason Denied", String.class);
  Property<ReportRecord> report = new Property<ReportRecord>("report", "Report", ReportRecord.class);

  Double getAmount();
  
  String getApproval();
  
  String getCategory();

  Date getCreated();

  String getDescription();

  String getReasonDenied();

  ReportRecord getReport();

  boolean isChanged();

  void setAmount(Double amount);

  void setApproval(String approval);

  void setCategory(String category);

  void setCreated(Date created);

  void setDescription(String description);

  void setReasonDenied(String reasonDenied);

  void setReport(ReportRecord report);
}
