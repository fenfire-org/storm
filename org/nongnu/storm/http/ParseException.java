/*
ParseException.java
 *    
 *    Copyright (c) 2003, Antti-Juhani Kaijanaho
 *    
 *    This file is part of Storm.
 *    
 *    Storm is free software; you can redistribute it and/or modify it under
 *    the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *    
 *    Storm is distributed in the hope that it will be useful, but WITHOUT
 *    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *    or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
 *    Public License for more details.
 *    
 *    You should have received a copy of the GNU General
 *    Public License along with Storm; if not, write to the Free
 *    Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *    MA  02111-1307  USA
 *    
 *    
 */
/*
 * Written by Antti-Juhani Kaijanaho
 */

package org.nongnu.storm.http;

public class ParseException extends Exception {
    
    public ParseException() { super(); }
    public ParseException(String s) { super(s); }
    
}
