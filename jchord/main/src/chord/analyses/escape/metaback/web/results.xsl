<xsl:stylesheet
	version="2.0"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:include href="E.xsl"/>
<xsl:include href="H.xsl"/>
<xsl:include href="M.xsl"/>

<xsl:template match="/results/resultlist">
    <xsl:result-document href="results.html">
	<html>
	<head>
		<title>Thread-Escape Results</title>
		<link rel="stylesheet" href="style.css" type="text/css"/>
		 <STYLE type="text/css">
		 table.details td.alloc {
  			  background-color: #FFFFFF;
			  font-size: 10pt;
		}
		 table.details td.proven {
  			  background-color: #BDFFBD;
			  font-size: 10pt;
		}
		table.details td.impossible{
			  background-color: #FFCCCC;
			  font-size: 10pt;
		}
		table.details td.unproven {
  			  background-color: #FF7547;
			  font-size: 10pt;
		}
		 </STYLE>
	</head>
		<body>
		<table border="1" class="details">
				<tr><td class="head1">
				Proven Queries ( 
				number = <xsl:value-of select="provenQs/@num"/> )</td>
				</tr>
				<xsl:for-each select="provenQs/group">
				<tr>
					<td class="head2">
					Allocation sites required in L ( 
					number=
					<xsl:value-of select="LHS/@size"/> )
					</td>
				</tr>
					<xsl:for-each select="LHS/H">
				<tr>
					<td class="alloc">
					 <xsl:apply-templates select="id(@Hid)"/> (
					 <xsl:value-of select="."/>
					 )
					</td>
				</tr>
					</xsl:for-each>
					<xsl:for-each select="Query">
					<tr>
					<td class="proven">
					<xsl:apply-templates select="id(@Eid)"/>(
					<xsl:value-of select="."/>
					)
					</td>
					</tr>
					</xsl:for-each>
				</xsl:for-each>
				
				<tr><td class="head1">
				Impossible Queries ( 
				number = <xsl:value-of select="impoQs/@num"/> )</td>
				</tr>
				<xsl:for-each select="impoQs/Query">
					<tr>
					<td class="impossible">
					<xsl:apply-templates select="id(@Eid)"/>(
					<xsl:value-of select="."/>
					)
					</td>
					</tr>
					</xsl:for-each>
				
				<tr><td class="head1">
				Timed-out Queries ( 
				number = <xsl:value-of select="timedQs/@num"/> )</td>
				</tr>
				<xsl:for-each select="timedQs/Query">
					<tr>
					<td class="unproven">
					<xsl:apply-templates select="id(@Eid)"/>(
					<xsl:value-of select="."/>
					)
					</td>
					</tr>
					</xsl:for-each>
				
				<tr><td class="head1">
				DNF-Exploded Queries ( 
				number = <xsl:value-of select="explodedQs/@num"/> )</td>
				</tr>
				<xsl:for-each select="explodedQs/Query">
					<tr>
					<td class="unproven">
					<xsl:apply-templates select="id(@Eid)"/>(
					<xsl:value-of select="."/>
					)
					</td>
					</tr>
					</xsl:for-each>
				
				<tr><td class="head1">
				Too-Many-Iteration Queries ( 
				number = <xsl:value-of select="iterExceedQs/@num"/> )</td>
				</tr>
				<xsl:for-each select="iterExceedQs/Query">
					<tr>
					<td class="unproven">
					<xsl:apply-templates select="id(@Eid)"/>(
					<xsl:value-of select="."/>
					)
					</td>
					</tr>
					</xsl:for-each>
				
		</table>
		</body>
	</html>
    </xsl:result-document>
</xsl:template>


</xsl:stylesheet>

