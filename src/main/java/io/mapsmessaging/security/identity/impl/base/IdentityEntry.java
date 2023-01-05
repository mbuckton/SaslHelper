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

import io.mapsmessaging.security.identity.parsers.PasswordParser;
import lombok.Getter;

public class IdentityEntry {

  @Getter
  protected String username;

  @Getter
  protected PasswordParser passwordParser;

  @Getter
  protected String password;

  @Override
  public String toString() {
    return username + ":" + password + "\n";
  }
}
