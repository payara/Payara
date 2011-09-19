$(function() {
    var available = $( "#available" ), selected = $( "#selected" );

    var deploy_icon_class = "ui-icon-arrowthickstop-1-e";
    var undeploy_icon_class = "ui-icon-arrowrefresh-1-w";
    var deploy_icon = '<a href="link/to/trash/script/when/we/have/js/off" title="Deploy" class="ui-icon ' + deploy_icon_class + '">Deploy</a>';
    var undeploy_icon = '<a href="link/to/undeploy/script/when/we/have/js/off" title="Undeploy" class="ui-icon ' + undeploy_icon_class + '">Undeploy</a>';
                    
    $('#available-applications').find('option').each(function(index) {
        var $item = createItem($(this).val(), deploy_icon);
        $('#available').append($item);
    });

    $('#selected-applications').find('option').each(function(index) {
        var $item = createItem($(this).val(), undeploy_icon);
        $( "ul.available", selected ).append($item);
    });

    // let the applications be draggable
    var $draggable = {
        cancel: "a.ui-icon", // clicking an icon won't initiate dragging
        revert: "invalid", // when not dropped, the item will revert back to its initial position
        containment: $( "#demo-frame" ).length ? "#demo-frame" : "document", // stick to demo-frame if present
        helper: "clone",
        cursor: "move"
    };
    $( "li", available ).draggable($draggable);
    $( "li", selected ).draggable($draggable);

    // let the server be droppable, accepting the available items
    selected.droppable({
        accept: "#available > li",
        activeClass: "ui-state-active",
        hoverClass: "ui-state-highlight",
        drop: function( event, ui ) {
            deploy( ui.draggable );
        }
    });

    // let the available be droppable as well, accepting items from the selected
    available.droppable({
        accept: "#selected li",
        activeClass: "ui-state-active",
        hoverClass: "ui-state-highlight",
        drop: function( event, ui ) {
            undeploy( ui.draggable );
        }
    });

    function createItem(value, a) {
        return "<li class='ui-widget-content ui-corner-tr'>" +
        "<h5 class='ui-widget-header'>" + value + "</h5>" +
        "<div style='width: 96px; height: 72px'>&nbsp;</div>" +
        "<a href='images/high_tatras.jpg' title='View larger image' class='ui-icon ui-icon-zoomin'>View Details</a>" +
        a +
        "</li>";
    }

    // select function
    function deploy( $item ) {
        $item.fadeOut(function() {
            $item.find( "a." + deploy_icon_class ).remove();
            $item.append( undeploy_icon ).appendTo( $( "ul.available", selected ) ).fadeIn();
            
            var $text = $item.find("h5").text();
            $("#available-applications > option[value='" + $text + "']").remove();
            $('#selected-applications').append($('<option></option>').val($text).html($text));

        });
    }

    // unselect function
    function undeploy ( $item ) {
        $item.fadeOut(function() {
            $item.find( "a." + undeploy_icon_class ).remove().end()
                .css( "width", "96px").append( deploy_icon )
                .find( "img" ).css( "height", "72px" ).end()
                .appendTo( available ).fadeIn();
        });

        var $text = $item.find("h5").text();
        $("#selected-applications > option[value='" + $text + "']").remove();
        $('#available-applications').append($('<option></option>').val($text).html($text));
    }

    // image preview function, demonstrating the ui.dialog used as a modal window
    function viewLargerImage( $link ) {
        var src = $link.attr( "href" ),
        title = $link.siblings( "img" ).attr( "alt" ),
        $modal = $( "img[src$='" + src + "']" );

        if ( $modal.length ) {
            $modal.dialog( "open" );
        } else {
            var img = $( "<img alt='" + title + "' width='384' height='288' style='display: none; padding: 8px;' />" )
            .attr( "src", src ).appendTo( "body" );
            setTimeout(function() {
                img.dialog({
                    title: title,
                    width: 400,
                    modal: true
                });
            }, 1 );
        }
    }

    // resolve the icons behavior with event delegation
    $( "ul.available > li" ).click(function( event ) {
        var $item = $( this ),
        $target = $( event.target );

        if ( $target.is( "a." + deploy_icon_class ) ) {
            deploy( $item );
        } else if ( $target.is( "a.ui-icon-zoomin" ) ) {
            viewLargerImage( $target );
        } else if ( $target.is( "a." + undeploy_icon_class ) ) {
            undeploy( $item );
        }

        return false;
    });

});
