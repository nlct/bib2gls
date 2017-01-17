BIB2GLS=../bin/bib2gls --debug --verbose
DEPS=../lib/bib2gls.jar ../lib/texparserlib.jar
RM=\rm -f

all	: test.pdf test-utf8.pdf

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

clean	:
	$(RM) *.aux
	$(RM) *.log
	$(RM) *.pdf
	$(RM) *.glstex
	$(RM) *.glg
	$(RM) *.out
