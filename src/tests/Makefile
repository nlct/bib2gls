BIB2GLS=../bin/bib2gls --debug --verbose
DEPS=../lib/bib2gls.jar ../lib/texparserlib.jar
RM=\rm -f

all	: test.pdf test-utf8.pdf test-xr.pdf test-crossrefs.pdf \
	  test-xe.pdf

test.pdf	: test.aux test-entries.glstex
		pdflatex test

test-entries.glstex	: test.aux $(DEPS)
		$(BIB2GLS) test

test.aux	: test.tex test-entries.bib
		pdflatex test

test-utf8.pdf	: test-utf8.aux test-utf8.glstex
		xelatex test-utf8

test-utf8.glstex	: test-utf8.aux $(DEPS)
		$(BIB2GLS) --group test-utf8

test-utf8.aux	: test-utf8.tex test-utf8.bib
		xelatex test-utf8

test-xr.pdf	: test-xr.aux test-xr.glstex test.pdf
		pdflatex test-xr

test-xr.glstex	: test-xr.aux $(DEPS)
		$(BIB2GLS) test-xr

test-xr.aux	: test-xr.tex test-entries.bib
		pdflatex test-xr

test-crossrefs.pdf	: test-crossrefs.aux test-crossrefs.glstex
		pdflatex test-crossrefs

test-crossrefs.glstex	: test-crossrefs.aux $(DEPS)
		$(BIB2GLS) test-crossrefs

test-crossrefs.aux	: test-crossrefs.tex entries-terms.bib \
			  entries-abbrv.bib entries-symbols.bib
		pdflatex test-crossrefs

test-xe.pdf	: test-xe.aux test-xe.glstex
		xelatex test-xe

test-xe.glstex	: test-xe.aux $(DEPS)
		$(BIB2GLS) test-xe

test-xe.aux	: test-xe.tex test-entries.bib
		xelatex test-xe

clean	:
	$(RM) *.aux
	$(RM) *.log
	$(RM) *.pdf
	$(RM) *.glstex
	$(RM) *.glg
	$(RM) *.out
