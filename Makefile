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
	scala-cli fmt --check

PHONY: format
format:
	scala-cli fmt

sendxmpp-native: sendxmpp.sc
	scala-cli --power package --native-image --force $^ -o $@
