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
package com.igormaznitsa.jbbp.compiler.varlen;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.utils.JBBPCompilerUtils;
import java.util.List;

/**
 * The Factory generates a special evaluator which is appropriate for variable array size text.
 * It is a singleton and can't be created directly, only through the special getInstance method.
 */
public final class JBBPEvaluatorFactory {
  private static final JBBPEvaluatorFactory instance = new JBBPEvaluatorFactory();
  
  private JBBPEvaluatorFactory(){
    
  }
  
  /**
   * Get an Instance of the factory.
   * @return the factory instance.
   */
  public static JBBPEvaluatorFactory getInstance(){
    return instance;
  }
  
  /**
   * Make an appropriate evaluator for an expression text.
   * @param expression an expression text, must not be null
   * @param namedFields a named field list
   * @param compiledScript a compiled script block
   * @return a generated evaluator, it will not be null in any case
   * @see JBBPExpressionEvaluator
   * @see JBBPOnlyFieldEvaluator
   */
  public JBBPLengthEvaluator make(final String expression, final List<JBBPNamedFieldInfo> namedFields, final byte [] compiledScript){
    final JBBPLengthEvaluator result;
    
    if (JBBPExpressionEvaluator.hasExpressionOperators(expression)){
      // expression
      result = new JBBPExpressionEvaluator(expression, namedFields, compiledScript);
    }else{
      // only field
      final String externalFieldName;
      int index = -1;
      if (expression.startsWith("$")) {
        result = new JBBPOnlyFieldEvaluator(expression.substring(1), index);
      }else{
        externalFieldName = null;
        for (int i = namedFields.size() - 1; i >= 0; i--) {
          final JBBPNamedFieldInfo field = namedFields.get(i);
          if (expression.equals(field.getFieldPath())) {
            index = i;
            break;
          }
        }
        if (index<0){
          result = new JBBPExpressionEvaluator(expression, namedFields, compiledScript);
        }else{
          JBBPCompilerUtils.assertFieldIsNotArrayOrInArray(namedFields.get(index), namedFields, compiledScript);
          result = new JBBPOnlyFieldEvaluator(externalFieldName, index);
        }
      }
    }
    return result;
  }
  



}
