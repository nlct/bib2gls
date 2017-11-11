BIB2GLS=../bin/bib2gls --debug --verbose --group
DEPS=../lib/bib2gls.jar ../lib/texparserlib.jar

all	: sample-chemical.pdf sample-units1.pdf sample-units2.pdf \
	sample-units3.pdf sample-people.pdf sample-bacteria.pdf \
	sample-media.pdf sample-authors.pdf sample-maths.pdf

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

sample-people.pdf	: $(DEPS) sample-people.tex people.bib \
	no-interpret-preamble.bib interpret-preamble.bib
	pdflatex sample-people
	$(BIB2GLS) sample-people --break-space
	pdflatex sample-people

sample-bacteria.pdf	: $(DEPS) sample-bacteria.tex bacteria.bib
	pdflatex sample-bacteria
	$(BIB2GLS) sample-bacteria
	pdflatex sample-bacteria

sample-media.pdf	: $(DEPS) sample-media.tex \
	books.bib films.bib no-interpret-preamble.bib interpret-preamble.bib
	pdflatex sample-media
	$(BIB2GLS) sample-media
	pdflatex sample-media

sample-authors.pdf	: $(DEPS) sample-authors.tex \
	books.bib people.bib no-interpret-preamble.bib interpret-preamble2.bib
	pdflatex sample-authors
	$(BIB2GLS) sample-authors
	pdflatex sample-authors
	pdflatex sample-authors

sample-maths.pdf	: $(DEPS) sample-maths.tex \
	bigmathsymbols.bib mathgreek.bib
	pdflatex sample-maths
	$(BIB2GLS) sample-maths
	pdflatex sample-maths
	pdflatex sample-maths

clean	:
	\rm -f *.{glg,glstex,aux,log,out,toc,pdf}
