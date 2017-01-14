BIB2GLS=../bin/bib2gls
DEPS=../lib/bib2gls.jar ../lib/texparserlib.jar
RM=\rm -f

all	: test.pdf

test.pdf	: test.tex test-entries.bib $(DEPS)
		pdflatex test
		$(BIB2GLS) test
		pdflatex test

clean	:
	$(RM) *.aux
	$(RM) *.log
	$(RM) *.pdf
	$(RM) *.glstex
	$(RM) *.glg
	$(RM) *.out
