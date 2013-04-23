#!/bin/sh



java -cp bin:lib/DepSkyDependencies.jar:lib/commons-io-1.4.jar:lib/NewPVSS.jar:lib/JReedSolEC depskys.core.LocalDepSkySClient $1 $2 $3 $4
