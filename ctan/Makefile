TEXPARSERDIR=$(HOME)/programming/java/texparser-git
BIB2GLSDIR=$(HOME)/programming/java/bib2gls

bib2gls.zip	: README.md CHANGES ../src/lib/bib2gls.jar ../src/lib/convertgls2bib.jar ../src/bin/bib2gls.sh ../src/bin/convertgls2bib.sh ../src/doc/bib2gls.pdf ../src/doc/bib2gls-begin.pdf
	mkdir -p bib2gls
	mkdir -p bib2gls/src
	mkdir -p bib2gls/examples
	cp ../src/bin/bib2gls.sh bib2gls
	cp ../src/bin/convertgls2bib.sh bib2gls
	cp ../src/lib/bib2gls.jar bib2gls
	cp ../src/lib/convertgls2bib.jar bib2gls
	cp -L ../src/lib/texparserlib.jar bib2gls
	cp ../src/doc/bib2gls*.tex bib2gls/src
	cp ../src/doc/bib2gls*.pdf bib2gls
	cp ../src/doc/*.pod bib2gls/src
	cp ../src/doc/*.bib bib2gls/src
	cp ../src/doc/*.1 bib2gls
	cp ../src/lib/resources/*.xml bib2gls
	cp ../src/examples/*.{tex,bib,pdf} bib2gls/examples
	cp README.md CHANGES DEPENDS.txt bib2gls
	cd ../src/java/; \
	zip -r bib2gls-src.zip bib2gls; \
	mv bib2gls-src.zip ../../ctan/bib2gls/src ; \
	zip -r gls2bib-src.zip gls2bib; \
	mv gls2bib-src.zip ../../ctan/bib2gls/src ; 
	cd $(TEXPARSERDIR)/src/java ; \
	zip -r texparser-src.zip lib/ ; \
	mv texparser-src.zip $(BIB2GLSDIR)/ctan/bib2gls/src/
	zip --exclude="*.swp" -r bib2gls.zip bib2gls
