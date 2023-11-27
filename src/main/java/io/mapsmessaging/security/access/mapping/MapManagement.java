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

package io.mapsmessaging.security.access.mapping;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MapManagement<T extends IdMap> {
  private final String fileName;
  private final MapParser<T> parser;
  private final Map<UUID, T> userIdMapByUuid;
  private final Map<String, T> userIdMapByUser;
  private boolean hasChanged;

  public MapManagement(String filename, MapParser<T> parser) {
    userIdMapByUuid = new ConcurrentHashMap<>();
    userIdMapByUser = new ConcurrentHashMap<>();
    this.fileName = filename;
    this.parser = parser;
    load();
    hasChanged = false;
  }

  public void clearAll() {
    userIdMapByUuid.clear();
    userIdMapByUser.clear();
  }

  public List<T> getAll() {
    return new ArrayList<>(userIdMapByUuid.values());
  }

  public T get(UUID uuid) {
    return userIdMapByUuid.get(uuid);
  }

  public T get(String username) {
    return userIdMapByUser.get(username);
  }

  public boolean delete(String name) {
    T entry = userIdMapByUser.remove(name);
    if (entry != null) {
      userIdMapByUuid.remove(entry.getAuthId());
      save();
      return true;
    }
    return false;
  }

  public boolean add(T entry) {
    if (!userIdMapByUser.containsKey(entry.getKey())) {
      userIdMapByUser.put(entry.getKey(), entry);
      userIdMapByUuid.put(entry.getAuthId(), entry);
      hasChanged = true;
      return true;
    }
    return false;
  }

  public void load() {
    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
      String line;
      while ((line = br.readLine()) != null) {
        T entry = parser.parse(line);
        if (entry != null) {
          userIdMapByUuid.put(entry.getAuthId(), entry);
          userIdMapByUser.put(entry.getKey(), entry);
        }
      }
    } catch (IOException e) {
      // e.printStackTrace();
    }
  }

  public void save() {
    if (hasChanged) {
      try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
        List<T> values = new ArrayList<>(userIdMapByUuid.values());
        List<String> linesToWrite = parser.writeToList(values);
        for (String line : linesToWrite) {
          bw.write(line);
          bw.newLine(); // Add a newline character after each line
        }
        hasChanged = false;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public boolean remove(String username) {
    T entry = userIdMapByUser.remove(username);
    if (!userIdMapByUser.containsKey(username)) {
      userIdMapByUser.put(entry.getKey(), entry);
      userIdMapByUuid.put(entry.getAuthId(), entry);
      hasChanged = true;
      return true;
    }
    return false;
  }

  public int size() {
    return userIdMapByUser.size();
  }

}
