package org.emerjoin.arqiva.mojo;

import org.apache.catalina.*;
import org.apache.catalina.loader.WebappLoader;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.apache.catalina.startup.Tomcat;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import org.emerjoin.arqiva.core.Project;
import org.emerjoin.arqiva.web.Middleware;

/**
 * @author Mário Júnior
 */
@Mojo(name = "run")
public class ArqivaRunMojo extends AbstractArqivaMojo {

    @Parameter(defaultValue = "9610")
    private int serverPort;

    @Component
    private RepositorySystem repoSystem;

    @Parameter( defaultValue = "${repositorySystemSession}", readonly = true, required = true )
    private RepositorySystemSession repoSession;

    @Parameter( defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true )
    private List<RemoteRepository> repositories;

    @Parameter(defaultValue = "false")
    private boolean buildTopicsTreeForEachServletRequest;

    private List<URL> getDependencies(org.eclipse.aether.artifact.Artifact artifact) throws MojoExecutionException{

        List<URL> dependencies = new ArrayList<>();

        try {

            //TODO: Read POM and fetch more remote repositories
            RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build();
            //CollectRequest collectRequest = new CollectRequest(new Dependency(artifact, JavaScopes.COMPILE), Arrays.asList(central));
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




    public void execute() throws MojoExecutionException
    {

        if(!getProject().getPackaging().toLowerCase().equals("war")){
            getLog().info("Cant run a project with packing different from WAR. Exiting...");
            return;
        }

        String webDirectory = projectDirectory()+"/"+getDocsDirectory();
        Project arqivaProject = createProject();
        watchProjectDirectory();

        Middleware.INVALIDATE_TOPICS_TREE = buildTopicsTreeForEachServletRequest;
        Middleware.ARQIVA_PROJECT = arqivaProject;

        Tomcat tomcat = new Tomcat();
        if(serverPort <0)
            serverPort = 9610;

        tomcat.setPort(serverPort);

        getLog().info("Running project from : "+webDirectory);
        setupContainerContext(tomcat,webDirectory);

        try {

            tomcat.start();
            tomcat.getServer().await();

        }catch (Throwable ex){

            throw new MojoExecutionException("Failed to start tomcat server",ex);

        }

    }

    private void setupContainerContext(Tomcat tomcat, String webDirectory) throws MojoExecutionException{

        Context context = tomcat.addContext("/arqiva",webDirectory);

        Wrapper defaultServlet = tomcat.addServlet(context,"default","org.apache.catalina.servlets.DefaultServlet");
        defaultServlet.setName("default");
        defaultServlet.setServletClass("org.apache.catalina.servlets.DefaultServlet");
        defaultServlet.addInitParameter("debug", "0");
        defaultServlet.addInitParameter("listings", "false");
        defaultServlet.setLoadOnStartup(1);
        context.addServletMapping("/","default");

        ArqivaClassLoader.libs = getArtifactsUrls();
        ArqivaClassLoader.parentClassLoader = (ClassRealm) getClass().getClassLoader();
        for(URL url : ArqivaClassLoader.libs)
            ArqivaClassLoader.parentClassLoader.addURL(url);

        //tomcat.setBaseDir(webappDirLocation);
        Wrapper arqivaRunServlet = tomcat.addServlet(context,"arqivaRun","org.emerjoin.arqiva.web.ArqivaRunServlet");
        arqivaRunServlet.addInitParameter("debug", "1");
        arqivaRunServlet.setLoadOnStartup(1);
        context.addServletMapping("*.html","arqivaRun");
        WebappLoader webappLoader = new WebappLoader(context.getParentClassLoader());
        webappLoader.setLoaderClass(ArqivaClassLoader.class.getName());
        context.setLoader(webappLoader);

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


}
