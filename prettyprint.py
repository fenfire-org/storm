#
# Copyright (c) 2003, Jukka Honkela, jukka@honkela.org
#
# This file is part of Fenfire.
# 
# Fenfire is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
# 
# Fenfire is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
# or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
# Public License for more details.
# 
# You should have received a copy of the GNU General
# Public License along with Fenfire; if not, write to the Free
# Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
# MA  02111-1307  USA
#

"""
Prettyprint the GISP XML written to the command line.
Suggested use::

    make peer ARGS="..." | python prettyprint.py
"""

import string
import sys

l=sys.stdin.readline()
level = 0
successive_end_tag = 0
successive_start_tag = 0
while l:
	if l[0:8] == "received" or l[0:8] == "sendMess": level = 0
	start_tags = l.split('<')
	if len(start_tags[0]) > 0: print start_tags[0]
	for n in start_tags[1:]:
		if len(n) == 0: continue
		
		if n[0] == "/": 
			successive_start_tag = 0
			if successive_end_tag:
				level = level - 1
				print "\t" * level + "<" + n
			else:
				print "<" + n
			successive_end_tag = 1
		else: 
			successive_end_tag = 0
			if n[-2] == "/": 
				print "\t" * level + "<" + n
				successive_start_tag = 0
			else:
				if successive_start_tag: 
					print ""
					level = level + 1
				print "\t"*level + "<" + n,
				successive_start_tag = 1

	l=sys.stdin.readline()

