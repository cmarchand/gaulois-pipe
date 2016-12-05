<?xml version="1.0" encoding="UTF-8"?>
<!--
This Source Code Form is subject to the terms of 
the Mozilla Public License, v. 2.0. If a copy of 
the MPL was not distributed with this file, You 
can obtain one at https://mozilla.org/MPL/2.0/.
-->
<xsl:stylesheet version="3.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
  xmlns:saxon="http://saxon.sf.net/"
  xmlns:efl="http://els.eu/ns/efl"
  xmlns:local="local"
  xmlns:p="http://www.w3.org/ns/xproc" 
  xmlns:c="http://www.w3.org/ns/xproc-step"
  xmlns:gp="http://efl.fr/chaine/saxon-pipe/config"
  xpath-default-namespace="http://efl.fr/chaine/saxon-pipe/config"
  exclude-result-prefixes="#all"
  >

  <!--Calabash special namespaces-->
  <!--xmlns:cx="http://xmlcalabash.com/ns/extensions" 
  xmlns:cxf="http://xmlcalabash.com/ns/extensions/fileutils"-->

  <xd:doc scope="stylesheet">
    <xd:desc>
      <xd:p>Generate an xproc pipeline from a gaulois-pipe configuration file</xd:p>
    </xd:desc>
  </xd:doc>

  <xsl:output method="xml" indent="yes"/>
  
  <xsl:key name="getElementByName" match="*" use="local-name(.)"/>
  
  <!--====================================-->
  <!--                MAIN                -->
  <!--====================================-->
  
  <xsl:template match="/config">
    <p:declare-step version="1.0" name="current">
      <xsl:namespace name="c" select="'http://www.w3.org/ns/xproc-step'"/>
      <xsl:for-each select="namespaces/mapping">
        <xsl:namespace name="{@prefix}" select="@uri"/>
      </xsl:for-each>
      <xsl:value-of select="'&#10;'"/>
