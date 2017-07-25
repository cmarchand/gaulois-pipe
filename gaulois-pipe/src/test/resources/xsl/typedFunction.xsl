<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
  xmlns:local="top:marchand:xml:gaulois-pipe"
  exclude-result-prefixes="xs xd"
  version="2.0">
  
  <xd:doc scope="stylesheet">
    <xd:desc>
      <xd:p><xd:b>Created on:</xd:b> Aug 21, 2017</xd:p>
      <xd:p><xd:b>Author:</xd:b> cmarchand</xd:p>
      <xd:p>This stylesheet must fail if document root node does not have a @value</xd:p>
    </xd:desc>
  </xd:doc>
  
  <xsl:template match="/*">
    <value><xsl:value-of select="local:getValue(.)"/></value>
  </xsl:template>
  
  <xd:doc>
    <xd:desc>
      <xd:p>Returns the @value of the node</xd:p>
    </xd:desc>
    <xd:param name="node"></xd:param>
    <xd:return></xd:return>
  </xd:doc>
  <xsl:function name="local:getValue" as="xs:string">
    <xsl:param name="node" as="element()"/>
    <xsl:variable name="value" as="xs:date" select="$node/xs:date(@value)"/>
    <xsl:sequence select="format-date($value,'[D01]/[M01]/[Y0001]')"/>
  </xsl:function>
  
</xsl:stylesheet>