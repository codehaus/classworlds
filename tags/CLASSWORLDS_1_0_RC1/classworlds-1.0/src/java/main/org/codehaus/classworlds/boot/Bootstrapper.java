package org.codehaus.classworlds.boot;

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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Bootstrapping entry-point.
 *
 *  <p>
 *  The <code>Bootstrapper</code> is to be used for standalone jars
 *  which carry all dependency jars within them.  The layout for
 *  the dependency jar should be similar to:
 *  </p>
 *
 *  <pre>
 *    myjar/
 *          classworlds.conf
 *          org/
 *              codehaus/
 *                     classworlds/
 *                                 boot/
 *                                 protocol/
 *          lib/
 *              myapp.jar
 *              depOne.jar
 *              depTwo.jar
 *  </pre>
 *
 *  @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 *  @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 *
 *  @version $Id$
 */
public class Bootstrapper
{
    // ----------------------------------------------------------------------
    //     Constants
    // ----------------------------------------------------------------------

    /** Main classworlds entry class. */
    public static final String LAUNCHER_CLASS_NAME = "org.codehaus.classworlds.Launcher";

    // ----------------------------------------------------------------------
    //     Instance members
    // ----------------------------------------------------------------------

    /** Command-line args. */
    private String[] args;

    /** Initial bootstrapping classloader. */
    private InitialClassLoader classLoader;

    // ----------------------------------------------------------------------
    //     Class methods
    // ----------------------------------------------------------------------

    /** Main entry-point.
     *
     *  @param args Command-line arguments.
     *
     *  @throws Exception If an error occurs.
     */
    public static void main(String[] args)
        throws Exception
    {
        System.setProperty( "java.protocol.handler.pkgs",
                            "org.codehaus.classworlds.protocol" );

        Bootstrapper bootstrapper = new Bootstrapper( args );

        bootstrapper.bootstrap();
    }

    // ----------------------------------------------------------------------
    //     Constructors
    // ----------------------------------------------------------------------

    /** Construct.
     *
     *  @param args Command-line arguments.
     *
     *  @throws Exception If an error occurs attempting to perform
     *          bootstrap initialization.
     */
    public Bootstrapper(String[] args)
        throws Exception
    {
        this.args        = args;
        this.classLoader = new InitialClassLoader();
    }

    // ----------------------------------------------------------------------
    //     Instance methods
    // ----------------------------------------------------------------------

    /** Retrieve the initial bootstrapping <code>ClassLoader</code>.
     *
     *  @return The classloader.
     */
    protected ClassLoader getInitialClassLoader()
    {
        return this.classLoader;
    }

    /** Perform bootstrap.
     *
     *  @throws Exception If an error occurs while bootstrapping.
     */
    public void bootstrap()
        throws Exception
    {
        ClassLoader cl = getInitialClassLoader();

        Class launcherClass = cl.loadClass( LAUNCHER_CLASS_NAME );

        Method[] methods    = launcherClass.getMethods();
        Method   mainMethod = null;

        for ( int i = 0 ; i < methods.length ; ++i )
        {
            if ( ! "main".equals( methods[i].getName() ) )
            {
                continue;
            }

            int modifiers = methods[i].getModifiers();

            if ( !( Modifier.isStatic( modifiers )
                    &&
                    Modifier.isPublic( modifiers ) ) )
            {
                continue;
            }

            if ( methods[i].getReturnType() != Void.TYPE )
            {
                continue;
            }

            Class[] paramTypes = methods[i].getParameterTypes();

            if (paramTypes.length != 1)
            {
                continue;
            }

            if (paramTypes[0] != String[].class)
            {
                continue;
            }

            mainMethod = methods[i];
            break;
        }

        if ( mainMethod == null )
        {
            throw new NoSuchMethodException( LAUNCHER_CLASS_NAME + "::main(String[] args)" );
        }

        System.setProperty( "classworlds.bootstrapped",
                            "true" );

        mainMethod.invoke( launcherClass,
                           new Object[] { this.args } );
    }
}
