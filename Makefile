all: java test

TEST=`find org/ -name "*.test" -and -not -path "org/nongnu/storm/modules/*"`
TESTMODULES=org/nongnu/storm/modules/

DEPENDS = ../depends
RAWSRC = `find org/ -name "*.java"` `find com/ -name "*.java"`
CLASSDIR=CLASSES/
CLASSPATH=$(CLASSDIR):$(DEPENDS)/cryptix-jce-provider.jar:$(DEPENDS)/jython.jar:$(DEPENDS)/gisp.jar:$(DEPENDS)/dom4j.jar:$(DEPENDS)/log4j.jar:$(DEPENDS)/bamboo.jar:$(DEPENDS)/oncrpc.jar:$(DEPENDS)/jtidy.jar:$(DEPENDS)/xom-1.0b7.jar:$(DEPENDS)/je.jar:$(shell echo $$CLASSPATH)
PYTHONPATH=-Dpython.path=.:$(DEPENDS)/jythonlib.jar:$(DEPENDS)/pythonlib.jar:../navidoc/:$(DEPENDS)/docutils.jar
export CLASSPATH
JAVAC?=javac
JAVA=java -Dpython.cachedir=. -Dpython.path=$(PYTHONPATH) -Dpython.verbose=message

POOL=`printf "DIR::%s" ~/.storm-pool`
PUBPOOL=`printf "DIR::%s" ~/.storm-pool-pub`
KEYFILE=~/.storm-keys
HTTPPOOL=

PORT=5555

clean:
	@echo "Removing everything found in .cvsignores"
	find . -name ".cvsignore"|while read ign; do (cd `dirname $$ign` && cat .cvsignore|while read files; do rm -Rf $$files; done); done
	find . -name "*.pyc" | xargs rm -f
	find . -name "*.class" | xargs rm -f

gisp:
	exec $(JAVA) $(DEBUG) org.nongnu.storm.modules.gispmap.GispP2PMap $(ARGS)

http-gateway:
	#exec $(JAVA) $(DEBUG) org.nongnu.storm.util.HTTPProxy $(POOL) $(KEYFILE)
	$(JAVA) $(DEBUG) org.nongnu.storm.util.HTTPProxy $(POOL) $(KEYFILE) $(PUBPOOL) $(PORT) $(HTTPPOOL)

sync:
	$(JAVA) $(DEBUG) org.nongnu.storm.util.StormSync $(POOL) $(POOL2)

peer:
	exec $(JAVA) $(DEBUG) org.nongnu.storm.modules.gispmap.GispPeer $(ARGS)

bamboo-peer:
	exec $(JAVA) $(DEBUG) org.nongnu.storm.modules.bamboo.BambooPeer $(ARGS)
bamboo-client:
	exec $(JAVA) $(DEBUG) org.nongnu.storm.modules.bamboo.BambooPeer -node himalia.it.jyu.fi:5556 -gw $(PORT)

fakepeer:
	exec $(JAVA) $(DEBUG) org.nongnu.storm.impl.p2p.Peer $(POOL)

java:
	mkdir -p CLASSES
	$(JAVAC) $(DEBUG) -d $(CLASSDIR) $(RAWSRC) 

runjava:
	$(JAVA) $(DEBUG) $(PYTHONPATH) $(CLASS) $(ARGS)

jython:
	$(JAVA) $(DEBUG) $(PYTHONPATH) org.python.util.jython

test:
	$(JAVA) $(DEBUG) $(PYTHONPATH) org.python.util.jython test.py $(TEST)

testmodules:
	$(JAVA) $(DEBUG) $(PYTHONPATH) org.python.util.jython test.py $(TESTMODULES)

test-bamboo:
	# The Bamboo RPC gateway must already be running on localhost
	$(JAVA) $(DEBUG) $(PYTHONPATH) org.python.util.jython org/nongnu/storm/modules/bamboo/testBamboo.py

import:
	$(JAVA) $(DEBUG) $(PYTHONPATH) org.python.util.jython import.py $(POOL) $(CT) $(FILES)

convertpointers:
	$(JAVA) $(DEBUG) $(PYTHONPATH) org.python.util.jython org/nongnu/storm/pointers/convertpointers.py $(POOL) $(OLDKEYS) $(IDENTITY) $(NEWKEYS)

pointer:
	$(JAVA) $(DEBUG) org.nongnu.storm.pointers.SetPointer $(KEYFILE) $(POOL) $(POINTER) $(TARGET)

run-jython:
	$(JAVA) $(DEBUG) $(PYTHONPATH) org.python.util.jython

createowner:
	$(JAVA) $(DEBUG) $(PYTHONPATH) org.python.util.jython createowner.py $(TYPE) $(DIR) $(FILE) $(MIME)

copyrighted:
	python ../fenfire/metacode/copyrighter.py Strom org/

##########################################################################
# General documentation targets
docs:   java-doc navidoc navilink

DOCPKGS= -subpackages org
#DOCPKGS= org.nongnu.storm\
#	 org.nongnu.storm.impl\
#	 org.nongnu.storm.impl.p2p\
#	 org.nongnu.storm.storage\
#	 org.nongnu.storm.util\
#	 org.nongnu.storm.modules.gispmap

JAVADOCOPTS=-use -version -author -windowtitle "Storm Java API"
java-doc:
	find . -name '*.class' | xargs rm -f # Don't let javadoc see these
	rm -Rf doc/javadoc
	mkdir -p doc/javadoc
	javadoc $(JAVADOCOPTS) -d doc/javadoc -sourcepath . $(DOCPKGS)
##########################################################################
# Navidoc documentation targets
navidoc: # Compiles reST into HTML
	make -C "../navidoc/" html DBG="$(DBG)" RST="../storm/doc/"

navilink: # Bi-directional linking using imagemaps
	make -C "../navidoc/" imagemap HTML="../storm/doc/"

naviloop: # Compiles, links, loops
	make -C "../navidoc/" html-loop DBG="--imagemap $(DBG)" RST="../storm/$(RST)"

peg: # Creates a new PEG, uses python for quick use
	make -C "../navidoc/" new-peg PEGDIR="../storm/doc/pegboard"

pegs:   # Compiles only pegboard
	make -C "../navidoc/" html DBG="$(DBG)" RST="../storm/doc/pegboard/"

html: # Compiles reST into HTML, directories are processed recursively
	make -C "../navidoc/" html DBG="$(DBG)" RST="../storm/$(RST)"

html-loop: # Loop version for quick recompiling
	make -C "../navidoc/" html-loop DBG="$(DBG)" RST="../storm/$(RST)"

latex: # Compiles reST into LaTeX, directories are processed recursively
	make -C "../navidoc/" latex DBG="$(DBG)" RST="../storm/$(RST)"

latex-loop: # Loop version for quick recompiling
	make -C "../navidoc/" latex-loop DBG="$(DBG)" RST="../storm/$(RST)"
