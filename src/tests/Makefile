BIB2GLS=../bin/bib2gls --debug --verbose
DEPS=../lib/bib2gls.jar ../lib/texparserlib.jar
RM=\rm -f

all	: test.pdf

test.pdf	: test.aux test-entries.glstex
		pdflatex test

test-entries.glstex	: test.aux $(DEPS)
		$(BIB2GLS) test

test.aux	: test.tex test-entries.bib
		pdflatex test

clean	:
	$(RM) *.aux
	$(RM) *.log
	$(RM) *.pdf
	$(RM) *.glstex
	$(RM) *.glg
	$(RM) *.out
