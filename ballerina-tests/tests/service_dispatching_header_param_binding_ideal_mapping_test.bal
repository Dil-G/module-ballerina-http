// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;
import ballerina/test;

listener http:Listener HeaderBindingIdealEP = new(headerParamBindingIdealTestPort);
http:Client headerBindingIdealClient = check new("http://localhost:" + headerParamBindingIdealTestPort.toString());

@http:ServiceConfig {
    treatNilableAsOptional : false
}
service /headerparamservice on HeaderBindingIdealEP {

    resource function get test1(@http:Header string foo, int bar) returns json {
        json responseJson = { value1: foo, value2: bar};
        return responseJson;
    }

    resource function get test2(@http:Header string? foo, int bar) returns json {
        json responseJson = { value1: foo ?: "empty", value2: bar};
        return responseJson;
    }
}

@test:Config {}
function testIdealHeaderParamBindingWithHeaderValue() {
    json|error response = headerBindingIdealClient->get("/headerparamservice/test1?foo=WSO2&bar=56", {"foo":"Ballerina"});
    if response is json {
        test:assertEquals(response, {value1:"Ballerina", value2:56}, msg = "Found unexpected output");
    } else {
        test:assertFail(msg = "Found unexpected output type: " + response.message());
    }

    response = headerBindingIdealClient->get("/headerparamservice/test2?foo=WSO2&bar=56", {"foo":"Ballerina"});
    if response is json {
        test:assertEquals(response, {value1:"Ballerina", value2:56}, msg = "Found unexpected output");
    } else {
        test:assertFail(msg = "Found unexpected output type: " + response.message());
    }
}

@test:Config {}
function testIdealHeaderParamBindingWithoutHeader() {
    http:Response|error response = headerBindingIdealClient->get("/headerparamservice/test1?foo=WSO2&bar=56");
    if response is http:Response {
        test:assertEquals(response.statusCode, 400);
        assertTextPayload(response.getTextPayload(), "no header value found for 'foo'");
    } else {
        test:assertFail(msg = "Found unexpected output type: " + response.message());
    }

    response = headerBindingIdealClient->get("/headerparamservice/test2?foo=WSO2&bar=56");
    if response is http:Response {
        test:assertEquals(response.statusCode, 400);
        assertTextPayload(response.getTextPayload(), "no header value found for 'foo'");
    } else {
        test:assertFail(msg = "Found unexpected output type: " + response.message());
    }
}

@test:Config {}
function testIdealHeaderParamBindingWithNoHeaderValue() {
    http:Response|error response = headerBindingIdealClient->get("/headerparamservice/test1?foo=WSO2&bar=56", {"foo" : ""});
    if response is http:Response {
        test:assertEquals(response.statusCode, 400);
        assertTextPayload(response.getTextPayload(), "no header value found for 'foo'");
    } else {
        test:assertFail(msg = "Found unexpected output type: " + response.message());
    }

    response = headerBindingIdealClient->get("/headerparamservice/test2?foo=WSO2&bar=56", {"foo" : ""});
    if response is http:Response {
        assertJsonPayload(response.getJsonPayload(), {value1:"empty", value2:56});
    } else {
        test:assertFail(msg = "Found unexpected output type: " + response.message());
    }
}
