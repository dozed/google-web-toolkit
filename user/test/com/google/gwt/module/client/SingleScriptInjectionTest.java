/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.module.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests waiting on a single external script.
 * 
 * @see com.google.gwt.module.client.DoubleScriptInjectionTest
 */
public class SingleScriptInjectionTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.module.SingleScriptInjectionTest";
  }

  /**
   * Coordinates with external ScriptInjectionTest1 JavaScript file in the
   * public folder, which uses a timer to delay its readiness indicator. This
   * proves that the test truly won't run until the script-ready function
   * defined in the module is satisfied.
   */
  public void testWaitForScript() {
    String answer = isScriptOneReady();
    assertEquals("yes1", answer);
  }

  /**
   * The native method called here is defined in SingleScriptInjectionTest1.
   */
  public static native String isScriptOneReady() /*-{
   return $wnd.isScriptOneReady();
   }-*/;
}
