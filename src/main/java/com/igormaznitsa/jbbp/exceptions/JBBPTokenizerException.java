/* 
 * Copyright 2014 Igor Maznitsa (http://www.igormaznitsa.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.jbbp.exceptions;

public class JBBPTokenizerException extends JBBPException {
  private static final long serialVersionUID = -1132154077305894146L;

  private final int position;

  public JBBPTokenizerException(String message, final int pos) {
    super(message);
    this.position = pos;
  }

  public int getPosition() {
    return this.position;
  }
}