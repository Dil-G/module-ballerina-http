/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.http.transport.http1point0test;

import io.ballerina.stdlib.http.transport.chunkdisable.ChunkClientTemplate;
import io.ballerina.stdlib.http.transport.contract.config.ChunkConfig;
import io.ballerina.stdlib.http.transport.message.HttpCarbonMessage;
import io.ballerina.stdlib.http.transport.util.TestUtil;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

/**
 * A test class for auto chunking behaviour for http 1.0.
 */
public class ChunkAutoHttpOnePointZeroClientTestCase extends ChunkClientTemplate {

    @BeforeClass
    public void setUp() {
        senderConfiguration.setChunkingConfig(ChunkConfig.AUTO);
        senderConfiguration.setHttpVersion("1.0");
        super.setUp();
    }

    @Test
    public void postTest() {
        try {
            HttpCarbonMessage response = sendRequest(TestUtil.largeEntity);
            assertEquals(response.getHeader(HttpHeaderNames.CONTENT_LENGTH.toString()), "9342");

            response = sendRequest(TestUtil.smallEntity);
            assertEquals(response.getHeader(HttpHeaderNames.CONTENT_LENGTH.toString()), "70");

        } catch (Exception e) {
            TestUtil.handleException("Exception occurred while running postTest", e);
        }
    }
}
