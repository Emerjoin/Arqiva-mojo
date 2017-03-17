package org.emerjoin.arqiva.mojo;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.emerjoin.arqiva.core.ArqivaProject;
import org.emerjoin.arqiva.core.ArqivaProjectContext;
import org.emerjoin.arqiva.core.Project;

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

        String projectDirectory = getProject().getBuild().getDirectory();
        String webappDirLocation = projectDirectory+getDocsDirectory();

        Project arqivaProject = createProject();
        ArqivaRunServlet.INVALIDATE_TOPICS_TREE = buildTopicsTreeForEachServletRequest;
        ArqivaRunServlet.ARQIVA_PROJECT = arqivaProject;

        Tomcat tomcat = new Tomcat();

        if(serverPort <0)
            serverPort = 9610;

        tomcat.setPort(serverPort);
        StandardContext ctx =  null;

        try {

            ctx = (StandardContext) tomcat.addWebapp("/", new File(webappDirLocation).getAbsolutePath());
            getLog().info("configuring tomcat server with basedir: " + webappDirLocation);

        }catch (ServletException ex){

            throw new MojoExecutionException("Failed to configure tomcat server",ex);

        }

        WebResourceRoot resources = new StandardRoot(ctx);
        Set<Artifact> artifactSet = getProject().getDependencyArtifacts();
        for(Artifact dependency : artifactSet){

            String scope = dependency.getScope();
            if(!(scope.equals("compile")||scope.equals("provided")||scope.equals("runtime")))
                continue;

            File artifactFile = null;

            if(dependency.isResolved())
                artifactFile = dependency.getFile();
            else
                resolveArtifact(dependency);

            if(artifactFile==null)
                continue;

            resources.addJarResources(new JarResourceSet(resources,"/WEB-INF/lib",artifactFile.getParentFile().getAbsolutePath(),"/"));

        }

        // Declare an alternative location for your "WEB-INF/classes" dir
        // Servlet 3.0 annotation will work
        /*
        File additionWebInfClasses = new File("target/classes");
        resources.addJarResources(new JarResourceSet());
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                additionWebInfClasses.getAbsolutePath(), "/"));
       */


        try {

            ctx.setResources(resources);

            getLog().info("Starting tomcat...");
            tomcat.start();
            tomcat.getServer().await();

        }catch (LifecycleException ex){

            throw new MojoExecutionException("Failed to start tomcat server",ex);

        }

    }


}
