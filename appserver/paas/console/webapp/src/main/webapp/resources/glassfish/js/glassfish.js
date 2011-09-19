var lazyLoadElements = [];

function doLazyLoad() {
    function isScrolledIntoView(elem) {
        var docViewTop = $(window).scrollTop(),
        docViewBottom = docViewTop + $(window).height(),
        elemTop = $(elem).offset().top,
        elemBottom = elemTop + $(elem).height();
        //Is more than half of the element visible
        return ((elemTop + ((elemBottom - elemTop)/2)) >= docViewTop && ((elemTop + ((elemBottom - elemTop)/2)) <= docViewBottom));
    }

    $('.__lazyload').each(function(el) {
        if (isScrolledIntoView($(this))) {
            jsf.ajax.request($(this).prop('id'), null, {render:'@form'});
        }
    });
}

$(document).scroll(doLazyLoad);
$(document).ready(doLazyLoad);
