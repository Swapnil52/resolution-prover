import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeworkTest {

    private homework.ExpressionParser expressionParser;
    private homework.AlgebraHandler algebraHandler;

    @BeforeEach
    void setUp() {
        this.expressionParser = new homework.ExpressionParser(new homework.Tokeniser(), new homework.AlgebraHandler());
        this.algebraHandler = new homework.AlgebraHandler();
    }

    @Test
    void tesParseWorksAsExpected() {
        homework.Sentence sentence = (homework.Sentence) expressionParser.fromString("A(x)|B(y)&D(z)|C(x)=>E(x)");
        assertEquals("(((A(x)|(B(y)&D(z)))|C(x))=>E(x))", sentence.toString());
    }

    @Test
    void testFlattenWorksAsExpected() {
        assertEquals("((A(x)|B(x))&(C(x)|D(x)))", algebraHandler.flatten(getSentence(getOperable("A(x)|B(x)"), homework.Operator.AND, getOperable("C(x)|D(x)"))).toString());
        assertEquals("((A(x)|B(x)))", algebraHandler.flatten(getSentence("A(x)|B(x)")).toString());
        assertEquals("((A(x)|B(x))&C(x))", algebraHandler.flatten(new homework.Sentence(getSentence("A(x)|B(x)"), homework.Operator.AND, getPredicate("C"))).toString());
        assertEquals("(A(x))", algebraHandler.flatten(getPredicate("A")).toString());
    }

    @Test
    void testAndWorksAsExpected() {
        assertEquals("(A(x)&B(x))", algebraHandler.and(getOperable("A(x)"), getOperable("B(x)")).toString());
        assertEquals("((A(x)|B(x))&(C(x)|D(x)))", algebraHandler.and(getOperable("A(x)|B(x)"), getOperable("C(x)|D(x)")).toString());
        assertEquals("(A(x)&B(x)&C(x)&D(x)&E(x))", algebraHandler.and(getOperable("A(x)&B(x)&C(x)&D(x)"), getOperable("E(x)")).toString());
        assertEquals("((A(x)|B(x)|C(x)|D(x))&E(x))", algebraHandler.and(getOperable("A(x)|B(x)|C(x)|D(x)"), getOperable("E(x)")).toString());
    }

    @Test
    void testOrWorksAsExpected() {
        assertEquals("((A(x)|B(x)))", algebraHandler.or(getOperable("A(x)"), getOperable("B(x)")).toString());
        assertEquals("((A(x)|D(x))&(B(x)|D(x)))", algebraHandler.or(getOperable("A(x)&B(x)"), getOperable("D(x)")).toString());
        assertEquals("((A(x)|D(x))&(A(x)|E(x))&(B(x)|D(x))&(B(x)|E(x))&(C(x)|D(x))&(C(x)|E(x)))", algebraHandler.or(getOperable("A(x)&B(x)&C(x)"), getOperable("D(x)&E(x)")).toString());
        assertEquals("(((A(x)|B(x)|C(x))|D(x))&((A(x)|B(x)|C(x))|E(x)))", algebraHandler.or(getOperable("A(x)|B(x)|C(x)"), getOperable("D(x)&E(x)")).toString());
    }

    @Test
    void testNegateWorksAsExpected() {
        assertEquals("(~A(x)|~B(x))", algebraHandler.negate(getSentence("A(x)&B(x)")).toString());
        assertEquals("(A(x)&B(x))", algebraHandler.negate(getSentence("~A(x)|~B(x)")).toString());
        assertEquals("~A(x)", algebraHandler.negate(getPredicate("A")).toString());
        assertEquals("((~A(x)&~B(y))|(~C(x)&D(x)))", algebraHandler.negate(getSentence(getSentence("A(x)|B(y)"), homework.Operator.AND, getSentence("C(x)|~D(x)"))).toString());
    }

    private homework.Predicate getPredicate(String name) {
        return new homework.Predicate(name, Collections.singletonList("x"), false);
    }

    private homework.Sentence getSentence(String line) {
        return (homework.Sentence) expressionParser.fromString(line);
    }

    private homework.Operable getOperable(String line) {
        return (homework.Operable) expressionParser.fromString(line);
    }

    private homework.Sentence getSentence(homework.Expression... expressions) {
        List<homework.Expression> _expressions = new ArrayList<>(Arrays.asList(expressions));
        return new homework.Sentence(_expressions);
    }
}