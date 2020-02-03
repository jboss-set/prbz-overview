<#-- macro to output PR data as JSON -->
<#macro pullRequest list>
<#list list as patch>
            {
                "link": "${patch.link}",
                "label": "${patch.label}",
                "codebase": "${patch.codebase}",
                "result": [
                    '<#switch patch.patchState>
                        <#case "OPEN"> <span class="label label-success">open</span><#break>
                        <#case "CLOSED"> <span class="label label-default">closed</span><#break>
                        <#case "UNDEFINED"> <span class="label label-default">undefined</span><#break>
                    </#switch>',
                    '<#switch patch.commitStatus>
                        <#case "success"> <span class="label label-success">success</span><#break>
                        <#case "failure"> <span class="label label-warning">failure</span><#break>
                        <#case "error"> <span class="label label-danger">error</span><#break>
                        <#case "pending"> <span class="label label-default">pending</span><#break>
                        <#case "unknown"> <span class="label label-primary">unknown</span><#break>
                    </#switch>'
                    <#if patch.noUpstreamRequired!false>
                    ,'<span class="label label-success">No Upstream Required</span>'
                    </#if>
                ]
            }<#sep>,
</#list>
</#macro>
