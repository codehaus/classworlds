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

import org.codehaus.classworlds.uberjar.UberJarRealmClassLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeSet;


/**
 * Implementation of <code>ClassRealm</code>.
 *
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 * @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 * @version $Id$
 * @todo allow inheritance to be turn on/off at runtime.
 * @todo allow direction of search
 */
public class DefaultClassRealm
    implements ClassRealm
{
    private ClassWorld world;

    private String id;

    private TreeSet imports;

    private ClassLoader classLoader;

    private ClassRealm parent;

    public DefaultClassRealm( ClassWorld world, String id )
    {
        this( world, id, null );
    }

    public DefaultClassRealm( ClassWorld world, String id, ClassLoader classLoader )
    {
        this.world = world;

        this.id = id;

        imports = new TreeSet();

        if ( classLoader != null )
        {
            this.classLoader = classLoader;
        }
        else
        {
            if ( "true".equals( System.getProperty( "classworlds.bootstrapped" ) ) )
            {
                this.classLoader = new UberJarRealmClassLoader();
            }
            else
            {
                this.classLoader = new RealmClassLoader();
            }
        }
    }

    public URL[] getConstituents()
    {
        if ( classLoader instanceof URLClassLoader )
        {
            return ((URLClassLoader)classLoader).getURLs();
        }

        return new URL[0];
    }

    public ClassRealm getParent()
    {
        return parent;
    }

    public void setParent( ClassRealm parent )
    {
        this.parent = parent;
    }

    public String getId()
    {
        return this.id;
    }

    public ClassWorld getWorld()
    {
        return this.world;
    }

    public void importFrom( String realmId, String packageName )
        throws NoSuchRealmException
    {
        imports.add( new Entry( getWorld().getRealm( realmId ), packageName ) );
    }

    public void addConstituent( URL constituent )
    {
        ( (RealmClassLoader) classLoader ).addConstituent( constituent );
    }

    public ClassRealm locateSourceRealm( String classname )
    {
        for ( Iterator iterator = imports.iterator(); iterator.hasNext(); )
        {
            Entry entry = (Entry) iterator.next();

            if ( entry.matches( classname ) )
            {
                return entry.getRealm();
            }
        }

        return this;
    }

    public ClassLoader getClassLoader()
    {
        return classLoader;
    }

    public ClassRealm createChildRealm( String id )
    {
        ClassRealm childRealm = new DefaultClassRealm( getWorld(), id );

        childRealm.setParent( this );

        return childRealm;
    }

    // ----------------------------------------------------------------------
    // Classloading
    // ----------------------------------------------------------------------

    public Class loadClass( String name )
        throws ClassNotFoundException
    {
        if ( name.startsWith( "org.codehaus.classworlds." ) )
        {
            return getWorld().loadClass( name );
        }

        return getClassFromRealm( locateSourceRealm( name ), name );
    }

    public Class getClassFromRealm( ClassRealm realm, String name )
        throws ClassNotFoundException
    {
        try
        {
            return realm.getClassLoader().loadClass( name );
        }
        catch ( ClassNotFoundException e )
        {
            if ( realm.getParent() != null )
            {
                return getParent().loadClass( name );
            }

            throw e;
        }
    }

    // ----------------------------------------------------------------------
    // Resource handling
    // ----------------------------------------------------------------------

    private URL getResourceFromRealm( ClassRealm realm, String name )
    {
        URL resource = realm.getClassLoader().getResource( UrlUtils.normalizeUrlPath( name ) );

        if ( resource == null && realm.getParent() != null )
        {
            return getResourceFromRealm( realm.getParent(), name );
        }

        return resource;
    }

    public URL getResource( String name )
    {
        return getResourceFromRealm( this, name );
    }

    // ----------------------------------------------------------------------
    // Resources handling
    // ----------------------------------------------------------------------

    private Enumeration getResourcesFromRealm( ClassRealm realm, String name )
        throws IOException
    {
        Enumeration resources = realm.getClassLoader().getResources( UrlUtils.normalizeUrlPath( name ) );

        if ( resources == null && realm.getParent() != null )
        {
            return getResourcesFromRealm( realm.getParent(), name );
        }

        return resources;
    }

    public Enumeration getResources( String name )
        throws IOException
    {
        return getResourcesFromRealm( this, name );
    }

    // ----------------------------------------------------------------------
    // Stream handling
    // ----------------------------------------------------------------------

    public InputStream getResourceAsStream( String name )
    {
        URL url = getResourceFromRealm( this, name );

        InputStream is = null;

        if ( url != null )
        {
            try
            {
                is = url.openStream();
            }
            catch ( IOException e )
            {
                // do nothing
            }
        }

        return is;
    }
}
