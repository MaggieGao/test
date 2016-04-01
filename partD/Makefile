#all: build

<<<<<<< HEAD
#build:
#	chmod +x clean.sh
#	chmod +x timeout.sh
#	chmod +x pipair 
#	javac PipairD.java
=======
build:
	chmod +x pipair 
	javac PipairD.java
>>>>>>> origin/master

#clean: 
#	rm -rf *class *o 

#run:
#	./pipair

	
BIN = ./bin
JC = javac
JFLAGS = -d $(BIN)
	
CLASSES = \
	PipairD.java

CLASS_FILES=$(CLASSES:%.java=$(BIN)/%.class)
	
all: classes

classes:
	mkdir -p $(BIN)
	chmod +x pipair
	$(JC) $(JFLAGS) $(CLASSES) 
	
clean:
	rm $(BIN)/*.class
	
run:
	./pipair
