                <xsl:stylesheet version="1.0"
                    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                    xmlns:json="http://www.ibm.com/xmlns/prod/2009/jsonx" exclude-result-prefixes="json">
                    
                    <xsl:output method="text" encoding="utf-8"/>
                    
                    <xsl:param name="pretty_print" select="true()"/>
                    <xsl:variable name="indent" select="'    '"/>
                    
                    <xsl:template name="json:quotify">
                        <xsl:param name="in"/>
                        <xsl:variable name="before" select="substring-before($in, '&quot;')"/>
                        <xsl:variable name="after" select="substring-after($in,'&quot;')"/>
                        <xsl:choose>
                            <xsl:when test="string-length($before) &gt; 0 or string-length($after) &gt; 0">
                               <xsl:value-of select="concat($before,'\&quot;')"/>
                               <xsl:call-template name="json:quotify">
                                   <xsl:with-param name="in"><xsl:value-of select="$after"/></xsl:with-param>
                               </xsl:call-template>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$in"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:template>
                    
                    <xsl:template name="json:doNameAttr">
                        <xsl:if test="local-name(..)!='array' and string-length(@name)>0">
                            <xsl:text>"</xsl:text>
                            <xsl:call-template name="json:quotify">
                                <xsl:with-param name="in"><xsl:value-of select="@name"/></xsl:with-param>
                            </xsl:call-template>
                            <xsl:text>" : </xsl:text>
                        </xsl:if>
                    </xsl:template>
                    
                    <xsl:template name="json:indent">
                        <xsl:param name="i" select="0"/>
                        <xsl:if test="$pretty_print">
                            <xsl:if test="$i &gt; 0">
                                <xsl:value-of select="$indent"/>
                                <xsl:call-template name="json:indent">
                                    <xsl:with-param name="i" select="$i - 1"/>
                                </xsl:call-template>
                            </xsl:if>
                       </xsl:if>
                    </xsl:template>
                    
                    <xsl:template name="json:eol">
                        <xsl:if test="$pretty_print"><xsl:text>&#x0a;</xsl:text></xsl:if>
                    </xsl:template>
                    
                    <xsl:template match="/">
                        <xsl:call-template name="json:eol"/>
                        <xsl:apply-templates/>
                    </xsl:template>
                    
                    <xsl:template match="json:object">
                        <xsl:param name="i" select="0"/>
                        <xsl:call-template name="json:indent">
                            <xsl:with-param name="i" select="$i"/>
                        </xsl:call-template>
                        <xsl:call-template name="json:doNameAttr"/>
                        <xsl:text>{</xsl:text>
                        <xsl:call-template name="json:eol"/>
                        <xsl:apply-templates>
                            <xsl:with-param name="i" select="$i + 1"/>
                        </xsl:apply-templates>
                        <xsl:call-template name="json:indent">
                            <xsl:with-param name="i" select="$i"/>
                        </xsl:call-template>
                        <xsl:text>}</xsl:text>
                        <xsl:if test="following-sibling::json:*">
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                        <xsl:call-template name="json:eol"/>
                    </xsl:template>
                    
                    <xsl:template match="json:array">
                        <xsl:param name="i" select="0"/>
                        <xsl:call-template name="json:indent">
                            <xsl:with-param name="i" select="$i"/>
                        </xsl:call-template>
                        <xsl:call-template name="json:doNameAttr" />
                        <xsl:text>[ </xsl:text>
                        <xsl:call-template name="json:eol"/>
                        <xsl:apply-templates>
                            <xsl:with-param name="i" select="$i + 1"/>
                        </xsl:apply-templates>
                        <xsl:call-template name="json:indent">
                            <xsl:with-param name="i" select="$i"/>
                        </xsl:call-template>
                        <xsl:text> ]</xsl:text>
                        <xsl:if test="following-sibling::json:*">
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                        <xsl:call-template name="json:eol"/>
                    </xsl:template>
                    
                    <xsl:template match="json:string">
                        <xsl:param name="i" select="0"/>
                        <xsl:call-template name="json:indent">
                            <xsl:with-param name="i" select="$i"/>
                        </xsl:call-template>
                        <xsl:call-template name="json:doNameAttr"/>
                        <xsl:text>"</xsl:text>
                        <xsl:call-template name="json:quotify">
                            <xsl:with-param name="in">
                                <xsl:value-of select="normalize-space()"/>
                            </xsl:with-param>
                        </xsl:call-template>
                        <xsl:text>"</xsl:text>
                        <xsl:if test="following-sibling::json:*">
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                        <xsl:call-template name="json:eol"/>
                    </xsl:template>
                    
                    <xsl:template match="json:number">
                        <xsl:param name="i" select="0"/>
                        <xsl:call-template name="json:indent">
                            <xsl:with-param name="i" select="$i"/>
                        </xsl:call-template>
                        <xsl:call-template name="json:doNameAttr"/>
                        <xsl:value-of select="normalize-space()"/>
                        <xsl:if test="following-sibling::json:*">
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                        <xsl:call-template name="json:eol"/>
                    </xsl:template>
                    
                    <xsl:template match="json:boolean">
                        <xsl:param name="i" select="0"/>
                        <xsl:call-template name="json:indent">
                            <xsl:with-param name="i" select="$i"/>
                        </xsl:call-template>
                        <xsl:call-template name="json:doNameAttr"/>
                        <xsl:value-of select="normalize-space()"/>
                        <xsl:if test="following-sibling::json:*">
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                        <xsl:call-template name="json:eol"/>
                    </xsl:template>
                    
                    <xsl:template match="json:null">
                        <xsl:param name="i" select="0"/>
                        <xsl:call-template name="json:indent">
                            <xsl:with-param name="i" select="$i"/>
                        </xsl:call-template>
                        <xsl:call-template name="json:doNameAttr"/>
                        <xsl:text>null</xsl:text>
                        <xsl:if test="following-sibling::json:*">
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                        <xsl:call-template name="json:eol"/>
                    </xsl:template>
                    
                </xsl:stylesheet>
