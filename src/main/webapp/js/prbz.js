// expandable row formatters
var formatPullRequests = function(pullRequests) {
    let table = '';

    pullRequests.forEach(
        patch => table +=
        `<tr>
            <td><a href="${patch.link}">#${patch.label}</a></td>
            <td>${patch.codebase}</td>
            <td>${patch.result.join('')}</td>
        </tr>\n`
    );

    return table;
};

var formatIssues = function(issues) {
    let table = '';

    issues.forEach(
        issue => table +=
        `<tr>
            <td><a href="${issue.link}" title="${issue.summary}">#${issue.label}</a></td>
            <td>${issue.status}</td>
            <td>${issue.type}</td>
            <td>${issue.flags.join('')}</td>
        </tr>\n`
    );

    return table;
};

var formatRow = function(data, payload) {
    let prTable = '', dTable = '';
    if (data.pullrequests.length) {
        prTable =
        `<div class="exp inner-div">
            <table class="inner-table table table-striped">
                <thead>
                    <tr>
                        <th>${payload?'':'Other '}Pull Requests</th>
                        <th>Branch</th>
                        <th>Build Result</th>
                    </tr>
                </thead>
                <tbody>
                    ${formatPullRequests(data.pullrequests)}
                </tbody>
            </table>
        </div>\n`;
    }

    if (data.issues.length) {
        dTable =
        `<div class="exp inner-div">
            <table class="inner-table table table-striped issues-table">
                <colgroup>
                    <col class="first-column">
                    <col class="second-column">
                    <col class="third-column">
                    <col>
                </colgroup>
                <thead>
                    <tr>
                        <th>${payload?'Depends On':'Other Issues'}</th>
                        <th>Status</th>
                        <th>Type</th>
                        <th>Flags</th>
                    </tr>
                </thead>
                <tbody>
                    ${formatIssues(data.issues)}
                </tbody>
            </table>
        </div>\n`;
    }

    return prTable + dTable;
};

var expandableRows = function(dataTable, rows, payload) {
    $('#eventTable tbody').on('click expand collapse', 'td.switch', function (e) {
        var tr = $(this).closest('tr');
        var row = dataTable.row(tr);

        if (row.child.isShown() && e.type != "expand") {
            row.child.hide();
            tr.removeClass('open');
        }
        else if (e.type != "collapse") {
            row.child(formatRow( rows[tr.data('prId')], payload )).show();
            tr.addClass('open');
        }
    } );

    $('th.switch').on('click',function () {
        let header = $(this).toggleClass('open'),
            eventType = header.hasClass('open') ? 'expand' : 'collapse';
        $('#eventTable tbody td.switch').trigger(eventType);
    });
}
