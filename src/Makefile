ifeq ($(shell javac -version 2>&1 | grep 1.8),)
  target_version := --release 8
else
  target_version =
endif

all	: lib/bib2gls.jar lib/convertgls2bib.jar lib/datatool2bib.jar

lib/bib2gls.jar	: java/bib2gls/Manifest.txt classes/com/dickimawbooks/bibgls/bib2gls/Bib2Gls.class
		cd classes; \
		jar cmf ../java/bib2gls/Manifest.txt ../lib/bib2gls.jar \
		com/dickimawbooks/bibgls/bib2gls/*.class

classes/com/dickimawbooks/bibgls/bib2gls/Bib2Gls.class	: classes/com/dickimawbooks/bibgls/bib2gls \
	java/bib2gls/*.java lib/bibglscommon.jar
	cd java/bib2gls; \
	javac $(target_version) -d ../../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	 -cp ../../lib/texparserlib.jar:../../lib/bibglscommon.jar *.java

classes/com/dickimawbooks/bibgls/bib2gls	:
	mkdir -p classes/com/dickimawbooks/bibgls/bib2gls

lib/convertgls2bib.jar	: java/gls2bib/Manifest.txt classes/com/dickimawbooks/bibgls/gls2bib/Gls2Bib.class
		cd classes; \
		jar cmf ../java/gls2bib/Manifest.txt ../lib/convertgls2bib.jar \
		com/dickimawbooks/bibgls/gls2bib/*.class

classes/com/dickimawbooks/bibgls/gls2bib/Gls2Bib.class	: classes/com/dickimawbooks/bibgls/gls2bib \
	java/gls2bib/*.java lib/bibglscommon.jar
	cd java/gls2bib; \
	javac $(target_version) -d ../../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	 -cp ../../lib/texparserlib.jar:../../lib/bibglscommon.jar *.java

classes/com/dickimawbooks/bibgls/gls2bib	:
	mkdir -p classes/com/dickimawbooks/bibgls/gls2bib

lib/datatool2bib.jar	: java/datatool2bib/Manifest.txt classes/com/dickimawbooks/bibgls/datatool2bib/DataTool2Bib.class
		cd classes; \
		jar cmf ../java/datatool2bib/Manifest.txt ../lib/datatool2bib.jar \
		com/dickimawbooks/bibgls/datatool2bib/*.class

classes/com/dickimawbooks/bibgls/datatool2bib/DataTool2Bib.class	: classes/com/dickimawbooks/bibgls/datatool2bib \
	java/datatool2bib/*.java lib/bibglscommon.jar
	cd java/datatool2bib; \
	javac $(target_version) -d ../../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	 -cp ../../lib/texparserlib.jar:../../lib/bibglscommon.jar *.java

classes/com/dickimawbooks/bibgls/datatool2bib	:
	mkdir -p classes/com/dickimawbooks/bibgls/datatool2bib

lib/bibglscommon.jar	: classes/com/dickimawbooks/bibgls/common/BibGlsCommon.class
		cd classes; \
		jar cf ../lib/bibglscommon.jar \
		com/dickimawbooks/bibgls/common/*.class

classes/com/dickimawbooks/bibgls/common/BibGlsCommon.class	: classes/com/dickimawbooks/bibgls/common \
	java/common/*.java
	cd java/common; \
	javac $(target_version) -d ../../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	 -cp ../../lib/texparserlib.jar *.java

classes/com/dickimawbooks/bibgls/common	:
	mkdir -p classes/com/dickimawbooks/bibgls/common

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
	cp bin/datatool2bib ../unstable/bib2gls/bin/
	cp doc/bib2gls.pdf ../unstable/bib2gls/doc/
	cd ../unstable; zip -r bib2gls.zip bib2gls/

clean	:
	\rm -f classes/com/dickimawbooks/bibgls/bib2gls/*.class
	\rm -f classes/com/dickimawbooks/bibgls/gls2bib/*.class
	\rm -f classes/com/dickimawbooks/bibgls/common/*.class

squeaky	:
	\rm -f lib/bib2gls.jar
	\rm -f lib/convertgls2bib.jar
	\rm -f lib/bibglscommon.jar
