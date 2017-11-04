BIB2GLS=../bin/bib2gls --debug --verbose --group
DEPS=../lib/bib2gls.jar ../lib/texparserlib.jar

all	: sample-chemical.pdf sample-units.pdf

sample-chemical.pdf	: $(DEPS) sample-chemical.tex chemicalformula.bib
	pdflatex sample-chemical
	$(BIB2GLS) sample-chemical
	pdflatex sample-chemical

sample-units.pdf	: $(DEPS) sample-units.tex baseunits.bib derivedunits.bib
	pdflatex sample-units
	$(BIB2GLS) sample-units
	pdflatex sample-units
	pdflatex sample-units

clean	:
	\rm -f *.{glg,glstex,aux,log,out,toc,pdf}
