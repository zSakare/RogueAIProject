JFLAGS = -classpath .
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Agent.java
	
default: classes

classes: $(CLASSES:.java=.class)

#classes: $(CLASSES)
#	tar -xvf $(TARBALL) && $(JC) $(JFLAGS) $(CLASSES)
	
clean:
	rm *.class
