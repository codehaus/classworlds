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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/** Autonomous sub-portion of a <code>ClassWorld</code>.
 *
 *  <p>
 *  This class most closed maps to the <code>ClassLoader</code>
 *  role from Java and in facts can provide a <code>ClassLoader</code>
 *  view of itself using {@link #getClassLoader}.
 *  </p>
 *
 *  @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 *  @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 *
 *  @version $Id$
 */
public interface ClassRealm
{
    /** Retrieve the id.
     *
     *  @return The id.
     */
    String getId();

    /** Retrieve the <code>ClassWorld</code> of which this is a member.
     *
     *  @return The class-world.
     */
    ClassWorld getWorld();

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
    void importFrom(String realmId,
                    String pkgName) throws NoSuchRealmException;

    /** Retrieve the <code>ClassLoader</code> that models this
     *  <code>ClassRealm</code>.
     *
     *  @return The classloader.
     */
    ClassLoader getClassLoader();

    /** Load a class.
     *
     *  @param name The name of the class to load.
     *
     *  @return The loaded class.
     *
     *  @throws ClassNotFoundException If the class cannot be found.
     */
    Class loadClass(String name) throws ClassNotFoundException;

    URL getResource( String name );

    Enumeration findResources( String name ) throws IOException;

    Enumeration getResources(String name) throws IOException;
    
    /** Add a constituent to this realm for locating classes.
     *
     *  <p>
     *  A constituent is a URL that points to either a JAR
     *  format file containing classes and/or resources, or
     *  a directory that should be used for searching.  If
     *  the constituent is a directory, then the URL <b>must</b>
     *  end with a slash (<code>/</code>).  Otherwise the
     *  constituent will be treated as a JAR file.
     *  </p>
     *
     *  @param constituent URL to contituent jar or directory.
     */
    void addConstituent(URL constituent);

    /** Set the parent <code>ClassRealm</code>.
     *
     * @param classRealm The parent ClassRealm.
     */
    void setParent( ClassRealm classRealm );

    /** Create a child realm.
     *
     * @param id The name of child realm.
     */
    ClassRealm createChildRealm( String id );

}
