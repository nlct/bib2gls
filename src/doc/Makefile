all	: bib2gls.man1.pdf convertgls2bib.man1.pdf datatool2bib.man1.pdf bib2gls.pdf bib2gls-begin.pdf 
	@grep -E '(Package glossaries-extra Warning:|unreferenced destination)' bib2gls*.log || true
	@grep -E '(Warning|Exception)' *.glg ../examples/*.glg || true

bib2gls.pdf	: bib2gls.tex bib2gls.bib bib2gls-terms.bib bib2gls-cite.bib\
		   ../lib/bib2gls.jar ../lib/texparserlib.jar \
		   ../examples/*.tex ../examples/*.bib version.tex
		make -C ../examples
		arara --verbose bib2gls
		@grep -E '(Package glossaries-extra Warning:|unreferenced destination)' bib2gls.log || true
		@grep -E '(Warning|Exception)' bib2gls.glg || true

bib2gls.glstex	: bib2gls.tex bib2gls.bib bib2gls-terms.bib bib2gls-cite.bib\
		   ../lib/bib2gls.jar ../lib/texparserlib.jar \
		   ../examples/*.tex ../examples/*.bib version.tex
		lualatex bib2gls
		bib2gls --group --packages mfirstuc-english bib2gls


bib2gls-begin.pdf	: bib2gls-begin.tex bib2gls.bib bib2gls-cite.bib version.tex
			arara --verbose bib2gls-begin
		@grep -E '(Package glossaries-extra Warning:|unreferenced destination)' bib2gls-begin.log || true
		@grep -E '(Warning|Exception)' bib2gls-begin.glg || true

version.tex	: ../java/common/BibGlsTeXApp.java
		@echo "\\date{Version " > version.tex
		@grep 'String VERSION = ' ../java/common/BibGlsTeXApp.java | sed "s/public\sstatic\sfinal\sString\sVERSION\s=//" | tr -d "\"\; " >> version.tex
		@grep 'String DATE = ' ../java/common/BibGlsTeXApp.java | sed "s/public\sstatic\sfinal\sString\sDATE\s=//" | tr -d "\"\; " >> version.tex
		@echo "}%" >> version.tex

bib2gls.man1.pdf	: bib2gls.1
		mandoc -Tpdf -l bib2gls.1 >bib2gls.man1.pdf

bib2gls.1	: bib2gls.pod
		pod2man -c bib2gls bib2gls.pod bib2gls.1

convertgls2bib.man1.pdf	: convertgls2bib.1
		mandoc -Tpdf -l convertgls2bib.1 >convertgls2bib.man1.pdf

convertgls2bib.1	: convertgls2bib.pod
		pod2man -c convertgls2bib convertgls2bib.pod convertgls2bib.1

datatool2bib.man1.pdf	: datatool2bib.1
		mandoc -Tpdf -l datatool2bib.1 >datatool2bib.man1.pdf

datatool2bib.1	: datatool2bib.pod
		pod2man -c datatool2bib datatool2bib.pod datatool2bib.1

clean	:
	\rm -f bib2gls*.{aux,log,toc,lot,lof,ilg,ind,idx,out,pdf,1,glstex,glg,blg,bbl}
	\rm -f arara.xml
