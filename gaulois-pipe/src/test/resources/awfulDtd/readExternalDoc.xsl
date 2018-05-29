<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:math="http://www.w3.org/2005/xpath-functions/math"
  xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
  exclude-result-prefixes="xs math xd"
  version="3.0">
  <xd:doc scope="stylesheet">
    <xd:desc>
      <xd:p><xd:b>Created on:</xd:b> May 24, 2018</xd:p>
      <xd:p><xd:b>Author:</xd:b> cmarchand</xd:p>
      <xd:p></xd:p>
    </xd:desc>
  </xd:doc>
  
  <!-- dans ce cas particulier, on recevra une URL absolue, car le pipe utilise un file et pas un dir -->
  <xsl:param name="input-relative-file" as="xs:string"/>
  
  <xsl:output indent="yes"/>
  
  <xd:doc>
    <xd:desc>Load document with document() function to check everything is OK</xd:desc>
  </xd:doc>
  <xsl:template match="/" expand-text="yes">
    <xsl:variable name="inputDoc" as="document-node()" select="document($input-relative-file)"/>
    <xsl:comment><xsl:value-of select="concat('input doc is a ', $inputDoc/*/local-name())"/></xsl:comment>
    <xsl:apply-templates select="node()"/>
  </xsl:template>
  
  <xd:doc>
    <xd:desc>Copy template</xd:desc>
  </xd:doc>
  <xsl:template match="node() | @*">
    <xsl:copy>
      <xsl:apply-templates select="node() | @*"/>
    </xsl:copy>
  </xsl:template>
  
  <xd:doc>
    <xd:desc>Copy comments &amp; processing instructions</xd:desc>
  </xd:doc>
  <xsl:template match="comment() | processing-instruction()">
    <xsl:copy/>
  </xsl:template>
  
</xsl:stylesheet>