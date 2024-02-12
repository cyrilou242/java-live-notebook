/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jln.render;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

// from https://jenkov.com/tutorials/java-reflection/dynamic-class-loading-reloading.html
public class ReloadingClassLoader extends ClassLoader {

  // FIXME CYRIL SHOULD BE STATIC AND CONTAINS A LIST OF ALL CLASSES TO RELOAD 
  private final String qualifiedName;

  public ReloadingClassLoader(final ClassLoader parent, final String qualifiedName) {
    super(parent);
    this.qualifiedName = qualifiedName;
  }

  public Class loadClass(String name) throws ClassNotFoundException {
    if (!qualifiedName.equals(name)) {
      // use the parent for other classes
      return super.loadClass(name);
    }
    try {
      String url =
              "file:C:/data/projects/tutorials/web/WEB-INF/" + "classes/reflection/MyObject.class";
      URL myUrl = new URL(url);
      URLConnection connection = myUrl.openConnection();
      InputStream input = connection.getInputStream();
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      int data = input.read();

      while (data != -1) {
        buffer.write(data);
        data = input.read();
      }

      input.close();

      byte[] classData = buffer.toByteArray();

      return defineClass("reflection.MyObject", classData, 0, classData.length);

    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }
}
