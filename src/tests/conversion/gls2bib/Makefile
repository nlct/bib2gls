GLS2BIB=../../../bin/convertgls2bib
DEPS=../../../lib/convertgls2bib.jar

all	: from-doc.bib dual.bib opt.bib original.bib original2.bib original-loop.bib

from-doc.bib	: doc.tex $(DEPS)
		$(GLS2BIB) --log-file convertgls2bib-from.log \
		--preamble-only doc.tex from-doc.bib

dual.bib	: dual.tex $(DEPS)
		$(GLS2BIB) --log-file convertgls2bib-dual.log \
		dual.tex dual.bib

opt.bib	: opt.tex $(DEPS)
	$(GLS2BIB) --log-file convertgls2bib-opt.log opt.tex opt.bib

original.bib	: original.tex $(DEPS)
	$(GLS2BIB) --log-file convertgls2bib-original.log \
	original.tex original.bib

original2.bib	: original2.tex $(DEPS)
	$(GLS2BIB) --log-file convertgls2bib-original2.log \
	original2.tex original2.bib

original-loop.bib	: original-loop.tex $(DEPS)
	$(GLS2BIB) --log-file convertgls2bib-original-loop.log \
	original-loop.tex original-loop.bib

clean	:
	\rm -f *.bib
	\rm -f doc.{log,aux,gls,glo,glg,ist,pdf}
