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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.jar.JarInputStream;
import java.util.jar.JarEntry;

/** Classloader for <code>ClassRealm</code>s.
 *
 *  Loads classes from an "uberjar".
 *
 *  @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 *
 *  @version $Id$
 */
class UberJarRealmClassLoader
    extends RealmClassLoader
{
    // ------------------------------------------------------------
    //     Instance members
    // ------------------------------------------------------------

    /** Classes, indexed by path. */
    private Map classIndex;

    /** Urls. */
    private List urls;

    /**
     * Indexes for constituent jars. Contains a Map for each constituent which
     * is a jar, keyed by url. The second map contains urls to each entry of
     * the jar keyed by path.
     */
    private Map jarIndexes;

    // ------------------------------------------------------------
    //     Constructors
    // ------------------------------------------------------------

    /** Construct.
     *
     *  @param realm The realm for which this loads.
     */
    UberJarRealmClassLoader( DefaultClassRealm realm )
    {
        super( realm );

        this.urls = new ArrayList();

        this.classIndex = new HashMap();

        this.jarIndexes = new HashMap();
    }

    // ------------------------------------------------------------
    //     Instance methods
    // ------------------------------------------------------------

    /** Retrieve the realm.
     *
     *  @return The realm.
     */
    DefaultClassRealm getRealm()
    {
        return this.realm;
    }

    /** Add a constituent to this realm for locating classes.
     *
     *  @param constituent URL to contituent jar or directory.
     */
    void addConstituent( URL constituent )
    {
        // If the constituent is a jar, build an index for it.
        if ( "jar".equals( constituent.getProtocol() )
              || constituent.toExternalForm().endsWith( ".jar" ) )
        {
            buildIndexForJar( constituent );
        }

        // Add the constituent to the urls collection

        this.urls.add( constituent );

        super.addConstituent( constituent );
    }

    /**
     * Builds an index for an incoming jar.
     *
     * @param inUrl
     */
    private void buildIndexForJar( URL inUrl )
    {
        HashMap index = new HashMap();

        String urlText = null;

        if ( inUrl.getProtocol().equals( "jar" ) )
        {
            urlText = inUrl.toExternalForm();
        }
        else
        {
            urlText = "jar:" + inUrl.toExternalForm();
        }

        String resourceName;
        URL resourceUrl = null;

        try
        {
            JarInputStream in = new JarInputStream( inUrl.openStream() );

            try
            {
                JarEntry entry = null;

                while ( ( entry = in.getNextJarEntry() ) != null )
                {
                    resourceName = entry.getName();

                    resourceUrl = new URL( urlText + "!/" + resourceName );

                    index.put( resourceName, resourceUrl );
                }
            }
            finally
            {
                in.close();
            }
        }
        catch ( IOException e )
        {
            // swallow
        }

        jarIndexes.put( inUrl, index );
    }

    /** Load a class directly from this classloader without
     *  defering through any other <code>ClassRealm</code>.
     *
     *  @param className The name of the class to load.
     *
     *  @return The loaded class.
     *
     *  @throws ClassNotFoundException If the class could not be found.
     */
    Class loadClassDirect( String className ) throws ClassNotFoundException
    {
        String classPath = className.replace( '.',
                                              '/' ) + ".class";

        if ( this.classIndex.containsKey( classPath ) )
        {
            return (Class) this.classIndex.get( classPath );
        }

        Iterator urlIter = this.urls.iterator();
        URL eachUrl = null;

        byte[] classBytes = null;

        while ( ( classBytes == null )
            &&
            ( urlIter.hasNext() ) )
        {
            eachUrl = (URL) urlIter.next();

            if ( "jar".equals( eachUrl.getProtocol() )
                ||
                eachUrl.toExternalForm().endsWith( ".jar" ) )
            {
                classBytes = findClassInJarStream( eachUrl,
                                                   classPath );
            }
            else
            {
                classBytes = findClassInDirectoryUrl( eachUrl,
                                                      classPath );
            }
        }

        if ( classBytes == null )
        {
            return super.loadClassDirect( className );
        }
        else
        {
            Class cls = defineClass( className,
                                     classBytes,
                                     0,
                                     classBytes.length );

            this.classIndex.put( classPath,
                                 cls );

            return cls;
        }

        // return super.loadClassDirect( name );
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    //     java.lang.ClassLoader
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    /** @see ClassLoader
     */
    public URL getResource( String name )
    {
        return getRealm().getResource( name );
    }

    /** @see ClassLoader
     */
    public URL findResource( String name )
    {
        URL resourceUrl = null;

        Iterator urlIter = this.urls.iterator();
        URL eachUrl = null;

        while ( urlIter.hasNext() )
        {
            eachUrl = (URL) urlIter.next();

            if ( "jar".equals( eachUrl.getProtocol() )
                ||
                eachUrl.toExternalForm().endsWith( ".jar" ) )
            {
                resourceUrl = findResourceInJarStream( eachUrl,
                                                       name );
            }
            else
            {
                resourceUrl = findResourceInDirectoryUrl( eachUrl,
                                                          name );
            }

            if ( resourceUrl != null )
            {
                return resourceUrl;
            }
        }

        return null;
    }

    /** Find a resource that potentially exists within a JAR stream.
     *
     *  @param inUrl The jar stream URL.
     *  @param path The resource path to find.
     *
     *  @return The resource URL or <code>null</code> if none found.
     */
    protected URL findResourceInJarStream( URL inUrl,
                                           String path )
    {
        return (URL) ( (Map) jarIndexes.get( inUrl ) ).get( path );
    }

    /** Find a resource that potentially exists within a directory.
     *
     *  @param inUrl The directory URL.
     *  @param path The resource path to find.
     *
     *  @return The resource URL or <code>null</code> if none found.
     */
    protected URL findResourceInDirectoryUrl( URL inUrl,
                                              String path )
    {
        return null;
    }

    /** Attempt to load the bytes of a class from a JAR <code>URL</code>.
     *
     *  @param inUrl The base url.
     *  @param path The path to the desired class.
     *
     *  @return The class bytes or <code>null</code> if not available
     *          via the base url.
     */
    protected byte[] findClassInJarStream( URL inUrl,
                                           String path )
    {
        URL classUrl = (URL) ( (Map) jarIndexes.get( inUrl ) ).get( path );

        if ( classUrl != null )
        {
            try
            {
                return readStream( classUrl.openStream() );
            }
            catch ( IOException e )
            {
                // Swallow
            }
        }

        return null;
    }

    /** Attempt to load the bytes of a class from a directory <code>URL</code>.
     *
     *  @param url The directory url.
     *  @param path The path to the desired class.
     *
     *  @return The class bytes or <code>null</code> if not available
     *          via the base url.
     */
    protected byte[] findClassInDirectoryUrl( URL url,
                                              String path )
    {
        try
        {
            URL classUrl = new URL( url,
                                    path );
        }
        catch ( IOException e )
        {
            // swallow
        }

        return null;
    }

    /** Load a class.
     *
     *  @param name The name of the class to load.
     *  @param resolve If <code>true</code> then resolve the class.
     *
     *  @return The loaded class.
     *
     *  @throws ClassNotFoundException If the class cannot be found.
     */
    protected Class loadClass( String name,
                               boolean resolve ) throws ClassNotFoundException
    {
        return getRealm().loadClass( name );
    }

    /**
     * Read the contents of the provided input stream and return the contents
     * as a byte array.
     */
    private byte[] readStream( InputStream in ) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try
        {
            byte[] buffer = new byte[2048];

            int read = 0;

            while ( in.available() > 0 )
            {
                read = in.read( buffer, 0, buffer.length );

                if ( read < 0 )
                {
                    break;
                }

                out.write( buffer, 0, read );
            }

            return out.toByteArray();
        }
        finally
        {
            out.close();
        }
    }
}
