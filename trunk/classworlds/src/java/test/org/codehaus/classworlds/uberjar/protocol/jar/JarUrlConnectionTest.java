package org.codehaus.classworlds.uberjar.protocol.jar;

import junit.framework.TestCase;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.codehaus.classworlds.uberjar.protocol.jar.JarUrlConnection;

public class JarUrlConnectionTest
    extends TestCase
{

    public void setUp()
    {
        System.setProperty( "java.protocol.handler.pkgs",
                            "org.codehaus.classworlds.uberjar.protocol" );
    }

    public void testConstruct()
        throws Exception
    {
        URL url = buildUrl( "nested.jar",
                            "!/lib/a.jar!/a/A.class" );

        JarUrlConnection connection = new JarUrlConnection( url );

        String[] segments = connection.getSegments();

        assertEquals( 2,
                      segments.length );

        assertEquals( "/lib/a.jar",
                      segments[0] );

        assertEquals( "/a/A.class",
                      segments[1] );

        URL baseResource = connection.getBaseResource();

        assertTrue( baseResource.toExternalForm().startsWith( "file:" ) );
        assertTrue( baseResource.toExternalForm().endsWith( "nested.jar" ) );
    }

    public void testConnect_Simple()
        throws Exception
    {
        URL url = buildUrl( "nested.jar", "" );

        JarUrlConnection connection = new JarUrlConnection( url );

        connection.connect();
    }

    protected URL buildUrl( String jarName,
                            String path )
        throws Exception
    {
        File testDir = new File( System.getProperty( "basedir" ),
                                 "target/test-data" );

        File jarFile = new File( testDir,
                                 jarName );

        URL jarUrl = jarFile.toURL();

        String urlText = "jar:" + jarUrl + path;

        System.err.println( "url-text: " + urlText );

        URL url = new URL( urlText );

        System.err.println( "url: " + url );

        return url;

    }

    public void testNormaliseURL() throws MalformedURLException
    {
        testNormaliseURL( "jar:http://localhost/ted.jar!/", "http://localhost/ted.jar" );

    }

    public void testNormaliseURL( String expected, String input ) throws MalformedURLException
    {
        assertEquals( "JarUrlConnection.normaliseURL(" + input + ")", new URL( expected ), JarUrlConnection.normaliseURL( new URL( input ) ) );
    }

    public void testConstructionMalformed( String expected, String input, Class exception ) throws Exception
    {

        String method = "JarUrlConnection.normaliseURL(" + input + ")";
        try
        {
            new JarUrlConnection( new URL( input ) );
            if ( exception != null )
            {
                fail( method + " should have thrown exception - " + exception.getName() );
            }
        }
        catch ( Exception e )
        {
            if ( exception != null && exception.isInstance( e ) )
            {
                //Success
                return;
            }
            throw e;
        }
    }

    public void testMalformedURL() throws Exception
    {
        testConstructionMalformed( "", "http://!!!", MalformedURLException.class );
        testConstructionMalformed( "", "jar://!!!/", MalformedURLException.class );
        testConstructionMalformed( "", "jar:flan://!/", MalformedURLException.class );
        testConstructionMalformed( "", "jar:file:///fred.jar!/", null );
    }
}
