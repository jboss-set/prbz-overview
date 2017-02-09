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
	<style>
		.streams {
             padding: 5px;
             width: 280px;
        }
	</style>
  </head>
  <body>
     <div class="container">
		<div class="row">
		  <div class="col-md-12"><h1>EAP Cumulative Patch Releases ${Request.streamName} -  ${Request.componentName} List</h1></div>
		</div>
		<div class="row">
		  <div class="col-md-12"><h4>${Request.pullRequestSize} pull requests on repository</h4></div>
			  	<table id="eventTable" class="table table-striped">
			  		<thead>
			  			<tr>
			  				<th>Pull Request</th>
							<th>Branch</th>
							<th>Streams</th>
							<th>Issues</th>
							<th>Other Streams Pull Request</th>
							<th>Issues Other Streams</th>
			  			</tr>
			  		</thead>
			  		<tbody id="eventTableBody">
						<#list rows as row>
							<#assign data = row.data>
							<tr>
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
							    		<ul>
							    		<#items as issue>
											<li>
												<a href="${issue.link}" title="${issue.summary}">#${issue.label}</a> - ${issue.status} - ${issue.type}
							    		   		<#assign status = data.status>
							    		   		<#switch status[issue.label]>
													  <#case 1> <span class="label label-success">ready to go</span><#break>
													  <#case 2> <span class="label label-warning">no stream</span><#break>
													  <#case 3> <span class="label label-danger">flags needed</span><#break>
												</#switch>
												<br/>
												<#list issue.flags?keys as key>
													<#switch issue.flags[key]>
														<#case "SET"> <span class="label label-primary">${key} ?</span><#break>
														<#case "ACCEPTED"> <span class="label label-success">${key} +</span><#break>
														<#case "REJECTED"> <span class="label label-danger">${key} -</span><#break>
													</#switch>
												</#list>
											</li>
							    		</#items>
							    		</ul> 
							    	</#list>
							    </td>

							    <td>
							    	<#list data.pullRequestsRelated>
							    		<ul>
							    		<#items as patch>
			  								<li>
											<a href="${patch.link}">#${patch.label}</a> - ${patch.codebase} -
											<#switch patch.patchState>
													<#case "OPEN"> <span class="label label-success">open</span><#break>
													<#case "CLOSED"> <span class="label label-default">closed</span><#break>
													<#case "UNDEFINED"> <span class="label label-default">undefined</span><#break>
												</#switch>
												<#switch patch.commitStatus>
													<#case "success"> <span class="label label-success">success</span><#break>
													<#case "failure"> <span class="label label-warning">failure</span><#break>
													<#case "error"> <span class="label label-danger">error</span><#break>
													<#case "pending"> <span class="label label-default">pending</span><#break>
													<#case "unknown"> <span class="label label-primary">unknown</span><#break>
												</#switch>
												<#if patch.noUpstreamRequired?? && (patch.noUpstreamRequired==true)>
													<span class="label label-success">No Upstream Required</span><#break>
												</#if>
			  								</li>
			  							</#items>
			  							</ul>
									</#list>
							    </td>

							    <td>
							    	<#list data.issuesOtherStreams>
							    		<ul>
							    		<#items as issue>
							    		   <li>
												<a href="${issue.link}" title="${issue.summary}">#${issue.label}</a> - ${issue.status} - ${issue.type}
												<#if issue.streamStatus??>
													<#list issue.streamStatus?keys as key>
														<span class="label label-primary">${key}</span>
													</#list>
												</#if>
							    		   		<#assign status = data.status>
							    		   		<#switch status[issue.label]>
													  <#case 1> <span class="label label-success">ready to go</span><#break>
													  <#case 2> <span class="label label-warning">no stream</span><#break>
													  <#case 3> <span class="label label-danger">flags needed</span><#break>
												</#switch>
												<br/>
												<#list issue.flags?keys as key>
													<#switch issue.flags[key]>
														<#case "SET"> <span class="label label-primary">${key} ?</span><#break>
														<#case "ACCEPTED"> <span class="label label-success">${key} +</span><#break>
														<#case "REJECTED"> <span class="label label-danger">${key} -</span><#break>
													</#switch>
												</#list>
							    		   </li>
							    		</#items>
							    		</ul> 
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

  </body>
</html>