package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodeRunEngineTest {
    private val javaBin: String = Path.of(System.getProperty("java.home"), "bin", if (windows) "java.exe" else "java").toString()
    private val windows: Boolean get() = System.getProperty("os.name").orEmpty().lowercase().contains("win")

    @AfterTest
    fun reset() {
        CodeRunEngine.resetForTests()
    }

    @Test
    fun parsesQuotedArgumentsWithoutAShell() {
        assertEquals(
            listOf("--name", "Moo Tool", "--count", "2"),
            CodeRunEngine.parseArguments("""--name "Moo Tool" --count 2""")
        )
        assertEquals(listOf("single value", "escaped value"), CodeRunEngine.parseArguments("""'single value' escaped\ value"""))
        assertFailsWith<CodeRunException> { CodeRunEngine.parseArguments("\"unfinished") }
        assertEquals("Main", CodeRunEngine.publicTypeName("class Demo {}"))
        assertEquals("App", CodeRunEngine.publicTypeName("public final class App {}"))
        assertEquals("Hello", CodeRunEngine.publicTypeName("public record Hello(int n) {}"))
        assertTrue(CodeRunEngine.formatSource("class Demo{public static void main(String[] args){}}", CodeRuntime.Java).contains("class Demo"))
        assertEquals("    print(\"moo\")", CodeRunEngine.formatSource("\tprint(\"moo\")  ", CodeRuntime.Python))
    }

    @Test
    fun javaPrintsFortyTwoWithArgumentsAndIsolatedDirectory() {
        val temp = Files.createTempDirectory("mootool-compose-runtime-")
        val cwdMarker = Files.createTempDirectory("mootool-compose-runtime-cwd-")
        try {
            val result = CodeRunEngine.run(
                CodeRunInput(
                    requestId = "java-test-001",
                    runtime = CodeRuntime.Java,
                    code = """
                        public class Main {
                            public static void main(String[] args) throws Exception {
                                System.out.println(42);
                                System.out.println(String.join("|", args));
                                System.out.println(new java.io.File(".").getCanonicalPath());
                            }
                        }
                    """.trimIndent(),
                    arguments = listOf("Moo Tool", "--flag"),
                    workingDirectory = cwdMarker.toString()
                ),
                CodeRunPaths(java = javaBin),
                temp
            )
            assertEquals(0, result.exitCode)
            assertFalse(result.cancelled)
            assertTrue(result.stdout.contains("42"))
            assertTrue(result.stdout.contains("Moo Tool|--flag"))
            assertTrue(result.stdout.contains(cwdMarker.fileName.toString()))
            assertTrue(result.command.contains("Main.java"))
        } finally {
            cwdMarker.toFile().deleteRecursively()
            temp.toFile().deleteRecursively()
        }
    }

    @Test
    fun cancelsTimeoutTruncatesAndRejectsBadInput() {
        val temp = Files.createTempDirectory("mootool-compose-runtime-")
        try {
            val invalid = CodeRunEngine.run(
                CodeRunInput(requestId = "../bad", runtime = CodeRuntime.Java, code = "class X {}"),
                CodeRunPaths(java = javaBin),
                temp
            )
            assertEquals(CodeRunErrorCode.INVALID_REQUEST, invalid.errorCode)

            val tooBig = CodeRunEngine.run(
                CodeRunInput(requestId = "java-big-01", runtime = CodeRuntime.Java, code = "x".repeat(CodeRunEngine.MAX_CODE_BYTES + 8)),
                CodeRunPaths(java = javaBin),
                temp
            )
            assertEquals(CodeRunErrorCode.INVALID_REQUEST, tooBig.errorCode)

            val hang = CodeRunEngine.run(
                CodeRunInput(
                    requestId = "java-timeout-1",
                    runtime = CodeRuntime.Java,
                    code = """
                        public class Main {
                            public static void main(String[] args) throws Exception {
                                Thread.sleep(8000);
                            }
                        }
                    """.trimIndent(),
                    timeoutMs = 1_000
                ),
                CodeRunPaths(java = javaBin),
                temp
            )
            assertTrue(hang.timedOut)
            assertEquals(CodeRunErrorCode.TIMEOUT, hang.errorCode)
            assertTrue(hang.durationMs < 5_000)

            val cancelId = "java-cancel-1"
            val future = java.util.concurrent.CompletableFuture<CodeRunResult>()
            Thread {
                future.complete(
                    CodeRunEngine.run(
                        CodeRunInput(
                            requestId = cancelId,
                            runtime = CodeRuntime.Java,
                            code = """
                                public class Main {
                                    public static void main(String[] args) throws Exception {
                                        while (true) { Thread.sleep(50); System.out.println("tick"); }
                                    }
                                }
                            """.trimIndent(),
                            timeoutMs = 12_000
                        ),
                        CodeRunPaths(java = javaBin),
                        temp
                    )
                )
            }.start()
            Thread.sleep(400)
            assertTrue(CodeRunEngine.cancel(cancelId))
            val cancelled = future.get(6, TimeUnit.SECONDS)
            assertTrue(cancelled.cancelled)
            assertEquals(CodeRunErrorCode.ABORTED, cancelled.errorCode)
            assertTrue(cancelled.durationMs < 4_000)

            CodeRunEngine.maxOutputBytes = 64
            val truncated = CodeRunEngine.run(
                CodeRunInput(
                    requestId = "java-trunc-1",
                    runtime = CodeRuntime.Java,
                    code = """
                        public class Main {
                            public static void main(String[] args) {
                                for (int i = 0; i < 200; i++) System.out.print("abcdefghij");
                            }
                        }
                    """.trimIndent()
                ),
                CodeRunPaths(java = javaBin),
                temp
            )
            assertTrue(truncated.truncated)
            assertTrue(truncated.stdout.length <= 80)
        } finally {
            temp.toFile().deleteRecursively()
        }
    }

    @Test
    fun optionalRuntimesPrintFortyTwoWhenInstalled() {
        val temp = Files.createTempDirectory("mootool-compose-runtime-opt-")
        try {
            val statuses = CodeRunEngine.detect(CodeRunPaths(java = javaBin))
            val java = statuses.first { it.id == CodeRuntime.Java }
            assertTrue(java.available, "JDK 21 java -version must succeed in this environment")
            assertTrue(java.version.isNotBlank())

            fun runIf(runtime: CodeRuntime, code: String) {
                val status = statuses.first { it.id == runtime }
                if (!status.available) return
                val result = CodeRunEngine.run(
                    CodeRunInput(requestId = "${runtime.name.lowercase()}-opt-01", runtime = runtime, code = code),
                    CodeRunPaths(java = javaBin),
                    temp
                )
                assertEquals(0, result.exitCode, result.stderr + result.statusText)
                assertTrue(result.stdout.contains("42"), result.stdout)
            }
            runIf(CodeRuntime.Python, "print(6 * 7)")
            runIf(CodeRuntime.Node, "console.log(6 * 7)")
            runIf(CodeRuntime.Groovy, "println 6 * 7")
        } finally {
            temp.toFile().deleteRecursively()
        }
    }
}
