package org.codehaus.classworlds.uberjar.protocol.jar;

import junit.framework.TestCase;

import java.net.URL;

public class HandlerTest
    extends TestCase
{

    public void setUp()
    {
        System.setProperty( "java.protocol.handler.pkgs",
                            "org.codehaus.classworlds.uberjar.protocol" );
    }

    public void testSimpleImplicit()
        throws Exception
    {
        URL url = new URL( "jar:/foo.jar" );

        assertEquals( "jar:/foo.jar",
                      url.toExternalForm() );
    }

    public void testRelativeFile()
        throws Exception
    {
        URL url = buildUrl( "jar:/path/to/foo.jar",
                            "bar.jar" );

        assertEquals( "jar:/path/to/bar.jar",
                      url.toExternalForm() );
    }

    public void testRelativeSegment()
        throws Exception
    {
        URL url = buildUrl( "jar:/path/to/foo.jar!/segment.jar",
                            "!/other-segment.jar" );

        assertEquals( "jar:/path/to/foo.jar!/other-segment.jar",
                      url.toExternalForm() );
    }

    public void testRelativeMultiSegment()
        throws Exception
    {
        URL url = buildUrl( "jar:/path/to/foo.jar!/segment-a.jar!/segment-b.jar",
                            "!/other-segment.jar#cheese" );

        assertEquals( "jar:/path/to/foo.jar!/segment-a.jar!/other-segment.jar",
                      url.toExternalForm() );
    }

    protected URL buildUrl(String contextText,
                           String urlText)
        throws Exception
    {
        URL context = new URL( contextText );

        URL url = new URL( context,
                           urlText );

        System.err.println( "build('" + context + "', '" + urlText + "') -- " + url );

        return url;
    }
}

