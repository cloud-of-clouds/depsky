
About DepSky:

- First you need to create the accounts at Amazon S3 (http://aws.amazon.com/pt/s3/), RaskSpace (http://www.rackspace.co.uk/), Windows Azure (https://www.windowsazure.com/en-us/) and Google Storage (https://developers.google.com/storage/). After create all necessary accounts, you need to fill the access credentials in the 'accounts.properties' file at config folder.


- To run DepSky on the command line just run the script DepSky_Run.sh passing 3 arguments: the id of the client; the protocol you want to use DepSky (0 for DepSky-A, 1 for DepSky-CA, 2 for use only erasure codes, and 3 for use only sercret sharing); and the the storage location (0 if you want to use cloud storage to replicate data and 1 if you want to run DepSky with localy storage). The main implemented in LocalDepSkySClient.java is just for test. You can change it. 


- If you want to test DepSky storing all data and metadata localy, you need to run first the local server at /src/depskys/clouds/drivers/localStorageService/ServerThread.java. To do this just run the script Run_LocalStorage.sh. This server will receive all requests at ip 127.0.0.1 and port 5555.


	
