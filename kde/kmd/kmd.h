/***************************************************************************
                          kmd.h  -  description
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
#ifndef __kmd_h__
#define __kmd_h__

#include <qstring.h>
#include <qcstring.h>
#include <qhttp.h> 
#include <qeventloop.h>

#include <kurl.h>
#include <kio/global.h>
#include <kio/slavebase.h>
//#include <kio/job.h>
class QCString;

class kio_kmdProtocol : public QObject, public KIO::SlaveBase
{
 Q_OBJECT
public:
  kio_kmdProtocol(const QCString &pool_socket, const QCString &app_socket);
  virtual ~kio_kmdProtocol();

  virtual void get(const KURL& url);
  virtual void mimetype(const KURL& url);
  virtual void put( const KURL& url, int permissions, bool overwrite, bool resume );

  QHttp *http;
  QApplication *app;
  int requestNr;
  int isMimeTypeRequest;
  int isFinished;
private:
  void request(const KURL& url, int isMimeTypeRequest);
private slots:
  void slotResponseHeaderReceived(const QHttpResponseHeader & resp);
  void slotReadyRead();
  void slotRequestFinished(int id);
};


#endif
