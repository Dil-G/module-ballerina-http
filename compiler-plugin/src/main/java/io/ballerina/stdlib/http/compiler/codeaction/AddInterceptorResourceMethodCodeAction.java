/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.http.compiler.codeaction;

import io.ballerina.stdlib.http.compiler.HttpDiagnosticCodes;

import static io.ballerina.stdlib.http.compiler.codeaction.Constants.LS;
import static io.ballerina.stdlib.http.compiler.codeaction.Constants.RESOURCE;

/**
 * CodeAction to add the resource method for request/ request error interceptor.
 */
public class AddInterceptorResourceMethodCodeAction extends AddInterceptorMethodCodeAction {
    @Override
    protected String diagnosticCode() {
        return HttpDiagnosticCodes.HTTP_132.getCode();
    }

    @Override
    protected String methodKind() {
        return RESOURCE;
    }

    @Override
    protected String methodSignature(boolean isErrorInterceptor) {
        String method = LS + "\tresource function 'default [string... path](";
        method += isErrorInterceptor ? "error err, " : "";
        method += "http:RequestContext ctx) returns http:NextService|error? {" + LS +
                "\t\t// add your logic here" + LS +
                "\t\treturn ctx.next();" + LS +
                "\t}" + LS;
        return method;
    }

    @Override
    public String name() {
        return "ADD_INTERCEPTOR_RESOURCE_METHOD";
    }
}
