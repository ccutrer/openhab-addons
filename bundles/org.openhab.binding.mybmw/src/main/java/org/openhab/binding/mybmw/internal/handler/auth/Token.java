/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mybmw.internal.handler.auth;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mybmw.internal.utils.Constants;

/**
 * The {@link Token} MyBMW Token storage
 *
 * @author Bernd Weymann - Initial contribution
 * @author Martin Grassl - extracted to own class
 */
@NonNullByDefault
public class Token {
    private String token = Constants.EMPTY;
    private String tokenType = Constants.EMPTY;
    private String refreshToken = Constants.EMPTY;
    private String gcid = Constants.EMPTY;

    private long expiration = 0;

    public String getBearerToken() {
        return token.equals(Constants.EMPTY) ? Constants.EMPTY
                : new StringBuilder(tokenType).append(Constants.SPACE).append(token).toString();
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setExpiration(int expiration) {
        this.expiration = System.currentTimeMillis() / 1000 + expiration;
    }

    public void setExpirationTotal(long expiration) {
        this.expiration = expiration;
    }

    public void setType(String type) {
        tokenType = type;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getGcid() {
        return gcid;
    }

    public void setGcid(String gcid) {
        this.gcid = gcid;
    }

    /**
     * check if the token is valid - for enough buffer it is not valid if it expires in <10s
     * 
     * @return
     */
    public boolean isValid() {
        return (!token.equals(Constants.EMPTY) && !tokenType.equals(Constants.EMPTY)
                && !refreshToken.equals(Constants.EMPTY) && (this.expiration - System.currentTimeMillis() / 1000) > 10);
    }

    @Override
    public String toString() {
        return "Token [token=" + token + ", tokenType=" + tokenType + ", refreshToken=" + refreshToken + ", gcid="
                + gcid + ", expiration=" + expiration + "] - is valid " + isValid() + ", will expire in s "
                + (this.expiration - System.currentTimeMillis() / 1000);
    }
}
