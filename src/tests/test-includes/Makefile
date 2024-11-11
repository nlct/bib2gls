outdir:=output-files
deps=../../lib/bib2gls.jar ../../lib/texparserlib.jar

output-files/test-main.pdf	: test-main.tex $(outdir) $(deps)
	pdflatex --output-directory $(outdir) test-main
	bib2gls --dir $(outdir) test-main
	pdflatex --output-directory $(outdir) test-main

test-automake	: test-main.tex $(outdir) $(deps)
	pdflatex --output-directory $(outdir) test-main
	pdflatex --shell-escape --output-directory $(outdir) test-main

$(outdir)	:
	mkdir $(outdir)
	mkdir $(outdir)/chapters

clean	:
	rm -rf $(outdir)/
	rm -f chapters/*.aux
	rm -f test-main.aux test-main.toc test-main.glstex test-main.log test-main.pdf
