package org.emerjoin.arqiva.mojo;

import com.sun.org.apache.regexp.internal.RE;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.emerjoin.arqiva.core.ArqivaProject;
import org.emerjoin.arqiva.core.ArqivaProjectContext;
import org.emerjoin.arqiva.core.Project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Mário Júnior
 */
public abstract class AbstractArqivaMojo extends AbstractMojo {

    @Parameter(defaultValue = "arqiva.properties")
    private String contextValuesPropertiesFile;

    @Parameter(defaultValue = "/docs")
    private String docsDirectory;

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}")
    private String outputDirectory;

    protected String getContextValuesFile(){

        return contextValuesPropertiesFile;

    }

    protected String getDocsDirectory(){

        return docsDirectory;

    }

    protected MavenProject getProject(){

        return project;

    }

    protected Properties readContextProperties() throws MojoExecutionException {

        String propertiesFilePath = project.getBuild().getDirectory()+contextValuesPropertiesFile;
        Properties properties = new Properties();

        File file = new File(propertiesFilePath);
        if(!file.exists()){
            getLog().info(String.format("Context properties file not found : %s",propertiesFilePath));
            return properties;
        }

        try {

            properties.load(new FileInputStream(file));

        }catch (IOException ex){

            throw new MojoExecutionException(String.format("Failed to read context properties file : %s",propertiesFilePath));

        }

        return properties;

    }


    protected String getOutputDirectory(){

        return outputDirectory;

    }

    protected Project createProject() throws MojoExecutionException{

        getLog().info("Creating Arqiva project context and project...");
        String projectDirectory = getProject().getBuild().getDirectory();
        String webappDirLocation = projectDirectory+getDocsDirectory();

        Properties contextProperties = readContextProperties();

        ArqivaProjectContext projectContext = new ArqivaProjectContext(webappDirLocation,outputDirectory);
        for(String key : contextProperties.stringPropertyNames())
            projectContext.getValues().put(key,contextProperties.getProperty(key));

        return new ArqivaProject(projectContext);


    }



}
