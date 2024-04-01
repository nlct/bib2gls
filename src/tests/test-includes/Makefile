output-files/test-main.pdf	: test-main.tex output-files
	pdflatex --output-directory output-files test-main
	bib2gls --dir output-files test-main
	pdflatex --output-directory output-files test-main

output-files	:
	mkdir output-files
	mkdir output-files/chapters

clean	:
	rm -rf output-files/
	rm -f chapters/*.aux
	rm -f test-main.aux test-main.toc test-main.glstex test-main.log test-main.pdf
