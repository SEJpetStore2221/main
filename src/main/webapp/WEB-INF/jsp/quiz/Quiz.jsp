<%--

       Copyright 2010-2026 the original author or authors.

       Licensed under the Apache License, Version 2.0 (the "License");
       you may not use this file except in compliance with the License.
       You may obtain a copy of the License at

          https://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing, software
       distributed under the License is distributed on an "AS IS" BASIS,
       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
       See the License for the specific language governing permissions and
       limitations under the License.

--%>
<%@ page pageEncoding="UTF-8"%>
<%@ include file="../common/IncludeTop.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<style>
    #Quiz { max-width: 720px; margin: 0 auto; text-align: left; }
    #Quiz h2 { text-align: center; }
    #Quiz .QuizIntro { color: #333; line-height: 1.6; }
    #Quiz .QuizProgress { font-weight: bold; color: #005e21; }
    #Quiz .QuizCard { border: 1px solid #ccc; background: #f7f7f7; padding: 1.5ex 2ex; margin: 2ex 0; }
    #Quiz .QuizCard.Solved { border-color: #54c07a; }
    #Quiz .QuizMeta { font-size: 1.6ex; color: #666; }
    #Quiz .QuizQuestion { font-size: 2.2ex; font-weight: bold; margin: 1ex 0 1.5ex; }
    #Quiz form { margin: 0; }
    #Quiz .QuizChoice {
        display: block; width: 100%; margin: .8ex 0; padding: 1ex 1.5ex; text-align: left;
        font-family: inherit; font-size: 1.9ex; color: #000; background: #fff;
        border: 1px solid #999; cursor: pointer;
    }
    #Quiz .QuizChoice:hover:enabled { background: #e6f5eb; border-color: #005e21; }
    #Quiz .QuizChoice:disabled { cursor: default; }
    #Quiz .QuizChoice.Wrong { color: #b00; background: #f3e6e6; border-color: #dbb; text-decoration: line-through; opacity: .7; }
    #Quiz .QuizChoice.Correct { color: #fff; background: #005e21; border-color: #005e21; font-weight: bold; }
    #Quiz .QuizChoice.Closed { color: #999; }
    #Quiz .QuizMark { display: inline-block; width: 2.5ex; font-weight: bold; text-decoration: none; }
    #Quiz .QuizRetry { color: #b00; margin: 1ex 0 0; }
    #Quiz .QuizExplanation { margin: 1.5ex 0 0; padding: 1ex 1.5ex; background: #e6f5eb; border-left: 4px solid #005e21; line-height: 1.6; }
    #Quiz .QuizNext { text-align: center; margin: 3ex 0 1ex; }
    #Quiz .QuizNext a.Button { font-size: 2ex; padding: 1ex 2ex; }
    #Quiz .QuizLocked { color: #666; }
</style>

<div id="BackLink"><a href="${pageContext.request.contextPath}/cart">Return to Shopping Cart</a></div>

<div id="Catalog">
    <div id="Quiz">

        <h2>구매 전 퀴즈</h2>
        <p class="QuizIntro">
            데려오기 전에 꼭 알아야 할 사육 지식을 확인해요.
            틀린 보기는 X 표시와 함께 지워지고, 정답을 고를 때까지 남은 보기에서 다시 고를 수 있어요.
            모든 문제를 맞히면 결제로 넘어갈 수 있습니다.
        </p>

        <c:choose>
            <c:when test="${empty quizSession.questions}">
                <p class="QuizProgress">장바구니에 담긴 동물에 대한 퀴즈가 아직 없어요.</p>
            </c:when>
            <c:otherwise>
                <p class="QuizProgress">맞힌 문제: ${quizSession.solvedCount} / ${fn:length(quizSession.questions)}</p>
            </c:otherwise>
        </c:choose>

        <c:forEach var="question" items="${quizSession.questions}" varStatus="status">
            <div id="q${status.count}" class="QuizCard${question.solved ? ' Solved' : ''}">
                <div class="QuizMeta">문제 ${status.count} · <c:out value="${question.productName}" /></div>
                <p class="QuizQuestion"><c:out value="${question.question}" /></p>

                <form method="post" action="${pageContext.request.contextPath}/quiz/answer">
                    <input type="hidden" name="index" value="${status.index}" />
                    <c:forEach var="choice" items="${question.choiceStates}">
                        <c:choose>
                            <c:when test="${choice.status == 'OPEN'}">
                                <button type="submit" class="QuizChoice" name="choice" value="${choice.number}">
                                    <span class="QuizMark">${choice.number}.</span><c:out value="${choice.text}" />
                                </button>
                            </c:when>
                            <c:when test="${choice.status == 'WRONG'}">
                                <button type="button" class="QuizChoice Wrong" disabled="disabled">
                                    <span class="QuizMark">✕</span><c:out value="${choice.text}" />
                                </button>
                            </c:when>
                            <c:when test="${choice.status == 'CORRECT'}">
                                <button type="button" class="QuizChoice Correct" disabled="disabled">
                                    <span class="QuizMark">✓</span><c:out value="${choice.text}" />
                                </button>
                            </c:when>
                            <c:otherwise>
                                <button type="button" class="QuizChoice Closed" disabled="disabled">
                                    <span class="QuizMark">${choice.number}.</span><c:out value="${choice.text}" />
                                </button>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>
                </form>

                <c:if test="${!question.solved && question.wrongCount > 0}">
                    <p class="QuizRetry">틀렸어요. 남은 보기 중에서 다시 골라 보세요.</p>
                </c:if>
                <c:if test="${question.solved}">
                    <div class="QuizExplanation">
                        <b>정답이에요!</b>
                        <c:if test="${question.wrongCount > 0}">(오답 ${question.wrongCount}번 후 정답)</c:if><br />
                        <c:out value="${question.explanation}" />
                    </div>
                </c:if>
            </div>
        </c:forEach>

        <div class="QuizNext">
            <c:choose>
                <c:when test="${quizSession.passed}">
                    <a class="Button" href="${pageContext.request.contextPath}/order/new">결제 계속하기</a>
                </c:when>
                <c:otherwise>
                    <p class="QuizLocked">모든 문제를 맞히면 결제 버튼이 열려요.</p>
                </c:otherwise>
            </c:choose>
        </div>

    </div>
</div>

<%@ include file="../common/IncludeBottom.jsp"%>
