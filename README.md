# sendxmpp - A CLI to send XMPP messages

```shell
$ sendxmpp send user@example.org "Hello from sendxmpp"
```

You can also send input from stdin by using `-` as message-to-send argument:

```shell
$ echo "Hello from sendxmpp via stdin" | sendxmpp send user@example.org -
```

For more information see `sendxmpp --help`.

## Configuration

`sendxmpp` lookups the XMPP credentials from a file. The first line of the file must contain the XMPP address and the second line the password.

The default location of the credentials file is `~/.config/sendxmpp/credentials`.

## Architecture

This program is implemented as [Ammonite Scala script](https://ammonite.io/#ScalaScripts) and uses the [Smack](https://igniterealtime.org/projects/smack/) XMPP library.

## License

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
