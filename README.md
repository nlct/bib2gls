# bib2gls
Command line application to convert `.bib` files to `glossaries-extra.sty` resource file

Experimental, still in development. Following on
from [my answer on TeX on
StackExchange](http://tex.stackexchange.com/a/343852/19862).

Requires at least Java 7 and at least v1.12 of
[glossaries-extra.sty](http://ctan.org/pkg/glossaries-extra)
which is *pending release*.
Version 1.12 of glossaries-extra is still unstable and not
yet ready for release.

There's a draft version of the (unfinished) user manual
in the `unstable` directory, which is provided for those 
interested in more detail.

Once the application is ready, the installation instructions
are below. They are here for future reference.

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
