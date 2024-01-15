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

package io.mapsmessaging.security.sasl.provider.scram.server.state;

import io.mapsmessaging.security.logging.AuthLogMessages;
import io.mapsmessaging.security.passwords.PasswordHandler;
import io.mapsmessaging.security.passwords.PasswordHandlerFactory;
import io.mapsmessaging.security.sasl.provider.scram.SessionContext;
import io.mapsmessaging.security.sasl.provider.scram.State;
import io.mapsmessaging.security.sasl.provider.scram.crypto.CryptoHelper;
import io.mapsmessaging.security.sasl.provider.scram.msgs.ChallengeResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import javax.security.auth.callback.*;

public class InitialState extends State {

  public InitialState(String protocol, String serverName, Map<String, ?> props, CallbackHandler cbh) {
    super("", protocol, serverName, props, cbh);
    logger.log(AuthLogMessages.SCRAM_SERVER_STATE_CHANGE, "Initial State");
  }

  @Override
  public boolean isComplete() {
    return false;
  }

  @Override
  public boolean hasInitialResponse() {
    return false;
  }

  @Override
  public ChallengeResponse produceChallenge(SessionContext context) throws IOException, UnsupportedCallbackException {
    if (!context.isReceivedClientMessage()) {
      return null;
    }
    ChallengeResponse response = new ChallengeResponse();
    response.put(ChallengeResponse.NONCE, context.getServerNonce());
    response.put(ChallengeResponse.ITERATION_COUNT, String.valueOf(context.getIterations()));
    response.put(ChallengeResponse.SALT, context.getPasswordSalt());
    context.setState(new ValidationState(this));
    context.setInitialServerChallenge(response.toString());
    return response;
  }

  @Override
  public void handeResponse(ChallengeResponse response, SessionContext context) throws IOException, UnsupportedCallbackException {
    if (response.isEmpty()) {
      return;
    }
    context.setInitialClientChallenge(response.toString());

    //
    // Set up the context with the received information
    //
    context.setReceivedClientMessage(true);
    context.setUsername(response.get(ChallengeResponse.USERNAME));
    context.setClientNonce(response.get(ChallengeResponse.NONCE));

    //
    // Get the server back end user information
    //
    Callback[] callbacks = new Callback[2];
    callbacks[0] = new NameCallback("SCRAM Username Prompt", context.getUsername());
    callbacks[1] = new PasswordCallback("SCRAM Password Prompt", false);
    cbh.handle(callbacks);
    String username = ((NameCallback) callbacks[0]).getName();
    if (username == null) {
      // Need to log an exception
    }

    char[] password = ((PasswordCallback) callbacks[1]).getPassword();
    //
    // To Do: Parse the password by type defined ( BCRYPT, CRYPT,  etc. ) then set the below based
    // on the parsed info
    //

    PasswordHandler handler = PasswordHandlerFactory.getInstance().parse(new String(password));
    context.setPasswordHasher(handler);
    context.setPrepPassword(new String(handler.getPassword()));
    context.setPasswordSalt(new String(Base64.getEncoder().encode(handler.getSalt())));
    context.setIterations(handler.getCost());
    context.setServerNonce(context.getClientNonce() + CryptoHelper.generateNonce(48));
  }
}