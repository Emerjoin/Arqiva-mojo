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
@WebServlet(name = "ArqivaRun",urlPatterns = "/*")
public class ArqivaRunServlet extends HttpServlet {

    protected static boolean INVALIDATE_TOPICS_TREE = false;
    protected static Project ARQIVA_PROJECT = null;


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if(INVALIDATE_TOPICS_TREE)
            ARQIVA_PROJECT.invalidateTopicsTree();


        //TODO: Render page here



    }


}
