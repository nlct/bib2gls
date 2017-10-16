all	: bib2gls.pdf bib2gls.1 convertgls2bib.1

bib2gls.pdf	: bib2gls.tex bib2gls.bib ../lib/bib2gls.jar ../lib/texparserlib.jar
		arara --verbose bib2gls
		cp bib2gls.pdf ../../unstable/

bib2gls.1	: bib2gls.pod
		pod2man -c bib2gls bib2gls.pod bib2gls.1

convertgls2bib.1	: convertgls2bib.pod
		pod2man -c convertgls2bib convertgls2bib.pod convertgls2bib.1

clean	:
	\rm -f bib2gls*.{aux,log,toc,ilg,ind,idx,out,pdf,1,glstex,glg}
