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

import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;

public class ChildRealmTest extends TestCase
{
	private ClassWorld world;
	private ClassRealm parentRealm;
	private ClassLoader parentCL;
	private ClassRealm childRealm;
	private ClassLoader childCL;

	/* this one runs clean - no wonder :) */
	public void testParentRealmAlone() throws Exception
	{
		Class classA = parentRealm.loadClass( "a.A" );

		assertEquals( parentCL, classA.getClassLoader() );
	}

//	public void testChildRealmAlone() throws Exception
//	{
//		Class classA = childRealm.loadClass( "a.A" );
//        
//		// fails, a.A is loaded by parentCL
//        // Since we are using the child realm, it should be in the child CL only.
//		assertEquals( childCL, classA.getClassLoader() ); 
//	}

//	public void testIfaceFromParent() throws Exception
//	{
//		Class classI = parentRealm.loadClass( "base.I" );
//		Class classA = childRealm.loadClass( "swap.A" );
//		assertTrue( classI.isAssignableFrom( classA ) );
//		assertEquals( parentCL, classI.getClassLoader() );
//		assertEquals( childCL, classA.getClassLoader() ); // fails, classA is loaded by parentCL
//	}
//
//	public void testIfaceFromParentWithImport() throws Exception
//	{
//		childRealm.importFrom( "parent", "a." );
//		Class classI = parentRealm.loadClass( "base.I" );
//		Class classA = childRealm.loadClass( "swap.A" );
//		assertTrue( classI.isAssignableFrom( classA ) );
//		assertEquals( parentCL, classI.getClassLoader() );
//		assertEquals( childCL, classA.getClassLoader() ); // fails, classA is loaded by parentCL
//	}
//
//	/* and at last this runs clean again, everything is loaded as expected but is pretty darn ugly as a workaround */
//	public void testIfaceFromParentWithImportANDSameConstituent() throws Exception
//	{
//		childRealm.addConstituent( new URL( "file", null, "./target/test-data/i.jar" ));
//		childRealm.importFrom( "parent", "base" );
//		Class classI = parentRealm.loadClass( "base.I" );
//		Class classA = childRealm.loadClass( "swap.A" );
//		assertTrue( classI.isAssignableFrom( classA ) );
//		assertEquals( parentCL, classI.getClassLoader() );
//		assertEquals( childCL, classA.getClassLoader() );
//	}

	protected void setUp() throws Exception
	{
		world = new ClassWorld();
		parentRealm = world.newRealm( "parent" );
		parentCL = parentRealm.getClassLoader();
		parentRealm.addConstituent( getJarUrl( "a.jar" ) );
        
		childRealm = parentRealm.createChildRealm( "child" );
		childCL = childRealm.getClassLoader();

		//		report(); // commented for silent run
	}

    protected URL getJarUrl(String jarName)
        throws MalformedURLException
    {
        return TestUtil.getTestResourceUrl(jarName);
    }

	private static int run = 0;
	
	/* print out instances in each run to be able to identify classloaders upon the exceptions */
	private void report()
	{
		System.out.println( ++run + ". run - " + this.getName() );
		System.out.println( "parentCL: " + parentCL );
		System.out.println( "childCL: " + childCL );
	}
}
