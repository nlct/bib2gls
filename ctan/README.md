# bib2gls

Command line application to convert `.bib` files to
`glossaries-extra.sty` resource files.

Author: Nicola Talbot (https://www.dickimaw-books.com/contact)

Application Home Page: https://www.dickimaw-books.com/software/bib2gls

Version details of the current installation can be obtained with
`--version` or `-v`:

```bash
bib2gls --version
convertgls2bib --version
datatool2bib --version
```

## Licence

Copyright (C) 2017-2025 Nicola L. C. Talbot (dickimaw-books.com)

License GPLv3+: GNU GPL version 3 or later
http://gnu.org/licenses/gpl.html
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.

## Requirements

  - Java Runtime Environment (at least Java 8).

  - TeX distribution that includes `glossaries-extra.sty` 
    and dependent packages (such as `glossaries.sty`).

## Summary

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

##Supplementary applications

### `convertgls2bib`

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

## Example

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

### `datatool2bib`.

The supplementary application `datatool2bib` can be used to convert
`datatool` databases to `.bib` files suitable for use with `bib2gls`.

## Related Resources

  - [bib2gls FAQ](https://www.dickimaw-books.com/faq.php?category=bib2gls).
  - [bib2gls gallery](https://www.dickimaw-books.com/gallery/#bib2gls).

TUGboat articles:

  - Glossaries with bib2gls, issue [40:1, 2019](http://tug.org/TUGboat/Contents/contents40-1.html). 
    [(PDF)](http://tug.org/TUGboat/tb40-1/tb124talbot-bib2gls.pdf)
  - bib2gls: selection, cross-references and locations, issue [41:3, 2020](http://tug.org/TUGboat/Contents/contents41-3.html).
    [(PDF)](http://tug.org/TUGboat/tb41-3/tb129talbot-bib2gls-more.pdf)
  - bib2gls: sorting, issue [42:2, 2021](http://tug.org/TUGboat/Contents/contents42-2.html). [(PDF)](https://tug.org/TUGboat/tb42-2/tb131talbot-sorting.pdf)
  - bib2gls: standalone entries and repeated lists (a little book of
    poisons), issue [43:1, 2022](https://tug.org/TUGboat/Contents/contents43-1.html).
    [(PDF)](https://tug.org/TUGboat/tb43-1/tb133talbot-bib2gls-reorder.pdf)

## Installation

The files should be installed as follows where *TEXMF* indicates
your local or home TEXMF path (for example, `~/texmf/`):

 - *TEXMF*`/scripts/bib2gls/bib2gls.jar` (Java application.)
 - *TEXMF*`/scripts/bib2gls/convertgls2bib.jar` (Java application.)
 - *TEXMF*`/scripts/bib2gls/datatool2bib.jar` (Java application.)
 - *TEXMF*`/scripts/bib2gls/bibglscommon.jar` (Java library
   providing code shared by `bib2gls` and the conversion tools.)
 - *TEXMF*`/scripts/bib2gls/texparserlib.jar` (Java library.)
 - *TEXMF*`/scripts/bib2gls/resources/bib2gls-en.xml` (main English
   resource file.)
 - *TEXMF*`/scripts/bib2gls/resources/bib2gls-extra-en.xml` 
   (supplementary English resource file.)
 - *TEXMF*`/scripts/bib2gls/resources/bib2gls-extra-nl.xml` 
   (supplementary Dutch resource file.)
 - *TEXMF*`/scripts/bib2gls/bib2gls.sh` (Unix-like systems only.)
 - *TEXMF*`/scripts/bib2gls/convertgls2bib.sh` (Unix-like systems
   only.)
 - *TEXMF*`/scripts/bib2gls/datatool2bib.sh` (Unix-like systems
   only.)
 - *TEXMF*`/doc/support/bib2gls/bib2gls.pdf` (User manual.)
 - *TEXMF*`/doc/support/bib2gls/bib2gls-begin.pdf` (Introductory Guide.)
 - *TEXMF*`/doc/support/bib2gls/examples/` (example files)

Note that `texparserlib.jar` isn't an application. It's
a library used by `bib2gls.jar` and the `*2bib` conversion tools
and so needs to be on the same class path as them.

The bash `.sh` scripts are provided for Unix-like users.
They're not required for Windows. The `.1` files are `man`
files and should be placed where `man` can find them. (They
are created from the `.pod` files.)

To test installation:
```bash
bib2gls --version
convertgls2bib --version
```
These should display the version details.

## Source Code

Instructions for compiling the manual and jar files are listed
below. The source is also available on GitHub, but that may be for
a newer experimental version.

  - https://github.com/nlct/bib2gls
  - https://github.com/nlct/texparser


### User Manual (bib2gls.pdf)

The examples directory needs to be ../examples relative to directory
containing bib2gls.tex as the .bib, .tex and .pdf files are included
in the manual.

```bash
xelatex bib2gls
bibtex bib2gls
bib2gls -g bib2gls
xelatex bib2gls
bib2gls -g bib2gls
xelatex bib2gls
xelatex bib2gls
```

Similarly for the bib2gls-begin.pdf document.

### JAR Files

Create the following directories:

`lib`
`lib/resources`
`java`
`classes/com/dickimawbooks/bib2gls`
`classes/com/dickimawbooks/gls2bib`
`classes/com/dickimawbooks/texparserlib`

Unpack the zip files:

```bash
unzip -d java bib2gls-src.zip
unzip -d java gls2bib-src.zip
unzip -d java texparserlib-src.zip
```

Copy the `.xml` language file to `lib/resources/`

Compile `texparserlib.jar`:

```bash
cd java/lib 
javac -d ../../classes
-Xlint:unchecked -Xlint:deprecation *.java */*.java */*/*.java
cd ../../classes 
jar cf ../lib/texparserlib.jar 
com/dickimawbooks/texparserlib/*.class \
com/dickimawbooks/texparserlib/*/*.class \
com/dickimawbooks/texparserlib/*/*/*.class 
```

Compile `bib2gls.jar`:

```bash
cd java/bib2gls
javac -d ../../classes -cp ../../lib/texparserlib.jar *.java
cd ../classes
jar cmf ../java/bib2gls/Manifest.txt ../lib/bib2gls.jar
com/dickimawbooks/bib2gls/*.class
```

Compile `convertgls2bib.jar`:

```bash
cd java/gls2bib
javac -d ../../classes -cp ../../lib/texparserlib.jar *.java
cd ../classes
jar cmf ../java/gls2bib/Manifest.txt ../lib/convertgls2bib.jar
com/dickimawbooks/gls2bib/*.class
```
