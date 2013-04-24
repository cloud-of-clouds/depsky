#!/bin/sh



java -cp bin:lib/DepSkyDependencies.jar:lib/commons-io-1.4.jar:lib/NewPVSS.jar:lib/JReedSolEC.jar:lib/AmazonAccess.jar:lib/GoogleSAccess.jar:lib/RackAzureAccess.jar:lib/azureblob-1.5.4.jar:lib/azure-common-1.5.4.jar depskys.core.LocalDepSkySClient $1 $2 $3
