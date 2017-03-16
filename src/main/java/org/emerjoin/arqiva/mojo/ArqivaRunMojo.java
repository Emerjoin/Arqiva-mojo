package org.emerjoin.arqiva.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author Mário Júnior
 */
@Mojo(name = "run")
public class ArqivaRunMojo extends AbstractArqivaMojo {

    @Parameter(defaultValue = "9610")
    private int tomcatPort;

    public void execute() throws MojoExecutionException
    {
        getLog().info( "Hello, world." );

    }


}
