ifeq ($(TEXPARSERDIR),)
  texparserdir=$(HOME)/programming/java/texparser-git
else
  texparserdir=$(TEXPARSERDIR)
endif

ifeq ($(BIB2GLSDIR),)
  bib2glsdir=$(HOME)/programming/java/bib2gls
else
  bib2glsdir=$(BIB2GLSDIR)
endif

bib2gls.zip	: README.md CHANGES DEPENDS.txt bib2gls.tds.zip bib2gls-src.zip texparser-src.zip \
	../src/lib/bib2gls.jar \
	../src/lib/bibglscommon.jar \
	../src/lib/convertgls2bib.jar \
	../src/lib/datatool2bib.jar \
	../src/bin/bib2gls.sh \
	../src/bin/convertgls2bib.sh \
	../src/bin/datatool2bib.sh \
	../src/doc/bib2gls.pdf \
	../src/doc/bib2gls-begin.pdf
	mkdir -p bib2gls
	mkdir -p bib2gls/src
	mkdir -p bib2gls/examples
	cp ../src/bin/*.sh bib2gls
	cp -L ../src/lib/*.jar bib2gls
	cp ../src/doc/*.tex bib2gls/src
	cp ../src/doc/bib2gls.pdf bib2gls
	cp ../src/doc/bib2gls-begin.pdf bib2gls
	cp ../src/doc/*.pod bib2gls/src
	cp ../src/doc/*.bib bib2gls/src
	cp ../src/doc/*.1 bib2gls
	cp -r ../src/lib/resources bib2gls
	cp ../src/examples/*.{tex,bib,pdf} bib2gls/examples
	cp *-src.zip bib2gls/src/
	cp README.md CHANGES DEPENDS.txt bib2gls
	zip --exclude="*.swp" -r bib2gls.zip bib2gls bib2gls.tds.zip

bib2gls-src.zip	:
	cd ../src/ ; \
	zip --exclude="*.swp" -r ../ctan/bib2gls-src.zip java 

texparser-src.zip	:
	cd $(texparserdir)/src/ ; \
	zip --exclude="*.swp" -r $(bib2glsdir)/ctan/texparser-src.zip java/lib

bib2gls.tds.zip	: README.md CHANGES bib2gls-src.zip texparser-src.zip \
	../src/lib/bib2gls.jar \
	../src/lib/bibglscommon.jar \
	../src/lib/convertgls2bib.jar \
	../src/lib/datatool2bib.jar \
	../src/bin/bib2gls.sh \
	../src/bin/convertgls2bib.sh \
	../src/bin/datatool2bib.sh \
	../src/doc/bib2gls.pdf \
	../src/doc/bib2gls-begin.pdf
	mkdir -p doc/support/bib2gls
	mkdir -p doc/support/bib2gls/examples
	cp README.md CHANGES doc/support/bib2gls/
	cp ../src/doc/bib2gls.pdf doc/support/bib2gls/
	cp ../src/doc/bib2gls-begin.pdf doc/support/bib2gls/
	cp ../src/examples/*.{tex,bib,pdf} doc/support/bib2gls/examples
	mkdir -p doc/man/man1/
	cp ../src/doc/*.{1,man1.pdf} doc/man/man1/
	mkdir -p source/support/bib2gls
	cp *-src.zip source/support/bib2gls/
	cp ../src/doc/*.{tex,bib,pod} source/support/bib2gls/
	mkdir -p scripts/bib2gls
	cp ../src/bin/*.sh scripts/bib2gls
	cp -L ../src/lib/*.jar scripts/bib2gls/
	cp -r ../src/lib/resources scripts/bib2gls/
	zip --exclude="*.swp" -r bib2gls.tds.zip doc scripts source

clean:
	rm -rf bib2gls
	rm -rf doc scripts source
	rm -f *-src.zip
	rm -f bib2gls.zip
	rm -f bib2gls.tds.zip
