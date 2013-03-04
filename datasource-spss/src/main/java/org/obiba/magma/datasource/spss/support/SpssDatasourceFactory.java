/*
 * Copyright (c) 2012 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.magma.datasource.spss.support;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.obiba.magma.AbstractDatasourceFactory;
import org.obiba.magma.Datasource;
import org.obiba.magma.datasource.spss.SpssDatasource;

public class SpssDatasourceFactory extends AbstractDatasourceFactory {

  public static final String DEFAULT_DATASOURCE_NAME = "spss";

  public static final String DEFAULT_CHARACTER_SET = "ISO-8859-1";

  //
  // Data members
  //

  private List<File> files = new ArrayList<File>();

  private String characterSet;

  public void setFile(String path) {
    setFile(new File(path));
  }

  public void setFile(File file) {
    files.add(file);
  }

  public void addFile(String path) {
    files.add(new File(path));
  }

  public void addFile(File file) {
    files.add(file);
  }

  public void setCharacterSet(String characterSet) {
    this.characterSet = characterSet;
  }

  public Datasource create() {
    return internalCreate();
  }

  @Nonnull
  @Override
  protected Datasource internalCreate() {
    if (characterSet == null || characterSet.isEmpty()) {
      characterSet =  DEFAULT_CHARACTER_SET;
    }

    return new SpssDatasource(getName(), files, characterSet);
  }
}