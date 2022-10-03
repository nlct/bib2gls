BIB2GLS=../bin/bib2gls --debug --verbose --group
DEPS=../lib/bib2gls.jar ../lib/texparserlib.jar
RM=\rm -f

all	: test.pdf test-utf8.pdf test-xr.pdf test-crossrefs.pdf \
	  test-xe.pdf test-label-prefix.pdf test-dual.pdf \
	  test-external-refs.pdf test-hierarchical.pdf \
	  test-match.pdf test-secondary.pdf test-ascii.pdf \
	  test-pifonts.pdf test-dual-abbrv.pdf \
	  test-dual-abbrv-desc.pdf test-numbers.pdf \
	  test-book.pdf test-article.pdf test-dual-symbols.pdf \
	  test-alias.pdf test-dep.pdf test-ranges.pdf test-counters.pdf \
	  test-supp.pdf test-supp2.pdf test\ supp3.pdf test-supp-main.pdf test-random.pdf \
	  test-glsignore.pdf test-dual-entryabbrv.pdf \
	  test-match-entrytype.pdf test-digraphs.pdf \
	  sample-testidx.pdf sample-testidx-alsoindex.pdf \
	  sample-testidx-xindy.pdf test-children.pdf \
	  test-cjk.pdf test-other.pdf test-homograph.pdf \
	  test-abbrv.pdf test-hypergroups.pdf test-dups.pdf \
	  test-dates.pdf test-people.pdf test-entrytypemap.pdf \
	  test-dualindexentry.pdf test-dualindexentry2.pdf \
	  test-dual-mix.pdf test-letternumber.pdf \
	  test-numeric.pdf test-copy.pdf test-seetrail.pdf \
	  test-record-prefix.pdf test-labelify.pdf \
	  test-labelify-fontspec.pdf test-custom-symbols.pdf \
	  test-crossresource-refs.pdf test-custom-rules.pdf \
	  test-same-as-base.pdf test-books.pdf test-bibtexdata.pdf \
	  test-indexcounter.pdf test-dualentryabbrv.pdf test-indexcounter2.pdf \
	  test-indexplural.pdf test-include.pdf \
	  test-primary-records.pdf test-primary-records-counter.pdf \
	  test-cyrillic-ascii.pdf test-cyrillic-inputenc.pdf \
	  test-dual2.pdf test-mfirstuc.pdf test-translations.pdf \
	  test-sort-labels.pdf test-lettersort-examples.pdf \
	  test-sortletternumber.pdf test-spawn.pdf \
	  test-interpret-fields.pdf test-interpret-fields2.pdf \
	  test-prefix.pdf test-dual-prefix.pdf test-prefix2.pdf \
	  sample-recordcount.pdf test-innergloss.pdf \
	  test-custom-sort-fallback.pdf test-dual-custom-fallback.pdf \
	  test-from-xr.pdf test-nodeps-see.pdf test-standalone-range.pdf \
	  test-dual-samename.pdf test-mgls.pdf test-mgls2.pdf \
	  test-hypernav-targetprefix.pdf test-hiergroups.pdf \
	  test-mergegroups.pdf test-break-at.pdf test-prune-xr.pdf \
	  test-glslike.pdf test-xr-recordcount.pdf test-emptyloc.pdf
	@grep Exception *.glg || true
	@grep -E 'glossaries(-extra)? Warning' *.log || true

