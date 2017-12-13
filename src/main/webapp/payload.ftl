<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
    <title>SET Payload Tracker Issue List</title>

    <!-- Bootstrap -->
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" rel="stylesheet">
    <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
      <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->

  </head>
  <body>
    <script type="text/javascript" src="../js/jquery.min.js"></script>
    <link href="../css/bootstrap.min.css" rel="stylesheet" type="text/css" />
    <script type="text/javascript" src="../js/bootstrap.min.js"></script>
    <link href="../css/bootstrap-multiselect.css" rel="stylesheet" type="text/css" />
    <script src="../js/bootstrap-multiselect.js" type="text/javascript"></script>
    <link href="../css/prbz.css" rel="stylesheet" type="text/css" />
    <script src="../js/prbz.js"></script>

    <ul class="nav nav-pills" style="width: 80%; margin: 6px auto">
        <li style="font-size: 20px"><a href="/prbz-overview/">Home</a></li>
        <#list payloadSet as payload>
            <li<#if payload == Request.payloadName> class="active"</#if>><a href="/prbz-overview/payloadview/overview?streamName=${streamName}&payloadName=${payload}">${payload}</a></li>
        </#list>
    </ul>

     <div class="container">
		<div class="row">
		  <div class="col-md-12"><h1>EAP Cumulative Patch Releases ${Request.payloadName} Issue List</h1></div>
		</div>
		<div class="row">
		  <div class="col-md-12">
		  <h4>

		  <#if RequestParameters.selectedStatus??>
			<#if RequestParameters.missedFlags??>
				<#assign summary = "found by issue status and CDW flags">
			<#else>
				<#assign summary = "found by issue status">
			</#if>
		  <#else>
			<#if RequestParameters.missedFlags??>
				<#assign summary = "found by CDW flags">
			<#else>
				<#assign summary = "">
			</#if>
		  </#if>

			${Request.payloadSize} issues ${summary} in payload in overall status
			<#if Request.payloadStatus?has_content>
				<#switch Request.payloadStatus>
					<#case "BLOCKER"><img src="../images/red-blocker.png" alt="red-blocker" title="blocker"><#break>
					<#case "CRITICAL"><img src="../images/orange-critical.png" alt="orange-critical" title="critical"><#break>
					<#case "MAJOR"><img src="../images/yellow-major.png" alt="yellow-major" title="major"><#break>
					<#case "MINOR"><img src="../images/blue-minor.png" alt="blue-minor" title="minor"><#break>
					<#case "TRIVIAL"><img src="../images/gray-trivial.png" alt="gray-trivial" title="trivial"><#break>
				</#switch>
			<#else>
				<img src="../images/green-good.png" alt="good green light" title="good">
			</#if>
		  </h4>
		  <ul>
				<li><img src="../images/red-blocker.png" alt="red-blocker" title="blocker"> Red status with blocker issue(s), an immediate call to triage.</li>
				<li><img src="../images/orange-critical.png" alt="orange-critical" title="critical"> Orange status with critical issue(s), attention is needed until progress can no longer be made.</li>
				<li><img src="../images/yellow-major.png" alt="yellow-major" title="major"> Yellow status with major issue(s), a pending condition to triage.</li>
				<li><img src="../images/blue-minor.png" alt="blue-minor" title="minor"> Blue status with minor issue(s), attention is needed, forward progress can be made.</li>
				<li><img src="../images/gray-trivial.png" alt="gray-trivial" title="trivial"> gray status with trivial issue(s), process is moving forward as planned with trivial obstacle.</li>
				<li><img src="../images/green-good.png" alt="good green light" title="good"> Green status without notable issue(s), process is moving forward as planned with no visible obstacle.</li>
		  </ul>

		  <form action="/prbz-overview/payloadview/overview">
			  <input type="hidden" name="payloadName" value=${Request.payloadName}>
			  <input type="hidden" name="streamName" value=${Request.streamName}>
			  <select id="lstStatus" name="selectedStatus" multiple="multiple">
				  <option value="RED">RED</option>
				  <option value="ORANGE">ORANGE</option>
				  <option value="YELLOW">YELLOW</option>
				  <option value="BLUE">BLUE</option>
				  <option value="GRAY">GRAY</option>
				  <option value="GREEN">GREEN</option>
			  </select>
			  <select id="lstFlags" name="missedFlags" multiple="multiple">
				  <option value="PM">Need PM+</option>
				  <option value="DEV">Need DEV+</option>
				  <option value="QE">Need QE+</option>
			  </select>
			  <input type="submit" id="" value="Search" />
		  </form>

		  </div>
		</div>
		<div class="row">
		  <div class="col-md-12">
			  	<table id="eventTable" class="table">
                    <colgroup>
                        <col style="width: 20px">
                        <col style="width: 140px">
                        <col style="width: 100px">
                        <col style="width: 50px">
                        <col style="width: 150px">
                        <col>
                    </colgroup>
			  		<thead>
			  			<tr>
                            <th><span class="switch" title="Expand All"></span></th>
                            <th>Dependency Issue</th>
                            <th>Status</th>
                            <th>Type</th>
                            <th>Flags</th>
                            <th>Violations</th>
			  			</tr>
			  		</thead>
			  		<tbody id="eventTableBody">
						<#list rows as row>
							<#assign data = row.data>
                            <#assign hasAdditionalData = data.associatedPullRequest?has_content || data.associatedUnrelatedPullRequest?has_content || data.dependsOn?has_content>
							<tr class="odd<#if hasAdditionalData> has-data</#if>">
                                <td>
                                    <span class="switch" title="Expand"></span>
                                </td>
								<td>
									<#if data.payloadDependency.maxSeverity?has_content>
										<#switch data.payloadDependency.maxSeverity>
											<#case "BLOCKER"><img src="../images/red-blocker.png" alt="red-blocker" title="blocker"><#break>
											<#case "CRITICAL"><img src="../images/orange-critical.png" alt="orange-critical" title="critical"><#break>
											<#case "MAJOR"><img src="../images/yellow-major.png" alt="yellow-major" title="major"><#break>
											<#case "MINOR"><img src="../images/blue-minor.png" alt="blue-minor" title="minor"><#break>
											<#case "TRIVIAL"><img src="../images/gray-trivial.png" alt="gray-trivial" title="trivial"><#break>
										</#switch>
									<#else>
										<img src="../images/green-good.png" alt="good green light" title="good">
									</#if>
                                    <a href="${data.payloadDependency.link}" title="${data.payloadDependency.summary}">#${data.payloadDependency.label}</a>
                                </td>
                                <td>
                                    ${data.payloadDependency.status}
                                </td>
                                <td>
                                    ${data.payloadDependency.type}
                                </td>
                                <td>
                                    <#if data.payloadDependency.allAcks>
                                        <span class="label label-success">Has all 3 acks</span>
                                    <#else>
                                        <#list data.payloadDependency.flags?keys as key>
                                            <#switch data.payloadDependency.flags[key]>
                                                <#case "SET"> <span class="label label-primary">${key} ?</span><#break>
                                                <#case "ACCEPTED"> <span class="label label-success">${key} +</span><#break>
                                                <#case "REJECTED"> <span class="label label-danger">${key} -</span><#break>
                                            </#switch>
                                        </#list>
                                    </#if>
                                </td>
                                <td>
                                    <#list data.payloadDependency.violations>
                                        <ul>
                                            <#items as violation>
                                                <li>
                                                    <#switch violation.level>
                                                        <#case "BLOCKER"><img src="../images/red-blocker.png" alt="red-blocker" title="blocker"><#break>
                                                        <#case "CRITICAL"><img src="../images/orange-critical.png" alt="orange-critical" title="critical"><#break>
                                                        <#case "MAJOR"><img src="../images/yellow-major.png" alt="yellow-major" title="major"><#break>
                                                        <#case "MINOR"><img src="../images/blue-minor.png" alt="blue-minor" title="minor"><#break>
                                                        <#case "TRIVIAL"><img src="../images/gray-trivial.png" alt="gray-trivial" title="trivial"><#break>
                                                    </#switch>
                                                    ${violation.level} Violation ${violation.checkName} : ${violation.message}
                                                </li>
                                            </#items>
                                        </ul>
                                    </#list>
                                </td>
                            </tr>
                            <tr class="even">
                                <td colspan="6">
                                    <#if data.associatedPullRequest?has_content || data.associatedUnrelatedPullRequest?has_content>
                                        <div class="inner-div">
                                            <table class="inner-table table table-striped">
                                                <thead>
                                                    <tr>
                                                        <th>Pull Requests</th>
                                                        <th>Branch</th>
                                                        <th>Build Result</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
            								    	<#list data.associatedPullRequest>
            								    		<#items as patch>
            				  								<tr>
                                                                <td>
                                                                    <a href="${patch.link}">#${patch.label}</a>
                                                                </td>
                                                                <td>
                                                                    ${patch.codebase}
                                                                </td>
                                                                <td>
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
                														<span class="label label-success">No Upstream Required</span>
                													</#if>
                				  								</td>
                                                            </tr>
            				  							</#items>
            										</#list>
            										<#list data.associatedUnrelatedPullRequest>
            											<#items as patch>
            												<tr>
                                                                <td><a href="${patch.link}">#${patch.label}</a></td>
                                                                <td>${patch.codebase}</td>
                                                                <td>
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
                													<span class="label label-info">other stream</span>
                													<#if patch.noUpstreamRequired?? && (patch.noUpstreamRequired==true)>
                														<span class="label label-success">No Upstream Required</span>
                													</#if>
            												    </td>
                                                            </tr>
            											</#items>
            										</#list>
                                                </tbody>
                                            </table>
                                        </div>
                                    </#if>
                                    <#list data.dependsOn>
                                        <div class="inner-div">
                                            <table class="inner-table table table-striped issues-table">
                                                <colgroup>
                                                    <col class="first-column">
                                                    <col class="second-column">
                                                    <col class="third-column">
                                                    <col>
                                                </colgroup>
                                                <thead>
                                                    <tr>
                                                        <th>Depends On</th>
                                                        <th>Status</th>
                                                        <th>Type</th>
                                                        <th>Flags</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <#items as issue>
                                                        <tr>
                                                            <td><a href="${issue.link}" title="${issue.summary}">#${issue.label}</a></td>
                                                            <td>${issue.status}</td>
                                                            <td>${issue.type}</td>
                                                            <td>
                                                                <#if issue.fixVersions?has_content>
                                                                    Fix Version:
                                                                    <#list issue.fixVersions as fixVersion> ${fixVersion} </#list>
                                                                </#if>
                                                                <#if issue.payload?has_content && (issue.payload != "N/A")>
                                                                    <span class="label label-success">${issue.payload} payload</span>
                                                                </#if>
                                                                <#if issue.streamStatus??>
                                                                    <#list issue.streamStatus?keys as key>
                                                                        <span class="label label-primary">${key} stream </span>
                                                                        </#list>
                                                                </#if>
                                                                <#if issue.inPayload?? && (issue.inPayload==false)>
                                                                    <span class="label label-warning">Not in Payload</span>
                                                                </#if>
                                                            </td>
                                                        </tr>
                                                    </#items>
                                                </tbody>
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
  </body>

<script type="text/javascript">
	$(function() {
		$('#lstStatus').multiselect({
			includeSelectAllOption: true
		});
	});
	$(function() {
		$('#lstFlags').multiselect({
			includeSelectAllOption: true
		});
	});
    $(function() {
        expandableRows();
    });
</script>
</html>
