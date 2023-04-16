import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeworkTest {

    private homework.ExpressionParser expressionParser;
    private homework.AlgebraHandler algebraHandler;

    @BeforeEach
    void setUp() {
        this.expressionParser = new homework.ExpressionParser(new homework.Tokeniser(), new homework.AlgebraHandler());
        this.algebraHandler = new homework.AlgebraHandler();
    }

    @Test
    void testPredicateEqualityWorksAsExpected() {
        homework.Predicate a = new homework.Predicate("Pred123", Collections.singletonList(new homework.Predicate.Variable("x")), false);
        homework.Predicate b = new homework.Predicate("Pred123", Collections.singletonList(new homework.Predicate.Variable("x")), false);
        assertEquals(a, b);

        a = new homework.Predicate("Pred123", Collections.singletonList(new homework.Predicate.Variable("y")), false);
        b = new homework.Predicate("Pred123", Collections.singletonList(new homework.Predicate.Variable("x")), false);
        assertNotEquals(a, b);
    }

    @Test
    void tesParseWorksAsExpected() {
        homework.Sentence sentence = (homework.Sentence) expressionParser.fromString("A(x)|B(y)&D(z)|C(x)=>E(x)");
        assertEquals("(((A(x)|(B(y)&D(z)))|C(x))=>E(x))", sentence.toString());
    }

    @Test
    void testFlattenWorksAsExpected() {
        assertEquals("((Pred1234(x)|B(x))&(C(x)|D(x)))", algebraHandler.flatten(getSentence(getOperable("Pred1234(x)|B(x)"), homework.Operator.AND, getOperable("C(x)|D(x)"))).toString());
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

    @Test
    void testToCNFWorksAsExpected() {
        assertEquals("(A(x))", expressionParser.toCNF(getPredicate("A")).toString());
        assertEquals("((A(x)|B(x)))", expressionParser.toCNF(getSentence("A(x)|B(x)")).toString());
        assertEquals("(A(x)&B(x)&C(x))", expressionParser.toCNF(getSentence("A(x)&B(x)&C(x)")).toString());
        assertEquals("((A(x)|B(x))&(A(x)|C(x)))", expressionParser.toCNF(getSentence("A(x)|B(x)&C(x)")).toString());
        assertEquals("((~A(x)|C(x))&(~B(x)|C(x)))", expressionParser.toCNF(getSentence(getSentence("~A(x)&~B(x)"), homework.Operator.OR, getPredicate("C"))).toString());
        assertEquals("((~A(x)|C(x))&(~B(x)|C(x)))", expressionParser.toCNF(getSentence("A(x)|B(x)=>C(x)")).toString());
        assertEquals("((~A(x)|~B(x)|D(x))&(~C(x)|D(x)))", expressionParser.toCNF(getSentence("A(x)&B(x)|C(x)=>D(x)")).toString());
        assertEquals("((~A(x)|~B(x)|D(x)|E(x))&(~C(x)|D(x)|E(x)))", expressionParser.toCNF(getSentence("A(x)&B(x)|C(x)=>D(x)|E(x)")).toString());
        assertEquals("((~A(x)|~B(x)|D(x))&(~A(x)|~B(x)|E(x))&(~C(x)|D(x))&(~C(x)|E(x)))", expressionParser.toCNF(getSentence("A(x)&B(x)|C(x)=>D(x)&E(x)")).toString());
    }

    @Test
    void testIsCNFWorksAsExpected() {
        assertFalse(algebraHandler.isCNF(homework.Operator.IMPLIES));
        assertFalse(algebraHandler.isCNF(homework.Operator.OR));
        assertTrue(algebraHandler.isCNF(homework.Operator.AND));
        assertTrue(algebraHandler.isCNF(getPredicate("A")));
        assertTrue(algebraHandler.isCNF(getSentence("A(x)|B(x)|C(x)")));
        assertTrue(algebraHandler.isCNF(getSentence(getSentence(getOperable("A(x)|B(x)"), homework.Operator.AND, getOperable("C(x)|D(x)")))));
        assertTrue(algebraHandler.isCNF(getSentence(getSentence(getOperable("A(x)&B(x)"), homework.Operator.AND, getOperable("C(x)&D(x)")))));
        assertFalse(algebraHandler.isCNF(getSentence("A(x)=>D(x)")));
        assertFalse(algebraHandler.isCNF(getSentence(getSentence(getOperable("A(x)&B(x)"), homework.Operator.OR, getOperable("C(x)&D(x)")))));
        assertFalse(algebraHandler.isCNF(getSentence("A(x)&B(x)|C(x)=>D(x)")));
    }

    @Test
    void testCleanupWorksAsExpected() {
        homework.Sentence sentence = getCNFSentence("A(x,y)|A(x,z)|A(x,y)");
        assertEquals("(A(x,y)|A(x,z))", expressionParser.cleanup((homework.Sentence) sentence.getExpressions().get(0), 0).toString());
        sentence = (homework.Sentence) getCNFSentence("A(x,y)|A(x,z)|~A(x,y)").getExpressions().get(0);
        assertNull(expressionParser.cleanup(sentence, 0));
    }

    @Test
    void testRandom() {
        homework.Sentence sentence = getCNFSentence("Order(x,y)=>Seated(x)&Stocked(y)");
        System.out.println(sentence);
    }

    private homework.Sentence getCNFSentence(String line) {
        return expressionParser.toCNF(getSentence(line));
    }

    private homework.Predicate getPredicate(String name) {
        return new homework.Predicate(name, Collections.singletonList(new homework.Predicate.Variable("x")), false);
    }

    private homework.Predicate getPredicate(String name, boolean isNegated, String... variables) {
        List<homework.Predicate.Argument> arguments = Arrays.stream(variables)
                .map(homework.Predicate.Variable::new)
                .collect(Collectors.toList());
        return new homework.Predicate(name, arguments, isNegated);
    }

    private homework.Sentence getSentence(String line) {
        return (homework.Sentence) expressionParser.fromString(line);
    }

    private homework.Operand getOperable(String line) {
        return (homework.Operand) expressionParser.fromString(line);
    }

    private homework.Sentence getSentence(homework.Expression... expressions) {
        List<homework.Expression> _expressions = new ArrayList<>(Arrays.asList(expressions));
        return new homework.Sentence(_expressions);
    }
}