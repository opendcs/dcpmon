#!/bin/bash

#
# This script creates the deployable dcpmon.war file by overlaying
# your customized files on the files from the distribution.

# BASEDIR -- we assume you are in the directory containing this script.
BASEDIR=`pwd`

# Remove staging area and save old war if there is one.
if [ -d war-stage ]
then
  rm -rf war-stage
fi
if [ -f dcpmon.war ]
then
  echo "Saving previous dcpmon.war as dcpmon.war.old"
  mv dcpmon.war dcpmon.war.old
fi

# Copy the stock distro into a new staging area
echo "Copying the distro files into war-stage..."
cd $BASEDIR
mkdir war-stage
cd war-distro
tar cf - . | (cd ../war-stage; tar xf -)

# Copy any custom files into the staging area, overwriting distro files
echo "Overlaying files from war-custom..."
cd $BASEDIR/war-custom
tar cf - . | (cd ../war-stage; tar xf -)

# Make the war
echo "Building new dcpmon.war..."
cd $BASEDIR/war-stage
jar cf ../dcpmon.war *

echo "To deploy, copy dcpmon.war from this directory into the webapps directory under your Tomcat installation."
