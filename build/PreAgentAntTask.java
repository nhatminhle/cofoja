/*
 * Copyright 2010 Nhat Minh Lê
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

import com.google.java.contract.core.agent.PreMain;
import com.google.java.contract.core.util.JavaUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;

import java.io.File;
import java.util.ArrayList;
import javax.tools.JavaFileObject.Kind;

public class PreAgentAntTask extends MatchingTask {
  protected File srcdir;
  protected File destdir;

  public void setSrcdir(File srcdir) {
    this.srcdir = srcdir;
  }

  public void setDestdir(File destdir) {
    this.destdir = destdir;
  }

  public void execute() throws BuildException {
    if (srcdir == null) {
      throw new BuildException("missing required attribute \"srcdir\"");
    }
    if (destdir == null) {
      throw new BuildException("missing required attribute \"destdir\"");
    }

    DirectoryScanner ds = getDirectoryScanner(srcdir);
    try {
      String[] srcs = ds.getIncludedFiles();
      ArrayList<String> absSrcs = new ArrayList<String>();

      for (String src : srcs) {
        if (!src.toString().endsWith(Kind.CLASS.extension)) {
          continue;
        }
        File srcFile = new File(srcdir + "/" + src);
        File destFile = new File(destdir + "/" + src);
        if (srcFile.lastModified() > destFile.lastModified()) {
          absSrcs.add(srcFile.toString());
        }
      }

      int n = absSrcs.size();
      if (n > 0) {
        System.out.println("Instrumenting " + n
                           + " class file" + (n == 1 ? "" : "s")
                           + " to " + destdir);
        ClassLoader loader =
            JavaUtils.getLoaderForPath(srcdir.toString(),
                                       getClass().getClassLoader());
        PreMain.instrument(absSrcs.toArray(new String[0]),
                           destdir.toString(),
                           loader);
      }
    } catch (Exception e) {
      throw new BuildException(e);
    }
  }
}
