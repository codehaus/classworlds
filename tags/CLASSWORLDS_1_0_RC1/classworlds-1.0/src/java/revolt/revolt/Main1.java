package revolt;

import java.util.Properties;

public class Main1
{
    public static void main(String[] args)
        throws Exception
    {
        try
        {
            System.err.println( Main1.class.getClassLoader().getResource( "revolt/revolt.properties" ) );
            System.err.println( Main1.class.getResource("revolt.properties"));

            {
                Properties props = new Properties();
                props.load( Main1.class.getClassLoader().getResourceAsStream( "revolt/revolt.properties" ) );
                System.err.println( "bob -> " + props.getProperty( "bob" ) );
                System.err.println( "jason -> " + props.getProperty( "jason" ) );
            }

            {
                Properties props = new Properties();
                props.load( Main1.class.getResourceAsStream( "revolt.properties" ) );
                System.err.println( "bob -> " + props.getProperty( "bob" ) );
                System.err.println( "jason -> " + props.getProperty( "jason" ) );
            }

            
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        } 
    }
}
