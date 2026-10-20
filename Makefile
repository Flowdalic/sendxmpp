VERSION := $(shell cat version)

.PHONY: all
all: check sendxmpp-dist-$(VERSION).jar

.PHONY: install
install:
	install -D --mode 755 sendxmpp.sc "${DESTDIR}/usr/bin/sendxmpp"
	install -D --mode 644 version "${DESTDIR}/var/lib/sendxmpp-scala/version"

.PHONY: check
check: check-compiles check-format

.PHONY: check-compiles
check-compiles:
	./sendxmpp.sc --help
	./trace.sc --help

.PHONY: check-format
check-format:
	scala-cli fmt --check sendxmpp.sc trace.sc

.PHONY: format
format:
	scala-cli fmt sendxmpp.sc trace.sc

sendxmpp-native: sendxmpp.sc
	scala-cli --power package --native-image --force $^ -o $@ -- --enable-url-protocols=https

.PHONY: sendxmpp-dist
sendxmpp-dist: sendxmpp-dist-$(VERSION).jar

sendxmpp-dist-$(VERSION).jar: sendxmpp.sc
	scala-cli --power package --assembly --preamble=false --force $^ -o $@

# Regenerate the native-image reachability metadata, see trace.sc.
.PHONY: trace
trace:
	./trace.sc

.PHONY: trace-offline
trace-offline:
	./trace.sc --offline