../lib/bib2gls.jar:	../java/bib2gls/*.java
		make -C ..

test.pdf	: test.glstex
		pdflatex test
		pdflatex test
		pdflatex test

test.glstex	: test.tex $(DEPS) test-entries.bib
		pdflatex test
		$(BIB2GLS) test
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
		$(BIB2GLS) test-crossrefs
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

test-dual-symbols.pdf	: test-dual-symbols.glstex
		pdflatex test-dual-symbols

test-dual-symbols.glstex	: test-dual-symbols.tex $(DEPS) entries-dual-symbols.bib
		pdflatex test-dual-symbols
		$(BIB2GLS) test-dual-symbols

test-alias.pdf	: test-alias.glstex
		pdflatex test-alias

test-alias.glstex	: test-alias.tex $(DEPS) test-alias.bib
		pdflatex test-alias
		$(BIB2GLS) test-alias

test-dep.pdf	: test-dep.glstex
		pdflatex test-dep

test-dep.glstex	: test-dep.tex $(DEPS) test-dep.bib
		pdflatex test-dep
		$(BIB2GLS) test-dep

test-ranges.pdf	: test-ranges.glstex
		pdflatex test-ranges

test-ranges.glstex	: test-ranges.tex $(DEPS) test-entries.bib
		pdflatex test-ranges
		$(BIB2GLS) test-ranges

test-counters.pdf	: test-counters.glstex
		pdflatex test-counters

test-counters.glstex	: test-counters.tex $(DEPS) test-entries.bib
		pdflatex test-counters
		$(BIB2GLS) test-counters

test-supp.pdf	: test-supp.glstex
		pdflatex test-supp

test-supp.glstex	: test-supp.tex $(DEPS) test-entries.bib
		pdflatex test-supp
		$(BIB2GLS) test-supp

test-supp2.pdf	: test-supp2.glstex
		pdflatex test-supp2

test-supp2.glstex	: test-supp2.tex $(DEPS) test-entries.bib
		pdflatex test-supp2
		$(BIB2GLS) test-supp2

test\ supp3.pdf	: test\ supp3.glstex
		pdflatex test\ supp3

test\ supp3.glstex	: test\ supp3.tex $(DEPS) test-entries.bib
		pdflatex test\ supp3
		$(BIB2GLS) test\ supp3

test-supp-main.pdf	: test-supp-main.glstex
		pdflatex test-supp-main

test-supp-main.glstex	: test-supp-main.tex test-supp.pdf test-supp2.pdf $(DEPS) test-entries.bib
		pdflatex test-supp-main
		$(BIB2GLS) test-supp-main

test-random.pdf	: test-random.glstex
		pdflatex test-random

test-random.glstex	: test-random.tex $(DEPS) test-ascii.bib
		pdflatex test-random
		$(BIB2GLS) test-random

test-glsignore.pdf	: test-glsignore.glstex
		pdflatex test-glsignore

test-glsignore.glstex	: test-glsignore.tex $(DEPS) test-entries.bib
		pdflatex test-glsignore
		$(BIB2GLS) test-glsignore

test-dual-entryabbrv.pdf	: test-dual-entryabbrv.glstex
		pdflatex test-dual-entryabbrv

test-dual-entryabbrv.glstex	: test-dual-entryabbrv.tex $(DEPS) entries-dual-entryabbrv.bib
		pdflatex test-dual-entryabbrv
		$(BIB2GLS) test-dual-entryabbrv

test-match-entrytype.pdf	: test-match-entrytype.glstex
		pdflatex test-match-entrytype
		pdflatex test-match-entrytype
		pdflatex test-match-entrytype

test-match-entrytype.glstex	: test-match-entrytype.tex $(DEPS) entries.bib
		pdflatex test-match-entrytype
		$(BIB2GLS) test-match-entrytype

test-digraphs.pdf	: test-digraphs.glstex
		xelatex test-digraphs

test-digraphs.glstex	: test-digraphs.tex $(DEPS) test-digraphs.bib
		xelatex test-digraphs
		$(BIB2GLS) test-digraphs

sample-testidx.pdf	: sample-testidx.tex $(DEPS)
			arara sample-testidx

sample-testidx-alsoindex.pdf	: sample-testidx-alsoindex.tex $(DEPS)
			arara sample-testidx-alsoindex

sample-testidx-xindy.pdf	: sample-testidx-xindy.tex
			arara sample-testidx-xindy

test-children.pdf	: test-children.glstex
		pdflatex test-children

test-children.glstex	: test-children.tex $(DEPS) test-children.bib
		pdflatex test-children
		$(BIB2GLS) test-children

test-cjk.pdf	: test-cjk.glstex
		pdflatex test-cjk

test-cjk.glstex	: test-cjk.tex $(DEPS) testcjk.bib
		pdflatex test-cjk
		$(BIB2GLS) test-cjk

test-other.pdf	: test-other.glstex
		pdflatex test-other

test-other.glstex	: test-other.tex $(DEPS) test-other.bib
		pdflatex test-other
		$(BIB2GLS) test-other

test-homograph.pdf	: test-homograph.glstex
		pdflatex test-homograph

test-homograph.glstex	: test-homograph.tex $(DEPS) test-homograph.bib
		pdflatex test-homograph
		$(BIB2GLS) test-homograph

test-abbrv.pdf	: test-abbrv.glstex
		pdflatex test-abbrv

test-abbrv.glstex	: test-abbrv.tex $(DEPS) entries-abbrv.bib
		pdflatex test-abbrv
		$(BIB2GLS) test-abbrv

test-hypergroups.pdf	: test-hypergroups.glstex
		pdflatex test-hypergroups

test-hypergroups.glstex	: test-hypergroups.tex $(DEPS)
		pdflatex test-hypergroups
		$(BIB2GLS) test-hypergroups

test-dups.pdf	: test-dups.glstex
		pdflatex test-dups

test-dups.glstex	: test-dups.tex test-dups.bib $(DEPS)
		pdflatex test-dups
		$(BIB2GLS) test-dups

test-dates.pdf	: test-dates.glstex
		pdflatex test-dates

test-dates.glstex	: test-dates.tex test-dates.bib test-datetimes.bib test-times.bib $(DEPS)
		pdflatex test-dates
		$(BIB2GLS) test-dates

test-people.pdf	: test-people.glstex
		pdflatex test-people

test-people.glstex	: test-people.tex people.bib $(DEPS)
		pdflatex test-people
		$(BIB2GLS) test-people

test-entrytypemap.pdf	: test-entrytypemap.glstex
		pdflatex test-entrytypemap

test-entrytypemap.glstex	: test-entrytypemap.tex entries-abbrv.bib $(DEPS)
		pdflatex test-entrytypemap
		$(BIB2GLS) test-entrytypemap

test-dualindexentry.pdf	: test-dualindexentry.glstex
		pdflatex test-dualindexentry

test-dualindexentry.glstex	: test-dualindexentry.tex entries-dual-index-entry.bib $(DEPS)
		pdflatex test-dualindexentry
		$(BIB2GLS) test-dualindexentry

test-dualindexentry2.pdf	: test-dualindexentry2.glstex
		pdflatex test-dualindexentry2

test-dualindexentry2.glstex	: test-dualindexentry2.tex entries-dual-index-entry.bib $(DEPS)
		pdflatex test-dualindexentry2
		$(BIB2GLS) test-dualindexentry2

test-dual-mix.pdf	: test-dual-mix.glstex
		pdflatex test-dual-mix

test-dual-mix.glstex	: test-dual-mix.tex test-dual-mix2.bib $(DEPS)
		pdflatex test-dual-mix
		$(BIB2GLS) test-dual-mix

test-letternumber.pdf	: test-letternumber.glstex
		xelatex test-letternumber

test-letternumber.glstex	: test-letternumber.tex test-letternumber.bib $(DEPS)
		xelatex test-letternumber
		$(BIB2GLS) test-letternumber

test-numeric.pdf	: test-numeric.glstex
		pdflatex test-numeric

test-numeric.glstex	: test-numeric.tex test-numeric.bib $(DEPS)
		pdflatex test-numeric
		$(BIB2GLS) test-numeric

test-copy.pdf	: test-copy.glstex
		pdflatex test-copy

test-copy.glstex	: test-copy.tex test-entries.bib $(DEPS)
		pdflatex test-copy
		$(BIB2GLS) test-copy

test-seetrail.pdf	: test-seetrail.glstex
		pdflatex test-seetrail

test-seetrail.glstex	: test-seetrail.tex test-seetrail.bib $(DEPS)
		pdflatex test-seetrail
		$(BIB2GLS) test-seetrail

test-record-prefix.pdf	: test-record-prefix.glstex
		pdflatex test-record-prefix

test-record-prefix.glstex	: test-record-prefix.tex test-entries.bib $(DEPS)
		pdflatex test-record-prefix
		$(BIB2GLS) test-record-prefix

test-labelify.pdf	: test-labelify.glstex
		pdflatex test-labelify

test-labelify.glstex	: test-labelify.tex test-labelify.bib $(DEPS)
		pdflatex test-labelify
		$(BIB2GLS) test-labelify

test-labelify-fontspec.pdf	: test-labelify-fontspec.glstex
		xelatex test-labelify-fontspec

test-labelify-fontspec.glstex	: test-labelify-fontspec.tex test-labelify.bib $(DEPS)
		xelatex test-labelify-fontspec
		$(BIB2GLS) test-labelify-fontspec

test-custom-symbols.pdf	: test-custom-symbols.glstex
		pdflatex test-custom-symbols

test-custom-symbols.glstex	: test-custom-symbols.tex test-custom-symbols.bib $(DEPS)
		pdflatex test-custom-symbols
		$(BIB2GLS) test-custom-symbols

test-crossresource-refs.pdf	: test-crossresource-refs.glstex
		pdflatex test-crossresource-refs

test-crossresource-refs.glstex	: test-crossresource-refs.tex test-xr1.bib test-xr1.bib $(DEPS)
		pdflatex test-crossresource-refs
		$(BIB2GLS) --force-cross-resource-refs test-crossresource-refs

test-custom-rules.pdf	: test-custom-rules.glstex
		xelatex test-custom-rules

test-custom-rules.glstex	: test-custom-rules.tex anglo-saxon.bib greek.bib upgreek.bib $(DEPS)
		xelatex test-custom-rules
		$(BIB2GLS) test-custom-rules

test-same-as-base.pdf	: test-same-as-base.glstex
		pdflatex test-same-as-base

test-same-as-base.glstex	: test-same-as-base.tex bacteria.bib topics.bib $(DEPS)
		pdflatex test-same-as-base
		$(BIB2GLS) test-same-as-base

test-books.pdf	: test-books.glstex
		pdflatex test-books

test-books.glstex	: test-books.tex books.bib $(DEPS)
		pdflatex test-books
		$(BIB2GLS) test-books

test-bibtexdata.pdf	: test-bibtexdata.glstex
		pdflatex test-bibtexdata
		pdflatex test-bibtexdata

test-bibtexdata.glstex	: test-bibtexdata.tex $(DEPS)
		pdflatex test-bibtexdata
		$(BIB2GLS) --cite-as-record test-bibtexdata
		bibtex test-bibtexdata

test-indexcounter.pdf	: test-indexcounter.glstex
		pdflatex test-indexcounter

test-indexcounter.glstex	: test-indexcounter.tex test-entries.bib $(DEPS)
		pdflatex test-indexcounter
		$(BIB2GLS) test-indexcounter

test-dualentryabbrv.pdf	: test-dualentryabbrv.glstex
		pdflatex test-dualentryabbrv

test-dualentryabbrv.glstex	: test-dualentryabbrv.tex entries-dualentryabbrv.bib $(DEPS)
		pdflatex test-dualentryabbrv
		$(BIB2GLS) test-dualentryabbrv

test-indexcounter2.pdf	: test-indexcounter2.glstex
		pdflatex test-indexcounter2
		pdflatex test-indexcounter2

test-indexcounter2.glstex	: test-indexcounter2.tex $(DEPS)
		pdflatex test-indexcounter2
		$(BIB2GLS) test-indexcounter2

test-indexplural.pdf	: test-indexplural.glstex
		pdflatex test-indexplural

test-indexplural.glstex	: test-indexplural.tex indexplural.bib $(DEPS)
		pdflatex test-indexplural
		$(BIB2GLS) test-indexplural

test-include.pdf	: test-include.glstex
		pdflatex test-include

test-include.glstex	: test-include.tex test-include-subfile1.tex test-include-subfile2.tex test-entries.bib $(DEPS)
		pdflatex test-include
		$(BIB2GLS) test-include

test-primary-records.pdf	: test-primary-records.glstex
		pdflatex test-primary-records

test-primary-records.glstex	: test-primary-records.tex $(DEPS) topics.bib
		pdflatex test-primary-records
		$(BIB2GLS) test-primary-records
		pdflatex test-primary-records
		$(BIB2GLS) test-primary-records

test-primary-records-counter.pdf	: test-primary-records-counter.glstex
		pdflatex test-primary-records-counter

test-primary-records-counter.glstex	: test-primary-records-counter.tex $(DEPS) topics.bib
		pdflatex test-primary-records-counter
		$(BIB2GLS) test-primary-records-counter
		pdflatex test-primary-records-counter
		$(BIB2GLS) test-primary-records-counter

test-cyrillic-ascii.pdf	: test-cyrillic-ascii.glstex
		pdflatex test-cyrillic-ascii

test-cyrillic-ascii.glstex	: test-cyrillic-ascii.tex $(DEPS) cyrillic-ascii.bib
		pdflatex test-cyrillic-ascii
		$(BIB2GLS) test-cyrillic-ascii

test-cyrillic-inputenc.pdf	: test-cyrillic-inputenc.glstex
		pdflatex test-cyrillic-inputenc

test-cyrillic-inputenc.glstex	: test-cyrillic-inputenc.tex $(DEPS) cyrillic-utf8.bib
		pdflatex test-cyrillic-inputenc
		$(BIB2GLS) test-cyrillic-inputenc

test-dual2.pdf	: test-dual2.glstex
		pdflatex test-dual2

test-dual2.glstex	: test-dual2.tex test-dual-mix2.bib $(DEPS)
		pdflatex test-dual2
		$(BIB2GLS) test-dual2

test-mfirstuc.pdf	: test-mfirstuc.glstex
		pdflatex test-mfirstuc

test-mfirstuc.glstex	: test-mfirstuc.tex test-mfirstuc.bib $(DEPS)
		pdflatex test-mfirstuc
		$(BIB2GLS) --mfirstuc-protection 'long,short,name' --packages mfirstuc-english test-mfirstuc

test-translations.pdf	: test-translations.glstex
		pdflatex test-translations
		pdflatex test-translations

test-translations.glstex	: test-translations.tex entries-en.bib entries-pt.bib $(DEPS)
		pdflatex test-translations
		$(BIB2GLS) test-translations

test-sort-labels.pdf	: test-sort-labels.glstex
		pdflatex test-sort-labels

test-sort-labels.glstex	: test-sort-labels.tex test-sort-labels.bib $(DEPS)
		pdflatex test-sort-labels
		$(BIB2GLS) test-sort-labels

test-lettersort-examples.pdf	: test-lettersort-examples.glstex
		pdflatex test-lettersort-examples

test-lettersort-examples.glstex	: test-lettersort-examples.tex test-lettersort-examples.bib $(DEPS)
		pdflatex test-lettersort-examples
		$(BIB2GLS) test-lettersort-examples

test-sortletternumber.pdf	: test-sortletternumber.glstex
		pdflatex test-sortletternumber

test-sortletternumber.glstex	: test-sortletternumber.tex test-sortletternumber.bib $(DEPS)
		pdflatex test-sortletternumber
		$(BIB2GLS) test-sortletternumber

test-spawn.pdf	: test-spawn.glstex
		pdflatex test-spawn

test-spawn.glstex	: test-spawn.tex test-spawn.bib $(DEPS)
		pdflatex test-spawn
		$(BIB2GLS) test-spawn

test-interpret-fields.pdf	: test-interpret-fields.glstex
		xelatex test-interpret-fields
		xelatex test-interpret-fields

test-interpret-fields.glstex	: test-interpret-fields.tex greek.bib $(DEPS)
		xelatex test-interpret-fields
		$(BIB2GLS) test-interpret-fields

test-interpret-fields2.pdf	: test-interpret-fields2.glstex
		pdflatex test-interpret-fields2
		pdflatex test-interpret-fields2

test-interpret-fields2.glstex	: test-interpret-fields2.tex greek.bib $(DEPS)
		pdflatex test-interpret-fields2
		$(BIB2GLS) test-interpret-fields2

test-blocks.pdf	: test-blocks.glstex
		pdflatex test-blocks

test-blocks.glstex	: test-blocks.tex blocks.bib $(DEPS)
		pdflatex test-blocks
		$(BIB2GLS) --no-group test-blocks

test-prefix.pdf	: test-prefix.glstex
		pdflatex test-prefix

test-prefix.glstex	: test-prefix.tex test-prefix.bib $(DEPS)
		pdflatex test-prefix
		$(BIB2GLS) test-prefix

test-prefix2.pdf	: test-prefix2.glstex
		pdflatex test-prefix2

test-prefix2.glstex	: test-prefix2.tex entries-dual-index-entry.bib $(DEPS)
		pdflatex test-prefix2
		$(BIB2GLS) test-prefix2

test-dual-prefix.pdf	: test-dual-prefix.glstex
		pdflatex test-dual-prefix

test-dual-prefix.glstex	: test-dual-prefix.tex test-dual-prefix.bib $(DEPS)
		pdflatex test-dual-prefix
		$(BIB2GLS) test-dual-prefix

sample-recordcount.pdf	: sample-recordcount.glstex
		pdflatex sample-recordcount

sample-recordcount.glstex	: sample-recordcount.tex $(DEPS)
		pdflatex sample-recordcount
		$(BIB2GLS) --record-count sample-recordcount

test-innergloss.pdf	: test-innergloss.glstex
		pdflatex test-innergloss

test-innergloss.glstex	: test-innergloss.tex $(DEPS)
		pdflatex test-innergloss
		$(BIB2GLS) --no-group --provide-glossaries test-innergloss

test-custom-sort-fallback.pdf	: test-custom-sort-fallback.glstex
		lualatex test-custom-sort-fallback

test-custom-sort-fallback.glstex	: test-custom-sort-fallback.tex test-custom-sort-fallback.bib $(DEPS)
		lualatex test-custom-sort-fallback
		$(BIB2GLS) test-custom-sort-fallback

test-dual-custom-fallback.pdf	: test-dual-custom-fallback.glstex
		lualatex test-dual-custom-fallback

test-dual-custom-fallback.glstex	: test-dual-custom-fallback.tex test-dual-custom-fallback.bib $(DEPS)
		lualatex test-dual-custom-fallback
		$(BIB2GLS) test-dual-custom-fallback

test-from-xr.pdf	: test-from-xr.glstex
		pdflatex test-from-xr

test-from-xr.glstex	: test-from-xr.tex test-from-xr.bib $(DEPS)
		pdflatex test-from-xr
		$(BIB2GLS) test-from-xr

test-nodeps-see.pdf	: test-nodeps-see.glstex
		pdflatex test-nodeps-see

test-nodeps-see.glstex	: test-nodeps-see.tex test-nodeps-see.bib $(DEPS)
		pdflatex test-nodeps-see
		$(BIB2GLS) test-nodeps-see

test-standalone-range.pdf	: test-standalone-range.glstex
		pdflatex test-standalone-range

test-standalone-range.glstex	: test-standalone-range.tex $(DEPS)
		pdflatex test-standalone-range
		$(BIB2GLS) test-standalone-range

test-dual-samename.pdf	: test-dual-samename.glstex
		pdflatex test-dual-samename

test-dual-samename.glstex	: test-dual-samename.tex test-dual-samename.bib $(DEPS)
		pdflatex test-dual-samename
		$(BIB2GLS) test-dual-samename

test-mgls.pdf	: test-mgls.glstex
		pdflatex test-mgls

test-mgls.glstex	: test-mgls.tex test-mgls.bib $(DEPS)
		pdflatex test-mgls
		$(BIB2GLS) test-mgls

test-mgls2.pdf	: test-mgls2.glstex
		pdflatex test-mgls2

test-mgls2.glstex	: test-mgls2.tex test-mgls2.bib $(DEPS)
		pdflatex test-mgls2
		$(BIB2GLS) test-mgls2

test-hypernav-targetprefix.pdf	: test-hypernav-targetprefix.glstex
		pdflatex test-hypernav-targetprefix
		pdflatex test-hypernav-targetprefix

test-hypernav-targetprefix.glstex	: test-hypernav-targetprefix.tex $(DEPS)
		pdflatex test-hypernav-targetprefix
		$(BIB2GLS) test-hypernav-targetprefix

test-hiergroups.pdf	: test-hiergroups.glstex
			pdflatex test-hiergroups
			pdflatex test-hiergroups

test-hiergroups.glstex	: test-hiergroups.tex $(DEPS)
			pdflatex test-hiergroups
			$(BIB2GLS) test-hiergroups

test-mergegroups.pdf	: test-mergegroups.glstex
			pdflatex test-mergegroups
			pdflatex test-mergegroups

test-mergegroups.glstex	: test-mergegroups.tex $(DEPS)
			pdflatex test-mergegroups
			$(BIB2GLS) test-mergegroups

test-break-at.pdf	: test-break-at.glstex
			pdflatex test-break-at
			pdflatex test-break-at

test-break-at.glstex	: test-break-at.tex test-break-at.bib $(DEPS)
			pdflatex test-break-at
			$(BIB2GLS) test-break-at

test-prune-xr.pdf	: test-prune-xr.glstex
			pdflatex test-prune-xr
			pdflatex test-prune-xr

test-prune-xr.glstex	: test-prune-xr.tex test-prune-xr.bib $(DEPS)
			pdflatex test-prune-xr
			$(BIB2GLS) test-prune-xr

test-glslike.pdf	: test-glslike.glstex
			pdflatex test-glslike
			pdflatex test-glslike

test-glslike.glstex	: test-glslike.tex test-glslike.bib $(DEPS)
			pdflatex test-glslike
			$(BIB2GLS) test-glslike

test-xr-recordcount.pdf	: test-xr-recordcount.glstex
			pdflatex test-xr-recordcount
			pdflatex test-xr-recordcount

test-xr-recordcount.glstex	: test-xr-recordcount.tex test-xr-recordcount.bib $(DEPS)
			pdflatex test-xr-recordcount
			$(BIB2GLS) test-xr-recordcount

test-emptyloc.pdf	: test-emptyloc.glstex
			pdflatex test-emptyloc
			pdflatex test-emptyloc

test-emptyloc.glstex	: test-emptyloc.tex entries.bib $(DEPS)
			pdflatex test-emptyloc
			$(BIB2GLS) test-emptyloc

clean	:
	$(RM) *.aux
	$(RM) *.log
	$(RM) *.pdf
	$(RM) *.glstex
	$(RM) *.glg
	$(RM) *.glo
	$(RM) *.gls
	$(RM) *.xdy
	$(RM) *.out
	$(RM) *.toc
