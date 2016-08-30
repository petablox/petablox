<xsl:stylesheet
        version="2.0"
        xmlns="http://www.w3.org/1999/xhtml"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:import href="M.xsl"/>
<xsl:import href="C.xsl"/>

<xsl:template match="AS">
    <xsl:apply-templates select="id(@Mid)"/> <br/>
    Context: <xsl:apply-templates select="id(@Cid)"/>
</xsl:template>

</xsl:stylesheet>

