#!/bin/sh

jarpath=`kpsewhich --progname=bib2gls --format=texmfscripts bib2gls.jar`
java -Djava.locale.providers=CLDR,JRE -jar "$jarpath" "$@"

