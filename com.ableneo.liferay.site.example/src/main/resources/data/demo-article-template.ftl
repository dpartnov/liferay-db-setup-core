<#--
    Application template for the demo article structure. The variables are named after the
    "fieldReference" of each field in demo-article-structure.json.
-->
<div class="db-setup-core-demo-article">
    <h2>${headline.getData()}</h2>

    <#if body?? && body.getData()?has_content>
        <p>${body.getData()}</p>
    </#if>
</div>
