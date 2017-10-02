<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
  exclude-result-prefixes="#all"
  version="2.0">
  <xd:doc scope="stylesheet">
    <xd:desc>
      <xd:p><xd:b>Created on:</xd:b> Sep 29, 2017</xd:p>
      <xd:p><xd:b>Author:</xd:b> cmarchand</xd:p>
      <xd:p></xd:p>
    </xd:desc>
  </xd:doc>
  
  <xsl:template match="node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()|namespace::*"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="*">
    <xsl:copy>
      <xsl:copy-of select="namespace::*"/>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="@*">
    <xsl:attribute name="{name()}" namespace="{namespace-uri(.)}" select="concat('_',.,'_')"></xsl:attribute>
  </xsl:template>
    
  <xsl:template match="text()">
    <xsl:text>_</xsl:text><xsl:copy/><xsl:text>_</xsl:text>
  </xsl:template>
</xsl:stylesheet>