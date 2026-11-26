/*
 *    Copyright 2010-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.jpetstore.domain;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * A pre-purchase quiz question about a product (animal). It has four choices and one answer (1~4).
 */
public class Quiz implements Serializable {

  private static final long serialVersionUID = 2794738572037915839L;

  private int quizId;
  private String productId;
  private String productName;
  private String question;
  private String choice1;
  private String choice2;
  private String choice3;
  private String choice4;
  private int answer;
  private String explanation;

  public int getQuizId() {
    return quizId;
  }

  public void setQuizId(int quizId) {
    this.quizId = quizId;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getQuestion() {
    return question;
  }

  public void setQuestion(String question) {
    this.question = question;
  }

  public String getChoice1() {
    return choice1;
  }

  public void setChoice1(String choice1) {
    this.choice1 = choice1;
  }

  public String getChoice2() {
    return choice2;
  }

  public void setChoice2(String choice2) {
    this.choice2 = choice2;
  }

  public String getChoice3() {
    return choice3;
  }

  public void setChoice3(String choice3) {
    this.choice3 = choice3;
  }

  public String getChoice4() {
    return choice4;
  }

  public void setChoice4(String choice4) {
    this.choice4 = choice4;
  }

  public int getAnswer() {
    return answer;
  }

  public void setAnswer(int answer) {
    this.answer = answer;
  }

  public String getExplanation() {
    return explanation;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  /**
   * Returns the four choices in display order (choice 1 is index 0).
   *
   * @return the choices
   */
  public List<String> getChoices() {
    return Arrays.asList(choice1, choice2, choice3, choice4);
  }

  /**
   * Checks a selected choice.
   *
   * @param choice
   *          the selected choice number (1~4)
   *
   * @return true if the choice is the answer
   */
  public boolean isCorrect(int choice) {
    return choice == answer;
  }

}
