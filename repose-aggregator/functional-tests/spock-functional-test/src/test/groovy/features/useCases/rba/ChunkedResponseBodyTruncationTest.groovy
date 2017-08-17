/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package features.useCases.rba

import framework.ReposeValveTest
import framework.mocks.MockIdentityV2Service
import org.rackspace.deproxy.*
import spock.lang.Unroll

class ChunkedResponseBodyTruncationTest extends ReposeValveTest {

    private static int bodySize = 5000000
    private static Random random = new Random()
    private static MockIdentityV2Service mockKeystoneV2

    private static byte[] generateBody() {
        byte[] body = new byte[bodySize]
        random.nextBytes(body)
        return body
    }

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/useCases/rba", params)

        mockKeystoneV2 = new MockIdentityV2Service(params.identityPort, params.targetPort)

        deproxy = new Deproxy()
        deproxy.addEndpoint(
                port: properties.targetPort,
                defaultHandler: { Request request, HandlerContext context ->
                    context.usedChunkedTransferEncoding = true
                    return new Response(
                            200,
                            null,
                            ["Content-Type": "application/octet-stream"],
                            generateBody())
                }
        )
        deproxy.addEndpoint(
                port: params.identityPort,
                defaultHandler: mockKeystoneV2.handler)

        repose.start()
    }

    @Unroll
    def "when request is sent check to make sure it goes through ip-identity and API-Validator filters #i"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
                url: reposeEndpoint + "/resource",
                method: "get",
                headers: ['X-Auth-Token': mockKeystoneV2.admin_token, 'x-trace-request': 'true'])

        then:
        messageChain.handlings[0].response.body.size() == bodySize
        messageChain.receivedResponse.body.size() == bodySize

        where:
        i << [1..100]
    }
}
