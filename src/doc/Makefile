all	: bib2gls.1 convertgls2bib.1 bib2gls.pdf bib2gls-begin.pdf 
		@grep 'Package glossaries-extra Warning:' bib2gls*.log || true
		@grep 'unreferenced destination' bib2gls*.log || true

bib2gls.pdf	: bib2gls.tex bib2gls.bib bib2gls-cite.bib\
		   ../lib/bib2gls.jar ../lib/texparserlib.jar \
		   ../examples/*.tex ../examples/*.bib
		make -C ../examples
		arara --verbose bib2gls
		@grep 'Package glossaries-extra Warning:' bib2gls.log || true
		@grep 'unreferenced destination' bib2gls.log || true

bib2gls-begin.pdf	: bib2gls-begin.tex bib2gls.bib bib2gls-cite.bib
			arara --verbose bib2gls-begin

bib2gls.1	: bib2gls.pod
		pod2man -c bib2gls bib2gls.pod bib2gls.1

convertgls2bib.1	: convertgls2bib.pod
		pod2man -c convertgls2bib convertgls2bib.pod convertgls2bib.1

clean	:
	\rm -f bib2gls*.{aux,log,toc,lot,lof,ilg,ind,idx,out,pdf,1,glstex,glg,blg,bbl}
	\rm -f arara.xml
