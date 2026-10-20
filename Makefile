VERSION := $(shell cat version)
GRAALVM := graalvm-community:25.0.2
NATIVE_IMAGE_METADATA_DIR := native-image-metadata
NATIVE_IMAGE_METADATA := $(NATIVE_IMAGE_METADATA_DIR)/reachability-metadata.json

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

# Invoke native-image directly, instead of via 'scala-cli package --native-image', as scala-cli
# caches the native image without considering changes of the reachability metadata.
sendxmpp-native: sendxmpp-dist-$(VERSION).jar $(NATIVE_IMAGE_METADATA)
	graalvm_home="$$(scala-cli run --quiet --jvm $(GRAALVM) -e 'println(System.getProperty("java.home"))')" && \
	"$${graalvm_home}/bin/native-image" \
		--no-fallback \
		--enable-url-protocols=https \
		-H:ConfigurationFileDirectories="$(NATIVE_IMAGE_METADATA_DIR)" \
		-jar $< \
		-o $@

# Generating the native-image reachability metadata requires an XMPP account, see trace.sc.
$(NATIVE_IMAGE_METADATA): sendxmpp.sc trace.sc
	SENDXMPP_TRACE_GRAALVM=$(GRAALVM) ./trace.sc

.PHONY: sendxmpp-dist
sendxmpp-dist: sendxmpp-dist-$(VERSION).jar

sendxmpp-dist-$(VERSION).jar: sendxmpp.sc
	scala-cli --power package --assembly --preamble=false --force $^ -o $@

# Regenerate the native-image reachability metadata, even if it is up to date.
.PHONY: trace
trace:
	SENDXMPP_TRACE_GRAALVM=$(GRAALVM) ./trace.sc
