import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class HomeworkTestRunner {

    public static void main(String[] args) throws IOException {
        runAllTests();
//        runTestCase(5);
    }

    private static void runAllTests() throws IOException {
        for (int i = 1; i <= 10; i++) {
            if (i == 5) {
                continue;
            }
            runTestCase(i);
        }
    }

    private static void runTestCase(int testCaseNumber) throws IOException {
        System.out.printf("------------Running test case %d------------%n", testCaseNumber);
        String inputPath = getInputPath(testCaseNumber);
        homework.Configuration configuration = homework.Configuration.load(inputPath);
        homework.Tokeniser tokeniser = new homework.Tokeniser();
        homework.AlgebraHandler handler = new homework.AlgebraHandler();
        homework.ExpressionParser parser = new homework.ExpressionParser(tokeniser, handler);
        homework.Unifier unifier = new homework.Unifier();
        homework.KnowledgeBase base = new homework.KnowledgeBase(configuration, parser, handler, unifier);
        for (homework.Sentence disjunction : base.getDisjunctions()) {
            System.out.println(disjunction);
        }
        boolean result = base.proveLogged();
        String resultString = result ? "TRUE" : "FALSE";
        if (resultString.equals(getExpectedOutput(testCaseNumber))) {
            System.out.println("TEST CASE PASSED!");
        }
        System.out.println(result);
        System.out.printf("------------Ending test case %d------------%n", testCaseNumber);
    }

    private static String getInputPath(int testCaseNumber) {
        return String.format("hw3_10_examples/test_case_%d/input.txt", testCaseNumber);
    }

    private static String getExpectedOutput(int testCaseNumber) throws IOException {
        String expectedOutputPath = String.format("hw3_10_examples/test_case_%d/output.txt", testCaseNumber);
        BufferedReader reader = new BufferedReader(new FileReader(expectedOutputPath));
        return reader.readLine();
    }
}