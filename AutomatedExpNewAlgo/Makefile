JFLAGS = 
CLASSPATH = 
JC = javac

.SUFFIXES: .java .class
.java.class: %.java
	$(JC) $(JFLAGS) $(CLASSPATH) $< -Xlint:none

CLASSES = $(wildcard *.java ./GroundedLanguage/*.java)

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
