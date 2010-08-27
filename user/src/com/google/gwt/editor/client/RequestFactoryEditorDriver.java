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
package com.google.gwt.editor.client;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.shared.Record;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestObject;

/**
 * The interface that links RequestFactory and the Editor framework together.
 * Used for configuration and lifecycle control. Expected that this will be
 * created with
 * 
 * <pre>
 * interface MyRFED extends RequestFactoryEditorDelegate&lt;MyRequestFactory, MyObjectProxy> {}
 * MyRFED instance = GWT.create(MyRFED.class);
 * {
 * instance.initialize(.....);
 * myRequestObject.with(instance.getPaths());
 * 
 * // Fire the request, in the callback 
 * instance.edit(retrievedRecord);
 * // Control when the request is sent
 * instance.flush().fire(new Receiver {...});
 * }
 * </pre>
 * 
 * @param <F> the type of RequestFactory that creates <code>R</code>
 * @param <R> the type of Record being edited
 */
public interface RequestFactoryEditorDriver<F extends RequestFactory, R extends Record> {
  void edit(R record, RequestObject<?> saveRequest);

  /**
   * Ensures that the Editor passed into {@link #initialize} and its
   * sub-editors, if any, have synced their UI state by invoking
   * {@link ValueAwareEditor#flush} in a depth-first manner.
   * 
   * @return the RequestObject passed into {@link #edit}
   */
  <T> RequestObject<T> flush();

  String[] getPaths();

  /**
   * In order to support object subscriptions, the EventBus passed into the
   * RequestFactory should be provided.
   */
  void initialize(EventBus eventBus, F requestFactory, Editor<R> editor);
}
