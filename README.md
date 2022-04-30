# bib2gls
Command line application to convert `.bib` files to `glossaries-extra.sty` resource file

Related resources: [bib2gls FAQ](https://www.dickimaw-books.com/faq.php?category=bib2gls) and [bib2gls gallery](https://www.dickimaw-books.com/gallery/#bib2gls).

Related TUGboat articles:

  - [Glossaries with bib2gls](http://tug.org/TUGboat/tb40-1/tb124talbot-bib2gls.pdf), issue [40:1, 2019](http://tug.org/TUGboat/Contents/contents40-1.html).
  - [bib2gls: selection, cross-references and locations](http://tug.org/TUGboat/tb41-3/tb129talbot-bib2gls-more.pdf), issue [41:3, 2020](http://tug.org/TUGboat/Contents/contents41-3.html).
  - bib2gls: sorting, issue [42:2, 2021](http://tug.org/TUGboat/Contents/contents42-2.html).

(This application was developed as a follow-up from [my answer on TeX on
StackExchange](http://tex.stackexchange.com/a/343852/19862).)

The bib2gls tool forms part of a LaTeX document build, performing two
functions in one: glossary information is fetched from one or
more `.bib` files by examining the `.aux` file (similar to BibTeX)
and the terms are then sorted hierarchically and the locations collated into
compact lists (similar to Makeindex/Xindy). A `.glstex` file 
is then created containing the data defined in terms of
`\longnewglossaryentry*` or `\newabbreviation` (provided by
`glossaries-extra.sty`) in the appropriate order. This file
is input by `\GlsXtrLoadResources` (which also writes the required
settings to the `.aux` file for `bib2gls`). The glossary
can then simply by displayed with `\printunsrtglossary`. The
`.bib` files can be managed in an application like JabRef.
You may prefer to start with the [introductory guide](http://mirrors.ctan.org/support/bib2gls/bib2gls-begin.pdf)
before moving on to the [main user manual](http://mirrors.ctan.org/support/bib2gls/bib2gls.pdf).

This application requires at least Java 8 and at least v1.12 of
[glossaries-extra.sty](http://ctan.org/pkg/glossaries-extra)
(2017-02-03) and at least v4.04 of [glossaries.sty](http://ctan.org/pkg/glossaries). (Although newer versions are recommended, 
and may be required for some features.)
The main home page is [`dickimaw-books.com/software/bib2gls`](http://www.dickimaw-books.com/software/bib2gls/).

The latest stable version of `bib2gls` is available from [CTAN](https://ctan.org/pkg/bib2gls).
The code in this GitHub repository may be for an unstable version.
Stable versions have a version number in the form _major_._minor_.
Unstable versions have a version number in the form _major_._minor_._YYYYMMDD_. 

If you already have a `.tex` file containing lots of
definitions using `\newglossaryentry`,
`\newacronym` etc, you can convert it to a `.bib`
file using the supplementary command line application
`convertgls2bib`. For example, if the original definitions
are in `entries.tex` then
```bash
convertgls2bib entries.tex entries.bib
```
will create the file `entries.bib` containing all the definitions
found in `entries.tex`. Other information in `entries.tex` should be
ignored, but command definitions will be parsed. (Avoid any code
that's overly complicated. The TeX parser library isn't a
TeX engine!)

The `.bib` format doesn't support spaces in labels, so if your
`.tex` file has spaces in labels use `--space-sub` _replacement_
to substitute the spaces with _replacement_. For example
```bash
convertgls2bib --space-sub '-' entries.tex entries.bib
```
will replace spaces with hyphens or
```bash
convertgls2bib --space-sub '' entries.tex entries.bib
```
will strip spaces. The values of the `see`, `seealso` and
`alias` fields will also be adjusted, but any references using
`\gls` etc will have to be replaced manually (or use your
text editor's search and replace function).

# Example Document

File `entries.bib`:

```bibtex
% Encoding UTF-8

@entry{bird,
  name={bird},
  description = {feathered animal}
}

@abbreviation{html,
  short="html",
  long={hypertext markup language}
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

\usepackage[record]{glossaries-extra}

\GlsXtrLoadResources[
  src={entries},% data in entries.bib
  sort={en-GB},% sort according to 'en-GB' locale
]

\begin{document}
\Gls{bird} and \gls{goose}.

\printunsrtglossaries
\end{document}
```

Document build:

```bash
pdflatex myDoc
bib2gls myDoc
pdflatex myDoc
```

Or, if letter groups are required:

```bash
pdflatex myDoc
bib2gls -g myDoc
pdflatex myDoc
```

# Installation

First ensure that you have Java installed.

Now that `bib2gls` is available in both TeX Live and MiKTeX,
the best installation method is via your TeX package manager.
Below are instructions for manual installation.

The files should be installed as follows where *TEXMF* indicates
your local or home TEXMF path (for example, `~/texmf/`):

 - *TEXMF*`/scripts/bib2gls/bib2gls.sh` (Unix-like systems only)
 - *TEXMF*`/scripts/bib2gls/bib2gls.jar`
 - *TEXMF*`/scripts/bib2gls/convertgls2bib.sh` (Unix-like systems only)
 - *TEXMF*`/scripts/bib2gls/convertgls2bib.jar`
 - *TEXMF*`/scripts/bib2gls/texparserlib.jar`
 - *TEXMF*`/scripts/bib2gls/resources/bib2gls-en.xml`
 - *TEXMF*`/doc/support/bib2gls/bib2gls.pdf`

For Unix-like systems, add a symbolic link called `bib2gls` somewhere on
your path that links to the `bib2gls.sh` bash script.
For example:
```bash
cd ~/bin
ln -s ~/texmf/scripts/bib2gls/bib2gls.sh bib2gls
```

To test the installation run the following from your command 
prompt or terminal:
```bash
bib2gls --version
```
If you get the following message:
```
Fatal error: Can't find language resource file.
```
then check that the `resources` sub-directory has been correctly
copied over.

Windows users may find that their TeX distribution has converted the
`bib2gls.jar` file to an executable `bib2gls.exe`. If not, you can create a
batch script analogous to `bib2gls.sh` called `bib2gls.bat` that
contains the following:
```com
@ECHO OFF
FOR /F "tokens=*" %%I IN ('kpsewhich --progname=bib2gls --format=texmfscripts bib2gls.jar') DO SET JARPATH=%%I
java -Djava.locale.providers=CLDR,JRE -jar "%JARPATH%" %*
```
(Similarly for `convertgls2bib.jar`.)

# Compile Source Code

Create sub-directories `src/lib`,
`src/classes/com/dickimawbooks/bib2gls` and
`src/classes/com/dickimawbooks/gls2bib`

Requires `texparserlib.jar` which can be compiled from
[texparser](https://github.com/nlct/texparser).

Copy `texparserlib.jar` to `src/lib`.

Compile from `src` using:

```bash 
cd java/bib2gls
javac -d ../../classes -cp ../../lib/texparserlib.jar *.java
cd ../classes
jar cmf ../java/bib2gls/Manifest.txt ../lib/bib2gls.jar com/dickimawbooks/bib2gls/*.class
```

Similarly for `gls2bib`. The actual `.jar` file is called
`convertgls2bib.jar` to reduce the chances of accidentally
using the wrong application.

```bash 
cd java/gls2bib
javac -d ../../classes -cp ../../lib/texparserlib.jar *.java
cd ../classes
jar cmf ../java/gls2bib/Manifest.txt ../lib/convertgls2bib.jar com/dickimawbooks/gls2bib/*.class
```

License GPLv3+: GNU GPL version 3 or later
http://gnu.org/licenses/gpl.html
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.

