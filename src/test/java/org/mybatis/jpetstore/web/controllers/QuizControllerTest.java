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
package org.mybatis.jpetstore.web.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mybatis.jpetstore.domain.Cart;
import org.mybatis.jpetstore.domain.Item;
import org.mybatis.jpetstore.domain.Product;
import org.mybatis.jpetstore.domain.Quiz;
import org.mybatis.jpetstore.service.QuizService;
import org.mybatis.jpetstore.web.controllers.QuizController.ChoiceState;
import org.mybatis.jpetstore.web.controllers.QuizController.QuizQuestion;
import org.mybatis.jpetstore.web.controllers.QuizController.QuizSession;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

@ExtendWith(MockitoExtension.class)
class QuizControllerTest {

  @Mock
  private QuizService quizService;

  @InjectMocks
  private QuizController quizController;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  @Test
  void viewQuizWithEmptyCartRedirectsToCart() {
    session.setAttribute("cart", new Cart());

    String view = quizController.viewQuiz(session, new ExtendedModelMap());

    assertThat(view).isEqualTo("redirect:/cart");
  }

  @Test
  void viewQuizCreatesQuizForAnimalsInCart() {
    session.setAttribute("cart", cartWith("FI-FW-02"));
    when(quizService.createQuiz(List.of("FI-FW-02"))).thenReturn(quizList("FI-FW-02", 3));
    Model model = new ExtendedModelMap();

    String view = quizController.viewQuiz(session, model);

    assertThat(view).isEqualTo("quiz/Quiz");
    QuizSession quizSession = (QuizSession) model.asMap().get(QuizController.QUIZ_SESSION);
    assertThat(quizSession.getQuestions()).hasSize(3);
    assertThat(quizSession.isPassed()).isFalse();
    assertThat(QuizController.isPassed(session)).isFalse();
  }

  @Test
  void viewQuizKeepsQuizWhileCartAnimalsAreTheSame() {
    session.setAttribute("cart", cartWith("FI-FW-02"));
    when(quizService.createQuiz(anyCollection())).thenReturn(quizList("FI-FW-02", 3));

    quizController.viewQuiz(session, new ExtendedModelMap());
    quizController.viewQuiz(session, new ExtendedModelMap());

    verify(quizService, times(1)).createQuiz(anyCollection());
  }

  @Test
  void viewQuizCreatesNewQuizWhenCartAnimalsChange() {
    Cart cart = cartWith("FI-FW-02");
    session.setAttribute("cart", cart);
    when(quizService.createQuiz(anyCollection())).thenReturn(quizList("FI-FW-02", 3));
    quizController.viewQuiz(session, new ExtendedModelMap());

    cart.addItem(item("K9-BD-01"), true);
    quizController.viewQuiz(session, new ExtendedModelMap());

    verify(quizService, times(2)).createQuiz(anyCollection());
  }

  @Test
  void wrongChoiceIsCrossedOutAndAnswerIsHiddenUntilSolved() {
    session.setAttribute("cart", cartWith("FI-FW-02"));
    when(quizService.createQuiz(anyCollection())).thenReturn(quizList("FI-FW-02", 1));
    quizController.viewQuiz(session, new ExtendedModelMap());

    // answer is 2 (see quizList)
    String view = quizController.answer(0, 1, session);

    assertThat(view).isEqualTo("redirect:/quiz#q1");
    QuizQuestion question = quizSession().getQuestions().get(0);
    assertThat(question.isSolved()).isFalse();
    assertThat(question.getWrongCount()).isEqualTo(1);
    assertThat(question.getExplanation()).isNull();
    assertThat(question.getChoiceStates()).extracting(ChoiceState::getStatus).containsExactly(ChoiceState.WRONG,
        ChoiceState.OPEN, ChoiceState.OPEN, ChoiceState.OPEN);
  }

