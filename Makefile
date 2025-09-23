DEFAULT: check

.PHONY: install
install:
	install -D --mode 755 sendxmpp.sc "${DESTDIR}/usr/bin/sendxmpp"
	install -D --mode 644 version "${DESTDIR}/var/lib/sendxmpp-scala/version"

.PHONY: check
check: check-compiles check-format

.PHONY: check-compiles
check-compiles:
	./sendxmpp.sc --help

.PHONY: check-format
check-format:
	scala-cli fmt --check sendxmpp.sc

.PHONY: format
format:
	scala-cli fmt sendxmpp.sc

sendxmpp-native: sendxmpp.sc
	scala-cli --power package --native-image --force $^ -o $@
