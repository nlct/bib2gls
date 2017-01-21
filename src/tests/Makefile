BIB2GLS=../bin/bib2gls --debug --verbose
DEPS=../lib/bib2gls.jar ../lib/texparserlib.jar
RM=\rm -f

all	: test.pdf test-utf8.pdf test-xr.pdf test-crossrefs.pdf \
	  test-xe.pdf test-label-prefix.pdf test-dual.pdf

../lib/bib2gls.jar:	../java/*.java
		make -C ..

test.pdf	: test.aux test-entries.glstex
		pdflatex test

test-entries.glstex	: test.aux $(DEPS) test-entries.bib
		$(BIB2GLS) test

test.aux	: test.tex
		pdflatex test

test-utf8.pdf	: test-utf8.aux test-utf8.glstex
		xelatex test-utf8

test-utf8.glstex	: test-utf8.aux $(DEPS) test-utf8.bib
		$(BIB2GLS) --group test-utf8

test-utf8.aux	: test-utf8.tex
		xelatex test-utf8

test-xr.pdf	: test-xr.aux test-xr.glstex test.pdf
		pdflatex test-xr

test-xr.glstex	: test-xr.aux $(DEPS) test-entries.bib
		$(BIB2GLS) test-xr

test-xr.aux	: test-xr.tex
		pdflatex test-xr

test-crossrefs.pdf	: test-crossrefs.aux test-crossrefs.glstex
		pdflatex test-crossrefs

test-crossrefs.glstex	: test-crossrefs.aux $(DEPS) \
			  entries-terms.bib \
			  entries-abbrv.bib \
			  entries-symbols.bib
		$(BIB2GLS) test-crossrefs

test-crossrefs.aux	: test-crossrefs.tex
		pdflatex test-crossrefs

test-xe.pdf	: test-xe.aux test-xe.glstex
		xelatex test-xe

test-xe.glstex	: test-xe.aux $(DEPS) test-entries.bib
		$(BIB2GLS) test-xe

test-xe.aux	: test-xe.tex
		xelatex test-xe

test-label-prefix.pdf	: test-label-prefix.aux test-label-prefix.glstex
		pdflatex test-label-prefix

test-label-prefix.glstex	: test-label-prefix.aux $(DEPS) \
	 			test-entries.bib
		$(BIB2GLS) test-label-prefix

test-label-prefix.aux	: test-label-prefix.tex
		pdflatex test-label-prefix

test-dual.pdf	: test-dual.aux test-dual.glstex
		pdflatex test-dual

test-dual.glstex	: test-dual.aux $(DEPS) \
	 			entries-dual.bib
		$(BIB2GLS) test-dual

test-dual.aux	: test-dual.tex
		pdflatex test-dual

clean	:
	$(RM) *.aux
	$(RM) *.log
	$(RM) *.pdf
	$(RM) *.glstex
	$(RM) *.glg
	$(RM) *.out
