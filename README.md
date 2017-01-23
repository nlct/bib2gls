# bib2gls
Command line application to convert `.bib` files to `glossaries-extra.sty` resource file

Experimental, still in development. Following on
from [my answer on TeX on
StackExchange](http://tex.stackexchange.com/a/343852/19862).

Requires at least v1.12 of
[glossaries-extra.sty](http://ctan.org/pkg/glossaries-extra)
which is pending release, and at least Java 7.

The basic idea is to have a `.bib` file containing all glossary
definitions in the form:
```bibtex
@entry{sample,
  name={sample},
  description="An example entry"
}

@abbreviation{html,
  short="html",
  long={hypertext markup language}
}

@index{goose,plural="geese"}

@symbol{pi,
  name="{}\ensuremath{\pi}"
}

```
It may then be possible to manage the entries in a reference
management system such as JabRef.

The document must load `glossaries-extra` with the `record` option
and either `\glsxtrresourcefile[`_options_`]{`_base_`}` or
`\GlsXtrLoadResources[`_options_`]` (which is a shortcut for
`\glsxtrresourcefile[`_options_`]{\jobname}`). Only one
`\GlsXtrLoadResources` is permitted but multiple
`\glsxtrresourcefile` instances are allowed.

If the `.bib` file is called `test-entries.bib`, then here's an
example document:
```latex
\documentclass{article}

\usepackage[record]{glossaries-extra}

\glsxtrresourcefile{test-entries}

\begin{document}
\gls{sample} \gls{goose}.

\printunsrtglossaries
\end{document}
```
The document build process is (assuming the file is called
`mydoc.tex`):
```bash
pdflatex mydoc
bib2gls mydoc
pdflatex mydoc
```

The call to `bib2gls` reads `mydoc.aux` to find out what `.bib` file to load
and which entries have been used in the document, and then creates the file 
`test-entries.glstex` which is loaded by `\glsxtrresourcefile` on
the next `pdflatex` run. Only the entries that have been used in the
document `sample` and `goose` are selected from the `.bib` file and
written to the `.glstex` file. You can have different names for the
`.glstex` file and the `.bib` file. The `.glstex` file name is
obtained from the mandatory argument of `\glsxtrresourcefile`. The
`.bib` file is taken from the value of the `src` option. If the
option is missing, the same base name as the `.glstex` file is
assumed. For example:
```latex
\glsxtrresourcefile[src=test-entries]{\jobname}
```
In this case, `bib2gls` parses the file `test-entries.bib` and creates a file 
called `mydoc.glstex` (since `\jobname` is `mydoc` for the file `mydoc.tex`).
There's a shortcut command that uses `\jobname` as the mandatory
argument of `\glsxtrresourcefile`:
```latex
\GlsXtrLoadResources[src=test-entries]
```
A document may only have one instance of `\GlsXtrLoadResources` but
any number of `\glsxtrresourcefile`. Just make sure that each
`\glsxtrresourcefile` has a different mandatory argument.

The entries are sorted before being written to the `.glstex` file,
which means that they don't need to be sorted by `makeindex` or
`xindy`. The default method of sorting is alphabetically according to the OS's
locale. You can override this in the optional argument of
`\glsxtrresourcefile` (or `\GlsXtrLoadResources`).

For example:
```latex
\GlsXtrLoadResources[src=test-entries,sort=none]
```
This won't sort the entries whereas the following will sort
according to use:
```latex
\GlsXtrLoadResources[src=test-entries,sort=use]
```

The value of `src` is a comma-separated list, so you can select over
multiple `.bib` files. For example:
```latex
\GlsXtrLoadResources[src={entries-terms,entries-abbrv}]
```

You can assign the entries to a particular glossary type, using
the `type` option. For example:
```latex
\documentclass{article}

\usepackage[record,abbreviations,symbols]{glossaries-extra}

\glsxtrresourcefile[type=main,src={entries-terms}]{terms}
\glsxtrresourcefile[type=abbreviations,src={entries-abbrv}]{abbrvs}
\glsxtrresourcefile[type=symbols,sort=use,src={entries-symbols}]{syms}

\begin{document}
\gls{sample} \gls{html} \gls{pi}.

\printunsrtglossaries
\end{document}
```

Note that you can't use `\glsadd` all with this method, as the
entries must be defined in order to iterate over the glossary list.
On the first instance of `pdflatex` the `.glstex` hasn't been
created, so no entries have been defined, which means there's
nothing to iterate over. Instead, if you want to select all entries
in the `.bib` file, use `selection=all`. For example
```latex
\GlsXtrLoadResources[src={test-entries},selection=all]
```
(You can't use `selection=all` with `sort=use`.)

If you want to add any new glossary fields, make sure you put
all instances of `\glsaddstoragekey` or `\glsaddkey` before you load
the resource file, since `bib2gls` will pick up the allowed keys
from the `.aux` file and will ignore any additional fields in the
`.bib` file. For example, if the `.bib` file contains the entry:
```bibtex
@entry{sample,
  name={sample},
  description="An example entry",
  note={sample note}
}
```
Then the `note` field will be omitted from the `.glstex` file unless
a new `note` key is defined before `\glsxtrresourcefile` (or
`\GlsXtrLoadResources`).

You can, of course, define the keys afterwards if you don't want
those fields selected, but that rather defeats the purpose of
defining the keys. You can tell `bib2gls` to ignore particular fields 
that are present in the `.bib` file using the `ignore-fields` option.
For example, suppose my `.bib` file contains:
```bibtex
@abbreviation{html,
  short = {html},
  long  = {hypertext markup language},
  description={a markup language for creating web pages}
}
```
This is appropriate for abbreviation styles such as `long-short-desc`, but 
the `description` field interferes with styles like `long-short`. If
I want to use `long-short`, then I can just use
`ignore-fields={description}` to skip the `description` field.

Any cross-references (through the `see` field or through
commands like `\gls` within the entry's fields) will be picked up by
`bib2gls` as it parses the `.bib` file and will be automatically
added to the selection list unless overridden by the `selection`
setting. Note that an extra document build will be required to
ensure the cross-referenced entries have the correct location list.
(This is necessary because the location list can't be determined
until the entry has been referenced when it's displayed in the
glossary and the glossary can't be displayed until the entries have
been selected from the `.bib` file and the `.glstex` file has been loaded 
by the document.)

If your entries have a `see` field, you can determine whether you
want the cross-referenced list to appear before or after the
location list or whether you want it completely omitted (but still
add the cross-referenced terms to the list).

If you want to implement different sort methods, just use a separate
`\glsxtrresourcefile` for each type.

The following example assumes that the file `terms-en.bib` contains English 
terms and `terms-de.bib` contains German terms:
```latex
\newglossary*{english}{English Terms}
\newglossary*{german}{German Terms}

\glsxtrresourcefile[sort=en,type=english]{terms-en}
\glsxtrresourcefile[sort=de-1996,type=german]{terms-de}
```
This sorts the English terms according to the English alphabet and
the German terms according to the new German orthography. The value
of the `sort` key may be `none` (or `unsrt`), `use` (order of use 
in the document), `letter-case` (sort by case-sensitive character
code), `letter-nocase` (case-insensitive letter sort), `locale` (use
the OS's default language), or a valid IETF language tag.

You can change the field to sort by using `sort-field`. This may
take the value `id` (to sort by the entry label) or the field name.
For example, to sort by the `category` field using a case-sensitive letter sort:
```latex
\glsxtrresourcefile[sort=letter-case,sort-field=category]{test-entries}
```

If your indexing rules are too complex for `bib2gls`, you can just
use `bib2gls` to select the entries from the `.bib` file and use
`xindy` as usual with a custom `xindy` module. Just remember to use
`record=alsoindex` instead of just `record` and use `\makeglossaries`
and `\printglossary`/`\printglossaries` as usual. This has the
advantage over just using `\loadglsentries` as only those entries
that have actually been used in the document will be defined. If you
have a file containing, say, 500 entries but you only actually use
10 in the document, it's a waste of resources to define all 500 of
them.

# Installation

The files should be installed as follows:

 - `TEXMF/scripts/bib2gls/bib2gls.sh` (Unix-like systems only)
 - `TEXMF/scripts/bib2gls/bib2gls.jar`
 - `TEXMF/scripts/bib2gls/texparserlib.jar`
 - `TEXMF/scripts/bib2gls/resources/bib2gls-en.xml`
 - `TEXMF/doc/support/bib2gls/README.md`
 - `TEXMF/doc/support/bib2gls/bib2gls.pdf`
 - `TEXMF/doc/support/bib2gls/bib2gls.tex`

Unix-like systems add a symbolic link called `bib2gls` somewhere on
your path that links to the bib2gls.sh bash script.

Windows users may find that their TeX distribution has converted the
`.jar` file to an executable `bib2gls.exe`. If not, you can create a
batch script analogous to `bib2gls.sh` called `bib2gls.bat` that
contains the following:
```com
@ECHO OFF
FOR /F %%I IN ('kpsewhich --progname=bib2gls --format=texmfscripts bib2gls.jar') DO SET JARPATH=%%I
java -Djava.locale.providers=CLDR,JRE -jar "%JARPATH%" %*
```

# Compile Source Code

Create sub-directories `src/lib` and
`src/classes/com/dickimawbooks/bib2gls`

Requires `texparserlib.jar` which can be compiled from
[texparser](https://github.com/nlct/texparser).

Copy `texparserlib.jar` to `src/lib`.

Compile from `src` using:

```bash 
cd java
javac -d ../classes -cp ../lib/texparserlib.jar *.java
cd ../classes
jar cmf ../java/Manifest.txt ../lib/bib2gls.jar com/dickimawbooks/bib2gls/*.class
```

No warranty etc.
