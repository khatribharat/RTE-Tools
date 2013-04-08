#!/bin/sh

raspCommand="/home/khatri/Dropbox/BTP-2012/Bharat/Tools/rasp3os/scripts/rasp.sh"

if [ $# -lt 2 ]
then
	echo "Usage: generateParses.sh <input-file> <output-file>"
	exit
fi

# We have been provided two arguments.
inputFile=$1
outputFile=$2

if [ ! -f $inputFile ]
then	
		echo "The file \""$inputFile"\" doesn't exist."
		exit
fi
 
while read line
do
	echo $line | $raspCommand -m -p'-og' >> $outputFile
done < $inputFile



