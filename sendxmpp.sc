#!/usr/bin/env amm
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright © 2020-2023 Florian Schmaus

import $ivy.`org.igniterealtime.smack:smack-java8-full:4.4.6`
import $ivy.`dev.dirs:directories:26`

import scala.io.Source

import org.jxmpp.jid.impl.JidCreate

import org.jivesoftware.smack._
import org.jivesoftware.smack.tcp.XMPPTCPConnection

import dev.dirs.ProjectDirectories

// Initialize Smack and then remove the BC provider, as it causes
// issues on certain Java versions.
// TODO: Remove this once sendxmpp uses Smack 4.5 or later.
SmackConfiguration.getVersion()
// Remove the BC provider after Smack was initialized.
java.security.Security.removeProvider("BC")

@main(doc =
  "Send an XMPP message to the provided recipient. Positional arguments are possible: the first is the XMPP address (JID) of the recipient, the second is the XMPP message (or '-' for stdin)"
)
def send(
    @arg(doc = "The XMPP address (JID) of the recipient") recipient: String,
    @arg(doc =
      "The message to send. Use '-' to read message from standard input (stdin)"
    ) message: String,
    @arg(doc =
      "(Optional) The file with the XMPP credentials. First line must contain the JID, the second line the password.  Default: ~/.config/sendxmpp/credentials"
    ) credfile: String = "auto",
) = {
  val recipientJid = JidCreate.from(recipient)

  val credfilePath = {
    if (credfile == "auto") {
      val projectDirectories = ProjectDirectories.from(
        "eu.geekplace",
        "Geekplace",
        "sendxmpp",
      )
      val configDir = os.Path(projectDirectories.configDir)
      configDir / "credentials"
    } else os.Path(os.FilePath(credfile), os.pwd)
  }.toIO

  if (!credfilePath.isFile) {
    System.err.println(s"No credentials file found at ${credfilePath}")
    System.exit(1)
  }

  val credfileLines = Source.fromFile(credfilePath).getLines
  val myJid = JidCreate.entityBareFrom(credfileLines.next)
  val password = credfileLines.next

  val messageBody =
    if (message == "-")
      Source.fromInputStream(System.in).mkString
    else message

  val connection = new XMPPTCPConnection(myJid, password)

  val messageStanza = connection.getStanzaFactory.buildMessageStanza
    .to(recipientJid)
    .setBody(messageBody)
    .build

  try {
    connection.connect.login

    connection.sendStanza(messageStanza)
  } finally {
    connection.disconnect
  }
}

@main(doc = "Show license information")
def license() = {
  val license = new StringBuilder(
    """sendxmpp - A command line tool to send XMPP messages.
Copyright © 2020-2023 Florian Schmaus

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

  val smackVersion = SmackConfiguration.getVersion()
  license.append(s"Smack ${smackVersion} XMPP client library\n")

  val smackNoticeStream = Smack.getNoticeStream()
  for (line <- Source.fromInputStream(smackNoticeStream).getLines)
    license.append(line).append('\n')

  print(license)
}
