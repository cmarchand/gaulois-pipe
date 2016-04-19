<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="2.0">
<!--
This Source Code Form is subject to the terms of 
the Mozilla Public License, v. 2.0. If a copy of 
the MPL was not distributed with this file, You 
can obtain one at https://mozilla.org/MPL/2.0/.
-->
    
    <xsl:param name="p-general" select="'unset'" as="xs:string"/>
    <xsl:param name="p-step3" select="'unset'" as="xs:string"/>
    
    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="root">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <step3>
                p-general=<xsl:value-of select="$p-general"/>
                p-step3=<xsl:value-of select="$p-step3"/>
            </step3>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="@*">
        <xsl:copy-of select="."/>
    </xsl:template>
    
    <xsl:template match="information">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <contenu><xsl:apply-templates select="node()"/></contenu>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>