import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

public class homework {

    public static void main(String[] args) throws IOException {
        Configuration configuration = Configuration.load();
        Tokeniser tokeniser = new Tokeniser();
        AlgebraHandler handler = new AlgebraHandler();
        ExpressionParser parser = new ExpressionParser(tokeniser, handler);
        KnowledgeBase base = new KnowledgeBase(configuration, parser, handler);
        System.out.println(base);
    }

    public static class Constants {

        public static final String INPUT_PATH = "input.txt";
    }

    public static class Configuration {

        private final String query;

        private final int size;

        private final List<String> facts;

        public static Configuration load() throws IOException {
            String query;
            int size;
            List<String> facts = new ArrayList<>();
            File file = new File(Constants.INPUT_PATH);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            query = reader.readLine();
            size = Integer.parseInt(reader.readLine());
            for (int i = 0; i < size; i++) {
                facts.add(reader.readLine());
            }
            reader.close();
            return new Configuration(query, size, facts);
        }

        private Configuration(String query, int size, List<String> facts) {
            this.size = size;
            this.query = query;
            this.facts = facts;
        }

        public String getQuery() {
            return query;
        }

        public int getSize() {
            return size;
        }

        public List<String> getFacts() {
            return facts;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("%s\n", query));
            builder.append(String.format("%d\n", size));
            for (String fact : facts) {
                builder.append(String.format("%s\n", fact));
            }
            return builder.toString();
        }
    }

    public static class KnowledgeBase {

        private final Configuration configuration;

        private final ExpressionParser parser;

        private final AlgebraHandler handler;

        private final Map<String, Sentence> positives;

        private final Map<String, Sentence> negatives;

        private final List<Sentence> disjunctions;

        private int size;

        public KnowledgeBase(Configuration configuration, ExpressionParser parser, AlgebraHandler handler) {
            this.configuration = configuration;
            this.parser = parser;
            this.handler = handler;
            this.disjunctions = new ArrayList<>();
            this.positives = new HashMap<>();
            this.negatives = new HashMap<>();
            this.size = 0;
            this.initialise();
        }

        private void initialise() {
            index(getNegatedQuery());
            for (String fact : configuration.getFacts()) {
                List<Sentence> disjunctions = getDisjunctions(fact);
                for (Sentence disjunction : disjunctions) {
                    index(disjunction);
                }
            }
        }

        private void index(Sentence disjunction) {
            Sentence standardised = this.parser.standardise(disjunction, this.size);
            this.disjunctions.add(standardised);
            for (Expression expression : standardised.getExpressions()) {
                if (expression.getType() == ExpressionType.PREDICATE) {
                    Predicate predicate = (Predicate) expression;
                    String name = predicate.getName();
                    if (predicate.isNegated()) {
                        this.negatives.put(name, standardised);
                    }
                    else {
                        this.positives.put(name, standardised);
                    }
                }
            }
            this.size++;
        }

        private Sentence getNegatedQuery() {
            Predicate query = (Predicate) parser.fromString(configuration.getQuery());
            Predicate negation = handler.negatePredicate(query);
            return handler.flatten(negation);
        }

        private List<Sentence> getDisjunctions(String line) {
            Sentence cnf = parser.toCNF(line);
            return parser.splitAndCleanup(cnf);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (Sentence disjunction : disjunctions) {
                builder.append(String.format("%s\n", disjunction.toString()));
            }
            return builder.toString();
        }
    }

    public static class Unifier {

        public Map<String, Predicate.Argument> getSubstitution(Predicate p, Predicate q) {
            if (!p.getName().equals(q.getName())) {
                throw new IllegalArgumentException(String.format("Cannot unify predicates with different names %s and %s", p, q));
            }
            if (p.getArguments().size() != q.getArguments().size()) {
                throw new IllegalArgumentException(String.format("Cannot unify predicates with different number of arguments %s and %s", p, q));
            }
            try {
                Map<String, Predicate.Argument> substitution = new HashMap<>();
                for (int i = 0; i < p.getArguments().size(); i++) {
                    Predicate.Argument a = p.getArguments().get(i);
                    Predicate.Argument b = q.getArguments().get(i);
                    if (a.getArgumentType() == Predicate.ArgumentType.CONSTANT && b.getArgumentType() == Predicate.ArgumentType.CONSTANT) {
                        handleConstantSubstitution((Predicate.Constant) a, (Predicate.Constant) b);
                    }
                    else if (a.getArgumentType() == Predicate.ArgumentType.VARIABLE) {
                        handleVariableSubstitution((Predicate.Variable) a, b, substitution);
                    }
                    else if (b.getArgumentType() == Predicate.ArgumentType.VARIABLE) {
                        handleVariableSubstitution((Predicate.Variable) b, a, substitution);
                    }
                }
                return substitution;
            } catch (Exception ex) {
                return null;
            }
        }

        public void apply(Sentence sentence, Map<String, Predicate.Argument> substitution) {
            for (Expression expression : sentence.getExpressions()) {
                if (expression.getType() == ExpressionType.PREDICATE) {
                    Predicate predicate = (Predicate) expression;
                    apply(predicate, substitution);
                }
            }
        }

        void apply(Predicate predicate, Map<String, Predicate.Argument> substitution) {
            for (int i = 0; i < predicate.getArguments().size(); i++) {
                Predicate.Argument argument = predicate.arguments.get(i);
                if (argument.getArgumentType() == Predicate.ArgumentType.VARIABLE && substitution.containsKey(argument.getName())) {
                    predicate.getArguments().set(i, substitution.get(argument.getName()));
                }
            }
        }

        private void handleConstantSubstitution(Predicate.Constant a, Predicate.Constant b) {
            if (!a.getName().equals(b.getName())) {
                throw new IllegalArgumentException("Cannot unify constants with different values");
            }
        }

        private void handleVariableSubstitution(Predicate.Variable variable, Predicate.Argument argument, Map<String, Predicate.Argument> substitution) {
            Predicate.Argument existing = substitution.get(variable.getName());
            if (Objects.isNull(existing)) {
                substitution.put(variable.getName(), argument);
            } else if (!existing.equals(argument)) {
                throw new IllegalArgumentException(String.format("Cannot unify a variable %s with two different constants %s %s", variable.getName(), argument.getName(), substitution.get(variable.getName()).getName()));
            }
        }
    }

    public static class ExpressionParser {

        private final Tokeniser tokeniser;

        private final AlgebraHandler handler;

        public ExpressionParser(Tokeniser tokeniser, AlgebraHandler handler) {
            this.tokeniser = tokeniser;
            this.handler = handler;
        }

        public List<Sentence> splitAndCleanup(Sentence cnf) {
            List<Sentence> disjunctions = new ArrayList<>();
            for (Expression cnfExpression : cnf.getExpressions()) {
                if (cnfExpression.getType() == ExpressionType.OPERATOR) {
                    if (cnfExpression == Operator.OR) {
                        throw new UnsupportedOperationException(String.format("Sentence %s is not in CNF", cnf));
                    }
                    else {
                        continue;
                    }
                }
                if (cnfExpression.getType() == ExpressionType.PREDICATE) {
                    Predicate predicate = (Predicate) cnfExpression;
                    Sentence disjunction = new Sentence(predicate);
                    disjunctions.add(disjunction);
                }
                else if (cnfExpression.getType() == ExpressionType.SENTENCE) {
                    Sentence cleanup = cleanup((Sentence) cnfExpression);
                    if (Objects.nonNull(cleanup)) {
                        disjunctions.add(cleanup);
                    }
                }
            }
            return disjunctions;
        }

        public Sentence standardise(Sentence sentence, int index) {
            for (int i = 0; i < sentence.getExpressions().size(); i++) {
                Expression expression = sentence.getExpressions().get(i);
                if (expression.getType() == ExpressionType.SENTENCE) {
                    throw new IllegalArgumentException(String.format("Sentence %s is not a pure disjunction", sentence));
                }
                else if (expression.getType() == ExpressionType.PREDICATE) {
                    sentence.getExpressions().set(i, standardise((Predicate) expression, index));
                }
            }
            return sentence;
        }

        public Sentence toCNF(String line) {
            List<Atom> atoms = tokeniser.tokenise(line.replaceAll("\\s", ""));
            Operand operand = (Operand) fromAtoms(atoms);
            return toCNF(operand);
        }

        Expression fromString(String line) {
            List<Atom> atoms = tokeniser.tokenise(line.replaceAll("\\s", ""));
            return fromAtoms(atoms);
        }

        Sentence toCNF(Operand operand) {
            if (handler.isCNF(operand)) {
                return handler.flatten(operand);
            }
            Sentence sentence = (Sentence) operand;
            List<Expression> postfixExpressions = toPostfix(sentence.getExpressions());
            Stack<Expression> stack = new Stack<>();
            for (Expression postfix : postfixExpressions) {
                switch (postfix.getType()) {
                    case PREDICATE:
                    case SENTENCE:
                        stack.add(postfix);
                        break;
                    case OPERATOR:
                        Operand second = (Operand) stack.pop();
                        Operand first = (Operand) stack.pop();
                        Operator operator = (Operator) postfix;
                        Expression operated = handleOperator(first, second, operator);
                        stack.add(operated);
                        break;
                }
            }
            return handler.flatten((Operand) stack.pop());
        }

        public Sentence cleanup(Sentence disjunction) {
            Map<String, Predicate> predicates = new HashMap<>();
            for (Expression expression : disjunction.getExpressions()) {
                if (expression == Operator.AND) {
                    throw new IllegalArgumentException("Disjunction should not contain any ANDs");
                }
                else if (expression.getType() == ExpressionType.SENTENCE) {
                    throw new IllegalArgumentException("Sentence must be a pure disjunction");
                }
                else if (expression.getType() == ExpressionType.PREDICATE) {
                    Predicate predicate = (Predicate) expression;
                    String key = predicate.getKey();
                    Predicate existing = predicates.get(key);
                    if (Objects.nonNull(existing) && existing.isNegation(predicate)) {
                        return null;
                    }
                    else {
                        predicates.put(key, predicate);
                    }
                }
            }
            List<Expression> expressions = new ArrayList<>();
            Iterator<Predicate> iterator = predicates.values().iterator();
            expressions.add(iterator.next());
            while (iterator.hasNext()) {
                Predicate predicate = iterator.next();
                expressions.add(Operator.OR);
                expressions.add(predicate);
            }
            return new Sentence(expressions);
        }

        private Predicate standardise(Predicate predicate, int index) {
            List<Predicate.Argument> standardisedArguments = new ArrayList<>();
            for (Predicate.Argument argument : predicate.getArguments()) {
                if (argument.getArgumentType() == Predicate.ArgumentType.CONSTANT) {
                    standardisedArguments.add(argument);
                }
                else {
                    String standardisedName = String.format("%s%d", argument.getName(), index);
                    standardisedArguments.add(new Predicate.Variable(standardisedName));
                }
            }
            return new Predicate(predicate.getName(), standardisedArguments, predicate.isNegated());
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
        private Expression handleOperator(Operand first, Operand second, Operator operator) {
            switch (operator) {
                case AND:
                    return handleAnd(first, second);
                case OR:
                    return handleOr(first, second);
                case IMPLIES:
                    return handleImplication(first, second);
                default:
                    throw new UnsupportedOperationException(String.format("Cannot apply operator %s on operables %s and %s", operator, first, second));
            }
        }

        private Expression handleAnd(Operand first, Operand second) {
            Operand cnfFirst = toCNF(first);
            Operand cnfSecond = toCNF(second);
            return handler.and(cnfFirst, cnfSecond);
        }

        private Expression handleOr(Operand first, Operand second) {
            Operand cnfFirst = toCNF(first);
            Operand cnfSecond = toCNF(second);
            return handler.or(cnfFirst, cnfSecond);
        }

        private Expression handleImplication(Operand first, Operand second) {
            Expression negated = handler.negate(first);
            return handleOr((Operand) negated, second);
        }
    }

    public static class Tokeniser {

        private static final char OPEN_BRACE = '(';
        private static final char CLOSE_BRACE = ')';

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
            List<String> argumentNames;

            /* Parse predicate name */
            i = currentIndex;
            j = sentence.indexOf(OPEN_BRACE, i);
            name = sentence.substring(i, j);

            /* Parse argument names */
            i = j + 1;
            j = sentence.indexOf(CLOSE_BRACE, i);
            argumentNames = Arrays.asList(sentence.substring(i, j).split(","));

            /* Add predicate to the expressions list */
            Predicate predicate = new Predicate(name, parseArguments(argumentNames), negated);
            expressions.add(predicate);

            return j + 1;
        }

        private List<Predicate.Argument> parseArguments(List<String> names) {
            return names.stream()
                    .map(this::parseArgument)
                    .collect(Collectors.toList());
        }

        private Predicate.Argument parseArgument(String name) {
            char first = name.charAt(0);
            if (Character.isUpperCase(first)) {
                return new Predicate.Constant(name);
            }
            return new Predicate.Variable(name);
        }
    }

    public static class AlgebraHandler {

        boolean isCNF(Expression expression) {
            if (expression.getType() == ExpressionType.OPERATOR && expression != Operator.AND) {
                return false;
            }
            if (expression == Operator.AND) {
                return true;
            }
            if (expression.getType() == ExpressionType.PREDICATE) {
                return true;
            }
            Sentence sentence = (Sentence) expression;
            Sentence pureDisjunction = (Sentence) getPureDisjunction(sentence);
            if (Objects.nonNull(pureDisjunction)) {
                return true;
            }
            boolean check = true;
            for (Expression sentenceExpression : sentence.getExpressions()) {
                check = check && isCNF(sentenceExpression);
            }
            return check;
        }

        /**
         * Conjuncts two operables in CNF form and returns the result in CNF form
         */
        public Expression and(Operand first, Operand second) {
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
        public Expression or(Operand first, Operand second) {
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
         * Negates a sentence in CNF and returns it in CNF form
         */
        public Expression negate(Operand operand) {
            return negateHelper(operand);
        }

        /**
         * Flattens a CNF operable into a single sentence
         */
        public Sentence flatten(Operand operand) {
            if (operand.getType() == ExpressionType.PREDICATE) {
                return new Sentence(operand);
            }
            Sentence sentence = (Sentence) operand;
            Sentence pureDisjunction = (Sentence) getPureDisjunction(sentence);
            if (Objects.nonNull(pureDisjunction)) {
                return new Sentence(pureDisjunction);
            }
            return (Sentence) flattenHelper(operand).getExpression();
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

        private Operand getPureDisjunction(Sentence operable) {
            try {
                return getPureOperable(operable, Operator.OR);
            }
            catch (Exception ex) {
                return null;
            }
        }

        private Operand getPureOperable(Operand operand, Operator operator) {
            if (operand.getType() == ExpressionType.PREDICATE) {
                return operand;
            }
            List<Expression> pureExpressions = new ArrayList<>();
            Sentence sentence = (Sentence) operand;
            for (Expression expression : sentence.getExpressions()) {
                if (expression == operator) {
                    pureExpressions.add(expression);
                    continue;
                }
                else if (expression == operator.complement()) {
                    throw new IllegalStateException(String.format("Not a pure %s expression", operator));
                }
                Operand pure = getPureOperable((Operand) expression, operator);
                if (pure.getType() == ExpressionType.PREDICATE) {
                    pureExpressions.add(pure);
                }
                else {
                    pureExpressions.addAll(((Sentence) pure).getExpressions());
                }
            }
            return new Sentence(pureExpressions);
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

        private Expression negateHelper(Operand operand) {
            ExpressionType type = operand.getType();
            switch (type) {
                case PREDICATE:
                    return negatePredicate((Predicate) operand);
                case SENTENCE:
                    return negateSentence((Sentence) operand);
                default:
                    throw new IllegalArgumentException(String.format("Expression of type %s cannot be negated", operand.getType()));
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
                    negatedExpressions.add(negateHelper((Operand) _expression));
                }
            }
            return new Sentence(negatedExpressions);
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
    }

    public interface Expression {

        ExpressionType getType();
    }

    public interface Operand extends Expression {

    }

    public interface Atom extends Expression {

    }

    public static class Predicate implements Atom, Operand {

        private final String name;

        private final List<Argument> arguments;

        private final boolean negated;

        public Predicate(String name, List<Argument> arguments, boolean negated) {
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

        public List<Argument> getArguments() {
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
            builder.append(getKey());
            return builder.toString();
        }

        public String getKey() {
            return String.format("%s(%s)", name, arguments.stream().map(Argument::toString).collect(Collectors.joining(",")));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Predicate predicate = (Predicate) o;
            return name.equals(predicate.name) && arguments.equals(predicate.arguments);
        }

        public boolean isNegation(Predicate other) {
            return equals(other) && negated == !other.isNegated();
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, arguments);
        }

        public static abstract class Argument {

            private final String name;

            private final ArgumentType argumentType;

            protected Argument(String name, ArgumentType argumentType) {
                this.name = name;
                this.argumentType = argumentType;
            }

            public String getName() {
                return name;
            }

            public ArgumentType getArgumentType() {
                return argumentType;
            }

            @Override
            public String toString() {
                return getName();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Argument argument = (Argument) o;
                return name.equals(argument.name) && argumentType == argument.argumentType;
            }

            @Override
            public int hashCode() {
                return Objects.hash(name, argumentType);
            }
        }

        public static class Variable extends Argument {
            public Variable(String name) {
                super(name, ArgumentType.VARIABLE);
            }
        }

        public static class Constant extends Argument {

            public Constant(String name) {
                super(name, ArgumentType.CONSTANT);
            }
        }

        public enum ArgumentType {

            CONSTANT,
            VARIABLE
        }
    }

    public static class Sentence implements Operand {

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
