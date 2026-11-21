package org.tormap.cli

import com.github.ajalt.clikt.completion.CompletionCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.subcommands
import java.io.PrintStream
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    exitProcess(runCli(args, System.out, System.err, System::getenv))
}

/** Runs the CLI and returns the exit code (see [ExitCode]); stdout and stderr are parameters for tests. */
fun runCli(args: Array<String>, stdout: PrintStream, stderr: PrintStream, env: (String) -> String?): Int {
    val command = Tormap(stdout, stderr, env).subcommands(
        Days(), Relays(), Relay(), Family(), AutonomousSystems(), Stats(), Usage(),
        CompletionCommand(name = "completion", help = "Print a shell completion script for bash, zsh or fish."),
    )
    command.context {
        echoMessage = { _, message, trailingNewline, err ->
            val stream = if (err) stderr else stdout
            if (trailingNewline) stream.println(message) else stream.print(message)
        }
    }
    return try {
        command.parse(hoistGlobalOptions(args))
        ExitCode.OK
    } catch (result: ProgramResult) {
        result.statusCode
    } catch (error: CliktError) {
        // Help and --version are CliktErrors too, printed to stdout with status 0
        command.echoFormattedHelp(error)
        if (error is UsageError) ExitCode.USAGE else error.statusCode
    } catch (error: NotFound) {
        stderr.println("tormap: ${sanitize(error.message.orEmpty())}")
        ExitCode.NOT_FOUND
    } catch (error: Ambiguous) {
        stderr.println(sanitize("tormap: ${error.matches.size} relays are named '${error.query}' on ${error.day}. Use one of these fingerprints:"))
        error.matches.forEach { stderr.println(sanitize("  ${it.text("fingerprint")}  ${it.text("country")}")) }
        ExitCode.AMBIGUOUS
    } catch (error: CheckFailed) {
        stderr.println("tormap: ${error.message}")
        ExitCode.CHECK_FAILED
    } catch (error: ApiException) {
        // Error messages can quote the server, e.g. the message of a 400 response
        stderr.println("tormap: ${sanitize(error.message.orEmpty())}")
        ExitCode.ERROR
    }
}

private val GLOBAL_OPTIONS_WITH_VALUE = setOf("--api-url", "--proxy", "--format", "-f", "--output", "-o", "--cache-dir", "--timeout")
private val GLOBAL_FLAGS = setOf("--no-cache", "--quiet", "-q")

/**
 * Moves the options of the root command in front of the subcommand, so `tormap relays --format csv` works like
 * `tormap --format csv relays`. Arguments after `--` stay where they are.
 */
internal fun hoistGlobalOptions(args: Array<String>): Array<String> {
    val global = mutableListOf<String>()
    val rest = mutableListOf<String>()
    var index = 0
    while (index < args.size) {
        val arg = args[index]
        val name = arg.substringBefore("=")
        when {
            arg == "--" -> {
                rest += args.drop(index)
                break
            }
            arg in GLOBAL_FLAGS -> global += arg
            name in GLOBAL_OPTIONS_WITH_VALUE && arg.contains("=") -> global += arg
            arg in GLOBAL_OPTIONS_WITH_VALUE && index + 1 < args.size -> {
                global += arg
                global += args[++index]
            }
            else -> rest += arg
        }
        index++
    }
    return (global + rest).toTypedArray()
}
