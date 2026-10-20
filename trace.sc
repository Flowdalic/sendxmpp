#!/usr/bin/env -S scala-cli shebang
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright © 2026 Florian Schmaus
//
// Generate the GraalVM native-image reachability metadata of sendxmpp by
// running sendxmpp under the native-image tracing agent. The metadata is
// written to native-image-metadata/, which is used by 'make sendxmpp-native'.
//
// The online scenarios log into a real XMPP account and send messages. The
// credentials file (JID on the first line, password on the second line) is
// looked up in the following order:
//   1. $SENDXMPP_TRACE_CREDFILE
//   2. <sendxmpp config dir>/trace-credentials
//   3. <sendxmpp config dir>/credentials
//
// Environment variables:
//   SENDXMPP_TRACE_CREDFILE    Credentials file to use for the online scenarios
//   SENDXMPP_TRACE_RECIPIENT   Recipient of the messages (default: the JID of the credentials file)
//   SENDXMPP_TRACE_GRAALVM     GraalVM JVM id (default: graalvm-community:25.0.2)
//> using scala 3.8.4
//> using option -deprecation
//> using dep dev.dirs:directories:26
//> using dep org.rogach::scallop:6.0.1
//> using dep com.lihaoyi::os-lib:0.11.8
import java.time.Instant

import dev.dirs.ProjectDirectories
import org.rogach.scallop.*

class Conf(args: Seq[String]) extends ScallopConf(args):
  banner("Generate the native-image reachability metadata of sendxmpp")
  val offline = opt[Boolean](
    descr = "Only run the scenarios that do not connect to an XMPP server. " +
      "The resulting metadata does not cover TLS, login and sending."
  )
  verify()

val conf = new Conf(args.toIndexedSeq)

val repoDir = os.Path(scriptPath, os.pwd) / os.up
val sendxmppScript = repoDir / "sendxmpp.sc"
val metadataDir = repoDir / "native-image-metadata"
val graalvm = sys.env.getOrElse("SENDXMPP_TRACE_GRAALVM", "graalvm-community:25.0.2")

case class TraceFailure(message: String) extends Exception(message)

def fail(message: String): Nothing = throw TraceFailure(message)

// Must match the config dir used by sendxmpp.
val configDir = os.Path(ProjectDirectories.from("eu.geekplace", "Geekplace", "sendxmpp").configDir)

def findCredfile(): Option[os.Path] =
  sys.env.get("SENDXMPP_TRACE_CREDFILE") match
    case Some(credfile) => Some(os.Path.expandUser(credfile, os.pwd)).filter(os.isFile(_))
    case None           => Seq(configDir / "trace-credentials", configDir / "credentials").find(os.isFile(_))

enum Expected:
  case Success, Failure

def run(): Unit =
  val online: Option[(os.Path, String)] =
    if conf.offline() then None
    else
      val credfile = findCredfile().getOrElse(
        fail(
          "No credentials file found for tracing, see the header of trace.sc. " +
            "Use --offline to trace only the scenarios that do not require an XMPP account."
        )
      )
      val recipient = sys.env.getOrElse("SENDXMPP_TRACE_RECIPIENT", os.read.lines(credfile).head)
      println(s"Using credentials file ${credfile}, sending messages to ${recipient}")
      Some((credfile, recipient))

  val workDir = os.temp.dir(deleteOnExit = false)
  val traceDir = workDir / "trace"

  def trace(expected: Expected, args: String*): Unit = traceWithStdin(expected, os.Pipe, args*)

  def traceWithStdin(expected: Expected, stdin: os.ProcessInput, args: String*): Unit =
    println(s"Tracing: sendxmpp ${args.mkString(" ")}")
    val result = os
      .proc(
        "scala-cli",
        "run",
        "--quiet",
        "--jvm",
        graalvm,
        "--java-opt",
        s"-agentlib:native-image-agent=config-merge-dir=${traceDir}",
        sendxmppScript,
        "--",
        args,
      )
      .call(check = false, stdin = stdin, mergeErrIntoOut = true)
    val asExpected = expected match
      case Expected.Success => result.exitCode == 0
      case Expected.Failure => result.exitCode != 0
    if !asExpected then
      System.err.print(result.out.text())
      fail(s"Expected ${expected}, but sendxmpp exited with ${result.exitCode}")

  try
    // Offline scenarios.
    trace(Expected.Success, "--help")
    trace(Expected.Success, "--version")
    trace(Expected.Success, "license")
    trace(Expected.Failure)
    trace(Expected.Failure, "send")
    trace(
      Expected.Failure,
      "send",
      "--credfile",
      (workDir / "nonexistent").toString,
      "alice@example.org",
      "message",
    )
    val malformedCredfile = workDir / "malformed-credentials"
    os.write(malformedCredfile, "alice@example.org\n", perms = "rw-------")
    trace(Expected.Failure, "send", "--credfile", malformedCredfile.toString, "alice@example.org", "message")
    // Builds the message stanza and performs the DNS lookups, but fails before connecting, since the .invalid
    // TLD never resolves (RFC 2606).
    val unresolvableCredfile = workDir / "unresolvable-credentials"
    os.write(unresolvableCredfile, "alice@sendxmpp-trace.invalid\npassword\n", perms = "rw-------")
    trace(
      Expected.Failure,
      "send",
      "--credfile",
      unresolvableCredfile.toString,
      "alice@example.org",
      "message",
    )

    // Online scenarios.
    for (credfile, recipient) <- online do
      val timestamp = Instant.now.truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
      val message = s"sendxmpp native-image tracing run ${timestamp}"
      trace(
        Expected.Success,
        "send",
        "--credfile",
        credfile.toString,
        recipient,
        s"${message}: message from argument",
      )
      traceWithStdin(
        Expected.Success,
        s"${message}: message from stdin\n",
        Seq("send", "--credfile", credfile.toString, recipient, "-")*
      )

    os.copy(
      traceDir / "reachability-metadata.json",
      metadataDir / "reachability-metadata.json",
      replaceExisting = true,
      createFolders = true,
    )
  finally os.remove.all(workDir)

  println(s"Wrote ${metadataDir / "reachability-metadata.json"}")
  if online.isEmpty then
    System.err.println("WARNING: Offline tracing only, the metadata does not cover TLS, login and sending")

try run()
catch
  case TraceFailure(message) =>
    System.err.println(s"ERROR: ${message}")
    sys.exit(1)
