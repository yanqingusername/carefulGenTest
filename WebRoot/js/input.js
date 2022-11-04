	var oHeight = $(document).height();
	$('#main').css("height", oHeight + "px");
	$(window).resize(function(){
	    if($(document).height() < oHeight){
		    $('#main').css("height", oHeight + "px");
		    $("footer").css("position","static");
		}else{
		    $("footer").css({"position":"fixed","bottom":0});
		}
	});
	// var $html = $('html');
 //  	var remBase = $(window).width() / 10;
 //  	var fontSize = remBase;
 //  	alert(fontSize);
 //  	while (true) {
	//     var actualSize = parseInt( $html.css('font-size') );
	//     if(actualSize > remBase && fontSize > 10){
	//     	fontSize--;

	//     	$html.attr('style', 'font-size:' + fontSize + 'px!important');
	//     } else {
	//       break;
	//     }
	// }
