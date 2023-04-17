import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeworkTest {

    private homework.ExpressionParser expressionParser;
    private homework.AlgebraHandler algebraHandler;

    private homework.Unifier unifier;

    private homework.KnowledgeBase base;

    @BeforeEach
    void setUp() throws IOException {
        this.expressionParser = new homework.ExpressionParser(new homework.Tokeniser(), new homework.AlgebraHandler());
        this.algebraHandler = new homework.AlgebraHandler();
        this.unifier = new homework.Unifier();
        this.base = new homework.KnowledgeBase(homework.Configuration.load(null), expressionParser, algebraHandler, unifier);
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
        assertEquals("(A(x,y)|A(x,z))", expressionParser.cleanup((homework.Sentence) sentence.getExpressions().get(0)).toString());
        sentence = (homework.Sentence) getCNFSentence("A(x,y)|A(x,z)|~A(x,y)").getExpressions().get(0);
        assertNull(expressionParser.cleanup(sentence));
    }

    @Test
    void testGetSubstitutionWorksAsExpected() {
        Map<String, homework.Predicate.Argument> substitution;

        assertThrows(IllegalArgumentException.class, () -> unifier.getSubstitution(getPredicate("A", false, "x"), getPredicate("B", false, "x")));
        assertThrows(IllegalArgumentException.class, () -> unifier.getSubstitution(getPredicate("A", false, "x", "y"), getPredicate("A", false, "DiffConst")));

        substitution = unifier.getSubstitution(getPredicate("A", false, "x", "Const"), getPredicate("A", false, "y", "z"));
        assertEquals(2, substitution.size());
        assertEquals(getArgument("y"), substitution.get("x"));
        assertEquals(getArgument("Const"), substitution.get("z"));

        substitution = unifier.getSubstitution(getPredicate("A", false, "x"), getPredicate("A", false, "y"));
        assertEquals(1, substitution.size());
        assertEquals(getArgument("y"), substitution.get("x"));

        substitution = unifier.getSubstitution(getPredicate("A", false, "x"), getPredicate("A", false, "Const"));
        assertEquals(1, substitution.size());
        assertEquals(getArgument("Const"), substitution.get("x"));

        substitution = unifier.getSubstitution(getPredicate("A", false, "Const"), getPredicate("A", false, "DiffConst"));
        assertNull(substitution);

        substitution = unifier.getSubstitution(getPredicate("A", false, "x", "x"), getPredicate("A", false, "Const", "Const"));
        assertEquals(1, substitution.size());
        assertEquals(getArgument("Const"), substitution.get("x"));

        substitution = unifier.getSubstitution(getPredicate("A", false, "x", "y", "x"), getPredicate("A", false, "Const", "DiffConst", "DiffConst"));
        assertNull(substitution);

        substitution = unifier.getSubstitution(getPredicate("A", false, "x", "y", "z", "x"), getPredicate("A", false, "Const", "DiffConst", "AnotherConst", "Const"));
        assertEquals(3, substitution.size());
        assertEquals(getArgument("Const"), substitution.get("x"));
        assertEquals(getArgument("DiffConst"), substitution.get("y"));
        assertEquals(getArgument("AnotherConst"), substitution.get("z"));

        substitution = unifier.getSubstitution(getPredicate("A", false, "x", "y", "z", "x"), getPredicate("A", false, "Const", "DiffConst", "AnotherConst", "w"));
        assertNull(substitution);

        substitution = unifier.getSubstitution(getPredicate("A", false, "x", "y", "z", "x"), getPredicate("A", true, "w", "DiffConst", "AnotherConst", "w"));
        assertEquals(3, substitution.size());
        assertEquals(getArgument("w"), substitution.get("x"));
        assertEquals(homework.Predicate.ArgumentType.VARIABLE, substitution.get("x").getArgumentType());
        assertEquals(getArgument("DiffConst"), substitution.get("y"));
        assertEquals(getArgument("AnotherConst"), substitution.get("z"));
    }

    @Test
    void testApplySubstitutionWorksAsExpected() {
        homework.Predicate p = getPredicate("A", false, "x", "y", "z", "x");
        homework.Predicate q = getPredicate("A", true, "w", "DiffConst", "AnotherConst", "w");
        Map<String, homework.Predicate.Argument> substitution = unifier.getSubstitution(p, q);

        homework.Predicate unified = unifier.apply(p, substitution);
        assertEquals(getPredicate("A", false, "w", "DiffConst", "AnotherConst", "w"), unified);

        unified = unifier.apply(q, substitution);
        assertEquals(getPredicate("A", true, "w", "DiffConst", "AnotherConst", "w"), unified);
    }

    @Test
    void testResolveWorksAsExpected() {
        homework.Sentence a = getSentence(getPredicate("A", false, "w", "DiffConst", "AnotherConst", "w"));
        homework.Sentence b = getSentence(getPredicate("A", true, "w", "DiffConst", "AnotherConst", "w"));
        homework.Predicate predicate = getPredicate("A", false, "w", "DiffConst", "AnotherConst", "w");
        homework.Sentence resolved = base.resolve(a, b, predicate);
        assertNull(resolved);

        b = getSentence(
                getPredicate("A", true, "w", "DiffConst", "AnotherConst", "w"),
                homework.Operator.OR,
                getPredicate("B", true, "w", "DiffConst", "AnotherConst", "w")
        );
        resolved = base.resolve(a, b, predicate);
        System.out.println(resolved);
    }

    @Test
    void testGetSentenceKeyWorksAsExpected() {
        homework.Sentence a = getSentence(
                getPredicate("A", true, "x", "DiffConst", "AnotherConst", "y"),
                homework.Operator.OR,
                getPredicate("B", true, "z", "DiffConst", "AnotherConst", "z")
        );
        homework.Sentence b = getSentence(
                getPredicate("B", true, "c", "DiffConst", "AnotherConst", "c"),
                homework.Operator.OR,
                getPredicate("A", false, "a", "DiffConst", "AnotherConst", "b")
        );
        assertEquals(base.getKey(a), base.getKey(b));

        a = getSentence(
                getPredicate("A", true, "x", "DiffConst", "AnotherConst", "y"),
                homework.Operator.OR,
                getPredicate("B", true, "z", "DiffConst", "AnotherConst", "z")
        );
        b = getSentence(
                getPredicate("B", true, "c", "DiffConst", "AnotherConst", "c"),
                homework.Operator.OR,
                getPredicate("C", true, "a", "DiffConst", "AnotherConst", "b")
        );
        assertNotEquals(base.getKey(a), base.getKey(b));
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

    private homework.Predicate getPredicate(String name, boolean isNegated, String... argumentNames) {
        List<homework.Predicate.Argument> arguments = new ArrayList<>();
        for (String argumentName : argumentNames) {
            arguments.add(getArgument(argumentName));
        }
        return new homework.Predicate(name, arguments, isNegated);
    }

    private homework.Predicate.Argument getArgument(String name) {
        if (Character.isUpperCase(name.charAt(0))) {
            return new homework.Predicate.Constant(name);
        }
        return new homework.Predicate.Variable(name);
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