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


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;


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
class DefaultClassRealm implements ClassRealm
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

    /** Parent ClassRealm */
    private ClassLoader parentClassLoader;

    /** Secondary ClassLoaders */
    List classLoaders;       //left as package variable for unit testing
    Map  urlCache;       //left as package variable for unit testing

    /** totals used for garbage collection
      * left as package variables for unit testing
      */
    int totalUniqueURLs;
    int totalURLs;
    int totalClassLoaders;
    int maxClassLoaders;

    // ------------------------------------------------------------
    //     Constructors
    // ------------------------------------------------------------

    /** Construct.
     *
     *  @param world The world of which this realm is a member.
     *  @param id This realm's id.
     *  @param parentClassLoader The class loader which to inherit from.
     */
    DefaultClassRealm( ClassWorld world,
                       String id,
                       ClassLoader parentClassLoader )
    {
        this.world = world;
        this.id = id;
        this.parentClassLoader = parentClassLoader;

        this.imports = new TreeSet();
        this.classLoaders = new ArrayList();
        this.urlCache = new HashMap();

        // We need to detect whether we are running in an UberJar
        // or not.
        if ( "true".equals( System.getProperty( "classworlds.bootstrapped" ) ) )
        {
            this.classLoader = new UberJarRealmClassLoader( this );
        }
        else
        {
            this.classLoader = new RealmClassLoader( this, parentClassLoader );
        }

        this.totalUniqueURLs = 0;
        this.totalURLs = 0;
        this.totalClassLoaders = 1;
        this.maxClassLoaders = 1;
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
     *
     */
    public void setMaxClassLoaders(int maxClassLoaders)
    {
      this.maxClassLoaders = maxClassLoaders;
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

    /**
     *  Add a constituent to this realm for locating classes.
     *  Currently has a very simple garbage collection algorithm,
     *  that is just a maxClassLoaders, but it could be a compination
     *  of totaClassLoaders, totalURLs and totalUniqueURLs as well.
     *  @todo : decent garbage collection algorithm
     *  @param constituent URL to contituent jar or directory.
     */
    public void addConstituent( URL constituent )
    {
        // check if constituent is in the current primary classLoader
        if (containsURL(this.classLoader, constituent))
        {
            // do we perform garbage collection
            if (this.totalClassLoaders < this.maxClassLoaders)
            {
                // no gc, so add primary to list, create new primary and add
                // constituent
                classLoaders.add(this.classLoader);
                this.classLoader = new RealmClassLoader(this);
                this.classLoader.addConstituent(constituent);
                this.urlCache.put(constituent.toExternalForm(), constituent);
                this.totalClassLoaders++;
                this.totalURLs++;
            }
            else
            {
                // add latest version of class and then reload(); - do garbage
                // collection
                this.urlCache.put(constituent.toExternalForm(), constituent);
                reload( false );
            }
        }
        else
        {
            //add constituent to the primary classLoader
            this.classLoader.addConstituent(constituent);
            this.urlCache.put(constituent.toExternalForm(), constituent);
            this.totalURLs++;
        }

        this.totalUniqueURLs = urlCache.size();
    }

    /**
     *  Adds a byte[] class definition as a constituent for locating classes.
     *  Currently uses BytesURLStreamHandler to hold a reference of the byte[] in memory.
     *  This ensures we have a unifed URL resource model for all constituents.
     *  The code to cache to disk is commented out - maybe a property to choose which method?
     *
     *  @param name class name
     *  @param b the class definition as a byte[]
     */
    public void addConstituent(String constituent,
                               byte[] b) throws ClassNotFoundException
    {
        try
        {
            File path, file;
            if (constituent.lastIndexOf('.') != -1)
            {
                path = new File("byteclass/" + constituent.substring(0, constituent.lastIndexOf('.') + 1).replace('.', File.separatorChar));
                /*
                if (!path.exists())
                {
                    path.mkdirs();
                }
                if ((path.getParentFile() != null)&&(!path.getParentFile().exists()))
                {
                    path.getParentFile().mkdirs();
                }
                file = File.createTempFile(constituent.substring(constituent.lastIndexOf('.') + 1), ".class", path);
                */
                file = new File(path, constituent.substring(constituent.lastIndexOf('.') + 1) + ".class");
            }
            else
            {
                path = new File("byteclass/");
                //file = File.createTempFile(constituent, ".class", path);
                file = new File(path, constituent + ".class");
            }
            /*
            FileOutputStream os = new FileOutputStream(path.getPath());
            os.write(b);
            os.close();
            url = path.toURL();
            */
            addConstituent( new URL( null,
                                     file.toURL().toExternalForm(),
                                     new BytesURLStreamHandler(b) ) );
        }
        catch (java.io.IOException e)
        {
            throw new ClassNotFoundException( "Couldn't load byte stream.", e );
        }
    }

    /** Locate the <code>ClassRealm</code> that should
     *  satisfy loading of a class.
     *
     *  @param classname The name of the class to load.
     *
     *  @return The appropriate realm.
     */
    DefaultClassRealm locateSourceRealm( String classname )
    {
        Iterator entryIter = this.imports.iterator();

        while ( entryIter.hasNext() )
        {
            Entry eachEntry = (Entry) entryIter.next();

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
        //must return a single unified classLoader, perform GC if necessary.
        if (this.classLoaders.size() > 0)
        {
          reload( false );
        }
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

        DefaultClassRealm sourceRealm = locateSourceRealm( name );

        if ( sourceRealm == this )
        {
            return loadClassDirect( name );
        }
        else
        {
            try
            {
                return sourceRealm.loadClass( name );
            }
            catch ( ClassNotFoundException cnfe )
            {
                // If we can't find it in an import, try loading directly.
                return loadClassDirect( name );
            }
        }
    }

    /** Load a class.  First try this realm's class loader, then
     *  the parent's if there is one.
     *
     * when trying this realm,perform the following sequences:
     *  1. Try this realm's primary ClassLoader.
     *  2. Try secondary classLoaders in order of precedent.
     *  3. If the realm has a parent try the parent's ClassLoader.
     *
     *  @param name The name of the class to load.
     *
     *  @return The loaded class.
     *
     *  @throws ClassNotFoundException If the class cannot be found.
     */
    Class loadClassDirect( String name ) throws ClassNotFoundException
    {
        Class clazz = null;

        try
        {
            clazz = this.classLoader.loadClassDirect( name );
        }
        catch ( ClassNotFoundException cnfe1 )
        {
            if (this.classLoaders.size() > 0)
            {
                for (int i = this.classLoaders.size() - 1; i >= 0; i--)
                {
                    try
                    {
                        clazz = ((RealmClassLoader)this.classLoaders.get(i)).loadClassDirect( name );
                        break;
                    }
                    catch ( ClassNotFoundException cnfe2 )
                    {
                    }
                }
            }

            if (clazz == null)
            {
                if (getParent() != null)
                {
                    clazz = getParent().getClassLoader().loadClass( name );
                }
                else
                {
                    throw cnfe1;
                }
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
        name = UrlUtils.normalizeUrlPath( name );

        // Search imports *first*.
        DefaultClassRealm sourceRealm = locateSourceRealm( name );

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

        //must use a single unified classLoader, perform GC if necessary.
        if (this.classLoaders.size() > 0)
        {
          reload( false );
        }
        URL resource = this.classLoader.getResourceFromClassLoader( name );

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
        /* Don't pass along the parent ClassLoader, because the child realm
         * will default to it.  Instead, the child realm will fall back on this
         * realm which will have the class loader.
         */
        ClassRealm childRealm = new DefaultClassRealm( getWorld(), id, null );
        childRealm.setParent( this );

        return childRealm;
    }

    /**
     * @param name
     * @return
     */
    public Enumeration findResources(String name)
        throws IOException
    {
        name = UrlUtils.normalizeUrlPath( name );

        Vector resources = new Vector();

        //must use a single unified classLoader, perform GC if necessary.
        if (this.classLoaders.size() > 0)
        {
          reload( false );
        }

        // Attempt to load directly first, then go to the imported packages.
        Enumeration direct = classLoader.findResourcesFromClassLoader( name );

        while ( direct.hasMoreElements() )
        {
            resources.addElement( direct.nextElement() );
        }

        // Find resources from the parent realm.
        if ( parent != null )
        {
            Enumeration parent = getParent().getResources( name );

            while ( parent.hasMoreElements() )
                resources.addElement( parent.nextElement() );
        }

        // TODO: get resources from imports too!

        return resources.elements();
    }

    public Enumeration getResources(String name)
        throws IOException
    {
        //must use a single unified classLoader, perform GC if necessary.
        if (this.classLoaders.size() > 0)
        {
          reload( false );
        }
        return this.classLoader.getResources( name );
    }

    /**
     * Checks to see if a given classLoader contains the given URL
     *
     */
    private static boolean containsURL(RealmClassLoader classLoader, URL url)
    {
        boolean contains = false;
        String urlStr;
        String srcUrlStr = url.toExternalForm();
        URL[] urls = classLoader.getURLs();

        for (int i=0; i < urls.length; i++)
        {
            urlStr = urls[i].toExternalForm();
            if (srcUrlStr.equals(urlStr ))
            {
                contains = true;
                break;
            }
        }
        return contains;
    }

    /**
     * Quick access reload method, doesn't perform a reload on parent
     */
    public void reload()
    {
        reload( false );
    }

    /**
     * This drops the primary classLoader, and the clears the secondary List
     * It then uses the HashMap to populate a single class load with
     * all the constituents. It also resets the stats values.
     *
     *@todo: if this fails, the current realm could be broken, need to rollback/throw exception, although it shouldn't fail
     */
    public void reload(boolean reloadParent)
    {
        this.classLoader = new RealmClassLoader( this );

        URL url;
        String urlStr;
        Set keys = this.urlCache.keySet();
        Iterator it = keys.iterator();

        while (it.hasNext())
        {
            urlStr = (String) it.next();
            this.classLoader.addConstituent((URL) this.urlCache.get(urlStr));
        }

        this.classLoaders.clear();
        this.totalUniqueURLs = this.urlCache.size();
        this.totalURLs = this.totalUniqueURLs;
        this.totalClassLoaders = 1;

        if (reloadParent && (getParent() != null))
        {
            getParent().reload(reloadParent);
        }
    }
}
