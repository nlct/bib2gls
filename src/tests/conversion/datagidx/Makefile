DATATOOL2BIB=../../../bin/datatool2bib
DEPS=../../../lib/datatool2bib.jar

all	: sample-index.bib

sample-index.bib	: sample-index.tex $(DEPS)
	$(DATATOOL2BIB) -p -i -f sort sample-index.tex sample-index.bib

