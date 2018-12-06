#!/bin/bash
mvn package
if [ $? -eq 0 ]; then
	mkdir q;
	cd q;
	jar xf ~/.m2/repository/org/glassfish/javax.json/1.0/javax.json-1.0.jar; 
	jar xf ../target/references-*.jar
	jar cfm ../qcomp.jar META-INF/MANIFEST.MF .
	cd ..
	rm -r q
fi
