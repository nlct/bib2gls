lib/bib2gls.jar	: java/Manifest.txt classes/com/dickimawbooks/bib2gls/Bib2Gls.class
		cd classes; \
		jar cmf ../java/Manifest.txt ../lib/bib2gls.jar \
		com/dickimawbooks/bib2gls/*.class

classes/com/dickimawbooks/bib2gls/Bib2Gls.class	: classes/com/dickimawbooks/bib2gls \
	java/*.java
	cd java; \
	javac -target 1.7 -source 1.7 -d ../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	 -cp ../lib/texparserlib.jar *.java

classes/com/dickimawbooks/bib2gls	:
	mkdir -p classes/com/dickimawbooks/bib2gls

expl	: lib/bib2gls.jar
	make -C doc
	cp -L lib/texparserlib.jar ../unstable/
	cp lib/bib2gls.jar ../unstable/

clean	:
	\rm -f classes/com/dickimawbooks/bib2gls/*.class

squeaky	:
	\rm -f lib/bib2gls.jar
