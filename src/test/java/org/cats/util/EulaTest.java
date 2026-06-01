package org.cats.util;

import org.cats.io.UserInput;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EulaTest {
    @Test
    void acceptEulaReturnsTrueForEnglishYes() {
        Eula eula = new Eula(new TestInput("yes"));

        assertTrue(eula.acceptEula());
    }

    @Test
    void acceptEulaReturnsFalseForEnglishNo() {
        Eula eula = new Eula(new TestInput("no"));

        assertFalse(eula.acceptEula());
    }

    @Test
    void acceptEulaRepeatsPromptUntilAnswerIsValid() {
        Eula eula = new Eula(new TestInput("maybe", "y"));

        assertTrue(eula.acceptEula());
    }

    private static final class TestInput implements UserInput {
        private final Queue<String> answers = new ArrayDeque<>();

        private TestInput(String... answers) {
            this.answers.addAll(java.util.List.of(answers));
        }

        @Override
        public String readLine() {
            return answers.remove();
        }
    }
}
