/***************************************************************************
                          kmd.cpp  -  description
                             -------------------
    begin                : Tue Jan 15 00:49:05 MST 2002
    copyright            : (C) 2002 by Simon R
    email                : mail@srobins.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
#include <qcstring.h>
#include <qbitarray.h>
#include <qhttp.h> 
#include <qeventloop.h>
#include <qbuffer.h>

#include <stdlib.h>

#include <kapp.h>
#include <kdebug.h>
#include <kstddirs.h>
#include <klocale.h>
#include <kurl.h>
#include <kprocess.h>
#include <kmessagebox.h>
#include "kmd.h"

using namespace KIO;
extern "C"{
  int kdemain( int argc, char **argv ){
    QApplication *app = new QApplication(argc, argv);
    KInstance instance( "kio_kmd" );

    if (argc != 4){
			exit(-1);
    }

    kio_kmdProtocol slave(argv[2], argv[3]);
    slave.app = app;

    slave.dispatchLoop();

    return 0;
  }
}

kio_kmdProtocol::kio_kmdProtocol(const QCString &pool_socket, const QCString &app_socket)
  : QObject(), SlaveBase("kio_kmd", pool_socket, app_socket)
{
  //kdDebug() << "kio_kmdProtocol::kio_kmdProtocol()" << endl;
  http = new QHttp();
  connect(http,  SIGNAL(responseHeaderReceived(const QHttpResponseHeader &)),
	  this, SLOT(slotResponseHeaderReceived(const QHttpResponseHeader &)));
  //kdDebug() << "1" << endl;
  connect(http,  SIGNAL(readyRead(const QHttpResponseHeader &)),
	  this, SLOT(slotReadyRead()));
  connect(http,  SIGNAL(requestFinished(int, bool)),
	  this, SLOT(slotRequestFinished(int)));
}
/* ---------------------------------------------------------------------------------- */


kio_kmdProtocol::~kio_kmdProtocol()
{
}


/* ---------------------------------------------------------------------------------- */
void kio_kmdProtocol::get(const KURL& url )
{
  request(url, 0);
}


void kio_kmdProtocol::mimetype(const KURL & url)
{
  request(url, 1);
}

void kio_kmdProtocol::put(const KURL& url, int permissions, bool overwrite, bool resume)
{
  //kdDebug() << "kio_kmdProtocol:: PUT" << endl;
  QBuffer buf;
  QByteArray arr;

  buf.open(IO_ReadWrite);

  while(1) {
    //kdDebug() << "req" << endl;
    dataReq();
    //kdDebug() << "read" << endl;
    int n = readData(arr);
    //kdDebug() << "read: " << n << endl;
    if (n > 0) {
      buf.writeBlock(arr);
      //kdDebug() << "written" << endl;
    } else {
      //kdDebug() << "break" << endl;
      break;
    }
  }

  isFinished = 0;
  isMimeTypeRequest = 0;

  //kdDebug() << "Buffer size: " << buf.buffer().size() << endl;

  //kdDebug() << "Start HTTP" << endl;

  http->setHost("localhost", 5555);
  QHttpRequestHeader hdr("PUT", url.prettyURL(), 1, 0);
  requestNr = http->request(hdr, buf.buffer());

  //kdDebug() << "HTTP initialized, reqnr = " << requestNr << endl;
  
  while(!isFinished) {
    //kdDebug() << "process events" << endl;
    app->processEvents();
  }
  //kdDebug() << "done" << endl;
  finished();

  //kdDebug() << "close" << endl;
  buf.close();
  //kdDebug() << "closed" << endl;
}

/* --------------------------------------------------------------------------- */
void kio_kmdProtocol::slotResponseHeaderReceived(const QHttpResponseHeader & resp)
{
  //kdDebug() << "kio_urn -> response header received" << endl;
  if(resp.hasContentType())
    mimeType(resp.contentType());
  if(isMimeTypeRequest)
    isFinished = 1;
}

void kio_kmdProtocol::slotReadyRead()
{
  //kdDebug() << "kio_urn -> data received" << endl;
  if(http->bytesAvailable() > 0)
    data(http->readAll());
}

void kio_kmdProtocol::slotRequestFinished(int req)
{
  if(req == requestNr)
    isFinished = true;
  //kdDebug() << "kio_urn -> reqfinished " << req << endl;
}


/* --------------------------------------------------------------------------- */
void kio_kmdProtocol::request(const KURL& url, int isMimeTypeRequest0) {
  isFinished = 0;
  isMimeTypeRequest = isMimeTypeRequest0;

  //kdDebug() << "kio_urn::get(const KURL& url)" << endl ;

  //kdDebug() << "myURL: " << url.prettyURL() << endl;

  //kdDebug() << "0" << endl;
  //kdDebug() << "2" << endl;
  http->setHost("localhost", 5555);
  requestNr = http->get(url.prettyURL());
  //kdDebug() << "request id: " << requestNr << endl;
  while(!isFinished) {
    ////kdDebug() << "process events" << endl;
    app->processEvents();
  }
  //kdDebug() << "done" << endl;
  finished();
}
