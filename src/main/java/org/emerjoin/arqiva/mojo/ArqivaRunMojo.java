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

    @Parameter(defaultValue = "false")
    private boolean buildTopicsTreeForEachServletRequest;





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

        configureClassLoaders();

        Wrapper arqivaRunServlet = tomcat.addServlet(context,"arqivaRun","org.emerjoin.arqiva.web.ArqivaRunServlet");
        arqivaRunServlet.addInitParameter("debug", "1");
        arqivaRunServlet.setLoadOnStartup(1);
        context.addServletMapping("*.html","arqivaRun");
        WebappLoader webappLoader = new WebappLoader(context.getParentClassLoader());
        webappLoader.setLoaderClass(ArqivaClassLoader.class.getName());
        context.setLoader(webappLoader);

    }




}
