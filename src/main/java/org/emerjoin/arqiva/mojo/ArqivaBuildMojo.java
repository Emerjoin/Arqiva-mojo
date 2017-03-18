package org.emerjoin.arqiva.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.emerjoin.arqiva.core.Arqiva;
import org.emerjoin.arqiva.core.Project;

/**
 * @author Mário Júnior
 */
@Mojo(name ="build")
public class ArqivaBuildMojo extends AbstractArqivaMojo {

    @Parameter(defaultValue = "")
    private String projectBuilderName;

    public void execute() throws MojoExecutionException
    {

        if(projectBuilderName.equals(""))
            getLog().info("Building Arqiva project with default builder");
        else
            getLog().info("Building Arqiva project with "+projectBuilderName+" builder");

        Project arqivaProject = createProject();
        Arqiva arqiva = new Arqiva(arqivaProject);
        arqiva.buildProject(projectBuilderName);

        getLog().info("Arqiva project build succeeded!");

    }



}
