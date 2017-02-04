# bib2gls
Command line application to convert `.bib` files to `glossaries-extra.sty` resource file

Experimental, still in development. Following on
from [my answer on TeX on
StackExchange](http://tex.stackexchange.com/a/343852/19862).

Requires at least Java 7 and at least v1.12 of
[glossaries-extra.sty](http://ctan.org/pkg/glossaries-extra)
(2017-02-03).

There's a draft version of the user manual
in the `unstable` directory, which is provided for those 
interested in more detail.

# Testing the Experimental Version

If you want to test the experimental version, download the
files listed below and follow the installation instructions.
Make sure you have at least v1.12 of 
[`glossaries-extra`](http://ctan.org/pkg/glossaries-extra)
and at least Java 7.

 - [`bib2gls.jar`](https://github.com/nlct/bib2gls/raw/master/unstable/bib2gls.jar)
 - [`texparserlib.jar`](https://github.com/nlct/bib2gls/raw/master/unstable/texparserlib.jar)
 - [`bib2gls.sh`](https://github.com/nlct/bib2gls/raw/master/src/bin/bib2gls.sh) (bash users only)
 - [`resources/bib2gls-en.xml`](https://github.com/nlct/bib2gls/raw/master/src/lib/resources/bib2gls-en.xml)

There are some test files available in the
[`src/tests`](https://github.com/nlct/bib2gls/tree/master/src/tests)
directory.

# Installation

The files should be installed as follows where *TEXMF* indicates
your local or home TEXMF path (for example, `~/texmf/`):

 - *TEXMF*`/scripts/bib2gls/bib2gls.sh` (Unix-like systems only)
 - *TEXMF*`/scripts/bib2gls/bib2gls.jar`
 - *TEXMF*`/scripts/bib2gls/texparserlib.jar`
 - *TEXMF*`/scripts/bib2gls/resources/bib2gls-en.xml`
 - *TEXMF*`/doc/support/bib2gls/bib2gls.pdf`

Unix-like systems add a symbolic link called `bib2gls` somewhere on
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
