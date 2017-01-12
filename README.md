# bib2gls
Command line application to convert .bib files to glossaries-extra.sty resource file

Experimental, still in development. Following on
from [my answer on TeX on
StackExchange](http://tex.stackexchange.com/a/343852/19862).

Requires at least v1.11 of
[glossaries-extra.sty](http://ctan.org/pkg/glossaries-extra), which is
also still being developed.

# Compile Source Code

Create sub-directory `src/lib`.

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
