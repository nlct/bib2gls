BIB2GLS=../bin/bib2gls --debug --verbose --group
DEPS=../lib/bib2gls.jar ../lib/texparserlib.jar

all	: sample-chemical.pdf sample-units1.pdf sample-units2.pdf \
	sample-units3.pdf sample-people.pdf sample-bacteria.pdf \
	sample-media.pdf sample-authors.pdf sample-msymbols.pdf \
	sample-maths.pdf sample-textsymbols.pdf sample-usergroups.pdf \
	sample-languages.pdf sample-multi1.pdf sample-multi2.pdf

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

sample-msymbols.pdf	: $(DEPS) sample-msymbols.tex \
	bigmathsymbols.bib mathgreek.bib mathsrelations.bib \
	binaryoperators.bib unaryoperators.bib
	pdflatex sample-msymbols
	$(BIB2GLS) sample-msymbols
	pdflatex sample-msymbols
	pdflatex sample-msymbols

sample-maths.pdf	: $(DEPS) sample-maths.tex \
	no-interpret-preamble.bib interpret-preamble.bib \
	bigmathsymbols.bib mathsobjects.bib
	pdflatex sample-maths
	$(BIB2GLS) sample-maths
	pdflatex sample-maths

sample-textsymbols.pdf	: $(DEPS) sample-textsymbols.tex \
	miscsymbols.bib
	pdflatex sample-textsymbols
	$(BIB2GLS) sample-textsymbols
	pdflatex sample-textsymbols

sample-usergroups.pdf	: $(DEPS) sample-usergroups.tex \
	usergroups.bib
	xelatex sample-usergroups
	$(BIB2GLS) sample-usergroups
	xelatex sample-usergroups

sample-languages.pdf	: $(DEPS) sample-languages.tex \
	markuplanguages.bib
	pdflatex sample-languages
	$(BIB2GLS) sample-languages
	pdflatex sample-languages

sample-multi1.pdf	: $(DEPS) sample-multi1.tex \
	bacteria.bib markuplanguages.bib minerals.bib \
	vegetables.bib animals.bib chemicalformula.bib \
	baseunits.bib derivedunits.bib
	pdflatex sample-multi1
	$(BIB2GLS) sample-multi1
	pdflatex sample-multi1

sample-multi2.pdf	: $(DEPS) sample-multi2.tex \
	bacteria.bib markuplanguages.bib minerals.bib \
	vegetables.bib animals.bib chemicalformula.bib \
	baseunits.bib derivedunits.bib terms.bib
	pdflatex sample-multi2
	$(BIB2GLS) --break-space sample-multi2
	pdflatex sample-multi2
	$(BIB2GLS) --break-space sample-multi2
	pdflatex sample-multi2

clean	:
	\rm -f *.{glg,glstex,aux,log,out,toc,pdf}
