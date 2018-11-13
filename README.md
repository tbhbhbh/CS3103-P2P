# P2P

This is the P2P file sharing application for CS3103 Computer Networks Practice module.

Instructions for running:

1) Host the server on a public domain.

2) Run two separate instances of clients behind different NATs.

3) Proceed with testing the commands.

Commands:

1. List files from server

Lists the files that are available for downloading from peers in the network.

2. Request for a file

Checks for a file to retrieve its information.

3. Download a file

Starts the downloading process. Downloads a file chunk by chunk. Command 2 must be executed before downloading can begin.

4. Update file on a server

Manually updates a file to make it available in the centralized directory listing.

5. Disconnect

Disconnects itself from the file sharing network.

For testing purposes, we have provided a file, pepe.jpg in the directory where files are to be detected "src/files/". Please follow the instructions below to test our program:

1) Run the tracker (Tracker/Server) on a public domain. During our testing, We used an AWS instance to run our tracker.

2) Run 2 different clients, both in seperate NATs. During our testing, we used a client in a private home network and a client behind NUS network.

3) Have one of the clients register pepe.jpg as a file with command (4).

4) Have the other client list the files available with command (1). You should see 'pepe.jpg' in the list. 

5) Now, request for the file with command (2) and type in the name of the file.

6) You can now use command (3) to download the file. 

7) Once downloading is successful, check the downloading client for the image in the output directory in src/files/output.

8) Once done, you can exit with command (5).
