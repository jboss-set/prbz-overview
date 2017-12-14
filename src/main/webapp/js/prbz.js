var expandableRows = function() {
    $('th .switch').parent().on('click',function () {
        var header = $(this).toggleClass('open');
        if (header.hasClass('open')) {
            $('.odd.has-data').addClass('open').find('.switch').attr('title','Collapse');
            header.find('.switch').attr('title','Collapse All');
        } else {
            $('.odd.has-data').removeClass('open').find('.switch').attr('title','Expand');
            header.find('switch').attr('title','Expand All');
        }
    });
    $('.odd.has-data .switch').parent().on('click', function () {
        var parent = $(this).parent().toggleClass('open');
        if (parent.hasClass('open')) {
            $(this).find('.switch').attr('title','Collapse');
        } else {
            $(this).find('.switch').attr('title','Expand');
        }
    });
}
