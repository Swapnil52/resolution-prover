import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

public class homework {

    private static final char OPEN_BRACE = '(';
    private static final char CLOSE_BRACE = ')';

    //each expression is a list of expressions
    public static void main(String[] args) {
        SentenceParser parser = new SentenceParser();

        String line = "~A(Akanksha, Potty) & B(Akanksha, Swapnil) | C(Mom, Dad) => D(Swapnil, Family)";
        Sentence sentence = parser.parse(line.replaceAll("\\s", ""));
        System.out.println(sentence);

        System.out.println(parser.toPostfix(sentence));
    }

    public static class SentenceParser {

        public Sentence parse(String line) {
            List<Expression> expressions = new ArrayList<>();
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
            return new Sentence(expressions);
        }

        public Sentence toPostfix(Sentence sentence) {
            Stack<Operator> stack = new Stack<>();
            List<Expression> postfix = new ArrayList<>();
            for (Expression expression : sentence.getExpressions()) {
                switch (expression.getType()) {
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
            return new Sentence(postfix);
        }

        private int parseOperator(String sentence, List<Expression> expressions, int currentIndex) {
            Operator operator = Operator.from(sentence.charAt(currentIndex));
            if (operator == Operator.NOT) {
                currentIndex += operator.getLength();
                return parsePredicate(sentence, expressions, currentIndex, true);
            }
            expressions.add(operator);
            return currentIndex + operator.getLength();
        }

        private int parsePredicate(String sentence, List<Expression> expressions, int currentIndex, boolean negated) {
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

    public interface Expression {

        ExpressionType getType();
    }

    public static class Predicate implements Expression {

        private final String name;

        private final List<String> arguments;

        private boolean negated;

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

        public void setNegated(boolean negated) {
            this.negated = negated;
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

    public static class Sentence implements Expression {

        private final List<Expression> expressions;

        public Sentence(List<Expression> expressions) {
            this.expressions = expressions;
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
            for (Expression expression : expressions) {
                builder.append(expression.toString());
            }
            return builder.toString();
        }
    }

    public enum ExpressionType {

        PREDICATE,
        OPERATOR,
        SENTENCE;
    }

    public enum Operator implements Expression {

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

        @Override
        public String toString() {
            return getLabel();
        }
    }
}
