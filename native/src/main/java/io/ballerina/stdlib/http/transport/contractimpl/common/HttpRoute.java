/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.ballerina.stdlib.http.transport.contractimpl.common;

/**
 * Class encapsulates the Endpoint address.
 */
public class HttpRoute {
    private String scheme;
    private String host;
    private int port;
    private int configHashCode;

    public HttpRoute(String scheme, String host, int port, int configHashCode) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.configHashCode = configHashCode;
    }

    @Override
    public String toString() {
        return scheme + "-" + host + "-" + port + "-" + configHashCode;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getScheme() {
        return scheme;
    }

    public int getConfigHash() {
        return configHashCode;
    }
}
