all	: lib/bib2gls.jar lib/convertgls2bib.jar

lib/bib2gls.jar	: java/bib2gls/Manifest.txt classes/com/dickimawbooks/bib2gls/Bib2Gls.class
		cd classes; \
		jar cmf ../java/bib2gls/Manifest.txt ../lib/bib2gls.jar \
		com/dickimawbooks/bib2gls/*.class

classes/com/dickimawbooks/bib2gls/Bib2Gls.class	: classes/com/dickimawbooks/bib2gls \
	java/bib2gls/*.java
	cd java/bib2gls; \
	javac -target 1.7 -source 1.7 -d ../../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	 -cp ../../lib/texparserlib.jar *.java

classes/com/dickimawbooks/bib2gls	:
	mkdir -p classes/com/dickimawbooks/bib2gls

lib/convertgls2bib.jar	: java/gls2bib/Manifest.txt classes/com/dickimawbooks/gls2bib/Gls2Bib.class
		cd classes; \
		jar cmf ../java/gls2bib/Manifest.txt ../lib/convertgls2bib.jar \
		com/dickimawbooks/gls2bib/*.class

classes/com/dickimawbooks/gls2bib/Gls2Bib.class	: classes/com/dickimawbooks/gls2bib \
	java/gls2bib/*.java 
	cd java/gls2bib; \
	javac -target 1.7 -source 1.7 -d ../../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	 -cp ../../lib/texparserlib.jar *.java

classes/com/dickimawbooks/gls2bib	:
	mkdir -p classes/com/dickimawbooks/gls2bib

expl	: lib/bib2gls.jar
	make -C doc
	cp -L lib/texparserlib.jar ../unstable/
	cp lib/bib2gls.jar ../unstable/
	cp lib/convertgls2bib.jar ../unstable/
	cp doc/bib2gls.pdf ../unstable/

clean	:
	\rm -f classes/com/dickimawbooks/bib2gls/*.class
	\rm -f classes/com/dickimawbooks/gls2bib/*.class

squeaky	:
	\rm -f lib/bib2gls.jar
	\rm -f lib/convertgls2bib.jar
