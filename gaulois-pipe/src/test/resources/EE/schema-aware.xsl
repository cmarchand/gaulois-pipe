<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:math="http://www.w3.org/2005/xpath-functions/math"
  xmlns:gpt="top:marchand:xml:gaulois:tests"
  exclude-result-prefixes="#all"
  version="3.0">

  <xsl:import-schema schema-location="schema.xsd" namespace="top:marchand:xml:gaulois:tests"/>

  <xsl:template match="/">
    <xsl:variable name="myDoc" as="document-node(schema-element(gpt:data))">
      <xsl:document validation="strict">
        <xsl:apply-templates/>
      </xsl:document>
    </xsl:variable>
    <xsl:copy-of select="$myDoc"/>
  </xsl:template>
    
  <xsl:template match="processing-instruction()" priority="1"/>
  
  <xsl:template match="node() | @*">
    <xsl:copy>
      <xsl:apply-templates select="node() | @*"/>
    </xsl:copy>
  </xsl:template>
  
</xsl:stylesheet>