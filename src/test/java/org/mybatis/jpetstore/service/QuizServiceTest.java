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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mybatis.jpetstore.domain.Quiz;
import org.mybatis.jpetstore.mapper.QuizMapper;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

  @Mock
  private QuizMapper quizMapper;

  @InjectMocks
  private QuizService quizService;

  @Test
  void createQuizPicksThreeDifferentQuestionsForOneAnimal() {
    when(quizMapper.getQuizListByProduct("FI-FW-02")).thenReturn(quizList("FI-FW-02", 10));

    List<Quiz> quiz = quizService.createQuiz(List.of("FI-FW-02"));

    assertThat(quiz).hasSize(QuizService.QUESTIONS_PER_QUIZ);
    assertThat(quiz).extracting(Quiz::getQuizId).doesNotHaveDuplicates();
  }

  @Test
  void createQuizAsksAboutEveryAnimalInCart() {
    when(quizMapper.getQuizListByProduct("FI-FW-02")).thenReturn(quizList("FI-FW-02", 10));
    when(quizMapper.getQuizListByProduct("K9-BD-01")).thenReturn(quizList("K9-BD-01", 10));
    when(quizMapper.getQuizListByProduct("AV-SB-02")).thenReturn(quizList("AV-SB-02", 10));

    List<Quiz> quiz = quizService.createQuiz(List.of("AV-SB-02", "FI-FW-02", "K9-BD-01"));

    assertThat(quiz).hasSize(3);
    assertThat(quiz).extracting(Quiz::getProductId).containsExactlyInAnyOrder("AV-SB-02", "FI-FW-02", "K9-BD-01");
  }

  @Test
  void createQuizSkipsAnimalsWithoutQuestions() {
    when(quizMapper.getQuizListByProduct("FI-FW-02")).thenReturn(quizList("FI-FW-02", 10));
    when(quizMapper.getQuizListByProduct("NO-QUIZ")).thenReturn(List.of());

    List<Quiz> quiz = quizService.createQuiz(List.of("FI-FW-02", "NO-QUIZ"));

    assertThat(quiz).hasSize(3).extracting(Quiz::getProductId).containsOnly("FI-FW-02");
  }

  @Test
  void createQuizReturnsFewerQuestionsWhenNotEnoughExist() {
    when(quizMapper.getQuizListByProduct("FI-FW-02")).thenReturn(quizList("FI-FW-02", 2));

    assertThat(quizService.createQuiz(List.of("FI-FW-02"))).hasSize(2);
  }

  private static List<Quiz> quizList(String productId, int count) {
    List<Quiz> quizList = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      Quiz quiz = new Quiz();
      quiz.setQuizId(productId.hashCode() + i);
      quiz.setProductId(productId);
      quiz.setAnswer(1);
      quizList.add(quiz);
    }
    return quizList;
  }

}
