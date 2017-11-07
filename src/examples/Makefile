BIB2GLS=../bin/bib2gls --debug --verbose --group
DEPS=../lib/bib2gls.jar ../lib/texparserlib.jar

all	: sample-chemical.pdf sample-units1.pdf sample-units2.pdf \
	sample-units3.pdf sample-people.pdf sample-bacteria.pdf

sample-chemical.pdf	: $(DEPS) sample-chemical.tex chemicalformula.bib
	pdflatex sample-chemical
	$(BIB2GLS) sample-chemical
	pdflatex sample-chemical

sample-units1.pdf	: $(DEPS) sample-units1.tex baseunits.bib derivedunits.bib
	pdflatex sample-units1
	$(BIB2GLS) sample-units1
	pdflatex sample-units1

sample-units2.pdf	: $(DEPS) sample-units2.tex baseunits.bib derivedunits.bib
	pdflatex sample-units2
	$(BIB2GLS) sample-units2
	pdflatex sample-units2

sample-units3.pdf	: $(DEPS) sample-units3.tex baseunits.bib derivedunits.bib
	pdflatex sample-units3
	$(BIB2GLS) sample-units3
	pdflatex sample-units3
	pdflatex sample-units3

sample-people.pdf	: $(DEPS) sample-people.tex people.bib
	pdflatex sample-people
	$(BIB2GLS) sample-people --break-space
	pdflatex sample-people

sample-bacteria.pdf	: $(DEPS) sample-bacteria.tex bacteria.bib
	pdflatex sample-bacteria
	$(BIB2GLS) sample-bacteria
	pdflatex sample-bacteria

clean	:
	\rm -f *.{glg,glstex,aux,log,out,toc,pdf}
