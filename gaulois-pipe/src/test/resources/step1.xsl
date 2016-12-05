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
    <xsl:param name="p-step1" select="'unset'" as="xs:string"/>
    
    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>
    
    <xsl:template match="root">
        <xsl:copy>
            <xsl:attribute name="date" select="'2015-09-25T09:42:11.046+02:00'"/>
            <step1>
                p-general=<xsl:value-of select="$p-general"/>
                p-step1=<xsl:value-of select="$p-step1"/>
            </step1>
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
    
    <xsl:template match="text()">
        <xsl:copy-of select="."/>
    </xsl:template>
</xsl:stylesheet>