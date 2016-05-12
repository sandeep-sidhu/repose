package features.filters.translation

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Response

class TranslateResponseTest extends ReposeValveTest {

    def String convertStreamToString(byte[] input) {
        return new Scanner(new ByteArrayInputStream(input)).useDelimiter("\\A").next();
    }

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
//        repose.configurationProvider.applyConfigs("features/filters/translation/common", params)
//        repose.configurationProvider.applyConfigs("features/filters/translation/response", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/identity", params)
        repose.start(waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    def "when translating responses"() {
        given:
        def resp = { request ->
            return new Response(200,
                    "OK",
                    ["content-type": "application/json"],
"""{
    "versions": {
        "version": [
            {
                "id": "v1.0",
                "link": {
                    "href": "https://staging.identity-internal.api.rackspacecloud.com/v1.0",
                    "rel": "self"
                },
                "status": "DEPRECATED",
                "updated": "2011-07-19T22:30:00Z"
            },
            {
                "id": "v1.1",
                "link": {
                    "href": "http://docs.rackspacecloud.com/auth/api/v1.1/auth.wadl",
                    "rel": "describedby",
                    "type": "application/vnd.sun.wadl+xml"
                },
                "status": "CURRENT",
                "updated": "2012-01-19T22:30:00.25Z"
            },
            {
                "id": "v2.0",
                "link": {
                    "href": "http://docs.rackspacecloud.com/auth/api/v2.0/auth.wadl",
                    "rel": "describedby",
                    "type": "application/vnd.sun.wadl+xml"
                },
                "status": "CURRENT",
                "updated": "2012-01-19T22:30:00.25Z"
            }
        ]
    }
}""")
        }

        when:
        def mc = deproxy.makeRequest(url: reposeEndpoint + "/cloud",
                method: "GET",
                headers: ["accept": "application/json"],
                requestBody: "something",
                defaultHandler: resp)

        then:
        println(new String(mc.receivedResponse.body))
        if (mc.receivedResponse.body instanceof byte[])
            assert(convertStreamToString(mc.receivedResponse.body).contains("v3.4"))
        else
            assert(mc.receivedResponse.body.contains("v3.4"))
    }
}
