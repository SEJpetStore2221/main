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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpSession;

import org.mybatis.jpetstore.domain.Cart;
import org.mybatis.jpetstore.domain.CartItem;
import org.mybatis.jpetstore.domain.Quiz;
import org.mybatis.jpetstore.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Pre-purchase quiz shown between the cart and checkout. A wrong choice is crossed out and cannot be picked again; the
 * buyer keeps choosing until the answer is found. Checkout opens only after every question is answered.
 * <p>
 * The answers stay in the session on the server; the page only receives the state of each choice.
 */
@Controller
@RequestMapping("/quiz")
public class QuizController {

  /** Session attribute holding the quiz in progress. */
  public static final String QUIZ_SESSION = "quizSession";

  private static final String QUIZ_VIEW = "quiz/Quiz";

  @Autowired
  private QuizService quizService;

  /**
   * Shows the quiz for the animals in the cart, creating it on first visit or when the animals in the cart changed.
   *
   * @param session
   *          the session
   * @param model
   *          the model
   *
   * @return the string
   */
  @GetMapping({ "", "/" })
  public String viewQuiz(HttpSession session, Model model) {
    Cart cart = (Cart) session.getAttribute("cart");
    if (cart == null || cart.getNumberOfItems() == 0) {
      return "redirect:/cart";
    }
    QuizSession quizSession = getCurrentQuizSession(session, cart);
    if (quizSession == null) {
      List<String> productIds = getProductIds(cart);
      quizSession = new QuizSession(productIds, quizService.createQuiz(productIds));
      session.setAttribute(QUIZ_SESSION, quizSession);
    }
    model.addAttribute(QUIZ_SESSION, quizSession);
    return QUIZ_VIEW;
  }

  /**
   * Checks one choice of one question.
   *
   * @param index
   *          the question index (0-based)
   * @param choice
   *          the selected choice (1~4)
   * @param session
   *          the session
   *
   * @return the string
   */
  @PostMapping("/answer")
  public String answer(@RequestParam("index") int index, @RequestParam("choice") int choice, HttpSession session) {
    Cart cart = (Cart) session.getAttribute("cart");
    QuizSession quizSession = cart == null ? null : getCurrentQuizSession(session, cart);
    if (quizSession == null || index < 0 || index >= quizSession.getQuestions().size()) {
      return "redirect:/quiz";
    }
    quizSession.getQuestions().get(index).choose(choice);
    return "redirect:/quiz#q" + (index + 1);
  }

  /**
   * Whether the buyer has answered every question for the animals currently in the cart.
   *
   * @param session
   *          the session
   *
   * @return true if checkout is allowed
   */
  public static boolean isPassed(HttpSession session) {
    Cart cart = (Cart) session.getAttribute("cart");
    if (cart == null) {
      return false;
    }
    QuizSession quizSession = getCurrentQuizSession(session, cart);
    return quizSession != null && quizSession.isPassed();
  }

  private static QuizSession getCurrentQuizSession(HttpSession session, Cart cart) {
    QuizSession quizSession = (QuizSession) session.getAttribute(QUIZ_SESSION);
    if (quizSession != null && quizSession.getProductIds().equals(getProductIds(cart))) {
      return quizSession;
    }
    return null;
  }

  private static List<String> getProductIds(Cart cart) {
    Set<String> productIds = new TreeSet<>();
    Iterator<CartItem> cartItems = cart.getCartItems();
    while (cartItems.hasNext()) {
      productIds.add(cartItems.next().getItem().getProduct().getProductId());
    }
    return new ArrayList<>(productIds);
  }

  /**
   * The quiz in progress, kept in the session.
   */
  public static class QuizSession implements Serializable {

    private static final long serialVersionUID = -4123067409126738512L;

    private final List<String> productIds;
    private final List<QuizQuestion> questions = new ArrayList<>();

    public QuizSession(List<String> productIds, List<Quiz> quizList) {
      this.productIds = new ArrayList<>(productIds);
      for (Quiz quiz : quizList) {
        questions.add(new QuizQuestion(quiz));
      }
    }

    public List<String> getProductIds() {
      return Collections.unmodifiableList(productIds);
    }

    public List<QuizQuestion> getQuestions() {
      return Collections.unmodifiableList(questions);
    }

    public int getSolvedCount() {
      int count = 0;
      for (QuizQuestion question : questions) {
        if (question.isSolved()) {
          count++;
        }
      }
      return count;
    }

    public boolean isPassed() {
      return getSolvedCount() == questions.size();
    }
  }

  /**
   * One question and the choices the buyer has tried.
   */
  public static class QuizQuestion implements Serializable {

    private static final long serialVersionUID = 6310845203496187726L;

    private final Quiz quiz;
    private final Set<Integer> wrongChoices = new LinkedHashSet<>();
    private boolean solved;

    public QuizQuestion(Quiz quiz) {
      this.quiz = quiz;
    }

    /**
     * Checks a choice. Choices already crossed out and questions already solved are ignored.
     *
     * @param choice
     *          the choice (1~4)
     *
     * @return true if the question is solved
     */
    public boolean choose(int choice) {
      if (solved || choice < 1 || choice > 4 || wrongChoices.contains(choice)) {
        return solved;
      }
      if (quiz.isCorrect(choice)) {
        solved = true;
      } else {
        wrongChoices.add(choice);
      }
      return solved;
    }

    public boolean isSolved() {
      return solved;
    }

    public int getWrongCount() {
      return wrongChoices.size();
    }

    public String getProductName() {
      return quiz.getProductName();
    }

    public String getQuestion() {
      return quiz.getQuestion();
    }

    /**
     * The explanation, shown only after the question is solved.
     *
     * @return the explanation, or null while unsolved
     */
    public String getExplanation() {
      return solved ? quiz.getExplanation() : null;
    }

    /**
     * The choices with their state, for the page. The answer is marked only once solved.
     *
     * @return the choice states
     */
    public List<ChoiceState> getChoiceStates() {
      List<ChoiceState> states = new ArrayList<>();
      List<String> choices = quiz.getChoices();
      for (int i = 0; i < choices.size(); i++) {
        int number = i + 1;
        String status;
        if (wrongChoices.contains(number)) {
          status = ChoiceState.WRONG;
        } else if (!solved) {
          status = ChoiceState.OPEN;
        } else if (quiz.isCorrect(number)) {
          status = ChoiceState.CORRECT;
        } else {
          status = ChoiceState.CLOSED;
        }
        states.add(new ChoiceState(number, choices.get(i), status));
      }
      return states;
    }
  }

  /**
   * A choice as shown on the page.
   */
  public static class ChoiceState implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Can still be chosen. */
    public static final String OPEN = "OPEN";
    /** Chosen and wrong: crossed out. */
    public static final String WRONG = "WRONG";
    /** The answer (shown once solved). */
    public static final String CORRECT = "CORRECT";
    /** Not chosen; the question is already solved. */
    public static final String CLOSED = "CLOSED";

    private final int number;
    private final String text;
    private final String status;

    public ChoiceState(int number, String text, String status) {
      this.number = number;
      this.text = text;
      this.status = status;
    }

    public int getNumber() {
      return number;
    }

    public String getText() {
      return text;
    }

    public String getStatus() {
      return status;
    }
  }

}
