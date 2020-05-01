JFLAGS = -g 
JC = javac 
.SUFFIXES: .java .class 
.java.class:
		$(JC) $(JFLAGS) $*.java 

CLASSES = \
	Client.java \
	Server.java \
	ReceiveClientThread.java \
	FileChunking.java \
	SendServerThread.java \
	Utility.java \
	TCPProtocol.java \

default: classes 

classes: $(CLASSES:.java=.class)

clean: 
		$(RM) *.class