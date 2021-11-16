DEFAULT: nothing

PHONY: install
install:
	install -D --mode 755 sendxmpp "${DESTDIR}/usr/bin/sendxmpp"

PHONY: check
check:
	./sendxmpp --help

nothing:
