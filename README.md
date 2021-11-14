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
