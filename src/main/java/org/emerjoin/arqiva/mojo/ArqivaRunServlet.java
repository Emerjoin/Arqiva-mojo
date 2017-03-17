package org.emerjoin.arqiva.mojo;

import org.emerjoin.arqiva.core.Project;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Mário Júnior
 */
public class ArqivaRunServlet extends HttpServlet {

    private boolean invalidateTopicsTree = false;
    private Project arqivaProject = null;

    public ArqivaRunServlet(Project arqivaProject, boolean invalidateTopicsTree){

        this.arqivaProject = arqivaProject;
        this.invalidateTopicsTree = invalidateTopicsTree;

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if(invalidateTopicsTree)
            arqivaProject.invalidateTopicsTree();


        //TODO: Render page here



    }


}
