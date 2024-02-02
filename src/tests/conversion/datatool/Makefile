DATATOOL2BIB=../../../bin/datatool2bib
DEPS=../../../lib/datatool2bib.jar

all	: test-action.bib

test-action.bib	: test-action.tex $(DEPS)
		$(DATATOOL2BIB) --label Name test-action.tex test-action.bib
