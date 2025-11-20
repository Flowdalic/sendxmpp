#!/usr/bin/env -S scala-cli shebang
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright © 2020-2025 Florian Schmaus
//> using scala 3.7.4
//> using option -deprecation
////> using packaging.graalvmArgs "--enable-url-protocols=http,https" -H:IncludeResources=.*
//> using dep org.igniterealtime.smack:smack-java11-full:4.5.0-rc1
//> using dep dev.dirs:directories:26
//> using dep org.rogach::scallop:5.3.0
//> using dep com.lihaoyi::os-lib:0.11.6
import scala.collection.*
import scala.io.Source

import dev.dirs.ProjectDirectories
import org.jivesoftware.smack.*
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jxmpp.jid.*
import org.jxmpp.jid.impl.JidCreate
import org.rogach.scallop.*
import os.*

lazy val sendxmppVersion =
  val absoluteScriptPath = os.pwd / os.RelPath(scriptPath)
  val scriptDir = absoluteScriptPath / os.up
  val dotGitDir = scriptDir / ".git"
  val sendxmppLibDir = os.root / "var" / "lib" / "sendxmpp-scala"
  val versionFiles = new mutable.ListBuffer[os.Path]()
  if os.isDir(dotGitDir) then versionFiles += scriptDir / "version"
  versionFiles += sendxmppLibDir / "version"
  versionFiles.view
    .filter(os.isFile(_))
    .map(os.read(_).trim)
    .headOption
    .getOrElse("unknown")

// TODO: Move under Conf?
sealed trait MessageSource
case class IsString(message: String) extends MessageSource
case class FromStdin() extends MessageSource

def toPath(path: String): Path = os.Path.expandUser(path, os.pwd)

class Conf(args: Seq[String]) extends ScallopConf(args):
  appendDefaultToDescription = true
  version(s"sendxmpp ${sendxmppVersion} (Scala 3, Smack ${Smack.getVersion()})")

  val jidConverter = singleArgConverter[Jid](JidCreate.from(_))
  val messageSourceConverter = singleArgConverter[MessageSource](_ match
    case "-" => FromStdin()
    case s   => IsString(s)
  )
  val credfileConverter = singleArgConverter[os.Path](toPath(_))

  object Send extends Subcommand("send", "to"):
    descr("Send an XMPP message to the provided recipient.")
    val defaultCredfile =
      val projectDirectories = ProjectDirectories.from(
        "eu.geekplace",
        "Geekplace",
        "sendxmpp",
      )
      val configDir = os.Path(projectDirectories.configDir)
      configDir / "credentials"
    val credfile = opt[os.Path](
      descr =
        "The file with the XMPP credentials. First line must contain the JID, the second line the password.",
      default = Some(defaultCredfile),
    )(using credfileConverter)
    val recipient =
      trailArg[Jid](required = true, descr = "The XMPP address (JID) of the recipient")(using jidConverter)
    val message = trailArg[MessageSource](
      required = true,
      descr = "The message to send. Use '-' to read message from standard input (stdin)",
    )(using messageSourceConverter)
  addSubcommand(Send)

  object License extends Subcommand("license"):
    descr("Show license information")
  addSubcommand(License)

  verify()

val conf = new Conf(args.toIndexedSeq)

conf.subcommand match
  case Some(s: conf.Send.type) => send(s)
  case Some(conf.License)      => license()
  case _ =>
    System.err.println("ERROR: No sub-command specified, must specify one!")
    System.err.println(conf.getHelpString())
    System.exit(1)

def send(args: conf.Send.type) =
  val recipientJid = args.recipient()
  val credfilePath = args.credfile().toIO

  if !credfilePath.isFile then
    System.err.println(s"ERROR: No credentials file found at ${credfilePath}")
    System.exit(1)

  val credfileLines = Source.fromFile(credfilePath).getLines
  val myJid = JidCreate.entityBareFrom(credfileLines.next)
  val password = credfileLines.next

  val messageBody = args.message() match
    case IsString(s) => s
    case FromStdin() => Source.fromInputStream(System.in).mkString

  val connection = new XMPPTCPConnection(myJid, password)

  val messageStanza = connection.getStanzaFactory.buildMessageStanza
    .to(recipientJid)
    .setBody(messageBody)
    .build

  try
    connection.connect.login

    connection.sendStanza(messageStanza)
  finally connection.disconnect

def license() =
  val license = new StringBuilder(
    s"""sendxmpp ${sendxmppVersion} - A command line tool to send XMPP messages
Copyright © 2020-2025 Florian Schmaus

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.


Used third party libraries:

"""
  )

  val smackNoticeStream = Smack.getNoticeStream()
  for line <- Source.fromInputStream(smackNoticeStream).getLines do license.append(line).append('\n')

  print(license)