  @Test
  void crossedOutChoiceCannotBeChosenAgain() {
    session.setAttribute("cart", cartWith("FI-FW-02"));
    when(quizService.createQuiz(anyCollection())).thenReturn(quizList("FI-FW-02", 1));
    quizController.viewQuiz(session, new ExtendedModelMap());

    quizController.answer(0, 1, session);
    quizController.answer(0, 1, session);

    assertThat(quizSession().getQuestions().get(0).getWrongCount()).isEqualTo(1);
  }

  @Test
  void correctChoiceSolvesQuestionAndShowsExplanation() {
    session.setAttribute("cart", cartWith("FI-FW-02"));
    when(quizService.createQuiz(anyCollection())).thenReturn(quizList("FI-FW-02", 1));
    quizController.viewQuiz(session, new ExtendedModelMap());

    quizController.answer(0, 3, session);
    quizController.answer(0, 2, session);

    QuizQuestion question = quizSession().getQuestions().get(0);
    assertThat(question.isSolved()).isTrue();
    assertThat(question.getWrongCount()).isEqualTo(1);
    assertThat(question.getExplanation()).isEqualTo("explanation 1");
    assertThat(question.getChoiceStates()).extracting(ChoiceState::getStatus).containsExactly(ChoiceState.CLOSED,
        ChoiceState.CORRECT, ChoiceState.WRONG, ChoiceState.CLOSED);
  }

  @Test
  void quizIsPassedOnlyWhenEveryQuestionIsSolved() {
    session.setAttribute("cart", cartWith("FI-FW-02"));
    when(quizService.createQuiz(anyCollection())).thenReturn(quizList("FI-FW-02", 3));
    quizController.viewQuiz(session, new ExtendedModelMap());

    quizController.answer(0, 2, session);
    quizController.answer(1, 2, session);
    assertThat(QuizController.isPassed(session)).isFalse();

    quizController.answer(2, 2, session);
    assertThat(QuizController.isPassed(session)).isTrue();
  }

  @Test
  void passedQuizNoLongerCountsWhenCartAnimalsChange() {
    Cart cart = cartWith("FI-FW-02");
    session.setAttribute("cart", cart);
    when(quizService.createQuiz(anyCollection())).thenReturn(quizList("FI-FW-02", 1));
    quizController.viewQuiz(session, new ExtendedModelMap());
    quizController.answer(0, 2, session);
    assertThat(QuizController.isPassed(session)).isTrue();

    cart.addItem(item("K9-BD-01"), true);

    assertThat(QuizController.isPassed(session)).isFalse();
  }

  @Test
  void answerWithoutQuizRedirectsToQuiz() {
    session.setAttribute("cart", cartWith("FI-FW-02"));

    String view = quizController.answer(0, 1, session);

    assertThat(view).isEqualTo("redirect:/quiz");
  }

  private QuizSession quizSession() {
    return (QuizSession) session.getAttribute(QuizController.QUIZ_SESSION);
  }

  private static Cart cartWith(String productId) {
    Cart cart = new Cart();
    cart.addItem(item(productId), true);
    return cart;
  }

  private static Item item(String productId) {
    Product product = new Product();
    product.setProductId(productId);
    Item item = new Item();
    item.setItemId("ITEM-" + productId);
    item.setProduct(product);
    return item;
  }

  /** Builds quiz questions whose answer is always choice 2. */
  private static List<Quiz> quizList(String productId, int count) {
    List<Quiz> quizList = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      Quiz quiz = new Quiz();
      quiz.setQuizId(i);
      quiz.setProductId(productId);
      quiz.setProductName("Goldfish");
      quiz.setQuestion("question " + i);
      quiz.setChoice1("a");
      quiz.setChoice2("b");
      quiz.setChoice3("c");
      quiz.setChoice4("d");
      quiz.setAnswer(2);
      quiz.setExplanation("explanation " + i);
      quizList.add(quiz);
    }
    return quizList;
  }

}
