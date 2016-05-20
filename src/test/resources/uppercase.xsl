<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema" 
    xmlns:math="http://www.w3.org/2005/xpath-functions/math" 
    xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
    exclude-result-prefixes="xs math xd" 
    version="2.0">
<!--
This Source Code Form is subject to the terms of 
the Mozilla Public License, v. 2.0. If a copy of 
the MPL was not distributed with this file, You 
can obtain one at https://mozilla.org/MPL/2.0/.
-->

    <xsl:import href="identity.xsl"/>
    <xd:doc scope="stylesheet">
        <xd:desc>
            <xd:p>This XSL program put all element names to uppercase</xd:p>
        </xd:desc>
    </xd:doc>
    
    <xd:doc scope="component">
        <xd:desc>
            <xd:p>Put the element names to upper-case, and do recursion</xd:p>
        </xd:desc>
    </xd:doc>
    <xsl:template match="*">
        <xsl:message>Message sans log</xsl:message>
        <xsl:message><log channel="MARC" level="info">Message avec log et channel</log></xsl:message>
        <xsl:message><log level="error">Message niveau error sans channel</log></xsl:message>
        
        <xsl:element name="{upper-case(local-name(.))}" namespace="{namespace-uri(.)}">
            <xsl:apply-templates select="node() | @*"/>
        </xsl:element>
    </xsl:template>
        
</xsl:transform>
