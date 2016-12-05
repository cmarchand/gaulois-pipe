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
    <xsl:param name="p-step2" select="'unset'" as="xs:string"/>
    
    <xsl:template match="text()">
        <xsl:value-of select="upper-case(.)"/>
    </xsl:template>

    <xsl:template match="root">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <step2>
                p-general=<xsl:value-of select="$p-general"/>
                p-step2=<xsl:value-of select="$p-step2"/>
            </step2>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="@*">
        <xsl:copy-of select="."/>
    </xsl:template>
</xsl:stylesheet>