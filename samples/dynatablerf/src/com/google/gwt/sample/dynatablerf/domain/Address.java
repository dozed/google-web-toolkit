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
package com.google.gwt.sample.dynatablerf.domain;

import com.google.gwt.sample.dynatablerf.server.SchoolCalendarService;

/**
 * Represents an address.
 */
public class Address {
  /**
   * The RequestFactory requires a static finder method for each proxied type.
   * Soon it should allow you to customize how instances are found.
   */
  public static Address findAddress(Long id) {
    return SchoolCalendarService.findPerson(id).getAddress();
  }

  private String city;
  private Long id;
  private String state;
  private String street;
  private Integer version = 0;
  private Integer zip;

  public String getCity() {
    return city;
  }

  public Long getId() {
    return id;
  }

  public String getState() {
    return state;
  }

  public String getStreet() {
    return street;
  }

  public Integer getVersion() {
    return version;
  }

  public Integer getZip() {
    return zip;
  }

  /**
   * When this was written the RequestFactory required a persist method per
   * type. That requirement should be relaxed very soon (and may well have been
   * already if we forget to update this comment).
   */
  public void persist() {
    SchoolCalendarService.persist(this);
  }

  public void setCity(String city) {
    this.city = city;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setState(String state) {
    this.state = state;
  }

  public void setStreet(String street) {
    this.street = street;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public void setZip(Integer zip) {
    this.zip = zip;
  }
}
