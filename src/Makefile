TARGET_VERSION := --release 8

all	: lib/bib2gls.jar lib/convertgls2bib.jar lib/datatool2bib.jar

lib/bib2gls.jar	: java/bib2gls/Manifest.txt classes/com/dickimawbooks/bib2gls/Bib2Gls.class
		cd classes; \
		jar cmf ../java/bib2gls/Manifest.txt ../lib/bib2gls.jar \
		com/dickimawbooks/bib2gls/*.class

classes/com/dickimawbooks/bib2gls/Bib2Gls.class	: classes/com/dickimawbooks/bib2gls \
	java/bib2gls/*.java
	cd java/bib2gls; \
	javac $(TARGET_VERSION) -d ../../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	 -cp ../../lib/texparserlib.jar *.java

classes/com/dickimawbooks/bib2gls	:
	mkdir -p classes/com/dickimawbooks/bib2gls

lib/convertgls2bib.jar	: java/gls2bib/Manifest.txt classes/com/dickimawbooks/gls2bib/Gls2Bib.class
		cd classes; \
		jar cmf ../java/gls2bib/Manifest.txt ../lib/convertgls2bib.jar \
		com/dickimawbooks/gls2bib/*.class

classes/com/dickimawbooks/gls2bib/Gls2Bib.class	: classes/com/dickimawbooks/gls2bib \
	java/gls2bib/*.java lib/bibglscommon.jar
	cd java/gls2bib; \
	javac $(TARGET_VERSION) -d ../../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	 -cp ../../lib/texparserlib.jar:../../lib/bibglscommon.jar *.java

classes/com/dickimawbooks/gls2bib	:
	mkdir -p classes/com/dickimawbooks/gls2bib

lib/datatool2bib.jar	: java/datatool2bib/Manifest.txt classes/com/dickimawbooks/datatool2bib/DataTool2Bib.class
		cd classes; \
		jar cmf ../java/datatool2bib/Manifest.txt ../lib/datatool2bib.jar \
		com/dickimawbooks/datatool2bib/*.class

classes/com/dickimawbooks/datatool2bib/DataTool2Bib.class	: classes/com/dickimawbooks/datatool2bib \
	java/datatool2bib/*.java lib/bibglscommon.jar
	cd java/datatool2bib; \
	javac $(TARGET_VERSION) -d ../../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	 -cp ../../lib/texparserlib.jar:../../lib/bibglscommon.jar *.java

classes/com/dickimawbooks/datatool2bib	:
	mkdir -p classes/com/dickimawbooks/datatool2bib

lib/bibglscommon.jar	: classes/com/dickimawbooks/bibglscommon/BibGlsCommon.class
		cd classes; \
		jar cf ../lib/bibglscommon.jar \
		com/dickimawbooks/bibglscommon/*.class

classes/com/dickimawbooks/bibglscommon/BibGlsCommon.class	: classes/com/dickimawbooks/bibglscommon \
	java/bibglscommon/*.java
	cd java/bibglscommon; \
	javac $(TARGET_VERSION) -d ../../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	 -cp ../../lib/texparserlib.jar *.java

classes/com/dickimawbooks/bibglscommon	:
	mkdir -p classes/com/dickimawbooks/bibglscommon

expl	: ../unstable/bib2gls.zip

../unstable/bib2gls.zip	: lib/bib2gls.jar lib/texparserlib.jar lib/convertgls2bib.jar
	make -C doc
	mkdir -p ../unstable/bib2gls/lib/resources
	mkdir -p ../unstable/bib2gls/bin
	mkdir -p ../unstable/bib2gls/doc
	cp -L lib/texparserlib.jar ../unstable/bib2gls/lib/
	cp lib/*.jar ../unstable/bib2gls/lib/
	cp lib/resources/*.xml ../unstable/bib2gls/lib/resources/
	cp bin/bib2gls ../unstable/bib2gls/bin/
	cp bin/convertgls2bib ../unstable/bib2gls/bin/
	cp doc/bib2gls.pdf ../unstable/bib2gls/doc/
	cd ../unstable; zip -r bib2gls.zip bib2gls/

clean	:
	\rm -f classes/com/dickimawbooks/bib2gls/*.class
	\rm -f classes/com/dickimawbooks/gls2bib/*.class
	\rm -f classes/com/dickimawbooks/bibglscommon/*.class

squeaky	:
	\rm -f lib/bib2gls.jar
	\rm -f lib/convertgls2bib.jar
	\rm -f lib/bibglscommon.jar