<xsl:text>      </xsl:text><xsl:comment>====================================</xsl:comment><xsl:text>
      </xsl:text><xsl:comment>             INPUT/OUTPUT           </xsl:comment><xsl:text>
      </xsl:text><xsl:comment>====================================</xsl:comment>
      <xsl:value-of select="'&#10;'"/>
      <xsl:apply-templates select="sources"/>
      <xsl:comment>Parameters must be set (even if empty) so they are binded to xslt parameters port</xsl:comment>
      <p:input port="parameters" kind="parameter" primary="true">
        <!--<p:empty/>-->
      </p:input>
      <p:output port="result" primary="true" sequence="true">
        <xsl:comment>Output port is always empty : results will always be saved with p:store</xsl:comment>
        <p:empty/>
      </p:output>
      <xsl:variable name="load" as="element(p:load)*">
        <xsl:apply-templates select="key('getElementByName', 'xslt')[contains(@href, '$')]" mode="load"/>
      </xsl:variable>
      <xsl:if test="count($load) gt 0">
        <xsl:comment>====================================</xsl:comment>
        <xsl:comment>                LOAD                </xsl:comment>
        <xsl:comment>====================================</xsl:comment>
        <xsl:value-of select="'&#10;'"/>
        <xsl:sequence select="$load"/>
      </xsl:if>
      <xsl:comment>Iterate over the sequence of source documents</xsl:comment>
      <p:for-each>
        <xsl:if test="descendant::*/@*[contains(., '$[input-name]')]">
          <p:variable name="input-name">
            <xsl:attribute name="select">
              <xsl:text>tokenize(base-uri(), '/')[last()]</xsl:text>
            </xsl:attribute>
          </p:variable>
        </xsl:if>
        <xsl:if test="descendant::*/@*[contains(., '$[input-basename]')]">
          <p:variable name="input-basename">
            <xsl:attribute name="select">
              <xsl:text>tokenize(tokenize(base-uri(), '/')[last()], '\.')[position() lt last()]</xsl:text>
            </xsl:attribute>
          </p:variable>
        </xsl:if>
        <xsl:if test="descendant::*/@*[contains(., '$[input-extension]')]">
          <p:variable name="input-extension">
            <xsl:attribute name="select">
              <xsl:text>tokenize(tokenize(base-uri(), '/')[last()], '\.')[last()]</xsl:text>
            </xsl:attribute>
          </p:variable>
        </xsl:if>
        <xsl:apply-templates select="params"/>
        <xsl:comment>Identity step to be able to pipe the current source document</xsl:comment>
        <p:identity name="current-iterated-source-document"/>
        <xsl:comment>====================================</xsl:comment>
        <xsl:comment>                STEPS               </xsl:comment>
        <xsl:comment>====================================</xsl:comment>
        <xsl:value-of select="'&#10;'"/>
        <xsl:if test="count($load) gt 0">
          <p:identity>
            <p:input port="source">
              <p:pipe port="source" step="current"/>
            </p:input>
          </p:identity>
        </xsl:if>
        <xsl:apply-templates select="pipe"/>
      </p:for-each>
    </p:declare-step>
  </xsl:template>
  
  <!--=============== PIPE =================-->
  
  <xsl:template match="pipe">
    <xsl:apply-templates/>
  </xsl:template>
  
  <!--=============== SOURCES =================-->
  
  <xsl:template match="/config/sources">
    <p:input port="source" sequence="true">
      <xsl:apply-templates/>
    </p:input>
  </xsl:template>
  
  <xsl:template match="/config/sources/file">
    <p:document href="{@href}"/>
    <!--FIXME : $ variable in href !-->
  </xsl:template>
  
  <!--=============== PARAMS =================-->
  
  <xsl:template match="/config/params[param]">
    <!--<p:input port="parameters" kind="parameter" primary="true">
      <p:inline>
        <c:param-set>
          <xsl:apply-templates/>
        </c:param-set>
      </p:inline>
    </p:input>-->
    <xsl:apply-templates/>
  </xsl:template>
  
  <xsl:template match="/config/params/param">
    <!--<c:param name="{@name}" value="{@value}"/>-->
    <p:variable name="{@name}" select="'{@value}'"/>
  </xsl:template>
  
  <!--=============== XSLT =================-->
  
  <xsl:template match="xslt" mode="load">
    <p:load name="load_xslt_{generate-id(.)}">
      <p:with-option name="href" select="{gp:getGpStringAsXpath(@href)}"/>
    </p:load>
  </xsl:template>
  
  <xsl:template match="xslt">
    <p:xslt name="{gp:makeStepName(.)}">
      <p:input port="stylesheet">
        <xsl:choose>
          <xsl:when test="gp:containsVar(@href)">
            <p:pipe port="result" step="load_xslt_{generate-id(.)}"/>
          </xsl:when>
          <xsl:otherwise>
            <p:document href="{@href}"/>
          </xsl:otherwise>
        </xsl:choose>
      </p:input>
      <xsl:apply-templates />
      <!--<p:log href="" port="result"/>-->
    </p:xslt>
  </xsl:template>
  
  <xsl:template match="xslt/param">
    <p:with-param name="{@name}" select="{gp:getGpStringAsXpath(@value, true())}"/>
    <!--FIXME : xproc params are always strings (?), the same in gp ? casting in the xslt might not works with xproc...-->
  </xsl:template>
  
  <!--=============== TEE =================-->
  
  <xsl:template match="tee/pipe[1]" priority="2">
    <p:group>
      <xsl:apply-templates/>
    </p:group>
  </xsl:template>
  
  <xsl:template match="tee/pipe" priority="1">
    <p:group>
      <p:identity>
        <p:input port="source">
          <p:pipe port="source" step="{gp:makeStepName(parent::tee/preceding-sibling::*)}"/>
        </p:input>
      </p:identity>
      <xsl:apply-templates/>
    </p:group>
  </xsl:template>
  
  <!--=============== CHOOSE =================-->
  <!--when/@test occurs on source document-->
  
  <xsl:template match="choose">
    <p:choose>
      <xsl:for-each select="when">
        <xsl:comment>Predefine when test because it's applied on source document</xsl:comment>
        <p:variable name="when{count(preceding-sibling::when) + 1}" select="{@test}">
          <p:pipe port="result" step="current-iterated-source-document"/>
        </p:variable>
      </xsl:for-each>
      <xsl:apply-templates/>
    </p:choose>
  </xsl:template>
  
  <xsl:template match="choose/when">
    <p:when test="xs:boolean($when{count(preceding-sibling::when) + 1})">
      <xsl:apply-templates/>
    </p:when>
  </xsl:template>
  
  <xsl:template match="choose/otherwise">
    <p:otherwise>
      <xsl:apply-templates/>
    </p:otherwise>
  </xsl:template>
  
  <!--=============== OUTPUT =================-->
  
  <xsl:template match="output">
    <p:store>
      <p:with-option name="href">
        <xsl:attribute name="select">
          <xsl:text>resolve-uri(</xsl:text>
          <xsl:value-of select="gp:getGpStringAsXpath(fileName/@name, true())"/>
          <xsl:text>, </xsl:text>
          <xsl:value-of select="gp:getGpStringAsXpath(folder/(@relative, @absolute)[1], true())"/>
          <xsl:text>)</xsl:text>
        </xsl:attribute>
      </p:with-option>
    </p:store>
  </xsl:template>
  
  <!--====================================-->
  <!--              COMMON                -->
  <!--====================================-->
  
  <xsl:template match="*">
    <xsl:message>[ERROR] '<xsl:value-of select="local-name(.)"/>' UNMATCHED</xsl:message>
    <xsl:next-match/>
  </xsl:template>
  
  <xsl:template match="text()"/>
  
  <xsl:variable name="quot" as="xs:string">
    <xsl:text>'</xsl:text>
  </xsl:variable>
  
  <xsl:function name="gp:makeStepName" as="xs:string">
    <xsl:param name="e" as="element()"/>
    <xsl:sequence select="($e/@id, concat(local-name($e), '_', generate-id($e)))[1]"/>
  </xsl:function>

  <!--FIXME : would match $[x} => make it better (keeping regex-groups !)-->
  <xsl:variable name="regVar" select="'\$([\[{{])(.+?)([}}\]])'" as="xs:string"/>
  
  <xsl:function name="gp:containsVar" as="xs:boolean">
    <xsl:param name="string" as="xs:string"/>
    <xsl:sequence select="matches($string, $regVar)"/>
    <!--<xsl:sequence select="matches($string, '\$\[(.+?)\]')"/>-->
  </xsl:function>
  
  <xsl:function name="gp:getGpStringAsXpath" as="xs:string">
    <xsl:param name="href" as="xs:string"/>
    <xsl:param name="withQuot" as="xs:boolean"/>
    <xsl:variable name="hrefAsXpath" as="xs:string*">
      <xsl:choose>
        <xsl:when test="gp:containsVar($href)">
          <xsl:text>concat(</xsl:text>
          <xsl:variable name="tmp" as="xs:string*">
            <xsl:analyze-string select="$href" regex="{$regVar}">
              <xsl:matching-substring>
                <!--FIXME : resolve system properties with p:system-property-->
                <xsl:value-of select="concat('$', regex-group(2))"/>
              </xsl:matching-substring>
              <xsl:non-matching-substring>
                <xsl:value-of select="concat($quot, ., $quot)"/>
              </xsl:non-matching-substring>
            </xsl:analyze-string>
          </xsl:variable>
          <xsl:value-of select="string-join($tmp, ', ')"/>
          <xsl:text>)</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$href"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$withQuot and not(starts-with($hrefAsXpath[1], 'concat('))">
        <xsl:value-of select="concat($quot, string-join($hrefAsXpath, ''), $quot)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="string-join($hrefAsXpath, '')"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>
  
  <xsl:function name="gp:getGpStringAsXpath" as="xs:string">
    <xsl:param name="href" as="xs:string"/>
    <xsl:sequence select="gp:getGpStringAsXpath($href, false())"/>
  </xsl:function>
  
</xsl:stylesheet>