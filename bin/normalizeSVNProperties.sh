#!/bin/bash
#
# This file is part of Jikes RVM (http://jikesrvm.sourceforge.net).
# The Jikes RVM project is distributed under the Common Public License (CPL).
# A copy of the license is included in the distribution, and is also
# available at http://www.opensource.org/licenses/cpl1.0.php
#
# (C) Copyright IBM Corp. 2006
#
# This is really an administrative script used by the core team to maintain
# the svn repository.  It's job is to normalize the svn properties on files
# based on their extensions.
#

cd $RVM_ROOT

# Source code files should have the following properties set:
#   svn:eol-style : native
#   svn:mime-type : text/plain
for extension in .java .c .h .C; do
    find . -name "*$extension" -exec svn propset svn:eol-style native {} \;
    find . -name "*$extension" -exec svn propset svn:mime-type text/plain {} \;
done



