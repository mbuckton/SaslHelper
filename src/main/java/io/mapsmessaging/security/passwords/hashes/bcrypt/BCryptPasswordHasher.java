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

package io.mapsmessaging.security.passwords.hashes.bcrypt;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.BCrypt.Version;
import at.favre.lib.crypto.bcrypt.Radix64Encoder;
import io.mapsmessaging.security.passwords.PasswordHasher;
import io.mapsmessaging.security.util.ArrayHelper;

public abstract class BCryptPasswordHasher implements PasswordHasher {

  private static final int SALT_SIZE = 22;
  private static final int DEFAULT_COST = 12;

  private final Version version;
  private final char[] password;
  private final byte[] salt;
  private final int cost;

  protected BCryptPasswordHasher() {
    password = new char[0];
    salt = new byte[0];
    cost = DEFAULT_COST;
    version = null;
  }

  protected BCryptPasswordHasher(Version version) {
    password = new char[0];
    salt = new byte[0];
    cost = DEFAULT_COST;
    this.version = version;
  }

  protected BCryptPasswordHasher(char[] password, Version version) {
    this.version = version;
    if (password == null || password.length == 0) {
      salt = new byte[0];
      this.password = new char[0];
      cost = DEFAULT_COST;
    } else {
      char[] t = ArrayHelper.substring(password, getKey().length());

      int dollar = ArrayHelper.indexOf(t, '$');
      cost = Integer.parseInt( new String(ArrayHelper.substring(t, 0, dollar)));
      t = ArrayHelper.substring(t, dollar + 1);
      char[] s = ArrayHelper.substring(t,0, SALT_SIZE);
      char[] p = ArrayHelper.substring(t, SALT_SIZE);
      Radix64Encoder encoder = new Radix64Encoder.Default();
      salt = encoder.decode(ArrayHelper.charArrayToByteArray(s));
      this.password = ArrayHelper.byteArrayToCharArray(encoder.decode(ArrayHelper.charArrayToByteArray(p)));
    }
  }

  @Override
  public int getCost() {
    return cost;
  }

  @Override
  public boolean hasSalt() {
    return true;
  }

  @Override
  public char[] transformPassword(char[] password, byte[] salt, int cost) {
    return ArrayHelper.byteArrayToCharArray(BCrypt.with(version).hash(cost, salt, ArrayHelper.charArrayToByteArray(password)));
  }

  @Override
  public byte[] getSalt() {
    return salt;
  }

  @Override
  public char[] getPassword() {
    return password;
  }

  @Override
  public char[] getFullPasswordHash() {
    Radix64Encoder encoder = new Radix64Encoder.Default();
    String t = new String(encoder.encode(salt)) + new String(encoder.encode(ArrayHelper.charArrayToByteArray(password)));
    return (getKey() + cost + "$" + t).toCharArray();
  }

  @Override
  public String getName() {
    return "BCrypt";
  }
}
