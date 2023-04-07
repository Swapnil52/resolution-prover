import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class homeworkTest {

    private homework.ExpressionParser expressionParser;
    private homework.AlgebraHandler algebraHandler;

    @BeforeEach
    void setUp() {
        this.expressionParser = new homework.ExpressionParser(new homework.Tokeniser(), new homework.AlgebraHandler());
        this.algebraHandler = new homework.AlgebraHandler();
    }

    @Test
    void testAndWorksAsExpectedForPureConjunction() {
        homework.Sentence a = (homework.Sentence) getExpression("A(x)&B(y)");
        homework.Predicate b = (homework.Predicate) getExpression("C(x)");

        assertEquals("(A(x)&B(y)&C(x))", algebraHandler.and(a, b).toString());
    }

    @Test
    void testAndWorksForDisjunctionAndConjunction() {
        homework.Sentence a = (homework.Sentence) getExpression("A(x)|B(y)");
        homework.Sentence b = (homework.Sentence) getExpression("C(x)&D(x)");
        homework.Sentence c = (homework.Sentence) algebraHandler.and(a, b);
        assertEquals("((A(x)|B(y))&C(x)&D(x))", algebraHandler.and(a, b).toString());
    }

    //
//    @Test
//    void testFlattenWorksAsExpectedForPureDisjunction() {
//        homework.Sentence a = getSentence("A(x)|B(y)");
//        homework.Sentence b = getSentence("C(x)|D(y)");
//        homework.Sentence c = getSentence(a, b, homework.Operator.OR);
//
//        assertEquals("(A(x)|B(y)|C(x)|D(y))", AlgebraHandler.flatten(c).toString());
//    }
//
//    @Test
//    void testFlattenWorksAsExpectedForImpureExpression() {
//        homework.Sentence a = getSentence("A(x)|B(y)");
//        homework.Sentence b = getSentence("C(x)|D(y)");
//        homework.Sentence c = getSentence(a, b, homework.Operator.AND);
//
//        assertEquals("((A(x)|B(y))&(C(x)|D(y)))", AlgebraHandler.flatten(c).toString());
//    }

//    @Test
//    void testAndWorksAsExpectedForPredicateAndSentence() {
//        homework.Sentence a = getSentence("A(x)|B(y)");
//        homework.Sentence b = getSentence("C(x)");
//        homework.Sentence c = AlgebraHandler.operate1(a, b, homework.Operator.AND);
//
//        System.out.println(c);
//    }
//
//    @Test
//    void testAndWorksAsExpectedForSentenceAndSentence() {
//        homework.Sentence a = getSentence("A(x)|B(y)");
//        homework.Sentence b = getSentence("C(x)&D(y)");
//        homework.Sentence c = AlgebraHandler.operate1(a, b, homework.Operator.AND);
//
//        System.out.println(c);
//    }

    @Test
    void testGroupWorksAsExpected() {
        homework.Sentence sentence = (homework.Sentence) getExpression("A(x)|B(y)&D(z)|C(x)=>E(x)");
        System.out.println(sentence);
    }

    @Test
    void testNegateWorksAsExpected() {
        homework.Sentence sentence = (homework.Sentence) getExpression("A(x)|B(y)&D(z)|C(x)");
        System.out.println(algebraHandler.negate(sentence));
    }

    private homework.Predicate getPredicate(String name) {
        return new homework.Predicate(name, Collections.singletonList("x"), false);
    }

    private homework.Expression getExpression(String line) {
        return expressionParser.fromString(line);
    }

    private homework.Sentence getExpression(homework.Sentence a, homework.Sentence b, homework.Operator operator) {
        return new homework.Sentence(Arrays.asList(a, operator, b));
    }
}