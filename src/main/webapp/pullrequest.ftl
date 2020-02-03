<#import "macros.ftl" as pr>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
    <title>SET Pull Requets on Repository List</title>

    <!-- Bootstrap -->
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" rel="stylesheet">
    <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
      <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->
    <link href="../../../../css/prbz.css" rel="stylesheet" type="text/css" />
    <link href="//cdn.datatables.net/1.10.20/css/jquery.dataTables.min.css" rel="stylesheet" />
  </head>
  <body>
      <ul class="nav nav-pills" style="width: 80%; margin: 6px auto">
          <li style="font-size: 20px"><a href="/prbz-overview/">Home</a></li>
          <#list streamMap?keys as stream>
              <li role="presentation" class="dropdown">
                <a class="dropdown-toggle" data-toggle="dropdown" href="#" role="button" aria-haspopup="true" aria-expanded="false">
                    ${stream} <span class="caret"></span>
                </a>
                <ul class="dropdown-menu">
                    <#list streamMap[stream] as component>
                    <li class="<#if component == Request.componentName && stream == Request.streamName>active</#if>"><a href="/prbz-overview/rest/streampullrequest/${stream}/component/${component}">${component}</a></li>
                    </#list>
                </ul>
              </li>
          </#list>
      </ul>
     <div class="container">
		<div class="row">
		  <div class="col-md-12"><h1>EAP Cumulative Patch Releases ${Request.streamName} -  ${Request.componentName} List</h1></div>
		</div>
		<div class="row">
		  <div class="col-md-12"><h4>${Request.pullRequestSize} pull requests on repository</h4></div>
			  	<table id="eventTable" class="table">
                    <colgroup>
                        <col style="width: 20px">
                        <col style="width: 330px">
                        <col style="width: 80px">
                        <col style="width: 120px">
                        <col>
                    </colgroup>
			  		<thead>
			  			<tr>
                            <th class="switch" title="Expand All"><span></span></th>
			  				<th>Pull Request</th>
							<th>Branch</th>
							<th>Streams</th>
							<th>Issues</th>
			  			</tr>
			  		</thead>
			  		<tbody id="eventTableBody">
						<#list rows as row>
							<#assign data = row.data>
                            <#assign hasAdditionalData = data.pullRequestsRelated?has_content || data.issuesOtherStreams?has_content>
                            <#assign issueCount = data.issuesRelated?size>
                            <tr<#if hasAdditionalData> class="has-data"</#if> data-pr-id="${data.pullRequest.label}">
                                <td<#if hasAdditionalData> class="switch" title="Expand"</#if>>
                                    <span></span>
                                </td>
								<td>
									<a href="${data.pullRequest.link}">#${data.pullRequest.label}</a>
									<#switch data.pullRequest.patchState>
										<#case "OPEN"> <span class="label label-success">open</span><#break>
										<#case "CLOSED"> <span class="label label-default">closed</span><#break>
										<#case "UNDEFINED"> <span class="label label-default">undefined</span><#break>
									</#switch>
									<#switch data.pullRequest.commitStatus>
										<#case "success"> <span class="label label-success">success</span><#break>
										<#case "failure"> <span class="label label-warning">failure</span><#break>
										<#case "error"> <span class="label label-danger">error</span><#break>
										<#case "pending"> <span class="label label-default">pending</span><#break>
										<#case "unknown"> <span class="label label-primary">unknown</span><#break>
									</#switch>
									<#if data.pullRequest.noUpstreamRequired?? && (data.pullRequest.noUpstreamRequired==true)>
										<span class="label label-success">No Upstream Required</span>
									</#if>
								</td>
								<td>${data.branch}</td>
								<td>
									<#list data.streams as stream> ${stream} </#list>
								</td>
                                <td>
							    	<#list data.issuesRelated>
                                        <div class="inner-div">
                                        <table class="table issues-table">
                                            <colgroup>
                                                <col class="first-column">
                                                <col class="second-column">
                                                <col class="third-column">
                                                <col>
                                            </colgroup>
                                            <#items as issue>
                                                <tr>
                                                    <td>
                                                        <a href="${issue.link}" title="${issue.summary}">#${issue.label}</a>
                                                    </td>
                                                    <td>
                                                        ${issue.status}
                                                    </td>
                                                    <td>
                                                        ${issue.type}
                                                    </td>
                                                    <#assign status = data.status>
                                                    <td>
                                                        <#switch status[issue.label]>
                                                            <#case 1> <span class="label label-success">ready to go</span><#break>
                                                            <#case 2> <span class="label label-warning">no stream</span><#break>
                                                            <#case 3> <span class="label label-danger">flags needed</span><#break>
                                                        </#switch>
                                                        <#list issue.flags?keys as key>
                                                            <#switch issue.flags[key]>
                                                                <#case "SET"> <span class="label label-primary">${key} ?</span><#break>
                                                                <#case "ACCEPTED"> <span class="label label-success">${key} +</span><#break>
                                                                <#case "REJECTED"> <span class="label label-danger">${key} -</span><#break>
                                                            </#switch>
                                                        </#list>
                                                    </td>
                                                </tr>
                                            </#items>
                                        </table>
                                    </div>
							    	</#list>
                                </td>
                            </tr>
						</#list>
			  		</tbody>
			  	</table>
		    </div>
		</div>
	</div>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js"></script>
<script src="https://cdn.datatables.net/1.10.20/js/jquery.dataTables.min.js"></script>
<script src="../../../../js/prbz.js"></script>
<script type="text/javascript">
    var expRows = {
        <#list rows as row>
            <#assign data = row.data>
            <#if data.pullRequestsRelated?has_content || data.issuesOtherStreams?has_content>
            "${data.pullRequest.label}": {
                "pullrequests": [
                <@pr.pullRequest data.pullRequestsRelated />
                ],
                "issues": [
                <#list data.issuesOtherStreams as issue>
                    {
                        "link": "${issue.link}",
                        "summary": "${issue.summary?html}",
                        "label": "#${issue.label}",
                        "status": "${issue.status}",
                        "type": "${issue.type}",
                        "flags": [
                            <#if issue.streamStatus??>
                                <#list issue.streamStatus?keys as key>
                                    '<span class="label label-primary">${key}</span>',
                                </#list>
                            </#if>
                            <#assign status = data.status>
                            '<#switch status[issue.label]>
                                <#case 1> <span class="label label-success">ready to go</span><#break>
                                <#case 2> <span class="label label-warning">no stream</span><#break>
                                <#case 3> <span class="label label-danger">flags needed</span><#break>
                            </#switch>',
                            <#list issue.flags?keys as key>
                                '<#switch issue.flags[key]>
                                    <#case "SET"> <span class="label label-primary">${key} ?</span><#break>
                                    <#case "ACCEPTED"> <span class="label label-success">${key} +</span><#break>
                                    <#case "REJECTED"> <span class="label label-danger">${key} -</span><#break>
                                </#switch>'<#sep>,
                            </#list>
                        ]
                    }<#sep>,
                </#list>
                ]
            },
            </#if>
            </#list>
        "": "" // required for syntactically correct JSON
    }

    $(document).ready( function () {
        var eventTable = $('#eventTable').DataTable({
            "paging": false,
            "info": false,
            "order": [],
            "columnDefs": [
                { "orderable": false, "targets": [0, 4] }
              ]
        });

        expandableRows(eventTable, expRows, false);
    } );
</script>
</body>
</html>
