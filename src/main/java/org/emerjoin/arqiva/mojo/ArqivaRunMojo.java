package org.emerjoin.arqiva.mojo;

import org.apache.catalina.*;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.loader.WebappLoader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.apache.catalina.startup.Tomcat;

import java.io.File;
import java.nio.file.Files;

import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.emerjoin.arqiva.core.Project;
import org.emerjoin.arqiva.web.Middleware;

/**
 * @author Mário Júnior
 */
@Mojo(name = "run",defaultPhase = LifecyclePhase.INSTALL)
public class ArqivaRunMojo extends AbstractArqivaMojo {

    @Parameter(defaultValue = "9610")
    private int serverPort;

    @Parameter(defaultValue = "true")
    private boolean buildTopicsTreeForEachServletReq;





    public void execute() throws MojoExecutionException
    {

        if(!getProject().getPackaging().toLowerCase().equals("war")){
            getLog().error("Cant run a project with packing different from WAR. Exiting...");
            return;
        }

        String webDirectory = projectDirectory()+File.separator+getDocsDirectory();
        Project arqivaProject = createProject();
        watchProjectDirectory();

        Middleware.INVALIDATE_TOPICS_TREE = buildTopicsTreeForEachServletReq;
        Middleware.ARQIVA_PROJECT = arqivaProject;
        Middleware.START_POINT = getStartPoint();
        Middleware.TOPICS_DIRECTORY = getTopicsDirectory();

        Tomcat tomcat = new Tomcat();
        if(serverPort <0)
            serverPort = 9610;

        tomcat.setPort(serverPort);


        getLog().info("Running project from : "+webDirectory);
        setupContainerContext(tomcat,webDirectory);

        try {

            tomcat.setBaseDir(Files.createTempDirectory("arqiva").toAbsolutePath().toString());
            tomcat.start();
            String url = "http://localhost:"+serverPort+"/arqiva/index.html";
            System.out.println("Browse your project at "+url);
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


        FilterDef filterDef = new FilterDef();
        filterDef.setFilterClass("org.apache.catalina.filters.ExpiresFilter");
        filterDef.setFilterName("Expires");
        filterDef.setDisplayName("Expires-filter");
        filterDef.addInitParameter("ExpiresDefault","access plus 1 second");

        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName("Expires");
        filterMap.addURLPattern("/*");

        context.addFilterDef(filterDef);
        context.addFilterMap(filterMap);

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
