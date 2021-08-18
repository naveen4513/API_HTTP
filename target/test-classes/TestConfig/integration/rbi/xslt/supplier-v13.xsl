<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:random="http://exslt.org/random"
                extension-element-prefixes="random">
    <xsl:output method="text"/>
    <xsl:template match="/">
        [
        <xsl:variable name="supplierCount" select="count(suppliers/supplier)"/>
        <xsl:for-each select="suppliers/supplier">
            {
            "basicInfo": {
            "entityType": {
            "id": 1,
            "name": "Supplier",
            "description": "Supplier"
            },
            "clientInfo": {
            "source" : "filename15",
            "id": 1005,
            "name": "QA",
            "alias": "qa"
            },
            "integrationSystem": {
            "id": 1,
            "name": "test"
            },
            "clientPrimaryKey": "<xsl:value-of select="H_SEGMENT1"/>",
            "parentKey": {
            "entityTypeId": 3,
            "clientPrimaryKey": "<xsl:value-of select="translate(H_ATTRIBUTE3,  ' ', '')"/>"
            }
            },
            "otherInfo": {
            "rawFile": {},
            "updateEnabled": true
            },
            "dataInfo": {
            "dataFields": [
            {
            "id": 11308,
            "apiName": "oldSystemId",
            "alias": "ECC ID",
            "properties": {},
            "fieldValue": {
            "type": "TEXT",
            "value": "<xsl:value-of select="H_SEGMENT1"/>"
            }
            },
            {
            "id": "510",
            "apiName": "globalRegions",
            "alias": "globalRegions",
            "properties":{},
            "fieldValue": {
            "type": "MULTI_SELECT",
            "value": [
            {
            <xsl:choose>
                <xsl:when test="S_COUNTRY = 'RU'">
                    "id": 1013,
                    "name": "EUROPE"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'AT'">
                    "id": 1013,
                    "name": "EUROPE"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'DE'">
                    "id": 1013,
                    "name": "EUROPE"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'GB'">
                    "id": 1013,
                    "name": "EUROPE"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'HU'">
                    "id": 1013,
                    "name": "EUROPE"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'PL'">
                    "id": 1013,
                    "name": "EUROPE"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'RO'">
                    "id": 1013,
                    "name": "EUROPE"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'SK'">
                    "id": 1013,
                    "name": "EUROPE"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'SK'">
                    "id": 1013,
                    "name": "EUROPE"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'CH'">
                    "id": 1013,
                    "name": "EUROPE"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'CZ'">
                    "id": 1013,
                    "name": "EUROPE"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'NO'">
                    "id": 1013,
                    "name": "EUROPE"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'HR'">
                    "id": 1013,
                    "name": "EUROPE"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'BY'">
                    "id": 1013,
                    "name": "EUROPE"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'UA'">
                    "id": 1013,
                    "name": "EUROPE"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'AL'">
                    "id": 1037,
                    "name": "hii"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'CS'">
                    "id": 1040,
                    "name": "NON EUROPE"
                </xsl:when>
            </xsl:choose>
            }
            ]
            }
            },
            {
            "id": "511",
            "apiName": "globalCountries",
            "alias": "globalCountries",
            "properties":{},
            "fieldValue": {
            "type": "MULTI_SELECT",
            "value": [
            {
            <xsl:choose>
                <xsl:when test="S_COUNTRY = 'RU'">
                    "id": 193,
                    "parentId": 1013,
                    "name": "Russia"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'AT'">
                    "id": 16,
                    "parentId": 1013,
                    "name": "Austria"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'DE'">
                    "id": 90,
                    "parentId": 1013,
                    "name": "Germany"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'GB'">
                    "id": 243,
                    "parentId": 1013,
                    "name": "United Kingdom"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'HU'">
                    "id": 109,
                    "parentId": 1013,
                    "name": "Hungary"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'PL'">
                    "id": 187,
                    "parentId": 1013,
                    "name": "Poland"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'RO'">
                    "id": 192,
                    "parentId": 1013,
                    "name": "Romania"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'SK'">
                    "id": 209,
                    "parentId": 1013,
                    "name": "Slovakia"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'CH'">
                    "id": 223,
                    "parentId": 1013,
                    "name": "Switzerland"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'CZ'">
                    "id": 63,
                    "parentId": 1013,
                    "name": "Czech Republic"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'NO'">
                    "id": 176,
                    "parentId": 1013,
                    "name": "Norway"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'HR'">
                    "id": 61,
                    "parentId": 1013,
                    "name": "Croatia"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'BY'">
                    "id": 23,
                    "parentId": 1013,
                    "name": "Belarus"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'UA'">
                    "id": 241,
                    "parentId": 1013,
                    "name": "Ukraine"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'AL'">
                    "id": 3,
                    "parentId": 1037,
                    "name": "Albania"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'CS'">
                    "id": 205,
                    "parentId": 1040,
                    "name": "Serbia and Montenegro"
                </xsl:when>
            </xsl:choose>
            }
            ]
            }
            },
            {
            "id": 504,
            "apiName": "alias",
            "alias": "Alias",
            "properties": null,
            "customExpression": [
            "if(hits > 1) {fieldValue = fieldValue.substring(0,3) + '0' + hitsHandled; return fieldValue;} else {return fieldValue; }",
            "if(hits > 9) {fieldValue = fieldValue.substring(0,3) + hitsHandled; return fieldValue;} else {return fieldValue; }"
            ],
            "fieldValue": {
            "type": "TEXT",
            "value": "<xsl:value-of select="substring(translate(H_VENDOR_NAME, ' ', ''),0,6)"/>"
            }
            },
            {
            "id": 503,
            "apiName": "name",
            "alias": "Name",
            "properties": null,
            "fieldValue": {
            "type": "TEXT",
            "value": "<xsl:value-of select="H_VENDOR_NAME"/>"
            }
            },
            {
            "id": 506,
            "apiName": "address",
            "alias": "Address",
            "properties": null,
            "fieldValue": {
            "type": "TEXT",
            "value": "<xsl:value-of
                select="concat(S_ADDRESS_LINE1, S_ADDRESS_LINE2, S_ADDRESS_LINE3, S_ADDRESS_LINE4, S_CITY, S_COUNTY, S_STATE, S_PROVINCE, S_ZIP)"/>"
            }
            },
            {
            "id": 101491,
            "apiName": "dyn101491",
            "alias": "Supplier TAX ID",
            "properties": null,
            "fieldValue": {
            "type": "TEXT",
            "value": "<xsl:value-of select="H_NUM_1099"/>"
            }
            },
            {
            "id": 101492,
            "apiName": "dyn101492",
            "alias": "Supplier VAT Number",
            "properties": null,
            "fieldValue": {
            "type": "TEXT",
            "value": "<xsl:value-of select="H_VAT_REGISTRATION_NUM"/>"
            }
            },
            {
            "id": 101496,
            "apiName": "dyn101496",
            "alias": "Supplier Site Name",
            "properties": null,
            "fieldValue": {
            "type": "TEXT",
            "value": "<xsl:value-of select="S_VENDOR_SITE_CODE"/>"
            }
            },
            {
            "id": 101495,
            "apiName": "dyn101495",
            "alias": "Supplier Site Id",
            "properties": null,
            "fieldValue": {
            "type": "TEXT",
            "value": "<xsl:value-of select="S_VENDOR_SITE_ID"/>"
 
            }
            },
            {
            "id": 101497,
            "apiName": "dyn101497",
            "alias": "Raiffeisen Unit",
            "properties": null,
            "fieldValue": {
            "type": "SINGLE_SELECT",
            "value": {
            <xsl:choose>
                <xsl:when test="H_ATTRIBUTE15 = 'Yes'">
                    "id": 11484,
                    "name": "Yes"
                </xsl:when>
                <xsl:when test="H_ATTRIBUTE15 = 'No'">
                    "id": 11485,
                    "name": "No"
                </xsl:when>
            </xsl:choose>
            }
            }
            },
            {
            "id": 101530,
            "apiName": "dyn101530",
            "alias": "Supplier Status",
            "properties": null,
            "fieldValue": {
            "type": "SINGLE_SELECT",
            "value": {
            <xsl:choose>
                <xsl:when test="H_END_DATE_ACTIVE != ''">
                    <xsl:variable name="endDateActive"
                                  select="xs:date(concat(substring(H_END_DATE_ACTIVE, 7, 4), '-',  substring(H_END_DATE_ACTIVE, 4, 2), '-', substring(H_END_DATE_ACTIVE, 1, 2)))"/>
                    <xsl:choose>
                        <xsl:when test="$endDateActive ge current-date()">
                            "id": 11762,
                            "name": "Active"
                        </xsl:when>
                        <xsl:otherwise>
                            "id": 11763,
                            "name": "Inactive"
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
            </xsl:choose>
            }
            }
            },
            {
            "id": "508",
            "apiName": "functions",
            "alias": "Functions",
            "properties":{},
            "fieldValue": {
            "type": "MULTI_SELECT",
            "value": [
            {
            "id": 1007,
            "name": "Human Resources"
            }
            ]
            }
            },
            {
            "id": 509,
            "apiName": "services",
            "alias": "Services",
            "properties": null,
            "fieldValue": {
            "type": "MULTI_SELECT",
            "value": [
            {
            "id" : 1024,
            "parentId": 1007,
            "value" : "End-User Computing"
            }
            ]
            }
            },
            {
            "id": "505",
            "apiName": "tiers",
            "alias": "Tiers",
            "fieldValue": {
            "type": "MULTI_SELECT",
            "value": [
            {
            <xsl:choose>
                <xsl:when test="H_VENDOR_TYPE_LOOKUP_CODE = 'VENDOR'">
                    "id": 1014,
                    "name": "Tier - 3"
                </xsl:when>
                <xsl:when test="H_VENDOR_TYPE_LOOKUP_CODE = 'CREDITOR'">
                    "id": 1028,
                    "name": "Tier 4"
                </xsl:when>
            </xsl:choose>
            }
            ]
            }
            },
            {
            "id": 101493,
            "apiName": "dyn101493",
            "alias": "Supplier Legal Registration Number",
            "properties": null,
            "fieldValue": {
            "type": "TEXT",
            "value": "SLRN-01"
            }
            },
            {
            "id": 507,
            "apiName": "",
            "alias": "Reporting Currency",
            "properties": null,
            "fieldValue": {
            "type": "MULTI_SELECT",
            "value": [
            {
            <xsl:choose>
                <xsl:when test="S_COUNTRY = 'RU'">
                    "id": 39,
                    "name": "Russian Ruble (RUB)"
                </xsl:when>
                <xsl:when test="S_COUNTRY = 'AT'">
                    "id": 3,
                    "name": "Australian Dollar (AUD)"
                </xsl:when>
            </xsl:choose>
            }
            ]
            }
            },
            {
            "id": 101513,
            "apiName": "dyn101513",
            "alias": "Business Unit",
            "properties": null,
            "fieldValue": {
            "type": "MULTI_SELECT",
            "value": [
            <xsl:choose>
                <xsl:when test="NWB_SHORT_NAME = 'RBHR'">
                    {
                    "id": 11730,
                    "name": "RBHR"
                    }
                </xsl:when>
                <xsl:when test="NWB_SHORT_NAME = 'RBRU'">
                    {
                    "id": 11727,
                    "name": "RBRU"
                    }
                </xsl:when>
                <xsl:when test="NWB_SHORT_NAME = 'RBHR'">
                    {
                    "id": 11730,
                    "name": "RBHR"
                    }
                </xsl:when>
                <xsl:when test="NWB_SHORT_NAME = 'RBRU'">
                    {
                    "id": 11727,
                    "name": "RBRU"
                    }
                </xsl:when>
                <xsl:when test="NWB_SHORT_NAME = 'RBHR'">
                    {
                    "id": 11730,
                    "name": "RBHR"
                    }
                </xsl:when>
                <xsl:when test="NWB_SHORT_NAME = 'RBRU'">
                    {
                    "id": 11727,
                    "name": "RBRU"
                    }
                </xsl:when>
            </xsl:choose>
            ]
            }
            }
            ]
            }
            }
            <xsl:if test="position() &lt; $supplierCount">
                ,
                <br/>
            </xsl:if>
        </xsl:for-each>
        ]
    </xsl:template>
    <!-- Object or Element Property-->
    <!-- <xsl:template match="*">
        "<xsl:value-of select="name()"/>" : <xsl:call-template name="Properties"/>
    </xsl:template> -->
    <!-- Array Element -->
    <!-- <xsl:template match="*" mode="ArrayElement">
        <xsl:call-template name="Properties"/>
    </xsl:template> -->
    <!-- Object Properties -->
    <!-- <xsl:template name="Properties">
        <xsl:variable name="childName" select="name(*[1])"/>
        <xsl:choose>
            <xsl:when test="not(*|@*)">"<xsl:value-of select="."/>"</xsl:when>
            <xsl:when test="count(*[name()=$childName]) > 1">
              { "<xsl:value-of select="$childName"/>" :[
              <xsl:apply-templates select="*" mode="ArrayElement"/>
              ] }
            </xsl:when>
            <xsl:otherwise>{
                <xsl:apply-templates select="@*"/>
                <xsl:apply-templates select="*"/>
    }</xsl:otherwise>
        </xsl:choose>
        <xsl:if test="following-sibling::*">,</xsl:if>
    </xsl:template> -->
    <!-- Attribute Property -->
    <!-- <xsl:template match="@*">"<xsl:value-of select="name()"/>" : "<xsl:value-of select="."/>",
    </xsl:template> -->
</xsl:stylesheet>