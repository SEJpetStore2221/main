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
package org.mybatis.jpetstore.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.mybatis.jpetstore.domain.Quiz;
import org.mybatis.jpetstore.mapper.QuizMapper;
import org.springframework.stereotype.Service;

/**
 * Picks the pre-purchase quiz questions for the animals in a cart.
 */
@Service
public class QuizService {

  /** Number of questions a buyer has to answer before checkout. */
  public static final int QUESTIONS_PER_QUIZ = 3;

  private final QuizMapper quizMapper;

  public QuizService(QuizMapper quizMapper) {
    this.quizMapper = quizMapper;
  }

  public List<Quiz> getQuizListByProduct(String productId) {
    return quizMapper.getQuizListByProduct(productId);
  }

  /**
   * Picks up to {@link #QUESTIONS_PER_QUIZ} random questions for the given products. When the cart has several animals,
   * the questions are spread over them in turn so that every animal gets asked about.
   *
   * @param productIds
   *          the product ids in the cart
   *
   * @return the questions (empty if none of the products has a quiz)
   */
  public List<Quiz> createQuiz(Collection<String> productIds) {
    List<Deque<Quiz>> pools = new ArrayList<>();
    for (String productId : productIds) {
      List<Quiz> quizList = new ArrayList<>(quizMapper.getQuizListByProduct(productId));
      if (!quizList.isEmpty()) {
        Collections.shuffle(quizList);
        pools.add(new ArrayDeque<>(quizList));
      }
    }
    Collections.shuffle(pools);

    List<Quiz> picked = new ArrayList<>();
    boolean added = true;
    while (picked.size() < QUESTIONS_PER_QUIZ && added) {
      added = false;
      for (Deque<Quiz> pool : pools) {
        if (picked.size() < QUESTIONS_PER_QUIZ && !pool.isEmpty()) {
          picked.add(pool.poll());
          added = true;
        }
      }
    }
    return picked;
  }

}
