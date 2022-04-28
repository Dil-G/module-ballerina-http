// Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

// This is added to test some auto generated code segments.
// Please ignore the indentation.

import ballerina/http;

service class RequestInterceptor {
	*http:RequestInterceptor;
}

service class RequestErrorInterceptor {
	*http:RequestErrorInterceptor;

	resource function 'default [string... path](error err, http:RequestContext ctx) returns http:NextService|error? {
		// add your logic here
		return ctx.next();
	}
}

service class ResponseInterceptor {
	*http:ResponseInterceptor;
}

service class ResponseErrorInterceptor {
	*http:ResponseErrorInterceptor;
}

service /greeting on new http:Listener(9090) {
	resource function get hi() returns string {
		return "hi";
	}
}
