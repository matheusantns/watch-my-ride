package watchmyride;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

public class PassedTestLogger implements TestExecutionListener {

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest() && testExecutionResult.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
            System.out.println("PASSED " + testIdentifier.getDisplayName());
        }
    }
}
