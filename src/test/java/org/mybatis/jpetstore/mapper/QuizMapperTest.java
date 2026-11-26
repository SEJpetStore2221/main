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
package org.mybatis.jpetstore.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.jpetstore.domain.Quiz;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MapperTestContext.class)
@Transactional
class QuizMapperTest {

  @Autowired
  private QuizMapper mapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void getQuizListByProduct() {
    List<Quiz> quizList = mapper.getQuizListByProduct("FI-FW-02");

    assertThat(quizList).hasSize(10);
    Quiz quiz = quizList.get(0);
    assertThat(quiz.getProductId()).isEqualTo("FI-FW-02");
    assertThat(quiz.getProductName()).isEqualTo("Goldfish");
    assertThat(quiz.getQuestion()).isEqualTo("잘 키운 금붕어의 수명은?");
    assertThat(quiz.getChoices()).hasSize(4).contains("10년 이상").doesNotContainNull();
    assertThat(quiz.getChoices().get(quiz.getAnswer() - 1)).isEqualTo("10년 이상");
    assertThat(quiz.getExplanation()).isNotBlank();
  }

  @Test
  void everyProductHasTenQuestionsWithValidAnswers() {
    List<String> productIds = jdbcTemplate.queryForList("SELECT productid FROM product", String.class);

    assertThat(productIds).hasSize(16);
    for (String productId : productIds) {
      List<Quiz> quizList = mapper.getQuizListByProduct(productId);
      assertThat(quizList).as(productId).hasSize(10);
      for (Quiz quiz : quizList) {
        assertThat(quiz.getAnswer()).as("quiz %d", quiz.getQuizId()).isBetween(1, 4);
        assertThat(quiz.getChoices()).as("quiz %d", quiz.getQuizId()).doesNotHaveDuplicates();
      }
    }
  }

}
