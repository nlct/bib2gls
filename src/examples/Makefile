BIB2GLS=../bin/bib2gls --debug --verbose --group
DEPS=../lib/bib2gls.jar

all	: sample-chemical.pdf

sample-chemical.pdf	: $(DEPS) sample-chemical.tex chemicalformula.bib
	pdflatex sample-chemical
	$(BIB2GLS) sample-chemical
	pdflatex sample-chemical

clean	:
	\rm -f *.{glg,glstex,aux,log,out,toc,pdf}
