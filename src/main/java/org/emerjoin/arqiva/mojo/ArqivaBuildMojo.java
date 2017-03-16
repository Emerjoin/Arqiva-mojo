package org.emerjoin.arqiva.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author Mário Júnior
 */
@Mojo(name ="build")
public class ArqivaBuildMojo extends AbstractArqivaMojo {

    @Parameter(defaultValue = "${project.build.directory}")
    private String outputDirectory;

    @Parameter(defaultValue = "")
    private String projectBuilderName;

    public void execute() throws MojoExecutionException
    {
        getLog().info( "Hello, world." );
    }


    protected String getOutputDirectory(){

        return outputDirectory;

    }


}
