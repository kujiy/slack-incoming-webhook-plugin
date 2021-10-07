
<#if executionData.job.group??>
    <#assign jobName="${executionData.project} > ${executionData.job.group} / ${executionData.job.name}">
<#else>
    <#assign jobName="${executionData.project} > ${executionData.job.name}">
</#if>
<#assign message="[Execution #${executionData.id}](${executionData.href}) of job [${jobName}](${executionData.job.href})">
<#if trigger == "start">
    <#assign state="Started">
<#elseif trigger == "failure">
    <#assign state="Failed">
<#else>
    <#assign state="Succeeded">
</#if>

{
   "channel": "${channel}",
   "username": "${username!"RunDeck"}",
   <#if (icon_url)?has_content>"icon_url": "${icon_url}",<#else>"icon_emoji": ":rundeck:",</#if>
   "author_name": "${username!"RunDeck"}",
   "attachments":[
      {
         "fallback":"${state}: ${message}",
         "pretext":":rundeck: ${message}",
         "color":"${color}",
         "fields":[
            {
               "title":":unicorn_face: Job Name",
               "value":"[${jobName}](${executionData.job.href})",
               "short":true
            },
            {
               "title":":rainbow: Status",
               "value":"${state}",
               "short":true
            }
<#if trigger == "failure">
            ,{
               "title":":skull_and_crossbones: Failed Nodes",
               "value":"${executionData.failedNodeListString!"- (Job itself failed)"}",
               "short":false
            }
</#if>
]
      }
   ]
}

