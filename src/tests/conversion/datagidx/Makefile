DATATOOL2BIB=../../../bin/datatool2bib
BIB2GLS=../../../bin/bib2gls
DEPS=../../../lib/datatool2bib.jar ../../../lib/bib2gls.jar ../../../lib/texparserlib.jar
ARARA=arara --verbose
RM=\rm -f

all	: test-converted-index.pdf test-converted-gidx.pdf test-converted-gidx2.pdf

sample-index.bib	: sample-index.tex $(DEPS)
	$(DATATOOL2BIB) --log-file datatool2bib-index.log -p -i --space-sub '-' -f sort sample-index.tex sample-index.bib

test-converted-index.pdf	: test-converted-index.tex sample-index.bib
				$(ARARA) test-converted-index.tex

sample-gidx-index.bib	: sample-gidx.tex $(DEPS)
	$(DATATOOL2BIB) --log-file datatool2bib-gidx.log --split -p -i --space-sub '-' -f sort sample-gidx.tex sample-gidx.bib

test-converted-gidx.pdf	: test-converted-gidx.tex sample-gidx-index.bib \
			sample-gidx-acronyms.bib \
			sample-gidx-glossary.bib \
			sample-gidx-notation.bib
			$(ARARA) test-converted-gidx.tex

sample-gidx.bib	: sample-gidx.tex $(DEPS)
	$(DATATOOL2BIB) --log-file datatool2bib-gidx2.log --detect-symbols --database-field identifier --strip-case-change -p -i --space-sub '-' -f sort sample-gidx.tex sample-gidx.bib

test-converted-gidx2.pdf	: test-converted-gidx2.tex sample-gidx.bib 
			$(ARARA) test-converted-gidx2.tex

clean	:
	$(RM) *.aux
	$(RM) *.log
	$(RM) *.pdf
	$(RM) *.glstex
	$(RM) *.glg
	$(RM) *.out
	$(RM) *.bib
