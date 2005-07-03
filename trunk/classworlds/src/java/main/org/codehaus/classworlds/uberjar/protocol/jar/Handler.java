package org.codehaus.classworlds.uberjar.protocol.jar;

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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * <code>URLStreamHandler</code> for <code>jar:</code> protocol <code>URL</code>s.
 *
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 * @version $Id$
 */
public class Handler
    extends URLStreamHandler
{
    // ----------------------------------------------------------------------
    //     Class members
    // ----------------------------------------------------------------------

    /**
     * Singleton instance.
     */
    private static final Handler INSTANCE = new Handler();

    // ----------------------------------------------------------------------
    //     Class methods
    // ----------------------------------------------------------------------

    /**
     * Retrieve the singleton instance.
     *
     * @return The singleton instance.
     */
    public static Handler getInstance()
    {
        return INSTANCE;
    }

    // ----------------------------------------------------------------------
    //     Constructors
    // ----------------------------------------------------------------------


    /**
     * Construct.
     */
    public Handler()
    {
        // intentionally left blank
    }

    // ----------------------------------------------------------------------
    //     Instance methods
    // ----------------------------------------------------------------------

    /**
     * @see java.net.URLStreamHandler
     */
    public URLConnection openConnection( URL url )
        throws IOException
    {
        return new JarUrlConnection( url );
    }

    /**
     * @see java.net.URLStreamHandler
     */
    public void parseURL( URL url,
                          String spec,
                          int start,
                          int limit )
    {
        String specPath = spec.substring( start,
                                          limit );

        String urlPath = null;

        if ( specPath.charAt( 0 ) == '/' )
        {
            urlPath = specPath;
        }
        else if ( specPath.charAt( 0 ) == '!' )
        {
            String relPath = url.getFile();

            int bangLoc = relPath.lastIndexOf( "!" );

            if ( bangLoc < 0 )
            {
                urlPath = relPath + specPath;
            }
            else
            {
                urlPath = relPath.substring( 0,
                                             bangLoc ) + specPath;
            }
        }
        else
        {
            String relPath = url.getFile();

            if ( relPath != null )
            {
                int lastSlashLoc = relPath.lastIndexOf( "/" );

                if ( lastSlashLoc < 0 )
                {
                    urlPath = "/" + specPath;
                }
                else
                {
                    urlPath = relPath.substring( 0,
                                                 lastSlashLoc + 1 ) + specPath;
                }
            }
            else
            {
                urlPath = specPath;
            }
        }

        setURL( url,
                "jar",
                "",
                0,
                null,
                null,
                urlPath,
                null,
                null );
    }
}
