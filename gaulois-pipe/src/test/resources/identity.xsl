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
    <xd:doc scope="stylesheet">
        <xd:desc>
            <xd:p>Cette XSL permet de recopier à l'identique le document source</xd:p>
            <xd:p>Elle conserve tous les commentaires</xd:p>
            <xd:p>Elle supprime toutes les processing-instructions</xd:p>
        </xd:desc>
    </xd:doc>
    
    <xd:doc scope="component">
        <xd:desc>
            <xd:p>Recopie les balises et le textes, et procède au traitement de tous les fils (y compris les commentaires et les PI)</xd:p>
        </xd:desc>
    </xd:doc>
    <xsl:template match="node()">
        <xsl:copy>
            <xsl:apply-templates select="node() | @*"/>
        </xsl:copy>
    </xsl:template>
    
    <xd:doc scope="component">
        <xd:desc>
            <xd:p>Recopie les attributs à l'identique</xd:p>
        </xd:desc>
    </xd:doc>
    <xsl:template match="@*">
        <xsl:copy-of select="."/>
    </xsl:template>
    
</xsl:transform>
