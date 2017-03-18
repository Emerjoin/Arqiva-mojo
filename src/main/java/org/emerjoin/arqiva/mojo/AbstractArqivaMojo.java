package org.emerjoin.arqiva.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.emerjoin.arqiva.core.ArqivaProject;
import org.emerjoin.arqiva.core.ArqivaProjectContext;
import org.emerjoin.arqiva.core.Project;
import org.emerjoin.arqiva.core.context.ProjectContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;

/**
 * @author Mário Júnior
 */
public abstract class AbstractArqivaMojo extends AbstractMojo {

    @Parameter(defaultValue = "arqiva.properties")
    private String contextValuesPropertiesFile;

    @Parameter(defaultValue = "docs-project")
    private String docsDirectory;

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}")
    private String outputDirectory;

    private Project arqivaProject = null;

    protected String getContextValuesFile(){

        return contextValuesPropertiesFile;

    }

    protected String getDocsDirectory(){

        return docsDirectory;

    }

    protected MavenProject getProject(){

        return project;

    }

    protected String projectDirectory(){

        return new File("").getAbsolutePath();

    }

    protected Properties readContextProperties() throws MojoExecutionException {

        String propertiesFilePath = projectDirectory()+"/"+contextValuesPropertiesFile;
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

    protected void watchProjectDirectory(){

        getLog().info("Starting directory watcher..");
        final Thread watchThread = new Thread(new Runnable() {

            @Override
            public void run() {

                final Path path = Paths.get(new File(projectDirectory()).toURI());


                try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
                    final WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                    while (true) {

                        final WatchKey wk = watchService.take();
                        for (WatchEvent<?> event : wk.pollEvents()) {
                            //we only register "ENTRY_MODIFY" so the context is always a Path.
                            final Path changed = (Path) event.context();
                            handleFileChanged(changed);
                        }
                        // reset the key
                        boolean valid = wk.reset();
                        if (!valid) {
                            getLog().info("Key has been unregisterede");
                        }
                    }
                }catch (Exception ex){

                    getLog().warn(projectDirectory()+" watching failed",ex);

                }

            }
        });

        watchThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(){

            public void run(){

                watchThread.interrupt();

            }

        });


    }

    protected void handleFileChanged(Path fileChanged){

        if(fileChanged.endsWith(contextValuesPropertiesFile)&&arqivaProject!=null){


            try {

                applyContextValues(readContextProperties(), arqivaProject.getContext());

            }catch (MojoExecutionException ex){

                getLog().error(String.format("Error handling file change for %s",fileChanged.toString(),ex));

            }

        }

    }


    protected String getOutputDirectory(){

        return outputDirectory;

    }

    protected Project createProject() throws MojoExecutionException{

        if(arqivaProject!=null)
            return arqivaProject;

        getLog().info("Creating Arqiva project context and project...");
        String webappDirLocation = projectDirectory()+"/"+getDocsDirectory();

        Properties contextProperties = readContextProperties();

        ArqivaProjectContext projectContext = new ArqivaProjectContext(webappDirLocation,outputDirectory);
        applyContextValues(contextProperties,projectContext);
        arqivaProject = new ArqivaProject(projectContext);
        return arqivaProject;


    }

    private void applyContextValues(Properties contextProperties, ProjectContext projectContext){

        for(String key : contextProperties.stringPropertyNames())
            projectContext.getValues().put(key,contextProperties.getProperty(key));


    }


}
