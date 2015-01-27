#!/bin/sh



java -cp bin:lib/AmazonDriver.jar:lib/aws-java-sdk-1.7.2.jar:lib/commons-codec-1.4.jar:lib/commons-io-1.4.jar:lib/commons-logging-1.1.1.jar:lib/DepSkyDependencies.jar:lib/GoogleStorageDriver.jar:lib/httpclient-4.3.3.jar:lib/httpcore-4.3.2.jar:lib/jackson-annotations-2.1.1.jar:lib/jackson-core-2.1.1.jar:lib/jackson-databind-2.1.1.jar:lib/joda-time-2.2.jar:lib/JReedSolEC.jar:lib/microsoft-windowsazure-api-0.4.6.jar:lib/PVSS.jar:lib/RackSpaceDriver.jar:lib/WindowsAzureDriver.jar:lib/jets3t/jackson-core-asl-1.8.1.jar:lib/jets3t/jackson-mapper-asl-1.8.1.jar:lib/jets3t/java-xmlbuilder-0.4.jar:lib/jets3t/jets3t-0.9.1.jar:lib/jets3t/servlet-api depskys.core.LocalDepSkySClient $1 $2 $3
