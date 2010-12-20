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
package com.google.gwt.sample.validationtck.messageinterpolation;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Wraps
 * {@link org.hibernate.jsr303.tck.tests.messageinterpolation.MessageInterpolationTest}
 * .
 */
public class MessageInterpolationTest extends GWTTestCase {
  private final org.hibernate.jsr303.tck.tests.messageinterpolation.MessageInterpolationTest delegate =
    new org.hibernate.jsr303.tck.tests.messageinterpolation.MessageInterpolationTest();

  @Override
  public String getModuleName() {
    return "com.google.gwt.sample.validationtck.messageinterpolation.TckTest";
  }

  public void testConstraintAttributeValuesAreInterpolated() {
    delegate.testConstraintAttributeValuesAreInterpolated();
  }

  public void testDefaultMessageInterpolatorIsNotNull() {
    delegate.testDefaultMessageInterpolatorIsNotNull();
  }

  public void testIfNoLocaleIsSpecifiedTheDefaultLocaleIsAssumed() {
    delegate.testIfNoLocaleIsSpecifiedTheDefaultLocaleIsAssumed();
  }

  public void testLiteralCurlyBraces() {
    delegate.testLiteralCurlyBraces();
  }

  public void testMessageInterpolationWithLocale() {
    delegate.testMessageInterpolationWithLocale();
  }

  public void testMessagesCanBeOverriddenAtConstraintLevel() {
    delegate.testMessagesCanBeOverriddenAtConstraintLevel();
  }

  public void testParametersAreExtractedFromBeanValidationProviderBundle() {
    delegate.testParametersAreExtractedFromBeanValidationProviderBundle();
  }

  public void testRecursiveMessageInterpolation() {
    delegate.testRecursiveMessageInterpolation();
  }

  public void testSuccessfulInterpolationOfValidationMessagesValue() {
    delegate.testSuccessfulInterpolationOfValidationMessagesValue();
  }

  public void testUnknownTokenInterpolation() {
    delegate.testUnknownTokenInterpolation();
  }

  public void testUnSuccessfulInterpolation() {
    delegate.testUnSuccessfulInterpolation();
  }

}
