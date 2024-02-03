DATATOOL2BIB=../../../bin/datatool2bib
DEPS=../../../lib/datatool2bib.jar

all	: test-action.bib test-datum-3.bib test-data-3.bib \
	  test-data-3-dtl.bib test-tex.bib test-literal.bib

test-action.bib	: test-action.tex $(DEPS)
		$(DATATOOL2BIB) --label Name test-action.tex test-action.bib

test-datum-3.bib	: test-datum-3.dbtex $(DEPS)
		$(DATATOOL2BIB) --label product test-datum-3.dbtex test-datum-3.bib

test-data-3.bib	: test-data-3.dbtex $(DEPS)
		$(DATATOOL2BIB) --label name test-data-3.dbtex test-data-3.bib

test-data-3-dtl.bib	: test-data-3.dtltex $(DEPS)
		$(DATATOOL2BIB) --label name test-data-3.dtltex test-data-3-dtl.bib

test-tex.bib	: test-tex.csv $(DEPS)
		$(DATATOOL2BIB) --auto-label --read "name=texdb,csv-content=tex,format=csv" test-tex.csv test-tex.bib

test-literal.bib	: test-literal.csv $(DEPS)
		$(DATATOOL2BIB) --auto-label --read "name=texdb,csv-content=literal,format=csv" test-literal.csv test-literal.bib
