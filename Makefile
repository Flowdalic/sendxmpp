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
	scala-cli --power package --native-image --force $^ -o $@ -- --enable-url-protocols=https

# See
# - https://github.com/VirtusLab/scala-cli/issues/3222
# - https://github.com/VirtusLab/scala-cli/issues/3877
.PHONY: trace
trace:
	LD_LIBRARY_PATH="$LD_LIBRARY_PATH:/home/flo/.cache/coursier/arc/https/github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.1/graalvm-ce-java17-linux-amd64-22.3.1.tar.gz/graalvm-ce-java17-22.3.1/lib" scala-cli run --jvm graalvm:21 --java-opt -agentlib:native-image-agent=config-output-dir=`pwd`/trace sendxmpp.sc -- --help
