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

import io.ballerina.projects.plugins.codeaction.CodeActionArgument;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static io.ballerina.stdlib.http.compiler.codeaction.Constants.IS_ERROR_INTERCEPTOR_TYPE;
import static io.ballerina.stdlib.http.compiler.codeaction.Constants.NODE_LOCATION_KEY;

/**
 * Test for adding interceptor method templates.
 */
public class AddInterceptorMethodTest extends AbstractCodeActionTest {

    @Test(dataProvider = "testDataProvider")
    public void testCodeActions(String srcFile, int line, int offset, CodeActionInfo expected, String resultFile)
            throws IOException {
        Path filePath = RESOURCE_PATH.resolve("ballerina_sources")
                .resolve("sample_codeaction_package_3")
                .resolve(srcFile);
        Path resultPath = RESOURCE_PATH.resolve("codeaction")
                .resolve(getConfigDir())
                .resolve(resultFile);

        performTest(filePath, LinePosition.from(line, offset), expected, resultPath);
    }

    @DataProvider
    private Object[][] testDataProvider() {
        return new Object[][]{
                {"service.bal", 22, 29, getAddInterceptorResourceMethodCodeAction(LinePosition.from(21, 0),
                                                    LinePosition.from(23, 1), false), "result1.bal"},
                {"service.bal", 26, 34, getAddInterceptorResourceMethodCodeAction(LinePosition.from(25, 0),
                                                    LinePosition.from(27, 1), true), "result2.bal"},
                {"service.bal", 30, 30, getAddInterceptorRemoteMethodCodeAction(LinePosition.from(29, 0),
                                                    LinePosition.from(31, 1), false), "result3.bal"},
                {"service.bal", 34, 35, getAddInterceptorRemoteMethodCodeAction(LinePosition.from(33, 0),
                                                    LinePosition.from(35, 1), true), "result4.bal"}
        };
    }

    private CodeActionInfo getAddInterceptorResourceMethodCodeAction(LinePosition startLine, LinePosition endLine,
                                                                     boolean isErrorInterceptor) {
        LineRange lineRange = LineRange.from("service.bal", startLine, endLine);
        CodeActionArgument locationArg = CodeActionArgument.from(NODE_LOCATION_KEY, lineRange);
        CodeActionArgument isError = CodeActionArgument.from(IS_ERROR_INTERCEPTOR_TYPE, isErrorInterceptor);
        CodeActionInfo codeAction = CodeActionInfo.from("Add interceptor resource method", List.of(locationArg,
                                                                                                   isError));
        codeAction.setProviderName("HTTP_132/ballerina/http/ADD_INTERCEPTOR_RESOURCE_METHOD");
        return codeAction;
    }

    private CodeActionInfo getAddInterceptorRemoteMethodCodeAction(LinePosition startLine, LinePosition endLine,
                                                                   boolean isErrorInterceptor) {
        LineRange lineRange = LineRange.from("service.bal", startLine, endLine);
        CodeActionArgument locationArg = CodeActionArgument.from(NODE_LOCATION_KEY, lineRange);
        CodeActionArgument isError = CodeActionArgument.from(IS_ERROR_INTERCEPTOR_TYPE, isErrorInterceptor);
        CodeActionInfo codeAction = CodeActionInfo.from("Add interceptor remote method", List.of(locationArg, isError));
        codeAction.setProviderName("HTTP_135/ballerina/http/ADD_INTERCEPTOR_REMOTE_METHOD");
        return codeAction;
    }

    protected String getConfigDir() {
        return "add_interceptor_method";
    }
}
