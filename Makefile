DEFAULT: check

PHONY: install
install:
	install -D --mode 755 sendxmpp "${DESTDIR}/usr/bin/sendxmpp"

PHONY: check
check: check-compiles check-format

PHONY: check-compiles
check-compiles:
	./sendxmpp --help

PHONY: check-format
check-format:
	scalafmt --test

PHONY: format
format:
	scalafmt

sendxmpp-native: sendxmpp.sc
	scala-cli --power package --native-image --force $^ -o $@
