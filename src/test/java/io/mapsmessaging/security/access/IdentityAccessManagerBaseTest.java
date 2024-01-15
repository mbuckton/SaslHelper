/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.security.access;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.github.javafaker.Faker;
import io.mapsmessaging.security.access.mapping.GroupIdMap;
import io.mapsmessaging.security.access.mapping.UserIdMap;
import io.mapsmessaging.security.access.mapping.store.MapFileStore;
import io.mapsmessaging.security.identity.IdentityLookupFactory;
import io.mapsmessaging.security.identity.PasswordGenerator;
import io.mapsmessaging.security.jaas.IdentityLoginModule;
import io.mapsmessaging.security.sasl.ClientCallbackHandler;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class IdentityAccessManagerBaseTest extends BaseSecurityTest {

  private static List<Arguments> configurations() {
    List<Arguments> arguments = new ArrayList<>();
    Map<String, Object> apacheConfig = new LinkedHashMap<>();
    apacheConfig.put("passwordFile", "htpasswordFile");
    apacheConfig.put("groupFile", "htgroupFile");
    apacheConfig.put("passwordHander", "UnixSha512PasswordHasher");

    Map<String, Object> cipherConfig = new LinkedHashMap<>();
    Map<String, Object> baseConfig = new LinkedHashMap<>();
    baseConfig.put("certificateStore", cipherConfig);
    baseConfig.put("passwordFile", "htpasswordFile-enc");
    baseConfig.put("groupFile", "htgroupFile-enc");
    baseConfig.put("passwordHander", "EncryptedPasswordCipher");

    cipherConfig.put("certificate.alias", "alias");
    cipherConfig.put("privateKey.password", "8 5tr0ng pr1v8t3 k3y p855w0rd!@#$%");
    cipherConfig.put("privateKey.name", "alias");
    cipherConfig.put("keystore.type", "JKS");
    cipherConfig.put("keystore.path", "test.jks");
    cipherConfig.put("keystore.password", "8 5Tr0Ng C3rt!f1c8t3 P855sw0rd!!!!");

    arguments.add(arguments("Encrypted-Auth", baseConfig));
    arguments.add(arguments("Apache-Basic-Auth", apacheConfig));
    return arguments;
  }

  @ParameterizedTest
  @MethodSource("configurations")
  void testUserManagement(String auth, Map<String, ?> config) throws IOException, LoginException {
    File userFile = new File("userMap");
    userFile.delete();
    File groupFile = new File("groupMap");
    groupFile.delete();

    MapFileStore<UserIdMap> users = new MapFileStore<>("userMap");
    MapFileStore<GroupIdMap> groups = new MapFileStore<>("groupMap");

    IdentityAccessManager identityAccessManager =
        new IdentityAccessManager(auth, config, users, groups);
    IdentityLookupFactory.getInstance()
        .registerSiteIdentityLookup(auth, identityAccessManager.getIdentityLookup());

    for (UserIdMap userIdMap : identityAccessManager.getAllUsers()) {
      identityAccessManager.deleteUser(userIdMap.getUsername());
    }
    for (GroupIdMap groupIdMap : identityAccessManager.getAllGroups()) {
      identityAccessManager.deleteGroup(groupIdMap.getGroupName());
    }
    Assertions.assertEquals(0, identityAccessManager.getAllUsers().size());
    Assertions.assertEquals(0, identityAccessManager.getAllGroups().size());

    Faker faker = new Faker();
    Random random = new Random(System.currentTimeMillis());
    List<String> groupNames = new ArrayList<>();
    for (int x = 0; x < 10; x++) {
      Assertions.assertEquals(x, identityAccessManager.getAllGroups().size());
      String group = faker.starTrek().specie();
      while (identityAccessManager.getGroup(group) != null) {
        group = faker.starTrek().specie();
      }
      System.err.println("New Group::" + group);
      identityAccessManager.createGroup(group);
      groupNames.add(group);
    }

    Map<String, String> userPasswordMap = new LinkedHashMap<>();
    for (int x = 0; x < 100; x++) {
      Assertions.assertEquals(x, identityAccessManager.getAllUsers().size());
      String username = faker.starTrek().character();
      int count = 0;
      while (identityAccessManager.getUser(username) != null) {
        if (count > 90) {
          username = faker.witcher().character();
        } else if (count > 75) {
          username = faker.backToTheFuture().character();
        } else if (count > 60) {
          username = faker.zelda().character();
        } else if (count > 45) {
          username = faker.aquaTeenHungerForce().character();
        } else if (count > 30) {
          username = faker.lebowski().character();
        } else if (count > 15) {
          username = faker.lordOfTheRings().character();
        } else {
          username = faker.starTrek().character();
        }
        count++;
      }
      System.err.println("New user::" + username);
      String password = PasswordGenerator.generateSalt(10 + Math.abs(random.nextInt(20)));
      identityAccessManager.createUser(username, password);
      String group = groupNames.get(Math.abs(random.nextInt(groupNames.size())));
      identityAccessManager.addUserToGroup(username, group);
      userPasswordMap.put(username, password);
      Assertions.assertEquals(username, identityAccessManager.getUser(username).getUsername());
      Assertions.assertNotNull(identityAccessManager.getUser(username).getAuthId());
    }

    for (Map.Entry<String, String> user : userPasswordMap.entrySet()) {
      Assertions.assertTrue(validateLogin(auth, user.getKey(), user.getValue()));
      Assertions.assertFalse(validateLogin(auth, user.getKey(), user.getValue() + "_bad_password"));
    }

    for (UserIdMap userIdMap : identityAccessManager.getAllUsers()) {
      identityAccessManager.deleteUser(userIdMap.getUsername());
    }
    List<GroupIdMap> groupList = identityAccessManager.getAllGroups();
    for (GroupIdMap groupIdMap : groupList) {
      identityAccessManager.deleteGroup(groupIdMap.getGroupName());
    }
    Assertions.assertEquals(0, identityAccessManager.getAllUsers().size());
    Assertions.assertEquals(0, identityAccessManager.getAllGroups().size());
  }

  private boolean validateLogin(String auth, String username, String password)
      throws LoginException {
    Map<String, String> initMap = new LinkedHashMap<>();
    initMap.put("siteWide", auth);
    Subject subject = new Subject();
    ClientCallbackHandler clientCallbackHandler = new ClientCallbackHandler(username, password, "");

    IdentityLoginModule identityLoginModule = new IdentityLoginModule();
    identityLoginModule.initialize(subject, clientCallbackHandler, new LinkedHashMap<>(), initMap);
    boolean isValid = false;
    try {
      isValid = identityLoginModule.login();
      identityLoginModule.commit();
    } catch (LoginException e) {
    }

    return isValid;
  }
}
