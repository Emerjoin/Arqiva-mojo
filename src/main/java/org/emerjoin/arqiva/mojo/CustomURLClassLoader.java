package org.emerjoin.arqiva.mojo;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Mário Júnior
 */
public class CustomURLClassLoader extends URLClassLoader {


    public CustomURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public Class<?> findClass(String name) throws ClassNotFoundException {

        return super.findClass(name);

    }
}
