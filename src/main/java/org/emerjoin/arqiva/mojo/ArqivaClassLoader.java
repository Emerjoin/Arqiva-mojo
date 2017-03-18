package org.emerjoin.arqiva.mojo;

import org.apache.catalina.loader.WebappClassLoaderBase;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mário Júnior
 */
public class ArqivaClassLoader extends WebappClassLoaderBase {

    public static URL[] libs = null;

    private CustomURLClassLoader customURLClassLoader = null;
    protected static ClassRealm parentClassLoader = null;

    public ArqivaClassLoader(ClassLoader parent) {
        super(parent);
        this.setDelegate(true);
        customURLClassLoader = new CustomURLClassLoader(libs,this);
    }

    @Override
    public ClassLoader copyWithoutTransformers() {
        return null;
    }

    public Class<?> findClass(String name) throws ClassNotFoundException {

        Class<?> clazz = null;

        try {

            //Class Found via Maven plugin classloader
            clazz = parentClassLoader.loadClass(name);

        }catch (ClassNotFoundException ex){

            clazz = customURLClassLoader.findClass(name);
            //Class found via Maven Project URLClassLoader

        }

        return  clazz;
    }

    public URL[] getURLs(){

        List<URL> urlList = new ArrayList<URL>();
        for(URL url : parentClassLoader.getURLs())
            urlList.add(url);

        for(URL url :libs)
            urlList.add(url);

        URL[] urlArray = new URL[urlList.size()];
        urlList.toArray(urlArray);

        return urlArray;


    }
}
