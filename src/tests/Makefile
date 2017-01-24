BIB2GLS=../bin/bib2gls --debug --verbose
DEPS=../lib/bib2gls.jar
RM=\rm -f

all	: test.pdf test-utf8.pdf test-xr.pdf test-crossrefs.pdf \
	  test-xe.pdf test-label-prefix.pdf test-dual.pdf \
	  test-external-refs.pdf

../lib/bib2gls.jar:	../java/*.java
		make -C ..

test.pdf	: test.glstex
		pdflatex test

test.glstex	: test.tex $(DEPS) test-entries.bib
		pdflatex test
		$(BIB2GLS) test

test-utf8.pdf	: test-utf8.glstex
		xelatex test-utf8

test-utf8.glstex	: test-utf8.tex $(DEPS) test-utf8.bib
		xelatex test-utf8
		$(BIB2GLS) --group test-utf8

test-xr.pdf	: test-xr.glstex
		pdflatex test-xr

test-xr.glstex	: test-xr.tex $(DEPS) test-entries.bib
		pdflatex test-xr
		$(BIB2GLS) test-xr

test-crossrefs.pdf	: test-crossrefs.glstex
		pdflatex test-crossrefs

test-crossrefs.glstex	: test-crossrefs.tex $(DEPS) \
			  entries-terms.bib \
			  entries-abbrv.bib \
			  entries-symbols.bib
		pdflatex test-crossrefs
		$(BIB2GLS) test-crossrefs

test-xe.pdf	: test-xe.glstex
		xelatex test-xe

test-xe.glstex	: test-xe.tex $(DEPS) test-entries.bib
		xelatex test-xe
		$(BIB2GLS) test-xe

test-label-prefix.pdf	: test-label-prefix.glstex
		pdflatex test-label-prefix

test-label-prefix.glstex	: test-label-prefix.tex $(DEPS) \
	 			test-entries.bib
		pdflatex test-label-prefix
		$(BIB2GLS) test-label-prefix

test-dual.pdf	: test-dual.glstex
		pdflatex test-dual

test-dual.glstex	: test-dual.tex $(DEPS) \
	 			entries-dual.bib
		pdflatex test-dual
		$(BIB2GLS) test-dual

test-external-refs.pdf	: test-external-refs.glstex
		pdflatex test-external-refs

test-external-refs.glstex	: test-external-refs.tex $(DEPS) \
	 			entries-terms.bib entries-symbols.bib
		pdflatex test-external-refs
		$(BIB2GLS) test-external-refs

clean	:
	$(RM) *.aux
	$(RM) *.log
	$(RM) *.pdf
	$(RM) *.glstex
	$(RM) *.glg
	$(RM) *.out
