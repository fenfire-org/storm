#!/usr/bin/python
# Open in gnuclient

import sys, os

file = sys.argv[1]
#print file
proxy = 'http://localhost:5555/'

if file.startswith('urn:x-storm:'):
    args = ('-eval', '(find-file \":%s%s\")' % (proxy, file))
elif file.startswith('http:'):
    args = ('-eval', '(find-file \":%s\")' % file)
else:
    args = file,

open('/tmp/opentext.log', 'a').write("Args: %s\n" % [args]);
#print args
os.execlp('/usr/bin/gnuclient', '/usr/bin/gnuclient', *args)
