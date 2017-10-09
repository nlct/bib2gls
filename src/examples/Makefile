all	: sample-chemical.pdf

sample-chemical.pdf	: sample-chemical.tex chemicalformula.bib
	pdflatex sample-chemical
	bib2gls sample-chemical
	pdflatex sample-chemical

clean	:
	\rm -f *.{glg,glstex,aux,log,out,toc,pdf}
