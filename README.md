# bib2gls
Command line application to convert `.bib` files to `glossaries-extra.sty` resource file

(Developed as a follow-up
from [my answer on TeX on
StackExchange](http://tex.stackexchange.com/a/343852/19862).)

This application requires at least Java 7 and at least v1.12 of
[glossaries-extra.sty](http://ctan.org/pkg/glossaries-extra)
(2017-02-03). (Although newer versions are recommended.)
The main home page is http://www.dickimaw-books.com/software/bib2gls/
which includes links to binaries.

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

# Installation

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

