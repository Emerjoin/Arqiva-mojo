package org.emerjoin.arqiva.mojo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.emerjoin.arqiva.core.Arqiva;
import org.emerjoin.arqiva.core.ArqivaProject;
import org.emerjoin.arqiva.core.ArqivaProjectContext;
import org.emerjoin.arqiva.core.Project;
import org.emerjoin.arqiva.core.context.ProjectContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author Mário Júnior
 */
public abstract class AbstractArqivaMojo extends AbstractMojo {

    @Parameter(defaultValue = "arqiva.properties")
    private String contextValuesPropsFile;

    @Parameter(defaultValue = "docs-project")
    private String docsDirectory;

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}")
    private String outputDirectory;

    @Component
    private RepositorySystem repoSystem;

    @Parameter( defaultValue = "${repositorySystemSession}", readonly = true, required = true )
    private RepositorySystemSession repoSession;

    @Parameter( defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true )
    private List<RemoteRepository> repositories;


    @Parameter( defaultValue = Arqiva.START_POINT_INDEX)
    private String startPoint;

    private Project arqivaProject = null;

    protected String getContextValuesFile(){

        return contextValuesPropsFile;

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

        String propertiesFilePath = projectDirectory()+File.separator+ contextValuesPropsFile;
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

        if(fileChanged.endsWith(contextValuesPropsFile)&&arqivaProject!=null){


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
        String webappDirLocation = projectDirectory()+File.separator+getDocsDirectory();

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

    protected void configureClassLoaders() throws MojoExecutionException{

        ArqivaClassLoader.libs = getArtifactsUrls();
        ArqivaClassLoader.parentClassLoader = (ClassRealm) getClass().getClassLoader();
        for(URL url : ArqivaClassLoader.libs)
            ArqivaClassLoader.parentClassLoader.addURL(url);

    }

    private List<URL> getDependencies(org.eclipse.aether.artifact.Artifact artifact) throws MojoExecutionException{

        List<URL> dependencies = new ArrayList<>();

        try {

            CollectRequest collectRequest = new CollectRequest(new Dependency(artifact, JavaScopes.COMPILE), repositories);
            DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE, JavaScopes.RUNTIME);
            DependencyRequest request = new DependencyRequest(collectRequest, filter);
            DependencyResult result = repoSystem.resolveDependencies(repoSession, request);

            for (ArtifactResult artifactResult : result.getArtifactResults()) {

                File artifactFile = artifactResult.getArtifact().getFile();
                if(artifactFile!=null)
                    dependencies.add(artifactFile.toURI().toURL());

            }

        }catch (Exception ex){

            throw new MojoExecutionException("Failed to resolve dependencies",ex);

        }

        return dependencies;

    }

    private File resolveArtifact(Artifact unresolvedArtifact) throws MojoExecutionException{

        String artifactId = unresolvedArtifact.getArtifactId();
        org.eclipse.aether.artifact.Artifact aetherArtifact = new DefaultArtifact(
                unresolvedArtifact.getGroupId(),
                unresolvedArtifact.getArtifactId(),
                unresolvedArtifact.getClassifier(),
                unresolvedArtifact.getType(),
                unresolvedArtifact.getVersion());

        ArtifactRequest req = new ArtifactRequest().setRepositories( this.repositories ).setArtifact( aetherArtifact );
        ArtifactResult resolutionResult;
        try {
            resolutionResult = this.repoSystem.resolveArtifact( this.repoSession, req );

        } catch( ArtifactResolutionException e ) {
            throw new MojoExecutionException("Artifact " + artifactId + "could not be resolved.", e );
        }


        // The file should exists, but we never know.
        File file = resolutionResult.getArtifact().getFile();
        if( file == null || ! file.exists()) {
            getLog().warn( "Artifact " + artifactId + " has no attached file. Its content will not be copied in the target model directory." );
            return null;
        }

        return file;

    }



    private URL[] getArtifactsUrls() throws MojoExecutionException{

        List<URL> urlList = new ArrayList<>();
        Set<Artifact> dependencies =  getProject().getDependencyArtifacts();
        for(Artifact dependency : dependencies){

            String scope = dependency.getScope().toLowerCase();
            if(!(scope.equals("compile")||scope.equals("runtime")))
                continue;

            File dependencyFile = null;
            if(dependency.isResolved())
                dependencyFile = dependency.getFile();
            else dependencyFile = resolveArtifact(dependency);

            try {

                if(dependencyFile==null)
                    continue;

                urlList.add(dependencyFile.toURI().toURL());
                org.eclipse.aether.artifact.Artifact artifact = new DefaultArtifact(String.format("%s:%s:%s",
                        dependency.getGroupId(),dependency.getArtifactId(),dependency.getVersion()));

                List<URL> urls = getDependencies(artifact);
                urlList.addAll(urls);

            }catch (MalformedURLException ex){
                throw new MojoExecutionException("Failed to generate artifact URL",ex);
            }
        }

        URL[] dependenciesArray = new URL[urlList.size()];
        urlList.toArray(dependenciesArray);
        return dependenciesArray;

    }


    protected String getStartPoint(){

        return startPoint;

    }


}
