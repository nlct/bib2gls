all	: bib2gls.1 convertgls2bib.1 bib2gls.pdf bib2gls-begin.pdf 
	@egrep '(Package glossaries-extra Warning:|unreferenced destination)' bib2gls*.log || true
	@grep Warning *.glg || true

bib2gls.pdf	: bib2gls.tex bib2gls.bib bib2gls-terms.bib bib2gls-cite.bib\
		   ../lib/bib2gls.jar ../lib/texparserlib.jar \
		   ../examples/*.tex ../examples/*.bib version.tex
		make -C ../examples
		arara --verbose bib2gls
		@egrep '(Package glossaries-extra Warning:|unreferenced destination)' bib2gls.log || true
		@grep Warning bib2gls.glg || true

bib2gls-begin.pdf	: bib2gls-begin.tex bib2gls.bib bib2gls-cite.bib version.tex
			arara --verbose bib2gls-begin
		@egrep '(Package glossaries-extra Warning:|unreferenced destination)' bib2gls-begin.log || true
		@grep Warning bib2gls-begin.glg || true

version.tex	: ../java/bib2gls/Bib2Gls.java
		@echo "\\date{Version " > version.tex
		@grep 'String VERSION = ' ../java/bib2gls/Bib2Gls.java | sed "s/public\sstatic\sfinal\sString\sVERSION\s=//" | tr -d "\"\; " >> version.tex
		@grep 'String DATE = ' ../java/bib2gls/Bib2Gls.java | sed "s/public\sstatic\sfinal\sString\sDATE\s=//" | tr -d "\"\; " >> version.tex
		@echo "}%" >> version.tex

bib2gls.1	: bib2gls.pod
		pod2man -c bib2gls bib2gls.pod bib2gls.1

convertgls2bib.1	: convertgls2bib.pod
		pod2man -c convertgls2bib convertgls2bib.pod convertgls2bib.1

clean	:
	\rm -f bib2gls*.{aux,log,toc,lot,lof,ilg,ind,idx,out,pdf,1,glstex,glg,blg,bbl}
	\rm -f arara.xml
