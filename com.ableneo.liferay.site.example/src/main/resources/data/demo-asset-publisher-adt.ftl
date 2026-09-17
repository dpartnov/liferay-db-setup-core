<#--
    Application display template (ADT) for the Asset Publisher portlet.
-->
<ul class="db-setup-core-demo-adt">
    <#list entries as entry>
        <#assign assetRenderer = entry.getAssetRenderer() />

        <li>${assetRenderer.getTitle(locale)}</li>
    </#list>
</ul>
