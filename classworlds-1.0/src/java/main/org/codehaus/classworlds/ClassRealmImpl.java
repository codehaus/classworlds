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
import java.util.TreeSet;
import java.util.Set;
import java.util.Iterator;

/** Implementation of <code>ClassRealm</code>.
 *
 *  @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 *  @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 *
 *  @version $Id$
 *
 * @todo allow inheritance to be turn on/off at runtime.
 * @todo allow direction of search
 */
class ClassRealmImpl implements ClassRealm
{
    // ------------------------------------------------------------
    //     Instance members
    // ------------------------------------------------------------

    /** The world of which this realm is a member. */
    private ClassWorld world;

    /** The id of this realm. */
    private String id;

    /** Import spec entries. */
    private Set imports;

    /** The classloader. */
    private RealmClassLoader classLoader;

    /** Parent ClassRealm */
    private ClassRealm parent;

    // ------------------------------------------------------------
    //     Constructors
    // ------------------------------------------------------------

    /** Construct.
     *
     *  @param world The world of which this realm is a member.
     *  @param id This realm's id.
     */
    ClassRealmImpl( ClassWorld world,
                    String id )
    {
        this.world = world;
        this.id = id;

        this.imports = new TreeSet();

        // We need to detect whether we are running in an UberJar
        // or not.
        if ( "true".equals( System.getProperty( "classworlds.bootstrapped" ) ) )
        {
            this.classLoader = new UberJarRealmClassLoader( this );
        }
        else
        {
            this.classLoader = new RealmClassLoader( this );
        }
    }

    // ------------------------------------------------------------
    //     Instance methods
    // ------------------------------------------------------------

    /**
     *
     * @return
     */
    public ClassRealm getParent()
    {
        return parent;
    }

    /**
     *
     * @param parent
     */
    public void setParent( ClassRealm parent )
    {
        this.parent = parent;
    }

    /** Retrieve the id.
     *
     *  @return The id.
     */
    public String getId()
    {
        return this.id;
    }

    /** Retrieve the <code>ClassWorld</code>.
     *
     *  @return The world.
     */
    public ClassWorld getWorld()
    {
        return this.world;
    }

    /** Import packages from another <code>ClassRealm</code>.
     *
     *  <p>
     *  Specific packages can be imported from another realm
     *  instead of attempting to load them locally from this
     *  one.  When importing a package a realm defers <b>completely</b>
     *  to the foreign realm to satisfy the package dependencies.
     *  </p>
     *
     *  @param realmId The realm id from which to import.
     *  @param pkgName The package name to import.
     *
     *  @throws NoSuchRealmException If the id of the realm from which
     *          to import does not correspond to a foreign realm within
     *          this realm's world.
     */
    public void importFrom( String realmId,
                            String pkgName ) throws NoSuchRealmException
    {
        this.imports.add( new Entry( getWorld().getRealmImpl( realmId ),
                                     pkgName ) );
    }

    /** Add a constituent to this realm for locating classes.
     *
     *  @param constituent URL to contituent jar or directory.
     */
    public void addConstituent( URL constituent )
    {
        this.classLoader.addConstituent( constituent );
    }

    /** Locate the <code>ClassRealm</code> that should
     *  satisfy loading of a class.
     *
     *  @param classname The name of the class to load.
     *
     *  @return The appropriate realm.
     */
    ClassRealmImpl locateSourceRealm( String classname )
    {
        Iterator entryIter = this.imports.iterator();
        Entry eachEntry = null;

        while ( entryIter.hasNext() )
        {
            eachEntry = (Entry) entryIter.next();

            if ( eachEntry.matches( classname ) )
            {
                return eachEntry.getRealm();
            }
        }

        return this;
    }

    /** Retrieve the <code>ClassLoader</code> view of
     *  this realm.
     *
     *  @return The class-loader view of this realm.
     */
    public ClassLoader getClassLoader()
    {
        return this.classLoader;
    }

    /** Load a class.
     *
     *  @param name The name of the class to load.
     *
     *  @return The loaded class.
     *
     *  @throws ClassNotFoundException If the class cannot be found.
     */
    public Class loadClass( String name ) throws ClassNotFoundException
    {
        if ( name.startsWith( "org.codehaus.classworlds." ) )
        {
            return getWorld().loadClass( name );
        }

        ClassRealmImpl sourceRealm = locateSourceRealm( name );

        if ( sourceRealm == this )
        {
            return loadClassDirect( name );
        }

        return sourceRealm.loadClass( name );
    }

    /** Load a class.
     *
     *  @param name The name of the class to load.
     *
     *  @return The loaded class.
     *
     *  @throws ClassNotFoundException If the class cannot be found.
     */
    Class loadClassDirect( String name ) throws ClassNotFoundException
    {
        // 1. Try this realm's ClassLoader.
        // 2. If the realm has a parent try the parent's ClassLoader.

        Class clazz = null;

        try
        {
            clazz = this.classLoader.loadClassDirect( name );
        }
        catch ( ClassNotFoundException cnfe1 )
        {
            if ( getParent() != null )
            {
                try
                {
                    clazz = getParent().getClassLoader().loadClass( name );
                }
                catch ( ClassNotFoundException cnfe2 )
                {
                    throw cnfe2;
                }
            }
            else
            {
                throw cnfe1;
            }
        }

        return clazz;
    }

    /** Retrieve a resource.
     *
     *  @param name The resource name.
     *
     *  @return The URL to the located resource or <code>null</code>
     *          if none could be located.
     */
    public URL getResource( String name )
    {
        ClassRealmImpl sourceRealm = locateSourceRealm( name );

        if ( sourceRealm == this )
        {
            return loadResourceDirect( name );
        }

        return sourceRealm.getResource( name );
    }


    URL loadResourceDirect( String name )
    {
        // 1. Try this realm's ClassLoader.
        // 2. If the realm has a parent try the parent's ClassLoader.

        URL resource = this.classLoader.findResource( name );

        if ( resource == null
             &&
             getParent() != null )
        {
            resource = getParent().getResource( name );
        }

        return resource;
    }

    /**
     * @see ClassRealm#createChildRealm
     */
    public ClassRealm createChildRealm( String id )
    {
        ClassRealm childRealm = new ClassRealmImpl( getWorld(), id );
        childRealm.setParent( this );

        return childRealm;
    }
}
