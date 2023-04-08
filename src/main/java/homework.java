import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

public class homework {

    private static final char OPEN_BRACE = '(';
    private static final char CLOSE_BRACE = ')';

    //each expression is a list of expressions
    public static void main(String[] args) {

    }

    public static class ExpressionParser {

        private final Tokeniser tokeniser;

        private final AlgebraHandler handler;

        public ExpressionParser(Tokeniser tokeniser, AlgebraHandler handler) {
            this.tokeniser = tokeniser;
            this.handler = handler;
        }

        public Expression fromString(String line) {
            List<Atom> atoms = tokeniser.tokenise(line);
            return fromAtoms(atoms);
        }

        /**
         * Sentence will be nested at most once
         */
        public Expression toCNF(Expression expression) {
           if (expression.getType() == ExpressionType.OPERATOR) {
               return expression;
           }
           if (expression.getType() == ExpressionType.PREDICATE) {
               return expression;
           }
           Sentence sentence = (Sentence) expression;
           List<Expression> postfixExpressions = toPostfix(sentence.getExpressions());
           Stack<Expression> stack = new Stack<>();
            for (Expression postfix : postfixExpressions) {
                switch (postfix.getType()) {
                    case PREDICATE:
                    case SENTENCE:
                        stack.add(postfix);
                        break;
                    case OPERATOR:
                        Operable second = (Operable) stack.pop();
                        Operable first = (Operable) stack.pop();
                        Operator operator = (Operator) postfix;
                        Expression operated = handleOperator(first, second, operator);
                        stack.add(handleOperator(first, second, operator));
                        break;
                }
            }
            return stack.pop();
        }
        
        /**
         * Generates a sentence from a list of atoms
         */
        private Expression fromAtoms(List<Atom> atoms) {
            List<Expression> expressions = new ArrayList<>(atoms);
            Stack<Expression> stack = new Stack<>();
            List<Expression> postfix = toPostfix(expressions);
            for (Expression expression : postfix) {
                switch (expression.getType()) {
                    case PREDICATE:
                        stack.add(expression);
                        break;
                    case OPERATOR:
                        Operator operator = (Operator) expression;
                        Expression first = stack.pop();
                        Expression second = stack.pop();
                        Sentence next = new Sentence(Arrays.asList(second, operator, first));
                        stack.add(next);
                        break;
                    default:
                        throw new UnsupportedOperationException(String.format("Expression type %s is not an atom", expression.getType()));
                }
            }
            return stack.pop();
        }

        private List<Expression> toPostfix(List<Expression> expressions) {
            Stack<Operator> stack = new Stack<>();
            List<Expression> postfix = new ArrayList<>();
            for (Expression expression : expressions) {
                switch (expression.getType()) {
                    case SENTENCE:
                    case PREDICATE:
                        postfix.add(expression);
                        break;
                    case OPERATOR:
                        Operator operator = (Operator) expression;
                        while (!stack.isEmpty() && operator.getPrecedence() <= stack.peek().getPrecedence()) {
                            Operator next = stack.pop();
                            postfix.add(next);
                        }
                        stack.add(operator);
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("Expression cannot be of type %s", expression.getType()));
                }
            }
            while (!stack.isEmpty()) {
                Operator next = stack.pop();
                postfix.add(next);
            }
            return postfix;
        }

