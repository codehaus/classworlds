package org.codehaus.classworlds;

/*
 $Id$

 Copyright 2002 (C) The Werken Company. All Rights Reserved.
 
 Redistribution and use of this software and associated documentation
 ("Software"), with or without modification, are permitted provided
 that the following conditions are met:

 1. Redistributions of source code must retain copyright
    statements and notices.  Redistributions must also contain a
    copy of this document.
 
 2. Redistributions in binary form must reproduce the
    above copyright notice, this list of conditions and the
    following disclaimer in the documentation and/or other
    materials provided with the distribution.
 
 3. The name "classworlds" must not be used to endorse or promote
    products derived from this Software without prior written
    permission of The Werken Company.  For written permission,
    please contact bob@werken.com.
 
 4. Products derived from this Software may not be called "classworlds"
    nor may "classworlds" appear in their names without prior written
    permission of The Werken Company. "classworlds" is a registered
    trademark of The Werken Company.
 
 5. Due credit should be given to The Werken Company.
    (http://classworlds.werken.com/).
 
 THIS SOFTWARE IS PROVIDED BY THE WERKEN COMPANY AND CONTRIBUTORS
 ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 THE WERKEN COMPANY OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 OF THE POSSIBILITY OF SUCH DAMAGE.
 
 */

import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;

/** Classloader for <code>ClassRealm</code>s.
 *
 *  @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 *
 *  @version $Id$
 */
class RealmClassLoader 
    extends URLClassLoader
{
    // ------------------------------------------------------------
    //     Instance members
    // ------------------------------------------------------------

    /** The realm. */
    protected ClassRealmImpl realm;

    // ------------------------------------------------------------
    //     Constructors
    // ------------------------------------------------------------

    /** Construct.
     *
     *  @param realm The realm for which this loads.
     */
    RealmClassLoader(ClassRealmImpl realm)
    {
        super( new URL[0] );
        this.realm = realm;
        this.realm = realm;
    }

    // ------------------------------------------------------------
    //     Instance methods
    // ------------------------------------------------------------

    /** Retrieve the realm.
     *
     *  @return The realm.
     */
    ClassRealmImpl getRealm()
    {
        return this.realm;
    }

    /** Add a constituent to this realm for locating classes.
     *
     *  @param constituent URL to contituent jar or directory.
     */
    void addConstituent(URL constituent)
    {
        String urlStr = constituent.toExternalForm();

        if ( urlStr.startsWith( "jar:" )
             &&
             urlStr.endsWith( "!/" ) )
        {
            urlStr = urlStr.substring( 4,
                                       urlStr.length() - 2 );

            try
            {
                constituent = new URL( urlStr );
            }
            catch (MalformedURLException e)
            {
                e.printStackTrace();
            }
        }

        addURL( constituent );
    }

    /** Load a class directly from this classloader without
     *  defering through any other <code>ClassRealm</code>.
     *
     *  @param name The name of the class to load.
     *
     *  @return The loaded class. 
     *
     *  @throws ClassNotFoundException If the class could not be found.
     */
    Class loadClassDirect(String name) throws ClassNotFoundException
    {
        return super.loadClass( name, true );
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
    //     java.lang.ClassLoader
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

    /** Load a class.
     *
     *  @param name The name of the class to load.
     *  @param resolve If <code>true</code> then resolve the class.
     *
     *  @return The loaded class. 
     *
     *  @throws ClassNotFoundException If the class cannot be found.
     */
    protected Class loadClass(String name,
                              boolean resolve) throws ClassNotFoundException
    {
        return getRealm().loadClass( name );
    }

    /** Retrieve the <code>URL</code>s used by this <code>ClassLoader</code>.
     *
     *  @return The urls.
     */
    public URL[] getURLs()
    {
        return super.getURLs();
    }

    public URL findResource( String name )
    {
        return super.findResource( name );
    }        
}