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

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

/** A collection of <code>ClassRealm</code>s, indexed by id.
 *
 *  @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 *
 *  @version $Id$
 */
public class ClassWorld
{
    // ------------------------------------------------------------
    //     Instance members
    // ------------------------------------------------------------

    /** Realms, indexed by id. */
    private Map realms;

    // ------------------------------------------------------------
    //     Constructors
    // ------------------------------------------------------------

    /** Construct.
     */
    public ClassWorld()
    {
        this.realms = new HashMap();
    }
    
    // ------------------------------------------------------------
    //     Instance methods
    // ------------------------------------------------------------
    
    /** Create a new <code>ClassRealm</code>.
     *
     *  @param id The realm id.
     *
     *  @return The newly created realm.
     *
     *  @throws DuplicateRealmException If this class-world already
     *          contains a realm with the specified id.
     */
    public ClassRealm newRealm(String id) throws DuplicateRealmException
    {
        return newRealm(id, null);
    }
    
    /** Create a new <code>ClassRealm</code>.
     *
     *  @param id The realm id.
     *
     *  @return The newly created realm.
     *
     *  @throws DuplicateRealmException If this class-world already
     *          contains a realm with the specified id.
     */
    public ClassRealm newRealm(String id,
                               ClassLoader classLoader)
        throws DuplicateRealmException
    {
        if ( this.realms.containsKey( id ) )
        {
            throw new DuplicateRealmException( this,
                                               id );
        }

        ClassRealm realm = null;
        
        // Create a realm for the classloader that is parent realm.
        // This bypasses parent ClassLoader issues.
        if ( classLoader != null )
        {
            String parentRealmId = id + "_ParentRealm";
            
            DefaultClassRealm parentRealm = new DefaultClassRealm( this,
                                                                   parentRealmId,
                                                                   classLoader );
            
            realm = parentRealm.createChildRealm( id );
            
            this.realms.put( parentRealmId, parentRealm );
        }
        else
        {
            realm = new DefaultClassRealm( this,
                                           id, 
                                           null );
        }

        this.realms.put( id,
                         realm );

        return realm;
    }

    public void disposeRealm(String id) throws NoSuchRealmException
    {
        ClassRealm realm = getRealm( id );
        
        realms.remove(id);
    }

    /** Retrieve a <code>ClassRealm</code> by its id.
     *
     *  @param id The id.
     *
     *  @return The matching class-realm.
     *
     *  @throws NoSuchRealmException If the id does not correspond
     *          to any known class-realm.
     */
    public ClassRealm getRealm(String id) throws NoSuchRealmException
    {
        if ( this.realms.containsKey( id ) )
        {
            return (ClassRealm) this.realms.get( id );
        }

        throw new NoSuchRealmException( this,
                                        id );
    }

    /** Retrieve a <code>ClassRealmImpl</code> by its id.
     *
     *  @param id The id.
     *
     *  @return The matching class-realm.
     *
     *  @throws NoSuchRealmException If the id does not correspond
     *          to any known class-realm.
     */
    DefaultClassRealm getRealmImpl(String id) throws NoSuchRealmException
    {
        return (DefaultClassRealm) getRealm( id );
    }

    /** Retrieve the collection of all <code>ClassRealm</code>s
     *  in this world.
     *
     *  @return The collectio of class-realms.
     */
    public Collection getRealms()
    {
        return this.realms.values();
    }

    /** Load a class.
     *
     *  @param name The name of the class to load.
     *
     *  @return The loaded class. 
     *
     *  @throws ClassNotFoundException If the class cannot be found.
     */
    Class loadClass(String name) throws ClassNotFoundException
    {
        return getClass().getClassLoader().loadClass( name );
    }
}
