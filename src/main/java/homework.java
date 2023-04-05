import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

public class homework {

    public static void main(String[] args) {

    }

    public static class ExpressionParser {


    }

    public interface Expression {

        ExpressionType getType();

        int getPrecedence();
    }

    public static abstract class UnaryExpression implements Expression {

        protected final Expression expression;

        public UnaryExpression(Expression expression) {
            this.expression = expression;
        }
    }

    public static abstract class BinaryExpression implements Expression {

        protected final Expression first;

        protected final Expression second;

        public BinaryExpression(Expression first, Expression second) {
            this.first = first;
            this.second = second;
        }

        public Expression getFirst() {
            return first;
        }

        public Expression getSecond() {
            return second;
        }

        @Override
        public String toString() {
            return String.format("(%s %s %s)", first.toString(), getType().getOperator(), second.toString());
        }
    }

    public static class Predicate extends UnaryExpression {

        private final String name;

        private final List<String> arguments;

        public Predicate(String name, List<String> arguments) {
            super(null);
            this.name = name;
            this.arguments = arguments;
        }

        public Predicate(String name, String... arguments) {
            super(null);
            this.name = name;
            this.arguments = new ArrayList<>();
            this.arguments.addAll(Arrays.asList(arguments));
        }

        @Override
        public ExpressionType getType() {
            return ExpressionType.PREDICATE;
        }

        @Override
        public int getPrecedence() {
            return -1;
        }

        @Override
        public String toString() {
            return String.format("%s(%s)", name, String.join(",", arguments));
        }
    }

    public static class Negation extends UnaryExpression {

        public static Negation of(Expression expression) {
            return new Negation(expression);
        }

        private Negation(Expression expression) {
            super(expression);
        }

        @Override
        public ExpressionType getType() {
            return ExpressionType.NEGATION;
        }

        @Override
        public int getPrecedence() {
            return 0;
        }

        @Override
        public String toString() {
            return String.format("~%s", expression.toString());
        }
    }

    public static class Conjunction extends BinaryExpression {

        public static Conjunction of(Expression first, Expression second) {
            return new Conjunction(first, second);
        }

        private Conjunction(Expression first, Expression second) {
            super(first, second);
        }

        @Override
        public ExpressionType getType() {
            return ExpressionType.CONJUNCTION;
        }

        @Override
        public int getPrecedence() {
            return 1;
        }
    }

    public static class Disjunction extends BinaryExpression {

        public static Disjunction of(Expression first, Expression second) {
            return new Disjunction(first, second);
        }

        private Disjunction(Expression first, Expression second) {
            super(first, second);
        }

        @Override
        public ExpressionType getType() {
            return ExpressionType.DISJUNCTION;
        }

        @Override
        public int getPrecedence() {
            return 2;
        }
    }

    public static class Implication extends BinaryExpression {

        public static Implication of(Expression first, Expression second) {
            return new Implication(first, second);
        }

        private Implication(Expression first, Expression second) {
            super(first, second);
        }

        @Override
        public ExpressionType getType() {
            return ExpressionType.IMPLICATION;
        }

        @Override
        public int getPrecedence() {
            return 3;
        }
    }

    public enum ExpressionType {

        PREDICATE(null),
        NEGATION("~"),
        CONJUNCTION("&"),
        DISJUNCTION("|"),
        IMPLICATION("=>");

        private final String operator;

        private static final Map<String, ExpressionType> labels = Arrays.stream(values())
                .collect(Collectors.toMap(ExpressionType::getOperator, Function.identity()));

        public static ExpressionType from(String label) {
            if (!labels.containsKey(label)) {
                throw new IllegalArgumentException(String.format("Label %s must belong to one of %s", label, Arrays.toString(values())));
            }
            return labels.get(label);
        }

        ExpressionType(String operator) {
            this.operator = operator;
        }

        public String getOperator() {
            return operator;
        }
    }

    public enum OperatorType {

        NOT("~"),
        AND("&"),
        OR("|"),
        IMPLIES("=>");

        private static final Map<String, OperatorType> labelsMap = Arrays.stream(values())
                .collect(Collectors.toMap(OperatorType::getLabel, Function.identity()));

        private final String label;

        public static OperatorType from(String label) {
            if (!labelsMap.containsKey(label)) {
                throw new IllegalArgumentException(String.format("Label must be one of %s", Arrays.toString(values())));
            }
            return labelsMap.get(label);
        }

        OperatorType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
