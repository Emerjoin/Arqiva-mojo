package org.emerjoin.arqiva.mojo;

import com.sun.org.apache.regexp.internal.RE;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author Mário Júnior
 */
public abstract class AbstractArqivaMojo extends AbstractMojo {

    @Parameter(defaultValue = "arqiva.xml")
    private String contextConfigXMLFile;

    @Parameter(defaultValue = "/docs")
    private String docsDirectory;

    protected String getContextConfigXMLFile(){

        return contextConfigXMLFile;

    }

    protected String getDocsDirectory(){

        return docsDirectory;

    }


}
