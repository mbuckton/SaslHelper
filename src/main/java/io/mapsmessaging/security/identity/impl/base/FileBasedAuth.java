/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.security.identity.impl.base;

import io.mapsmessaging.security.identity.IdentityLookup;
import io.mapsmessaging.security.identity.NoSuchUserFoundException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class FileBasedAuth implements IdentityLookup {

  private final String filePath;
  private final Map<String, IdentityEntry> usernamePasswordMap;
  private long lastModified;

  public FileBasedAuth(){
    filePath = "";
    usernamePasswordMap = new LinkedHashMap<>();
  }

  public FileBasedAuth(String filepath) {
    filePath = filepath;
    lastModified = 0;
    usernamePasswordMap = new LinkedHashMap<>();
  }

  @Override
  public char[] getPasswordHash(String username) throws NoSuchUserFoundException {
    load();
    IdentityEntry identityEntry = usernamePasswordMap.get(username);
    if (identityEntry == null) {
      throw new NoSuchUserFoundException("User: " + username + " not found");
    }
    return identityEntry.getPassword().toCharArray();
  }

  protected abstract IdentityEntry load(String line);

  private void load() {
    File file = new File(filePath);
    if (file.exists() && lastModified != file.lastModified()) {
      lastModified = file.lastModified();
      usernamePasswordMap.clear();
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        String line = reader.readLine();
        while (line != null) {
          IdentityEntry identityEntry = load(line);
          usernamePasswordMap.put(identityEntry.getUsername(), identityEntry);
          line = reader.readLine();
        }
      } catch (IOException e) {
        // To Do : Need to add log meesage
      }
    }
  }
}
