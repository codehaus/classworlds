package org.codehaus.classworlds;

import junit.framework.TestCase;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

public class RealmClassLoaderTest
    extends TestCase
{
    private ClassWorld world;
    private ClassRealm realm;

    public void setUp()
        throws Exception
    {
        System.setProperty( "java.protocol.handler.pkgs",
                            "org.codehaus.classworlds.protocol" );

        System.setProperty( "classworlds.bootstrapped",
                            "true" );

        this.world = new ClassWorld();

        this.realm = this.world.newRealm( "realm" );
    }

    public void testFindResource_Simple()
        throws Exception
    {
        URL url = getJarUrl( "nested.jar" );

        this.realm.addConstituent( url );

        RealmClassLoader cl = (RealmClassLoader) this.realm.getClassLoader();

        URL resource = cl.findResource( "nested.properties" );

        assertNotNull( resource );

        byte[] buffer = new byte[1024];
        int read = 0;
        StringBuffer content = new StringBuffer();

        InputStream in = resource.openStream();

        while ( ( read = in.read( buffer,
                                  0,
                                  1024 ) ) >= 0 )
        {
            content.append( new String( buffer,
                                        0,
                                        read ) );
        }

        assertTrue( content.toString().startsWith( "nested.properties" ) );
    }

    public void testGetResourceAsStream_Simple()
        throws Exception
    {
        URL url = getJarUrl( "nested.jar" );

        this.realm.addConstituent( url );

        RealmClassLoader cl = (RealmClassLoader) this.realm.getClassLoader();

        InputStream in = cl.getResourceAsStream( "nested.properties" );

        assertNotNull( in );

        byte[] buffer = new byte[1024];
        int read = 0;
        StringBuffer content = new StringBuffer();

        while ( ( read = in.read( buffer,
                                  0,
                                  1024 ) ) >= 0 )
        {
            content.append( new String( buffer,
                                        0,
                                        read ) );
        }

        assertTrue( content.toString().startsWith( "nested.properties" ) );
    }

    public void testStandardJarUrl()
        throws Exception
    {
        File testDir = new File ( System.getProperty( "basedir" ), "target/test-data" );
        URL url = new URL( "jar:file:" + testDir + "/a.jar!/" );
        // URL url = new URL( "file:" + testDir + "/a.jar" );
        
        // This will produce something like the following:
        //
        // jar:file:/home/jvanzyl/js/org/codehaus/classworlds/test-data/a.jar!/
        
        this.realm.addConstituent( url );

        RealmClassLoader cl = (RealmClassLoader) this.realm.getClassLoader();

        cl.loadClass( "a.A" );
    }


    public void testFindResource_NotFound()
        throws Exception
    {
        URL url = getJarUrl( "nested.jar" );

        this.realm.addConstituent( url );

        RealmClassLoader cl = (RealmClassLoader) this.realm.getClassLoader();

        URL resource = cl.findResource( "deadbeef" );

        assertNull( resource );
    }

    public void testGetResourceAsStream_NotFound()
        throws Exception
    {
        URL url = getJarUrl( "nested.jar" );

        this.realm.addConstituent( url );

        RealmClassLoader cl = (RealmClassLoader) this.realm.getClassLoader();

        InputStream in = cl.getResourceAsStream( "deadbeef" );

        assertNull( in );
    }

    public void testFindResource_Nested()
        throws Exception
    {
        URL url = buildUrl( "nested.jar",
                            "!/lib/a.jar");

        this.realm.addConstituent( url );

        RealmClassLoader cl = (RealmClassLoader) this.realm.getClassLoader();

        URL resource = cl.findResource( "a.properties" );

        assertNotNull( resource );

        byte[] buffer = new byte[1024];
        int read = 0;
        StringBuffer content = new StringBuffer();

        InputStream in = resource.openStream();

        while ( ( read = in.read( buffer,
                                  0,
                                  1024 ) ) >= 0 )
        {
            content.append( new String( buffer,
                                        0,
                                        read ) );
        }

        assertTrue( content.toString().startsWith( "a properties" ) );
    }

    public void testGetResourceAsStream_Nested()
        throws Exception
    {
        URL url = buildUrl( "nested.jar",
                            "!/lib/a.jar");

        this.realm.addConstituent( url );

        RealmClassLoader cl = (RealmClassLoader) this.realm.getClassLoader();

        InputStream in = cl.getResourceAsStream( "a.properties" );

        assertNotNull( in );

        byte[] buffer = new byte[1024];
        int read = 0;
        StringBuffer content = new StringBuffer();

        while ( ( read = in.read( buffer,
                                  0,
                                  1024 ) ) >= 0 )
        {
            content.append( new String( buffer,
                                        0,
                                        read ) );
        }

        assertTrue( content.toString().startsWith( "a properties" ) );
    }
    
    protected URL getJarUrl(String jarName)
        throws Exception
    {
        File testDir = new File ( System.getProperty( "basedir" ),
                                  "target/test-data" );

        File jarFile = new File( testDir,
                                 jarName );


        String urlText = "jar:" + jarFile.toURL();

        return new URL( urlText );
    }

    protected URL buildUrl(String jarName,
                           String path)
        throws Exception
    {
        URL jarUrl = getJarUrl( jarName );

        String urlText = jarUrl.toExternalForm() + path;

        URL url = new URL( urlText );

        return url;
    }
}
