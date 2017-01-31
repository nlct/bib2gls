BIB2GLS=../bin/bib2gls --debug --verbose
DEPS=../lib/bib2gls.jar
RM=\rm -f

all	: test.pdf test-utf8.pdf test-xr.pdf test-crossrefs.pdf \
	  test-xe.pdf test-label-prefix.pdf test-dual.pdf \
	  test-external-refs.pdf test-hierarchical.pdf \
	  test-match.pdf test-secondary.pdf test-ascii.pdf \
	  test-pifonts.pdf test-dual-abbrv.pdf \
	  test-dual-abbrv-desc.pdf test-numbers.pdf \
	  test-book.pdf test-article.pdf

../lib/bib2gls.jar:	../java/*.java
		make -C ..

test.pdf	: test.glstex
		pdflatex test

test.glstex	: test.tex $(DEPS) test-entries.bib
		pdflatex test
		$(BIB2GLS) test

test-utf8.pdf	: test-utf8.glstex
		xelatex test-utf8

test-utf8.glstex	: test-utf8.tex $(DEPS) test-utf8.bib
		xelatex test-utf8
		$(BIB2GLS) --group test-utf8

test-xr.pdf	: test-xr.glstex
		pdflatex test-xr

test-xr.glstex	: test-xr.tex $(DEPS) test-entries.bib
		pdflatex test-xr
		$(BIB2GLS) test-xr

test-crossrefs.pdf	: test-crossrefs.glstex
		pdflatex test-crossrefs

test-crossrefs.glstex	: test-crossrefs.tex $(DEPS) \
			  entries-terms.bib \
			  entries-abbrv.bib \
			  entries-symbols.bib
		pdflatex test-crossrefs
		$(BIB2GLS) test-crossrefs

test-xe.pdf	: test-xe.glstex
		xelatex test-xe

test-xe.glstex	: test-xe.tex $(DEPS) test-entries.bib
		xelatex test-xe
		$(BIB2GLS) test-xe

test-label-prefix.pdf	: test-label-prefix.glstex
		pdflatex test-label-prefix

test-label-prefix.glstex	: test-label-prefix.tex $(DEPS) \
	 			test-entries.bib
		pdflatex test-label-prefix
		$(BIB2GLS) test-label-prefix

test-dual.pdf	: test-dual.glstex
		pdflatex test-dual

test-dual.glstex	: test-dual.tex $(DEPS) \
	 			entries-dual.bib
		pdflatex test-dual
		$(BIB2GLS) test-dual

test-external-refs.pdf	: test-external-refs.glstex
		pdflatex test-external-refs

test-external-refs.glstex	: test-external-refs.tex $(DEPS) \
	 			entries-terms.bib entries-symbols.bib
		pdflatex test-external-refs
		$(BIB2GLS) test-external-refs

test-hierarchical.pdf	: test-hierarchical.glstex
		pdflatex test-hierarchical

test-hierarchical.glstex	: test-hierarchical.tex $(DEPS) \
	 			hierarchical.bib
		pdflatex test-hierarchical
		$(BIB2GLS) test-hierarchical

test-match.pdf	: test-match.glstex
		pdflatex test-match

test-match.glstex	: test-match.tex $(DEPS) topics.bib
		pdflatex test-match
		$(BIB2GLS) test-match

test-secondary.pdf	: test-secondary.glstex
		pdflatex test-secondary

test-secondary.glstex	: test-secondary.tex $(DEPS) \
			entries-symbols.bib entries-terms.bib
		pdflatex test-secondary
		$(BIB2GLS) test-secondary

test-ascii.pdf	: test-ascii.glstex
		pdflatex test-ascii

test-ascii.glstex	: test-ascii.tex $(DEPS) test-ascii.bib
		pdflatex test-ascii
		$(BIB2GLS) --group test-ascii

test-pifonts.pdf	: test-pifonts.glstex
		pdflatex test-pifonts

test-pifonts.glstex	: test-pifonts.tex $(DEPS) test-pifonts.bib
		pdflatex test-pifonts
		$(BIB2GLS) test-pifonts

test-multipar.pdf	: test-multipar.glstex
		pdflatex test-multipar

test-multipar.glstex	: test-multipar.tex $(DEPS) entries-multipar.bib
		pdflatex test-multipar
		$(BIB2GLS) test-multipar

test-dual-abbrv.pdf	: test-dual-abbrv.glstex
		pdflatex test-dual-abbrv

test-dual-abbrv.glstex	: test-dual-abbrv.tex $(DEPS) entries-dual-abbrv.bib
		pdflatex test-dual-abbrv
		$(BIB2GLS) test-dual-abbrv

test-dual-abbrv-desc.pdf	: test-dual-abbrv-desc.glstex
		pdflatex test-dual-abbrv-desc

test-dual-abbrv-desc.glstex	: test-dual-abbrv-desc.tex $(DEPS) entries-dual-abbrv-desc.bib
		pdflatex test-dual-abbrv-desc
		$(BIB2GLS) test-dual-abbrv-desc

test-numbers.pdf	: test-numbers.glstex
		pdflatex test-numbers

test-numbers.glstex	: test-numbers.tex $(DEPS) test-numbers.bib
		pdflatex test-numbers
		$(BIB2GLS) test-numbers

test-book.pdf	: test-book.glstex
		pdflatex test-book

test-book.glstex	: test-book.tex $(DEPS) entries-multipar.bib
		pdflatex test-book
		$(BIB2GLS) test-book

test-article.pdf	: test-book.pdf $(DEPS) test-article.tex
			pdflatex test-article
			$(BIB2GLS) test-article
			pdflatex test-article

clean	:
	$(RM) *.aux
	$(RM) *.log
	$(RM) *.pdf
	$(RM) *.glstex
	$(RM) *.glg
	$(RM) *.out
	$(RM) *.toc
