# Matti J. Katila

"""
The data to be written into the owner block is either read
from a file, or, if none is specified, created in an interactive session
with the user.
"""

import sys, os, java
import org.nongnu.storm as storm

# Questions to ask about the owner block.
questions = ['Whole name', 'E-mail','Address',
             'Birthday','Phone', 'Additional information']



def printUsage():
    print """
Usage: make createowner TYPE=pooltype DIR=pooldir [FILE=file of
                        information [MIME=text/plain]]

Where:

   pooltype:             Type of pool. For example dirpool or berkeleydb.

   pooldir:              Directory of pool. For example /home/user/myPool .
                         Pool directory must exists.

   file of information:  The path of a file that contains information
                         about the owner.
"""


def prettyprint(str, l=15):
    if len(str) < l:
        return str+' '*(l-len(str))+': '
    return str+': '


if __name__ == '__main__':
    if len(sys.argv[1:]) < 2:
        print """Not enough parameters given!"""
        printUsage()
        sys.exit(0)

    pooltype = sys.argv[1]
    pooldir = sys.argv[2]
    mime = None
    if len(sys.argv[1:]) > 2:
        information = java.io.File(sys.argv[3])
        if len(sys.argv[1:]) > 3:
            mime = sys.argv[4]
    else:
        information = None

    # Check pooldir
    if not os.path.isdir(pooldir):
        print """Directory for pool does not exist '%s'!""" % (pooldir)
        printUsage()
        sys.exit(0)
    else: pooldir = java.io.File(pooldir)
    
    
    # Check pooltype and create pool
    if pooltype.startswith('dir'):
        pool = storm.impl.DirPool(pooldir, \
           java.util.Collections.singleton(storm.references.PointerIndex.type))
    elif pooltype.startswith('berk'):
        pool = storm.impl.BerkeleyDBPool(pooldir, \
           java.util.Collections.singleton(storm.references.PointerIndex.type))
    else:
        print "Unkown pooltype '%s'! Try 'dirpool' or 'berkeleydb' instead." \
              % (pooltype)
        printUsage()
        sys.exit(0)
        
    if information == None:
        print """
Because you didn't specify a file containing identifying information
about yourself, you will be asked a few questions; your answers will be
used to create the file. Questions that will be asked include name,
address, e-mail, phone number, and place of birth. All answers are
voluntary, and you don't need to share the created block publicly.

    The reason for having this block is that we may later operate a
registry allowing you to register a new cryptographic key, for example
if your old one is stolen; without such a registry, you would not any
more be able to update any of your old documents maintained in Storm.

    The block will be used to prove to the registry that you are really
the owner of this identity, so you will have to share this information
with it. If you specify less information, you might find it harder to
prove to the registry that you are really the legitimate owner of this
identity; it's your choice. Do not specify incorrect information: that
would make it impossible for you to register with the registry. Rather,
just leave any field blank that you do not want to fill out.

"""
        answers = []
        while 1:
            answers = []
            for i in questions:
                answers.append(raw_input(prettyprint(i)))

            print '-'*60
            for i in range(len(questions)):
                print prettyprint(questions[i])+answers[i]
            print '-'*60
            if raw_input('Is the information correct? '+ \
               'If yes, press \'[y]\'') in ['y','Y','yes','YES']: break

        bos = pool.getBlockOutputStream('text/plain')
        for i in range(len(questions)):
            q = questions[i]
            a = answers[i]
            bos.write(prettyprint(q)+a+'\n')
        bos.close()

    else:
        if mime == None:
            print 'Using text/plain as a MIME type.'
            bos = pool.getBlockOutputStream('text/plain')
        else:
            bos = pool.getBlockOutputStream(mime)
        storm.util.CopyUtil.copy(java.io.FileInputStream(information),
                                 bos)

    print 'id', bos.getBlockId()

    signer = storm.references.PointerSigner.createOwner(
        pool, bos.getBlockId())
    signer.writeKeys(java.io.FileOutputStream(
        java.io.File(pooldir, 'privateOwner.key')))

    print pool
    i = pool.getIds().iterator()
    while i.hasNext(): print i.next()
    try:
        pool.close()
    except: pass

