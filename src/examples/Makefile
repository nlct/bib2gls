BIB2GLS=../bin/bib2gls --verbose --log-encoding ISO-8859-1
DEPS=../lib/bib2gls.jar ../lib/texparserlib.jar

all	: sample-chemical.pdf sample-units1.pdf sample-units2.pdf \
	sample-units3.pdf sample-people.pdf sample-bacteria.pdf \
	sample-media.pdf sample-authors.pdf sample-msymbols.pdf \
	sample-maths.pdf sample-textsymbols.pdf sample-textsymbols2.pdf \
	sample-constants.pdf sample-usergroups.pdf \
	sample-markuplanguages.pdf sample-multi1.pdf sample-multi2.pdf \
	sample-citations.pdf sample-hierarchical.pdf sample-nested.pdf
	@grep Exception *.glg || true

sample-hierarchical.pdf	: $(DEPS) sample-hierarchical.tex terms.bib animals.bib minerals.bib vegetables.bib
	pdflatex sample-hierarchical
	$(BIB2GLS) --group sample-hierarchical
	pdflatex sample-hierarchical

sample-nested.pdf	: $(DEPS) sample-nested.tex terms.bib animals.bib minerals.bib vegetables.bib
	pdflatex sample-nested
	$(BIB2GLS) --group --provide-glossaries sample-nested
	pdflatex sample-nested

sample-chemical.pdf	: $(DEPS) sample-chemical.tex chemicalformula.bib
	pdflatex sample-chemical
	$(BIB2GLS) --group sample-chemical
	pdflatex sample-chemical

sample-units1.pdf	: $(DEPS) sample-units1.tex baseunits.bib derivedunits.bib
	pdflatex sample-units1
	$(BIB2GLS) --group sample-units1
	pdflatex sample-units1

sample-units2.pdf	: $(DEPS) sample-units2.tex baseunits.bib derivedunits.bib
	pdflatex sample-units2
	$(BIB2GLS) --group sample-units2
	pdflatex sample-units2

sample-units3.pdf	: $(DEPS) sample-units3.tex baseunits.bib derivedunits.bib
	pdflatex sample-units3
	$(BIB2GLS) --group sample-units3
	pdflatex sample-units3
	pdflatex sample-units3

sample-people.pdf	: $(DEPS) sample-people.tex people.bib \
	no-interpret-preamble.bib interpret-preamble.bib
	pdflatex sample-people
	$(BIB2GLS) --group --break-space sample-people
	pdflatex sample-people

sample-bacteria.pdf	: $(DEPS) sample-bacteria.tex bacteria.bib
	pdflatex sample-bacteria
	$(BIB2GLS) --group sample-bacteria
	pdflatex sample-bacteria

sample-media.pdf	: $(DEPS) sample-media.tex \
	books.bib films.bib no-interpret-preamble.bib interpret-preamble.bib
	pdflatex sample-media
	$(BIB2GLS) --group sample-media
	pdflatex sample-media

sample-authors.pdf	: $(DEPS) sample-authors.tex \
	books.bib people.bib no-interpret-preamble.bib interpret-preamble2.bib
	pdflatex sample-authors
	$(BIB2GLS) --group sample-authors
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

sample-textsymbols2.pdf	: $(DEPS) sample-textsymbols2.tex \
	miscsymbols.bib topics.bib
	pdflatex sample-textsymbols2
	$(BIB2GLS) --group sample-textsymbols2
	pdflatex sample-textsymbols2

sample-usergroups.pdf	: $(DEPS) sample-usergroups.tex \
	usergroups.bib
	xelatex sample-usergroups
	$(BIB2GLS) --group sample-usergroups
	xelatex sample-usergroups

sample-markuplanguages.pdf	: $(DEPS) sample-markuplanguages.tex \
	markuplanguages.bib
	pdflatex sample-markuplanguages
	$(BIB2GLS) --group sample-markuplanguages
	pdflatex sample-markuplanguages

sample-multi1.pdf	: $(DEPS) sample-multi1.tex \
	bacteria.bib markuplanguages.bib minerals.bib \
	vegetables.bib animals.bib chemicalformula.bib \
	baseunits.bib derivedunits.bib
	pdflatex sample-multi1
	$(BIB2GLS) --group sample-multi1
	pdflatex sample-multi1
	$(BIB2GLS) --group sample-multi1
	pdflatex sample-multi1

sample-multi2.pdf	: $(DEPS) sample-multi2.tex \
	bacteria.bib markuplanguages.bib minerals.bib \
	vegetables.bib animals.bib chemicalformula.bib \
	baseunits.bib derivedunits.bib terms.bib
	pdflatex sample-multi2
	$(BIB2GLS) --group --break-space sample-multi2
	pdflatex sample-multi2
	$(BIB2GLS) --group --break-space sample-multi2
	pdflatex sample-multi2

sample-constants.pdf	: $(DEPS) sample-constants.tex constants.bib
	pdflatex sample-constants
	$(BIB2GLS) sample-constants
	pdflatex sample-constants

sample-citations.pdf	: $(DEPS) sample-citations.tex citations.bib
	pdflatex sample-citations
	bibtex sample-citations
	$(BIB2GLS) --cite-as-record sample-citations
	pdflatex sample-citations
	pdflatex sample-citations

clean	:
	\rm -f *.{glg,glstex,aux,log,out,toc,pdf,bbl,blg}
