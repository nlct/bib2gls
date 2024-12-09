DATATOOL2BIB=../../../bin/datatool2bib
BIB2GLS=../../../bin/bib2gls
DEPS=../../../lib/datatool2bib.jar ../../../lib/bib2gls.jar ../../../lib/texparserlib.jar
ARARA=arara --verbose

all	: test-converted-index.pdf sample-gidx.bib

sample-index.bib	: sample-index.tex $(DEPS)
	$(DATATOOL2BIB) -p -i --space-sub '-' -f sort sample-index.tex sample-index.bib

test-converted-index.pdf	: test-converted-index.tex sample-index.bib
				$(ARARA) test-converted-index.tex

sample-gidx.bib	: sample-gidx.tex $(DEPS)
	$(DATATOOL2BIB) -p -i -f sort sample-gidx.tex sample-gidx.bib

