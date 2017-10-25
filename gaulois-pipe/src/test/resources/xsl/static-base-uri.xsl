<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:math="http://www.w3.org/2005/xpath-functions/math"
  xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
  xmlns:gp="http://efl.fr/chaine/saxon-pipe/config"
  exclude-result-prefixes="xs math xd"
  version="3.0">
  <xd:doc scope="stylesheet">
    <xd:desc>
      <xd:p><xd:b>Created on:</xd:b> Oct 24, 2017</xd:p>
      <xd:p><xd:b>Author:</xd:b> cmarchand</xd:p>
      <xd:p></xd:p>
    </xd:desc>
  </xd:doc>
  
  <xsl:param name="gp:static-base-uri" as="xs:anyURI?"/>
  
  <xsl:template match="/*">
    <ret>
      <static-base-uri><xsl:value-of select="static-base-uri()"/></static-base-uri>
      <gp:static-base-uri><xsl:value-of select="$gp:static-base-uri"/></gp:static-base-uri>
    </ret>
  </xsl:template>
</xsl:stylesheet>