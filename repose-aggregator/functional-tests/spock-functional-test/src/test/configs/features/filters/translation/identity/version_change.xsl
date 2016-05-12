<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:json="http://www.ibm.com/xmlns/prod/2009/jsonx"
    version="2.0">

    <xsl:output method="xml" encoding="UTF-8"/>

    <!-- copy -->
    <xsl:template match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- change version object -->
    <xsl:template match="json:object[@name='versions']">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
            <json:array name="values">
                <json:object>
                    <json:string name="id">v3.4</json:string>
                    <json:array name="links">
                        <json:object>
                            <json:string name="href">https://staging.identity-internal.api.rackspacecloud.com/v3</json:string>
                            <json:string name="rel">self</json:string>
                        </json:object>
                    </json:array>
                    <json:array name="media-types">
                        <json:object>
                            <json:string name="base">application/json</json:string>
                            <json:string name="type">application/vnd.openstack.identity-v3+json</json:string>
                        </json:object>
                    </json:array>
                    <json:string name="status">stable</json:string>
                    <json:string name="updated">2015-03-30T00:00:00Z</json:string>
                </json:object>
            </json:array>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
