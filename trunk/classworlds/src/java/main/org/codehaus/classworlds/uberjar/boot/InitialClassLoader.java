package org.codehaus.classworlds.uberjar.boot;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.Map;
import java.util.HashMap;
import java.util.jar.JarInputStream;
import java.util.jar.JarEntry;

/**
 * Initial bootstrapping <code>ClassLoader</code>.
 *
 * @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 * @version $Id$
 */
public class InitialClassLoader
    extends SecureClassLoader
{
    // ----------------------------------------------------------------------
    //     Instance members
    // ----------------------------------------------------------------------

    /**
     * Class index.
     */
    private Map index;

    /**
     * Classworlds jar URL.
     */
    private URL classworldsJarUrl;

    // ----------------------------------------------------------------------
    //     Constructors
    // ----------------------------------------------------------------------

    /**
     * Construct.
     *
     * @throws Exception If an error occurs while attempting to perform
     *                   bootstrap initialization.
     */
    public InitialClassLoader()
        throws Exception
    {
        this.index = new HashMap();

        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        URL classUrl = getClass().getResource( "InitialClassLoader.class" );

        String urlText = classUrl.toExternalForm();

        int bangLoc = urlText.indexOf( "!" );

        System.setProperty( "classworlds.lib",
                            urlText.substring( 0,
                                               bangLoc ) + "!/WORLDS-INF/lib" );

        this.classworldsJarUrl = new URL( urlText.substring( 0,
                                                             bangLoc ) + "!/WORLDS-INF/classworlds.jar" );
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

    /**
     * @see ClassLoader
     */
    public synchronized Class findClass( String className )
        throws ClassNotFoundException
    {
        String classPath = className.replace( '.',
                                              '/' ) + ".class";

        if ( this.index.containsKey( classPath ) )
        {
            return (Class) this.index.get( classPath );
        }

        try
        {
            JarInputStream in = new JarInputStream( this.classworldsJarUrl.openStream() );

            try
            {
                JarEntry entry = null;

                while ( ( entry = in.getNextJarEntry() ) != null )
                {
                    if ( entry.getName().equals( classPath ) )
                    {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        try
                        {
                            byte[] buffer = new byte[2048];

                            int read = 0;

                            while ( in.available() > 0 )
                            {
                                read = in.read( buffer,
                                                0,
                                                buffer.length );

                                if ( read < 0 )
                                {
                                    break;
                                }

                                out.write( buffer,
                                           0,
                                           read );
                            }

                            buffer = out.toByteArray();

                            Class cls = defineClass( className,
                                                     buffer,
                                                     0,
                                                     buffer.length );

                            this.index.put( className,
                                            cls );

                            return cls;
                        }
                        finally
                        {
                            out.close();
                        }
                    }
                }
            }
            finally
            {
                in.close();
            }
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw new ClassNotFoundException( "io error reading stream for: " + className );
        }

        throw new ClassNotFoundException( className );
    }
}
