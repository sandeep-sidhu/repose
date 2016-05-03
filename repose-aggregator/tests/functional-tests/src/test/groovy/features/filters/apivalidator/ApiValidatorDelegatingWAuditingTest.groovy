package features.filters.apivalidator

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class ApiValidatorDelegatingWAuditingTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/delegable/withauditing", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def "delegating mode should not break intra-filter logging"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/audit", method: method, headers: headers, requestBody: "horrible input")

        then:
        reposeLogSearch.searchByString("Unable to populate request body").isEmpty()

        where:
        method | headers
        "POST" | ["x-roles": "default", "Content-Type": "application/atom+xml"]
    }
}
