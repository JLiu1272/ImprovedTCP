# Data Communication and Networks Project 4

Objective: Implementing a more efficient, resource saving TCP Protocol

How to run this?

1. Run the make command in terminal to compile all the java files
2. Start a server. Run java Server
3. Initialise a client. Run java Client <Destination IP> <File Name to send> (i.e java Client 127.0.0.1 t1.gif)
4. Once the server has received everything, the client will close and output the "Transaction Completed" message

How is the final output stored?

The files that the client made will be stored in the TestFiles directory. The files
that the server received from the client will be stored in the TestFilesReceive directory.
The chunks of the files will also be stored in these directories.
