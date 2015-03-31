#!/bin/bash
VERSION=0.1
POM=spellcheck-$VERSION.pom
echo "pom is $POM"
cp pom.xml $POM

mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=$POM -Dfile=confspellcheck-$VERSION-sources.jar -Dclassifier=sources
mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=$POM -Dfile=confspellcheck-$VERSION-javadoc.jar -Dclassifier=javadoc
mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=$POM -Dfile=confspellcheck-$VERSION.jar
#mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=$POM -Dfile=$POM