        /**
         * Operates on two expressions already in CNF
         */
        private Expression handleOperator(Operable first, Operable second, Operator operator) {
            switch (operator) {
                case AND:
                    /* Join the two sentences by a conjunction */
                    return handler.and(first, second);
                case OR:
                    /* Distribute */
                    return handler.or(first, second);
                case IMPLIES:
                    /*
                     * Negate first expression.
                     * Or with the second expression
                     * */
                    Expression negated = handler.negate(first);
                    Expression or = handler.or((Operable) negated, second);
                    return toCNF(or);
                default:
                    throw new UnsupportedOperationException(String.format("Cannot apply operator %s on operables %s and %s", operator, first, second));
            }
        }
    }

    public static class Tokeniser {

        public List<Atom> tokenise(String line) {
            List<Atom> expressions = new ArrayList<>();
            int i = 0;
            while (i < line.length()) {
                Character c = line.charAt(i);
                if (Operator.isOperator(c)) {
                    i = parseOperator(line, expressions, i);
                }
                else {
                    i = parsePredicate(line, expressions, i, false);
                }
            }
            return expressions;
        }

        private int parseOperator(String sentence, List<Atom> expressions, int currentIndex) {
            Operator operator = Operator.from(sentence.charAt(currentIndex));
            if (operator == Operator.NOT) {
                currentIndex += operator.getLength();
                return parsePredicate(sentence, expressions, currentIndex, true);
            }
            expressions.add(operator);
            return currentIndex + operator.getLength();
        }

        private int parsePredicate(String sentence, List<Atom> expressions, int currentIndex, boolean negated) {
            int i;
            int j;
            String name;
            List<String> arguments;

            /* Parse predicate name */
            i = currentIndex;
            j = sentence.indexOf(OPEN_BRACE, i);
            name = sentence.substring(i, j);

            /* Parse argument names */
            i = j + 1;
            j = sentence.indexOf(CLOSE_BRACE, i);
            arguments = Arrays.asList(sentence.substring(i, j).split(","));

            /* Add predicate to the expressions list */
            Predicate predicate = new Predicate(name, arguments, negated);
            expressions.add(predicate);

            return j + 1;
        }
    }

    public static class AlgebraHandler {

        /**
         * Conjuncts two operables in CNF form and returns the result in CNF form
         */
        public Expression and(Operable first, Operable second) {
            Sentence flattenedFirst = flatten(first);
            Sentence flattenedSecond = flatten(second);
            List<Expression> expressions = new ArrayList<>(flattenedFirst.getExpressions());
            expressions.add(Operator.AND);
            expressions.addAll(flattenedSecond.getExpressions());
            return new Sentence(expressions);
        }

        /**
         * Disjuncts two operables in CNF form and returns the result in CNF form
         */
        public Expression or(Operable first, Operable second) {
            Sentence flattenedFirst = flatten(first);
            Sentence flattenedSecond = flatten(second);
            List<Expression> expressions = new ArrayList<>();
            for (Expression a : flattenedFirst.getExpressions()) {
                if (a == Operator.AND) {
                    expressions.add(a);
                }
                else {
                    for (Expression b : flattenedSecond.getExpressions()) {
                        if (b == Operator.AND) {
                            expressions.add(b);
                        }
                        else {
                            Sentence disjunction = new Sentence(a, Operator.OR, b);
                            expressions.add(disjunction);
                        }
                    }
                }
            }
            return new Sentence(expressions);
        }

        /**
         * Flattens a CNF operable into a single sentence
         */
        Sentence flatten(Operable operable) {
            if (operable.getType() == ExpressionType.PREDICATE) {
                return new Sentence(operable);
            }
            Sentence sentence = (Sentence) operable;
            Sentence pureDisjunction = (Sentence) getPureDisjunction(sentence);
            if (Objects.nonNull(pureDisjunction)) {
                return new Sentence(pureDisjunction);
            }
            return (Sentence) flattenHelper(operable).getExpression();
        }

        private FlattenResult flattenHelper(Expression expression) {
            if (expression.getType() == ExpressionType.OPERATOR && expression != Operator.AND) {
                throw new UnsupportedOperationException(String.format("Cannot flatten operator to CNF %s", expression));
            }
            if (expression == Operator.AND) {
                return new FlattenResult(expression, false);
            }
            if (expression.getType() == ExpressionType.PREDICATE) {
                return new FlattenResult(expression, false);
            }
            Sentence sentence = (Sentence) expression;
            Sentence pureDisjunction = (Sentence) getPureDisjunction(sentence);
            if (Objects.nonNull(pureDisjunction)) {
                return new FlattenResult(pureDisjunction, true);
            }
            List<Expression> expressions = new ArrayList<>();
            for (Expression sentenceExpression : sentence.getExpressions()) {
                FlattenResult result = flattenHelper(sentenceExpression);
                addFlattenedExpression(expressions, result);
            }
            return new FlattenResult(new Sentence(expressions), false);
        }

        private Operable getPureDisjunction(Sentence operable) {
            try {
                return getPureOperable(operable, Operator.OR);
            }
            catch (Exception ex) {
                return null;
            }
        }

        private Operable getPureOperable(Operable operable, Operator operator) {
            if (operable.getType() == ExpressionType.PREDICATE) {
                return operable;
            }
            List<Expression> pureExpressions = new ArrayList<>();
            Sentence sentence = (Sentence) operable;
            for (Expression expression : sentence.getExpressions()) {
                if (expression == operator) {
                    pureExpressions.add(expression);
                    continue;
                }
                else if (expression == operator.complement()) {
                    throw new IllegalStateException(String.format("Not a pure %s expression", operator));
                }
                Operable pure = getPureOperable((Operable) expression, operator);
                if (pure.getType() == ExpressionType.PREDICATE) {
                    pureExpressions.add(pure);
                }
                else {
                    pureExpressions.addAll(((Sentence) pure).getExpressions());
                }
            }
            return new Sentence(pureExpressions);
        }

        private static class FlattenResult {

            private final Expression expression;

            private final boolean disjunction;

            public FlattenResult(Expression expression, Boolean disjunction) {
                this.expression = expression;
                this.disjunction = disjunction;
            }

            public Expression getExpression() {
                return expression;
            }

            public boolean isDisjunction() {
                return disjunction;
            }
        }

        private void addFlattenedExpression(List<Expression> expressions, FlattenResult flattenResult) {
            switch (flattenResult.getExpression().getType()) {
                case OPERATOR:
                case PREDICATE:
                    expressions.add(flattenResult.getExpression());
                    break;
                case SENTENCE:
                    Sentence sentence = (Sentence) flattenResult.getExpression();
                    if (flattenResult.isDisjunction()) {
                        expressions.add(sentence);
                    }
                    else {
                        expressions.addAll(sentence.getExpressions());
                    }
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("Expression of type %s not supported for flattening", flattenResult.getExpression()));
            }
        }

        /**
         * Negates a sentence in CNF and returns it in CNF form
         */
        public Expression negate(Operable operable) {
            return negateHelper(operable);
        }

        public Expression negateHelper(Operable operable) {
            ExpressionType type = operable.getType();
            switch (type) {
                case PREDICATE:
                    return negatePredicate((Predicate) operable);
                case SENTENCE:
                    return negateSentence((Sentence) operable);
                default:
                    throw new IllegalArgumentException(String.format("Expression of type %s cannot be negated", operable.getType()));
            }
        }

        private Predicate negatePredicate(Predicate predicate) {
            return new Predicate(predicate.getName(), predicate.getArguments(), !predicate.isNegated());
        }

        private Sentence negateSentence(Sentence sentence) {
            List<Expression> negatedExpressions = new ArrayList<>();
            for (Expression _expression : sentence.getExpressions()) {
                if (_expression.getType() == ExpressionType.OPERATOR) {
                    Operator operator = (Operator) _expression;
                    negatedExpressions.add(operator.complement());
                }
                else {
                    negatedExpressions.add(negateHelper((Operable) _expression));
                }
            }
            return new Sentence(negatedExpressions);
        }
    }

    public interface Expression {

        ExpressionType getType();
    }

    public interface Operable extends Expression {

    }

    public interface Atom extends Expression {

    }

    public static class Predicate implements Atom, Operable {

        private final String name;

        private final List<String> arguments;

        private final boolean negated;

        public Predicate(String name, List<String> arguments, boolean negated) {
            this.name = name;
            this.arguments = arguments;
            this.negated = negated;
        }

        @Override
        public ExpressionType getType() {
            return ExpressionType.PREDICATE;
        }

        public String getName() {
            return name;
        }

        public List<String> getArguments() {
            return arguments;
        }

        public boolean isNegated() {
            return negated;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (isNegated()) {
                builder.append(Operator.NOT.getPrefix());
            }
            builder.append(String.format("%s(%s)", name, String.join(",", arguments)));
            return builder.toString();
        }
    }

    public static class Sentence implements Operable {

        private final List<Expression> expressions;

        public Sentence(List<Expression> expressions) {
            this.expressions = expressions;
        }

        public Sentence(Expression... expressions) {
            this.expressions = new ArrayList<>(Arrays.asList(expressions));
        }

        @Override
        public ExpressionType getType() {
            return ExpressionType.SENTENCE;
        }

        public List<Expression> getExpressions() {
            return expressions;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("(");
            for (Expression expression : expressions) {
                builder.append(expression.toString());
            }
            builder.append(")");
            return builder.toString();
        }
    }

    public enum ExpressionType {

        PREDICATE,
        OPERATOR,
        SENTENCE
    }

    public enum Operator implements Atom {

        NOT('~', "~", 1, 3),
        AND('&', "&", 1, 2),
        OR('|', "|", 1, 1),
        IMPLIES('=', "=>", 2, 0);

        private static final Map<Character, Operator> prefixMap = Arrays.stream(values())
                .collect(Collectors.toMap(Operator::getPrefix, Function.identity()));

        private static final Set<Character> prefixes = Arrays.stream(values())
                .map(Operator::getPrefix)
                .collect(Collectors.toSet());

        private final char prefix;

        private final String label;

        private final int length;

        private final int precedence;

        public static Operator from(Character prefix) {
            if (!prefixMap.containsKey(prefix)) {
                throw new IllegalArgumentException(String.format("Prefix must be one of %s", Arrays.toString(values())));
            }
            return prefixMap.get(prefix);
        }

        Operator(char prefix, String label, int length, int precedence) {
            this.prefix = prefix;
            this.label = label;
            this.length = length;
            this.precedence = precedence;
        }

        public static boolean isOperator(Character c) {
            return prefixes.contains(c);
        }

        @Override
        public ExpressionType getType() {
            return ExpressionType.OPERATOR;
        }

        public char getPrefix() {
            return prefix;
        }

        public String getLabel() {
            return label;
        }

        public int getLength() {
            return length;
        }

        public int getPrecedence() {
            return precedence;
        }

        public Operator complement() {
            switch (this) {
                case OR:
                    return AND;
                case AND:
                    return OR;
                default:
                    throw new UnsupportedOperationException(String.format("Complement not supported for operator %s", this));
            }
        }

        @Override
        public String toString() {
            return getLabel();
        }
    }
}
