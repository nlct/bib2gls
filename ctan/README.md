# bib2gls

Command line application to convert `.bib` files to
`glossaries-extra.sty` resource files.

This application may be used to extract glossary information
stored in a `.bib` file and convert it into glossary entry
definition commands. This application should be used 
with `glossaries-extra`'s `record` package option. It performs
two functions:

  - selects entries according to records found in the `.aux` file
    (similar to `bibtex`)

  - hierarchically sorts entries and collates location lists
    (similar to `makeindex` or `xindy`)

This means that the glossary entries can now be managed
in a system such as JabRef, and only the entries that are
actually required will be defined, reducing the resources
required by TeX.

The supplementary application `convertgls2bib` can be used
to convert existing `.tex` files containing definitions
(`\newglossaryentry` etc) to the `.bib` format
required by `bib2gls`.

For example, if `entries.tex` contains:
```latex
\newglossaryentry{bird}{name={bird},description={feathered animal}}

\newabbreviation{html}{HTML}{Hypertext Markup Language}

\newterm[plural=geese]{goose}

\newdualentry{svm}{SVM}{support vector machine}
  {Statistical pattern recognition technique}
```
Then do:
```bash
convertgls2tex entries.tex entries.bib
```
to create `entries.bib`.

# Example

File `entries.bib`:

```bibtex
@entry{bird,
  name={bird},
  description = {feathered animal}
}

@abbreviation{html,
  short="HTML",
  long={hypertext markup language}
}

@dualentryabbreviation{svm,
   long = {support vector machine},
   short = {SVM},
   description = {statistical pattern recognition technique}
}

@symbol{v,
  name={$\vec{v}$},
  text={\vec{v}},
  description={a vector}
}

@index{goose,plural="geese"}
```

File `myDoc.tex`:

```latex
\documentclass{article}

\usepackage[record,abbreviations]{glossaries-extra}

\GlsXtrLoadResources[
  src={entries},% data in entries.bib
  sort={en-GB}% sort according to 'en-GB' locale
]

\begin{document}
\Gls{bird} and \gls{goose}.

First: \gls{svm}. Next: \gls{svm}.

\printunsrtglossaries
\end{document}
```

Document build:
```bash
pdflatex myDoc
bib2gls myDoc
pdflatex myDoc
```
(Replace `pdflatex` with `latex`, `xelatex` or `lualatex` as
appropriate.)

# Installation

The files should be installed as follows where *TEXMF* indicates
your local or home TEXMF path (for example, `~/texmf/`):

 - *TEXMF*`/scripts/bib2gls/bib2gls.sh` (Unix-like systems only.)
 - *TEXMF*`/scripts/bib2gls/bib2gls.jar` (Java application.)
 - *TEXMF*`/scripts/bib2gls/convertgls2bib.sh` (Unix-like systems
   only.)
 - *TEXMF*`/scripts/bib2gls/convertgls2bib.jar` (Java application.)
 - *TEXMF*`/scripts/bib2gls/texparserlib.jar` (Java library.)
 - *TEXMF*`/scripts/bib2gls/resources/bib2gls-en.xml` (English
   resource file.)
 - *TEXMF*`/doc/support/bib2gls/bib2gls.pdf` (User manual.)

Note that `texparserlib.jar` isn't an application. It's
a library used by `bib2gls.jar` and `convertgls2bib.jar`
and so needs to be on the same class path as them.

The bash `.sh` scripts are provided for Unix-like users.
They're not required for Windows.

To test installation:
```bash
bib2gls --version
convertgls2bib --version
```
These should display the version details.

GitHub:

  - https://github.com/nlct/bib2gls
  - https://github.com/nlct/texparserlib

Author's Home Page: www.dickimaw-books.com
