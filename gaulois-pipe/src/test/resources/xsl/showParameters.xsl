<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl" exclude-result-prefixes="xs xd" version="2.0">
  <!--
This Source Code Form is subject to the terms of 
the Mozilla Public License, v. 2.0. If a copy of 
the MPL was not distributed with this file, You 
can obtain one at https://mozilla.org/MPL/2.0/.
-->

  <xsl:param name="input-basename" as="xs:string"/>
  <xsl:param name="input-name" as="xs:string"/>
  <xsl:param name="input-extension" as="xs:string"/>
  <xsl:param name="input-absolute" as="xs:string"/>
  <xsl:param name="input-relative-file" as="xs:string"/>
  <xsl:param name="input-relative-dir" as="xs:string"/>

  <xsl:template match="/"><xsl:text xml:space="preserve">
</xsl:text>input-basename=<xsl:value-of select="$input-basename"/><xsl:text xml:space="preserve">
</xsl:text>input-name=<xsl:value-of select="$input-name"/><xsl:text xml:space="preserve">
</xsl:text>input-extension=<xsl:value-of select="$input-extension"/><xsl:text xml:space="preserve">
</xsl:text>input-absolute=<xsl:value-of select="$input-absolute"/><xsl:text xml:space="preserve">
</xsl:text>input-relative-file=<xsl:value-of select="$input-relative-file"/><xsl:text xml:space="preserve">
</xsl:text>input-relative-dir=<xsl:value-of select="$input-relative-dir"/><xsl:text xml:space="preserve">
</xsl:text>
  </xsl:template>
</xsl:stylesheet>
