<xsl:stylesheet
	version="2.0"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:import href="E.xsl"/>

<xsl:template match="/">
	<xsl:result-document href="results.html">
	<html>
	<head>
		<title>Wait Notify Errors</title>
		<link rel="stylesheet" href="style.css" type="text/css"/>
	</head>
	<body>
		<table class="summary">
			<tr>
				<td>Kind</td><td>Read accesses</td><td>Write accesses</td>
			</tr>
			<xsl:for-each select="results/waitNotifyErrorList/waitNotifyError">
 				<tr>
					<td><xsl:value-of select="@kind"/></td>
					<td>
						<xsl:for-each select="id(@e1idList)">
							<xsl:apply-templates select="."/>
							<xsl:if test="position()!=last()">
								<br/>
							</xsl:if>
						</xsl:for-each>
					</td>
					<td>
						<xsl:for-each select="id(@e2idList)">
							<xsl:apply-templates select="."/>
							<xsl:if test="position()!=last()">
								<br/>
							</xsl:if>
						</xsl:for-each>
					</td>
				</tr>
			</xsl:for-each>
		</table>
	</body>
	</html>
	</xsl:result-document>
</xsl:template>

</xsl:stylesheet>

