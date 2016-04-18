<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
    <title>Set Feed</title>

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
		  <div class="col-md-12"><h1>pull-request-processor feed</h1></div>
		</div>
		<div class="row">
		  <div class="col-md-12">
			  	<table id="eventTable" class="table table-striped">
			  		<thead>
			  			<tr>
			  				<th>Repository</th>
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
								<td><a href="${data.repository.link}">${data.repository.label}</a></td>
								<td><a href="${data.pullRequest.link}">#${data.pullRequest.label}</a></td>
								<td>${data.branch}</td>
								<td>	
									<#list data.streams as stream> ${stream} </#list>
								</td>
								<td>
							    	<#list data.issuesRelated>
							    		<ul>
							    		<#items as issue>
							    		   <li>
							    		   		<a href="${issue.link}">#${issue.label} <#if issue.streams?has_content>(<#list issue.streams as stream> ${stream}<#sep>, </#list> )</#if> </a>
							    		   		<#assign status = data.status>
							    		   		<#switch status[issue.label]>
													  <#case 1> <span class="label label-success">ready to go</span><#break>
													  <#case 2> <span class="label label-warning">no stream</span><#break>
													  <#case 3> <span class="label label-danger">flags needed</span><#break>
												</#switch>
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
			  									<a href="${patch.link}">#${patch.label} <#if patch.streams?has_content>(<#list patch.streams as stream> ${stream}<#sep>, </#list> )</#if> </a>
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
							    		   		<a href="${issue.link}">#${issue.label} <#if issue.streams?has_content>(<#list issue.streams as stream> ${stream}<#sep>, </#list> )</#if> </a>
							    		   		<#assign status = data.status>
							    		   		<#switch status[issue.label]>
													  <#case 1> <span class="label label-success">ready to go</span><#break>
													  <#case 2> <span class="label label-warning">no stream</span><#break>
													  <#case 3> <span class="label label-danger">flags needed</span><#break>
												</#switch>
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